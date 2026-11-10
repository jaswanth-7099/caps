package com.example.ocr.web;

import com.example.ocr.config.OCRMode;
import com.example.ocr.config.OCRSettings;
import com.example.ocr.input.DecodedImage;
import com.example.ocr.input.ImageInputHandler;
import com.example.ocr.output.OCRResult;
import com.example.ocr.service.ErrorRates;
import com.example.ocr.service.OCRComparisonRow;
import com.example.ocr.service.OCRComparisonService;
import com.example.ocr.service.OCRExecutionService;
import com.example.ocr.service.OCRMetrics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OcrWebServer implements AutoCloseable {
    private static final int MAX_REQUEST_BYTES = 32 * 1024 * 1024;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpServer server;
    private final Path tessdataPath;
    private final String defaultLanguage;
    private final ConcurrentMap<String, OCRExecutionService> executionServices = new ConcurrentHashMap<>();
    private final Object executionLock = new Object();
    private final ExecutorService executor;

    public OcrWebServer(int port, Path tessdataPath, String language) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 0);
        this.tessdataPath = tessdataPath;
        defaultLanguage = language == null || language.isBlank() ? "eng" : language;
        server.createContext("/", this::handle);
        ClassLoader applicationClassLoader = OcrWebServer.class.getClassLoader();
        executor = Executors.newFixedThreadPool(4, task -> {
            Thread thread = new Thread(task, "ocr-web-worker");
            thread.setContextClassLoader(applicationClassLoader);
            return thread;
        });
        server.setExecutor(executor);
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") && exchange.getRequestMethod().equals("GET")) {
                servePage(exchange);
            } else if (path.equals("/api/ocr") && exchange.getRequestMethod().equals("POST")) {
                handleOcr(exchange);
            } else {
                sendJsonError(exchange, 404, "Not found.");
            }
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            sendJsonError(exchange, 500, message(exception));
        } finally {
            exchange.close();
        }
    }

    private void servePage(HttpExchange exchange) throws IOException {
        try (var page = OcrWebServer.class.getResourceAsStream("/web/index.html")) {
            if (page == null) {
                sendJsonError(exchange, 500, "Web interface resource /web/index.html is missing.");
                return;
            }
            byte[] content = page.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
        }
    }

    private void handleOcr(HttpExchange exchange) throws IOException {
        if (!"application/json".equalsIgnoreCase(exchange.getRequestHeaders().getFirst("Content-Type"))) {
            sendJsonError(exchange, 415, "Content-Type must be application/json.");
            return;
        }
        byte[] body = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
        if (body.length > MAX_REQUEST_BYTES) {
            sendJsonError(exchange, 413, "Request is too large; maximum JSON request size is 32 MB.");
            return;
        }

        JsonNode request;
        try {
            request = JSON.readTree(body);
        } catch (IOException exception) {
            sendJsonError(exchange, 400, "Invalid JSON request: " + message(exception));
            return;
        }
        if (request == null || !request.isObject()) {
            sendJsonError(exchange, 400, "Request body must be a JSON object.");
            return;
        }
        JsonNode imageData = request.get("imageData");
        if (imageData == null || !imageData.isTextual() || imageData.asText().isBlank()) {
            sendJsonError(exchange, 400, "imageData must contain a Base64 image or image data URI.");
            return;
        }
        JsonNode compareNode = request.get("compare");
        if (compareNode != null && !compareNode.isBoolean()) {
            sendJsonError(exchange, 400, "compare must be a boolean.");
            return;
        }
        boolean compare = compareNode != null && compareNode.booleanValue();
        final String groundTruth;
        String language;
        try {
            groundTruth = optionalText(request, "groundTruth");
            language = optionalText(request, "language");
        } catch (IllegalArgumentException exception) {
            sendJsonError(exchange, 400, message(exception));
            return;
        }
        if (language == null || language.isBlank()) {
            language = defaultLanguage;
        }
        if (language.length() > 64) {
            sendJsonError(exchange, 400, "language must not exceed 64 characters.");
            return;
        }
        OCRExecutionService executionService = executionServices.computeIfAbsent(language,
                key -> new OCRExecutionService(new OCRSettings(tessdataPath, key)));
        final DecodedImage decoded;
        try {
            decoded = new ImageInputHandler().fromBase64(imageData.asText());
        } catch (IOException exception) {
            sendJsonError(exchange, 400, message(exception));
            return;
        }

        try {
            ObjectNode response;
            synchronized (executionLock) {
                response = compare
                        ? compare(decoded.image(), groundTruth, executionService)
                        : recognize(decoded.image(), request, groundTruth, executionService);
            }
            response.put("imagePreview", toPngDataUri(decoded.image()));
            sendJson(exchange, 200, response);
        } catch (IllegalArgumentException exception) {
            sendJsonError(exchange, 400, message(exception));
        } catch (Exception | LinkageError exception) {
            System.getLogger(OcrWebServer.class.getName()).log(System.Logger.Level.ERROR,
                    "OCR web request failed", exception);
            sendJsonError(exchange, 500, exception.getClass().getSimpleName() + ": " + message(exception));
        }
    }

    private ObjectNode recognize(BufferedImage image, JsonNode request, String groundTruth,
                                 OCRExecutionService executionService) throws Exception {
        String modeValue = optionalText(request, "mode");
        if (modeValue == null || modeValue.isBlank()) {
            throw new IllegalArgumentException("Select an OCR mode or choose Compare all.");
        }
        OCRMode mode = OCRMode.parse(modeValue);
        OCRResult result = executionService.createProcessor(mode).process(image);
        ErrorRates rates = groundTruth == null ? null : OCRMetrics.calculate(groundTruth, result.text());
        ObjectNode response = JSON.createObjectNode();
        response.put("compare", false);
        ArrayNode results = response.putArray("results");
        results.add(resultNode(result, rates == null ? null : rates.cer(), rates == null ? null : rates.wer()));
        return response;
    }

    private ObjectNode compare(BufferedImage image, String groundTruth, OCRExecutionService executionService) {
        List<OCRComparisonRow> rows = new OCRComparisonService(executionService).compare(image, groundTruth);
        ObjectNode response = JSON.createObjectNode();
        response.put("compare", true);
        ArrayNode results = response.putArray("results");
        for (OCRComparisonRow row : rows) {
            results.add(resultNode(row.result(), row.cer(), row.wer()));
        }
        return response;
    }

    private ObjectNode resultNode(OCRResult result, Double cer, Double wer) {
        ObjectNode node = JSON.createObjectNode();
        node.put("mode", result.mode().name());
        node.put("engine", result.engine());
        node.put("processingMillis", result.processingMillis());
        node.put("text", result.text());
        if (result.confidence() == null) {
            node.putNull("confidence");
        } else {
            node.put("confidence", result.confidence());
        }
        if (result.error() == null) {
            node.putNull("error");
        } else {
            node.put("error", result.error());
        }
        if (cer == null) {
            node.putNull("cer");
        } else {
            node.put("cer", cer);
        }
        if (wer == null) {
            node.putNull("wer");
        } else {
            node.put("wer", wer);
        }
        return node;
    }

    private static String optionalText(JsonNode request, String field) {
        JsonNode value = request.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new IllegalArgumentException(field + " must be a string.");
        }
        return value.asText();
    }

    private static String toPngDataUri(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output)) {
            throw new IOException("No PNG ImageIO writer is available for the preview.");
        }
        return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(output.toByteArray());
    }

    private static void sendJsonError(HttpExchange exchange, int status, String error) throws IOException {
        ObjectNode body = JSON.createObjectNode();
        body.put("error", error);
        sendJson(exchange, status, body);
    }

    private static void sendJson(HttpExchange exchange, int status, JsonNode body) throws IOException {
        byte[] content = JSON.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, content.length);
        exchange.getResponseBody().write(content);
    }

    private static String message(Throwable exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? "No error message was provided."
                : exception.getMessage();
    }
}

package com.example.ocr.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrWebServerTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void servesSinglePageInterface() throws Exception {
        try (OcrWebServer server = new OcrWebServer(0, null, "eng")) {
            server.start();
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create(url(server) + "/")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("Distorted Message OCR"));
            assertTrue(response.body().contains("Compare all six modes"));
        }
    }

    @Test
    void rejectsMalformedBase64WithActionableClientError() throws Exception {
        try (OcrWebServer server = new OcrWebServer(0, null, "eng")) {
            server.start();
            HttpResponse<String> response = post(server, """
                    {"imageData":"not-valid-base64%%%","mode":"RAPIDOCR","compare":false}
                    """);

            assertEquals(400, response.statusCode());
            assertTrue(response.body().contains("Invalid Base64"));
        }
    }

    @Test
    void recognizesBase64ImageAndReturnsPreviewAndSelectedMode() throws Exception {
        try (OcrWebServer server = new OcrWebServer(0, null, "eng")) {
            server.start();
            String imageData = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes());
            String body = JSON.createObjectNode()
                    .put("imageData", imageData)
                    .put("mode", "RAPIDOCR")
                    .put("compare", false)
                    .toString();

            HttpResponse<String> response = post(server, body);
            JsonNode result = JSON.readTree(response.body());

            assertEquals(200, response.statusCode());
            assertTrue(result.get("imagePreview").asText().startsWith("data:image/png;base64,"));
            assertEquals("RAPIDOCR", result.get("results").get(0).get("mode").asText());
            assertTrue(result.get("results").get(0).get("text").asText().toLowerCase().contains("hello"));
        }
    }

    @Test
    void comparesAllSixModesThroughWebApi() throws Exception {
        try (OcrWebServer server = new OcrWebServer(0, null, "eng")) {
            server.start();
            String imageData = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes());
            String body = JSON.createObjectNode()
                    .put("imageData", imageData)
                    .put("compare", true)
                    .toString();

            HttpResponse<String> response = post(server, body);
            JsonNode result = JSON.readTree(response.body());

            assertEquals(200, response.statusCode());
            assertTrue(result.get("compare").asBoolean());
            assertEquals(6, result.get("results").size());
        }
    }

    private static HttpResponse<String> post(OcrWebServer server, String body) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url(server) + "/api/ocr"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String url(OcrWebServer server) {
        return "http://127.0.0.1:" + server.port();
    }

    private static byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(600, 140, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(java.awt.Color.BLACK);
            graphics.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 32));
            graphics.drawString("Hello, please complete the task.", 20, 85);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}

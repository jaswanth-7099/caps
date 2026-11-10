package com.example.ocr;

import com.example.ocr.config.OCRMode;
import com.example.ocr.config.OCRSettings;
import com.example.ocr.input.DecodedImage;
import com.example.ocr.input.ImageInputHandler;
import com.example.ocr.output.OCRResult;
import com.example.ocr.output.ResultWriter;
import com.example.ocr.service.OCRComparisonRow;
import com.example.ocr.service.OCRComparisonService;
import com.example.ocr.service.OCRExecutionService;
import com.example.ocr.web.OcrWebServer;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            if (args.length == 0 || List.of(args).contains("--help")) {
                printUsage();
                return;
            }
            if (List.of(args).contains("--web")) {
                runWeb(args);
                return;
            }
            Arguments arguments = Arguments.parse(args);
            DecodedImage input = readInput(arguments);
            OCRSettings settings = new OCRSettings(arguments.tessdataPath, arguments.language);
            OCRExecutionService execution = new OCRExecutionService(settings);

            if (arguments.compare) {
                runComparison(arguments, input, execution);
            } else {
                OCRResult result = execution.createProcessor(arguments.mode).process(input.image());
                ResultWriter.print(result);
                if (arguments.output != null) {
                    ResultWriter.writeText(arguments.output, result);
                    System.out.println("Saved text: " + arguments.output.toAbsolutePath().normalize());
                }
                if (result.error() != null) {
                    System.exit(1);
                }
            }
        } catch (Exception exception) {
            System.err.println("OCR failed: " + exception.getClass().getSimpleName() + ": "
                    + (exception.getMessage() == null ? "No error message." : exception.getMessage()));
            System.exit(1);
        }
    }

    private static void runWeb(String[] args) throws Exception {
        int port = 8765;
        Path tessdata = null;
        String language = "eng";
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--web" -> {
                }
                case "--port" -> {
                    if (++i >= args.length) {
                        throw new IllegalArgumentException("Missing value for --port.");
                    }
                    port = Integer.parseInt(args[i]);
                    if (port < 0 || port > 65535) {
                        throw new IllegalArgumentException("--port must be between 0 and 65535.");
                    }
                }
                case "--tessdata" -> {
                    if (++i >= args.length) {
                        throw new IllegalArgumentException("Missing value for --tessdata.");
                    }
                    tessdata = Path.of(args[i]);
                }
                case "--language" -> {
                    if (++i >= args.length) {
                        throw new IllegalArgumentException("Missing value for --language.");
                    }
                    language = args[i];
                }
                default -> throw new IllegalArgumentException("Unknown web option: " + args[i]);
            }
        }
        OcrWebServer server = new OcrWebServer(port, tessdata, language);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "ocr-web-shutdown"));
        server.start();
        URI uri = URI.create("http://127.0.0.1:" + server.port() + "/");
        System.out.println("OCR web app is running locally at " + uri);
        System.out.println("Press Ctrl+C to stop it.");
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(uri);
            } catch (Exception exception) {
                System.err.println("Could not open a browser automatically: " + exception.getMessage());
                System.err.println("Open this local URL manually: " + uri);
            }
        }
    }

    private static DecodedImage readInput(Arguments arguments) throws Exception {
        ImageInputHandler inputHandler = new ImageInputHandler();
        if (arguments.image != null) {
            return inputHandler.fromImage(arguments.image);
        }
        if (arguments.base64 != null) {
            return inputHandler.fromBase64(arguments.base64);
        }
        return inputHandler.fromBase64File(arguments.base64File);
    }

    private static void runComparison(Arguments arguments, DecodedImage input,
                                      OCRExecutionService execution) throws Exception {
        String groundTruth = arguments.groundTruth == null
                ? null
                : Files.readString(arguments.groundTruth);
        OCRComparisonService comparison = new OCRComparisonService(execution);
        List<OCRComparisonRow> rows = comparison.compare(input.image(), groundTruth);
        String report = ResultWriter.comparisonReport(rows, groundTruth != null);
        System.out.print(report);
        if (arguments.report != null) {
            ResultWriter.write(arguments.report, report);
            System.out.println("Saved comparison report: " + arguments.report.toAbsolutePath().normalize());
        }
    }

    private static void printUsage() {
        System.out.println("""
                Usage:
                  --web [--port 8765] [--tessdata PATH] [--language eng]
                  --image PATH | --base64 VALUE | --base64-file PATH
                  --mode TESS4J|RAPIDOCR|OPENCV_TESS4J|OPENCV_RAPIDOCR|BOOFCV_TESS4J|IMAGEJ_TESS4J
                  [--compare] [--ground-truth PATH] [--output PATH] [--report PATH]
                  [--tessdata PATH] [--language eng]

                Exactly one input option is required. Use --compare instead of --mode to run all six modes.
                """);
    }

    private static final class Arguments {
        private Path image;
        private String base64;
        private Path base64File;
        private OCRMode mode;
        private boolean compare;
        private Path groundTruth;
        private Path output;
        private Path report;
        private Path tessdataPath;
        private String language = "eng";

        private static Arguments parse(String[] args) {
            Map<String, String> values = new java.util.HashMap<>();
            boolean compare = false;
            for (int i = 0; i < args.length; i++) {
                String key = args[i];
                if (key.equals("--compare")) {
                    if (compare) {
                        throw new IllegalArgumentException("--compare may only be specified once.");
                    }
                    compare = true;
                    continue;
                }
                if (!SetOfOptions.contains(key)) {
                    throw new IllegalArgumentException("Unknown option: " + key);
                }
                if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                    throw new IllegalArgumentException("Missing value for " + key);
                }
                if (values.putIfAbsent(key, args[++i]) != null) {
                    throw new IllegalArgumentException("Option may only be specified once: " + key);
                }
            }

            Arguments result = new Arguments();
            result.image = path(values, "--image");
            result.base64 = values.get("--base64");
            result.base64File = path(values, "--base64-file");
            int inputCount = (result.image == null ? 0 : 1) + (result.base64 == null ? 0 : 1)
                    + (result.base64File == null ? 0 : 1);
            if (inputCount != 1) {
                throw new IllegalArgumentException("Provide exactly one of --image, --base64, or --base64-file.");
            }
            result.compare = compare;
            result.mode = values.containsKey("--mode") ? OCRMode.parse(values.get("--mode")) : null;
            if (compare == (result.mode != null)) {
                throw new IllegalArgumentException("Provide either --mode for a single run or --compare, but not both.");
            }
            result.groundTruth = path(values, "--ground-truth");
            result.output = path(values, "--output");
            result.report = path(values, "--report");
            result.tessdataPath = path(values, "--tessdata");
            result.language = values.getOrDefault("--language", "eng");
            if (result.language.isBlank()) {
                throw new IllegalArgumentException("--language must not be blank.");
            }
            if (!compare && result.report != null) {
                throw new IllegalArgumentException("--report is only valid with --compare.");
            }
            if (compare && result.output != null) {
                throw new IllegalArgumentException("--output is for a single OCR result; use --report for comparison.");
            }
            return result;
        }

        private static Path path(Map<String, String> values, String option) {
            String value = values.get(option);
            return value == null ? null : Path.of(value);
        }
    }

    private static final class SetOfOptions {
        private static final java.util.Set<String> VALUES = java.util.Set.of(
                "--image", "--base64", "--base64-file", "--mode", "--ground-truth",
                "--output", "--report", "--tessdata", "--language");

        private static boolean contains(String key) {
            return VALUES.contains(key);
        }
    }
}

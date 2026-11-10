package com.example.ocr.output;

import com.example.ocr.service.OCRComparisonRow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class ResultWriter {
    private ResultWriter() {
    }

    public static void print(OCRResult result) {
        System.out.println("Mode: " + result.mode());
        System.out.println("Processing time: " + result.processingMillis() + " ms");
        System.out.println("OCR confidence: " + confidence(result.confidence()));
        if (result.error() != null) {
            System.out.println("Error: " + result.error());
            return;
        }
        System.out.println("Extracted text:");
        System.out.println(result.text());
    }

    public static void writeText(Path path, OCRResult result) throws IOException {
        if (result.error() != null) {
            throw new IOException("Cannot save text because OCR failed: " + result.error());
        }
        write(path, result.text());
    }

    public static String comparisonReport(List<OCRComparisonRow> rows, boolean hasGroundTruth) {
        StringBuilder report = new StringBuilder();
        report.append("OCR comparison").append(System.lineSeparator());
        if (hasGroundTruth) {
            report.append("Winner (lowest CER, then WER/time): ")
                    .append(rows.stream()
                            .filter(row -> row.result().error() == null && row.cer() != null)
                            .min((left, right) -> {
                                int cer = Double.compare(left.cer(), right.cer());
                                if (cer != 0) {
                                    return cer;
                                }
                                int wer = Double.compare(left.wer(), right.wer());
                                return wer != 0 ? wer
                                        : Long.compare(left.result().processingMillis(), right.result().processingMillis());
                            })
                            .map(row -> row.result().mode().name())
                            .orElse("No successful OCR result"))
                    .append(System.lineSeparator());
        } else {
            report.append("No ground truth supplied; fastest successful mode is not an accuracy winner: ")
                    .append(rows.stream()
                            .filter(row -> row.result().error() == null)
                            .min((left, right) -> Long.compare(left.result().processingMillis(),
                                    right.result().processingMillis()))
                            .map(row -> row.result().mode().name())
                            .orElse("No successful OCR result"))
                    .append(System.lineSeparator());
        }
        report.append(String.format(Locale.ROOT, "%-20s | %8s | %8s | %9s | %10s | %s%n",
                "Mode", "CER", "WER", "Time(ms)", "Confidence", "Extracted text / error"));
        report.append("-".repeat(120)).append(System.lineSeparator());
        for (OCRComparisonRow row : rows) {
            OCRResult result = row.result();
            String text = result.error() == null ? oneLine(result.text()) : "ERROR: " + oneLine(result.error());
            report.append(String.format(Locale.ROOT, "%-20s | %8s | %8s | %9d | %10s | %s%n",
                    result.mode(), metric(row.cer()), metric(row.wer()), result.processingMillis(),
                    confidence(result.confidence()), text));
        }
        return report.toString();
    }

    public static void write(Path path, String content) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(absolute, content, StandardCharsets.UTF_8);
    }

    private static String metric(Double value) {
        return value == null ? "N/A" : String.format(Locale.ROOT, "%.4f", value);
    }

    private static String confidence(Double value) {
        return value == null ? "N/A" : String.format(Locale.ROOT, "%.4f", value);
    }

    private static String oneLine(String text) {
        return text.replace("\r", "\\r").replace("\n", "\\n");
    }
}

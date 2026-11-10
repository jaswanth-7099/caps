package com.example.ocr.service;

import com.example.ocr.config.OCRMode;
import com.example.ocr.interfaces.OCRProcessor;
import com.example.ocr.output.OCRResult;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class OCRComparisonService {
    private final OCRExecutionService executionService;

    public OCRComparisonService(OCRExecutionService executionService) {
        this.executionService = executionService;
    }

    public List<OCRComparisonRow> compare(BufferedImage image, String groundTruth) {
        List<OCRComparisonRow> rows = new ArrayList<>();
        for (OCRMode mode : OCRMode.values()) {
            long started = System.nanoTime();
            String engine = switch (mode) {
                case TESS4J, OPENCV_TESS4J, BOOFCV_TESS4J, IMAGEJ_TESS4J -> "Tess4J/Tesseract";
                case RAPIDOCR, OPENCV_RAPIDOCR -> "RapidOCR4j";
            };
            try {
                OCRProcessor processor = executionService.createProcessor(mode);
                OCRResult result = processor.process(image);
                ErrorRates rates = groundTruth == null ? null : OCRMetrics.calculate(groundTruth, result.text());
                rows.add(new OCRComparisonRow(result, rates == null ? null : rates.cer(),
                        rates == null ? null : rates.wer()));
            } catch (Exception | LinkageError exception) {
                rows.add(new OCRComparisonRow(
                        OCRResult.failure(mode, engine,
                                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started),
                                exception.getClass().getSimpleName() + ": "
                                + (exception.getMessage() == null ? "No error message." : exception.getMessage())),
                        null, null));
            }
        }
        return List.copyOf(rows);
    }
}

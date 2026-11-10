package com.example.ocr.output;

import com.example.ocr.config.OCRMode;

public record OCRResult(
        OCRMode mode,
        String engine,
        long processingMillis,
        String text,
        Double confidence,
        String error) {

    public static OCRResult success(OCRMode mode, String engine, long processingMillis, String text, Double confidence) {
        return new OCRResult(mode, engine, processingMillis, text, confidence, null);
    }

    public static OCRResult failure(OCRMode mode, String engine, long processingMillis, String error) {
        return new OCRResult(mode, engine, processingMillis, "", null, error);
    }
}

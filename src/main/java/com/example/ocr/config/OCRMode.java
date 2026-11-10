package com.example.ocr.config;

import java.util.Arrays;

public enum OCRMode {
    TESS4J,
    RAPIDOCR,
    OPENCV_TESS4J,
    OPENCV_RAPIDOCR,
    BOOFCV_TESS4J,
    IMAGEJ_TESS4J;

    public static OCRMode parse(String value) {
        return Arrays.stream(values())
                .filter(mode -> mode.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown mode '" + value + "'. Choose one of: " + Arrays.toString(values())));
    }
}

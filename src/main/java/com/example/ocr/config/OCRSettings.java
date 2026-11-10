package com.example.ocr.config;

import java.nio.file.Path;

public record OCRSettings(Path tessdataPath, String language) {
    public OCRSettings {
        language = language == null || language.isBlank() ? "eng" : language;
    }
}

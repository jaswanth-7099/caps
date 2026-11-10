package com.example.ocr.service;

import com.example.ocr.output.OCRResult;

public record OCRComparisonRow(OCRResult result, Double cer, Double wer) {
}

package com.example.ocr.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OCRMetricsTest {
    @Test
    void calculatesCharacterAndWordErrorRates() {
        ErrorRates rates = OCRMetrics.calculate("hello world", "hallo word");

        assertEquals(2.0 / 11.0, rates.cer(), 1e-10);
        assertEquals(1.0, rates.wer(), 1e-10);
    }

    @Test
    void handlesEmptyGroundTruth() {
        assertEquals(0.0, OCRMetrics.calculate("", "").cer());
        assertEquals(1.0, OCRMetrics.calculate("", "text").wer());
    }
}

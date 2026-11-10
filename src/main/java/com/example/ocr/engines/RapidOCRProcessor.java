package com.example.ocr.engines;

import com.example.ocr.config.OCRMode;
import com.example.ocr.interfaces.OCRProcessor;
import com.example.ocr.output.OCRResult;
import io.github.hzkitty.RapidOCR;
import io.github.hzkitty.entity.OcrResult;
import io.github.hzkitty.entity.RecResult;

import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

public final class RapidOCRProcessor implements OCRProcessor {
    private RapidOCR rapidOCR;

    @Override
    public OCRMode mode() {
        return OCRMode.RAPIDOCR;
    }

    @Override
    public OCRResult process(BufferedImage image) throws Exception {
        long started = System.nanoTime();
        if (rapidOCR == null) {
            rapidOCR = RapidOCR.create();
        }
        OcrResult recognition = rapidOCR.run(image);
        Double confidence = recognition.getRecRes() == null || recognition.getRecRes().isEmpty()
                ? null
                : recognition.getRecRes().stream()
                .mapToDouble(RecResult::getConfidence)
                .average()
                .orElse(Double.NaN);
        if (confidence != null && !Double.isFinite(confidence)) {
            confidence = null;
        }
        return OCRResult.success(mode(), "RapidOCR4j",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started),
                recognition.getStrRes(), confidence);
    }
}

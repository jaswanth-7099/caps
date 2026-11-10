package com.example.ocr.combination;

import com.example.ocr.config.OCRMode;
import com.example.ocr.interfaces.ImagePreprocessor;
import com.example.ocr.interfaces.OCRProcessor;
import com.example.ocr.output.OCRResult;

import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

public abstract class CombinedOCRProcessor implements OCRProcessor {
    private final ImagePreprocessor preprocessor;
    private final OCRProcessor ocrProcessor;
    private final OCRMode mode;

    protected CombinedOCRProcessor(OCRMode mode, ImagePreprocessor preprocessor, OCRProcessor ocrProcessor) {
        this.mode = mode;
        this.preprocessor = preprocessor;
        this.ocrProcessor = ocrProcessor;
    }

    @Override
    public final OCRMode mode() {
        return mode;
    }

    @Override
    public final OCRResult process(BufferedImage image) throws Exception {
        long started = System.nanoTime();
        BufferedImage prepared = preprocessor.preprocess(image);
        OCRResult result = ocrProcessor.process(prepared);
        return OCRResult.success(mode, result.engine(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started),
                result.text(), result.confidence());
    }
}

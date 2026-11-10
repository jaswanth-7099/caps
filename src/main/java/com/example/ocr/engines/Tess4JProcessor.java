package com.example.ocr.engines;

import com.example.ocr.config.OCRMode;
import com.example.ocr.config.OCRSettings;
import com.example.ocr.interfaces.OCRProcessor;
import com.example.ocr.output.OCRResult;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class Tess4JProcessor implements OCRProcessor {
    private final OCRSettings settings;

    public Tess4JProcessor(OCRSettings settings) {
        this.settings = settings;
    }

    @Override
    public OCRMode mode() {
        return OCRMode.TESS4J;
    }

    @Override
    public OCRResult process(BufferedImage image) throws Exception {
        long started = System.nanoTime();
        Tesseract tesseract = new Tesseract();
        if (settings.tessdataPath() != null) {
            tesseract.setDatapath(settings.tessdataPath().toString());
        }
        tesseract.setLanguage(settings.language());
        String text = tesseract.doOCR(image);
        List<Word> words = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD);
        Double confidenceScore = null;
        if (!words.isEmpty()) {
            double average = words.stream().mapToDouble(Word::getConfidence).average().orElse(Double.NaN) / 100.0;
            if (Double.isFinite(average)) {
                confidenceScore = average;
            }
        }
        return OCRResult.success(mode(), "Tess4J/Tesseract",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started), text, confidenceScore);
    }
}

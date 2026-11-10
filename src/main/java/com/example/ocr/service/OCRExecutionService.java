package com.example.ocr.service;

import com.example.ocr.combination.BoofCVTess4J;
import com.example.ocr.combination.ImageJTess4J;
import com.example.ocr.combination.OpenCVRapidOCR;
import com.example.ocr.combination.OpenCVTess4J;
import com.example.ocr.config.OCRMode;
import com.example.ocr.config.OCRSettings;
import com.example.ocr.engines.RapidOCRProcessor;
import com.example.ocr.engines.Tess4JProcessor;
import com.example.ocr.interfaces.OCRProcessor;

public final class OCRExecutionService {
    private final OCRSettings settings;
    private RapidOCRProcessor rapidOCRProcessor;

    public OCRExecutionService(OCRSettings settings) {
        this.settings = settings;
    }

    public OCRProcessor createProcessor(OCRMode mode) {
        return switch (mode) {
            case TESS4J -> new Tess4JProcessor(settings);
            case RAPIDOCR -> rapidOCRProcessor();
            case OPENCV_TESS4J -> new OpenCVTess4J(new Tess4JProcessor(settings));
            case OPENCV_RAPIDOCR -> new OpenCVRapidOCR(rapidOCRProcessor());
            case BOOFCV_TESS4J -> new BoofCVTess4J(new Tess4JProcessor(settings));
            case IMAGEJ_TESS4J -> new ImageJTess4J(new Tess4JProcessor(settings));
        };
    }

    private RapidOCRProcessor rapidOCRProcessor() {
        if (rapidOCRProcessor == null) {
            rapidOCRProcessor = new RapidOCRProcessor();
        }
        return rapidOCRProcessor;
    }
}

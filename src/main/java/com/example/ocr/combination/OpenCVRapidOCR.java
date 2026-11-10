package com.example.ocr.combination;

import com.example.ocr.config.OCRMode;
import com.example.ocr.engines.RapidOCRProcessor;
import com.example.ocr.preprocessors.OpenCVProcessor;

public final class OpenCVRapidOCR extends CombinedOCRProcessor {
    public OpenCVRapidOCR(RapidOCRProcessor rapidOCRProcessor) {
        super(OCRMode.OPENCV_RAPIDOCR, new OpenCVProcessor(), rapidOCRProcessor);
    }
}

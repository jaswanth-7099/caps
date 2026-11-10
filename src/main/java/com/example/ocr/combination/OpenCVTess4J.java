package com.example.ocr.combination;

import com.example.ocr.config.OCRMode;
import com.example.ocr.engines.Tess4JProcessor;
import com.example.ocr.preprocessors.OpenCVProcessor;

public final class OpenCVTess4J extends CombinedOCRProcessor {
    public OpenCVTess4J(Tess4JProcessor tess4JProcessor) {
        super(OCRMode.OPENCV_TESS4J, new OpenCVProcessor(), tess4JProcessor);
    }
}

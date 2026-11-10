package com.example.ocr.combination;

import com.example.ocr.config.OCRMode;
import com.example.ocr.engines.Tess4JProcessor;
import com.example.ocr.preprocessors.ImageJProcessor;

public final class ImageJTess4J extends CombinedOCRProcessor {
    public ImageJTess4J(Tess4JProcessor tess4JProcessor) {
        super(OCRMode.IMAGEJ_TESS4J, new ImageJProcessor(), tess4JProcessor);
    }
}

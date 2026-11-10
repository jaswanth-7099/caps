package com.example.ocr.combination;

import com.example.ocr.config.OCRMode;
import com.example.ocr.engines.Tess4JProcessor;
import com.example.ocr.preprocessors.BoofCVProcessor;

public final class BoofCVTess4J extends CombinedOCRProcessor {
    public BoofCVTess4J(Tess4JProcessor tess4JProcessor) {
        super(OCRMode.BOOFCV_TESS4J, new BoofCVProcessor(), tess4JProcessor);
    }
}

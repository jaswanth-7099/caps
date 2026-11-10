package com.example.ocr.interfaces;

import com.example.ocr.config.OCRMode;
import com.example.ocr.output.OCRResult;

import java.awt.image.BufferedImage;

public interface OCRProcessor {
    OCRMode mode();

    OCRResult process(BufferedImage image) throws Exception;
}

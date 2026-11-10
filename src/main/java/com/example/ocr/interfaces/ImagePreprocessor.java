package com.example.ocr.interfaces;

import java.awt.image.BufferedImage;

public interface ImagePreprocessor {
    BufferedImage preprocess(BufferedImage image) throws Exception;
}

package com.example.ocr.preprocessors;

import com.example.ocr.interfaces.ImagePreprocessor;
import ij.ImagePlus;
import ij.plugin.ContrastEnhancer;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.image.BufferedImage;

public final class ImageJProcessor implements ImagePreprocessor {
    @Override
    public BufferedImage preprocess(BufferedImage image) {
        BufferedImage grayscale = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        var graphics = grayscale.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        ImageProcessor processor = new ByteProcessor(grayscale);
        processor.medianFilter();
        ImagePlus imagePlus = new ImagePlus("message", processor);
        new ContrastEnhancer().stretchHistogram(imagePlus, 0.35);
        return imagePlus.getBufferedImage();
    }
}

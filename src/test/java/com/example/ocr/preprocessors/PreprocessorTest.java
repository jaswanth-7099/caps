package com.example.ocr.preprocessors;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreprocessorTest {
    @Test
    void boofCvPreprocessorReturnsImageOfSameDimensions() {
        BufferedImage result = new BoofCVProcessor().preprocess(sampleImage());

        assertEquals(160, result.getWidth());
        assertEquals(60, result.getHeight());
    }

    @Test
    void imageJPreprocessorReturnsImageOfSameDimensions() {
        BufferedImage result = new ImageJProcessor().preprocess(sampleImage());

        assertEquals(160, result.getWidth());
        assertEquals(60, result.getHeight());
    }

    @Test
    void openCvPreprocessorLoadsNativeRuntimeAndReturnsImageOfSameDimensions() throws Exception {
        BufferedImage result = new OpenCVProcessor().preprocess(sampleImage());

        assertEquals(160, result.getWidth());
        assertEquals(60, result.getHeight());
    }

    private static BufferedImage sampleImage() {
        BufferedImage image = new BufferedImage(160, 60, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.drawString("Test message", 8, 35);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}

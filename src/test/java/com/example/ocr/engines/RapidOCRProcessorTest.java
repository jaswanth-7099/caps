package com.example.ocr.engines;

import com.example.ocr.combination.OpenCVRapidOCR;
import com.example.ocr.config.OCRMode;
import com.example.ocr.output.OCRResult;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RapidOCRProcessorTest {
    @Test
    void rapidOcrReturnsTextResultForGeneratedMessageImage() throws Exception {
        OCRResult result = new RapidOCRProcessor().process(sampleImage());

        assertEquals(OCRMode.RAPIDOCR, result.mode());
        assertNotNull(result.text());
        assertTrue(result.text().toLowerCase().contains("hello"), result.text());
    }

    @Test
    void openCvRapidOcrCombinationRunsBothStages() throws Exception {
        OCRResult result = new OpenCVRapidOCR(new RapidOCRProcessor()).process(sampleImage());

        assertEquals(OCRMode.OPENCV_RAPIDOCR, result.mode());
        assertNotNull(result.text());
        assertTrue(result.text().toLowerCase().contains("hello"), result.text());
    }

    private static BufferedImage sampleImage() {
        BufferedImage image = new BufferedImage(600, 140, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
            graphics.drawString("Hello, please complete the task.", 20, 85);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}

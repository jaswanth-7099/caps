package com.example.ocr.input;

import java.awt.image.BufferedImage;

public record DecodedImage(BufferedImage image, String source) {
}

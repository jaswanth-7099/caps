package com.example.ocr.preprocessors;

import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import com.example.ocr.interfaces.ImagePreprocessor;

import java.awt.image.BufferedImage;

public final class BoofCVProcessor implements ImagePreprocessor {
    @Override
    public BufferedImage preprocess(BufferedImage image) {
        GrayU8 gray = ConvertBufferedImage.convertFromSingle(image, null, GrayU8.class);
        GrayU8 smoothed = new GrayU8(gray.width, gray.height);
        BlurImageOps.gaussian(gray, smoothed, 0.8, -1, null);
        return ConvertBufferedImage.convertTo(smoothed, null, true);
    }
}

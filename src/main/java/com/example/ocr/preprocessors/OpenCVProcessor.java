package com.example.ocr.preprocessors;

import com.example.ocr.interfaces.ImagePreprocessor;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.photo.Photo;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OpenCVProcessor implements ImagePreprocessor {
    private static volatile boolean loaded;

    @Override
    public BufferedImage preprocess(BufferedImage image) throws IOException {
        ensureLoaded();
        Mat encoded = toMat(image);
        Mat gray = new Mat();
        Mat denoised = new Mat();
        Mat enhanced = new Mat();
        Mat edges = new Mat();
        Mat lines = new Mat();
        Mat lineMask = new Mat();
        Mat repaired = new Mat();
        Mat rotated = new Mat();
        Mat output = new Mat();
        CLAHE clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));
        try {
            Imgproc.cvtColor(encoded, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.medianBlur(gray, denoised, 3);
            clahe.apply(denoised, enhanced);

            Imgproc.Canny(enhanced, edges, 50, 150);
            int minimumLength = Math.max(30, (int) (image.getWidth() * 0.25));
            Imgproc.HoughLinesP(edges, lines, 1, Math.PI / 180,
                    Math.max(30, image.getWidth() / 8), minimumLength, 12);
            List<Double> nearHorizontalAngles = new ArrayList<>();
            lineMask = Mat.zeros(enhanced.size(), CvType.CV_8UC1);
            for (int i = 0; i < lines.rows(); i++) {
                double[] line = lines.get(i, 0);
                if (line == null || line.length < 4) {
                    continue;
                }
                double dx = line[2] - line[0];
                double dy = line[3] - line[1];
                double angle = Math.toDegrees(Math.atan2(dy, dx));
                double length = Math.hypot(dx, dy);
                if (Math.abs(angle) < 10.0) {
                    nearHorizontalAngles.add(angle);
                }
                if (length >= image.getWidth() * 0.45 && Math.abs(angle) < 1.2) {
                    Imgproc.line(lineMask, new Point(line[0], line[1]), new Point(line[2], line[3]),
                            new org.opencv.core.Scalar(255), 3);
                }
            }

            Mat source = enhanced;
            if (Core.countNonZero(lineMask) > 0) {
                Photo.inpaint(enhanced, lineMask, repaired, 2.0, Photo.INPAINT_TELEA);
                source = repaired;
            }

            double skew = robustMedian(nearHorizontalAngles);
            if (nearHorizontalAngles.size() >= 3 && Math.abs(skew) >= 0.6 && Math.abs(skew) <= 8.0) {
                Mat transform = Imgproc.getRotationMatrix2D(
                        new Point(source.cols() / 2.0, source.rows() / 2.0), skew, 1.0);
                Imgproc.warpAffine(source, rotated, transform, source.size(),
                        Imgproc.INTER_CUBIC, Core.BORDER_REPLICATE, new org.opencv.core.Scalar(255));
                transform.release();
                source = rotated;
            }
            source.copyTo(output);
            return toBufferedImage(output);
        } finally {
            encoded.release();
            gray.release();
            denoised.release();
            enhanced.release();
            edges.release();
            lines.release();
            lineMask.release();
            repaired.release();
            rotated.release();
            output.release();
            clahe.collectGarbage();
        }
    }

    private static double robustMedian(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        Collections.sort(values);
        return values.get(values.size() / 2);
    }

    private static synchronized void ensureLoaded() {
        if (!loaded) {
            OpenCV.loadShared();
            loaded = true;
        }
    }

    private static Mat toMat(BufferedImage image) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", bytes)) {
            throw new IOException("No PNG ImageIO writer is available.");
        }
        MatOfByte encoded = new MatOfByte(bytes.toByteArray());
        try {
            Mat decoded = Imgcodecs.imdecode(encoded, Imgcodecs.IMREAD_COLOR);
            if (decoded.empty()) {
                decoded.release();
                throw new IOException("OpenCV could not decode the input image.");
            }
            return decoded;
        } finally {
            encoded.release();
        }
    }

    private static BufferedImage toBufferedImage(Mat mat) throws IOException {
        MatOfByte encoded = new MatOfByte();
        try {
            if (!Imgcodecs.imencode(".png", mat, encoded)) {
                throw new IOException("OpenCV could not encode the processed image.");
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(encoded.toArray()));
            if (image == null) {
                throw new IOException("ImageIO could not decode the processed OpenCV image.");
            }
            return image;
        } finally {
            encoded.release();
        }
    }
}

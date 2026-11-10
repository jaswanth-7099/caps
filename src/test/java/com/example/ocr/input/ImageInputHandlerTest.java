package com.example.ocr.input;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageInputHandlerTest {
    @TempDir
    Path tempDir;

    @Test
    void readsPngImageFile() throws Exception {
        Path imagePath = tempDir.resolve("message.png");
        ImageIO.write(testImage(), "png", imagePath.toFile());

        DecodedImage decoded = new ImageInputHandler().fromImage(imagePath);

        assertEquals(24, decoded.image().getWidth());
        assertEquals(16, decoded.image().getHeight());
    }

    @Test
    void readsPlainBase64Png() throws Exception {
        String base64 = Base64.getEncoder().encodeToString(pngBytes());

        DecodedImage decoded = new ImageInputHandler().fromBase64(base64);

        assertEquals(24, decoded.image().getWidth());
    }

    @Test
    void readsPngDataUri() throws Exception {
        String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes());

        DecodedImage decoded = new ImageInputHandler().fromBase64(dataUri);

        assertEquals(16, decoded.image().getHeight());
    }

    @Test
    void readsJpegDataUri() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(testImage(), "jpeg", output);
        String dataUri = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(output.toByteArray());

        DecodedImage decoded = new ImageInputHandler().fromBase64(dataUri);

        assertEquals(24, decoded.image().getWidth());
    }

    @Test
    void readsBase64TextFile() throws Exception {
        Path textFile = tempDir.resolve("message.txt");
        Files.writeString(textFile, Base64.getEncoder().encodeToString(pngBytes()), StandardCharsets.UTF_8);

        DecodedImage decoded = new ImageInputHandler().fromBase64File(textFile);

        assertEquals(24, decoded.image().getWidth());
    }

    @Test
    void rejectsMalformedBase64AndUnsupportedFileExtension() throws Exception {
        assertThrows(IOException.class, () -> new ImageInputHandler().fromBase64("%%%"));
        Path unsupported = tempDir.resolve("message.gif");
        Files.write(unsupported, pngBytes());
        assertThrows(IOException.class, () -> new ImageInputHandler().fromImage(unsupported));
    }

    private static BufferedImage testImage() {
        return new BufferedImage(24, 16, BufferedImage.TYPE_INT_RGB);
    }

    private static byte[] pngBytes() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(testImage(), "png", output);
        return output.toByteArray();
    }
}

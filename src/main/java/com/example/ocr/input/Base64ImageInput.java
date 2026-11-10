package com.example.ocr.input;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public final class Base64ImageInput {
    private static final String PNG_DATA_URI_PREFIX = "data:image/png;base64,";

    private Base64ImageInput() {
    }

    public static DecodedImage read(Path path) throws IOException {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IOException("Base64 text file does not exist or is not a regular file: " + path);
        }
        return decode(Files.readString(path, StandardCharsets.UTF_8), path.toAbsolutePath().normalize().toString());
    }

    public static DecodedImage decode(String input, String source) throws IOException {
        if (input == null || input.isBlank()) {
            throw new IOException("Base64 image input is empty.");
        }
        String payload = input.strip();
        if (payload.regionMatches(true, 0, PNG_DATA_URI_PREFIX, 0, PNG_DATA_URI_PREFIX.length())) {
            payload = payload.substring(PNG_DATA_URI_PREFIX.length());
        }
        payload = payload.replaceAll("\\s", "");

        final byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid Base64 image input: " + exception.getMessage(), exception);
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new IOException("Base64 data is not a supported decodable image.");
        }
        return new DecodedImage(image, source);
    }
}

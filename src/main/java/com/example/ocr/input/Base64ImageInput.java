package com.example.ocr.input;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Base64ImageInput {
    private static final Pattern IMAGE_DATA_URI = Pattern.compile(
            "^data:image/(?:png|jpe?g|bmp);base64,", Pattern.CASE_INSENSITIVE);

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
        Matcher dataUri = IMAGE_DATA_URI.matcher(payload);
        if (dataUri.find()) {
            payload = payload.substring(dataUri.end());
        } else if (payload.regionMatches(true, 0, "data:", 0, "data:".length())) {
            throw new IOException("Unsupported image data URI. Supported types: PNG, JPG, JPEG, BMP.");
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

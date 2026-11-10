package com.example.ocr.input;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public final class FileImageInput {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "bmp");

    private FileImageInput() {
    }

    public static DecodedImage read(Path path) throws IOException {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IOException("Image file does not exist or is not a regular file: " + path);
        }
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String extension = dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IOException("Unsupported image extension: " + (extension.isEmpty() ? "(none)" : extension)
                    + ". Supported: PNG, JPG, JPEG, BMP.");
        }
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IOException("ImageIO could not decode image file: " + path);
        }
        return new DecodedImage(image, path.toAbsolutePath().normalize().toString());
    }
}

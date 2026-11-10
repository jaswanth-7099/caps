package com.example.ocr.input;

import java.io.IOException;
import java.nio.file.Path;

public final class ImageInputHandler {
    public DecodedImage fromImage(Path path) throws IOException {
        return FileImageInput.read(path);
    }

    public DecodedImage fromBase64(String encoded) throws IOException {
        return Base64ImageInput.decode(encoded, "Base64 input");
    }

    public DecodedImage fromBase64File(Path path) throws IOException {
        return Base64ImageInput.read(path);
    }
}

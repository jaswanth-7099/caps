# Distorted Message OCR POC

A standalone, local Java 21/Maven application for extracting text from distorted chat-message images. It includes a browser-based local UI and CLI, and evaluates Tess4J/Tesseract, RapidOCR4j, and optional OpenCV, BoofCV, or ImageJ preprocessing. The six modes are independent: selecting one mode constructs and runs only that mode's preprocessor and OCR engine.

This project returns recognized text; it does not treat an enhanced image as the result. Preprocessing is heuristic. No OCR engine can reliably reconstruct characters that have been fully obscured or destroyed.

## Requirements

- 64-bit Windows (the same Java code can also run on other platforms with suitable native runtimes)
- Java 21
- Maven 3.9+
- Internet access on first Maven run to retrieve dependencies
- Tesseract language data for Tess4J (English by default)
- Microsoft Visual C++ v14 Redistributable for Tess4J's Windows native binaries

Install a Java 21 JDK from [Adoptium](https://adoptium.net/temurin/releases/?version=21) and Maven from the [Apache Maven download page](https://maven.apache.org/download.cgi). Add their `bin` folders to `PATH`, open a new terminal, and confirm:

```powershell
java -version
mvn -version
```

Install the current supported [Microsoft Visual C++ Redistributable](https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist?view=msvc-170) for Windows x64 before running Tess4J modes. RapidOCR4j uses packaged ONNX Runtime and OpenCV native binaries and does not require Python.

## Browser UI

Start the app from the project directory:

```powershell
mvn exec:java "-Dexec.args=--web --tessdata C:/tools/tesseract/tessdata"
```

In Eclipse, run `com.example.ocr.Main` as a Java Application and set **Program arguments** to:

```text
--web --tessdata "C:\path\to\tessdata"
-------------------------------------------
In Eclipse, run com.example.ocr.Main with --web --tessdata "D:\caps\tessdata"; open http://127.0.0.1:8765/ if it does not open automatically.
```
The app prints its local URL (`http://127.0.0.1:8765/`) and opens it in your default browser when possible. The server binds to loopback only; OCR is done on your computer. Use Ctrl+C in the terminal or stop the Eclipse launch to stop the server.

On the page, drag/drop or browse for one PNG/JPG/JPEG/BMP image or a Base64 `.txt` file, or paste Base64/data URI into the text box. Choose one OCR mode or check **Compare all six modes**, then select **Run OCR**. The image preview and extracted text appear together. Optionally enter ground-truth text to see CER/WER. Each successful result can be saved as a `.txt` file. In comparison mode, errors for individual modes appear in their result cards and do not hide other results.

Use `--port 8765` to change the local port and `--language eng` to set the default Tesseract language. The language field in the page can override that default. The `--tessdata` value must be the directory containing the trained data for Tess4J modes.

## Build and run

From this directory:

```powershell
mvn test
mvn package
mvn exec:java "-Dexec.args=--image input/message.png --mode TESS4J"
```

In Windows CMD, quote the argument property as well:

```bat
mvn test
mvn package
mvn exec:java "-Dexec.args=--image input\message.png --mode TESS4J"
```

Maven's `exec:java` goal is used (not a lifecycle phase named `exec`). All supported image paths include PNG, JPG/JPEG, and BMP.

## Run a mode

PowerShell:

```powershell
mvn exec:java "-Dexec.args=--image input/message.png --mode TESS4J"
mvn exec:java "-Dexec.args=--image input/message.png --mode RAPIDOCR"
mvn exec:java "-Dexec.args=--image input/message.png --mode OPENCV_TESS4J"
mvn exec:java "-Dexec.args=--image input/message.png --mode OPENCV_RAPIDOCR"
mvn exec:java "-Dexec.args=--image input/message.png --mode BOOFCV_TESS4J"
mvn exec:java "-Dexec.args=--image input/message.png --mode IMAGEJ_TESS4J"
```

Equivalent Windows CMD examples:

```bat
mvn exec:java "-Dexec.args=--image input\message.png --mode TESS4J"
mvn exec:java "-Dexec.args=--image input\message.png --mode RAPIDOCR"
mvn exec:java "-Dexec.args=--image input\message.png --mode OPENCV_TESS4J"
mvn exec:java "-Dexec.args=--image input\message.png --mode OPENCV_RAPIDOCR"
mvn exec:java "-Dexec.args=--image input\message.png --mode BOOFCV_TESS4J"
mvn exec:java "-Dexec.args=--image input\message.png --mode IMAGEJ_TESS4J"
```

The application prints the mode, elapsed time, confidence when available, and text. Save recognized text with `--output output/message.txt`.

### Tesseract data and language

Tess4J uses Tesseract through its Java/native dependencies. Download the required trained-data file, for example `eng.traineddata`, from the official [tessdata repository](https://github.com/tesseract-ocr/tessdata), and put it in a local `tessdata` directory. Pass that directory (the parent of `eng.traineddata`) with `--tessdata`:

```powershell
mvn exec:java "-Dexec.args=--image input/message.png --mode TESS4J --tessdata C:/tools/tesseract/tessdata --language eng"
```

No separate Tesseract installation is normally needed on Windows because Tess4J resolves its supported native binaries through Maven. Native support is architecture-dependent; use 64-bit Java on 64-bit Windows. Tesseract trained data is a separate download and must match the selected language. Tess4J modes fail with the original error if the requested data is unavailable.

### Base64 input

`--base64` accepts plain Base64 or a `data:image/png;base64,` URI. `--base64-file` reads either form from a UTF-8 text file:

```powershell
mvn exec:java "-Dexec.args=--base64 iVBORw0KGgo... --mode RAPIDOCR"
mvn exec:java "-Dexec.args=--base64-file input/message-base64.txt --mode OPENCV_TESS4J --tessdata C:/tools/tesseract/tessdata"
```

CMD quoting:

```bat
mvn exec:java "-Dexec.args=--base64 iVBORw0KGgo... --mode RAPIDOCR"
mvn exec:java "-Dexec.args=--base64-file input\message-base64.txt --mode OPENCV_TESS4J --tessdata C:\tools\tesseract\tessdata"
```

For long payloads or data containing shell-special characters, use `--base64-file`. Exactly one of `--image`, `--base64`, or `--base64-file` is required.

All six modes also accept the same Base64 text file:

PowerShell:

```powershell
mvn exec:java "-Dexec.args=--base64-file input/message-base64.txt --mode TESS4J --tessdata C:/tools/tesseract/tessdata"
mvn exec:java "-Dexec.args=--base64-file input/message-base64.txt --mode RAPIDOCR"
mvn exec:java "-Dexec.args=--base64-file input/message-base64.txt --mode OPENCV_TESS4J --tessdata C:/tools/tesseract/tessdata"
mvn exec:java "-Dexec.args=--base64-file input/message-base64.txt --mode OPENCV_RAPIDOCR"
mvn exec:java "-Dexec.args=--base64-file input/message-base64.txt --mode BOOFCV_TESS4J --tessdata C:/tools/tesseract/tessdata"
mvn exec:java "-Dexec.args=--base64-file input/message-base64.txt --mode IMAGEJ_TESS4J --tessdata C:/tools/tesseract/tessdata"
```

Windows CMD:

```bat
mvn exec:java "-Dexec.args=--base64-file input\message-base64.txt --mode TESS4J --tessdata C:\tools\tesseract\tessdata"
mvn exec:java "-Dexec.args=--base64-file input\message-base64.txt --mode RAPIDOCR"
mvn exec:java "-Dexec.args=--base64-file input\message-base64.txt --mode OPENCV_TESS4J --tessdata C:\tools\tesseract\tessdata"
mvn exec:java "-Dexec.args=--base64-file input\message-base64.txt --mode OPENCV_RAPIDOCR"
mvn exec:java "-Dexec.args=--base64-file input\message-base64.txt --mode BOOFCV_TESS4J --tessdata C:\tools\tesseract\tessdata"
mvn exec:java "-Dexec.args=--base64-file input\message-base64.txt --mode IMAGEJ_TESS4J --tessdata C:\tools\tesseract\tessdata"
```

### Compare all six modes

```powershell
mvn exec:java "-Dexec.args=--image input/message.png --compare --ground-truth input/message.txt --report output/comparison.txt --tessdata C:/tools/tesseract/tessdata"
mvn exec:java "-Dexec.args=--base64-file input/message-base64.txt --compare --ground-truth input/message.txt --tessdata C:/tools/tesseract/tessdata"
```

CMD:

```bat
mvn exec:java "-Dexec.args=--image input\message.png --compare --ground-truth input\message.txt --report output\comparison.txt --tessdata C:\tools\tesseract\tessdata"
```

Comparison continues if one engine fails and reports that error beside the other results. With ground truth, the winner is the successful mode with the lowest CER (ties broken by WER, then elapsed time). Without ground truth, OCR accuracy cannot be ranked from confidence alone; the report calls out the fastest mode instead. Confidence values are not calibrated across engines.

## Preprocessing and limitations

- **OpenCV** converts to grayscale, applies median denoising and CLAHE, estimates modest skew from line segments, and attempts inpainting only for very long, nearly horizontal detected lines. The line heuristic can also remove character strokes, so it is intentionally conservative.
- **BoofCV** converts to grayscale and applies a small Gaussian blur.
- **ImageJ** converts to 8-bit grayscale, median-filters, and stretches histogram contrast.
- No transformation is run in the direct `TESS4J` and `RAPIDOCR` modes. Adaptive thresholding and aggressive line removal are not forced across images because they can erase thin glyph strokes.

These are independent experiments, not a promise that preprocessing improves each image. Review outputs against ground truth before selecting a production pipeline.

## Libraries, APIs, licensing, and model data

Coordinates and versions are pinned in `pom.xml`; versions were checked against Maven Central and the projects' published metadata:

| Library | Maven coordinate | API use | License / local data |
|---|---|---|---|
| Library | Maven coordinate | API use | License / local data |
|---|---|---|---|
| [Tess4J](https://github.com/nguyenq/tess4j) | `net.sourceforge.tess4j:tess4j:5.16.0` | `net.sourceforge.tess4j.Tesseract#doOCR(BufferedImage)` and word-level confidence via `getWords(BufferedImage, RIL_WORD)` | Apache-2.0; Tesseract/leptonica native code and JNA are resolved from Maven. Trained data is separately downloaded; the official `tessdata` repository is Apache-2.0. |
| [RapidOCR4j](https://github.com/hzkitty/RapidOCR4j) | `io.github.hzkitty:rapidocr4j:1.0.3` | `io.github.hzkitty.RapidOCR#create()` and `run(BufferedImage)` | Apache-2.0 project. Its packaged PP-OCRv4 detection/recognition and orientation models originate from [PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR), which is Apache-2.0. ONNX Runtime is MIT and OpenCV is Apache-2.0. Models ship in the RapidOCR4j artifact; no Python or separate model download is required. |
| [OpenCV](https://github.com/opencv/opencv) / [OpenPnP packaging](https://github.com/openpnp/opencv) | `org.openpnp:opencv:4.6.0-0` | Java API `org.opencv.*`; native library loaded locally/shared by the OpenPnP loader | OpenCV 4.5+ is Apache-2.0. The dependency bundles supported platform native files. Version matches RapidOCR4j's OpenCV dependency. |
| [BoofCV](https://github.com/lessthanoptimal/BoofCV) | `org.boofcv:boofcv-all:1.2.2` | `boofcv.io.image.ConvertBufferedImage`, `GrayU8`, and `BlurImageOps` | Apache-2.0; no trained model or native library required. |
| [ImageJ 1.x](https://github.com/imagej/ImageJ) | `net.imagej:ij:1.54p` | `ij.ImagePlus`, `ij.process.ByteProcessor`, and `ij.plugin.ContrastEnhancer` | ImageJ is public domain; no model or native library required. |

These are the direct library/project licenses and runtime/model notes; a company should also review the full transitive dependency and model notices for its distribution use. Model provenance and licenses can be updated by upstream separately from the Java wrapper.

## Tests

`mvn test` exercises PNG-file decoding, plain Base64 and data-URI PNG input, Base64 text-file input, rejection of malformed/unsupported image input, and CER/WER calculations. It does not need OCR language data or initialize native OCR models.

## Layout

```text
src/main/java/com/example/ocr/
  Main.java
  config/                    OCR modes and runtime settings
  input/                     shared file/Base64 image decoding
  interfaces/                preprocessing and execution interfaces
  engines/                   Tess4J and RapidOCR4j adapters
  preprocessors/             OpenCV, BoofCV, and ImageJ adapters
  combination/               four explicit preprocessor + OCR modes
  service/                   mode selection, comparison, and edit rates
  output/                    result and comparison formatting
src/test/java/com/example/ocr/
  input/                     image decoding tests
  service/                   CER/WER tests
```

## Help

Run without arguments or with `--help` to display CLI syntax. The application is fully local after dependencies and (for Tess4J) trained data have been downloaded.

package com.vectorconvert.service;

import com.vectorconvert.model.ColorMode;
import com.vectorconvert.model.ConversionConfig;
import com.vectorconvert.model.DetailLevel;
import com.vectorconvert.model.ImageItem;
import com.vectorconvert.service.ColorQuantizer.RgbColor;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class VectorizationService {

    private final File potraceExecutable;

    public VectorizationService() {
        this.potraceExecutable = resolvePotraceExecutable();
    }

    public VectorizationService(File potraceExecutable) {
        this.potraceExecutable = potraceExecutable;
    }


    private File resolvePotraceExecutable() {
        String userDir = System.getProperty("user.dir");
        String[] candidatePaths = {
            "tools/potrace.exe",
            "VectorConvert/tools/potrace.exe",
            "../tools/potrace.exe",
            userDir + File.separator + "tools" + File.separator + "potrace.exe",
            userDir + File.separator + "VectorConvert" + File.separator + "tools" + File.separator + "potrace.exe",
            "tools/potrace",
            "VectorConvert/tools/potrace",
            userDir + File.separator + "tools" + File.separator + "potrace"
        };

        for (String path : candidatePaths) {
            File f = new File(path);
            if (f.exists() && f.canExecute()) {
                return f;
            }
        }

        return null;
    }

    public boolean isToolAvailable() {
        return potraceExecutable != null && potraceExecutable.exists();
    }

    public File getPotraceExecutable() {
        return potraceExecutable;
    }


    public int detectColors(ImageItem item) throws IOException {
        if (item == null) return 2;
        Image image = item.getFxImage();
        if (image == null || image.isError() || image.getPixelReader() == null) {
            throw new IOException("Unable to decode image using JavaFX: " + item.getFileName());
        }
        int detected = ColorQuantizer.detectDistinctColorCount(image);
        item.setDetectedColorCount(detected);
        return detected;
    }

    //Use Potrace to build SVG
    public File convertToSvg(ImageItem item, ConversionConfig config, File outputDirectory) throws Exception {
        if (!isToolAvailable()) {
            throw new IllegalStateException(
                    "Potrace CLI tool not found. Please ensure potrace.exe is located in the 'tools/' directory."
            );
        }

        if (item == null) {
            throw new IllegalArgumentException("ImageItem must not be null.");
        }

        Image fxImage = item.getFxImage();
        if (fxImage == null || fxImage.isError() || fxImage.getPixelReader() == null) {
            throw new IOException("Corrupted or unreadable image file: " + item.getFileName());
        }

        if (outputDirectory == null) {
            outputDirectory = item.getOriginalFile() != null ? item.getOriginalFile().getParentFile() : new File(".");
        }
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        int width = (int) fxImage.getWidth();
        int height = (int) fxImage.getHeight();
        if (width <= 0 || height <= 0) {
            throw new IOException("Image has invalid dimensions: " + width + "x" + height);
        }


        int k;
        if (config.getColorMode() == ColorMode.UNLIMITED) {
            k = 16;
        } else {
            k = Math.max(2, Math.min(5, config.getCustomColorCount()));
        }


        List<RgbColor> palette = ColorQuantizer.extractPalette(fxImage, k);
        int bgIndex = ColorQuantizer.findBackgroundColorIndex(fxImage, palette);


        File tempDir = Files.createTempDirectory("vector_convert_").toFile();
        try {
            List<String> svgLayers = new ArrayList<>();
            String viewBox = "0 0 " + width + " " + height;
            String transform = "";

            for (int i = 0; i < palette.size(); i++) {

                if (config.isRemoveBackground() && i == bgIndex) {
                    continue;
                }

                RgbColor color = palette.get(i);
                File maskPbm = new File(tempDir, "mask_" + i + ".pbm");
                File layerSvg = new File(tempDir, "layer_" + i + ".svg");

                // PixelReader
                generateBinaryMaskPbm(fxImage, palette, i, maskPbm);


                runPotrace(maskPbm, layerSvg, config.getDetailLevel());

                if (layerSvg.exists() && layerSvg.length() > 0) {
                    String svgContent = Files.readString(layerSvg.toPath(), StandardCharsets.UTF_8);


                    if (transform.isEmpty()) {
                        Matcher transMatcher = Pattern.compile("<g transform=\"([^\"]+)\"").matcher(svgContent);
                        if (transMatcher.find()) {
                            transform = transMatcher.group(1);
                        }
                    }


                    List<String> pathElements = extractPaths(svgContent, color.toHex());
                    svgLayers.addAll(pathElements);
                }
            }


            String bgHex = palette.get(bgIndex).toHex();
            String finalSvg = assembleCompositeSvg(width, height, viewBox, transform, svgLayers, config.isRemoveBackground(), bgHex);


            String baseName = getBaseName(item.getFileName());
            File outputFile = new File(outputDirectory, baseName + ".svg");
            Files.writeString(outputFile.toPath(), finalSvg, StandardCharsets.UTF_8);


            item.setVectorizedSvgFile(outputFile);
            item.setSvgContent(finalSvg);

            return outputFile;
        } finally {
            deleteRecursively(tempDir);
        }
    }


    private void generateBinaryMaskPbm(Image image, List<RgbColor> palette, int targetColorIndex, File outputFile) throws IOException {
        PixelReader reader = image.getPixelReader();
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int rowBytes = (width + 7) / 8;

        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {

            String header = "P4\n" + width + " " + height + "\n";
            out.write(header.getBytes(StandardCharsets.US_ASCII));

            byte[] rowBuffer = new byte[rowBytes];

            for (int y = 0; y < height; y++) {
                Arrays.fill(rowBuffer, (byte) 0);
                for (int x = 0; x < width; x++) {
                    int argb = reader.getArgb(x, y);
                    int alpha = (argb >> 24) & 0xFF;
                    if (alpha < 64) {
                        continue;
                    }

                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    RgbColor pixel = new RgbColor(r, g, b);

                    if (ColorQuantizer.findClosestColorIndex(pixel, palette) == targetColorIndex) {

                        int byteIndex = x / 8;
                        int bitIndex = 7 - (x % 8);
                        rowBuffer[byteIndex] |= (byte) (1 << bitIndex);
                    }
                }
                out.write(rowBuffer);
            }
        }
    }


    private void runPotrace(File inputPbm, File outputSvg, DetailLevel detailLevel) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(potraceExecutable.getAbsolutePath());
        command.add("-s");
        command.add("--flat");
        command.add("-o");
        command.add(outputSvg.getAbsolutePath());

        //enum use
        switch (detailLevel) {
            case LOW:
                command.add("--alphamax"); command.add("0.0");
                command.add("--opttolerance"); command.add("0.4");
                command.add("--turdsize"); command.add("8");
                break;
            case HIGH:
                command.add("--alphamax"); command.add("1.33");
                command.add("--opttolerance"); command.add("0.05");
                command.add("--turdsize"); command.add("1");
                break;
            case MEDIUM:
            default:
                command.add("--alphamax"); command.add("1.0");
                command.add("--opttolerance"); command.add("0.2");
                command.add("--turdsize"); command.add("3");
                break;
        }

        command.add(inputPbm.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder errOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errOutput.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Potrace error (exit code " + exitCode + "): " + errOutput);
        }
    }


    private List<String> extractPaths(String svgContent, String hexColor) {
        List<String> paths = new ArrayList<>();
        Matcher matcher = Pattern.compile("<path\\s+d=\"([^\"]+)\"[^>]*/>").matcher(svgContent);
        while (matcher.find()) {
            String d = matcher.group(1);
            paths.add(String.format("  <path d=\"%s\" fill=\"%s\" stroke=\"none\" />", d, hexColor));
        }
        return paths;
    }

    private String assembleCompositeSvg(int width, int height, String viewBox, String transform, 
                                        List<String> paths, boolean removeBackground, String bgHex) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append(String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"%d\" height=\"%d\" viewBox=\"%s\">\n",
                width, height, viewBox));


        if (!removeBackground && bgHex != null && !bgHex.isEmpty()) {
            sb.append(String.format("  <rect width=\"100%%\" height=\"100%%\" fill=\"%s\" />\n", bgHex));
        }

        if (!transform.isEmpty()) {
            sb.append(String.format(" <g transform=\"%s\">\n", transform));
        } else {

            sb.append(String.format(" <g transform=\"translate(0.0,%d.0) scale(0.1,-0.1)\">\n", height));
        }

        for (String path : paths) {
            sb.append(path).append("\n");
        }

        sb.append(" </g>\n");
        sb.append("</svg>\n");
        return sb.toString();
    }

    private String getBaseName(String filename) {
        if (filename == null) return "output";
        int dotIdx = filename.lastIndexOf('.');
        return dotIdx > 0 ? filename.substring(0, dotIdx) : filename;
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}

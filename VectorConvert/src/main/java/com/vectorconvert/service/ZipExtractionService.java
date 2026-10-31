package com.vectorconvert.service;

import com.vectorconvert.util.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class ZipExtractionService {


    public List<File> extractImages(File zipFile) throws IOException {
        if (zipFile == null || !zipFile.exists() || !zipFile.isFile()) {
            throw new FileNotFoundException("ZIP archive file not found: " + (zipFile != null ? zipFile.getName() : "null"));
        }

        Path tempDir = Files.createTempDirectory("vector_zip_extract_");
        File destDirectory = tempDir.toFile();
        destDirectory.deleteOnExit();

        List<File> extractedImages = new ArrayList<>();
        String canonicalDestDirPath = destDirectory.getCanonicalPath();

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                File destinationFile = new File(destDirectory, entry.getName());


                String canonicalDestFile = destinationFile.getCanonicalPath();
                if (!canonicalDestFile.startsWith(canonicalDestDirPath + File.separator)) {
                    throw new SecurityException("Security risk detected: Zip entry traversal blocked (" + entry.getName() + ")");
                }

                // Filter .jpg, .jpeg, .png
                if (FileUtils.isImageFile(destinationFile)) {
                    File parent = destinationFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(destinationFile))) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                    destinationFile.deleteOnExit();
                    extractedImages.add(destinationFile);
                }

                zis.closeEntry();
            }
        }

        if (extractedImages.isEmpty()) {
            throw new IOException("The ZIP archive '" + zipFile.getName() + "' contains no permissible image files (.jpg or .png).");
        }

        // Sort images alphabetically
        extractedImages.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        return extractedImages;
    }
}

package com.vectorconvert;

import com.vectorconvert.service.ZipExtractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;


public class ZipExtractionServiceTest {

    private ZipExtractionService zipService;

    @BeforeEach
    public void setUp() {
        zipService = new ZipExtractionService();
    }


    private File createTestZip(String zipName, String[][] entries) throws IOException {
        File zipFile = File.createTempFile(zipName, ".zip");
        zipFile.deleteOnExit();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (String[] entry : entries) {
                String path = entry[0];
                byte[] content = entry[1].getBytes(StandardCharsets.UTF_8);

                zos.putNextEntry(new ZipEntry(path));
                zos.write(content);
                zos.closeEntry();
            }
        }
        return zipFile;
    }

    @Test
    public void testExtractImagesFromValidZip() throws Exception {
        String[][] entries = {
                {"photo1.jpg", "fake-jpg-content"},
                {"image2.png", "fake-png-content"},
                {"nested/graphic.jpeg", "fake-jpeg-content"},
                {"readme.txt", "text-file-to-ignore"},
                {"nested/notes.md", "markdown-to-ignore"}
        };
        File zip = createTestZip("valid_bundle", entries);

        List<File> extracted = zipService.extractImages(zip);

        assertNotNull(extracted);
        assertEquals(3, extracted.size(), "Should extract exactly the 3 image files and ignore txt/md");
        assertTrue(extracted.stream().anyMatch(f -> f.getName().equalsIgnoreCase("photo1.jpg")));
        assertTrue(extracted.stream().anyMatch(f -> f.getName().equalsIgnoreCase("image2.png")));
        assertTrue(extracted.stream().anyMatch(f -> f.getName().equalsIgnoreCase("graphic.jpeg")));
    }

    @Test
    public void testZipWithNoImagesThrowsException() throws Exception {
        String[][] entries = {
                {"document.pdf", "pdf-content"},
                {"data.json", "{}"},
                {"script.py", "print('hello')"}
        };
        File zip = createTestZip("no_images", entries);

        IOException ex = assertThrows(IOException.class, () -> zipService.extractImages(zip));
        assertTrue(ex.getMessage().contains("no permissible image files"),
                "Should throw informative error when zip contains no valid images");
    }

    @Test
    public void testNonExistentZipThrowsException() {
        File nonExistent = new File("non_existent_folder_xyz/missing.zip");
        assertThrows(IOException.class, () -> zipService.extractImages(nonExistent));
    }
}

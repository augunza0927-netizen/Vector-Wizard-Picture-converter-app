package com.vectorconvert;

import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class ToolingVerificationTest {

    @Test
    public void testPotraceBinaryExecution() throws Exception {
        File potraceExe = new File("tools/potrace.exe");
        if (!potraceExe.exists()) {

            potraceExe = new File(System.getProperty("user.dir"), "tools/potrace.exe");
        }

        assertTrue(potraceExe.exists(), "potrace.exe should exist in tools directory");

        ProcessBuilder pb = new ProcessBuilder(potraceExe.getAbsolutePath(), "--version");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        System.out.println("Potrace test output:\n" + output);

        assertTrue(exitCode == 0, "Potrace should exit with code 0");
        assertTrue(output.toString().contains("potrace"), "Output should contain 'potrace'");
    }
}

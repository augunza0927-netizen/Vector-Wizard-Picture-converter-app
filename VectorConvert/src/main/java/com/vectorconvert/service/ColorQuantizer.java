package com.vectorconvert.service;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import java.util.*;

public class ColorQuantizer {


    public static class RgbColor {
        private final int r, g, b;

        public RgbColor(int r, int g, int b) {
            this.r = Math.max(0, Math.min(255, r));
            this.g = Math.max(0, Math.min(255, g));
            this.b = Math.max(0, Math.min(255, b));
        }

        public RgbColor(int rgb) {
            this((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        }

        public RgbColor(Color fxColor) {
            this((int) Math.round(fxColor.getRed() * 255.0),
                 (int) Math.round(fxColor.getGreen() * 255.0),
                 (int) Math.round(fxColor.getBlue() * 255.0));
        }

        public int getR() { return r; }
        public int getG() { return g; }
        public int getB() { return b; }

        public Color toFxColor() {
            return Color.rgb(r, g, b);
        }

        public String toHex() {
            return String.format("#%02X%02X%02X", r, g, b);
        }

        public double distanceSquared(RgbColor other) {
            int dr = this.r - other.r;
            int dg = this.g - other.g;
            int db = this.b - other.b;
            return dr * dr + dg * dg + db * db;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RgbColor rgbColor = (RgbColor) o;
            return r == rgbColor.r && g == rgbColor.g && b == rgbColor.b;
        }

        @Override
        public int hashCode() {
            return Objects.hash(r, g, b);
        }

        @Override
        public String toString() {
            return toHex();
        }
    }


    public static int detectDistinctColorCount(Image image) {
        if (image == null || image.getPixelReader() == null) return 2;

        PixelReader reader = image.getPixelReader();
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        if (width <= 0 || height <= 0) return 2;

        int step = Math.max(1, Math.min(width, height) / 100);
        Set<Integer> reducedColors = new HashSet<>();

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int argb = reader.getArgb(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha < 64) continue; // Skip transparent pixels

                //aaa
                int r = ((argb >> 16) & 0xFF) >> 3;
                int g = ((argb >> 8) & 0xFF) >> 3;
                int b = (argb & 0xFF) >> 3;
                int quantized = (r << 10) | (g << 5) | b;
                reducedColors.add(quantized);

                if (reducedColors.size() >= 10) {
                    return 5;
                }
            }
        }

        int count = reducedColors.size();
        return Math.max(2, Math.min(5, count));
    }


    public static List<RgbColor> extractPalette(Image image, int k) {
        if (image == null || image.getPixelReader() == null) return Collections.emptyList();
        k = Math.max(2, k);

        PixelReader reader = image.getPixelReader();
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int step = Math.max(1, Math.min(width, height) / 80);

        List<RgbColor> sampledPixels = new ArrayList<>();
        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int argb = reader.getArgb(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha >= 64) {
                    sampledPixels.add(new RgbColor(argb));
                }
            }
        }

        if (sampledPixels.isEmpty()) {
            return Arrays.asList(new RgbColor(0, 0, 0), new RgbColor(255, 255, 255));
        }


        List<RgbColor> centroids = new ArrayList<>();
        int interval = Math.max(1, sampledPixels.size() / k);
        for (int i = 0; i < k; i++) {
            centroids.add(sampledPixels.get(Math.min(i * interval, sampledPixels.size() - 1)));
        }


        for (int iter = 0; iter < 8; iter++) {
            long[] sumR = new long[k];
            long[] sumG = new long[k];
            long[] sumB = new long[k];
            int[] counts = new int[k];

            for (RgbColor pixel : sampledPixels) {
                int closestIdx = findClosestColorIndex(pixel, centroids);
                sumR[closestIdx] += pixel.getR();
                sumG[closestIdx] += pixel.getG();
                sumB[closestIdx] += pixel.getB();
                counts[closestIdx]++;
            }

            boolean changed = false;
            for (int i = 0; i < k; i++) {
                if (counts[i] > 0) {
                    RgbColor newCentroid = new RgbColor(
                            (int) (sumR[i] / counts[i]),
                            (int) (sumG[i] / counts[i]),
                            (int) (sumB[i] / counts[i])
                    );
                    if (!newCentroid.equals(centroids.get(i))) {
                        centroids.set(i, newCentroid);
                        changed = true;
                    }
                }
            }
            if (!changed) break;
        }

        return centroids;
    }


    public static int findClosestColorIndex(RgbColor pixel, List<RgbColor> palette) {
        int bestIdx = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < palette.size(); i++) {
            double dist = pixel.distanceSquared(palette.get(i));
            if (dist < bestDist) {
                bestDist = dist;
                bestIdx = i;
            }
        }
        return bestIdx;
    }


    public static int findBackgroundColorIndex(Image image, List<RgbColor> palette) {
        if (image == null || image.getPixelReader() == null || palette == null || palette.isEmpty()) return 0;
        PixelReader reader = image.getPixelReader();
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();

        int[] cornerRgb = new int[] {
                reader.getArgb(0, 0),
                reader.getArgb(w - 1, 0),
                reader.getArgb(0, h - 1),
                reader.getArgb(w - 1, h - 1),
                reader.getArgb(w / 2, 0),
                reader.getArgb(w / 2, h - 1)
        };

        int[] votes = new int[palette.size()];
        for (int rgb : cornerRgb) {
            int idx = findClosestColorIndex(new RgbColor(rgb), palette);
            votes[idx]++;
        }

        int maxIdx = 0;
        int maxVotes = -1;
        for (int i = 0; i < votes.length; i++) {
            if (votes[i] > maxVotes) {
                maxVotes = votes[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }
}

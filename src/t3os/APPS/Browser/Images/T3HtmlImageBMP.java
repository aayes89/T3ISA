/*
 * The MIT License
 *
 * Copyright 2026 Slam.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package t3os.APPS.Browser.Images;

/**
 *
 * @author Slam
 */
import java.io.IOException;
import t3os.APPS.Browser.T3HtmlImage;

public final class T3HtmlImageBMP {

    private static final int MAX_DIMENSION = 8192;
    private static final long MAX_PIXELS = 16_777_216L;

    private T3HtmlImageBMP() {
    }

    public static T3HtmlImage decode(byte[] data) throws IOException {
        if (data == null || data.length < 54) {
            throw new IOException("BMP demasiado pequeño");
        }

        if (data[0] != 'B' || data[1] != 'M') {
            throw new IOException("Firma BMP inválida");
        }

        long pixelOffset = uint32(data, 10);
        long dibSize = uint32(data, 14);

        if (dibSize < 40 || 14L + dibSize > data.length) {
            throw new IOException("Cabecera BMP no compatible");
        }

        int width = int32(data, 18);
        int signedHeight = int32(data, 22);
        int planes = uint16(data, 26);
        int bitsPerPixel = uint16(data, 28);
        long compression = uint32(data, 30);

        if (width <= 0 || signedHeight == 0 || signedHeight == Integer.MIN_VALUE) {
            throw new IOException("Dimensiones BMP inválidas");
        }

        int height = Math.abs(signedHeight);

        if (width > MAX_DIMENSION || height > MAX_DIMENSION || (long) width * height > MAX_PIXELS) {
            throw new IOException("BMP excede los límites permitidos");
        }

        if (planes != 1 || compression != 0 || (bitsPerPixel != 24 && bitsPerPixel != 32)) {
            throw new IOException("BMP compatible únicamente con BI_RGB de 24 o 32 bits");
        }

        long rowSize = (((long) width * bitsPerPixel + 31) / 32) * 4;
        long requiredSize = pixelOffset + rowSize * height;

        if (pixelOffset < 14L + dibSize || requiredSize > data.length || requiredSize < pixelOffset) {
            throw new IOException("Datos de píxeles BMP incompletos");
        }

        int[] pixels = new int[width * height];
        int bytesPerPixel = bitsPerPixel / 8;
        boolean topDown = signedHeight < 0;

        for (int y = 0; y < height; y++) {
            int sourceY = topDown ? y : height - 1 - y;
            int row = (int) (pixelOffset + rowSize * sourceY);

            for (int x = 0; x < width; x++) {
                int p = row + x * bytesPerPixel;

                int blue = data[p] & 0xFF;
                int green = data[p + 1] & 0xFF;
                int red = data[p + 2] & 0xFF;

                // BI_RGB de 32 bits suele reservar el cuarto byte.
                // No se interpreta como transparencia.
                pixels[y * width + x] = 0xFF000000 | (red << 16) | (green << 8) | blue;
            }
        }

        return new T3HtmlImage(width, height, pixels);
    }

    private static int uint16(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    private static long uint32(byte[] data, int offset) {
        return (data[offset] & 0xFFL)
                | ((data[offset + 1] & 0xFFL) << 8)
                | ((data[offset + 2] & 0xFFL) << 16)
                | ((data[offset + 3] & 0xFFL) << 24);
    }

    private static int int32(byte[] data, int offset) {
        return (int) uint32(data, offset);
    }
}

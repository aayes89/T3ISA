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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public final class T3HtmlImageGIF {

    private static final int MAX_DIMENSION = 8192;
    private static final long MAX_PIXELS = 16_777_216L;

    private T3HtmlImageGIF() {
    }

    public static T3HtmlImage decode(byte[] data) throws IOException {
        if (data == null || data.length < 13) {
            throw new IOException("GIF demasiado pequeño");
        }

        String signature = new String(new char[]{(char) data[0], (char) data[1], (char) data[2], (char) data[3], (char) data[4], (char) data[5]});

        if (!"GIF87a".equals(signature) && !"GIF89a".equals(signature)) {
            throw new IOException("Firma GIF inválida");
        }

        Reader in = new Reader(data);
        in.skip(6);

        int screenWidth = in.u16();
        int screenHeight = in.u16();
        int packed = in.u8();
        int backgroundIndex = in.u8();
        in.u8(); // Aspect ratio

        if (screenWidth <= 0 || screenHeight <= 0 || screenWidth > MAX_DIMENSION || screenHeight > MAX_DIMENSION || (long) screenWidth * screenHeight > MAX_PIXELS) {
            throw new IOException("Dimensiones GIF no permitidas");
        }

        int[] globalPalette = null;

        if ((packed & 0x80) != 0) {
            int entries = 1 << ((packed & 7) + 1);
            globalPalette = readPalette(in, entries);
        }

        int[] canvas = new int[screenWidth * screenHeight];

        // El fondo GIF puede ser transparente cuando existe una extensión GCE.
        // Inicialmente se usa el color de fondo global si es válido.
        if (globalPalette != null && backgroundIndex < globalPalette.length) {
            Arrays.fill(canvas, 0xFF000000 | globalPalette[backgroundIndex]);
        }

        int transparentIndex = -1;

        while (!in.atEnd()) {
            int marker = in.u8();

            if (marker == 0x3B) {
                break; // Trailer
            }

            if (marker == 0x21) {
                int label = in.u8();

                if (label == 0xF9) {
                    int blockSize = in.u8();
                    if (blockSize != 4) {
                        throw new IOException("Extensión gráfica GIF inválida");
                    }

                    int gcePacked = in.u8();
                    in.u16(); // Delay, se usará para animación posteriormente
                    int transIndex = in.u8();
                    in.u8(); // Terminador

                    transparentIndex = (gcePacked & 1) != 0
                            ? transIndex : -1;

                } else {
                    // Ignorar extensiones que no son necesarias para
                    // decodificar el primer fotograma.
                    skipSubBlocks(in);
                }

                continue;
            }

            if (marker != 0x2C) {
                throw new IOException("Bloque GIF desconocido: " + marker);
            }

            int left = in.u16();
            int top = in.u16();
            int imageWidth = in.u16();
            int imageHeight = in.u16();
            int imagePacked = in.u8();

            if (imageWidth <= 0 || imageHeight <= 0
                    || (long) imageWidth * imageHeight > MAX_PIXELS
                    || (long) left + imageWidth > screenWidth
                    || (long) top + imageHeight > screenHeight) {
                throw new IOException("Dimensiones del fotograma GIF inválidas");
            }

            int[] palette = globalPalette;

            if ((imagePacked & 0x80) != 0) {
                int entries = 1 << ((imagePacked & 7) + 1);
                palette = readPalette(in, entries);
            }

            if (palette == null) {
                throw new IOException("GIF sin paleta de colores");
            }

            int minimumCodeSize = in.u8();
            if (minimumCodeSize < 2 || minimumCodeSize > 8) {
                throw new IOException("Tamaño LZW GIF inválido");
            }

            byte[] compressed = readSubBlocks(in);
            byte[] indices = decodeLZW(compressed, minimumCodeSize, imageWidth * imageHeight);

            int source = 0;

            if ((imagePacked & 0x40) == 0) {
                for (int y = 0; y < imageHeight; y++) {
                    source = drawRow(canvas, screenWidth, left, top + y, imageWidth, palette, transparentIndex, indices, source);
                }
            } else {
                int[] starts = {0, 4, 2, 1};
                int[] steps = {8, 8, 4, 2};

                for (int pass = 0; pass < 4; pass++) {
                    for (int y = starts[pass]; y < imageHeight; y += steps[pass]) {
                        source = drawRow(canvas, screenWidth, left, top + y, imageWidth, palette, transparentIndex, indices, source);
                    }
                }
            }

            // Primer fotograma decodificado.
            return new T3HtmlImage(screenWidth, screenHeight, canvas);
        }

        throw new IOException("GIF sin imagen");
    }

    private static int drawRow(int[] canvas, int screenWidth, int left, int y, int width, int[] palette, int transparentIndex, byte[] indices, int source) throws IOException {
        for (int x = 0; x < width; x++) {
            int index = indices[source++] & 0xFF;

            if (index == transparentIndex) {
                continue;
            }

            if (index >= palette.length) {
                throw new IOException("Índice de paleta GIF inválido");
            }

            canvas[y * screenWidth + left + x] = 0xFF000000 | palette[index];
        }

        return source;
    }

    private static int[] readPalette(Reader in, int entries) throws IOException {
        int[] palette = new int[entries];

        for (int i = 0; i < entries; i++) {
            int r = in.u8();
            int g = in.u8();
            int b = in.u8();
            palette[i] = (r << 16) | (g << 8) | b;
        }

        return palette;
    }

    private static byte[] decodeLZW(byte[] input, int minimumCodeSize, int expected) throws IOException {
        int clearCode = 1 << minimumCodeSize;
        int endCode = clearCode + 1;

        int[] prefix = new int[4096];
        byte[] suffix = new byte[4096];
        byte[] stack = new byte[4097];
        byte[] output = new byte[expected];

        int codeSize = minimumCodeSize + 1;
        int nextCode = endCode + 1;
        int oldCode = -1;
        int first = 0;
        int out = 0;
        int bitPos = 0;

        for (int i = 0; i < clearCode; i++) {
            suffix[i] = (byte) i;
        }

        while (bitPos + codeSize <= input.length * 8) {
            int code = readCode(input, bitPos, codeSize);
            bitPos += codeSize;

            if (code == clearCode) {
                codeSize = minimumCodeSize + 1;
                nextCode = endCode + 1;
                oldCode = -1;
                continue;
            }

            if (code == endCode) {
                break;
            }

            if (code > nextCode || code >= 4096) {
                throw new IOException("Código LZW GIF inválido");
            }

            int currentCode = code;
            int stackSize = 0;

            if (code == nextCode) {
                if (oldCode < 0) {
                    throw new IOException("Secuencia LZW GIF inválida");
                }

                stack[stackSize++] = (byte) first;
                currentCode = oldCode;
            }

            while (currentCode >= clearCode) {
                if (currentCode >= nextCode || stackSize >= stack.length) {
                    throw new IOException("Diccionario LZW GIF inválido");
                }

                stack[stackSize++] = suffix[currentCode];
                currentCode = prefix[currentCode];
            }

            first = suffix[currentCode] & 0xFF;
            stack[stackSize++] = (byte) first;

            while (stackSize > 0) {
                if (out >= expected) {
                    throw new IOException("GIF produce demasiados píxeles");
                }
                output[out++] = stack[--stackSize];
            }

            if (oldCode >= 0 && nextCode < 4096) {
                prefix[nextCode] = oldCode;
                suffix[nextCode] = (byte) first;
                nextCode++;

                if (nextCode == (1 << codeSize) && codeSize < 12) {
                    codeSize++;
                }
            }

            oldCode = code;
        }

        if (out != expected) {
            throw new IOException("Datos LZW GIF incompletos");
        }

        return output;
    }

    private static int readCode(byte[] data, int bitPos, int size) {
        int value = 0;

        for (int i = 0; i < size; i++) {
            int bit = bitPos + i;
            value |= ((data[bit >>> 3] >>> (bit & 7)) & 1) << i;
        }

        return value;
    }

    private static byte[] readSubBlocks(Reader in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        while (true) {
            int size = in.u8();

            if (size == 0) {
                break;
            }

            if ((long) out.size() + size > 64L * 1024 * 1024) {
                throw new IOException("GIF comprimido demasiado grande");
            }

            out.write(in.bytes(size));
        }

        return out.toByteArray();
    }

    private static void skipSubBlocks(Reader in) throws IOException {
        while (true) {
            int size = in.u8();
            if (size == 0) {
                return;
            }
            in.skip(size);
        }
    }

    private static final class Reader {

        private final byte[] data;
        private int pos;

        Reader(byte[] data) {
            this.data = data;
        }

        int u8() throws IOException {
            if (pos >= data.length) {
                throw new IOException("GIF truncado");
            }
            return data[pos++] & 0xFF;
        }

        int u16() throws IOException {
            int lo = u8();
            int hi = u8();
            return lo | (hi << 8);
        }

        byte[] bytes(int length) throws IOException {
            if (length < 0 || (long) pos + length > data.length) {
                throw new IOException("Bloque GIF incompleto");
            }

            byte[] result = Arrays.copyOfRange(data, pos, pos + length);
            pos += length;
            return result;
        }

        void skip(int length) throws IOException {
            if (length < 0 || (long) pos + length > data.length) {
                throw new IOException("Bloque GIF incompleto");
            }
            pos += length;
        }

        boolean atEnd() {
            return pos >= data.length;
        }
    }
}

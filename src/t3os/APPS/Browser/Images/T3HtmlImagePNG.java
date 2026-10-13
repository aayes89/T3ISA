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
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public final class T3HtmlImagePNG {

    private static final int MAX_DIMENSION = 8192;
    private static final long MAX_PIXELS = 16_777_216L;
    private static final byte[] SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};

    private T3HtmlImagePNG() {
    }

    public static T3HtmlImage decode(byte[] data) throws IOException {
        if (data == null || data.length < 33) {
            throw new IOException("PNG demasiado pequeño");
        }

        for (int i = 0; i < SIGNATURE.length; i++) {
            if (data[i] != SIGNATURE[i]) {
                throw new IOException("Firma PNG inválida");
            }
        }

        int width = 0;
        int height = 0;
        int bitDepth = 0;
        int colorType = -1;
        int interlace = -1;

        byte[] palette = null;
        byte[] transparency = null;
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();

        int pos = 8;
        boolean seenIHDR = false;
        boolean seenIDAT = false;
        boolean endedIDAT = false;
        boolean seenIEND = false;

        while (pos <= data.length - 12) {
            long lengthLong = uint32(data, pos);
            if (lengthLong > Integer.MAX_VALUE) {
                throw new IOException("Chunk PNG demasiado grande");
            }

            int length = (int) lengthLong;
            long endLong = (long) pos + 12 + length;

            if (endLong > data.length) {
                throw new IOException("Chunk PNG incompleto");
            }

            int typePos = pos + 4;
            int payload = pos + 8;

            String type = new String(new char[]{
                (char) data[typePos],
                (char) data[typePos + 1],
                (char) data[typePos + 2],
                (char) data[typePos + 3]
            });

            // Verificar CRC de todos los chunks.
            java.util.zip.CRC32 crc = new java.util.zip.CRC32();
            crc.update(data, typePos, 4 + length);
            long storedCRC = uint32(data, payload + length);

            if (crc.getValue() != storedCRC) {
                throw new IOException("CRC inválido en chunk PNG " + type);
            }

            if (!seenIHDR && !"IHDR".equals(type)) {
                throw new IOException("IHDR debe ser el primer chunk");
            }

            if ("IHDR".equals(type)) {
                if (seenIHDR || pos != 8 || length != 13) {
                    throw new IOException("IHDR inválido");
                }

                long w = uint32(data, payload);
                long h = uint32(data, payload + 4);

                if (w == 0 || h == 0 || w > MAX_DIMENSION || h > MAX_DIMENSION || w * h > MAX_PIXELS) {
                    throw new IOException("Dimensiones PNG no permitidas");
                }

                width = (int) w;
                height = (int) h;
                bitDepth = data[payload + 8] & 0xFF;
                colorType = data[payload + 9] & 0xFF;

                int compressionMethod = data[payload + 10] & 0xFF;
                int filterMethod = data[payload + 11] & 0xFF;
                interlace = data[payload + 12] & 0xFF;

                if (compressionMethod != 0 || filterMethod != 0 || interlace != 0) {
                    throw new IOException("PNG no compatible: requiere compresión/filtro estándar " + "y sin entrelazado Adam7");
                }

                boolean validDepth
                        = bitDepth == 8
                        || (bitDepth == 16 && colorType != 3)
                        || (bitDepth == 1 && colorType == 0)
                        || (bitDepth == 2 && (colorType == 0 || colorType == 3))
                        || (bitDepth == 4 && (colorType == 0 || colorType == 3));

                if (!validDepth) {
                    throw new IOException("Profundidad PNG no compatible");
                }

                if (colorType != 0 && colorType != 2
                        && colorType != 3 && colorType != 4
                        && colorType != 6) {
                    throw new IOException("Tipo de color PNG inválido");
                }

                seenIHDR = true;

            } else if ("PLTE".equals(type)) {
                if (seenIDAT || length == 0 || length % 3 != 0 || length > 768) {
                    throw new IOException("Paleta PNG inválida");
                }
                palette = Arrays.copyOfRange(data, payload, payload + length);

            } else if ("tRNS".equals(type)) {
                if (seenIDAT) {
                    throw new IOException("tRNS después de IDAT");
                }
                transparency = Arrays.copyOfRange(
                        data, payload, payload + length);

            } else if ("IDAT".equals(type)) {
                if (endedIDAT) {
                    throw new IOException("Chunks IDAT no contiguos");
                }

                seenIDAT = true;
                if ((long) compressed.size() + length > 64L * 1024 * 1024) {
                    throw new IOException("PNG comprimido demasiado grande");
                }
                compressed.write(data, payload, length);

            } else if ("IEND".equals(type)) {
                if (length != 0 || !seenIDAT) {
                    throw new IOException("IEND inválido");
                }
                seenIEND = true;
                pos = (int) endLong;
                break;

            } else {
                // Un chunk desconocido crítico no puede ignorarse.
                if ((data[typePos] & 0x20) == 0) {
                    throw new IOException("Chunk crítico PNG desconocido: " + type);
                }
            }

            if (seenIDAT && !"IDAT".equals(type)) {
                endedIDAT = true;
            }

            pos = (int) endLong;
        }

        if (!seenIHDR || !seenIDAT || !seenIEND) {
            throw new IOException("PNG incompleto");
        }

        if (colorType == 3 && palette == null) {
            throw new IOException("PNG indexado sin paleta");
        }

        int channels;
        switch (colorType) {
            case 0:
                channels = 1;
                break;
            case 2:
                channels = 3;
                break;
            case 3:
                channels = 1;
                break;
            case 4:
                channels = 2;
                break;
            case 6:
                channels = 4;
                break;
            default:
                throw new IOException("Tipo de color no compatible");
        }

        long rowBytesLong = ((long) width * channels * bitDepth + 7) / 8;
        long rawSizeLong = (rowBytesLong + 1) * height;

        if (rawSizeLong > 128L * 1024 * 1024 || rowBytesLong > Integer.MAX_VALUE) {
            throw new IOException("PNG descomprimido demasiado grande");
        }

        int rowBytes = (int) rowBytesLong;
        byte[] raw = inflate(compressed.toByteArray(), (int) rawSizeLong);

        int bytesPerPixel = Math.max(1, (channels * bitDepth + 7) / 8);
        byte[] previous = new byte[rowBytes];
        byte[] current = new byte[rowBytes];
        int[] pixels = new int[width * height];

        int offset = 0;

        int transparentGray = -1;
        int transparentRed = -1;
        int transparentGreen = -1;
        int transparentBlue = -1;

        if (transparency != null) {
            if (colorType == 0 && transparency.length == 2) {
                transparentGray = uint16be(transparency, 0);
            } else if (colorType == 2 && transparency.length == 6) {
                transparentRed = uint16be(transparency, 0);
                transparentGreen = uint16be(transparency, 2);
                transparentBlue = uint16be(transparency, 4);
            } else if (colorType == 3 && transparency.length > palette.length / 3) {
                throw new IOException("Transparencia de paleta inválida");
            } else if (colorType != 0 && colorType != 2 && colorType != 3) {
                throw new IOException("tRNS incompatible con tipo de color");
            }
        }

        for (int y = 0; y < height; y++) {
            int filter = raw[offset++] & 0xFF;

            if (offset + rowBytes > raw.length) {
                throw new IOException("Datos PNG incompletos");
            }

            System.arraycopy(raw, offset, current, 0, rowBytes);
            offset += rowBytes;

            unfilter(current, previous, bytesPerPixel, filter);

            for (int x = 0; x < width; x++) {
                int r, g, b, a = 255;

                if (bitDepth < 8) {
                    int sample = packedSample(current, x, bitDepth);
                    int max = (1 << bitDepth) - 1;

                    if (colorType == 3) {
                        int index = sample;
                        int p = index * 3;
                        if (p + 2 >= palette.length) {
                            throw new IOException("Índice de paleta inválido");
                        }
                        r = palette[p] & 0xFF;
                        g = palette[p + 1] & 0xFF;
                        b = palette[p + 2] & 0xFF;
                        if (transparency != null && index < transparency.length) {
                            a = transparency[index] & 0xFF;
                        }
                    } else {
                        r = g = b = sample * 255 / max;
                        if (transparentGray == sample) {
                            a = 0;
                        }
                    }
                } else {
                    int base = x * channels * (bitDepth / 8);

                    int c0 = sample(current, base, bitDepth);
                    int c1 = channels > 1 ? sample(current, base + bitDepth / 8, bitDepth) : c0;
                    int c2 = channels > 2 ? sample(current, base + 2 * (bitDepth / 8), bitDepth) : c0;
                    int c3 = channels > 3 ? sample(current, base + 3 * (bitDepth / 8), bitDepth) : 255;

                    switch (colorType) {
                        case 0:
                            r = g = b = scale(c0, bitDepth);
                            if (transparentGray == c0) {
                                a = 0;
                            }
                            break;
                        case 2:
                            r = scale(c0, bitDepth);
                            g = scale(c1, bitDepth);
                            b = scale(c2, bitDepth);
                            if (transparentRed == c0 && transparentGreen == c1
                                    && transparentBlue == c2) {
                                a = 0;
                            }
                            break;
                        case 3:
                            int index = c0;
                            int p = index * 3;
                            if (p + 2 >= palette.length) {
                                throw new IOException("Índice de paleta inválido");
                            }
                            r = palette[p] & 0xFF;
                            g = palette[p + 1] & 0xFF;
                            b = palette[p + 2] & 0xFF;
                            if (transparency != null && index < transparency.length) {
                                a = transparency[index] & 0xFF;
                            }
                            break;
                        case 4:
                            r = g = b = scale(c0, bitDepth);
                            a = scale(c1, bitDepth);
                            break;
                        case 6:
                            r = scale(c0, bitDepth);
                            g = scale(c1, bitDepth);
                            b = scale(c2, bitDepth);
                            a = scale(c3, bitDepth);
                            break;
                        default:
                            throw new IOException("Tipo de color PNG inválido");
                    }
                }

                pixels[y * width + x]
                        = (a << 24) | (r << 16) | (g << 8) | b;
            }

            byte[] swap = previous;
            previous = current;
            current = swap;
        }

        return new T3HtmlImage(width, height, pixels);
    }

    private static byte[] inflate(byte[] input, int expected) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(input);

        byte[] result = new byte[expected];
        int offset = 0;

        try {
            while (!inflater.finished() && offset < expected) {
                int n = inflater.inflate(result, offset, expected - offset);
                if (n == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) {
                        break;
                    }
                }
                offset += n;
            }

            if (!inflater.finished() || offset != expected) {
                throw new IOException("Flujo DEFLATE PNG inválido o incompleto");
            }

            return result;
        } catch (DataFormatException ex) {
            throw new IOException("Datos comprimidos PNG inválidos", ex);
        } finally {
            inflater.end();
        }
    }

    private static void unfilter(
            byte[] row, byte[] previous, int bpp, int filter)
            throws IOException {

        for (int i = 0; i < row.length; i++) {
            int raw = row[i] & 0xFF;
            int left = i >= bpp ? row[i - bpp] & 0xFF : 0;
            int up = previous[i] & 0xFF;
            int upperLeft = i >= bpp ? previous[i - bpp] & 0xFF : 0;

            switch (filter) {
                case 0:
                    break;
                case 1:
                    raw += left;
                    break;
                case 2:
                    raw += up;
                    break;
                case 3:
                    raw += (left + up) >>> 1;
                    break;
                case 4:
                    raw += paeth(left, up, upperLeft);
                    break;
                default:
                    throw new IOException("Filtro PNG desconocido: " + filter);
            }

            row[i] = (byte) raw;
        }
    }

    private static int paeth(int a, int b, int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);

        if (pa <= pb && pa <= pc) {
            return a;
        }
        if (pb <= pc) {
            return b;
        }
        return c;
    }

    private static int packedSample(byte[] row, int x, int depth) {
        int bit = x * depth;
        int shift = 8 - depth - (bit & 7);
        return ((row[bit >>> 3] & 0xFF) >>> shift) & ((1 << depth) - 1);
    }

    private static int sample(byte[] row, int offset, int depth) {
        if (depth == 16) {
            return ((row[offset] & 0xFF) << 8) | (row[offset + 1] & 0xFF);
        }
        return row[offset] & 0xFF;
    }

    private static int scale(int value, int depth) {
        return depth == 16 ? value >>> 8 : value;
    }

    private static int uint16be(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static long uint32(byte[] data, int offset) {
        return ((data[offset] & 0xFFL) << 24)
                | ((data[offset + 1] & 0xFFL) << 16)
                | ((data[offset + 2] & 0xFFL) << 8)
                | (data[offset + 3] & 0xFFL);
    }
}

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

public final class T3HtmlImageJPEG {

    private static final int MAX_DIMENSION = 8192;
    private static final int MAX_PIXELS = 16_777_216;

    private static final int[] ZIGZAG = {
        0, 1, 8, 16, 9, 2, 3, 10,
        17, 24, 32, 25, 18, 11, 4, 5,
        12, 19, 26, 33, 40, 48, 41, 34,
        27, 20, 13, 6, 7, 14, 21, 28,
        35, 42, 49, 56, 57, 50, 43, 36,
        29, 22, 15, 23, 30, 37, 44, 51,
        58, 59, 52, 45, 38, 31, 39, 46,
        53, 60, 61, 54, 47, 55, 62, 63
    };

    private static final int[] STD_LUMA_Q = {
        16, 11, 10, 16, 24, 40, 51, 61,
        12, 12, 14, 19, 26, 58, 60, 55,
        14, 13, 16, 24, 40, 57, 69, 56,
        14, 17, 22, 29, 51, 87, 80, 62,
        18, 22, 37, 56, 68, 109, 103, 77,
        24, 35, 55, 64, 81, 104, 113, 92,
        49, 64, 78, 87, 103, 121, 120, 101,
        72, 92, 95, 98, 112, 100, 103, 99
    };

    private static final int[] STD_CHROMA_Q = {
        17, 18, 24, 47, 99, 99, 99, 99,
        18, 21, 26, 66, 99, 99, 99, 99,
        24, 26, 56, 99, 99, 99, 99, 99,
        47, 66, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99
    };

    private static final int[][] STD_DC_BITS = {
        {0, 1, 5, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0},
        {0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0}
    };

    private static final int[][] STD_DC_VALS = {
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11},
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}
    };

    private static final int[][] STD_AC_BITS = {
        {0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0, 0, 1, 0x7d},
        {0, 2, 1, 2, 4, 4, 3, 4, 7, 5, 4, 4, 0, 1, 2, 0x77}
    };

    private static final int[][] STD_AC_VALS = {
        {
            0x01, 0x02, 0x03, 0x00, 0x04, 0x11, 0x05, 0x12,
            0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07,
            0x22, 0x71, 0x14, 0x32, 0x81, 0x91, 0xa1, 0x08,
            0x23, 0x42, 0xb1, 0xc1, 0x15, 0x52, 0xd1, 0xf0,
            0x24, 0x33, 0x62, 0x72, 0x82, 0x09, 0x0a, 0x16,
            0x17, 0x18, 0x19, 0x1a, 0x25, 0x26, 0x27, 0x28,
            0x29, 0x2a, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
            0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49,
            0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59,
            0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69,
            0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79,
            0x7a, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89,
            0x8a, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98,
            0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7,
            0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6,
            0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3, 0xc4, 0xc5,
            0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4,
            0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xe1, 0xe2,
            0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea,
            0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
            0xf9, 0xfa
        },
        {
            0x00, 0x01, 0x02, 0x03, 0x11, 0x04, 0x05, 0x21,
            0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71,
            0x13, 0x22, 0x32, 0x81, 0x08, 0x14, 0x42, 0x91,
            0xa1, 0xb1, 0xc1, 0x09, 0x23, 0x33, 0x52, 0xf0,
            0x15, 0x62, 0x72, 0xd1, 0x0a, 0x16, 0x24, 0x34,
            0xe1, 0x25, 0xf1, 0x17, 0x18, 0x19, 0x1a, 0x26,
            0x27, 0x28, 0x29, 0x2a, 0x35, 0x36, 0x37, 0x38,
            0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
            0x49, 0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58,
            0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68,
            0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78,
            0x79, 0x7a, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,
            0x88, 0x89, 0x8a, 0x92, 0x93, 0x94, 0x95, 0x96,
            0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5,
            0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4,
            0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3,
            0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2,
            0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda,
            0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9,
            0xea, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
            0xf9, 0xfa
        }
    };

    private static final class Huffman {

        final int[] minCode = new int[17];
        final int[] maxCode = new int[18];
        final int[] valPtr = new int[17];
        int[] values;

        Huffman(int[] bits, int[] vals) throws IOException {
            values = vals;
            int code = 0;
            int index = 0;

            for (int len = 1; len <= 16; len++) {
                int count = bits[len - 1];

                if (count == 0) {
                    minCode[len] = -1;
                    maxCode[len] = -1;
                } else {
                    minCode[len] = code;
                    maxCode[len] = code + count - 1;
                    valPtr[len] = index;
                    index += count;
                }

                code = (code + count) << 1;
            }

            if (index != vals.length || code > 65536) {
                throw new IOException("Tabla Huffman JPEG inválida");
            }
        }

        int decode(Bits bits) throws IOException {
            int code = 0;

            for (int len = 1; len <= 16; len++) {
                code = (code << 1) | bits.readBit();

                if (maxCode[len] >= 0 && code >= minCode[len] && code <= maxCode[len]) {
                    int i = valPtr[len] + code - minCode[len];

                    if (i < 0 || i >= values.length) {
                        throw new IOException("Índice Huffman inválido");
                    }

                    return values[i];
                }
            }

            throw new IOException("Código Huffman inválido");
        }
    }

    private static final class Bits {

        final byte[] data;
        int pos;
        int current;
        int remaining;

        Bits(byte[] data, int pos) {
            this.data = data;
            this.pos = pos;
        }

        int readBit() throws IOException {
            if (remaining == 0) {
                if (pos >= data.length) {
                    throw new IOException("JPEG truncado");
                }

                current = data[pos++] & 0xFF;

                if (current == 0xFF) {
                    if (pos >= data.length || (data[pos++] & 0xFF) != 0) {
                        throw new IOException("Marcador inesperado dentro de JPEG");
                    }
                }

                remaining = 8;
            }

            remaining--;
            return (current >> remaining) & 1;
        }

        int readBits(int count) throws IOException {
            int value = 0;

            for (int i = 0; i < count; i++) {
                value = (value << 1) | readBit();
            }

            return value;
        }

        void align() {
            remaining = 0;
        }

        int position() {
            return pos;
        }

        void restart(int expected) throws IOException {
            align();

            if (pos >= data.length || (data[pos++] & 0xFF) != 0xFF) {
                throw new IOException("Marcador JPEG de reinicio ausente");
            }

            while (pos < data.length && (data[pos] & 0xFF) == 0xFF) {
                pos++;
            }

            if (pos >= data.length || (data[pos++] & 0xFF) != 0xD0 + expected) {
                throw new IOException("Secuencia de reinicio JPEG inválida");
            }
        }
    }

    private static final class Component {

        int id, h, v, qid, dcid, acid;
        int predictor;
        int sampleWidth, sampleHeight;
        byte[] samples;
        int[] quant;
    }

    private static final class Decoder {

        final byte[] data;
        int pos;

        int width, height;
        int maxH, maxV;
        int restartInterval;
        int componentCount;

        final Component[] components = new Component[4];
        final int[][] quant = new int[4][];
        final Huffman[][] huffman = new Huffman[2][4];

        Decoder(byte[] data) {
            this.data = data;
        }

        int u8() throws IOException {
            if (pos >= data.length) {
                throw new IOException("JPEG truncado");
            }
            return data[pos++] & 0xFF;
        }

        int u16() throws IOException {
            return (u8() << 8) | u8();
        }

        void skip(int count) throws IOException {
            if (count < 0 || pos + count > data.length) {
                throw new IOException("Segmento JPEG truncado");
            }
            pos += count;
        }

        int marker() throws IOException {
            while (pos < data.length && (data[pos++] & 0xFF) != 0xFF) {
                // Busca el prefijo del marcador.
            }

            while (pos < data.length && (data[pos] & 0xFF) == 0xFF) {
                pos++;
            }

            if (pos >= data.length) {
                throw new IOException("Marcador JPEG ausente");
            }

            int m = data[pos++] & 0xFF;

            if (m == 0) {
                throw new IOException("Marcador JPEG inválido");
            }

            return m;
        }

        void readQuant() throws IOException {
            int length = u16();
            int end = pos + length - 2;

            if (length < 2 || end > data.length) {
                throw new IOException("DQT inválido");
            }

            while (pos < end) {
                int info = u8();
                int precision = info >> 4;
                int id = info & 15;

                if (id > 3 || precision > 1) {
                    throw new IOException("Tabla cuantizadora no soportada");
                }

                int[] table = new int[64];

                for (int i = 0; i < 64; i++) {
                    table[ZIGZAG[i]] = precision == 0 ? u8() : u16();
                }

                quant[id] = table;
            }

            if (pos != end) {
                throw new IOException("Longitud DQT inválida");
            }
        }

        void readHuffman() throws IOException {
            int length = u16();
            int end = pos + length - 2;

            if (length < 2 || end > data.length) {
                throw new IOException("DHT inválido");
            }

            while (pos < end) {
                int info = u8();
                int type = info >> 4;
                int id = info & 15;

                if (type > 1 || id > 3) {
                    throw new IOException("Tabla Huffman no soportada");
                }

                int[] bits = new int[16];
                int count = 0;

                for (int i = 0; i < 16; i++) {
                    bits[i] = u8();
                    count += bits[i];
                }

                if (count > 256 || pos + count > end) {
                    throw new IOException("Longitud DHT inválida");
                }

                int[] vals = new int[count];

                for (int i = 0; i < count; i++) {
                    vals[i] = u8();
                }

                huffman[type][id] = new Huffman(bits, vals);
            }

            if (pos != end) {
                throw new IOException("Longitud DHT inválida");
            }
        }

        void readFrame() throws IOException {
            int length = u16();

            if (length < 8) {
                throw new IOException("SOF0 inválido");
            }

            int precision = u8();
            height = u16();
            width = u16();
            componentCount = u8();

            if (precision != 8) {
                throw new IOException("JPEG debe ser de 8 bits");
            }

            if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION || (long) width * height > MAX_PIXELS) {
                throw new IOException("Dimensiones JPEG fuera de límites");
            }

            if (componentCount != 1 && componentCount != 3) {
                throw new IOException("JPEG soporta únicamente escala de grises o tres componentes");
            }

            if (length != 8 + componentCount * 3) {
                throw new IOException("Longitud SOF0 inválida");
            }

            maxH = 1;
            maxV = 1;

            for (int i = 0; i < componentCount; i++) {
                Component c = new Component();

                c.id = u8();
                int sampling = u8();
                c.h = sampling >> 4;
                c.v = sampling & 15;
                c.qid = u8();

                if (c.h < 1 || c.h > 4 || c.v < 1 || c.v > 4 || c.qid > 3) {
                    throw new IOException("Submuestreo JPEG inválido");
                }

                maxH = Math.max(maxH, c.h);
                maxV = Math.max(maxV, c.v);
                components[i] = c;
            }

            int blocksPerMCU = 0;

            for (int i = 0; i < componentCount; i++) {
                Component c = components[i];
                blocksPerMCU += c.h * c.v;
            }

            if (blocksPerMCU > 10) {
                throw new IOException("Cantidad de bloques JPEG excesiva");
            }

            for (int i = 0; i < componentCount; i++) {
                Component c = components[i];

                int mcuCols = (width + 8 * maxH - 1) / (8 * maxH);
                int mcuRows = (height + 8 * maxV - 1) / (8 * maxV);

                c.sampleWidth = mcuCols * c.h * 8;
                c.sampleHeight = mcuRows * c.v * 8;

                long size = (long) c.sampleWidth * c.sampleHeight;

                if (size > MAX_PIXELS * 4L) {
                    throw new IOException("Componente JPEG demasiado grande");
                }

                c.samples = new byte[(int) size];
                c.quant = quant[c.qid];

                if (c.quant == null) {
                    throw new IOException("Falta tabla cuantizadora JPEG");
                }
            }
        }

        int receiveExtend(Bits bits, int size) throws IOException {
            if (size == 0) {
                return 0;
            }

            int value = bits.readBits(size);

            if (value < (1 << (size - 1))) {
                value -= (1 << size) - 1;
            }

            return value;
        }

        void decodeBlock(Bits bits, Component c, int bx, int by)
                throws IOException {

            Huffman dc = huffman[0][c.dcid];
            Huffman ac = huffman[1][c.acid];

            if (dc == null || ac == null) {
                throw new IOException("Falta tabla Huffman JPEG");
            }

            int[] coeff = new int[64];

            int category = dc.decode(bits);

            if (category > 11) {
                throw new IOException("Categoría DC JPEG inválida");
            }

            c.predictor += receiveExtend(bits, category);
            coeff[0] = c.predictor * c.quant[0];

            int k = 1;

            while (k < 64) {
                int symbol = ac.decode(bits);
                int run = symbol >> 4;
                int size = symbol & 15;

                if (size == 0) {
                    if (run == 0) {
                        break;
                    }

                    if (run != 15) {
                        throw new IOException("Código AC JPEG inválido");
                    }

                    k += 16;
                    continue;
                }

                if (size > 10) {
                    throw new IOException("Categoría AC JPEG inválida");
                }

                k += run;

                if (k >= 64) {
                    throw new IOException("Bloque AC JPEG inválido");
                }

                int natural = ZIGZAG[k];
                coeff[natural] = receiveExtend(bits, size) * c.quant[natural];
                k++;
            }

            int[] block = idct(coeff);

            int offsetX = bx * 8;
            int offsetY = by * 8;

            for (int y = 0; y < 8; y++) {
                int dst = (offsetY + y) * c.sampleWidth + offsetX;

                for (int x = 0; x < 8; x++) {
                    c.samples[dst + x] = (byte) Math.max(0, Math.min(255, block[y * 8 + x]));
                }
            }
        }

        int[] idct(int[] coefficients) {
            int[] out = new int[64];

            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    double sum = 0.0;

                    for (int v = 0; v < 8; v++) {
                        double cv = v == 0 ? 1.0 / Math.sqrt(2.0) : 1.0;
                        double cy = Math.cos(((2 * y + 1) * v * Math.PI) / 16.0);

                        for (int u = 0; u < 8; u++) {
                            double cu = u == 0 ? 1.0 / Math.sqrt(2.0) : 1.0;
                            double cx = Math.cos(((2 * x + 1) * u * Math.PI) / 16.0);

                            sum += cu * cv * coefficients[v * 8 + u] * cx * cy;
                        }
                    }

                    out[y * 8 + x] = (int) Math.round(sum / 4.0 + 128.0);
                }
            }

            return out;
        }

        int sample(Component c, int x, int y) {
            int sx = x * c.h / maxH;
            int sy = y * c.v / maxV;

            sx = Math.min(sx, c.sampleWidth - 1);
            sy = Math.min(sy, c.sampleHeight - 1);

            return c.samples[sy * c.sampleWidth + sx] & 0xFF;
        }

        T3HtmlImage decode() throws IOException {
            if (u8() != 0xFF || u8() != 0xD8) {
                throw new IOException("No es un archivo JPEG");
            }

            boolean frameRead = false;

            while (pos < data.length) {
                int m = marker();

                switch (m) {
                    case 0xDB:
                        readQuant();
                        break;

                    case 0xC4:
                        readHuffman();
                        break;

                    case 0xC0:
                        if (frameRead) {
                            throw new IOException("Múltiples SOF0 no soportados");
                        }
                        readFrame();
                        frameRead = true;
                        break;

                    case 0xDD: {
                        int len = u16();

                        if (len != 4) {
                            throw new IOException("DRI inválido");
                        }

                        restartInterval = u16();
                        break;
                    }

                    case 0xDA:
                        if (!frameRead) {
                            throw new IOException("JPEG sin SOF0");
                        }
                        return readScan();

                    case 0xC1:
                    case 0xC2:
                    case 0xC3:
                    case 0xC5:
                    case 0xC6:
                    case 0xC7:
                    case 0xC9:
                    case 0xCA:
                    case 0xCB:
                    case 0xCD:
                    case 0xCE:
                    case 0xCF:
                        throw new IOException("JPEG progresivo o modo no baseline no soportado");

                    case 0xD9:
                        throw new IOException("JPEG sin imagen SOS");

                    default:
                        if (m >= 0xD0 && m <= 0xD7) {
                            throw new IOException("Reinicio fuera de scan");
                        }

                        int len = u16();

                        if (len < 2) {
                            throw new IOException("Segmento JPEG inválido");
                        }

                        skip(len - 2);
                        break;
                }
            }

            throw new IOException("JPEG incompleto");
        }

        T3HtmlImage readScan() throws IOException {
            int length = u16();
            int end = pos + length - 2;

            if (length < 6 || end > data.length) {
                throw new IOException("SOS inválido");
            }

            int scanComponents = u8();

            if (scanComponents != componentCount) {
                throw new IOException("JPEG requiere una única scan intercalada");
            }

            for (int i = 0; i < scanComponents; i++) {
                int id = u8();
                int tables = u8();
                Component c = null;

                for (Component candidate : components) {
                    if (candidate.id == id) {
                        c = candidate;
                        break;
                    }
                }

                if (c == null) {
                    throw new IOException("Componente SOS desconocido");
                }

                c.dcid = tables >> 4;
                c.acid = tables & 15;

                if (c.dcid > 3 || c.acid > 3) {
                    throw new IOException("Selector Huffman inválido");
                }
            }

            int spectralStart = u8();
            int spectralEnd = u8();
            int successive = u8();

            if (pos != end || spectralStart != 0 || spectralEnd != 63 || successive != 0) {
                throw new IOException("Scan JPEG no baseline");
            }

            int mcuCols = (width + maxH * 8 - 1) / (maxH * 8);
            int mcuRows = (height + maxV * 8 - 1) / (maxV * 8);

            Bits bits = new Bits(data, pos);
            int restartCount = 0;
            int expectedRestart = 0;

            for (Component c : components) {
                c.predictor = 0;
            }

            for (int my = 0; my < mcuRows; my++) {
                for (int mx = 0; mx < mcuCols; mx++) {
                    if (restartInterval > 0 && restartCount == restartInterval) {
                        bits.restart(expectedRestart);
                        expectedRestart = (expectedRestart + 1) & 7;
                        restartCount = 0;

                        for (Component c : components) {
                            c.predictor = 0;
                        }
                    }

                    for (Component c : components) {
                        for (int vy = 0; vy < c.v; vy++) {
                            for (int hx = 0; hx < c.h; hx++) {
                                decodeBlock(bits, c, mx * c.h + hx, my * c.v + vy);
                            }
                        }
                    }

                    restartCount++;
                }
            }

            pos = bits.position();

            int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int r, g, b;

                    int a = sample(components[0], x, y);

                    if (componentCount == 1) {
                        r = g = b = a;
                    } else {
                        int cb = sample(components[1], x, y) - 128;
                        int cr = sample(components[2], x, y) - 128;

                        r = clamp((int) Math.round(a + 1.402 * cr));
                        g = clamp((int) Math.round(a - 0.344136 * cb - 0.714136 * cr));
                        b = clamp((int) Math.round(a + 1.772 * cb));
                    }

                    pixels[y * width + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }

            return new T3HtmlImage(width, height, pixels);
        }

        int clamp(int value) {
            return Math.max(0, Math.min(255, value));
        }
    }

    private T3HtmlImageJPEG() {
    }

    public static T3HtmlImage decode(byte[] data) throws IOException {
        if (data == null || data.length < 4) {
            throw new IOException("JPEG vacío o demasiado corto");
        }

        return new Decoder(data).decode();
    }
}

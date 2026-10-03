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
package t3isa.FS;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class TFileSystem {

    public static final int BLOCK_SIZE = 256;
    public static final int BLOCK_COUNT = 256;

    private static final int MAGIC = 0x54334653; // "T3FS"
    private static final int VERSION = 2;

    /*
     * Bloque 0:
     *
     *   0..3    MAGIC
     *   4..7    VERSION
     *   8..11   BLOCK_SIZE
     *   12..15  BLOCK_COUNT
     *   16..19  SUPERBLOCK
     *   20..23  BITMAP
     *   24..27  DATA_START
     */
    private static final int SUPERBLOCK = 0;
    private static final int BITMAP_BLOCK = 1;
    private static final int DATA_START = 2;

    private final boolean[] usedBlocks;
    private final String persistenceFile;
    private final byte[][] blocks;

    public TFileSystem() {
        this("t3fs.dat");
    }

    public TFileSystem(String persistenceFile) {
        if (persistenceFile == null || persistenceFile.isEmpty()) {
            throw new IllegalArgumentException("Archivo de persistencia inválido");
        }

        this.persistenceFile = persistenceFile;
        this.usedBlocks = new boolean[BLOCK_COUNT];
        this.blocks = new byte[BLOCK_COUNT][BLOCK_SIZE];

        if (!load()) {
            format();
        }
    }

    public void format() {
        for (int i = 0; i < BLOCK_COUNT; i++) {
            usedBlocks[i] = false;
            for (int j = 0; j < BLOCK_SIZE; j++) {
                blocks[i][j] = 0;
            }
        }

        /*
         * Los bloques 0 y 1 están reservados:
         *
         * 0 = superbloque
         * 1 = bitmap
         */
        usedBlocks[SUPERBLOCK] = true;
        usedBlocks[BITMAP_BLOCK] = true;

        writeSuperblock();
        writeBitmap();

        save();
    }

    public int allocateBlock() {
        for (int i = DATA_START; i < BLOCK_COUNT; i++) {
            if (!usedBlocks[i]) {
                usedBlocks[i] = true;
                writeBitmap();
                save();
                return i;
            }
        }
        throw new IllegalStateException("No hay bloques libres");
    }

    public void freeBlock(int block) {
        checkDataBlock(block);
        usedBlocks[block] = false;

        for (int i = 0; i < BLOCK_SIZE; i++) {
            blocks[block][i] = 0;
        }

        writeBitmap();
        save();
    }

    public void writeBlock(int block, byte[] data) {
        checkDataBlock(block);

        if (!usedBlocks[block]) {
            throw new IllegalStateException("El bloque no está asignado: " + block);
        }

        if (data == null) {
            throw new IllegalArgumentException("Datos null");
        }

        if (data.length > BLOCK_SIZE) {
            throw new IllegalArgumentException("Datos exceden el tamaño del bloque");
        }

        for (int i = 0; i < BLOCK_SIZE; i++) {
            blocks[block][i] = 0;
        }

        System.arraycopy(data, 0, blocks[block], 0, data.length);
        save();
    }

    public byte[] readBlock(int block) {
        checkDataBlock(block);

        if (!usedBlocks[block]) {
            throw new IllegalStateException("El bloque no está asignado: " + block);
        }

        return blocks[block].clone();
    }

    public boolean isBlockUsed(int block) {
        checkBlock(block);
        return usedBlocks[block];
    }

    public int getTotalBlocks() {
        return BLOCK_COUNT - DATA_START;
    }

    public int getUsedBlocks() {
        int count = 0;

        for (int i = DATA_START; i < BLOCK_COUNT; i++) {
            if (usedBlocks[i]) {
                count++;
            }
        }

        return count;
    }

    public int getFreeBlocks() {
        return getTotalBlocks() - getUsedBlocks();
    }

    public void save() {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(persistenceFile))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);

            out.writeInt(BLOCK_SIZE);
            out.writeInt(BLOCK_COUNT);

            out.writeInt(SUPERBLOCK);
            out.writeInt(BITMAP_BLOCK);
            out.writeInt(DATA_START);

            /*
             * Bitmap.
             */
            for (int i = 0; i < BLOCK_COUNT; i++) {
                out.writeBoolean(usedBlocks[i]);
            }

            /*
             * Todos los bloques.
             *
             * Los bloques reservados también se persisten.
             */
            for (int i = 0; i < BLOCK_COUNT; i++) {
                if (usedBlocks[i]) {
                    out.write(blocks[i]);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el filesystem", e);
        }
    }

    private boolean load() {
        try (DataInputStream in = new DataInputStream(new FileInputStream(persistenceFile))) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                return false;
            }

            int version = in.readInt();
            if (version != VERSION) {
                return false;
            }

            int blockSize = in.readInt();
            int blockCount = in.readInt();
            if (blockSize != BLOCK_SIZE || blockCount != BLOCK_COUNT) {
                return false;
            }

            int superblock = in.readInt();
            int bitmapBlock = in.readInt();
            int dataStart = in.readInt();
            if (superblock != SUPERBLOCK || bitmapBlock != BITMAP_BLOCK || dataStart != DATA_START) {
                return false;
            }

            /*
             * Leer bitmap.
             */
            for (int i = 0; i < BLOCK_COUNT; i++) {
                usedBlocks[i] = in.readBoolean();
            }

            /*
             * Los bloques reservados deben estar siempre ocupados.
             */
            if (!usedBlocks[SUPERBLOCK]
                    || !usedBlocks[BITMAP_BLOCK]) {
                return false;
            }

            /*
             * Leer bloques ocupados.
             */
            for (int i = 0; i < BLOCK_COUNT; i++) {
                if (usedBlocks[i]) {
                    in.readFully(blocks[i]);
                }
            }
            return true;
        } catch (EOFException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private void writeSuperblock() {

        byte[] data = blocks[SUPERBLOCK];

        writeInt(data, 0, MAGIC);
        writeInt(data, 4, VERSION);
        writeInt(data, 8, BLOCK_SIZE);
        writeInt(data, 12, BLOCK_COUNT);
        writeInt(data, 16, SUPERBLOCK);
        writeInt(data, 20, BITMAP_BLOCK);
        writeInt(data, 24, DATA_START);
        writeInt(data, 28, getTotalBlocks());
    }

    private void writeBitmap() {
        byte[] bitmap = blocks[BITMAP_BLOCK];

        for (int i = 0; i < BLOCK_SIZE; i++) {
            bitmap[i] = 0;
        }

        for (int i = 0; i < BLOCK_COUNT; i++) {
            if (usedBlocks[i]) {
                int byteIndex = i / 8;
                int bitIndex = i % 8;
                bitmap[byteIndex] |= (byte) (1 << bitIndex);
            }
        }
    }

    private void writeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >>> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >>> 24) & 0xFF);
    }

    private void checkBlock(int block) {
        if (block < 0 || block >= BLOCK_COUNT) {
            throw new IllegalArgumentException("Bloque inválido: " + block);
        }
    }

    private void checkDataBlock(int block) {
        checkBlock(block);

        if (block < DATA_START) {
            throw new IllegalArgumentException("Bloque reservado: " + block);
        }
    }
}

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

/**
 *
 * @author Slam
 */
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class TFileSystem {

    public static final int BLOCK_SIZE = 256;
    public static final int BLOCK_COUNT = 256;

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
        save();
    }

    // Bloque 0 - directorios y Bloques 1-255 archivos
    public int allocateBlock() {
        for (int i = 0; i < BLOCK_COUNT; i++) {
            if (!usedBlocks[i]) {
                usedBlocks[i] = true;
                save();
                return i;
            }
        }

        throw new IllegalStateException("No hay bloques libres");
    }

    public void freeBlock(int block) {
        checkBlock(block);
        usedBlocks[block] = false;
        for (int i = 0; i < BLOCK_SIZE; i++) {
            blocks[block][i] = 0;
        }

        save();
    }

    public void writeBlock(int block, byte[] data) {
        checkBlock(block);

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
        checkBlock(block);

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
        return BLOCK_COUNT;
    }

    public int getUsedBlocks() {
        int count = 0;
        for (boolean used : usedBlocks) {
            if (used) {
                count++;
            }
        }
        return count;
    }

    public int getFreeBlocks() {
        return BLOCK_COUNT - getUsedBlocks();
    }

    public void save() {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(persistenceFile))) {
            out.writeInt(1);

            out.writeInt(BLOCK_COUNT);
            out.writeInt(BLOCK_SIZE);

            for (int i = 0; i < BLOCK_COUNT; i++) {
                out.writeBoolean(usedBlocks[i]);
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
            int version = in.readInt();

            if (version != 1) {
                return false;
            }

            int blockCount = in.readInt();
            int blockSize = in.readInt();

            if (blockCount != BLOCK_COUNT || blockSize != BLOCK_SIZE) {
                return false;
            }

            for (int i = 0; i < BLOCK_COUNT; i++) {
                usedBlocks[i] = in.readBoolean();
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

    private void checkBlock(int block) {
        if (block < 0 || block >= BLOCK_COUNT) {
            throw new IllegalArgumentException("Bloque inválido: " + block);
        }
    }
}

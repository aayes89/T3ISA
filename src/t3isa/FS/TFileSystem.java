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
import java.util.ArrayList;
import java.util.List;

public final class TFileSystem {

    public static final int BLOCK_SIZE = 256;
    public static final int BLOCK_COUNT = 256;

    private final boolean[] usedBlocks;
    private final List<TFile> files;

    public TFileSystem() {
        usedBlocks = new boolean[BLOCK_COUNT];
        files = new ArrayList<>();
    }

    public void format() {
        files.clear();

        for (int i = 0; i < usedBlocks.length; i++) {
            usedBlocks[i] = false;
        }
    }

    public boolean exists(String name) {
        return find(name) != null;
    }

    public void create(String name) {
        validateName(name);

        if (exists(name)) {
            throw new IllegalStateException("El archivo ya existe: " + name);
        }

        files.add(new TFile(name));
    }

    public void delete(String name) {
        TFile file = find(name);

        if (file == null) {
            throw new IllegalStateException("Archivo no encontrado: " + name);
        }

        freeBlocks(file);
        files.remove(file);
    }

    public void write(String name, String content) {
        TFile file = find(name);

        if (file == null) {
            throw new IllegalStateException("Archivo no encontrado: " + name);
        }

        if (content == null) {
            content = "";
        }

        int requiredBlocks = (content.length() + BLOCK_SIZE - 1) / BLOCK_SIZE;
        freeBlocks(file);

        if (requiredBlocks > freeBlockCount()) {
            throw new IllegalStateException("Espacio insuficiente");
        }

        file.blocks.clear();
        int position = 0;

        for (int i = 0; i < requiredBlocks; i++) {
            int block = allocateBlock();
            file.blocks.add(block);
            int end = Math.min(position + BLOCK_SIZE, content.length());
            file.data.add(content.substring(position, end));
            position = end;
        }

        file.size = content.length();
    }

    public String read(String name) {
        TFile file = find(name);

        if (file == null) {
            throw new IllegalStateException("Archivo no encontrado: " + name);
        }

        StringBuilder result = new StringBuilder();

        for (String block : file.data) {
            result.append(block);
        }

        return result.toString();
    }

    public TFileInfo[] list() {
        TFileInfo[] result = new TFileInfo[files.size()];

        for (int i = 0; i < files.size(); i++) {
            TFile file = files.get(i);
            result[i] = new TFileInfo(file.name, file.size, file.blocks.size());
        }

        return result;
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

    private TFile find(String name) {
        for (TFile file : files) {
            if (file.name.equals(name)) {
                return file;
            }
        }

        return null;
    }

    private int allocateBlock() {
        for (int i = 0; i < BLOCK_COUNT; i++) {
            if (!usedBlocks[i]) {
                usedBlocks[i] = true;
                return i;
            }
        }

        throw new IllegalStateException("No hay bloques libres");
    }

    private void freeBlocks(TFile file) {
        for (int block : file.blocks) {
            usedBlocks[block] = false;
        }

        file.blocks.clear();
        file.data.clear();
        file.size = 0;
    }

    private int freeBlockCount() {
        return BLOCK_COUNT - getUsedBlocks();
    }

    private void validateName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }

        if (name.length() > 64) {
            throw new IllegalArgumentException("Nombre de archivo demasiado largo");
        }

        if (name.contains("/") || name.contains("\\")) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }
    }

    private static final class TFile {

        private final String name;
        private final List<Integer> blocks;
        private final List<String> data;
        private int size;

        private TFile(String name) {
            this.name = name;
            this.blocks = new ArrayList<>();
            this.data = new ArrayList<>();
            this.size = 0;
        }
    }

    public static final class TFileInfo {

        private final String name;
        private final int size;
        private final int blocks;

        private TFileInfo(String name, int size, int blocks) {
            this.name = name;
            this.size = size;
            this.blocks = blocks;
        }

        public String getName() {
            return name;
        }

        public int getSize() {
            return size;
        }

        public int getBlocks() {
            return blocks;
        }
    }
}

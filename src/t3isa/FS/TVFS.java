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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class TVFS {

    private static final int DIRECTORY_BLOCK = 0;

    private final TFileSystem fs;
    private final List<TNode> nodes;

    public TVFS(TFileSystem fs) {
        if (fs == null) {
            throw new IllegalArgumentException("Filesystem null");
        }

        this.fs = fs;
        this.nodes = new ArrayList<>();

        loadDirectory();
    }

    public void format() {
        fs.format();
        nodes.clear();
        nodes.add(new TNode("/", "", true));
        saveDirectory();
    }

    public boolean exists(String path) {
        return find(normalize(path)) != null;
    }

    public boolean isDirectory(String path) {
        TNode node = find(normalize(path));
        return node != null && node.directory;
    }

    public void mkdir(String path) {
        String normalized = normalize(path);

        if ("/".equals(normalized)) {
            throw new IllegalStateException("El directorio ya existe: /");
        }

        if (exists(normalized)) {
            throw new IllegalStateException("Ya existe: " + normalized);
        }

        String parent = parentPath(normalized);
        TNode parentNode = find(parent);

        if (parentNode == null || !parentNode.directory) {
            throw new IllegalStateException("Directorio no encontrado: " + parent);
        }

        nodes.add(new TNode(normalized, fileName(normalized), true));
        saveDirectory();
    }

    public void create(String path) {
        String normalized = normalize(path);

        if ("/".equals(normalized)) {
            throw new IllegalStateException("Nombre inválido");
        }

        if (exists(normalized)) {
            throw new IllegalStateException("Ya existe: " + normalized);
        }

        String parent = parentPath(normalized);
        TNode parentNode = find(parent);

        if (parentNode == null || !parentNode.directory) {
            throw new IllegalStateException("Directorio no encontrado: " + parent);
        }

        nodes.add(new TNode(normalized, fileName(normalized), false));
        saveDirectory();
    }

    public void write(String path, String content) {
        String normalized = normalize(path);
        TNode node = find(normalized);

        if (node == null) {
            throw new IllegalStateException("Archivo no encontrado: " + normalized);
        }

        if (node.directory) {
            throw new IllegalStateException("Es un directorio: " + normalized);
        }

        if (content == null) {
            content = "";
        }

        freeFileBlocks(node);

        byte[] data = content.getBytes(StandardCharsets.UTF_8);

        int required = (data.length + TFileSystem.BLOCK_SIZE - 1) / TFileSystem.BLOCK_SIZE;
        if (required > fs.getFreeBlocks()) {
            throw new IllegalStateException("Espacio insuficiente");
        }

        int position = 0;
        for (int i = 0; i < required; i++) {
            int block = fs.allocateBlock();
            int length = Math.min(TFileSystem.BLOCK_SIZE, data.length - position);

            byte[] blockData = new byte[length];
            System.arraycopy(data, position, blockData, 0, length);

            fs.writeBlock(block, blockData);
            node.blocks.add(block);
            position += length;
        }

        node.size = data.length;
        saveDirectory();
    }

    public String read(String path) {
        String normalized = normalize(path);
        TNode node = find(normalized);

        if (node == null) {
            throw new IllegalStateException("Archivo no encontrado: " + normalized);
        }

        if (node.directory) {
            throw new IllegalStateException("Es un directorio: " + normalized);
        }

        byte[] result = new byte[node.size];
        int position = 0;

        for (int block : node.blocks) {
            byte[] data = fs.readBlock(block);
            int length = Math.min(data.length, result.length - position);

            System.arraycopy(data, 0, result, position, length);
            position += length;

            if (position >= result.length) {
                break;
            }
        }

        return new String(result, StandardCharsets.UTF_8);
    }

    public void delete(String path) {
        String normalized = normalize(path);

        if ("/".equals(normalized)) {
            throw new IllegalStateException("No se puede eliminar /");
        }

        TNode node = find(normalized);
        if (node == null) {
            throw new IllegalStateException("No encontrado: " + normalized);
        }

        if (node.directory) {
            String prefix = normalized + "/";

            for (TNode other : nodes) {
                if (other != node && other.path.startsWith(prefix)) {
                    throw new IllegalStateException("El directorio no está vacío");
                }
            }
        }

        freeFileBlocks(node);
        nodes.remove(node);
        saveDirectory();
    }

    public TFileInfo[] list(String path) {
        String normalized = normalize(path);
        TNode directory = find(normalized);

        if (directory == null || !directory.directory) {
            throw new IllegalStateException("Directorio no encontrado: " + normalized);
        }

        List<TFileInfo> result = new ArrayList<>();
        String prefix = "/".equals(normalized) ? "/" : normalized + "/";

        for (TNode node : nodes) {
            if (node == directory) {
                continue;
            }

            if (!node.path.startsWith(prefix)) {
                continue;
            }

            String remainder = node.path.substring(prefix.length());
            if (remainder.isEmpty() || remainder.contains("/")) {
                continue;
            }

            result.add(new TFileInfo(node.name, node.path, node.directory, node.size, node.blocks.size()));
        }

        return result.toArray(new TFileInfo[0]);
    }

    public int getTotalBlocks() {
        return fs.getTotalBlocks();
    }

    public int getUsedBlocks() {

        return fs.getUsedBlocks();
    }

    public int getFreeBlocks() {

        return fs.getFreeBlocks();
    }

    private void freeFileBlocks(TNode node) {

        for (int block : node.blocks) {
            fs.freeBlock(block);
        }

        node.blocks.clear();
        node.size = 0;
    }

    private void saveDirectory() {

        /*
         * El directorio se serializa dentro
         * del bloque reservado 0.
         */
        StringBuilder data = new StringBuilder();

        for (TNode node : nodes) {
            data.append(node.directory ? "D" : "F")
                    .append('|')
                    .append(node.path)
                    .append('|')
                    .append(node.name)
                    .append('|')
                    .append(node.size)
                    .append('|');

            for (int i = 0; i < node.blocks.size(); i++) {
                if (i > 0) {
                    data.append(',');
                }

                data.append(node.blocks.get(i));
            }

            data.append('\n');
        }

        byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > TFileSystem.BLOCK_SIZE) {
            throw new IllegalStateException("Directorio raíz lleno");
        }

        if (!fs.isBlockUsed(DIRECTORY_BLOCK)) {
            fs.allocateBlock();
        }

        fs.writeBlock(DIRECTORY_BLOCK, bytes);
    }

    private void loadDirectory() {
        if (!fs.isBlockUsed(DIRECTORY_BLOCK)) {
            nodes.add(new TNode("/", "", true));
            saveDirectory();
            return;
        }

        byte[] bytes = fs.readBlock(DIRECTORY_BLOCK);
        String content = new String(bytes, StandardCharsets.UTF_8).trim();

        if (content.isEmpty()) {
            nodes.add(new TNode("/", "", true));
            saveDirectory();
            return;
        }

        String[] lines = content.split("\\R");
        for (String line : lines) {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 5) {
                continue;
            }

            boolean directory = "D".equals(parts[0]);
            TNode node = new TNode(parts[1], parts[2], directory);
            node.size = Integer.parseInt(parts[3]);

            if (!parts[4].isEmpty()) {
                String[] blockList = parts[4].split(",");
                for (String block : blockList) {
                    node.blocks.add(Integer.parseInt(block));
                }
            }

            nodes.add(node);
        }

        if (find("/") == null) {
            nodes.clear();
            nodes.add(new TNode("/", "", true));
            saveDirectory();
        }
    }

    private TNode find(String path) {
        for (TNode node : nodes) {
            if (node.path.equals(path)) {
                return node;
            }
        }

        return null;
    }

    private String normalize(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Ruta inválida");
        }

        path = path.trim();

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        while (path.contains("//")) {
            path = path.replace("//", "/");
        }

        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    private String parentPath(String path) {
        int index = path.lastIndexOf('/');
        if (index <= 0) {
            return "/";
        }

        return path.substring(0, index);
    }

    private String fileName(String path) {
        int index = path.lastIndexOf('/');
        return path.substring(index + 1);
    }

    private static final class TNode {

        private final String path;
        private final String name;
        private final boolean directory;

        private final List<Integer> blocks;

        private int size;

        private TNode(String path, String name, boolean directory) {
            this.path = path;
            this.name = name;
            this.directory = directory;
            this.blocks = new ArrayList<>();
        }
    }

    public static final class TFileInfo {

        private final String name;
        private final String path;
        private final boolean directory;
        private final int size;
        private final int blocks;

        private TFileInfo(String name, String path, boolean directory, int size, int blocks) {
            this.name = name;
            this.path = path;
            this.directory = directory;
            this.size = size;
            this.blocks = blocks;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public boolean isDirectory() {
            return directory;
        }

        public int getSize() {
            return size;
        }

        public int getBlocks() {
            return blocks;
        }
    }
}

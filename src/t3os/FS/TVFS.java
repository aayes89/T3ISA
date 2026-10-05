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
package t3os.FS;

/**
 *
 * @author Slam
 */
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TVFS {

    /*
     * TFileSystem:
     *
     * 0  superbloque
     * 1  bitmap
     * 2  metadata VFS
     * 3    MAGIC
     * 4..7    VERSION
     * 8..11   NEXT BLOCK
     * 12..15  PAYLOAD SIZE
     * 3..255 = datos
     */
    private static final int DIRECTORY_BLOCK = 2;
    private static final int HEADER_SIZE = 16;
    private static final int MAGIC = 0x54334452; // T3DR 
    private static final int VERSION = 1;

    private final TFileSystem fs;
    private final List<TNode> nodes;
    private final List<Integer> directoryBlocks;

    public TVFS(TFileSystem fs) {
        if (fs == null) {
            throw new IllegalArgumentException("Filesystem null");
        }
        this.fs = fs;
        this.nodes = new ArrayList<>();
        this.directoryBlocks = new ArrayList<>();
        loadDirectory();
    }

    public void format() {
        fs.format();
        nodes.clear();
        directoryBlocks.clear();
        nodes.add(new TNode("/", "", true));
        directoryBlocks.add(DIRECTORY_BLOCK);
        if (!fs.isBlockUsed(DIRECTORY_BLOCK)) {
            int block = fs.allocateBlock();
            if (block != DIRECTORY_BLOCK) {
                fs.freeBlock(block);
                throw new IllegalStateException("No se pudo reservar el bloque VFS: " + DIRECTORY_BLOCK);
            }
        }
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

        validateConsistency();
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

        validateConsistency();
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

        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        int required = (data.length + TFileSystem.BLOCK_SIZE - 1) / TFileSystem.BLOCK_SIZE;

        /*
        * Reservar primero todos los bloques nuevos.
        * Los bloques actuales permanecen intactos hasta
        * que la nueva escritura haya terminado correctamente.
         */
        List<Integer> newBlocks = new ArrayList<>();

        try {
            if (required > fs.getFreeBlocks()) {
                throw new IllegalStateException("Espacio insuficiente");
            }

            for (int i = 0; i < required; i++) {
                newBlocks.add(fs.allocateBlock());
            }

            int position = 0;
            for (int block : newBlocks) {
                int length = Math.min(TFileSystem.BLOCK_SIZE, data.length - position);
                byte[] blockData = new byte[length];

                if (length > 0) {
                    System.arraycopy(data, position, blockData, 0, length);
                    position += length;
                }
                fs.writeBlock(block, blockData);
            }
        } catch (RuntimeException e) {
            // Rollback: liberar únicamente los bloques nuevos.
            for (int block : newBlocks) {
                fs.freeBlock(block);
            }

            throw e;
        }

        /*
        * La nueva copia ya está escrita correctamente.
        * Ahora liberar los bloques anteriores.
         */
        freeFileBlocks(node);

        node.blocks.clear();
        node.blocks.addAll(newBlocks);
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

        if (!node.directory) {
            freeFileBlocks(node);
        }

        nodes.remove(node);
        saveDirectory();

        validateConsistency();
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
        if (directoryBlocks.isEmpty()) {
            directoryBlocks.add(DIRECTORY_BLOCK);
        }

        byte[] data = serializeDirectory();
        int payloadSize = TFileSystem.BLOCK_SIZE - HEADER_SIZE;
        int requiredBlocks = Math.max(1, (data.length + payloadSize - 1) / payloadSize);

        while (directoryBlocks.size() < requiredBlocks) {
            int block = fs.allocateBlock();
            directoryBlocks.add(block);
        }

        while (directoryBlocks.size() > requiredBlocks) {
            int last = directoryBlocks.remove(directoryBlocks.size() - 1);
            fs.freeBlock(last);
        }

        int position = 0;
        for (int i = 0; i < directoryBlocks.size(); i++) {
            int block = directoryBlocks.get(i);
            int next = (i + 1 < directoryBlocks.size()) ? directoryBlocks.get(i + 1) : -1;

            int length = Math.min(payloadSize, data.length - position);
            byte[] blockData = new byte[TFileSystem.BLOCK_SIZE];

            ByteBuffer buffer = ByteBuffer.wrap(blockData);
            buffer.putInt(MAGIC);
            buffer.putInt(VERSION);
            buffer.putInt(next);
            buffer.putInt(length);

            if (length > 0) {
                buffer.put(data, position, length);
                position += length;
            }

            fs.writeBlock(block, blockData);
        }
        validateConsistency();
    }

    private byte[] serializeDirectory() {
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
        return data.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void loadDirectory() {
        nodes.clear();
        directoryBlocks.clear();

        if (!fs.isBlockUsed(DIRECTORY_BLOCK)) {
            nodes.add(new TNode("/", "", true));
            directoryBlocks.add(DIRECTORY_BLOCK);

            int block = fs.allocateBlock();
            if (block != DIRECTORY_BLOCK) {
                fs.freeBlock(block);
                throw new IllegalStateException("No se pudo reservar el bloque VFS: " + DIRECTORY_BLOCK);
            }

            saveDirectory();
            return;
        }

        int block = DIRECTORY_BLOCK;

        ByteArrayOutputStream directoryData = new ByteArrayOutputStream();

        boolean[] visited = new boolean[TFileSystem.BLOCK_COUNT];

        while (block >= 0) {
            if (block < 2 || block >= TFileSystem.BLOCK_COUNT) {
                throw new IllegalStateException("Bloque VFS inválido: " + block);
            }

            if (visited[block]) {
                throw new IllegalStateException("Ciclo detectado en la cadena VFS: " + block);
            }

            visited[block] = true;
            if (!fs.isBlockUsed(block)) {
                throw new IllegalStateException("Bloque VFS no asignado: " + block);
            }

            byte[] blockData = fs.readBlock(block);
            if (blockData.length < HEADER_SIZE) {
                throw new IllegalStateException("Metadatos VFS corruptos");
            }

            ByteBuffer buffer = ByteBuffer.wrap(blockData);

            int magic = buffer.getInt();
            int version = buffer.getInt();
            int next = buffer.getInt();
            int length = buffer.getInt();

            if (magic != MAGIC) {
                throw new IllegalStateException("Magic VFS inválido");
            }

            if (version != VERSION) {
                throw new IllegalStateException("Versión VFS no soportada: " + version);
            }

            if (next != -1 && (next < 2 || next >= TFileSystem.BLOCK_COUNT)) {
                throw new IllegalStateException("Siguiente bloque VFS inválido: " + next);
            }

            if (length < 0 || length > blockData.length - HEADER_SIZE) {
                throw new IllegalStateException("Tamaño de metadatos VFS inválido");
            }

            directoryBlocks.add(block);

            byte[] payload = new byte[length];
            buffer.get(payload);

            directoryData.writeBytes(payload);

            block = next;
        }

        appendDirectoryPayload(directoryData.toByteArray());

        if (find("/") == null) {
            nodes.clear();
            directoryBlocks.clear();

            nodes.add(new TNode("/", "", true));
            directoryBlocks.add(DIRECTORY_BLOCK);

            saveDirectory();
        }
        validateConsistency();
    }

    private void appendDirectoryPayload(byte[] payload) {
        String content = new String(payload, StandardCharsets.UTF_8);
        if (content.isEmpty()) {
            return;
        }

        String[] lines = content.split("\\R");
        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split("\\|", -1);
            if (parts.length < 5) {
                throw new IllegalStateException("Entrada VFS corrupta");
            }

            boolean directory = "D".equals(parts[0]);
            if (!directory && !"F".equals(parts[0])) {
                throw new IllegalStateException("Tipo de nodo VFS inválido");
            }

            TNode node = new TNode(parts[1], parts[2], directory);
            node.size = Integer.parseInt(parts[3]);

            if (!parts[4].isEmpty()) {
                String[] blockList = parts[4].split(",");
                for (String blockString : blockList) {
                    int dataBlock = Integer.parseInt(blockString);
                    if (dataBlock < 3 || dataBlock >= TFileSystem.BLOCK_COUNT) {
                        throw new IllegalStateException("Bloque de archivo inválido: " + dataBlock);
                    }
                    node.blocks.add(dataBlock);
                }
            }
            nodes.add(node);
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

    private void validateConsistency() {
        if (directoryBlocks.isEmpty()) {
            throw new IllegalStateException("VFS sin bloques de directorio");
        }

        if (directoryBlocks.get(0) != DIRECTORY_BLOCK) {
            throw new IllegalStateException("El directorio VFS no comienza en el bloque " + DIRECTORY_BLOCK);
        }

        boolean[] referenced = new boolean[TFileSystem.BLOCK_COUNT];

        // Bloques utilizados por el directorio VFS.
        for (int block : directoryBlocks) {
            if (block < 2 || block >= TFileSystem.BLOCK_COUNT) {
                throw new IllegalStateException("Bloque de directorio inválido: " + block);
            }

            if (referenced[block]) {
                throw new IllegalStateException("Bloque de directorio duplicado: " + block);
            }

            referenced[block] = true;
            if (!fs.isBlockUsed(block)) {
                throw new IllegalStateException("Bloque de directorio no asignado: " + block);
            }
        }

        // Rutas duplicadas y raíz.    
        Set<String> paths = new HashSet<>();
        int rootCount = 0;

        for (TNode node : nodes) {
            if (node.path == null || node.path.isEmpty()) {
                throw new IllegalStateException("Nodo VFS con ruta inválida");
            }

            if (!paths.add(node.path)) {
                throw new IllegalStateException("Ruta VFS duplicada: " + node.path);
            }

            if (node.path.equals("/")) {
                rootCount++;

                if (!node.directory) {
                    throw new IllegalStateException("La raíz VFS no es un directorio");
                }

                if (!node.name.isEmpty()) {
                    throw new IllegalStateException("Nombre inválido para la raíz VFS");
                }
            }

            if (node.size < 0) {
                throw new IllegalStateException("Tamaño negativo: " + node.path);
            }

            // Los directorios no contienen bloques de datos.
            if (node.directory) {
                if (node.size != 0) {
                    throw new IllegalStateException("Directorio con tamaño inválido: " + node.path);
                }

                if (!node.blocks.isEmpty()) {
                    throw new IllegalStateException("Directorio con bloques de datos: " + node.path);
                }

                continue;
            }

            // Comprobar cantidad de bloques del archivo.
            int expectedBlocks = (node.size + TFileSystem.BLOCK_SIZE - 1) / TFileSystem.BLOCK_SIZE;

            if (node.blocks.size() != expectedBlocks) {
                throw new IllegalStateException("Cantidad de bloques inconsistente en: " + node.path);
            }

            // Comprobar bloques del archivo.
            for (int block : node.blocks) {
                if (block < 3 || block >= TFileSystem.BLOCK_COUNT) {
                    throw new IllegalStateException("Bloque de archivo inválido: " + block);
                }

                if (referenced[block]) {
                    throw new IllegalStateException("Bloque compartido o duplicado: " + block);
                }

                referenced[block] = true;
                if (!fs.isBlockUsed(block)) {
                    throw new IllegalStateException("Archivo referencia bloque libre: " + block);
                }
            }
        }

        // Debe existir exactamente una raíz.
        if (rootCount != 1) {
            throw new IllegalStateException("Cantidad de raíces VFS inválida: " + rootCount);
        }

        // Comprobar que cada nodo tenga un padre válido.
        for (TNode node : nodes) {
            if (node.path.equals("/")) {
                continue;
            }

            int lastSlash = node.path.lastIndexOf('/');
            if (lastSlash < 0) {
                throw new IllegalStateException("Ruta VFS inválida: " + node.path);
            }

            String parentPath;

            if (lastSlash == 0) {
                parentPath = "/";
            } else {
                parentPath = node.path.substring(0, lastSlash);
            }

            TNode parent = find(parentPath);
            if (parent == null) {
                throw new IllegalStateException("Padre inexistente para: " + node.path);
            }

            if (!parent.directory) {
                throw new IllegalStateException("El padre no es directorio: " + parentPath);
            }
        }

        // Detectar bloques ocupados pero no referenciados.
        for (int block = 2; block < TFileSystem.BLOCK_COUNT; block++) {
            if (fs.isBlockUsed(block) && !referenced[block]) {
                throw new IllegalStateException("Bloque VFS huérfano: " + block);
            }
        }
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

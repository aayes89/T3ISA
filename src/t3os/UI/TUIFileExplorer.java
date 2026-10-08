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
package t3os.UI;

/**
 *
 * @author Slam
 */
import t3os.FS.TVFS;
import t3os.KERNEL.TKernel;

// Explorador de archivos
public final class TUIFileExplorer extends TUIElement {

    private static final int TITLE_HEIGHT = 24;
    private static final int TOOLBAR_HEIGHT = 28;
    private static final int ROW_HEIGHT = 22;

    private TUI ui;
    private final TKernel kernel;
    private final TVFS vfs;
    private final TUIPopupMenu contextMenu;

    private String currentPath;
    private String selectedPath;
    private boolean selectedDirectory;

    private String clipboardPath;
    private boolean clipboardCut;

    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    private int selectedIndex;
    private long lastClickTime;
    private String lastClickPath;

    // Constructor
    public TUIFileExplorer(int x, int y, int width, int height, TKernel kernel) {
        super(x, y, width, height);
        if (kernel == null) {
            throw new IllegalArgumentException("Kernel no puede ser null");
        }

        this.kernel = kernel;
        this.vfs = kernel.getVFS();

        if (vfs == null) {
            throw new IllegalStateException("VFS no disponible en el kernel");
        }

        currentPath = "/";
        selectedIndex = -1;
        selectedPath = null;
        selectedDirectory = false;
        lastClickTime = 0;
        lastClickPath = null;

        clipboardPath = null;
        clipboardCut = false;
        contextMenu = new TUIPopupMenu(0, 0, 180, 240);
        contextMenu.addItem("Abrir", this::openSelected);
        contextMenu.addItem("Ejecutar", this::executeSelected);
        contextMenu.addItem("Copiar", this::copySelected);
        contextMenu.addItem("Pegar", this::pasteClipboard);
        contextMenu.addItem("Eliminar", this::deleteSelected);
        contextMenu.addItem("Renombrar", this::renameSelected);
        contextMenu.addItem("Nueva carpeta", this::createDirectory);
        contextMenu.addItem("Nuevo archivo", this::createFile);
        contextMenu.addItem("Actualizar", this::refresh);
    }

    // Dibuja la interfaz del explorador
    @Override
    public void draw(TUI ui) {
        this.ui = ui;
        // Ventana
        ui.fillRect(x, y, width, height, 0x00D0D0D0);

        // Barra de título
        ui.fillRect(x, y, width, TITLE_HEIGHT, 0x00008080);

        ui.drawText("T3Explorador", x + 6, y + 4, 0x00FFFFFF);

        // Botón cerrar         
        ui.fillRect(x + width - 22, y + 4, 16, 16, 0x00C0C0C0);

        ui.drawText("X", x + width - 18, y + 4, 0x00000000);

        // Toolbar
        int toolbarY = y + TITLE_HEIGHT;
        ui.fillRect(x, toolbarY, width, TOOLBAR_HEIGHT, 0x00B0B0B0);

        // Botón arriba
        ui.fillRect(x + 4, toolbarY + 4, 24, 20, 0x00D0D0D0);
        ui.drawRect(x + 4, toolbarY + 4, 24, 20, 0x00000000);
        ui.drawText("..", x + 10, toolbarY + 6, 0x00000000);

        // Ruta
        ui.fillRect(x + 34, toolbarY + 4, width - 38, 20, 0x00FFFFFF);
        ui.drawText(currentPath, x + 40, toolbarY + 6, 0x00000000);

        // Listado
        drawFiles(ui);

        if (contextMenu.isOpen()) {
            contextMenu.draw(ui);
        }
    }

    // Dibuja los componentes internos del explorador (contenido)
    private void drawFiles(TUI ui) {
        int listY = y + TITLE_HEIGHT + TOOLBAR_HEIGHT;
        TVFS.TFileInfo[] files;

        try {
            files = vfs.list(currentPath);
        } catch (RuntimeException e) {
            ui.drawText(e.getMessage(), x + 8, listY + 8, 0x00FF0000);
            return;
        }

        for (int i = 0; i < files.length; i++) {
            TVFS.TFileInfo file = files[i];
            int rowY = listY + i * ROW_HEIGHT;
            if (i == selectedIndex) {
                ui.fillRect(x + 2, rowY, width - 4, ROW_HEIGHT, 0x00808080);
            }

            String type = file.isDirectory() ? "[DIR]" : "[FILE]";
            ui.drawText(type, x + 8, rowY + 3, 0x00000000);
            ui.drawText(file.getName(), x + 60, rowY + 3, 0x00000000);

            if (!file.isDirectory()) {
                ui.drawText(String.valueOf(file.getSize()), x + width - 80, rowY + 3, 0x00000000);
            }
        }
    }

    // Procesar evento de movimiento del mouse 
    @Override
    public void mouseMove(int mouseX, int mouseY) {
        if (!dragging) {
            return;
        }
        x = mouseX - dragOffsetX;
        y = mouseY - dragOffsetY;
    }

    // Procesar evento de clic presionado en el mouse
    @Override
    public void mouseDown(int button, int mouseX, int mouseY) {
        // El popup tiene prioridad sobre la ventana.
        if (contextMenu.isOpen()) {
            if (mouseX >= contextMenu.getX() && mouseX < contextMenu.getX()
                    + contextMenu.getWidth() && mouseY >= contextMenu.getY()
                    && mouseY < contextMenu.getY() + contextMenu.getHeight()) {
                contextMenu.mouseDown(button, mouseX, mouseY);
                return;
            }

            // Click fuera del popup.
            contextMenu.close();
        }

        if (!contains(mouseX, mouseY)) {
            return;
        }

        // Click derecho: ejecutar la acción igual que izquierdo.
        if (button == 3) {
            int toolbarY = y + TITLE_HEIGHT;
            int listY = y + TITLE_HEIGHT + TOOLBAR_HEIGHT;
            if (mouseY >= listY) {
                int index = (mouseY - listY) / ROW_HEIGHT;
                selectEntry(index);
            }
            if (contextMenu.isOpen()) {
                contextMenu.close();
            }
            contextMenu.open(mouseX, mouseY);
            return;
        }

        if (button != 1) {
            return;
        }

        // Cerrar
        if (mouseX >= x + width - 24 && mouseY >= y && mouseY < y + TITLE_HEIGHT) {
            setVisible(false);
            return;
        }

        // Arrastrar ventana
        if (mouseY >= y && mouseY < y + TITLE_HEIGHT) {
            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
            return;
        }

        handleClick(mouseX, mouseY);
    }

    // Procesar liberación del clic en el mouse
    @Override
    public void mouseUp(int button) {
        if (contextMenu.isOpen()) {
            contextMenu.mouseUp(button);
            return;
        }

        if (button == 1) {
            dragging = false;
        }
    }

    // Manejar evento de clic del mouse según posición
    private void handleClick(int mouseX, int mouseY) {
        // Botón ".."
        int toolbarY = y + TITLE_HEIGHT;
        if (mouseY >= toolbarY && mouseY < toolbarY + TOOLBAR_HEIGHT && mouseX >= x + 4 && mouseX < x + 28) {
            goParent();
            return;
        }

        // Listado
        int listY = y + TITLE_HEIGHT + TOOLBAR_HEIGHT;
        if (mouseY < listY) {
            return;
        }

        int index = (mouseY - listY) / ROW_HEIGHT;
        openEntry(index);
    }

    // Procesar ejecución de archivo seleccionado según extensión
    private void openEntry(int index) {
        TVFS.TFileInfo[] files;

        try {
            files = vfs.list(currentPath);
        } catch (RuntimeException e) {
            return;
        }

        if (index < 0 || index >= files.length) {
            return;
        }

        TVFS.TFileInfo file = files[index];
        selectEntry(index);

        long now = System.currentTimeMillis();
        boolean doubleClick = file.getPath().equals(lastClickPath) && now - lastClickTime <= 400;

        lastClickTime = now;
        lastClickPath = file.getPath();
        if (!doubleClick) {
            return;
        }

        if (file.isDirectory()) {
            openDirectory(file.getPath());
            return;
        }

        String name = file.getName().toLowerCase();
        if (name.endsWith(".t3i") || name.endsWith(".it3")) {
            executeSelected();
        } else {
            openSelected();
        }
    }

    // Seleccionar archivo o directorio
    private void selectEntry(int index) {

        TVFS.TFileInfo[] files;

        try {
            files = vfs.list(currentPath);
        } catch (RuntimeException e) {
            return;
        }

        if (index < 0 || index >= files.length) {
            selectedPath = null;
            selectedDirectory = false;
            selectedIndex = -1;
            return;
        }

        TVFS.TFileInfo file = files[index];

        selectedIndex = index;
        selectedPath = file.getPath();
        selectedDirectory = file.isDirectory();
    }

    // Restaura parámetros
    public void refresh() {
        selectedIndex = -1;
        selectedPath = null;
        selectedDirectory = false;
    }

    // Cambia el estado de un directorio para indicar que está abierto
    // Obtiene su ruta
    public void openDirectory(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }

        if (!vfs.isDirectory(path)) {
            return;
        }

        currentPath = path;
        refresh();
    }

    // Copia la ruta del elemento seleccionado al portapapeles
    private void copySelected() {
        if (selectedPath == null) {
            return;
        }

        clipboardPath = selectedPath;
        clipboardCut = false;

        System.out.println("Copiado: " + clipboardPath);
    }

    // Toma el elemento en el portapapeles y lo genera en la posición actual
    private void pasteClipboard() {
        if (clipboardPath == null) {
            return;
        }

        try {
            String name = clipboardPath.substring(clipboardPath.lastIndexOf('/') + 1);
            String destination = "/".equals(currentPath) ? "/" + name : currentPath + "/" + name;
            if (destination.equals(clipboardPath)) {
                System.out.println("T3Explorador: origen y destino son iguales");
                return;
            }

            vfs.copy(clipboardPath, destination);
            System.out.println("Pegado: " + destination);
            refresh();
            /*
            * El portapapeles permanece disponible
            * para realizar múltiples copias.
             */
            clipboardCut = false;
        } catch (RuntimeException e) {
            System.out.println("T3Explorador: " + e.getMessage());
        }
    }

    // Elimina el elemento seleccionado
    private void deleteSelected() {
        if (selectedPath == null) {
            return;
        }

        String path = selectedPath;
        try {
            vfs.delete(path);
            selectedPath = null;
            selectedDirectory = false;
            selectedIndex = -1;
            refresh();
            System.out.println("Eliminado: " + path);
        } catch (RuntimeException e) {
            System.out.println("T3Explorador: " + e.getMessage());
        }
    }

    // Renombrar el elemento seleccionado
    private void renameSelected() {
        if (selectedPath == null) {
            return;
        }
        // Crear un menu contextual que espera un texto para el nuevo nombre
        requestName(nuevoNombre -> {
            try {
                // Obtengo ruta del archivo o carpeta seleccionado                
                // Se llama a la función rename con esos parámetros
                kernel.getVFS().rename(selectedPath, nuevoNombre);
                System.out.println("Archivo -> " + selectedPath + ", renombrado a " + nuevoNombre);
                // Actualizo explorador
                refresh();
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
            }
        });
    }

    // Crear un directorio
    private void createDirectory() {
        if (currentPath == null) {
            return;
        }
        // Crear un menu contextual que espera un texto para el nuevo nombre   

        requestName(nuevoNombre -> {
            try {
                // Se llama a la función mkdir con esos parámetros
                String path;
                if (currentPath.equals("/")) {
                    path = currentPath + nuevoNombre;
                } else {
                    path = currentPath + "/" + nuevoNombre;
                }
                // Creo el directorio
                kernel.getVFS().mkdir(path);
                System.out.println("Directorio -> " + nuevoNombre + " creado.");
                // Actualizo explorador
                refresh();
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
            }
        });
    }

    // Crear un archivo
    private void createFile() {
        if (currentPath == null) {
            return;
        }
        // Crear un menu contextual que espera un texto para el nuevo nombre       
        requestName(nuevoNombre -> {
            try {
                // Se llama a la función mkdir con esos parámetros
                String path;
                if (currentPath.equals("/")) {
                    path = "/" + nuevoNombre;
                } else {
                    path = currentPath + "/" + nuevoNombre;
                }

                kernel.getVFS().create(path);
                System.out.println("Archivo -> " + nuevoNombre + " creado.");

                // Actualizo explorador
                refresh();
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
            }
        });
    }

    // Auxiliar para métodos renombrar, mkdir y touch
    private void requestName(NameAction action) {
        if (ui == null || action == null) {
            return;
        }

        TUIInputDialog dialog = new TUIInputDialog(x + 60, y + 80, 320, 130);

        dialog.setTitle("Introducir nombre");
        dialog.setText("");

        dialog.setAcceptAction(nombre -> {
            action.execute(nombre);
            ui.remove(dialog);
        });

        dialog.setCancelAction(() -> ui.remove(dialog));

        ui.add(dialog);
        ui.bringToFront(dialog);

        dialog.getTextArea().setFocused(true);
    }

    // Ingresa a un directorio o inicia el editor según el tipo de archivo    
    private void openSelected() {
        if (selectedPath == null) {
            return;
        }

        if (selectedDirectory) {
            openDirectory(selectedPath);
            return;
        }

        if (ui == null) {
            return;
        }

        try {
            String content = vfs.read(selectedPath);
            TUIEditor editor = new TUIEditor(100, 80, 600, 460, kernel, selectedPath);
            editor.getTextArea().setText(content);

            ui.add(editor);
            ui.bringToFront(editor);

        } catch (RuntimeException e) {
            System.out.println("T3Editor: error abriendo " + selectedPath + ": " + e.getMessage());
        }
    }

    // Ejecuta un programa si la extesión es .t3i o .it3
    private void executeSelected() {
        if (selectedPath == null || selectedDirectory) {
            return;
        }

        String name = selectedPath.toLowerCase();
        if (!name.endsWith(".t3i") && !name.endsWith(".it3")) {
            System.out.println("T3Explorador: no es un ejecutable T3OS: " + selectedPath);
            return;
        }

        try {
            String source = vfs.read(selectedPath);
            if (source == null || source.trim().isEmpty()) {
                System.out.println("T3Explorador: archivo vacío: " + selectedPath);
                return;
            }

            /*
            * Exactamente el mismo mecanismo
            * utilizado por TShell.run().
            *
            * TKernel.createProcess(String)
            * ensambla mediante TAssemblerText
            * y crea el proceso.
             */
            kernel.createProcess(source);
            System.out.println("T3OS: ejecutando " + selectedPath);
            if (kernel.getScheduler().getCurrentProcess() == null) {
                kernel.getScheduler().schedule(kernel.getMachine());
            }
        } catch (RuntimeException e) {
            System.out.println("T3Explorador: error ejecutando " + selectedPath + ": " + e.getMessage());
        }
    }

    // Almacena la ruta de la raíz
    public void goParent() {
        if ("/".equals(currentPath)) {
            return;
        }

        int index = currentPath.lastIndexOf('/');
        if (index <= 0) {
            currentPath = "/";
        } else {
            currentPath = currentPath.substring(0, index);
        }

        refresh();
    }

    // GETTER y SETTERS
    public void setUI(TUI ui) {
        this.ui = ui;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public TVFS getVFS() {
        return vfs;
    }

    public TKernel getKernel() {
        return kernel;
    }

    public boolean isDragging() {
        return dragging;
    }

    @FunctionalInterface
    private interface NameAction {

        void execute(String nombre);
    }
}

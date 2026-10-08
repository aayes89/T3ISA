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

import t3isa.DEVICE.TKeyboardDevice;
import t3os.KERNEL.TKernel;

/**
 *
 * @author Slam
 */
// Editor de texto
public class TUIEditor extends TUIElement {

    private static final int TITLE_HEIGHT = 24;
    private static final int TOOLBAR_HEIGHT = 28;
    //private static final int ROW_HEIGHT = 22;

    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    private final TUITextArea text_area;

    private final String path;
    private final TKernel kernel;

    private boolean modified;

    // Constructor
    public TUIEditor(int x, int y, int width, int height, TKernel kernel, String path) {
        super(x, y, width, height);

        if (kernel == null) {
            throw new IllegalArgumentException("Kernel no puede ser null");
        }
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Ruta de archivo no puede ser null");
        }

        if (!kernel.getVFS().exists(path)) {
            if (path.contains("archivo.txt")) {
                // archivo nuevo, debe ser creado
                kernel.getVFS().create(path);
            } else {
                // Nunca va a llegar aquí pero lo guardamos
                throw new IllegalArgumentException("Archivo no existe: " + path);
            }
        }

        if (kernel.getVFS().isDirectory(path)) {
            throw new IllegalArgumentException("La ruta no es un archivo: " + path);
        }

        this.kernel = kernel;
        this.path = path;

        String content = kernel.getVFS().read(path);

        text_area = new TUITextArea(
                x + 4,
                y + TITLE_HEIGHT + TOOLBAR_HEIGHT + 4,
                width - 8,
                height - TITLE_HEIGHT - TOOLBAR_HEIGHT - 8,
                content,
                0x00000000
        );

        modified = false;
    }

    // Dibujar interfaz del editor y sus componentes
    @Override
    public void draw(TUI ui) {
        if (!visible) {
            return;
        }

        // Ventana
        ui.fillRect(x, y, width, height, 0x00D0D0D0);
        // Barra de título
        ui.fillRect(x, y, width, TITLE_HEIGHT, 0x00008080);

        String title = "T3Editor";

        if (path != null && !path.isEmpty()) {
            int slash = path.lastIndexOf('/');
            if (slash >= 0 && slash + 1 < path.length()) {
                title = path.substring(slash + 1);
            }
        }

        if (modified) {
            title += " *";
        }

        ui.drawText(title, x + 6, y + 4, 0x00FFFFFF);

        // Botón cerrar
        ui.fillRect(x + width - 22, y + 4, 16, 16, 0x00C0C0C0);
        ui.drawText("X", x + width - 18, y + 4, 0x00000000);

        // Toolbar
        int toolbarY = y + TITLE_HEIGHT;
        ui.fillRect(x, toolbarY, width, TOOLBAR_HEIGHT, 0x00B0B0B0);

        // Guardar
        ui.fillRect(x + 4, toolbarY + 4, 64, 20, 0x00D0D0D0);
        ui.drawRect(x + 4, toolbarY + 4, 64, 20, 0x00000000);
        ui.drawText("Guardar", x + 9, toolbarY + 6, 0x00000000);

        // Editor
        text_area.setPosition(x + 4, y + TITLE_HEIGHT + TOOLBAR_HEIGHT + 4);
        text_area.draw(ui);
    }

    // Comando para guardar los cambios en el editor
    public void Guardar() {
        if (!kernel.getVFS().exists(path)) {
            return;
        }

        if (kernel.getVFS().isDirectory(path)) {
            return;
        }

        kernel.getVFS().write(path, text_area.getText());

        modified = false;
    }

    // Procesar evento de movimiento del mouse
    @Override
    public void mouseMove(int mouseX, int mouseY) {
        if (!dragging) {
            return;
        }
        x = mouseX - dragOffsetX;
        y = mouseY - dragOffsetY;

        text_area.setPosition(x + 4, y + TITLE_HEIGHT + TOOLBAR_HEIGHT + 4);
    }

    // Procesar evento de clic presionado en mouse
    @Override
    public void mouseDown(int button, int mouseX, int mouseY) {
        if (!visible || !enabled) {
            return;
        }

        if (button != 1) {
            return;
        }

        if (!contains(mouseX, mouseY)) {
            text_area.setFocused(false);
            return;
        }

        // Cerrar
        if (mouseX >= x + width - 24 && mouseY >= y && mouseY < y + TITLE_HEIGHT) {
            text_area.setFocused(false);
            setVisible(false);
            return;
        }

        // Arrastrar ventana
        if (mouseY >= y && mouseY < y + TITLE_HEIGHT) {
            text_area.setFocused(false);

            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;

            return;
        }

        handleClick(mouseX, mouseY);
    }

    // Procesar evento de liberación del clic en el mouse
    @Override
    public void mouseUp(int button) {
        if (button == 1) {
            dragging = false;
        }
    }

    // Actualizar posición del cursor en el área de texto
    public void refresh() {
        text_area.setPosition(x + 4, y + TITLE_HEIGHT + TOOLBAR_HEIGHT + 4);
    }

    // Manejar el evento de clic sobre el botón guardar o el área de texto
    private void handleClick(int mouseX, int mouseY) {
        int toolbarY = y + TITLE_HEIGHT;

        // Guardar
        if (mouseY >= toolbarY + 4 && mouseY < toolbarY + 24 && mouseX >= x + 4 && mouseX < x + 68) {
            text_area.setFocused(false);
            Guardar();
            return;
        }

        int editorY = y + TITLE_HEIGHT + TOOLBAR_HEIGHT;
        int editorX = x + 4;
        int editorWidth = width - 8;
        int editorHeight = height - TITLE_HEIGHT - TOOLBAR_HEIGHT - 8;

        // Área de edición
        if (mouseX >= editorX && mouseX < editorX + editorWidth && mouseY >= editorY && mouseY < editorY + editorHeight) {
            text_area.mouseDown(1, mouseX, mouseY);
            text_area.setFocused(true);
            return;
        }

        text_area.setFocused(false);
    }

    // Procesar evento de teclado (escribir en área de texto)
    public void keyPressed(TKeyboardDevice.Key key) {
        if (key == null) {
            return;
        }

        if (!text_area.isFocused()) {
            return;
        }

        String textBefore = text_area.getText();
        text_area.keyPressed(key.getCode(), key.getCharacter());

        if (!textBefore.equals(text_area.getText())) {
            modified = true;
        }
    }

    // GETTER y SETTER
    public String getPath() {
        return path;
    }

    public String getText() {
        return text_area.getText();
    }

    public TUITextArea getTextArea() {
        return text_area;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

}

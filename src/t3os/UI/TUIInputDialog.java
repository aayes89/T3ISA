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
 *
 */
import t3isa.DEVICE.TKeyboardDevice;
import t3os.KERNEL.TKernel;

/**
 * Cuadro de entrada de texto reutilizable.
 */
public final class TUIInputDialog extends TUIElement {

    private static final int TITLE_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 22;

    private final TUIEditor editor;
    private final String title;

    private java.util.function.Consumer<String> acceptAction;
    private Runnable cancelAction;

    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public TUIInputDialog(int x, int y, int width, int height, String title, TKernel kernel) {
        super(x, y, width, height);
        if (kernel == null) {
            throw new IllegalArgumentException("Kernel no puede ser null");
        }

        this.title = title == null ? "Entrada" : title;

        editor = new TUIEditor(x + 8, y + TITLE_HEIGHT + 8, width - 16, 28, kernel, null, "");
    }

    @Override
    public void draw(TUI ui) {
        // Fondo
        ui.fillRect(x, y, width, height, 0x00D0D0D0);

        // Barra de título
        ui.fillRect(x, y, width, TITLE_HEIGHT, 0x00008080);
        ui.drawText(title, x + 6, y + 4, 0x00FFFFFF);

        // Campo de texto
        ui.fillRect(x + 8, y + TITLE_HEIGHT + 8, width - 16, 28, 0x00FFFFFF);
        ui.drawRect(x + 8, y + TITLE_HEIGHT + 8, width - 16, 28, 0x00000000);

        editor.draw(ui);

        int buttonY = y + height - BUTTON_HEIGHT - 6;

        // Aceptar
        ui.fillRect(x + width - 150, buttonY, 65, BUTTON_HEIGHT, 0x00C0C0C0);
        ui.drawRect(x + width - 150, buttonY, 65, BUTTON_HEIGHT, 0x00000000);
        ui.drawText("Aceptar", x + width - 144, buttonY + 4, 0x00000000);

        // Cancelar
        ui.fillRect(x + width - 78, buttonY, 65, BUTTON_HEIGHT, 0x00C0C0C0);
        ui.drawRect(x + width - 78, buttonY, 65, BUTTON_HEIGHT, 0x00000000);
        ui.drawText("Cancelar", x + width - 72, buttonY + 4, 0x00000000);
    }

    @Override
    public void mouseMove(int mouseX, int mouseY) {
        if (dragging) {
            x = mouseX - dragOffsetX;
            y = mouseY - dragOffsetY;

            editor.setPosition(x + 8, y + TITLE_HEIGHT + 8);
            return;
        }

        editor.mouseMove(mouseX, mouseY);
    }

    @Override
    public void mouseDown(int button, int mouseX, int mouseY) {
        if (button != 1) {
            return;
        }

        // Arrastrar desde la barra de título
        if (mouseY >= y && mouseY < y + TITLE_HEIGHT) {
            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
            return;
        }

        int buttonY = y + height - BUTTON_HEIGHT - 6;

        // Aceptar
        if (mouseX >= x + width - 150 && mouseX < x + width - 85 && mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT) {
            accept();
            return;
        }

        // Cancelar
        if (mouseX >= x + width - 78 && mouseX < x + width - 13 && mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT) {
            cancel();
            return;
        }

        // Campo de texto
        if (mouseX >= x + 8 && mouseX < x + width - 8 && mouseY >= y + TITLE_HEIGHT + 8 && mouseY < y + TITLE_HEIGHT + 36) {
            editor.mouseDown(button, mouseX, mouseY);
        }
    }

    @Override
    public void mouseUp(int button) {
        if (button == 1) {
            dragging = false;
            editor.mouseUp(button);
        }
    }

    private void accept() {
        String value = editor.getTextArea().getText();
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        if (acceptAction != null) {
            acceptAction.accept(value.trim());
        }
    }

    private void cancel() {
        if (cancelAction != null) {
            cancelAction.run();
        }

        setVisible(false);
    }

    public void setAcceptAction(java.util.function.Consumer<String> action) {
        this.acceptAction = action;
    }

    public void setCancelAction(Runnable action) {
        this.cancelAction = action;
    }

    public String getText() {
        return editor.getTextArea().getText();
    }

    public TUIEditor getEditor() {
        return editor;
    }

    public boolean hasFocusedEditor() {
        return editor.getTextArea().isFocused();
    }

    public void keyPressed(TKeyboardDevice.Key key) {
        editor.keyPressed(key);
    }
}

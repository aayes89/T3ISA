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
import java.util.function.Consumer;
import t3isa.DEVICE.TKeyboardDevice;

/**
 * Cuadro de entrada de texto reutilizable.
 */
public final class TUIInputDialog extends TUIElement {

    private static final int TITLE_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 22;

    private final TUITextArea textArea;

    private String title = "Entrada";
    private Consumer<String> acceptAction;
    private Runnable cancelAction;

    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public TUIInputDialog(int x, int y, int width, int height) {
        super(x, y, width, height);
        textArea = new TUITextArea(x + 8, y + TITLE_HEIGHT + 8, width - 16, 28, "", 0x00000000);
    }

    @Override
    public void draw(TUI ui) {
        if (!visible) {
            return;
        }

        ui.fillRect(x, y, width, height, 0x00D0D0D0);

        ui.fillRect(x, y, width, TITLE_HEIGHT, 0x00008080);
        ui.drawText(title, x + 6, y + 4, 0x00FFFFFF);

        // El propio TUITextArea dibuja el campo y su borde.
        textArea.draw(ui);

        int buttonY = y + height - BUTTON_HEIGHT - 6;

        ui.fillRect(x + width - 150, buttonY, 65, BUTTON_HEIGHT, 0x00C0C0C0);
        ui.drawRect(x + width - 150, buttonY, 65, BUTTON_HEIGHT, 0x00000000);
        ui.drawText("Aceptar", x + width - 144, buttonY + 4, 0x00000000);

        ui.fillRect(x + width - 78, buttonY, 65, BUTTON_HEIGHT, 0x00C0C0C0);
        ui.drawRect(x + width - 78, buttonY, 65, BUTTON_HEIGHT, 0x00000000);
        ui.drawText("Cancelar", x + width - 72, buttonY + 4, 0x00000000);
    }

    @Override
    public void setPosition(int x, int y) {
        super.setPosition(x, y);
        updateTextAreaPosition();
    }

    private void updateTextAreaPosition() {
        textArea.setPosition(x + 8, y + TITLE_HEIGHT + 8);
    }

    @Override
    public void mouseMove(int mouseX, int mouseY) {
        if (dragging) {
            setPosition(mouseX - dragOffsetX, mouseY - dragOffsetY);
        }
    }

    @Override
    public void mouseDown(int button, int mouseX, int mouseY) {
        if (!visible || !enabled || button != 1) {
            return;
        }

        // Arrastrar desde la barra de título.
        if (mouseY >= y && mouseY < y + TITLE_HEIGHT) {
            textArea.setFocused(false);
            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
            return;
        }

        int buttonY = y + height - BUTTON_HEIGHT - 6;

        // Aceptar.
        if (mouseX >= x + width - 150 && mouseX < x + width - 85 && mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT) {
            accept();
            return;
        }

        // Cancelar.
        if (mouseX >= x + width - 78 && mouseX < x + width - 13 && mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT) {
            cancel();
            return;
        }

        // Campo de entrada.
        if (textArea.contains(mouseX, mouseY)) {
            textArea.mouseDown(button, mouseX, mouseY);
        } else {
            textArea.setFocused(false);
        }
    }

    @Override
    public void mouseUp(int button) {
        if (button == 1) {
            dragging = false;
        }
    }

    private void accept() {
        String value = textArea.getText();

        if (value == null || value.trim().isEmpty()) {
            return;
        }

        if (acceptAction != null) {
            acceptAction.accept(value.trim());
        }
    }

    private void cancel() {
        textArea.setFocused(false);

        if (cancelAction != null) {
            cancelAction.run();
        } else {
            setVisible(false);
        }
    }

    public void setTitle(String title) {
        this.title = title == null ? "Entrada" : title;
    }

    public String getTitle() {
        return title;
    }

    public void setText(String text) {
        textArea.setText(text);
    }

    public String getText() {
        return textArea.getText();
    }

    public void setAcceptAction(Consumer<String> action) {
        acceptAction = action;
    }

    public void setCancelAction(Runnable action) {
        cancelAction = action;
    }

    public boolean hasFocusedEditor() {
        return textArea.isFocused();
    }

    public TUITextArea getTextArea() {
        return textArea;
    }

    public void keyPressed(TKeyboardDevice.Key key) {
        if (key == null || !textArea.isFocused()) {
            return;
        }

        textArea.keyPressed(key.getCode(), key.getCharacter());
    }
}

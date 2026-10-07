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
import java.util.ArrayList;
import java.util.List;

// Componente Barra de tareas
public final class TUITaskbar extends TUIElement {

    private final List<TUIButton> buttons;

    private int backgroundColor;
    private int borderColor;

    private final TUIButton startButton;

    // Constructor
    public TUITaskbar(int x, int y, int width, int height) {
        super(x, y, width, height);
        buttons = new ArrayList<>();
        backgroundColor = 0x00303030;
        borderColor = 0x00000000;
        // Boton de inicio
        startButton = new TUIButton(x + 4, y + 4, 80, height - 8, "Start");
    }

    // Dibujar en pantalla
    @Override
    public void draw(TUI ui) {
        ui.fillRect(x, y, width, height, backgroundColor);
        ui.drawRect(x, y, width, height, borderColor);

        startButton.draw(ui);

        for (TUIButton button : buttons) {
            if (!button.isVisible()) {
                continue;
            }

            button.draw(ui);
        }
    }

    // Capturar eventos de mouse
    @Override
    public void mouseMove(int mouseX, int mouseY) {
        startButton.mouseMove(mouseX, mouseY);

        for (TUIButton button : buttons) {
            button.mouseMove(mouseX, mouseY);
        }
    }

    @Override
    public void mouseDown(int button, int mouseX, int mouseY) {
        if (button != 1 && button != 3) {
            return;
        }
        if (startButton.contains(mouseX, mouseY)) {
            startButton.mouseDown(button, mouseX, mouseY);
            return;
        }
        for (TUIButton item : buttons) {
            if (!item.contains(mouseX, mouseY)) {
                continue;
            }
            item.mouseDown(button, mouseX, mouseY);
            return;
        }
    }

    @Override
    public void mouseUp(int button) {
        if (button != 1) {
            return;
        }

        startButton.mouseUp(button);

        for (TUIButton item : buttons) {
            item.mouseUp(button);
        }
    }

    // Añadir boton a la barra
    public void addButton(TUIButton button) {
        if (button == null) {
            throw new IllegalArgumentException("Botón no puede ser null");
        }

        buttons.add(button);
    }

    // Eliminar boton de la barra
    public void removeButton(TUIButton button) {
        buttons.remove(button);
    }

    // Getter y Setters
    public TUIButton getStartButton() {
        return startButton;
    }

    public void setStartAction(Runnable action) {
        startButton.setAction(action);
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int color) {
        backgroundColor = color;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int color) {
        borderColor = color;
    }
}

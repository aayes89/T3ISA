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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Slam
 */
// Componente de ventana
public final class TUIWindow extends TUIElement {

    private String title;
    private static final int TITLE_HEIGHT = 24;

    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private final List<TUIElement> children;

    // Constructor
    public TUIWindow(int x, int y, int width, int height, String title) {
        super(x, y, width, height);
        this.title = title;
        this.children = new ArrayList<>();
    }

    // Dibujar ventana y sus componentes en pantalla
    @Override
    public void draw(TUI ui) {
        // Fondo
        ui.fillRect(x, y, width, height, 0x00D0D0D0);

        // Barra de título
        ui.fillRect(x, y, width, 24, 0x00008080);

        // Marco
        ui.drawRect(x, y, width, height, 0x00000000);

        // Título
        ui.drawText(title, x + 6, y + 4, 0x00FFFFFF);

        // Componentes internos
        for (TUIElement child : children) {
            if (!child.isVisible()) {
                continue;
            }
            child.draw(ui);
        }
    }

    // Captura de eventos de mouse
    @Override
    public void mouseMove(int mouseX, int mouseY) {
        if (!dragging) {
            return;
        }
        x = mouseX - dragOffsetX;
        y = mouseY - dragOffsetY;
    }

    @Override
    public void mouseDown(int button, int mouseX, int mouseY) {
        if (button != 1) {
            return;
        }
        if (mouseY < y || mouseY >= y + TITLE_HEIGHT) {
            return;
        }
        dragging = true;
        dragOffsetX = mouseX - x;
        dragOffsetY = mouseY - y;
    }

    @Override
    public void mouseUp(int button) {
        if (button == 1) {
            dragging = false;
        }
    }

    // Añadir elemento a la ventana
    public void add(TUIElement element) {
        if (element == null) {
            throw new IllegalArgumentException("Elemento UI no puede ser null");
        }
        children.add(element);
    }

    // Eliminar elemento de la ventana
    public void remove(TUIElement element) {
        children.remove(element);
    }

    // Getter y Setters
    public int getChildCount() {
        return children.size();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isDragging() {
        return dragging;
    }
}
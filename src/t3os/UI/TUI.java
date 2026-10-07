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
import t3isa.HARDWARE.TMachine;

public final class TUI {

    private final List<TUIElement> elements;

    private int mouseX;
    private int mouseY;
    private int mouseButtons;
    TMachine machine;
    private TUIElement mouseCapture;
    private TUIElement contextElement;
    private TUIPopupMenu popupMenu;

    public TUI(TMachine machine) {
        elements = new ArrayList<>();

        mouseX = 0;
        mouseY = 0;
        mouseButtons = 0;
        mouseCapture = null;
        if (machine == null) {
            throw new IllegalArgumentException("Machine no puede ser null");
        }
        this.machine = machine;
        this.popupMenu = null;
    }

    public void add(TUIElement element) {
        if (element == null) {
            throw new IllegalArgumentException("Elemento UI no puede ser null");
        }

        elements.add(element);
    }

    public void remove(TUIElement element) {
        if (mouseCapture == element) {
            mouseCapture = null;
        }

        if (contextElement == element) {
            contextElement = null;
        }

        if (popupMenu == element) {
            popupMenu = null;
        }

        elements.remove(element);
    }

    public void setPopupMenu(TUIPopupMenu popupMenu) {
        if (this.popupMenu != null) {
            elements.remove(this.popupMenu);
        }
        this.popupMenu = popupMenu;
        if (popupMenu != null) {
            add(popupMenu);
            bringToFront(popupMenu);
        }
    }

    public TUIPopupMenu getPopupMenu() {
        return popupMenu;
    }

    public void openPopup(int x, int y) {
        if (popupMenu == null) {
            return;
        }
        popupMenu.open(x, y);
        bringToFront(popupMenu);
    }

    public void closePopup() {
        if (popupMenu != null) {
            popupMenu.close();
        }
        contextElement = null;
    }

    public void draw() {
        for (TUIElement element : elements) {
            if (!element.isVisible()) {
                continue;
            }
            element.draw(this);
        }
    }

    public void updateMouse(int x, int y, int buttons) {
        int previousButtons = mouseButtons;

        mouseX = x;
        mouseY = y;
        mouseButtons = buttons;

        // Elemento que tiene captura del botón izquierdo.
        if (mouseCapture != null) {
            mouseCapture.mouseMove(mouseX, mouseY);
            if ((buttons & 1) == 0 && (previousButtons & 1) != 0) {
                mouseCapture.mouseUp(1);
                mouseCapture = null;
            }

            return;
        }

        // Clic derecho.
        if ((buttons & 4) != 0 && (previousButtons & 4) == 0) {
            TUIElement target = null;
            for (int i = elements.size() - 1; i >= 0; i--) {
                TUIElement element = elements.get(i);
                if (!element.isVisible() || !element.isEnabled()) {
                    continue;
                }

                if (!element.contains(mouseX, mouseY)) {
                    continue;
                }

                target = element;
                break;
            }

            // Si ya había un popup y se hizo clic fuera de él, cerrarlo.
            if (popupMenu != null && popupMenu.isOpen() && target != popupMenu) {
                closePopup();
            }

            contextElement = target;

            // El popup no recibe su propio clic derecho.
            if (target != null && target != popupMenu) {
                target.mouseDown(3, mouseX, mouseY);
            }
            return;
        }

        // Buscar el elemento superior.
        TUIElement target = null;
        for (int i = elements.size() - 1; i >= 0; i--) {
            TUIElement element = elements.get(i);
            if (!element.isVisible() || !element.isEnabled()) {
                continue;
            }

            if (!element.contains(mouseX, mouseY)) {
                continue;
            }

            target = element;
            break;
        }

        // Movimiento.
        if (target != null) {
            target.mouseMove(mouseX, mouseY);
        }

        // Botón izquierdo: transición UP -> DOWN.
        if ((buttons & 1) != 0 && (previousButtons & 1) == 0) {
            if (target == null) {
                return;
            }

            // El escritorio siempre permanece debajo de los demás elementos.
            if (!(target instanceof TUIDesktop)) {
                bringToFront(target);
            }

            target.mouseDown(1, mouseX, mouseY);
            // El escritorio no necesita captura.
            if (!(target instanceof TUIDesktop)) {
                mouseCapture = target;
            }
        }
    }

    public void bringToFront(TUIElement element) {
        if (!elements.remove(element)) {
            return;
        }

        elements.add(element);
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public int getMouseButtons() {
        return mouseButtons;
    }

    void fillRect(int x, int y, int width, int height, int color) {
        machine.graphics(5, x, y, width, height, color);
    }

    void drawRect(int x, int y, int width, int height, int color) {
        machine.graphics(4, x, y, width, height, color);
    }

    void drawText(String text, int x, int y, int color) {
        if (text == null) {
            return;
        }

        int cursorX = x;
        for (int i = 0; i < text.length(); i++) {
            machine.drawChar(text.charAt(i), cursorX, y, 1, color, -1);
            cursorX += 8;
        }
    }
}

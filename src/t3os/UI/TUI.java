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
import t3isa.DEVICE.TKeyboardDevice;
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

    // Constructor
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

    // Captura de eventos del teclado
    public void updateKeyboard() {
        TKeyboardDevice keyboard = machine.getKeyboardDevice();
        while (keyboard.hasKey()) {
            TKeyboardDevice.Key key = keyboard.poll();

            if (key == null) {
                continue;
            }
            /*
            * Buscar desde el elemento superior hacia atrás.
            * El último elemento de la lista es el que está
            * visualmente al frente.
             */
            for (int i = elements.size() - 1; i >= 0; i--) {
                TUIElement element = elements.get(i);

                if (!element.isVisible() || !element.isEnabled()) {
                    continue;
                }

                // Integración con InputDialog
                if (element instanceof TUIInputDialog) {
                    TUIInputDialog dialog = (TUIInputDialog) element;
                    if (!dialog.hasFocusedEditor()) {
                        continue;
                    }

                    dialog.keyPressed(key);
                    break;
                }

                // Integración con Editor
                if (element instanceof TUIEditor) {
                    TUIEditor editor = (TUIEditor) element;
                    if (!editor.getTextArea().isFocused()) {
                        continue;
                    }

                    editor.keyPressed(key);
                    break;
                }

                // Integración con Terminal
                if (element instanceof TUITerminal terminal && terminal.hasKeyboardFocus()) {
                    terminal.keyPressed(key);
                    break;
                }
            }
        }
    }

    // Añadir componentes al UI
    public void add(TUIElement element) {
        if (element == null) {
            throw new IllegalArgumentException("Elemento UI no puede ser null");
        }

        elements.add(element);
    }

    // Eliminar un componente del UI
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

    // Dibujar elementos en el UI
    public void draw() {
        for (TUIElement element : elements) {
            if (!element.isVisible()) {
                continue;
            }
            element.draw(this);
        }
    }

    // Capturar y procesar eventos del mouse
    public void updateMouse(int x, int y, int buttons) {
        int previousButtons = mouseButtons;

        mouseX = x;
        mouseY = y;
        mouseButtons = buttons;

        boolean leftPressed = (buttons & 1) != 0 && (previousButtons & 1) == 0;
        boolean leftReleased = (buttons & 1) == 0 && (previousButtons & 1) != 0;
        boolean rightPressed = (buttons & 4) != 0 && (previousButtons & 4) == 0;

        // Gestionar la captura del ratón.
        if (mouseCapture != null) {
            if (!mouseCapture.isVisible() || !mouseCapture.isEnabled()) {
                mouseCapture = null;
            } else {
                TUIElement captured = mouseCapture;
                captured.mouseMove(mouseX, mouseY);

                if (leftReleased) {
                    mouseCapture = null;
                    captured.mouseUp(1);
                } else {
                    return;
                }
            }
        }

        // Buscar el elemento superior situado bajo el cursor.
        TUIElement target = null;

        for (int i = elements.size() - 1; i >= 0; i--) {
            TUIElement element = elements.get(i);

            if (!element.isVisible() || !element.isEnabled()) {
                continue;
            }

            if (element.contains(mouseX, mouseY)) {
                target = element;
                break;
            }
        }

        // Procesar el clic derecho.
        if (rightPressed) {
            if (popupMenu != null && popupMenu.isOpen() && target != popupMenu) {
                closePopup();
            }

            contextElement = target;

            if (target != null && target != popupMenu) {
                if (!(target instanceof TUIDesktop)) {
                    bringToFront(target);
                }

                target.mouseDown(3, mouseX, mouseY);
            }

            return;
        }

        // Cerrar el popup con clic izquierdo fuera de él.
        if (leftPressed && popupMenu != null && popupMenu.isOpen() && target != popupMenu) {
            closePopup();

            // Volver a determinar el elemento superior.
            target = null;

            for (int i = elements.size() - 1; i >= 0; i--) {
                TUIElement element = elements.get(i);

                if (!element.isVisible() || !element.isEnabled()) {
                    continue;
                }

                if (element.contains(mouseX, mouseY)) {
                    target = element;
                    break;
                }
            }
        }

        // Notificar movimiento.
        if (target != null) {
            target.mouseMove(mouseX, mouseY);
        }

        // Procesar la pulsación izquierda.
        if (leftPressed) {
            if (target == null) {
                return;
            }

            if (!(target instanceof TUIDesktop)) {
                bringToFront(target);
            }

            target.mouseDown(1, mouseX, mouseY);

            if (!(target instanceof TUIDesktop)) {
                mouseCapture = target;
            }
        }

        // Entregar la liberación al escritorio si no hubo captura.
        if (leftReleased && mouseCapture == null) {
            for (int i = elements.size() - 1; i >= 0; i--) {
                TUIElement element = elements.get(i);

                if (element instanceof TUIDesktop && element.isVisible() && element.isEnabled()) {
                    element.mouseUp(1);
                    break;
                }
            }
        }
    }

    // Poner delante un elemento determinado
    public void bringToFront(TUIElement element) {
        if (!elements.remove(element)) {
            return;
        }

        elements.add(element);
    }

    // Abrir un menu popup en la posición x e y
    public void openPopup(int x, int y) {
        if (popupMenu == null) {
            return;
        }
        popupMenu.open(x, y);
        bringToFront(popupMenu);
    }

    // Cerrar el menu popup activo
    public void closePopup() {
        if (popupMenu != null) {
            popupMenu.close();
        }
        contextElement = null;
    }

    // Auxiliar para pintar un rectángulo relleno en pantalla
    void fillRect(int x, int y, int width, int height, int color) {
        machine.graphics(5, x, y, width, height, color, 0);
    }

    // Auxiliar para dibujar contorno de un rectángulo en pantalla
    void drawRect(int x, int y, int width, int height, int color) {
        machine.graphics(4, x, y, width, height, color, 0);
    }

    // Auxiliar para imprimir texto en pantalla
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

    // GETTERS y SETTERS
    // Establecer un Menu Popup
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

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public int getMouseButtons() {
        return mouseButtons;
    }

}

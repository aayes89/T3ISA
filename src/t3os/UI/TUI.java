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

public final class TUI {

    private final List<TUIElement> elements;

    private int mouseX;
    private int mouseY;
    private int mouseButtons;

    public TUI() {
        elements = new ArrayList<>();

        mouseX = 0;
        mouseY = 0;
        mouseButtons = 0;
    }

    public void add(TUIElement element) {

        if (element == null) {
            throw new IllegalArgumentException("Elemento UI no puede ser null");
        }

        elements.add(element);
    }

    public void remove(TUIElement element) {
        elements.remove(element);
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

        for (TUIElement element : elements) {
            if (!element.isVisible() || !element.isEnabled()) {
                continue;
            }

            boolean inside = element.contains(mouseX, mouseY);
            if (inside) {
                element.mouseMove(mouseX, mouseY);

                if ((buttons & 1) != 0 && (previousButtons & 1) == 0) {
                    element.mouseDown(1);
                }

                if ((buttons & 1) == 0 && (previousButtons & 1) != 0) {
                    element.mouseUp(1);
                }
            }
        }
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

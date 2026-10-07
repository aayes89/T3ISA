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
public final class TUIButton extends TUIElement {

    private String text;

    private boolean hovered;
    private boolean pressed;

    private Runnable action;

    public TUIButton(int x, int y, int width, int height, String text) {
        super(x, y, width, height);
        this.text = text;
    }

    @Override
    public void draw(TUI ui) {

        if (pressed) {
            ui.fillRect(x + 2, y + 2, width - 2, height - 2, 0x00808080);
        } else if (hovered) {
            ui.fillRect(x, y, width, height, 0x00C0C0C0);
        } else {
            ui.fillRect(x, y, width, height, 0x00A0A0A0);
        }

        ui.drawRect(x, y, width, height, 0x00000000);
        ui.drawText(text, x + 5, y + height / 2, 0x00000000);
    }

    @Override
    public void mouseMove(int mouseX, int mouseY) {
        hovered = contains(mouseX, mouseY);
    }

    @Override
    public void mouseDown(int button, int mouseX, int mouseY) {
        if (!contains(mouseX, mouseY)) {
            return;
        }
        if (button == 1) {
            pressed = true;
            return;
        }
        if (button == 3) {
            if (action != null) {
                action.run();
            }
        }
    }

    @Override
    public void mouseUp(int button) {
        if (button != 1) {
            return;
        }
        boolean click = pressed;
        pressed = false;
        if (click && action != null) {
            action.run();
        }
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setAction(Runnable action) {
        this.action = action;
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean isPressed() {
        return pressed;
    }

}

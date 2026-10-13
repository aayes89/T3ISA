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
package t3os.APPS.Browser.Rendering;

/**
 *
 * @author Slam
 */
import java.awt.Color;
import t3os.APPS.Browser.T3RenderContext;

import t3os.UI.TUI;

public class T3Painter {

    private static final int CHAR_WIDTH = 8;

    private final TUI ui;
    private final T3RenderContext context;
    private int scrollY;

    public T3Painter(TUI ui, T3RenderContext context) {
        this.ui = ui;
        this.context = context;
        this.scrollY = 0;
    }

    public void setScrollY(int scrollY) {
        this.scrollY = Math.max(0, scrollY);
    }

    public void paint(T3LayoutBox box) {
        if (box == null) {
            return;
        }

        // Aplicamos el scroll y calculamos posición absoluta en pantalla
        int screenX = box.x + context.getX();
        int screenY = box.y - scrollY + context.getY();

        // Culling (No dibujar si está completamente fuera de la vista)
        if (screenY + box.height < context.getY() || screenY > context.getBottom()) {
            return;
        }

        if (box.isText && box.textContent != null && !box.textContent.isEmpty()) {
            int color = box.style.getColor() != null ? toRGB(box.style.getColor()) : context.getColor();
            drawTextClipped(box.textContent, screenX, screenY, color);
        } else {
            // Dibujar fondo
            if (box.style.getBackgroundColor() != null && box.width > 0 && box.height > 0) {
                fillRectClipped(screenX, screenY, box.width, box.height, toRGB(box.style.getBackgroundColor()));
            }

            // Dibujar bordes
            int borderWidth = box.style.getBorderWidth();
            if (borderWidth > 0 && box.width > 0 && box.height > 0) {
                int borderColor = toRGB(box.style.getBorderColor());
                for (int i = 0; i < borderWidth; i++) {
                    int w = box.width - i * 2;
                    int h = box.height - i * 2;
                    if (w > 0 && h > 0) {
                        drawRectClipped(screenX + i, screenY + i, w, h, borderColor);
                    }
                }
            }

            // Dibujar imagen si existe
            if (box.image != null) {
                ui.drawImagePixels(screenX, screenY, box.width, box.height, box.image.getPixels());
            }
        }

        // Pintar recursivamente los hijos
        for (T3LayoutBox child : box.children) {
            paint(child);
        }
    }

    private int toRGB(Color color) {
        return color == null ? 0 : color.getRGB() & 0xFFFFFF;
    }

    private void fillRectClipped(int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int left = Math.max(x, context.getX());
        int top = Math.max(y, context.getY());
        int right = Math.min(x + width, context.getRight());
        int bottom = Math.min(y + height, context.getBottom());

        if (right > left && bottom > top) {
            ui.fillRect(left, top, right - left, bottom - top, color);
        }
    }

    private void drawRectClipped(int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int left = Math.max(x, context.getX());
        int top = Math.max(y, context.getY());
        int right = Math.min(x + width, context.getRight());
        int bottom = Math.min(y + height, context.getBottom());

        if (right > left && bottom > top) {
            ui.drawRect(left, top, right - left, bottom - top, color);
        }
    }

    private void drawTextClipped(String text, int x, int y, int color) {
        if (text == null || y < context.getY() || y >= context.getBottom()) {
            return;
        }

        int left = context.getX();
        int right = context.getRight();

        for (int i = 0; i < text.length(); i++) {
            int charX = x + i * CHAR_WIDTH;
            if (charX >= left && charX + CHAR_WIDTH <= right) {
                ui.drawText(String.valueOf(text.charAt(i)), charX, y, color);
            }
        }
    }
}

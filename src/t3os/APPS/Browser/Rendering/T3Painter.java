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

    //Constructor
    public T3Painter(TUI ui, T3RenderContext context) {
        this.ui = ui;
        this.context = context;
        this.scrollY = 0;
    }

    public void setScrollY(int scrollY) {
        this.scrollY = Math.max(0, scrollY);
    }

    // Dibujar la caja
    public void paint(T3LayoutBox box) {
        if (box == null) {
            return;
        }

        int screenX = box.x + context.getX();
        int screenY = box.y - scrollY + context.getY();

        if (screenY + box.height < context.getY() || screenY > context.getBottom()) {
            return;
        }

        // Dibujar múltiples líneas
        if (box.isText && box.textLines != null && !box.textLines.isEmpty()) {
            int color = box.style.getColor() != null ? toRGB(box.style.getColor()) : context.getColor();
            int lineY = screenY;

            for (String line : box.textLines) {
                drawTextClipped(line, screenX, lineY, color);
                lineY += 16; // Avanzar LINE_HEIGHT píxeles hacia abajo
            }
        } else {
            if (box.style.getBackgroundColor() != null && box.width > 0 && box.height > 0) {
                fillRectClipped(screenX, screenY, box.width, box.height, toRGB(box.style.getBackgroundColor()));
            }

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
            // Dibujar el texto de los Inputs
            if ("input".equals(box.node.getTagName())) {
                String val = box.node.getAttributes().getOrDefault("value", "");
                String type = box.node.getAttributes().getOrDefault("type", "text");
                int textColor = context.getColor(); // Negro por defecto

                // Si es un botón, centrar el texto aproximadamente
                if ("submit".equals(type) || "button".equals(type)) {
                    int txtX = screenX + (box.width - (val.length() * CHAR_WIDTH)) / 2;
                    drawTextClipped(val, Math.max(screenX + 2, txtX), screenY + 4, textColor);
                } else {
                    drawTextClipped(val, screenX + 4, screenY + 4, textColor);
                }
            }

            if (box.image != null) {
                drawImageClipped(box, screenX, screenY);
            }
        }

        for (T3LayoutBox child : box.children) {
            paint(child);
        }
    }

    // Auxiliares para dibujar
    private void drawImageClipped(T3LayoutBox box, int screenX, int screenY) {
        int imgW = box.image.getWidth();
        int imgH = box.image.getHeight();
        int reqW = box.width;
        int reqH = box.height;

        if (reqW <= 0 || reqH <= 0) {
            return;
        }

        // Escalar la imagen al tamaño de la caja calculada por LayoutEngine
        int[] pixels = box.image.getPixels();
        if (reqW != imgW || reqH != imgH) {
            pixels = scaleNearestNeighbor(pixels, imgW, imgH, reqW, reqH);
        }

        // Calcular los límites de recorte respecto al Viewport
        int startX = Math.max(0, context.getX() - screenX);
        int startY = Math.max(0, context.getY() - screenY);
        int endX = Math.min(reqW, context.getRight() - screenX);
        int endY = Math.min(reqH, context.getBottom() - screenY);

        // Si la imagen está completamente fuera de los límites, abortar
        if (startX >= endX || startY >= endY) {
            return;
        }

        int clippedW = endX - startX;
        int clippedH = endY - startY;

        // Si no hay que recortar, dibujamos directo para máxima velocidad
        if (clippedW == reqW && clippedH == reqH) {
            ui.drawImagePixels(screenX, screenY, reqW, reqH, pixels);
            return;
        }

        // Si hay que recortar (Scroll o ventana pequeña), extraemos solo los píxeles visibles
        int[] clippedPixels = new int[clippedW * clippedH];
        for (int y = 0; y < clippedH; y++) {
            System.arraycopy(pixels, (startY + y) * reqW + startX, clippedPixels, y * clippedW, clippedW);
        }

        ui.drawImagePixels(screenX + startX, screenY + startY, clippedW, clippedH, clippedPixels);
    }

    private int[] scaleNearestNeighbor(int[] source, int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        int[] result = new int[targetWidth * targetHeight];
        for (int y = 0; y < targetHeight; y++) {
            int sourceY = (int) ((long) y * sourceHeight / targetHeight);
            int rowOffsetTarget = y * targetWidth;
            int rowOffsetSource = sourceY * sourceWidth;
            for (int x = 0; x < targetWidth; x++) {
                int sourceX = (int) ((long) x * sourceWidth / targetWidth);
                result[rowOffsetTarget + x] = source[rowOffsetSource + sourceX];
            }
        }
        return result;
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

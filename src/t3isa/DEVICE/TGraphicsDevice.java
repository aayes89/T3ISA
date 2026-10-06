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
package t3isa.DEVICE;

import t3isa.HARDWARE.TDevice;
import t3isa.Core.TWord;
import t3isa.HARDWARE.TMMIODevice;

/**
 *
 * @author Slam
 */
public final class TGraphicsDevice implements TDevice, TMMIODevice {

    private final int width;
    private final int height;
    private final int refreshRate;
    private final int colorDepth;

    private final int[] framebuffer;
    private int mmioX;
    private int mmioY;
    private int mmioColor;

    private int cursor;

    public TGraphicsDevice(int width, int height, int refreshRate, int colorDepth) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Resolución inválida");
        }

        if (refreshRate <= 0) {
            throw new IllegalArgumentException("Tasa de refrescado inválida");
        }

        if (colorDepth <= 0) {
            throw new IllegalArgumentException("Profundidad de color inválida");
        }

        this.width = width;
        this.height = height;
        this.refreshRate = refreshRate;
        this.colorDepth = colorDepth;

        framebuffer = new int[width * height];
        mmioX = 0;
        mmioY = 0;
        mmioColor = 0;
        cursor = 0;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRefreshRate() {
        return refreshRate;
    }

    public int getColorDepth() {
        return colorDepth;
    }

    @Override
    public void write(TWord value) {
        int index = cursor;

        if (index >= framebuffer.length) {
            return;
        }

        framebuffer[index] = (int) value.toLong();
        cursor++;
    }

    @Override
    public TWord read() {
        if (cursor >= framebuffer.length) {
            return TWord.zero();
        }

        return TWord.fromLong(framebuffer[cursor]);
    }

    @Override
    public boolean hasInput() {
        return false;
    }

    public void reset() {
        cursor = 0;

        for (int i = 0; i < framebuffer.length; i++) {
            framebuffer[i] = 0;
        }
    }

    public void setPixel(int x, int y, int value) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return;
        }

        framebuffer[y * width + x] = value;
    }

    public int getPixel(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return 0;
        }

        return framebuffer[y * width + x];
    }

    public int[] getFramebuffer() {
        return framebuffer;
    }

    @Override
    public TWord read(int offset) {
        switch (offset) {
            case 0:
                return TWord.fromLong(mmioX);

            case 1:
                return TWord.fromLong(mmioY);

            case 2:
                return TWord.fromLong(mmioColor);

            case 3:
                return TWord.zero();

            default:
                return TWord.zero();
        }
    }

    @Override
    public void write(int offset, TWord value) {
        int v = (int) value.toLong();

        switch (offset) {

            case 0:
                mmioX = v;
                break;

            case 1:
                mmioY = v;
                break;

            case 2:
                mmioColor = v;
                break;

            case 3:
                if (v == 1) {
                    setPixel(mmioX, mmioY, mmioColor);
                }
                if (v == 2) {
                    reset();
                }
                break;
        }
    }

    // Limpia toda la pantalla.
    public void clear(int color) {
        for (int i = 0; i < framebuffer.length; i++) {
            framebuffer[i] = color;
        }
    }

    // Línea mediante Bresenham.
    public void drawLine(int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);

        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;

        int err = dx - dy;

        while (true) {
            setPixel(x1, y1, color);

            if (x1 == x2 && y1 == y2) {
                break;
            }

            int e2 = err * 2;

            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }

            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    // Rectángulo sin relleno.
    public void drawRect(int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }

        drawLine(
                x,
                y,
                x + w - 1,
                y,
                color
        );

        drawLine(
                x,
                y,
                x,
                y + h - 1,
                color
        );

        drawLine(
                x + w - 1,
                y,
                x + w - 1,
                y + h - 1,
                color
        );

        drawLine(
                x,
                y + h - 1,
                x + w - 1,
                y + h - 1,
                color
        );
    }

    // Rectángulo relleno.
    public void fillRect(int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }

        int x2 = x + w;
        int y2 = y + h;

        if (x < 0) {
            x = 0;
        }

        if (y < 0) {
            y = 0;
        }

        if (x2 > width) {
            x2 = width;
        }

        if (y2 > height) {
            y2 = height;
        }

        for (int py = y; py < y2; py++) {
            int offset = py * width + x;

            for (int px = x; px < x2; px++) {
                framebuffer[offset++] = color;
            }
        }
    }

    // Círculo mediante punto medio.
    public void drawCircle(int cx, int cy, int radius, int color) {
        if (radius < 0) {
            return;
        }

        int x = radius;
        int y = 0;
        int decision = 1 - radius;

        while (x >= y) {

            setPixel(cx + x, cy + y, color);
            setPixel(cx + y, cy + x, color);
            setPixel(cx - y, cy + x, color);
            setPixel(cx - x, cy + y, color);
            setPixel(cx - x, cy - y, color);
            setPixel(cx - y, cy - x, color);
            setPixel(cx + y, cy - x, color);
            setPixel(cx + x, cy - y, color);

            y++;

            if (decision <= 0) {
                decision += 2 * y + 1;
            } else {
                x--;
                decision += 2 * (y - x) + 1;
            }
        }
    }

    // Círculo relleno.
    public void fillCircle(int cx, int cy, int radius, int color) {
        if (radius < 0) {
            return;
        }

        int r2 = radius * radius;

        int minY = Math.max(0, cy - radius);

        int maxY = Math.min(height - 1, cy + radius);

        for (int y = minY; y <= maxY; y++) {
            int dy = y - cy;

            int remaining = r2 - dy * dy;

            int dx = (int) Math.sqrt(remaining);

            int minX = Math.max(0, cx - dx);

            int maxX = Math.min(width - 1, cx + dx);

            for (int x = minX; x <= maxX; x++) {
                framebuffer[y * width + x] = color;
            }
        }
    }

    /*
     * Operación gráfica general.
     *
     * command:
     *
     * 1  = pixel
     * 2  = clear
     * 3  = line
     * 4  = rect
     * 5  = fill rect
     * 6  = circle
     * 7  = fill circle
     */
    public void graphics(int command, int a, int b, int c, int d, int e) {

        switch (command) {

            case 1:
                setPixel(a, b, c);
                break;

            case 2:
                clear(a);
                break;

            case 3:
                drawLine(a, b, c, d, e);
                break;

            case 4:
                drawRect(a, b, c, d, e);
                break;

            case 5:
                fillRect(a, b, c, d, e);
                break;

            case 6:
                drawCircle(a, b, c, d);
                break;

            case 7:
                fillCircle(a, b, c, d);
                break;
        }
    }

}

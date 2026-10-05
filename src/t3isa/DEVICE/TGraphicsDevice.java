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
import t3isa.CORE.TWord;

/**
 *
 * @author Slam
 */
public final class TGraphicsDevice implements TDevice {

    private final int width;
    private final int height;
    private final int refreshRate;
    private final int colorDepth;

    private final int[] framebuffer;

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
}

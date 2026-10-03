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
package t3isa;

/**
 *
 * @author Slam
 */
public final class TGraphicsDevice implements TDevice {

    public static final int WIDTH = 640;
    public static final int HEIGHT = 480;

    private final int[] framebuffer;

    private int cursor;

    public TGraphicsDevice() {
        framebuffer = new int[WIDTH * HEIGHT];
        cursor = 0;
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
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            return;
        }

        framebuffer[y * WIDTH + x] = value;
    }

    public int getPixel(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            return 0;
        }

        return framebuffer[y * WIDTH + x];
    }

    public int[] getFramebuffer() {
        return framebuffer;
    }
}

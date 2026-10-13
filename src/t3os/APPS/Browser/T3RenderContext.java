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
package t3os.APPS.Browser;

/**
 *
 * @author Slam
 *
 * Contexto de layout para el renderizador HTML de T3OS. No utiliza java.awt ni
 * bibliotecas externas.
 */
public final class T3RenderContext {

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private int cursorX;
    private int cursorY;

    private int color = 0x000000;
    private int background = -1;
    private int scrollY;

    public T3RenderContext(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("El área de renderizado debe tener dimensiones positivas");
        }

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.cursorX = x;
        this.cursorY = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRight() {
        return x + width;
    }

    public int getBottom() {
        return y + height;
    }

    public int getCursorX() {
        return cursorX;
    }

    public int getCursorY() {
        return cursorY;
    }

    public void setCursorX(int value) {
        cursorX = value;
    }

    public void setCursorY(int value) {
        cursorY = value;
    }

    public void advanceY(int pixels) {
        cursorY += pixels;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color & 0xFFFFFF;
    }

    public int getBackground() {
        return background;
    }

    public void setBackground(int background) {
        this.background = background < 0 ? -1 : background & 0xFFFFFF;
    }

    public boolean isVisibleY(int top, int elementHeight) {
        return top + elementHeight > y && top < getBottom();
    }

    public boolean isInside(int px, int py) {
        return px >= x && px < getRight() && py >= y && py < getBottom();
    }

    public int getScrollY() {
        return scrollY;
    }

    public void setScrollY(int value) {
        scrollY = Math.max(0, value);
    }
}

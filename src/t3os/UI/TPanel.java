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

// Componente de Panel (JPanel equivalente)
public final class TPanel extends TUIElement {

    private final List<TUIElement> children;
    private int backgroundColor;
    private int borderColor;

    // Constructor
    public TPanel(int x, int y, int width, int height, int backgroundColor, int borderColor) {
        super(x, y, width, height);

        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;

        children = new ArrayList<>();
    }

    // Añadir elemento al panel
    public void add(TUIElement element) {
        if (element == null) {
            throw new IllegalArgumentException("Elemento UI no puede ser null");
        }

        children.add(element);
    }

    // Eliminar elemento del panel
    public void remove(TUIElement element) {
        children.remove(element);
    }

    // Dibujar Panel y sus elementos en pantalla
    @Override
    public void draw(TUI ui) {
        ui.fillRect(x, y, width, height, backgroundColor);
        ui.drawRect(x, y, width, height, borderColor);

        for (TUIElement child : children) {
            if (!child.isVisible()) {
                continue;
            }

            child.draw(ui);
        }
    }

    // Getter y Setters
    public int getChildCount() {
        return children.size();
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int color) {
        backgroundColor = color;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int color) {
        borderColor = color;
    }
}

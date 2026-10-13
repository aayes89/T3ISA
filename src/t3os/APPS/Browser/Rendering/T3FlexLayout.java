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
public class T3FlexLayout {

    public static void apply(T3LayoutBox container, int availableWidth) {
        if (container == null || container.children.isEmpty()) {
            return;
        }

        boolean isRow = "row".equals(container.style.getFlexDirection());
        String justify = container.style.getJustifyContent();
        String align = container.style.getAlignItems();

        int totalChildWidth = 0;
        int maxChildHeight = 0;

        // Calcular el espacio consumido por los hijos en su flujo normal
        for (T3LayoutBox child : container.children) {
            totalChildWidth += child.width + Math.max(0, child.style.getMarginLeft()) + Math.max(0, child.style.getMarginRight());
            maxChildHeight = Math.max(maxChildHeight, child.height + Math.max(0, child.style.getMarginTop()) + Math.max(0, child.style.getMarginBottom()));
        }

        // Determinar el espacio sobrante en el eje principal (Horizontal para Row)
        int mainSpace = availableWidth - container.style.getPaddingLeft() - container.style.getPaddingRight() - totalChildWidth;
        if (mainSpace < 0) {
            mainSpace = 0;
        }

        int spacing = 0;
        int currentX = container.x + container.style.getPaddingLeft();

        if ("center".equals(justify)) {
            currentX += mainSpace / 2;
        } else if ("flex-end".equals(justify)) {
            currentX += mainSpace;
        } else if ("space-between".equals(justify) && container.children.size() > 1) {
            spacing = mainSpace / (container.children.size() - 1);
        }

        // Posicionar cada hijo definitivamente (y mover sus sub-elementos)
        for (T3LayoutBox child : container.children) {
            int dx = currentX + Math.max(0, child.style.getMarginLeft()) - child.x;

            // Alineación en el eje secundario (Vertical para Row)
            int targetY = container.y + container.style.getPaddingTop() + Math.max(0, child.style.getMarginTop());
            if ("center".equals(align)) {
                targetY += (maxChildHeight - child.height) / 2;
            } else if ("flex-end".equals(align)) {
                targetY += (maxChildHeight - child.height);
            }

            int dy = targetY - child.y;

            // Mover el elemento y todo su contenido
            child.shift(dx, dy);

            // Preparar el cursor para el siguiente elemento
            currentX += child.width + Math.max(0, child.style.getMarginLeft()) + Math.max(0, child.style.getMarginRight()) + spacing;
        }

        // Ajustar la altura del contenedor flex si estaba en auto
        if (container.style.getHeight() == null || "auto".equals(container.style.getHeight())) {
            container.height = maxChildHeight + container.style.getPaddingTop() + container.style.getPaddingBottom();
        }
    }
}

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
import java.util.ArrayList;

import java.util.List;

public class T3TableLayout {

    public static void apply(T3LayoutBox tableBox, int availableWidth) {
        if (tableBox == null || tableBox.children.isEmpty()) {
            return;
        }

        List<T3LayoutBox> rows = new ArrayList<>();
        for (T3LayoutBox child : tableBox.children) {
            String tag = child.node.getTagName();
            if ("tbody".equals(tag) || "thead".equals(tag)) {
                for (T3LayoutBox tr : child.children) {
                    if ("tr".equals(tr.node.getTagName())) {
                        rows.add(tr);
                    }
                }
            } else if ("tr".equals(tag)) {
                rows.add(child);
            }
        }

        if (rows.isEmpty()) {
            return;
        }

        int maxCols = 0;
        for (T3LayoutBox row : rows) {
            maxCols = Math.max(maxCols, row.children.size());
        }
        if (maxCols == 0) {
            return;
        }

        // Prevención de anchos negativos o mínimos absolutos irrompibles para Wikipedia
        int usableWidth = Math.max(maxCols * 10, availableWidth - tableBox.style.getPaddingLeft() - tableBox.style.getPaddingRight());
        int colWidth = usableWidth / maxCols;

        int currentY = tableBox.y + tableBox.style.getPaddingTop();

        for (T3LayoutBox row : rows) {
            int currentX = tableBox.x + tableBox.style.getPaddingLeft();
            int maxRowHeight = 0;

            for (T3LayoutBox cell : row.children) {
                int dx = currentX - cell.x;
                int dy = currentY - cell.y;
                cell.shift(dx, dy);

                // Respetar anchos precalculados mayores al ancho de columna
                cell.width = Math.max(colWidth, cell.width);
                maxRowHeight = Math.max(maxRowHeight, cell.height);

                currentX += cell.width;
            }

            currentY += maxRowHeight;
            row.height = maxRowHeight;
        }

        tableBox.height = currentY - tableBox.y + tableBox.style.getPaddingBottom();
    }
}

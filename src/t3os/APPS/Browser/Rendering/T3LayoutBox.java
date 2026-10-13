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
import t3os.APPS.Browser.CSS.T3Style;

import t3os.APPS.Browser.Images.T3HtmlImage;
import t3os.APPS.Browser.T3HtmlNode;

public class T3LayoutBox {

    public int x;
    public int y;
    public int width;
    public int height;

    public T3HtmlNode node;
    public T3Style style;
    public List<T3LayoutBox> children;
    public T3LayoutBox parent;

    public boolean isText;
    public String textContent;
    public T3HtmlImage image;

    public T3LayoutBox(T3HtmlNode node, T3Style style) {
        this.node = node;
        this.style = style;
        this.children = new ArrayList<>();
        this.isText = node != null && node.isTextNode();
        this.textContent = node != null ? node.getText() : "";
    }

    public void addChild(T3LayoutBox child) {
        if (child != null) {
            child.parent = this;
            this.children.add(child);
        }
    }
}

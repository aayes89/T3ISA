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
import java.util.List;
import java.util.Locale;
import t3os.APPS.Browser.CSS.T3CSSRule;
import t3os.APPS.Browser.CSS.T3CSSUtils;

import t3os.APPS.Browser.CSS.T3Style;
import t3os.APPS.Browser.T3HtmlNode;

public class T3LayoutEngine {

    private static final int CHAR_WIDTH = 8;
    private static final int LINE_HEIGHT = 16;

    private final int viewportWidth;
    private final List<T3CSSRule> cssRules;
    private int contentHeight = 0;

    public T3LayoutEngine(int viewportWidth, List<T3CSSRule> cssRules) {
        this.viewportWidth = viewportWidth;
        this.cssRules = cssRules;
    }

    public T3LayoutBox buildLayoutTree(T3HtmlNode root) {
        this.contentHeight = 0;
        return buildBox(root, null, 0, 0, viewportWidth);
    }

    private T3LayoutBox buildBox(T3HtmlNode node, T3Style inheritedStyle, int currentX, int currentY, int availableWidth) {
        if (node == null) {
            return null;
        }

        String tag = node.getTagName();
        if (tag != null) {
            tag = tag.toLowerCase(Locale.ROOT);
            if (tag.equals("head") || tag.equals("script") || tag.equals("style") || tag.equals("meta") || tag.equals("link") || tag.equals("title")) {
                return null;
            }
        }

        T3Style style = T3CSSUtils.computeT3Style(node, inheritedStyle != null ? inheritedStyle : new T3Style(), cssRules);
        T3LayoutBox box = new T3LayoutBox(node, style);
        box.x = currentX;
        box.y = currentY;

        if (box.isText) {
            box.width = box.textContent != null ? box.textContent.length() * CHAR_WIDTH : 0;
            if (box.width > availableWidth) {
                box.width = availableWidth; // Límite básico para texto inline
            }
            box.height = LINE_HEIGHT;
            contentHeight = Math.max(contentHeight, box.y + box.height);
            return box;
        }

        boolean isBlock = isBlockElement(tag, style);

        int marginTop = Math.max(0, style.getMarginTop());
        int marginRight = Math.max(0, style.getMarginRight());
        int marginBottom = Math.max(0, style.getMarginBottom());
        int marginLeft = Math.max(0, style.getMarginLeft());

        int paddingTop = style.getPaddingTop();
        int paddingBottom = style.getPaddingBottom();
        int paddingLeft = style.getPaddingLeft();
        int paddingRight = style.getPaddingRight();

        box.x += marginLeft;
        box.y += marginTop;

        int childX = box.x + paddingLeft;
        int childY = box.y + paddingTop;
        int childAvailableWidth = availableWidth - marginLeft - marginRight - paddingLeft - paddingRight;

        if (childAvailableWidth < 0) {
            childAvailableWidth = 0;
        }

        int maxChildBottom = childY;
        int maxChildRight = childX;

        for (T3HtmlNode childNode : node.getChildren()) {
            T3LayoutBox childBox = buildBox(childNode, style, childX, childY, childAvailableWidth);
            if (childBox != null) {
                box.addChild(childBox);

                if (isBlockElement(childNode.getTagName(), childBox.style)) {
                    childY += childBox.height + Math.max(0, childBox.style.getMarginBottom());
                    childX = box.x + paddingLeft;
                } else {
                    childX += childBox.width;
                    if (childX > box.x + paddingLeft + childAvailableWidth) {
                        childY += LINE_HEIGHT;
                        childX = box.x + paddingLeft;
                    }
                }
                maxChildBottom = Math.max(maxChildBottom, childBox.y + childBox.height);
                maxChildRight = Math.max(maxChildRight, childBox.x + childBox.width);
            }
        }

        int calculatedWidth = isBlock ? (availableWidth - marginLeft - marginRight) : (maxChildRight - box.x + paddingRight);
        box.width = resolveWidth(style.getWidth(), calculatedWidth);
        box.height = resolveHeight(style.getHeight(), maxChildBottom - box.y + paddingBottom);

        contentHeight = Math.max(contentHeight, box.y + box.height + marginBottom);

        return box;
    }

    private boolean isBlockElement(String tag, T3Style style) {
        if (tag == null) {
            return false;
        }
        String display = style.getDisplay();

        if ("inline".equals(display) || "inline-block".equals(display)) {
            return false;
        }
        if ("flex".equals(display)) {
            return true; // Preparado para T3FlexLayout en el futuro
        }
        switch (tag) {
            case "document":
            case "html":
            case "body":
            case "main":
            case "header":
            case "footer":
            case "nav":
            case "section":
            case "article":
            case "aside":
            case "div":
            case "p":
            case "blockquote":
            case "pre":
            case "ul":
            case "ol":
            case "li":
            case "table":
            case "tr":
            case "form":
            case "fieldset":
            case "h1":
            case "h2":
            case "h3":
            case "h4":
            case "h5":
            case "h6":
                return true;
            default:
                return false;
        }
    }

    private int resolveWidth(String value, int fallback) {
        if (value == null || "auto".equals(value)) {
            return fallback;
        }
        try {
            if (value.endsWith("%")) {
                int percent = Integer.parseInt(value.substring(0, value.length() - 1));
                return Math.max(0, fallback * percent / 100);
            }
            return Math.max(0, parsePixels(value, fallback));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private int resolveHeight(String value, int fallback) {
        if (value == null || "auto".equals(value)) {
            return fallback;
        }
        try {
            if (value.endsWith("%")) {
                return fallback; // Evita dependencias cíclicas de altura
            }
            return Math.max(0, parsePixels(value, fallback));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private int parsePixels(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        String cleaned = value.trim().toLowerCase(Locale.ROOT);
        try {
            if (cleaned.endsWith("px")) {
                cleaned = cleaned.substring(0, cleaned.length() - 2).trim();
            } else if (cleaned.endsWith("%") || cleaned.endsWith("em") || cleaned.endsWith("rem") || cleaned.equals("auto")) {
                return fallback;
            }
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public int getContentHeight() {
        return contentHeight;
    }
}

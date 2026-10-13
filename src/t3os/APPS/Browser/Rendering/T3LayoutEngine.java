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
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import t3os.APPS.Browser.T3HtmlNode;
import t3os.APPS.Browser.CSS.T3CSSRule;
import t3os.APPS.Browser.CSS.T3CSSUtils;
import t3os.APPS.Browser.CSS.T3Style;

import t3os.APPS.Browser.Images.T3HtmlImage;

public class T3LayoutEngine {

    private static final int CHAR_WIDTH = 8;
    private static final int LINE_HEIGHT = 16;

    private final int viewportWidth;
    private final int viewportHeight;
    private final List<T3CSSRule> cssRules;
    private final Map<String, T3HtmlImage> loadedImages;
    private final URI baseUrl;
    private int contentHeight = 0;

    // ACTUALIZADO: Añadido baseUrl al constructor para resolver URLs relativas
    public T3LayoutEngine(int viewportWidth, int viewportHeight, List<T3CSSRule> cssRules, Map<String, T3HtmlImage> loadedImages, URI baseUrl) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.cssRules = cssRules;
        this.loadedImages = loadedImages;
        this.baseUrl = baseUrl;
    }

    public T3LayoutBox buildLayoutTree(T3HtmlNode root) {
        this.contentHeight = 0;
        return buildBox(root, null, 0, 0, viewportWidth);
    }

    private T3LayoutBox buildBox(T3HtmlNode node, T3Style inheritedStyle, int currentX, int currentY, int availableWidth) {
        if (node == null) {
            return null;
        }

        String tag = node.getTagName() != null ? node.getTagName().toLowerCase(Locale.ROOT) : "";

        // Si es un doctype, comentario, CDATA o una etiqueta no visual de la cabecera, lo ignoro
        if (node.isDoctypeNode() || node.isCommentNode() || node.isCDataNode()
                || tag.equals("head") || tag.equals("script") || tag.equals("style")
                || tag.equals("meta") || tag.equals("link") || tag.equals("title")) {
            return null;
        }

        T3Style style = T3CSSUtils.computeT3Style(node, inheritedStyle != null ? inheritedStyle : new T3Style(), cssRules);

        // NUEVO: Traducir atributos HTML de los 90s a reglas CSS
        mapLegacyAttributes(tag, node, style);

        T3LayoutBox box = new T3LayoutBox(node, style);
        box.x = currentX;
        box.y = currentY;

        if (box.isText) {
            String text = box.textContent.replaceAll("\\s+", " ").trim();
            if (text.isEmpty()) {
                box.width = 0;
                box.height = 0;
                return box;
            }

            String[] words = text.split(" ");
            StringBuilder currentLine = new StringBuilder();
            int currentLineWidth = 0;
            int maxCalculatedWidth = 0;

            for (String word : words) {
                int wordWidth = word.length() * CHAR_WIDTH;
                if (currentLineWidth + wordWidth > availableWidth && currentLineWidth > 0) {
                    box.textLines.add(currentLine.toString().trim());
                    currentLine = new StringBuilder();
                    maxCalculatedWidth = Math.max(maxCalculatedWidth, currentLineWidth);
                    currentLineWidth = 0;
                }
                currentLine.append(word).append(" ");
                currentLineWidth += wordWidth + CHAR_WIDTH;
            }
            if (currentLine.length() > 0) {
                box.textLines.add(currentLine.toString().trim());
                maxCalculatedWidth = Math.max(maxCalculatedWidth, currentLineWidth - CHAR_WIDTH);
            }

            box.width = Math.min(maxCalculatedWidth, availableWidth);
            box.height = box.textLines.size() * LINE_HEIGHT;
            contentHeight = Math.max(contentHeight, box.y + box.height);
            return box;
        }

        // ACTUALIZADO: Búsqueda precisa de imágenes resolviendo la URL
        if (tag.equals("img")) {
            String src = node.getAttributes().get("src");
            if (src != null && baseUrl != null) {
                try {
                    String absoluteUrl = baseUrl.resolve(src.trim()).normalize().toString();
                    T3HtmlImage img = loadedImages.get(absoluteUrl);
                    box.image = img;
                    int intrinsicW = img != null ? img.getWidth() : 64;
                    int intrinsicH = img != null ? img.getHeight() : 48;

                    box.width = resolveWidth(node.getAttributes().get("width"), resolveWidth(style.getWidth(), intrinsicW));
                    box.height = resolveHeight(node.getAttributes().get("height"), resolveHeight(style.getHeight(), intrinsicH));
                    contentHeight = Math.max(contentHeight, box.y + box.height);
                } catch (IllegalArgumentException e) {
                    box.width = 64;
                    box.height = 48;
                }
            }
            return box;
        }

        boolean isBlock = isBlockElement(tag, style);
        boolean isFlex = "flex".equals(style.getDisplay());

        int marginTop = Math.max(0, style.getMarginTop());
        int marginBottom = Math.max(0, style.getMarginBottom());
        int marginLeft = Math.max(0, style.getMarginLeft());
        int marginRight = Math.max(0, style.getMarginRight());

        int paddingLeft = style.getPaddingLeft();
        int paddingTop = style.getPaddingTop();
        int paddingBottom = style.getPaddingBottom();
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
        int currentLineHeight = 0;

        for (T3HtmlNode childNode : node.getChildren()) {
            T3LayoutBox childBox = buildBox(childNode, style, childX, childY, childAvailableWidth);

            if (childBox != null) {
                box.addChild(childBox);

                if (!isFlex) {
                    boolean childIsBlock = isBlockElement(childNode.getTagName(), childBox.style) || "block".equals(childBox.style.getDisplay());

                    if (childIsBlock) {
                        if (childX > box.x + paddingLeft) {
                            childY += currentLineHeight;
                            childX = box.x + paddingLeft;
                            currentLineHeight = 0;
                            childBox.shift(childX - childBox.x, childY - childBox.y);
                        }

                        // Centrado legacy con margin: auto
                        if ("center".equals(childBox.style.getTextAlign()) || "center".equals(childNode.getTagName())) {
                            int shiftX = (childAvailableWidth - childBox.width) / 2;
                            if (shiftX > 0) {
                                childBox.shift(shiftX, 0);
                            }
                        }

                        childY += childBox.height + Math.max(0, childBox.style.getMarginBottom()) + Math.max(0, childBox.style.getMarginTop());
                    } else {
                        int totalChildW = childBox.width + Math.max(0, childBox.style.getMarginRight()) + Math.max(0, childBox.style.getMarginLeft());
                        if (totalChildW > childAvailableWidth - (childX - (box.x + paddingLeft)) && childX > box.x + paddingLeft) {
                            childY += Math.max(currentLineHeight, LINE_HEIGHT);
                            childX = box.x + paddingLeft;
                            currentLineHeight = 0;
                            childBox.shift(childX - childBox.x, childY - childBox.y);
                        }

                        childX += totalChildW;
                        currentLineHeight = Math.max(currentLineHeight, childBox.height);

                        if (childBox.isText && childBox.height > LINE_HEIGHT) {
                            childY += childBox.height;
                            childX = box.x + paddingLeft;
                            currentLineHeight = 0;
                        }
                    }
                }

                maxChildBottom = Math.max(maxChildBottom, childBox.y + childBox.height + Math.max(0, childBox.style.getMarginBottom()));
                maxChildRight = Math.max(maxChildRight, childBox.x + childBox.width + Math.max(0, childBox.style.getMarginRight()));
            }
        }

        if (!isFlex) {
            maxChildBottom = Math.max(maxChildBottom, childY + currentLineHeight);
        }

        int calculatedWidth = isBlock ? childAvailableWidth : (maxChildRight - box.x - paddingLeft);
        box.width = resolveWidth(style.getWidth(), calculatedWidth) + paddingLeft + paddingRight;
        box.height = resolveHeight(style.getHeight(), maxChildBottom - box.y) + paddingBottom;

        if (isFlex) {
            T3FlexLayout.apply(box, box.width);
            maxChildBottom = box.y;
            for (T3LayoutBox c : box.children) {
                maxChildBottom = Math.max(maxChildBottom, c.y + c.height + Math.max(0, c.style.getMarginBottom()));
            }
            box.height = resolveHeight(style.getHeight(), maxChildBottom - box.y) + paddingBottom;
        }

        if (tag.equals("body") || tag.equals("html") || tag.equals("document")) {
            box.height = Math.max(box.height, viewportHeight);
        }

        contentHeight = Math.max(contentHeight, box.y + box.height + marginBottom);
        return box;
    }

    // NUEVO MÉTIDO: Soporte para atributos de HTML de 1996
    private void mapLegacyAttributes(String tag, T3HtmlNode node, T3Style style) {
        Map<String, String> attrs = node.getAttributes();

        if (attrs.containsKey("bgcolor")) {
            java.awt.Color bg = T3CSSUtils.parseColor(attrs.get("bgcolor"));
            if (bg != null) {
                style.setBackgroundColor(bg);
            }
        }

        if (attrs.containsKey("text")) { // Color general del body
            java.awt.Color tc = T3CSSUtils.parseColor(attrs.get("text"));
            if (tc != null) {
                style.setColor(tc);
            }
        }

        if (attrs.containsKey("color")) { // Etiqueta <font>
            java.awt.Color c = T3CSSUtils.parseColor(attrs.get("color"));
            if (c != null) {
                style.setColor(c);
            }
        }

        if (attrs.containsKey("align")) {
            style.setTextAlign(attrs.get("align"));
        }

        if ("center".equals(tag)) {
            style.setTextAlign("center");
            style.setDisplay("block");
        }
    }

    private boolean isBlockElement(String tag, T3Style style) {
        if (tag == null) {
            return false;
        }
        String display = style.getDisplay();
        if ("inline".equals(display) || "inline-block".equals(display)) {
            return false;
        }
        if ("flex".equals(display) || "block".equals(display)) {
            return true;
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
            case "center":
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
                return fallback;
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

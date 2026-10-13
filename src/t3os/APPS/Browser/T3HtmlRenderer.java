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
 */
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import t3os.APPS.Browser.CSS.T3CSSRule;
import t3os.APPS.Browser.CSS.T3CSSUtils;
import t3os.APPS.Browser.CSS.T3Style;
import t3os.UI.TUI;

/**
 * Renderizador HTML básico para T3OS.
 *
 * El dibujo se realiza mediante TUI. No utiliza Swing, Graphics, FontMetrics ni
 * BufferedImage.
 */
public final class T3HtmlRenderer {

    private static final int CHAR_WIDTH = 8;
    private static final int LINE_HEIGHT = 16;

    private final TUI ui;
    private final T3RenderContext context;
    private final List<T3CSSRule> cssRules = new ArrayList<>();

    public T3HtmlRenderer(TUI ui, int x, int y, int width, int height) {
        if (ui == null) {
            throw new IllegalArgumentException("TUI no puede ser null");
        }

        this.ui = ui;
        this.context = new T3RenderContext(x, y, width, height);
    }

    public void render(T3HtmlNode root) {
        if (root == null) {
            return;
        }

        cssRules.clear();
        collectCSSRules(root);

        context.setCursorX(context.getX());
        context.setCursorY(context.getY());

        renderNode(root, false, new T3Style());
    }

    /*
     * Recopila las reglas antes de dibujar el documento.
     */
    private void collectCSSRules(T3HtmlNode node) {
        if (node == null) {
            return;
        }

        String tag = node.getTagName();

        if ("style".equalsIgnoreCase(tag)) {
            StringBuilder css = new StringBuilder();

            String nodeText = node.getText();
            if (nodeText != null && !nodeText.isEmpty()) {
                css.append(nodeText);
            } else {
                for (T3HtmlNode child : node.getChildren()) {
                    if (child.isTextNode() && child.getText() != null) {
                        css.append(child.getText());
                    }
                }
            }

            if (!css.isEmpty()) {
                cssRules.addAll(T3CSSUtils.parseT3Stylesheet(css.toString()));
            }
        }

        for (T3HtmlNode child : node.getChildren()) {
            collectCSSRules(child);
        }
    }

    private void renderNode(T3HtmlNode node, boolean inline, T3Style inheritedStyle) {
        if (node == null || !hasVerticalSpace()) {
            return;
        }

        if (node.isTextNode()) {
            renderText(node.getText());
            return;
        }

        String tag = node.getTagName();

        if (tag == null) {
            return;
        }

        tag = tag.toLowerCase(Locale.ROOT);

        if (tag.equals("head")
                || tag.equals("script")
                || tag.equals("style")
                || tag.equals("meta")
                || tag.equals("link")
                || tag.equals("title")
                || tag.equals("noscript")) {
            return;
        }

        if (tag.equals("br")) {
            newLine();
            return;
        }

        if (tag.equals("hr")) {
            ui.fillRect(context.getX(), context.getCursorY(), context.getWidth(), 1, 0x808080);
            newLine();
            return;
        }

        /*
         * El estilo se calcula antes de dibujar para
         * incluir las reglas CSS, las clases y el estilo inline.
         */
        T3Style style = T3CSSUtils.computeT3Style(node, inheritedStyle, cssRules);

        boolean block = isBlockElement(tag, style);

        if (block && !atLineStart()) {
            newLine();
        }

        int oldX = context.getCursorX();

        int marginTop = Math.max(0, style.getMarginTop());
        int marginRight = Math.max(0, style.getMarginRight());
        int marginBottom = Math.max(0, style.getMarginBottom());
        int marginLeft = Math.max(0, style.getMarginLeft());

        int paddingTop = style.getPaddingTop();
        int paddingRight = style.getPaddingRight();
        int paddingBottom = style.getPaddingBottom();
        int paddingLeft = style.getPaddingLeft();

        if (block) {
            context.advanceY(marginTop);
            context.setCursorX(context.getX() + marginLeft);
        } else {
            context.advanceY(0);
        }

        int contentX = context.getCursorX();
        int contentY = context.getCursorY();

        int boxX = block ? context.getX() + marginLeft : contentX;
        int boxWidth = resolveWidth(style.getWidth(), block ? context.getWidth() - marginLeft - marginRight : context.getRight() - contentX);
        boxWidth = Math.max(0, Math.min(boxWidth, context.getRight() - boxX));

        int boxHeight = resolveHeight(style.getHeight(), LINE_HEIGHT + paddingTop + paddingBottom);

        /*
         * Dibujo el fondo y el borde con el estilo computado.
         * El renderizador actual no realiza un cálculo de layout previo;
         * por eso el alto automático inicial es una línea.
         */
        Color backgroundColor = style.getBackgroundColor();

        if (backgroundColor != null && boxWidth > 0 && boxHeight > 0) {
            ui.fillRect(boxX, contentY, boxWidth, boxHeight, toRGB(backgroundColor));
        }

        int borderWidth = style.getBorderWidth();

        if (borderWidth > 0 && boxWidth > 0 && boxHeight > 0) {
            int borderColor = toRGB(style.getBorderColor());

            for (int i = 0; i < borderWidth; i++) {
                int width = boxWidth - i * 2;
                int height = boxHeight - i * 2;

                if (width <= 0 || height <= 0) {
                    break;
                }

                ui.drawRect(boxX + i, contentY + i, width, height, borderColor);
            }
        }

        int savedColor = context.getColor();

        if (style.getColor() != null) {
            context.setColor(toRGB(style.getColor()));
        }

        context.setCursorX(contentX + paddingLeft);
        context.setCursorY(contentY + paddingTop);

        if (tag.matches("h[1-6]")) {
            context.setColor(0x000080);
        }

        if (tag.equals("pre")) {
            renderPreformatted(node, style);
        } else if (tag.equals("ul") || tag.equals("ol")) {
            renderList(node, tag.equals("ol"), style);
        } else if (tag.equals("img")) {
            renderImagePlaceholder(node);
        } else {
            for (T3HtmlNode child : node.getChildren()) {
                renderNode(child, !block, style);
                if (!hasVerticalSpace()) {
                    break;
                }
            }
        }

        context.setColor(savedColor);

        if (block) {
            if (!atLineStart()) {
                newLine();
            }

            context.advanceY(paddingBottom + marginBottom);
            context.setCursorX(context.getX());
        } else {
            context.setCursorX(oldX);
        }
    }

    /*
     * Renderiza texto normal: colapsa espacios consecutivos y ajusta
     * las palabras al ancho disponible.
     */
    private void renderText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        String normalized = text.replace('\t', ' ');
        normalized = normalized.replace('\r', ' ');
        normalized = normalized.replace('\n', ' ');
        normalized = normalized.replaceAll("\\s+", " ");

        String[] words = normalized.split(" ");

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            if (!hasVerticalSpace()) {
                return;
            }

            int available = context.getRight() - context.getCursorX();
            int wordWidth = word.length() * CHAR_WIDTH;

            if (wordWidth > available && !atLineStart()) {
                newLine();
            }

            for (int i = 0; i < word.length(); i++) {
                if (!hasVerticalSpace()) {
                    return;
                }

                if (context.getCursorX() + CHAR_WIDTH > context.getRight()) {
                    newLine();
                }

                ui.drawText(String.valueOf(word.charAt(i)), context.getCursorX(), context.getCursorY(), context.getColor());
                context.setCursorX(context.getCursorX() + CHAR_WIDTH);
            }

            if (context.getCursorX() + CHAR_WIDTH <= context.getRight()) {
                context.setCursorX(context.getCursorX() + CHAR_WIDTH);
            }
        }
    }

    /*
     * <pre> conserva espacios y saltos de línea.
     */
    private void renderPreformatted(T3HtmlNode node, T3Style inheritedStyle) {
        String text = node.getText();

        if (text != null && !text.isEmpty()) {
            renderPreformattedText(text);
            return;
        }

        for (T3HtmlNode child : node.getChildren()) {
            if (child.isTextNode()) {
                renderPreformattedText(child.getText());
            } else {
                renderNode(child, false, inheritedStyle);
            }

            if (!hasVerticalSpace()) {
                return;
            }
        }
    }

    private void renderPreformattedText(String text) {
        if (text == null) {
            return;
        }

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').replace('\t', ' ');

        for (int i = 0; i < normalized.length(); i++) {
            if (!hasVerticalSpace()) {
                return;
            }

            char c = normalized.charAt(i);

            if (c == '\n') {
                newLine();
                continue;
            }

            if (context.getCursorX() + CHAR_WIDTH > context.getRight()) {
                newLine();
            }

            ui.drawText(String.valueOf(c), context.getCursorX(), context.getCursorY(), context.getColor());
            context.setCursorX(context.getCursorX() + CHAR_WIDTH);
        }
    }

    private void renderList(T3HtmlNode node, boolean ordered, T3Style inheritedStyle) {
        int index = 1;

        for (T3HtmlNode child : node.getChildren()) {
            if (!"li".equalsIgnoreCase(child.getTagName())) {
                renderNode(child, false, inheritedStyle);
                continue;
            }

            if (!atLineStart()) {
                newLine();
            }

            T3Style itemStyle = T3CSSUtils.computeT3Style(child, inheritedStyle, cssRules);
            String marker = ordered ? index + ". " : "* ";

            context.setCursorX(context.getX() + CHAR_WIDTH);
            renderText(marker);

            for (T3HtmlNode itemChild : child.getChildren()) {
                renderNode(itemChild, true, itemStyle);

                if (!hasVerticalSpace()) {
                    return;
                }
            }

            if (!atLineStart()) {
                newLine();
            }

            index++;
        }
    }

    private void renderImagePlaceholder(T3HtmlNode node) {
        int x = context.getCursorX();
        int y = context.getCursorY();

        if (!hasVerticalSpace()) {
            return;
        }

        int width = parsePixels(node.getAttributes().get("width"), 64);
        int height = parsePixels(node.getAttributes().get("height"), 48);

        width = Math.max(16, Math.min(width, context.getWidth()));
        height = Math.max(16, Math.min(height, context.getHeight()));

        ui.drawRect(x, y, width, height, 0x808080);

        String alt = node.getAttributes().get("alt");

        if (alt != null && !alt.isEmpty()) {
            ui.drawText(alt, x + 2, y + 2, context.getColor());
        }

        context.setCursorX(x + width + CHAR_WIDTH);

        if (context.getCursorX() + CHAR_WIDTH > context.getRight()) {
            newLine();
        }
    }

    private boolean isBlockElement(String tag, T3Style style) {
        String display = style.getDisplay();

        if ("inline".equals(display) || "inline-block".equals(display)) {
            return false;
        }

        /*
         * El layout flex real requiere distribuir los hijos (no lo implemento aún)
         */
        if ("flex".equals(display)) {
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
                return true;

            default:
                return false;
        }
    }

    private boolean atLineStart() {
        return context.getCursorX() <= context.getX();
    }

    private boolean hasVerticalSpace() {
        return context.getCursorY() < context.getBottom();
    }

    private void newLine() {
        context.setCursorX(context.getX());
        context.advanceY(LINE_HEIGHT);
    }

    private int resolveWidth(String value, int fallback) {
        if (value == null || "auto".equals(value)) {
            return fallback;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        try {
            if (normalized.endsWith("%")) {
                int percent = Integer.parseInt(normalized.substring(0, normalized.length() - 1));
                return Math.max(0, fallback * percent / 100);
            }

            return Math.max(0, parsePixels(normalized, fallback));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private int resolveHeight(String value, int fallback) {
        if (value == null || "auto".equals(value)) {
            return fallback;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        try {
            if (normalized.endsWith("%")) {
                int percent = Integer.parseInt(normalized.substring(0, normalized.length() - 1));
                return Math.max(0, context.getHeight() * percent / 100);
            }

            return Math.max(0, parsePixels(normalized, fallback));
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

    private int toRGB(Color color) {
        return color == null ? 0 : color.getRGB() & 0xFFFFFF;
    }
}

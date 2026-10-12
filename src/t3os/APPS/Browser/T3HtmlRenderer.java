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
import java.util.Locale;

import java.util.Map;
import t3os.APPS.Browser.CSS.T3CSSRule;
import t3os.APPS.Browser.CSS.T3CSSUtils;
import t3os.APPS.Browser.CSS.T3Style;

import t3os.UI.TUI;

/**
 * Port of HTMLRender from jWebView project Renderizador HTML básico para T3OS.
 *
 * No utiliza AWT, Swing, FontMetrics ni BufferedImage. Dibuja a través de las
 * primitivas gráficas de TUI.
 */
public final class T3HtmlRenderer {

    private static final int CHAR_WIDTH = 8;
    private static final int LINE_HEIGHT = 16;

    private final TUI ui;
    private final T3RenderContext context;

    private final java.util.List<T3CSSRule> cssRules = new java.util.ArrayList<>();

    public T3HtmlRenderer(TUI ui, int x, int y, int width, int height) {
        if (ui == null) {
            throw new IllegalArgumentException("TUI no puede ser null");
        }

        this.ui = ui;
        this.context = new T3RenderContext(x, y, width, height);
    }

    private void collectCSSRules(T3HtmlNode node) {
        if (node == null) {
            return;
        }

        String tag = node.getTagName();

        if ("style".equalsIgnoreCase(tag)) {
            StringBuilder css = new StringBuilder();

            // El parser que compartiste puede guardar el CSS
            // directamente en node.text.
            if (node.getText() != null) {
                css.append(node.getText());
            }

            // También admitimos el caso en que esté en nodos de texto.
            for (T3HtmlNode child : node.getChildren()) {
                if (child.isTextNode() && child.getText() != null) {
                    css.append('\n').append(child.getText());
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

    /**
     * Renderiza un documento o un fragmento HTML.
     *
     * @param root
     */
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

        // Estos elementos no se muestran como contenido de página.
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
            int oldY = context.getCursorY();
            ui.fillRect(context.getX(), oldY, context.getWidth(), 1, 0x808080);
            context.advanceY(LINE_HEIGHT);
            return;
        }

        boolean block = isBlockElement(tag);

        if (block && !atLineStart()) {
            newLine();
        }

        int oldX = context.getCursorX();
        int oldY = context.getCursorY();

        int marginTop = 0;
        int marginBottom = 0;
        int padding = 0;

        Map<String, String> attributes = node.getAttributes();
        String inlineStyle = attributes.get("style");

        if (inlineStyle != null) {
            Map<String, String> styles = parseInlineStyle(inlineStyle);

            marginTop = parsePixels(styles.get("margin-top"), 0);
            marginBottom = parsePixels(styles.get("margin-bottom"), 0);
            padding = parsePixels(styles.get("padding"), 0);

            String margin = styles.get("margin");
            if (margin != null) {
                int[] values = parseMargin(margin);
                marginTop = values[0];
                marginBottom = values[2];
            }
        }

        if (block) {
            context.advanceY(marginTop);
            oldY = context.getCursorY();
            context.setCursorX(context.getX());
        }

        int contentX = context.getCursorX();
        int contentY = context.getCursorY();

        int background = -1;
        int foreground = 0x000000;
        boolean hasBorder = false;

        if (inlineStyle != null) {
            Map<String, String> styles = parseInlineStyle(inlineStyle);

            background = parseColor(styles.get("background-color"), parseColor(styles.get("background"), -1));
            foreground = parseColor(styles.get("color"), 0x000000);

            String border = styles.get("border");
            hasBorder = border != null && !border.equalsIgnoreCase("none") && !border.equals("0");
        }

        int boxX = block ? context.getX() : contentX;
        int boxWidth = block ? context.getWidth() : Math.max(CHAR_WIDTH, context.getRight() - contentX);

        if (background >= 0) {
            ui.fillRect(boxX, contentY, boxWidth, LINE_HEIGHT + padding * 2, background);
        }

        if (hasBorder) {
            ui.drawRect(boxX, contentY, boxWidth, LINE_HEIGHT + padding * 2, 0x808080);
        }

        int savedColor = context.getColor();

        T3Style computedStyle = T3CSSUtils.computeT3Style(node, inheritedStyle, cssRules);

        if (computedStyle.getColor() != null) {
            foreground = computedStyle.getColor().getRGB() & 0xFFFFFF;
        }

        context.setColor(foreground);

        if (padding > 0) {
            context.setCursorX(contentX + padding);
            context.setCursorY(contentY + padding);
        }

        if (tag.matches("h[1-6]")) {
            context.setColor(0x000080);
            context.advanceY(2);
        }

        if (tag.equals("pre")) {
            renderPreformatted(node);
        } else if (tag.equals("ul") || tag.equals("ol")) {
            renderList(node, tag.equals("ol"));
        } else if (tag.equals("img")) {
            renderImagePlaceholder(node);
        } else {
            for (T3HtmlNode child : node.getChildren()) {
                renderNode(child, !block, computedStyle);
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

            context.advanceY(marginBottom);
            context.setCursorX(context.getX());
        } else {
            context.setCursorX(oldX);
        }
    }

    private void renderText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        String normalized = text.replace('\t', ' ');

        // En texto normal se colapsan los espacios consecutivos.
        normalized = normalized.replaceAll("\\s+", " ");

        String[] words = normalized.split(" ");

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            int available = context.getRight()
                    - context.getCursorX();

            int wordWidth = word.length() * CHAR_WIDTH;

            if (wordWidth > available && !atLineStart()) {
                newLine();
            }

            // Si una palabra supera todo el ancho, se corta.
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

            // Separación entre palabras.
            if (context.getCursorX() + CHAR_WIDTH <= context.getRight()) {
                context.setCursorX(context.getCursorX() + CHAR_WIDTH);
            }
        }
    }

    private void renderPreformatted(T3HtmlNode node) {
        String text = node.getText();

        if (text != null && !text.isEmpty()) {
            String[] lines = text.split("\\n", -1);

            for (String line : lines) {
                if (!hasVerticalSpace()) {
                    return;
                }

                renderText(line);
                newLine();
            }
            return;
        }

        for (T3HtmlNode child : node.getChildren()) {
            if (child.isTextNode()) {
                String[] lines = child.getText().split("\\n", -1);

                for (String line : lines) {
                    renderText(line);
                    newLine();
                }
            } else {
                T3Style computedStyle = T3CSSUtils.computeT3Style(node, new T3Style(), cssRules);
                renderNode(child, false, computedStyle);
            }
        }
    }

    private void renderList(T3HtmlNode node, boolean ordered) {
        int index = 1;
        T3Style computedStyle = T3CSSUtils.computeT3Style(node, new T3Style(), cssRules);

        for (T3HtmlNode child : node.getChildren()) {
            if (!"li".equalsIgnoreCase(child.getTagName())) {

                renderNode(child, false, computedStyle);
                continue;
            }

            String marker = ordered
                    ? index + ". "
                    : "* ";

            context.setCursorX(context.getX() + CHAR_WIDTH);

            renderText(marker);

            for (T3HtmlNode itemChild : child.getChildren()) {
                renderNode(itemChild, true, computedStyle);
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

        int width = parsePixels(
                node.getAttributes().get("width"), 64
        );

        int height = parsePixels(
                node.getAttributes().get("height"), 48
        );

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

    private boolean isBlockElement(String tag) {
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

    private Map<String, String> parseInlineStyle(String style) {
        Map<String, String> result = new java.util.HashMap<>();

        for (String declaration : style.split(";")) {
            int colon = declaration.indexOf(':');

            if (colon <= 0) {
                continue;
            }

            String property = declaration.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = declaration.substring(colon + 1).trim();

            if (!property.isEmpty() && !value.isEmpty()) {
                result.put(property, value);
            }
        }

        return result;
    }

    private int parsePixels(String value, int fallback) {
        if (value == null) {
            return fallback;
        }

        String cleaned = value.trim().toLowerCase(Locale.ROOT);

        try {
            if (cleaned.endsWith("px")) {
                cleaned = cleaned.substring(0, cleaned.length() - 2).trim();
            } else if (cleaned.endsWith("%") || cleaned.endsWith("em") || cleaned.endsWith("rem")) {
                return fallback;
            }

            return Integer.parseInt(cleaned);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private int[] parseMargin(String value) {
        String[] parts = value.trim().split("\\s+");
        int[] result = {0, 0, 0, 0};

        if (parts.length == 1) {
            int v = parsePixels(parts[0], 0);
            result[0] = result[1] = result[2] = result[3] = v;
        } else if (parts.length == 2) {
            result[0] = result[2] = parsePixels(parts[0], 0);
            result[1] = result[3] = parsePixels(parts[1], 0);
        } else if (parts.length == 3) {
            result[0] = parsePixels(parts[0], 0);
            result[1] = result[3] = parsePixels(parts[1], 0);
            result[2] = parsePixels(parts[2], 0);
        } else if (parts.length >= 4) {
            for (int i = 0; i < 4; i++) {
                result[i] = parsePixels(parts[i], 0);
            }
        }

        return result;
    }

    private int parseColor(String value, int fallback) {
        if (value == null) {
            return fallback;
        }

        String color = value.trim().toLowerCase(Locale.ROOT);

        switch (color) {
            case "black":
                return 0x000000;
            case "white":
                return 0xFFFFFF;
            case "red":
                return 0xFF0000;
            case "green":
                return 0x008000;
            case "blue":
                return 0x0000FF;
            case "yellow":
                return 0xFFFF00;
            case "gray":
            case "grey":
                return 0x808080;
            case "silver":
                return 0xC0C0C0;
            case "navy":
                return 0x000080;
            case "purple":
                return 0x800080;
            case "orange":
                return 0xFFA500;
            case "transparent":
                return -1;
            default:
                break;
        }

        if (color.matches("#[0-9a-f]{3}")) {
            int r = Character.digit(color.charAt(1), 16);
            int g = Character.digit(color.charAt(2), 16);
            int b = Character.digit(color.charAt(3), 16);

            return (r * 17 << 16) | (g * 17 << 8) | (b * 17);
        }

        if (color.matches("#[0-9a-f]{6}")) {
            try {
                return Integer.parseInt(color.substring(1), 16);
            } catch (NumberFormatException ex) {
                return fallback;
            }
        }

        return fallback;
    }
}

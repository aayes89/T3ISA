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
import t3os.APPS.Browser.Images.T3HtmlImage;
import java.awt.Color;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private final Map<String, T3HtmlImage> images = new HashMap<>();
    private int scrollY;
    private int contentHeight;
    private boolean measuring;
    private String baseUrl = "";

    public T3HtmlRenderer(TUI ui, int x, int y, int width, int height) {
        if (ui == null) {
            throw new IllegalArgumentException("TUI no puede ser null");
        }

        this.ui = ui;
        this.context = new T3RenderContext(x, y, width, height);
    }

    // Dibuja el nodo raiz
    public void render(T3HtmlNode root) {
        if (root == null) {
            contentHeight = 0;
            return;
        }

        cssRules.clear();
        collectCSSRules(root);

        measuring = true;
        context.setCursorX(context.getX());
        context.setCursorY(context.getY());
        context.setScrollY(0);

        renderNode(root, false, new T3Style());

        contentHeight = Math.max(context.getHeight(), context.getCursorY() - context.getY() + LINE_HEIGHT);
        measuring = false;

        ui.fillRect(context.getX(), context.getY(), context.getWidth(), context.getHeight(), 0xFFFFFF);

        context.setCursorX(context.getX());
        context.setCursorY(context.getY() - scrollY);
        context.setScrollY(scrollY);

        renderNode(root, false, new T3Style());
    }

    // Recopila las reglas antes de dibujar el documento.
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

    // Auxiliar para dibujar nodo
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
            fillRectClipped(context.getX(), context.getCursorY(), context.getWidth(), 1, 0x808080);
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
            fillRectClipped(boxX, contentY, boxWidth, boxHeight, toRGB(backgroundColor));
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

                drawRectClipped(boxX + i, contentY + i, width, height, borderColor);
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
            //renderImagePlaceholder(node);
            renderImage(node);
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

    /* Renderiza texto normal: 
     * colapsa espacios consecutivos y ajusta las palabras al ancho disponible.
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

                int drawX = context.getCursorX();
                int drawY = context.getCursorY();

                drawTextClipped(String.valueOf(word.charAt(i)), drawX, drawY, context.getColor());

                context.setCursorX(drawX + CHAR_WIDTH);
            }

            if (context.getCursorX() + CHAR_WIDTH <= context.getRight()) {
                context.setCursorX(context.getCursorX() + CHAR_WIDTH);
            }
        }
    }

    // <pre> conserva espacios y saltos de línea.
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

    // Auxiliar para dibujar texto con formato
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

            drawTextClipped(String.valueOf(c), context.getCursorX(), context.getCursorY(), context.getColor());

            context.setCursorX(context.getCursorX() + CHAR_WIDTH);
        }
    }

    // Auxiliar para dibujar lista
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

    // Auxiliar para saber si es un bloque de elementos
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

    // Auxiliar para saber si está al principio
    private boolean atLineStart() {
        return context.getCursorX() <= context.getX();
    }

    // Auxiliar para saber si hay espacio vertical
    private boolean hasVerticalSpace() {
        // La medición debe recorrer el documento completo.
        if (measuring) {
            return true;
        }

        // En el dibujo real se detiene al llegar al final visible.
        return context.getCursorY() < context.getBottom();
    }

    // Auxiliar para añadir nueva línea
    private void newLine() {
        context.setCursorX(context.getX());
        context.advanceY(LINE_HEIGHT);
    }

    // Auxiliar para resolver ancho
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

    // Auxiliar para resolver altura
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

    // Auxiliar para parser de pixeles
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

    // Auxiliar para convertir el color a RGB
    private int toRGB(Color color) {
        return color == null ? 0 : color.getRGB() & 0xFFFFFF;
    }

    // Auxiliar para scrollbar
    private boolean isVisibleY(int y) {
        return y >= context.getY() && y < context.getBottom();
    }

    // Auxiliar para dibujar letras según tamaño de ventana
    private void drawTextClipped(String text, int x, int y, int color) {
        if (text == null || !isVisibleY(y)) {
            return;
        }

        int left = context.getX();
        int right = context.getRight();

        for (int i = 0; i < text.length(); i++) {
            int charX = x + i * CHAR_WIDTH;

            if (charX >= left && charX + CHAR_WIDTH <= right) {
                ui.drawText(String.valueOf(text.charAt(i)), charX, y, color);
            }
        }
    }

    // Auxiliar para pintar rectángulo encuadrado
    private void fillRectClipped(int x, int y, int width, int height, int color) {
        if (measuring || width <= 0 || height <= 0) {
            return;
        }

        int left = Math.max(x, context.getX());
        int top = Math.max(y, context.getY());
        int right = Math.min(x + width, context.getRight());
        int bottom = Math.min(y + height, context.getBottom());

        if (right > left && bottom > top) {
            ui.fillRect(left, top, right - left, bottom - top, color);
        }
    }

    // Auxiliar para dibujar rectángulo encuadrado
    private void drawRectClipped(int x, int y, int width, int height, int color) {
        if (measuring || width <= 0 || height <= 0) {
            return;
        }

        int left = Math.max(x, context.getX());
        int top = Math.max(y, context.getY());
        int right = Math.min(x + width, context.getRight());
        int bottom = Math.min(y + height, context.getBottom());

        if (right > left && bottom > top) {
            ui.drawRect(left, top, right - left, bottom - top, color);
        }
    }

    // Dibujar imagen
    private void renderImage(T3HtmlNode node) {
        int imageX = context.getCursorX();
        int imageY = context.getCursorY();

        String src = node.getAttributes().get("src");
        String alt = node.getAttributes().get("alt");

        T3HtmlImage image = null;

        if (src != null && !src.isBlank()) {
            String imageUrl = src.trim();

            try {
                if (baseUrl != null && !baseUrl.isBlank()) {
                    imageUrl = URI.create(baseUrl).resolve(imageUrl).normalize().toString();
                }

                image = images.get(imageUrl);

                if (image == null) {
                    image = images.get(src.trim());
                }
            } catch (IllegalArgumentException ignored) {
                image = images.get(src.trim());
            }
        }

        int requestedWidth;
        int requestedHeight;

        if (image != null) {
            requestedWidth = parsePixels(node.getAttributes().get("width"), image.getWidth());
            requestedHeight = parsePixels(node.getAttributes().get("height"), image.getHeight());
        } else {
            requestedWidth = parsePixels(node.getAttributes().get("width"), 64);
            requestedHeight = parsePixels(node.getAttributes().get("height"), 48);
        }

        requestedWidth = Math.max(1, Math.min(requestedWidth, context.getWidth()));
        requestedHeight = Math.max(1, Math.min(requestedHeight, context.getHeight()));

        if (measuring) {
            context.setCursorX(imageX + requestedWidth + CHAR_WIDTH);

            if (context.getCursorX() + CHAR_WIDTH > context.getRight()) {
                newLine();
            }
            return;
        }

        if (image != null) {
            if (requestedWidth == image.getWidth() && requestedHeight == image.getHeight()) {
                ui.drawImagePixels(imageX, imageY, image.getWidth(), image.getHeight(), image.getPixels());
            } else {
                int[] scaledPixels = scaleNearestNeighbor(image.getPixels(), image.getWidth(), image.getHeight(), requestedWidth, requestedHeight);

                ui.drawImagePixels(imageX, imageY, requestedWidth, requestedHeight, scaledPixels);
            }
        } else {
            drawRectClipped(imageX, imageY, requestedWidth, requestedHeight, 0x808080);

            if (alt != null && !alt.isEmpty()) {
                drawTextClipped(alt, imageX + 2, imageY + 2, context.getColor());
            }
        }

        context.setCursorX(imageX + requestedWidth + CHAR_WIDTH);

        if (context.getCursorX() + CHAR_WIDTH > context.getRight()) {
            newLine();
        }
    }

    // Auxiliar para que el escalado produzca el array del tamaño correcto
    private int[] scaleNearestNeighbor(int[] source, int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        int[] result = new int[targetWidth * targetHeight];

        for (int y = 0; y < targetHeight; y++) {
            int sourceY = (int) ((long) y * sourceHeight / targetHeight);

            for (int x = 0; x < targetWidth; x++) {
                int sourceX = (int) ((long) x * sourceWidth / targetWidth);
                result[y * targetWidth + x] = source[sourceY * sourceWidth + sourceX];
            }
        }

        return result;
    }

    // Getter y Setters
    public int getContentHeight() {
        return contentHeight;
    }

    public void setScrollY(int value) {
        scrollY = Math.max(0, value);
    }

    public void setImage(String src, T3HtmlImage image) {
        if (src == null || src.isBlank()) {
            return;
        }

        if (image == null) {
            images.remove(src);
        } else {
            images.put(src, image);
        }
    }

    public void setBaseUrl(String url) {
        if (url != null) {
            baseUrl = url;
        }
    }
}

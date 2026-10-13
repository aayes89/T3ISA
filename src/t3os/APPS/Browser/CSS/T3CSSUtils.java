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
package t3os.APPS.Browser.CSS;

/**
 *
 * @author Slam
 */
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import t3os.APPS.Browser.T3HtmlNode;

/**
 * Utilidades para parsear y combinar estilos CSS.
 */
public class T3CSSUtils {

    private static final Logger LOGGER = Logger.getLogger(T3CSSUtils.class.getName());
    private static final Pattern MARGIN_PADDING_PATTERN = Pattern.compile(
            "(\\d+px|auto)(?:\\s+(\\d+px|auto))?(?:\\s+(\\d+px|auto))?(?:\\s+(\\d+px|auto))?"
    );
    private static final Pattern COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Map<List<T3CSSRule>, List<T3CSSRule>> RULE_CACHE = new HashMap<>();

    /**
     * Parsea un atributo de estilo inline.
     *
     * @param styleAttr Cadena de estilo inline (por ejemplo, "color: red;
     * font-size: 16px")
     * @return Objeto T3Style con las propiedades parseadas
     */
    public static T3Style parse(String styleAttr) {
        T3Style style = new T3Style();
        if (styleAttr == null || styleAttr.trim().isEmpty()) {
            return style;
        }

        for (String declaration : styleAttr.split(";")) {
            declaration = declaration.trim();
            if (declaration.isEmpty()) {
                continue;
            }
            int colon = declaration.indexOf(':');

            if (colon <= 0) {
                LOGGER.fine("Declaración CSS inválida: " + declaration);
                continue;
            }

            String property = declaration.substring(0, colon)
                    .trim().toLowerCase(Locale.ROOT);

            String value = declaration.substring(colon + 1).trim();

            applyDeclaration(style, property, value);
        }
        return style;
    }

    /**
     * Parsea una hoja de estilo CSS.
     *
     * @param cssContent Contenido de la hoja de estilo
     * @return Lista de reglas CSS
     */
    public static List<T3CSSRule> parseT3Stylesheet(String cssContent) {
        // Compatibilidad con las llamadas existentes.
        return parseT3Stylesheet(cssContent, 1024);
    }

    /**
     * Parsea CSS aplicando las condiciones @media según el ancho disponible.
     * Métodos auxiliares debajo
     *
     * @param cssContent
     * @param viewportWidth
     * @return
     */
    public static List<T3CSSRule> parseT3Stylesheet(String cssContent, int viewportWidth) {
        List<T3CSSRule> rules = new ArrayList<>();

        if (cssContent == null || cssContent.trim().isEmpty()) {
            return Collections.unmodifiableList(rules);
        }

        String cleaned = COMMENT_PATTERN.matcher(cssContent).replaceAll("");

        parseCssBlocks(cleaned, viewportWidth, true, rules);

        return Collections.unmodifiableList(rules);
    }

    private static void parseCssBlocks(String css, int viewportWidth, boolean mediaApplies, List<T3CSSRule> rules) {
        int pos = 0;

        while (pos < css.length()) {
            pos = skipWhitespace(css, pos);

            if (pos >= css.length()) {
                break;
            }

            int open = findCssChar(css, '{', pos);

            if (open < 0) {
                break;
            }

            String header = css.substring(pos, open).trim();

            int close = findMatchingBrace(css, open);

            if (close < 0) {
                LOGGER.warning("Bloque CSS sin cerrar: " + header);
                break;
            }

            String body = css.substring(open + 1, close);

            if (header.isEmpty()) {
                pos = close + 1;
                continue;
            }

            if (header.regionMatches(true, 0, "@media", 0, 6)) {
                boolean applies = mediaApplies && mediaQueryMatches(header.substring(6).trim(), viewportWidth);

                if (applies) {
                    parseCssBlocks(body, viewportWidth, true, rules);
                }
            } else if (header.startsWith("@")) {
                // Otras directivas (@supports, @keyframes, etc.)
                // todavía no se implementan.
                LOGGER.fine("Directiva CSS no soportada: " + header);
            } else if (mediaApplies) {
                Map<String, String> declarations = parseCssDeclarations(body);

                if (!declarations.isEmpty()) {
                    for (String selector : header.split(",")) {
                        selector = selector.trim();

                        if (selector.isEmpty()) {
                            continue;
                        }

                        try {
                            rules.add(new T3CSSRule(selector, declarations));
                        } catch (IllegalArgumentException ex) {
                            LOGGER.fine("Selector CSS no compatible: " + selector + " - " + ex.getMessage());
                        }
                    }
                }
            }

            pos = close + 1;
        }
    }

    private static int skipWhitespace(String text, int pos) {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private static int findCssChar(String text, char target, int start) {
        char quote = 0;
        boolean escaped = false;
        int parentheses = 0;
        int brackets = 0;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
                continue;
            }

            if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '(') {
                parentheses++;
            } else if (c == ')') {
                parentheses = Math.max(0, parentheses - 1);
            } else if (c == '[') {
                brackets++;
            } else if (c == ']') {
                brackets = Math.max(0, brackets - 1);
            } else if (c == target && parentheses == 0 && brackets == 0) {
                return i;
            }
        }

        return -1;
    }

    private static int findMatchingBrace(String text, int open) {
        int depth = 1;
        char quote = 0;
        boolean escaped = false;

        for (int i = open + 1; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
                continue;
            }

            if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;

                if (depth == 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    private static Map<String, String> parseCssDeclarations(String body) {
        Map<String, String> declarations = new HashMap<>();

        for (String declaration : splitCss(body, ';')) {
            declaration = declaration.trim();

            if (declaration.isEmpty()) {
                continue;
            }

            int colon = findCssChar(declaration, ':', 0);

            if (colon <= 0) {
                LOGGER.fine("Declaración CSS inválida: " + declaration);
                continue;
            }

            String property = declaration.substring(0, colon).trim().toLowerCase(Locale.ROOT);

            String value = declaration.substring(colon + 1).trim();

            if (!property.isEmpty() && !value.isEmpty()) {
                declarations.put(property, value);
            }
        }

        return declarations;
    }

    private static List<String> splitCss(String text, char separator) {
        List<String> result = new ArrayList<>();
        char quote = 0;
        boolean escaped = false;
        int parentheses = 0;
        int start = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
                continue;
            }

            if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '(') {
                parentheses++;
            } else if (c == ')') {
                parentheses = Math.max(0, parentheses - 1);
            } else if (c == separator && parentheses == 0) {
                result.add(text.substring(start, i));
                start = i + 1;
            }
        }

        result.add(text.substring(start));
        return result;
    }

    private static boolean mediaQueryMatches(String query, int viewportWidth) {
        String normalized = query.toLowerCase(Locale.ROOT);

        // Un navegador sin soporte de medios impresos solo aplica screen.
        if (normalized.contains("print")) {
            return false;
        }

        Matcher min = Pattern.compile("\\(\\s*min-width\\s*:\\s*(\\d+)\\s*(px)?\\s*\\)").matcher(normalized);

        while (min.find()) {
            if (viewportWidth < Integer.parseInt(min.group(1))) {
                return false;
            }
        }

        Matcher max = Pattern.compile("\\(\\s*max-width\\s*:\\s*(\\d+)\\s*(px)?\\s*\\)").matcher(normalized);

        while (max.find()) {
            if (viewportWidth > Integer.parseInt(max.group(1))) {
                return false;
            }
        }

        // No aceptar condiciones que este parser todavía no entiende.
        String remaining = normalized
                .replaceAll("\\(\\s*(min-width|max-width)\\s*:\\s*\\d+\\s*(px)?\\s*\\)", "")
                .replaceAll("\\bonly\\b|\\bscreen\\b|\\band\\b", "")
                .replaceAll("\\s+", " ")
                .trim();

        return remaining.isEmpty();
    }

    /**
     * Aplica una declaración CSS a un objeto T3Style.
     *
     * @param style Objeto T3Style
     * @param property Propiedad CSS
     * @param value Valor de la propiedad
     */
    public static void applyDeclaration(T3Style style, String property, String value) {
        if (style == null || property == null || value == null) {
            return;
        }

        property = property.trim().toLowerCase();
        value = value.trim();

        switch (property) {
            case "color":
                style.setColor(parseColor(value));
                break;

            case "font-size":
                style.setFontSize(parseDimension(value));
                break;

            case "font-weight":
                style.setFontWeight(parseFontWeight(value));
                break;

            case "font-family":
                style.setFontFamily(value.replace("\"", "").replace("'", ""));
                break;

            case "text-align":
                style.setTextAlign(value);
                break;

            case "text-decoration":
                style.setTextDecoration(value);
                break;

            case "background-color":
                style.setBackgroundColor(parseColor(value));
                break;

            case "background":
                Color background = extractBackgroundColor(value);
                if (background != null) {
                    style.setBackgroundColor(background);
                }
                break;

            case "display":
                style.setDisplay(value);
                break;

            case "margin":
                parseMarginPadding(style, value, true);
                break;

            case "padding":
                parseMarginPadding(style, value, false);
                break;

            case "margin-top":
            case "margin-right":
            case "margin-bottom":
            case "margin-left": {
                int dimension = parseDimension(value);
                if (dimension >= 0) {
                    switch (property) {
                        case "margin-top":
                            style.setMarginTop(dimension);
                            break;
                        case "margin-right":
                            style.setMarginRight(dimension);
                            break;
                        case "margin-bottom":
                            style.setMarginBottom(dimension);
                            break;
                        case "margin-left":
                            style.setMarginLeft(dimension);
                            break;
                    }
                }
                break;
            }

            case "padding-top":
            case "padding-right":
            case "padding-bottom":
            case "padding-left": {
                int dimension = parseDimension(value);
                if (dimension >= 0) {
                    switch (property) {
                        case "padding-top":
                            style.setPaddingTop(dimension);
                            break;
                        case "padding-right":
                            style.setPaddingRight(dimension);
                            break;
                        case "padding-bottom":
                            style.setPaddingBottom(dimension);
                            break;
                        case "padding-left":
                            style.setPaddingLeft(dimension);
                            break;
                    }
                }
                break;
            }

            case "border-width":
                style.setBorderWidth(parseDimension(value));
                break;

            case "border-color":
                style.setBorderColor(parseColor(value));
                break;

            case "border-radius":
                style.setBorderRadius(parseDimension(value));
                break;

            case "box-shadow":
                style.setBoxShadow(value);
                break;

            case "flex-direction":
                style.setFlexDirection(value);
                break;

            case "justify-content":
                style.setJustifyContent(value);
                break;

            case "align-items":
                style.setAlignItems(value);
                break;

            case "width":
            case "max-width":
                style.setWidth(value);
                break;

            case "height":
                style.setHeight(value);
                break;

            case "float":
                style.setDisplay("inline-block");
                break;

            default:
                LOGGER.fine("Propiedad CSS no soportada: "
                        + property + " = " + value);
        }
    }

    /**
     * Parsea un color CSS.
     *
     * @param value Valor del color (hex, nombre, rgb, rgba)
     * @return Objeto Color o null si es inválido
     */
    public static Color parseColor(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim().toLowerCase(Locale.ROOT);

        // !important no forma parte del valor del color.
        value = value.replaceFirst("\\s*!\\s*important\\s*$", "").trim();

        if ("transparent".equals(value)) {
            return new Color(0, 0, 0, 0);
        }

        // Los colores heredados requieren resolución en el motor CSS.
        if ("inherit".equals(value)
                || "initial".equals(value)
                || "unset".equals(value)
                || "currentcolor".equals(value)) {
            return null;
        }

        try {
            if (value.matches("#[0-9a-f]{3}")) {
                int r = Integer.parseInt(value.substring(1, 2), 16) * 17;
                int g = Integer.parseInt(value.substring(2, 3), 16) * 17;
                int b = Integer.parseInt(value.substring(3, 4), 16) * 17;
                return new Color(r, g, b);
            }

            if (value.matches("#[0-9a-f]{6}")) {
                return new Color(Integer.parseInt(value.substring(1), 16));
            }

            if (value.matches("#[0-9a-f]{8}")) {
                long rgba = Long.parseLong(value.substring(1), 16);
                return new Color(
                        (int) ((rgba >> 24) & 0xFF),
                        (int) ((rgba >> 16) & 0xFF),
                        (int) ((rgba >> 8) & 0xFF),
                        (int) (rgba & 0xFF)
                );
            }

            if (value.startsWith("#")) {
                return Color.decode(value);
            }
            if (value.startsWith("rgb(")) {
                String[] rgb = value.replace("rgb(", "").replace(")", "").split(",");
                if (rgb.length == 3) {
                    return new Color(
                            clamp(Integer.parseInt(rgb[0].trim()), 0, 255),
                            clamp(Integer.parseInt(rgb[1].trim()), 0, 255),
                            clamp(Integer.parseInt(rgb[2].trim()), 0, 255)
                    );
                }
            }
            if (value.startsWith("rgba(")) {
                String[] rgba = value.replace("rgba(", "").replace(")", "").split(",");
                if (rgba.length == 4) {
                    return new Color(
                            clamp(Integer.parseInt(rgba[0].trim()), 0, 255),
                            clamp(Integer.parseInt(rgba[1].trim()), 0, 255),
                            clamp(Integer.parseInt(rgba[2].trim()), 0, 255),
                            clamp((int) (Float.parseFloat(rgba[3].trim()) * 255), 0, 255)
                    );
                }
            }
            if (value.matches("[0-9a-f]{6}")) {
                return new Color(Integer.parseInt(value, 16));
            }
            return switch (value) {
                case "red" ->
                    Color.RED;
                case "blue" ->
                    Color.BLUE;
                case "green" ->
                    Color.GREEN;
                case "black" ->
                    Color.BLACK;
                case "white" ->
                    Color.WHITE;
                case "f8f9fa" ->
                    new Color(248, 249, 250);
                case "6c757d" ->
                    new Color(108, 117, 125);
                case "212529" ->
                    new Color(33, 37, 41);
                case "ced4da" ->
                    new Color(206, 212, 218);
                case "dc3545" ->
                    new Color(220, 53, 69);
                case "0d6efd" ->
                    new Color(13, 110, 253);
                default -> {
                    LOGGER.warning("Color no reconocido: " + value + "; usando null");
                    yield null;
                }
            };
        } catch (Exception e) {
            LOGGER.warning("Error al parsear color: " + value + "; usando null - " + e.getMessage());
            return null;
        }
    }

    /**
     * Parsea una dimensión CSS.
     *
     * @param value Valor de la dimensión (px, rem, %, auto)
     * @return Valor en píxeles o -1 para auto
     */
    public static int parseDimension(String value) {
        if (value == null) {
            return 0;
        }

        value = value.trim().toLowerCase();

        if (value.isEmpty()) {
            return 0;
        }

        if ("auto".equals(value)) {
            return -1;
        }

        // Los porcentajes se resuelven en el motor de renderizado.
        if (value.endsWith("%")) {
            return -1;
        }

        // Para no interpretar valores shorthand, como "0 auto",
        // como si fueran una dimensión individual.
        if (value.matches(".*\\s+.*")) {
            LOGGER.fine("Valor CSS compuesto ignorado como dimensión: " + value);
            return 0;
        }

        try {
            if (value.endsWith("rem")) {
                return Math.round(Float.parseFloat(value.substring(0, value.length() - 3).trim()) * 16);
            }

            if (value.endsWith("px")) {
                value = value.substring(0, value.length() - 2).trim();
            }

            return Math.round(Float.parseFloat(value));
        } catch (NumberFormatException e) {
            LOGGER.fine("Dimensión CSS no compatible: " + value);
            return 0;
        }
    }

    /**
     * Parsea el peso de fuente CSS.
     *
     * @param value normal, bold, bolder, lighter o 100-900
     * @return Peso CSS normalizado entre 100 y 900
     */
    private static int parseFontWeight(String value) {
        if (value == null) {
            return -1;
        }

        value = value.trim().toLowerCase(Locale.ROOT);

        return switch (value) {
            case "normal" ->
                400;
            case "bold" ->
                700;
            case "bolder" ->
                700;
            case "lighter" ->
                300;
            default -> {
                try {
                    int weight = Integer.parseInt(value);

                    if (weight >= 100 && weight <= 900 && weight % 100 == 0) {
                        yield weight;
                    }

                    LOGGER.fine("Peso de fuente CSS no válido: " + value);
                    yield -1;
                } catch (NumberFormatException e) {
                    LOGGER.fine("Peso de fuente CSS no compatible: " + value);
                    yield -1;
                }
            }
        };
    }

    /**
     * Limita un valor a un rango.
     *
     * @param value Valor
     * @param min Mínimo
     * @param max Máximo
     * @return Valor limitado
     */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Parsea márgenes o paddings.
     *
     * @param style Objeto T3Style
     * @param value Valor CSS
     * @param isMargin Si es margen (true) o padding (false)
     */
    private static void parseMarginPadding(T3Style style, String value, boolean isMargin) {
        Matcher matcher = MARGIN_PADDING_PATTERN.matcher(value);
        if (matcher.matches()) {
            String top = matcher.group(1);
            String right = matcher.group(2) != null ? matcher.group(2) : top;
            String bottom = matcher.group(3) != null ? matcher.group(3) : top; // Corregido de "custom" a "bottom"
            String left = matcher.group(4) != null ? matcher.group(4) : right;
            if (isMargin) {
                style.setMarginTop(top.equals("auto") ? -1 : parseDimension(top));
                style.setMarginRight(right.equals("auto") ? -1 : parseDimension(right));
                style.setMarginBottom(bottom.equals("auto") ? -1 : parseDimension(bottom)); // Corregido a "bottom"
                style.setMarginLeft(left.equals("auto") ? -1 : parseDimension(left));
            } else {
                style.setPaddingTop(parseDimension(top));
                style.setPaddingRight(parseDimension(right));
                style.setPaddingBottom(parseDimension(bottom)); // Corregido a "bottom"
                style.setPaddingLeft(parseDimension(left));
            }
        } else {
            int dim = parseDimension(value);
            if (isMargin) {
                style.setMarginTop(dim);
                style.setMarginRight(dim);
                style.setMarginBottom(dim);
                style.setMarginLeft(dim);
            } else {
                style.setPaddingTop(dim);
                style.setPaddingRight(dim);
                style.setPaddingBottom(dim);
                style.setPaddingLeft(dim);
            }
        }
    }

    /**
     * Calcula el estilo efectivo para un nodo HTML.
     *
     * @param node Nodo HTML
     * @param inherited Estilo heredado
     * @param rules Reglas CSS aplicables
     * @return Estilo computado
     */
    public static T3Style computeT3Style(T3HtmlNode node, T3Style inherited, List<T3CSSRule> rules) {
        T3Style computed = new T3Style();
        if (inherited != null) {
            computed.setColor(inherited.getColor());
            computed.setFontSize(inherited.getFontSize());
            computed.setFontWeight(inherited.getFontWeight());
            computed.setFontFamily(inherited.getFontFamily());
            computed.setTextAlign(inherited.getTextAlign());
        }

        String classAttr = node.getAttributes().get("class");
        if (classAttr != null) {
            for (String cls : classAttr.split("\\s+")) {
                Map<String, String> bootstrapDecls = T3Style.BOOTSTRAP_STYLES.get(cls);
                if (bootstrapDecls != null) {
                    for (Map.Entry<String, String> decl : bootstrapDecls.entrySet()) {
                        applyDeclaration(computed, decl.getKey(), decl.getValue());
                    }
                }
            }
        }

        List<T3CSSRule> matchingRules = RULE_CACHE.computeIfAbsent(rules, r -> {
            List<T3CSSRule> sorted = new ArrayList<>(r);
            sorted.sort(Comparator.comparingInt(T3CSSRule::getSpecificity));
            return Collections.unmodifiableList(sorted);
        });

        for (T3CSSRule rule : matchingRules) {
            if (rule.matches(node)) {
                for (Map.Entry<String, String> decl : rule.getDeclarations().entrySet()) {
                    applyDeclaration(computed, decl.getKey(), decl.getValue());
                }
            }
        }

        T3Style inline = parse(node.getAttributes().get("style"));
        mergeT3Styles(computed, inline);

        return computed;
    }

    /**
     * Combina dos estilos, dando prioridad al estilo override.
     *
     * @param base Estilo base
     * @param override Estilo que sobrescribe
     */
    public static void mergeT3Styles(T3Style base, T3Style override) {
        if (override.getColor() != null) {
            base.setColor(override.getColor());
        }
        if (override.getFontSize() >= 0) {
            base.setFontSize(override.getFontSize());
        }
        if (override.getFontWeight() >= 0) {
            base.setFontWeight(override.getFontWeight());
        }
        if (override.getFontFamily() != null) {
            base.setFontFamily(override.getFontFamily());
        }
        if (override.getTextAlign() != null) {
            base.setTextAlign(override.getTextAlign());
        }
        if (!"none".equals(override.getTextDecoration())) {
            base.setTextDecoration(override.getTextDecoration());
        }
        if (override.getBackgroundColor() != null) {
            base.setBackgroundColor(override.getBackgroundColor());
        }
        if (!"block".equals(override.getDisplay())) {
            base.setDisplay(override.getDisplay());
        }
        if (override.getMarginTop() >= 0) {
            base.setMarginTop(override.getMarginTop());
        }
        if (override.getMarginRight() >= 0) {
            base.setMarginRight(override.getMarginRight());
        }
        if (override.getMarginBottom() >= 0) {
            base.setMarginBottom(override.getMarginBottom());
        }
        if (override.getMarginLeft() >= 0) {
            base.setMarginLeft(override.getMarginLeft());
        }
        base.setPaddingTop(override.getPaddingTop());
        base.setPaddingRight(override.getPaddingRight());
        base.setPaddingBottom(override.getPaddingBottom());
        base.setPaddingLeft(override.getPaddingLeft());
        base.setBorderWidth(override.getBorderWidth());
        if (override.getBorderColor() != null) {
            base.setBorderColor(override.getBorderColor());
        }
        base.setBorderRadius(override.getBorderRadius());
        if (!"none".equals(override.getBoxShadow())) {
            base.setBoxShadow(override.getBoxShadow());
        }
        if (!"row".equals(override.getFlexDirection())) {
            base.setFlexDirection(override.getFlexDirection());
        }
        if (!"flex-start".equals(override.getJustifyContent())) {
            base.setJustifyContent(override.getJustifyContent());
        }
        if (override.getWidth() != null) {
            base.setWidth(override.getWidth());
        }
        if (override.getHeight() != null) {
            base.setHeight(override.getHeight());
        }
    }

    // Auxiliar para extraer color de fondo
    private static Color extractBackgroundColor(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        // Una imagen de fondo no es un color.
        if (normalized.contains("url(")) {
            return null;
        }

        // Busca un color hexadecimal dentro del shorthand.
        Matcher matcher = Pattern.compile("(?i)(#[0-9a-f]{3,8})\\b").matcher(normalized);

        if (matcher.find()) {
            return parseColor(matcher.group(1));
        }

        // No enviar posiciones, repeticiones ni otros valores a parseColor.
        if (normalized.matches("(transparent|#[0-9a-f]{3}|#[0-9a-f]{6}|#[0-9a-f]{8}"
                + "|red|blue|green|black|white|yellow|gray|grey|orange|purple)")) {
            return parseColor(normalized);
        }

        return null;
    }
}

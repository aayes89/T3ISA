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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * @author Slam HTML Parser with enhanced error handling and additional features
 * Supports HTML comments, CDATA sections, DOCTYPE declarations, and special
 * handling for script/style tags
 */
public class T3HtmlParser {

    private static final Set<String> SELF_CLOSING_TAGS = new HashSet<>(Arrays.asList(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr"
    ));
    private static final Set<String> AUTO_CLOSE_TAGS = new HashSet<>(Arrays.asList(
            "li", "p", "dt", "dd", "option", "tr", "td", "th"
    ));

    // TODO - lógica de preservación de espacios <pre>
    private static final Set<String> WHITESPACE_PRESERVING_TAGS = new HashSet<>(Arrays.asList(
            "pre", "code", "textarea"
    ));

    private int pos;
    private int depth = 0;
    private final String content;
    private final int contentLength;

    public T3HtmlParser(String htmlContent) {
        if (htmlContent == null) {
            throw new IllegalArgumentException("HTML content cannot be null");
        }
        this.content = htmlContent;
        this.contentLength = htmlContent.length();
        this.pos = 0;
    }

    public T3HtmlNode parse() {
        T3HtmlNode root = new T3HtmlNode("document");

        try {
            while (pos < contentLength) {
                T3HtmlNode child = parseNode();
                if (child != null) {
                    root.addChild(child);
                }
            }

            return root;
        } catch (Exception e) {
            throw new ParseException("Failed to parse HTML at position " + pos + ": " + e.getMessage(), e);
        }
    }

    private T3HtmlNode parseNode() {
        if (pos >= contentLength) {
            return null;
        }

        if (content.charAt(pos) != '<') {
            return parseTextNode();
        }

        if (content.startsWith("<!--", pos)) {
            return parseComment();
        }
        if (content.startsWith("<![CDATA[", pos)) {
            return parseCDATA();
        }
        if (content.regionMatches(true, pos, "<!DOCTYPE", 0, 9)) {
            return parseDoctype();
        }
        if (content.startsWith("</", pos)) {
            skipTag();
            return null;
        }

        return parseElement();
    }

    private T3HtmlNode parseElement() {
        int startPos = pos;
        pos++;

        String tagName = readTagName().toLowerCase(java.util.Locale.ROOT);
        if (tagName.isEmpty()) {
            pos = startPos + 1;
            T3HtmlNode textNode = new T3HtmlNode("#text");
            textNode.setText("<");
            return textNode;
        }

        T3HtmlNode node = new T3HtmlNode(tagName);
        parseAttributes(node);

        if (pos >= contentLength) {
            return node;
        }

        boolean selfClosing = content.charAt(pos) == '/' || isSelfClosing(tagName);

        if (content.charAt(pos) == '/') {
            pos++;
            skipWhitespace();
        }

        // Prevenir colisión de etiquetas anidadas sin cierre
        while (pos < contentLength && content.charAt(pos) != '>' && content.charAt(pos) != '<') {
            pos++;
        }

        if (pos < contentLength && content.charAt(pos) == '>') {
            pos++;
        } else {
            selfClosing = true;
        }

        if (selfClosing) {
            return node;
        }

        if (tagName.equals("script") || tagName.equals("style")) {
            node.setText(readRawContentUntil("</" + tagName + ">"));
            if (pos < contentLength) {
                skipTag();
            }
            return node;
        }

        // Límite de profundidad máxima para evitar StackOverflow en el parser (Wikipedia)
        if (depth > 200) {
            return node;
        }

        depth++;

        while (pos < contentLength) {
            int loopStartPos = pos;

            if (content.regionMatches(true, pos, "</", 0, 2)) {
                int closeStart = pos;
                pos += 2;
                String closeName = readTagName().toLowerCase(Locale.ROOT);
                skipTag();

                if (closeName.equals(tagName)) {
                    depth--;
                    return node;
                }

                if (AUTO_CLOSE_TAGS.contains(tagName)) {
                    pos = closeStart;
                    depth--;
                    return node;
                }

                continue;
            }

            if (content.charAt(pos) == '<' && AUTO_CLOSE_TAGS.contains(tagName) && startsAutoCloseTag(tagName, pos)) {
                depth--;
                return node;
            }

            T3HtmlNode child = parseNode();
            if (child != null) {
                node.addChild(child);
            }

            if (pos == loopStartPos) {
                pos++;
            }
        }

        depth--;
        return node;
    }

    private boolean startsAutoCloseTag(String currentTag, int at) {
        if (content.charAt(at) != '<' || at + 1 >= contentLength) {
            return false;
        }

        int nameStart = at + 1;
        if (content.charAt(nameStart) == '/') {
            return false;
        }

        int end = nameStart;
        while (end < contentLength && (Character.isLetterOrDigit(content.charAt(end)) || content.charAt(end) == '-' || content.charAt(end) == ':')) {
            end++;
        }

        if (end == nameStart) {
            return false;
        }

        String nextTag = content.substring(nameStart, end).toLowerCase(java.util.Locale.ROOT);

        if (currentTag.equals("li")) {
            return nextTag.equals("li");
        }
        if (currentTag.equals("p")) {
            return nextTag.equals("p") || isBlockTag(nextTag);
        }
        if (currentTag.equals("dt") || currentTag.equals("dd")) {
            return nextTag.equals("dt") || nextTag.equals("dd");
        }
        if (currentTag.equals("option")) {
            return nextTag.equals("option") || nextTag.equals("optgroup");
        }
        if (currentTag.equals("tr")) {
            return nextTag.equals("tr");
        }
        if (currentTag.equals("td") || currentTag.equals("th")) {
            return nextTag.equals("td") || nextTag.equals("th") || nextTag.equals("tr");
        }

        return false;
    }

    private boolean isBlockTag(String tag) {
        return new HashSet<>(Arrays.asList(
                "address", "article", "aside", "blockquote", "div", "dl",
                "fieldset", "footer", "form", "h1", "h2", "h3", "h4",
                "h5", "h6", "header", "hr", "main", "nav", "ol",
                "p", "pre", "section", "table", "ul"
        )).contains(tag);
    }

    private T3HtmlNode parseTextNode() {
        String text = readText();
        if (text.isEmpty()) {
            return null;
        }

        T3HtmlNode textNode = new T3HtmlNode("#text");
        textNode.setText(decodeHtmlEntities(text));

        return textNode;
    }

    private String decodeHtmlEntities(String text) {
        if (text == null || text.indexOf('&') < 0) {
            return text;
        }

        StringBuilder result = new StringBuilder(text.length());

        for (int i = 0; i < text.length();) {
            char current = text.charAt(i);

            if (current != '&') {
                result.append(current);
                i++;
                continue;
            }

            int semicolon = text.indexOf(';', i + 1);

            if (semicolon < 0 || semicolon - i > 32) {
                result.append(current);
                i++;
                continue;
            }

            String entity = text.substring(i + 1, semicolon);
            String decoded = decodeHtmlEntity(entity);

            if (decoded == null) {
                result.append(current);
                i++;
            } else {
                result.append(decoded);
                i = semicolon + 1;
            }
        }

        return result.toString();
    }

    private String decodeHtmlEntity(String entity) {
        switch (entity) {
            case "amp":
                return "&";
            case "lt":
                return "<";
            case "gt":
                return ">";
            case "quot":
                return "\"";
            case "apos":
                return "'";
            case "nbsp":
                return "\u00A0";
            case "times":
                return "\u00D7";
            case "copy":
                return "\u00A9";
            case "reg":
                return "\u00AE";
            case "trade":
                return "\u2122";
            case "aacute":
                return "\u00E1";
            case "Aacute":
                return "\u00C1";
            case "eacute":
                return "\u00E9";
            case "Eacute":
                return "\u00C9";
            case "iacute":
                return "\u00ED";
            case "Iacute":
                return "\u00CD";
            case "oacute":
                return "\u00F3";
            case "Oacute":
                return "\u00D3";
            case "uacute":
                return "\u00FA";
            case "Uacute":
                return "\u00DA";
            case "ntilde":
                return "\u00F1";
            case "Ntilde":
                return "\u00D1";
            case "uuml":
                return "\u00FC";
            case "Uuml":
                return "\u00DC";
            case "iexcl":
                return "\u00A1";
            case "iquest":
                return "\u00BF";
            case "mdash":
                return "\u2014";
            case "ndash":
                return "\u2013";
            case "hellip":
                return "\u2026";
            case "lsquo":
                return "\u2018";
            case "rsquo":
                return "\u2019";
            case "ldquo":
                return "\u201C";
            case "rdquo":
                return "\u201D";
            default:
                break;
        }

        try {
            if (entity.startsWith("#x") || entity.startsWith("#X")) {
                int codePoint = Integer.parseInt(entity.substring(2), 16);
                return validCodePoint(codePoint) ? new String(Character.toChars(codePoint)) : null;
            }

            if (entity.startsWith("#")) {
                int codePoint = Integer.parseInt(entity.substring(1));
                return validCodePoint(codePoint) ? new String(Character.toChars(codePoint)) : null;
            }
        } catch (NumberFormatException ex) {
            return null;
        }

        return null;
    }

    private T3HtmlNode parseComment() {
        pos += 4;
        int start = pos;
        while (pos < contentLength && !content.startsWith("-->", pos)) {
            pos++;
        }
        if (pos >= contentLength) {
            throw new ParseException("Unclosed comment at position " + start);
        }
        String comment = content.substring(start, pos);
        pos += 3;
        T3HtmlNode commentNode = new T3HtmlNode("#comment");
        commentNode.setText(comment);
        return commentNode;
    }

    private T3HtmlNode parseCDATA() {
        pos += 9;
        int start = pos;
        while (pos < contentLength && !content.startsWith("]]>", pos)) {
            pos++;
        }
        if (pos >= contentLength) {
            throw new ParseException("Unclosed CDATA section at position " + start);
        }
        String cdata = content.substring(start, pos);
        pos += 3;
        T3HtmlNode cdataNode = new T3HtmlNode("#cdata");
        cdataNode.setText(cdata);
        return cdataNode;
    }

    private T3HtmlNode parseDoctype() {
        pos += 9;
        int start = pos;
        while (pos < contentLength && content.charAt(pos) != '>') {
            pos++;
        }
        if (pos >= contentLength) {
            throw new ParseException("Unclosed DOCTYPE at position " + start);
        }
        String doctype = content.substring(start, pos).trim();
        pos++;
        T3HtmlNode doctypeNode = new T3HtmlNode("#doctype");
        doctypeNode.setText(doctype);
        return doctypeNode;
    }

    private String readTagName() {
        skipWhitespace();
        int start = pos;
        while (pos < contentLength && (Character.isLetterOrDigit(content.charAt(pos)) || content.charAt(pos) == '-' || content.charAt(pos) == ':')) {
            pos++;
        }
        return content.substring(start, pos).trim();
    }

    private void parseAttributes(T3HtmlNode node) {
        while (pos < contentLength && content.charAt(pos) != '>' && content.charAt(pos) != '/') {
            skipWhitespace();

            // Añado interrupción por '<' para evitar parsear basura.
            if (pos >= contentLength || content.charAt(pos) == '>' || content.charAt(pos) == '/' || content.charAt(pos) == '<') {
                break;
            }

            int oldPos = pos;
            String key = readAttributeName();

            if (key.isEmpty()) {
                pos++;
                continue;
            }

            skipWhitespace();
            String value = "";

            if (pos < contentLength && content.charAt(pos) == '=') {
                pos++;
                value = readAttributeValue();
            }

            node.attributes.put(key.toLowerCase(java.util.Locale.ROOT), value);

            if (pos <= oldPos) {
                pos = oldPos + 1;
            }
        }
    }

    private String readAttributeName() {
        int start = pos;
        while (pos < contentLength && (Character.isLetterOrDigit(content.charAt(pos)) || content.charAt(pos) == '-' || content.charAt(pos) == ':')) {
            pos++;
        }
        return content.substring(start, pos);
    }

    private String readAttributeValue() {
        skipWhitespace();

        if (pos >= contentLength) {
            return "";
        }

        char quote = content.charAt(pos);

        if (quote == '"' || quote == '\'') {
            pos++;
            int start = pos;

            while (pos < contentLength && content.charAt(pos) != quote) {
                pos++;
            }

            String value = content.substring(start, pos);

            if (pos < contentLength) {
                pos++;
            }

            return decodeHtmlEntities(value);
        }

        int start = pos;

        while (pos < contentLength && !Character.isWhitespace(content.charAt(pos)) && content.charAt(pos) != '>') {
            pos++;
        }

        return decodeHtmlEntities(content.substring(start, pos));
    }

    private String readRawContentUntil(String endTag) {
        int start = pos;
        while (pos < contentLength && !content.regionMatches(true, pos, endTag, 0, endTag.length())) {
            pos++;
        }
        return content.substring(start, pos);
    }

    private boolean isSelfClosing(String tag) {
        return tag != null && SELF_CLOSING_TAGS.contains(tag.toLowerCase(java.util.Locale.ROOT));
    }

    private String readText() {
        int start = pos;
        while (pos < contentLength && content.charAt(pos) != '<') {
            pos++;
        }
        return content.substring(start, pos);
    }

    private void skip(char c) {
        if (pos < contentLength && content.charAt(pos) == c) {
            pos++;
        }
    }

    private void skipTag() {
        while (pos < contentLength && content.charAt(pos) != '>') {
            pos++;
        }
        if (pos < contentLength) {
            pos++;
        }
    }

    private void skipWhitespace() {
        while (pos < contentLength && Character.isWhitespace(content.charAt(pos))) {
            pos++;
        }
    }

    private boolean validCodePoint(int codePoint) {
        return Character.isValidCodePoint(codePoint) && !(codePoint >= 0xD800 && codePoint <= 0xDFFF) && codePoint != 0;
    }

    public static class ParseException extends RuntimeException {

        public ParseException(String message) {
            super(message);
        }

        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

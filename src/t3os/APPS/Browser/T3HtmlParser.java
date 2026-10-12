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
 */
import java.util.Arrays;
import java.util.HashSet;
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

    private static final Set<String> WHITESPACE_PRESERVING_TAGS = new HashSet<>(Arrays.asList(
            "pre", "code", "textarea"
    ));

    private int pos;
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
                skipWhitespace();
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
        if (content.startsWith("<!DOCTYPE", pos)) {
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
        pos++; // Skip '<'
        String tagName = readTagName();
        if (tagName.isEmpty()) {
            throw new ParseException("Invalid tag name at position " + startPos);
        }

        T3HtmlNode node = new T3HtmlNode(tagName);
        parseAttributes(node);

        if (pos >= contentLength) {
            throw new ParseException("Unclosed tag: " + tagName + " at position " + startPos);
        }

        if (content.charAt(pos - 1) == '/' || isSelfClosing(node.getTagName())) {
            skip('>');
            return node;
        }

        skip('>');
        // Special handling for script and style tags
        if (node.getTagName().equals("script") || node.getTagName().equals("style")) {
            node.setText(readRawContentUntil("</" + tagName + ">"));
            if (pos < contentLength && content.regionMatches(true, pos, "</" + tagName, 0, tagName.length() + 2)) {
                skipTag();
            }
            return node;
        }

        while (pos < contentLength && !content.regionMatches(true, pos, "</" + tagName, 0, tagName.length() + 2)) {
            T3HtmlNode child = parseNode();
            if (child != null) {
                node.addChild(child);
            }
        }

        if (pos >= contentLength) {
            throw new ParseException("Unclosed tag: " + tagName + " at position " + startPos);
        }

        if (content.regionMatches(true, pos, "</" + tagName, 0, tagName.length() + 2)) {
            skipTag();
        }
        return node;
    }

    private T3HtmlNode parseTextNode() {
        String text = readText();
        if (text.isEmpty()) {
            return null;
        }
        T3HtmlNode textNode = new T3HtmlNode("#text");
        textNode.setText(isWhitespacePreserving(textNode.getParent()) ? text : text.trim());
        return textNode;
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
        while (pos < contentLength
                && (Character.isLetterOrDigit(content.charAt(pos))
                || content.charAt(pos) == '-'
                || content.charAt(pos) == ':')) {
            pos++;
        }
        return content.substring(start, pos).trim();
    }

    private void parseAttributes(T3HtmlNode node) {
        while (pos < contentLength && content.charAt(pos) != '>' && content.charAt(pos) != '/') {
            skipWhitespace();
            if (pos >= contentLength || content.charAt(pos) == '>' || content.charAt(pos) == '/') {
                break;
            }
            String key = readAttributeName();
            String value = "";
            skipWhitespace();
            if (pos < contentLength && content.charAt(pos) == '=') {
                skip('=');
                value = readAttributeValue();
            }
            node.attributes.put(key.toLowerCase(), value);
        }
    }

    private String readAttributeName() {
        int start = pos;
        while (pos < contentLength
                && (Character.isLetterOrDigit(content.charAt(pos))
                || content.charAt(pos) == '-'
                || content.charAt(pos) == ':')) {
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
        if (quote != '"' && quote != '\'') {
            int start = pos;
            while (pos < contentLength && !Character.isWhitespace(content.charAt(pos)) && content.charAt(pos) != '>' && content.charAt(pos) != '/') {
                pos++;
            }
            return content.substring(start, pos).trim();
        }

        pos++;
        int start = pos;
        while (pos < contentLength && content.charAt(pos) != quote) {
            pos++;
        }
        if (pos >= contentLength) {
            throw new ParseException("Unclosed attribute quote at position " + start);
        }
        String value = content.substring(start, pos);
        pos++;
        return value;
    }

    private String readRawContentUntil(String endTag) {
        int start = pos;
        while (pos < contentLength && !content.regionMatches(true, pos, endTag, 0, endTag.length())) {
            pos++;
        }
        return content.substring(start, pos);
    }

    private void skipUntil(char target) {
        while (pos < contentLength && content.charAt(pos) != target) {
            pos++;
        }
        if (pos < contentLength) {
            pos++;
        }
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

    private boolean isSelfClosing(String tag) {
        return SELF_CLOSING_TAGS.contains(tag.toLowerCase());
    }

    private boolean isWhitespacePreserving(T3HtmlNode parent) {
        return parent != null && WHITESPACE_PRESERVING_TAGS.contains(parent.getTagName().toLowerCase());
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

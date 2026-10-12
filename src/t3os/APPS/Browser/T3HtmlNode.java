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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Slam Represents a node in an HTML document tree, which can be an
 * element, text, comment, CDATA section, or DOCTYPE declaration.
 */
public class T3HtmlNode {

    public String tagName;
    public String text;
    public Map<String, String> attributes;
    public List<T3HtmlNode> children;
    public T3HtmlNode parent;

    /**
     * Constructs a new HtmlNode with the specified tag name.
     *
     * @param tagName the name of the tag (e.g., "div", "#text", "#comment")
     * @throws IllegalArgumentException if tagName is null
     */
    public T3HtmlNode(String tagName) {
        if (tagName == null) {
            throw new IllegalArgumentException("Tag name cannot be null");
        }
        this.tagName = tagName.toLowerCase();
        this.text = "";
        this.attributes = new HashMap<>();
        this.children = new ArrayList<>();
        this.parent = null;
    }

    public T3HtmlNode getParent() {
        return parent;
    }

    /*
    * Método para obtener el índice del nodo en su padre
     */
    public int getChildIndex(T3HtmlNode node) {
        if (parent == null) {
            return -1;
        }
        return parent.children.indexOf(node);
    }

    /*
    * Método para agregar hijos de manera segura
     */
    public void addChild(T3HtmlNode child) {
        if (child != null) {
            child.parent = this; // Asignar referencia al padre
            children.add(child);
        }
    }

    /**
     * Checks if this node is a text node.
     *
     * @return true if the node represents text content (tagName is "#text")
     */
    public boolean isTextNode() {
        return "#text".equals(tagName);
    }

    /**
     * Checks if this node is a comment node.
     *
     * @return true if the node represents a comment (tagName is "#comment")
     */
    public boolean isCommentNode() {
        return "#comment".equals(tagName);
    }

    /**
     * Checks if this node is a CDATA node.
     *
     * @return true if the node represents a CDATA section (tagName is "#cdata")
     */
    public boolean isCDataNode() {
        return "#cdata".equals(tagName);
    }

    /**
     * Checks if this node is a DOCTYPE node.
     *
     * @return true if the node represents a DOCTYPE declaration (tagName is
     * "#doctype")
     */
    public boolean isDoctypeNode() {
        return "#doctype".equals(tagName);
    }

    /**
     * Gets the tag name of this node.
     *
     * @return the tag name
     */
    public String getTagName() {
        return tagName;
    }

    /**
     * Gets the text content of this node.
     *
     * @return the text content, never null
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text content of this node.
     *
     * @param text the text content to set
     */
    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    /**
     * Gets the attributes of this node.
     *
     * @return an unmodifiable view of the attributes map
     */
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Gets the child nodes of this node.
     *
     * @return an unmodifiable view of the children list
     */
    public List<T3HtmlNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Returns a string representation of this node for debugging purposes.
     *
     * @return a string representation of the node
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HtmlNode{tagName='").append(tagName).append("'");
        if (!text.isEmpty()) {
            sb.append(", text='").append(text).append("'");
        }
        if (!attributes.isEmpty()) {
            sb.append(", attributes=").append(attributes);
        }
        if (!children.isEmpty()) {
            sb.append(", children=").append(children.size());
        }
        sb.append("}");
        return sb.toString();
    }

    public void setAttributes(Map<String, String> map) {
        this.attributes = map;
    }
}

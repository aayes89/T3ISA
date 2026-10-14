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

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Slam
 *
 * Almacena propiedades CSS parseadas con validación.
 */
public class T3Style {

    private Color color = null;
    private int fontSize = -1;
    private int fontWeight = -1;
    private String fontFamily = null;
    private String textAlign = null;
    private String textDecoration = "none";
    private Color backgroundColor = null;
    private String display = "block";
    private int marginTop = -1, marginRight = -1, marginBottom = -1, marginLeft = -1;
    private int paddingTop = 0, paddingRight = 0, paddingBottom = 0, paddingLeft = 0;
    private int borderWidth = 0;
    private Color borderColor = Color.BLACK;
    private int borderRadius = 0;
    private String boxShadow = "none";
    private String flexDirection = "row";
    private String justifyContent = "flex-start";
    private String width = null; // Soporta px, %, auto
    private String height = null; // Soporta px, %, auto
    private String overflow = "visible";
    private String top = "auto";
    private String right = "auto";
    private String bottom = "auto";
    private String left = "auto";
    private int zIndex = 0;
    private String floatElement = "none"; // Evitamos usar la palabra reservada 'float'

    public static final Map<String, Map<String, String>> BOOTSTRAP_STYLES;
    private String position = "static";
    private String alignItems = "stretch";

    static {
        Map<String, Map<String, String>> styles = new HashMap<>();
        styles.put("bg-primary", Map.of("background-color", "#007bff"));
        styles.put("bg-secondary", Map.of("background-color", "#6c757d"));
        styles.put("text-center", Map.of("text-align", "center"));
        styles.put("text-end", Map.of("text-align", "right"));
        styles.put("btn", Map.of(
                "padding", "8px 16px",
                "border-width", "1px",
                "border-color", "#000",
                "border-radius", "6px",
                "display", "inline-block",
                "font-size", "16px",
                "font-weight", "400",
                "text-align", "center"
        ));
        styles.put("btn-primary", Map.of(
                "background-color", "#0d6efd",
                "color", "#ffffff",
                "border-color", "#0d6efd"
        ));
        styles.put("container", Map.of(
                "margin-left", "auto",
                "margin-right", "auto",
                "padding-left", "15px",
                "padding-right", "15px",
                "width", "1200px"
        ));
        styles.put("form-label", Map.of(
                "display", "block",
                "margin-bottom", "8px",
                "font-size", "16px",
                "color", "#212529"
        ));
        styles.put("form-select", Map.of(
                "display", "block",
                "width", "100%",
                "padding", "8px 12px",
                "font-size", "16px",
                "border-width", "1px",
                "border-color", "#ced4da",
                "border-radius", "6px",
                "background-color", "#ffffff",
                "color", "#212529"
        ));
        styles.put("form-control", Map.of(
                "display", "block",
                "width", "100%",
                "padding", "8px 12px",
                "font-size", "16px",
                "border-width", "1px",
                "border-color", "#ced4da",
                "border-radius", "6px",
                "background-color", "#ffffff",
                "color", "#212529"
        ));
        styles.put("mb-3", Map.of("margin-bottom", "16px"));
        styles.put("mb-4", Map.of("margin-bottom", "24px"));
        styles.put("mt-1", Map.of("margin-top", "4px"));
        styles.put("w-100", Map.of("width", "100%"));
        styles.put("is-invalid", Map.of(
                "border-color", "#dc3545",
                "background-color", "#fff"
        ));
        styles.put("nav", Map.of(
                "display", "flex",
                "flex-direction", "row",
                "background-color", "#2c2c2c",
                "padding", "10px 15px",
                "width", "100%"
        ));
        styles.put("nav-link", Map.of(
                "color", "#ffffff",
                "text-decoration", "none",
                "padding", "5px 10px",
                "margin-right", "15px"
        ));
        styles.put("float-right", Map.of("float", "right"));
        styles.put("main-content", Map.of("margin-right", "200px"));
        BOOTSTRAP_STYLES = Collections.unmodifiableMap(styles);
    }

    /**
     * Crea un nuevo objeto Style con valores predeterminados.
     */
    public T3Style() {
    }

    // Getters y setters con validación
    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize >= 0 ? fontSize : -1;
    }

    public int getFontWeight() {
        return fontWeight;
    }

    public void setFontWeight(int fontWeight) {
        this.fontWeight = fontWeight >= 0 ? fontWeight : -1;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily != null && !fontFamily.trim().isEmpty() ? fontFamily.trim() : null;
    }

    public String getTextAlign() {
        return textAlign;
    }

    public void setTextAlign(String textAlign) {
        this.textAlign = textAlign != null && List.of("left", "center", "right").contains(textAlign.toLowerCase())
                ? textAlign.toLowerCase() : null;
    }

    public String getTextDecoration() {
        return textDecoration;
    }

    public void setTextDecoration(String textDecoration) {
        this.textDecoration = textDecoration != null ? textDecoration.toLowerCase() : "none";
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display != null && List.of("block", "inline", "flex", "inline-block").contains(display.toLowerCase())
                ? display.toLowerCase() : "block";
    }

    public int getMarginTop() {
        return marginTop;
    }

    public void setMarginTop(int marginTop) {
        this.marginTop = marginTop;
    }

    public int getMarginRight() {
        return marginRight;
    }

    public void setMarginRight(int marginRight) {
        this.marginRight = marginRight;
    }

    public int getMarginBottom() {
        return marginBottom;
    }

    public void setMarginBottom(int marginBottom) {
        this.marginBottom = marginBottom;
    }

    public int getMarginLeft() {
        return marginLeft;
    }

    public void setMarginLeft(int marginLeft) {
        this.marginLeft = marginLeft;
    }

    public int getPaddingTop() {
        return paddingTop;
    }

    public void setPaddingTop(int paddingTop) {
        this.paddingTop = Math.max(0, paddingTop);
    }

    public int getPaddingRight() {
        return paddingRight;
    }

    public void setPaddingRight(int paddingRight) {
        this.paddingRight = Math.max(0, paddingRight);
    }

    public int getPaddingBottom() {
        return paddingBottom;
    }

    public void setPaddingBottom(int paddingBottom) {
        this.paddingBottom = Math.max(0, paddingBottom);
    }

    public int getPaddingLeft() {
        return paddingLeft;
    }

    public void setPaddingLeft(int paddingLeft) {
        this.paddingLeft = Math.max(0, paddingLeft);
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(int borderWidth) {
        this.borderWidth = Math.max(0, borderWidth);
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor != null ? borderColor : Color.BLACK;
    }

    public int getBorderRadius() {
        return borderRadius;
    }

    public void setBorderRadius(int borderRadius) {
        this.borderRadius = Math.max(0, borderRadius);
    }

    public String getBoxShadow() {
        return boxShadow;
    }

    public void setBoxShadow(String boxShadow) {
        this.boxShadow = boxShadow != null ? boxShadow : "none";
    }

    public String getFlexDirection() {
        return flexDirection;
    }

    public void setFlexDirection(String flexDirection) {
        this.flexDirection = flexDirection != null && List.of("row", "column").contains(flexDirection.toLowerCase())
                ? flexDirection.toLowerCase() : "row";
    }

    public String getJustifyContent() {
        return justifyContent;
    }

    public void setJustifyContent(String justifyContent) {
        this.justifyContent = justifyContent != null && List.of("flex-start", "flex-end", "center", "space-between", "space-around").contains(justifyContent.toLowerCase())
                ? justifyContent.toLowerCase() : "flex-start";
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = normalizeDimension(width);
    }

    public void setHeight(String height) {
        this.height = normalizeDimension(height);
    }

    private static String normalizeDimension(String value) {
        if (value == null) {
            return null;
        }

        value = value.trim().toLowerCase(java.util.Locale.ROOT);

        if (value.equals("auto")
                || value.matches("\\d+(\\.\\d+)?(px|%)")
                || value.matches("\\d+(\\.\\d+)?rem")) {
            return value;
        }

        return null;
    }

    public String getHeight() {
        return height;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position != null && List.of("static", "absolute", "fixed", "relative").contains(position.toLowerCase())
                ? position.toLowerCase() : "static";
    }

    public String getAlignItems() {
        return alignItems;
    }

    public void setAlignItems(String alignItems) {
        this.alignItems = alignItems != null && List.of("flex-start", "flex-end", "center", "baseline", "stretch").contains(alignItems.toLowerCase())
                ? alignItems.toLowerCase() : "stretch";
    }

    public String getOverflow() {
        return overflow;
    }

    public void setOverflow(String overflow) {
        this.overflow = overflow != null && List.of("visible", "hidden", "scroll").contains(overflow.toLowerCase())
                ? overflow.toLowerCase() : "visible";
    }

    public String getTop() {
        return top;
    }

    public void setTop(String top) {
        this.top = normalizeDimension(top);
    }

    public String getRight() {
        return right;
    }

    public void setRight(String right) {
        this.right = normalizeDimension(right);
    }

    public String getBottom() {
        return bottom;
    }

    public void setBottom(String bottom) {
        this.bottom = normalizeDimension(bottom);
    }

    public String getLeft() {
        return left;
    }

    public void setLeft(String left) {
        this.left = normalizeDimension(left);
    }

    public int getZIndex() {
        return zIndex;
    }

    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    public String getFloatElement() {
        return floatElement;
    }

    public void setFloatElement(String floatElement) {
        this.floatElement = floatElement != null && List.of("left", "right", "none").contains(floatElement.toLowerCase())
                ? floatElement.toLowerCase() : "none";
    }
}

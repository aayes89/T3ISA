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
package t3os.APPS.Browser.Images;

/**
 *
 * @author Slam
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.Color;

import t3os.APPS.Browser.T3HtmlNode;
import t3os.APPS.Browser.T3HtmlParser;

import t3os.APPS.Browser.CSS.T3CSSUtils;

// Mates y Primitivas tomadas del proyecto JVMOS y JVMOS-JIT
// Adaptadas a Java desde ASM
public final class T3HtmlImageSVG {

    private static final int DEFAULT_SIZE = 100;
    private static final int MAX_DIMENSION = 2048;
    private static final long MAX_PIXELS = 4_194_304L;

    private T3HtmlImageSVG() {
    }

    public static T3HtmlImage decode(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            throw new IOException("SVG vacío");
        }

        try {
            String source = new String(data, java.nio.charset.StandardCharsets.UTF_8);

            T3HtmlNode document = new T3HtmlParser(source).parse();
            T3HtmlNode svg = findNode(document, "svg");

            if (svg == null) {
                throw new IOException("No se encontró <svg>");
            }

            Map<String, String> attrs = svg.getAttributes();

            float viewX = 0;
            float viewY = 0;
            float viewW = DEFAULT_SIZE;
            float viewH = DEFAULT_SIZE;

            String viewBox = attr(attrs, "viewBox");

            if (viewBox != null) {
                float[] vb = numbers(viewBox);
                if (vb.length == 4 && vb[2] > 0 && vb[3] > 0) {
                    viewX = vb[0];
                    viewY = vb[1];
                    viewW = vb[2];
                    viewH = vb[3];
                }
            }

            int width = dimension(attr(attrs, "width"), viewW);
            int height = dimension(attr(attrs, "height"), viewH);

            if (width <= 0) {
                width = Math.round(viewW);
            }
            if (height <= 0) {
                height = Math.round(viewH);
            }

            width = Math.max(1, Math.min(width, MAX_DIMENSION));
            height = Math.max(1, Math.min(height, MAX_DIMENSION));

            if ((long) width * height > MAX_PIXELS) {
                float factor = (float) Math.sqrt(MAX_PIXELS / ((double) width * height));
                width = Math.max(1, (int) (width * factor));
                height = Math.max(1, (int) (height * factor));
            }

            Raster r = new Raster(width, height, viewX, viewY, viewW, viewH);

            Style rootStyle = new Style();
            applyStyle(rootStyle, attrs);

            for (T3HtmlNode child : svg.getChildren()) {
                renderNode(child, r, rootStyle);
            }

            return new T3HtmlImage(width, height, r.pixels);

        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Error al decodificar SVG: " + ex.getMessage(), ex);
        }
    }

    private static T3HtmlNode findNode(T3HtmlNode node, String tag) {
        if (node == null) {
            return null;
        }

        if (tag.equalsIgnoreCase(node.getTagName())) {
            return node;
        }

        for (T3HtmlNode child : node.getChildren()) {
            T3HtmlNode found = findNode(child, tag);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private static String attr(Map<String, String> attrs, String name) {
        String value = attrs.get(name);
        if (value != null) {
            return value;
        }

        for (Map.Entry<String, String> e : attrs.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }

        return null;
    }

    private static int dimension(String value, float fallback) {
        if (value == null) {
            return Math.round(fallback);
        }

        Matcher m = Pattern.compile("^\\s*([-+]?[0-9]*\\.?[0-9]+)").matcher(value);

        if (!m.find()) {
            return Math.round(fallback);
        }

        try {
            float n = Float.parseFloat(m.group(1));
            if (!Float.isFinite(n) || n <= 0) {
                return Math.round(fallback);
            }
            return Math.round(n);
        } catch (NumberFormatException ex) {
            return Math.round(fallback);
        }
    }

    private static float number(Map<String, String> attrs, String key, float fallback) {
        String s = attr(attrs, key);
        if (s == null) {
            return fallback;
        }

        try {
            Matcher m = Pattern.compile("[-+]?(?:[0-9]*\\.)?[0-9]+").matcher(s.trim());

            if (m.find()) {
                return Float.parseFloat(m.group());
            }
        } catch (Exception ignored) {
        }

        return fallback;
    }

    private static float[] numbers(String s) {
        Matcher m = Pattern.compile("[-+]?(?:[0-9]*\\.)?[0-9]+(?:[eE][-+]?[0-9]+)?").matcher(s);

        List<Float> list = new ArrayList<>();

        while (m.find()) {
            try {
                list.add(Float.parseFloat(m.group()));
            } catch (NumberFormatException ignored) {
            }
        }

        float[] result = new float[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    private static final class Style {

        Color fill = Color.BLACK;
        Color stroke = null;
        float opacity = 1;
        float fillOpacity = 1;
        float strokeOpacity = 1;
        float strokeWidth = 1;
        boolean fillEnabled = true;
        boolean strokeEnabled = false;

        Style copy() {
            Style s = new Style();
            s.fill = fill;
            s.stroke = stroke;
            s.opacity = opacity;
            s.fillOpacity = fillOpacity;
            s.strokeOpacity = strokeOpacity;
            s.strokeWidth = strokeWidth;
            s.fillEnabled = fillEnabled;
            s.strokeEnabled = strokeEnabled;
            return s;
        }
    }

    private static void applyStyle(Style style, Map<String, String> attrs) {
        String fill = attr(attrs, "fill");
        if (fill != null) {
            if ("none".equalsIgnoreCase(fill.trim())) {
                style.fillEnabled = false;
            } else {
                Color c = parseColor(fill);
                if (c != null) {
                    style.fill = c;
                    style.fillEnabled = true;
                }
            }
        }

        String stroke = attr(attrs, "stroke");
        if (stroke != null) {
            if ("none".equalsIgnoreCase(stroke.trim())) {
                style.strokeEnabled = false;
            } else {
                Color c = parseColor(stroke);
                if (c != null) {
                    style.stroke = c;
                    style.strokeEnabled = true;
                }
            }
        }

        style.opacity = clamp(number(attrs, "opacity", style.opacity));
        style.fillOpacity = clamp(number(attrs, "fill-opacity", style.fillOpacity));
        style.strokeOpacity = clamp(number(attrs, "stroke-opacity", style.strokeOpacity));
        style.strokeWidth = Math.max(0, number(attrs, "stroke-width", style.strokeWidth));

        String inline = attr(attrs, "style");
        if (inline != null) {
            for (String declaration : inline.split(";")) {
                int colon = declaration.indexOf(':');
                if (colon < 0) {
                    continue;
                }

                String key = declaration.substring(0, colon).trim().toLowerCase(java.util.Locale.ROOT);
                String value = declaration.substring(colon + 1).trim();

                switch (key) {
                    case "fill":
                        if ("none".equalsIgnoreCase(value)) {
                            style.fillEnabled = false;
                        } else {
                            Color c = parseColor(value);
                            if (c != null) {
                                style.fill = c;
                                style.fillEnabled = true;
                            }
                        }
                        break;
                    case "stroke":
                        if ("none".equalsIgnoreCase(value)) {
                            style.strokeEnabled = false;
                        } else {
                            Color c = parseColor(value);
                            if (c != null) {
                                style.stroke = c;
                                style.strokeEnabled = true;
                            }
                        }
                        break;
                    case "stroke-width":
                        style.strokeWidth = Math.max(
                                0, parseFloat(value, style.strokeWidth));
                        break;
                    case "opacity":
                        style.opacity = clamp(
                                parseFloat(value, style.opacity));
                        break;
                    case "fill-opacity":
                        style.fillOpacity = clamp(
                                parseFloat(value, style.fillOpacity));
                        break;
                    case "stroke-opacity":
                        style.strokeOpacity = clamp(
                                parseFloat(value, style.strokeOpacity));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private static float parseFloat(String s, float fallback) {
        try {
            Matcher m = Pattern.compile("[-+]?(?:[0-9]*\\.)?[0-9]+").matcher(s);
            if (m.find()) {
                return Float.parseFloat(m.group());
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static float clamp(float n) {
        return Math.max(0, Math.min(1, n));
    }

    private static Color parseColor(String value) {
        if (value == null) {
            return null;
        }

        String s = value.trim();

        if ("transparent".equalsIgnoreCase(s)) {
            return new Color(0, 0, 0, 0);
        }

        try {
            return T3CSSUtils.parseColor(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void renderNode(T3HtmlNode node, Raster r, Style inherited) {
        if (node == null) {
            return;
        }

        String tag = node.getTagName();
        if (tag == null) {
            return;
        }

        tag = tag.toLowerCase(java.util.Locale.ROOT);

        // Elementos que no se dibujan directamente.
        if ("defs".equals(tag) || "metadata".equals(tag) || "title".equals(tag) || "desc".equals(tag) || "style".equals(tag) || "script".equals(tag)) {
            return;
        }

        Style style = inherited.copy();
        applyStyle(style, node.getAttributes());

        Map<String, String> a = node.getAttributes();

        switch (tag) {
            case "rect":
                drawRect(r, number(a, "x", 0), number(a, "y", 0), number(a, "width", 0), number(a, "height", 0), number(a, "rx", 0), number(a, "ry", 0), style);
                break;

            case "circle":
                drawEllipse(r, number(a, "cx", 0), number(a, "cy", 0), number(a, "r", 0), number(a, "r", 0), style);
                break;

            case "ellipse":
                drawEllipse(r, number(a, "cx", 0), number(a, "cy", 0), number(a, "rx", 0), number(a, "ry", 0), style);
                break;

            case "line":
                drawLine(r, number(a, "x1", 0), number(a, "y1", 0), number(a, "x2", 0), number(a, "y2", 0), style);
                break;

            case "polygon":
                drawPoints(r, attr(a, "points"), style, true);
                break;

            case "polyline":
                drawPoints(r, attr(a, "points"), style, false);
                break;

            case "path":
                drawPath(r, attr(a, "d"), style);
                break;

            default:
                break;
        }

        // Los grupos heredan estilo de sus padres.
        for (T3HtmlNode child : node.getChildren()) {
            renderNode(child, r, style);
        }
    }

    private static final class Raster {

        final int width;
        final int height;
        final int[] pixels;
        final float sx;
        final float sy;
        final float vx;
        final float vy;

        Raster(int width, int height, float vx, float vy, float vw, float vh) {
            this.width = width;
            this.height = height;
            this.pixels = new int[width * height];
            this.vx = vx;
            this.vy = vy;
            this.sx = width / vw;
            this.sy = height / vh;
        }

        int px(float x) {
            return Math.round((x - vx) * sx);
        }

        int py(float y) {
            return Math.round((y - vy) * sy);
        }

        void blend(int x, int y, Color color, float opacity) {
            if (x < 0 || y < 0 || x >= width || y >= height) {
                return;
            }

            float alpha = color.getAlpha() / 255f * clamp(opacity);
            if (alpha <= 0) {
                return;
            }

            int index = y * width + x;
            int dst = pixels[index];

            float da = ((dst >>> 24) & 255) / 255f;
            float outA = alpha + da * (1 - alpha);

            int sr = color.getRed();
            int sg = color.getGreen();
            int sb = color.getBlue();

            int dr = (dst >>> 16) & 255;
            int dg = (dst >>> 8) & 255;
            int db = dst & 255;

            int rr = outA == 0 ? 0 : Math.round((sr * alpha + dr * da * (1 - alpha)) / outA);
            int rg = outA == 0 ? 0 : Math.round((sg * alpha + dg * da * (1 - alpha)) / outA);
            int rb = outA == 0 ? 0 : Math.round((sb * alpha + db * da * (1 - alpha)) / outA);

            pixels[index] = (Math.round(outA * 255) << 24) | (rr << 16) | (rg << 8) | rb;
        }

        void fillPolygon(List<float[]> points, Color color, float opacity) {
            if (points.size() < 3) {
                return;
            }

            float minY = Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;

            for (float[] p : points) {
                minY = Math.min(minY, p[1]);
                maxY = Math.max(maxY, p[1]);
            }

            int y0 = Math.max(0, py(minY));
            int y1 = Math.min(height - 1, py(maxY));

            for (int y = y0; y <= y1; y++) {
                float userY = y / sy + vy;
                List<Float> intersections = new ArrayList<>();

                for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {

                    float[] p1 = points.get(j);
                    float[] p2 = points.get(i);

                    if ((p1[1] <= userY && p2[1] > userY) || (p2[1] <= userY && p1[1] > userY)) {
                        float x = p1[0] + (userY - p1[1]) * (p2[0] - p1[0]) / (p2[1] - p1[1]);
                        intersections.add(x);
                    }
                }

                intersections.sort(Float::compare);

                for (int i = 0; i + 1 < intersections.size(); i += 2) {
                    int x0 = Math.max(0, px(intersections.get(i)));
                    int x1 = Math.min(width - 1, px(intersections.get(i + 1)));

                    for (int x = x0; x <= x1; x++) {
                        blend(x, y, color, opacity);
                    }
                }
            }
        }

        void line(float x1, float y1, float x2, float y2, Color color, float opacity, float strokeWidth) {
            int ax = px(x1), ay = py(y1);
            int bx = px(x2), by = py(y2);

            int dx = Math.abs(bx - ax);
            int dy = Math.abs(by - ay);
            int steps = Math.max(dx, dy);

            int radius = Math.max(0, Math.round(strokeWidth * (sx + sy) / 4f));

            if (steps == 0) {
                blendDisk(ax, ay, radius, color, opacity);
                return;
            }

            for (int i = 0; i <= steps; i++) {
                int x = ax + (bx - ax) * i / steps;
                int y = ay + (by - ay) * i / steps;
                blendDisk(x, y, radius, color, opacity);
            }
        }

        void blendDisk(int cx, int cy, int radius, Color color, float opacity) {
            if (radius <= 0) {
                blend(cx, cy, color, opacity);
                return;
            }

            int rr = radius * radius;
            for (int y = cy - radius; y <= cy + radius; y++) {
                for (int x = cx - radius; x <= cx + radius; x++) {
                    int dx = x - cx, dy = y - cy;
                    if (dx * dx + dy * dy <= rr) {
                        blend(x, y, color, opacity);
                    }
                }
            }
        }
    }

    private static void paintShape(Raster r, List<float[]> points, Style s, boolean closed) {
        if (closed && s.fillEnabled) {
            r.fillPolygon(points, s.fill, s.opacity * s.fillOpacity);
        }

        if (s.strokeEnabled && s.stroke != null) {
            for (int i = 1; i < points.size(); i++) {
                float[] p = points.get(i - 1);
                float[] q = points.get(i);
                r.line(p[0], p[1], q[0], q[1], s.stroke, s.opacity * s.strokeOpacity, s.strokeWidth);
            }

            if (closed && points.size() > 2) {
                float[] p = points.get(points.size() - 1);
                float[] q = points.get(0);
                r.line(p[0], p[1], q[0], q[1], s.stroke, s.opacity * s.strokeOpacity, s.strokeWidth);
            }
        }
    }

    private static void drawRect(Raster r, float x, float y, float w, float h, float rx, float ry, Style s) {
        if (w <= 0 || h <= 0) {
            return;
        }

        // Los rectángulos redondeados se aproximan mediante una elipse
        // en las esquinas en versiones posteriores.
        List<float[]> p = new ArrayList<>();
        p.add(new float[]{x, y});
        p.add(new float[]{x + w, y});
        p.add(new float[]{x + w, y + h});
        p.add(new float[]{x, y + h});

        paintShape(r, p, s, true);
    }

    private static void drawEllipse(Raster r, float cx, float cy, float rx, float ry, Style s) {
        if (rx <= 0 || ry <= 0) {
            return;
        }

        List<float[]> points = new ArrayList<>();
        int segments = 96;

        for (int i = 0; i < segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            points.add(new float[]{cx + (float) Math.cos(angle) * rx, cy + (float) Math.sin(angle) * ry});
        }

        paintShape(r, points, s, true);
    }

    private static void drawLine(Raster r, float x1, float y1, float x2, float y2, Style s) {
        if (s.strokeEnabled && s.stroke != null) {
            r.line(x1, y1, x2, y2, s.stroke, s.opacity * s.strokeOpacity, s.strokeWidth);
        }
    }

    private static void drawPoints(Raster r, String text, Style s, boolean closed) {
        if (text == null) {
            return;
        }

        float[] nums = numbers(text);
        List<float[]> points = new ArrayList<>();

        for (int i = 0; i + 1 < nums.length; i += 2) {
            points.add(new float[]{nums[i], nums[i + 1]});
        }

        paintShape(r, points, s, closed);
    }

    private static void drawPath(Raster r, String d, Style s) {
        if (d == null || d.isBlank()) {
            return;
        }

        Matcher matcher = Pattern.compile("[AaCcHhLlMmQqSsTtVvZz]|"
                + "[-+]?(?:[0-9]*\\.)?[0-9]+(?:[eE][-+]?[0-9]+)?").matcher(d);

        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        List<float[]> path = new ArrayList<>();
        float x = 0, y = 0, startX = 0, startY = 0;
        char command = ' ';
        int i = 0;

        while (i < tokens.size()) {
            String token = tokens.get(i);

            if (token.length() == 1 && Character.isLetter(token.charAt(0))) {
                command = token.charAt(0);
                i++;
                if (command == 'Z' || command == 'z') {
                    paintShape(r, path, s, true);
                    path = new ArrayList<>();
                    x = startX;
                    y = startY;
                }
                continue;
            }

            boolean relative = Character.isLowerCase(command);
            char c = Character.toUpperCase(command);

            try {
                switch (c) {
                    case 'M':
                    case 'L': {
                        if (i + 1 >= tokens.size()) {
                            return;
                        }

                        float nx = Float.parseFloat(tokens.get(i++));
                        float ny = Float.parseFloat(tokens.get(i++));

                        if (relative) {
                            nx += x;
                            ny += y;
                        }

                        if (c == 'M') {
                            if (!path.isEmpty()) {
                                paintShape(r, path, s, false);
                                path = new ArrayList<>();
                            }
                            startX = nx;
                            startY = ny;
                            command = relative ? 'l' : 'L';
                        }

                        x = nx;
                        y = ny;
                        path.add(new float[]{x, y});
                        break;
                    }

                    case 'H': {
                        float nx = Float.parseFloat(tokens.get(i++));
                        if (relative) {
                            nx += x;
                        }
                        x = nx;
                        path.add(new float[]{x, y});
                        break;
                    }

                    case 'V': {
                        float ny = Float.parseFloat(tokens.get(i++));
                        if (relative) {
                            ny += y;
                        }
                        y = ny;
                        path.add(new float[]{x, y});
                        break;
                    }

                    case 'C': {
                        if (i + 5 >= tokens.size()) {
                            return;
                        }

                        float x1 = Float.parseFloat(tokens.get(i++));
                        float y1 = Float.parseFloat(tokens.get(i++));
                        float x2 = Float.parseFloat(tokens.get(i++));
                        float y2 = Float.parseFloat(tokens.get(i++));
                        float ex = Float.parseFloat(tokens.get(i++));
                        float ey = Float.parseFloat(tokens.get(i++));

                        if (relative) {
                            x1 += x;
                            x2 += x;
                            ex += x;
                            y1 += y;
                            y2 += y;
                            ey += y;
                        }

                        float ox = x, oy = y;
                        for (int k = 1; k <= 24; k++) {
                            float t = k / 24f, u = 1 - t;
                            float px = u * u * u * ox + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * ex;
                            float py = u * u * u * oy + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * ey;
                            path.add(new float[]{px, py});
                        }

                        x = ex;
                        y = ey;
                        break;
                    }

                    case 'Q': {
                        if (i + 3 >= tokens.size()) {
                            return;
                        }

                        float x1 = Float.parseFloat(tokens.get(i++));
                        float y1 = Float.parseFloat(tokens.get(i++));
                        float ex = Float.parseFloat(tokens.get(i++));
                        float ey = Float.parseFloat(tokens.get(i++));

                        if (relative) {
                            x1 += x;
                            ex += x;
                            y1 += y;
                            ey += y;
                        }

                        float ox = x, oy = y;
                        for (int k = 1; k <= 24; k++) {
                            float t = k / 24f, u = 1 - t;
                            float px = u * u * ox + 2 * u * t * x1 + t * t * ex;
                            float py = u * u * oy + 2 * u * t * y1 + t * t * ey;
                            path.add(new float[]{px, py});
                        }

                        x = ex;
                        y = ey;
                        break;
                    }

                    default:
                        // A, S y T aún no están implementados.
                        // No interpretar sus parámetros como coordenadas
                        // de otro comando.
                        return;
                }
            } catch (RuntimeException ex) {
                return;
            }
        }

        if (!path.isEmpty()) {
            paintShape(r, path, s, false);
        }
    }
}

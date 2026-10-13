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
import java.util.Locale;

public final class T3HtmlImageDecoder {

    private T3HtmlImageDecoder() {
    }

    public static T3HtmlImage decode(byte[] data, String source)
            throws IOException {

        if (data == null || data.length < 2) {
            throw new IOException("Archivo de imagen vacío o inválido");
        }

        String extension = getExtension(source);

        // Primero se intenta identificar el formato por su firma.
        if (isPNG(data)) {
            return T3HtmlImagePNG.decode(data);
        }

        if (isJPEG(data)) {
            return T3HtmlImageJPEG.decode(data);
        }

        if (isGIF(data)) {
            return T3HtmlImageGIF.decode(data);
        }

        if (isBMP(data)) {
            return T3HtmlImageBMP.decode(data);
        }

        // Como alternativa, se utiliza la extensión de la URL.
        switch (extension) {
            case "jpg":
            case "jpeg":
                return T3HtmlImageJPEG.decode(data);

            case "png":
                return T3HtmlImagePNG.decode(data);

            case "gif":
                return T3HtmlImageGIF.decode(data);

            case "bmp":
                return T3HtmlImageBMP.decode(data);
            default:
                throw new IOException("Formato de imagen no reconocido: " + source);
        }
    }

    private static String getExtension(String source) {
        if (source == null || source.isEmpty()) {
            return "";
        }

        int end = source.length();
        int query = source.indexOf('?');
        int fragment = source.indexOf('#');

        if (query >= 0 && query < end) {
            end = query;
        }

        if (fragment >= 0 && fragment < end) {
            end = fragment;
        }

        String path = source.substring(0, end);
        int slash = Math.max(
                path.lastIndexOf('/'),
                path.lastIndexOf('\\')
        );
        int dot = path.lastIndexOf('.');

        if (dot <= slash || dot == path.length() - 1) {
            return "";
        }

        return path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isPNG(byte[] d) {
        return d.length >= 8
                && (d[0] & 0xFF) == 0x89
                && d[1] == 0x50
                && d[2] == 0x4E
                && d[3] == 0x47
                && d[4] == 0x0D
                && d[5] == 0x0A
                && d[6] == 0x1A
                && d[7] == 0x0A;
    }

    private static boolean isJPEG(byte[] d) {
        return d.length >= 3
                && (d[0] & 0xFF) == 0xFF
                && (d[1] & 0xFF) == 0xD8
                && (d[2] & 0xFF) == 0xFF;
    }

    private static boolean isGIF(byte[] d) {
        return d.length >= 6
                && d[0] == 'G'
                && d[1] == 'I'
                && d[2] == 'F'
                && d[3] == '8'
                && (d[4] == '7' || d[4] == '9')
                && d[5] == 'a';
    }

    private static boolean isBMP(byte[] d) {
        return d.length >= 2
                && d[0] == 'B'
                && d[1] == 'M';
    }
}

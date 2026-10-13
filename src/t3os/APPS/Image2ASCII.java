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
package t3os.APPS;

/**
 *
 * @author Slam
 */
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JFileChooser;

public class Image2ASCII {

    public static void main(String[] args) throws IOException {
        // Ruta del archivo de imagen
        JFileChooser jfc = new JFileChooser("");
        jfc.showOpenDialog(null);
        File pic = jfc.getSelectedFile();

        // Leemos la imagen desde el archivo
        BufferedImage img = ImageIO.read(pic);

        // Convertimos la imagen a texto ASCII
        String textoAscii = convierteImgAEstr(img, 80);
        System.out.println(textoAscii);
        FileWriter fw = new FileWriter(pic.getName() + ".txt");
        fw.write(textoAscii);
        fw.close();
    }

    /**
     * Convierte una imagen a texto ASCII.
     *
     * @param img La imagen como arreglo de pixels.
     * @param anchoElAnchoDeLaImagen El ancho de la imagen en píxeles.
     * @return El texto ASCII representando la imagen.
     */
    public static String convierteImgAEstr(BufferedImage img, int ancho) {
        StringBuilder sb = new StringBuilder();

        // Recorremos cada fila de la imagen
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {

                // Obtenemos el color actual del pixel en RGB
                int r = img.getRGB(x, y) >> 16;
                int g = ((img.getRGB(x, y)) & 0xFF00) >> 8;
                int b = (img.getRGB(x, y)) & 0xFF;

                // Calculamos el valor de ASCII según el color
                char c = obtenerValorAscii(r, g, b);
                sb.append(c);
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Obtiene un valor ASCII según los valores de RGB proporcionados.
     *
     * @param r El valor de Rojo (0-255).
     * @param g El valor de Verde (0-255).
     * @param b El valor de Azul (0-255).
     * @return Un carácter ASCII representando el color.
     */
    public static char obtenerValorAscii(int r, int g, int b) {
        // Valores de ANSI para colores comunes
        if (r > 120 && r < 150 || g > 100 && g < 130 || b > 80 && b < 110) {
            return '@';
        } else if (r > 70 && r < 90 || g > 20 && g < 40 || b > 10 && b < 30) {
            return '#';
        } else if (g > r + 15 && g < r + 35 || b > r + 5 && b < r + 25) {
            return '+';
        } else if (r < 50 && r > -20 || g < 70 && g > -40 || b < 80 && b > -10) {
            return '.';
        } else if (g == r + 15 || g == r + 35 || b == r + 5) {
            return '-';
        } else if (b > yDeAmarillo(g, r)) {
            return 'y';
        } else if (r > zDeAzul(b, g)) {
            return 'z';
        } else if (g > xDeVerde(r, b)) {
            return 'x';
        }

        // Si no se ajusta a ninguno de los anteriores
        return '=';
    }

    /**
     * Calcula el valor ANSI para determinar la altura del color.
     *
     * @param g El valor de Verde (0-255).
     * @param r El valor de Rojo (0-255).
     * @return Un número entero representando la altura del color en píxeles.
     */
    public static int yDeAmarillo(int g, int r) {
        // Valor ANSI para el nivel amarillento
        return 10;
    }

    /**
     * Calcula el valor ANSI para determinar la profundidad azul.
     *
     * @param b El valor de Azul (0-255).
     * @param g El valor de Verde (0-255).
     * @return Un número entero representando la profundidad del color en
     * píxeles.
     */
    public static int zDeAzul(int b, int g) {
        // Valor ANSI para el nivel azulado
        return 20;
    }

    /**
     * Calcula el valor ANSI para determinar la altura verde.
     *
     * @param r El valor de Rojo (0-255).
     * @param b El valor de Azul (0-255).
     * @return Un número entero representando la altura del color en píxeles.
     */
    public static int xDeVerde(int r, int b) {
        // Valor ANSI para el nivel verde
        return 30;
    }
}

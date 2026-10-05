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
package test;

/**
 *
 * @author Slam
 */
import java.util.HashMap;
import java.util.Map;
import t3isa.DEVICE.TGraphicsDevice;

// Clase de prueba para render en TGraphics
public class TRenderizadorTexto {

    // Diccionario con las matrices 5x5 de cada letra
    private static final Map<Character, int[][]> ALFABETO = new HashMap<>();

    static {
        ALFABETO.put('A', new int[][]{{0, 0, 1, 0, 0}, {0, 1, 0, 1, 0}, {1, 1, 1, 1, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}});
        ALFABETO.put('B', new int[][]{{1, 1, 1, 0, 0}, {1, 0, 0, 1, 0}, {1, 1, 1, 0, 0}, {1, 0, 0, 1, 0}, {1, 1, 1, 0, 0}});
        ALFABETO.put('C', new int[][]{{0, 1, 1, 1, 0}, {1, 0, 0, 0, 0}, {1, 0, 0, 0, 0}, {1, 0, 0, 0, 0}, {0, 1, 1, 1, 0}});
        ALFABETO.put('D', new int[][]{{1, 1, 1, 0, 0}, {1, 0, 0, 1, 0}, {1, 0, 0, 1, 0}, {1, 0, 0, 1, 0}, {1, 1, 1, 0, 0}});
        ALFABETO.put('E', new int[][]{{1, 1, 1, 1, 1}, {1, 0, 0, 0, 0}, {1, 1, 1, 1, 0}, {1, 0, 0, 0, 0}, {1, 1, 1, 1, 1}});
        ALFABETO.put('F', new int[][]{{1, 1, 1, 1, 1}, {1, 0, 0, 0, 0}, {1, 1, 1, 1, 0}, {1, 0, 0, 0, 0}, {1, 0, 0, 0, 0}});
        ALFABETO.put('G', new int[][]{{0, 1, 1, 1, 0}, {1, 0, 0, 0, 0}, {1, 0, 1, 1, 0}, {1, 0, 0, 1, 0}, {0, 1, 1, 1, 0}});
        ALFABETO.put('H', new int[][]{{1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 1, 1, 1, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}});
        ALFABETO.put('I', new int[][]{{0, 1, 1, 1, 0}, {0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}, {0, 1, 1, 1, 0}});
        ALFABETO.put('J', new int[][]{{0, 0, 0, 1, 1}, {0, 0, 0, 0, 1}, {0, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {0, 1, 1, 1, 0}});
        ALFABETO.put('K', new int[][]{{1, 0, 0, 1, 0}, {1, 0, 1, 0, 0}, {1, 1, 0, 0, 0}, {1, 0, 1, 0, 0}, {1, 0, 0, 1, 0}});
        ALFABETO.put('L', new int[][]{{1, 0, 0, 0, 0}, {1, 0, 0, 0, 0}, {1, 0, 0, 0, 0}, {1, 0, 0, 0, 0}, {1, 1, 1, 1, 1}});
        ALFABETO.put('M', new int[][]{{1, 0, 0, 0, 1}, {1, 1, 0, 1, 1}, {1, 0, 1, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}});
        ALFABETO.put('N', new int[][]{{1, 0, 0, 0, 1}, {1, 1, 0, 0, 1}, {1, 0, 1, 0, 1}, {1, 0, 0, 1, 1}, {1, 0, 0, 0, 1}});
        ALFABETO.put('O', new int[][]{{0, 1, 1, 1, 0}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {0, 1, 1, 1, 0}});
        ALFABETO.put('P', new int[][]{{1, 1, 1, 0, 0}, {1, 0, 0, 1, 0}, {1, 1, 1, 0, 0}, {1, 0, 0, 0, 0}, {1, 0, 0, 0, 0}});
        ALFABETO.put('Q', new int[][]{{0, 1, 1, 1, 0}, {1, 0, 0, 0, 1}, {1, 0, 1, 0, 1}, {1, 0, 0, 1, 0}, {0, 1, 1, 1, 1}});
        ALFABETO.put('R', new int[][]{{1, 1, 1, 0, 0}, {1, 0, 0, 1, 0}, {1, 1, 1, 0, 0}, {1, 0, 1, 0, 0}, {1, 0, 0, 1, 0}});
        ALFABETO.put('S', new int[][]{{0, 1, 1, 1, 0}, {1, 0, 0, 0, 0}, {0, 1, 1, 1, 0}, {0, 0, 0, 0, 1}, {0, 1, 1, 1, 0}});
        ALFABETO.put('T', new int[][]{{1, 1, 1, 1, 1}, {0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}});
        ALFABETO.put('U', new int[][]{{1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {0, 1, 1, 1, 0}});
        ALFABETO.put('V', new int[][]{{1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {0, 1, 0, 1, 0}, {0, 0, 1, 0, 0}});
        ALFABETO.put('W', new int[][]{{1, 0, 0, 0, 1}, {1, 0, 0, 0, 1}, {1, 0, 1, 0, 1}, {1, 1, 0, 1, 1}, {1, 0, 0, 0, 1}});
        ALFABETO.put('X', new int[][]{{1, 0, 0, 0, 1}, {0, 1, 0, 1, 0}, {0, 0, 1, 0, 0}, {0, 1, 0, 1, 0}, {1, 0, 0, 0, 1}});
        ALFABETO.put('Y', new int[][]{{1, 0, 0, 0, 1}, {0, 1, 0, 1, 0}, {0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}});
        ALFABETO.put('Z', new int[][]{{1, 1, 1, 1, 1}, {0, 0, 0, 1, 0}, {0, 0, 1, 0, 0}, {0, 1, 0, 0, 0}, {1, 1, 1, 1, 1}});

        // === NÚMEROS 0-9 ===
        ALFABETO.put('0', new int[][]{{0, 1, 1, 1, 0}, {1, 0, 0, 1, 1}, {1, 0, 1, 0, 1}, {1, 1, 0, 0, 1}, {0, 1, 1, 1, 0}});
        ALFABETO.put('1', new int[][]{{0, 0, 1, 0, 0}, {0, 1, 1, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}, {0, 1, 1, 1, 0}});
        ALFABETO.put('2', new int[][]{{0, 1, 1, 1, 0}, {1, 0, 0, 0, 1}, {0, 0, 1, 1, 0}, {0, 1, 0, 0, 0}, {1, 1, 1, 1, 1}});
        ALFABETO.put('3', new int[][]{{0, 1, 1, 1, 0}, {0, 0, 0, 0, 1}, {0, 0, 1, 1, 0}, {0, 0, 0, 0, 1}, {0, 1, 1, 1, 0}});
        ALFABETO.put('4', new int[][]{{1, 0, 0, 1, 0}, {1, 0, 0, 1, 0}, {1, 1, 1, 1, 1}, {0, 0, 0, 1, 0}, {0, 0, 0, 1, 0}});
        ALFABETO.put('5', new int[][]{{1, 1, 1, 1, 1}, {1, 0, 0, 0, 0}, {1, 1, 1, 1, 0}, {0, 0, 0, 0, 1}, {1, 1, 1, 1, 0}});
        ALFABETO.put('6', new int[][]{{0, 1, 1, 1, 0}, {1, 0, 0, 0, 0}, {1, 1, 1, 1, 0}, {1, 0, 0, 0, 1}, {0, 1, 1, 1, 0}});
        ALFABETO.put('7', new int[][]{{1, 1, 1, 1, 1}, {0, 0, 0, 0, 1}, {0, 0, 0, 1, 0}, {0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}});
        ALFABETO.put('8', new int[][]{{0, 1, 1, 1, 0}, {1, 0, 0, 0, 1}, {0, 1, 1, 1, 0}, {1, 0, 0, 0, 1}, {0, 1, 1, 1, 0}});
        ALFABETO.put('9', new int[][]{{0, 1, 1, 1, 0}, {1, 0, 0, 0, 1}, {0, 1, 1, 1, 1}, {0, 0, 0, 0, 1}, {0, 1, 1, 1, 0}});

        // === SÍMBOLOS ESPECIALES Y PUNTUACIÓN ===
        ALFABETO.put('.', new int[][]{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 1, 0, 0}});
        ALFABETO.put(',', new int[][]{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 1, 0, 0}, {0, 1, 0, 0, 0}});
        ALFABETO.put(':', new int[][]{{0, 0, 0, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 0, 0, 0}});
        ALFABETO.put(';', new int[][]{{0, 0, 0, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 1, 0, 0}, {0, 1, 0, 0, 0}});
        ALFABETO.put('!', new int[][]{{0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 1, 0, 0}});
        ALFABETO.put('?', new int[][]{{0, 1, 1, 1, 0}, {0, 0, 0, 0, 1}, {0, 0, 1, 1, 0}, {0, 0, 0, 0, 0}, {0, 0, 1, 0, 0}});
        ALFABETO.put('+', new int[][]{{0, 0, 0, 0, 0}, {0, 0, 1, 0, 0}, {0, 1, 1, 1, 0}, {0, 0, 1, 0, 0}, {0, 0, 0, 0, 0}});
        ALFABETO.put('-', new int[][]{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 1, 1, 1, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}});
        ALFABETO.put('*', new int[][]{{0, 0, 0, 0, 0}, {1, 0, 1, 0, 1}, {0, 0, 1, 0, 0}, {1, 0, 1, 0, 1}, {0, 0, 0, 0, 0}});
        ALFABETO.put('/', new int[][]{{0, 0, 0, 0, 1}, {0, 0, 0, 1, 0}, {0, 0, 1, 0, 0}, {0, 1, 0, 0, 0}, {1, 0, 0, 0, 0}});
        ALFABETO.put('=', new int[][]{{0, 0, 0, 0, 0}, {0, 1, 1, 1, 0}, {0, 0, 0, 0, 0}, {0, 1, 1, 1, 0}, {0, 0, 0, 0, 0}});
        ALFABETO.put('_', new int[][]{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {1, 1, 1, 1, 1}});
        ALFABETO.put('(', new int[][]{{0, 0, 0, 1, 0}, {0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 0, 1, 0}});
        ALFABETO.put(')', new int[][]{{0, 1, 0, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}, {0, 1, 0, 0, 0}});
        ALFABETO.put('[', new int[][]{{0, 1, 1, 1, 0}, {0, 1, 0, 0, 0}, {0, 1, 0, 0, 0}, {0, 1, 0, 0, 0}, {0, 1, 1, 1, 0}});
        ALFABETO.put(']', new int[][]{{0, 1, 1, 1, 0}, {0, 0, 0, 1, 0}, {0, 0, 0, 1, 0}, {0, 0, 0, 1, 0}, {0, 1, 1, 1, 0}});
        ALFABETO.put('<', new int[][]{{0, 0, 0, 1, 0}, {0, 0, 1, 0, 0}, {0, 1, 0, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 0, 1, 0}});
        ALFABETO.put('>', new int[][]{{0, 1, 0, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 0, 1, 0}, {0, 0, 1, 0, 0}, {0, 1, 0, 0, 0}});
        ALFABETO.put('"', new int[][]{{0, 1, 0, 1, 0}, {0, 1, 0, 1, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}});
        ALFABETO.put('\'', new int[][]{{0, 0, 1, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}});
        ALFABETO.put('#', new int[][]{{0, 1, 0, 1, 0}, {1, 1, 1, 1, 1}, {0, 1, 0, 1, 0}, {1, 1, 1, 1, 1}, {0, 1, 0, 1, 0}});
        ALFABETO.put('$', new int[][]{{0, 0, 1, 0, 0}, {0, 1, 1, 1, 1}, {0, 1, 0, 0, 0}, {1, 1, 1, 1, 0}, {0, 0, 1, 0, 0}});
        ALFABETO.put('%', new int[][]{{1, 1, 0, 0, 1}, {1, 1, 0, 1, 0}, {0, 0, 1, 0, 0}, {0, 1, 0, 1, 1}, {1, 0, 0, 1, 1}});
        ALFABETO.put('&', new int[][]{{0, 1, 1, 0, 0}, {0, 1, 1, 0, 0}, {0, 1, 0, 1, 0}, {1, 0, 0, 1, 0}, {0, 1, 1, 0, 1}});
        ALFABETO.put('@', new int[][]{{0, 1, 1, 1, 0}, {1, 0, 1, 0, 1}, {1, 0, 1, 1, 1}, {1, 0, 0, 0, 0}, {0, 1, 1, 1, 0}});
        ALFABETO.put('Ñ', new int[][]{{0, 1, 1, 1, 0}, {1, 0, 0, 0, 1}, {1, 1, 0, 0, 1}, {1, 0, 1, 0, 1}, {1, 0, 0, 0, 1}});
    }

    /**
     * Dibuja un texto completo píxel a píxel.
     *
     * @param graphicsDevice
     * @param texto Cadena a renderizar.
     * @param startX Coordenada X inicial.
     * @param startY Coordenada Y inicial.
     * @param scale Tamaño en píxeles de cada bloque (ej. 1 = 1px, 2 = 2x2px por
     * punto).
     * @param colorFg Color ARGB/RGB del píxel encendido (ej. 0x00FF0000 para
     * rojo).
     * @param colorBg Color ARGB/RGB de fondo. Pasa -1 si deseas fondo
     * transparente.
     */
    public void renderizarTexto(TGraphicsDevice graphicsDevice, String texto, int startX, int startY, int scale, int colorFg, int colorBg) {
        texto = texto.toUpperCase();
        int curX = startX;
        int curY = startY;

        // Altura de una letra (5) más espacio interlineal (2 píxeles) por la escala
        int altoLinea = (5 + 2) * scale;

        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);

            // --- MANEJO DE CARACTERES DE CONTROL Y ESPACIOS ---
            // Salto de línea (\n)
            if (c == '\n') {
                curY += altoLinea;
                curX = startX; // Regresa al margen izquierdo inicial
                continue;
            }

            // Retorno de carro (\r)
            if (c == '\r') {
                curX = startX;
                continue;
            }

            // Tabulación (\t)
            if (c == '\t') {
                curX += (5 + 1) * scale * 4; // Avanza el equivalente a 4 caracteres
                continue;
            }

            // Espacio en blanco
            if (c == ' ') {
                curX += 3 * scale; // Espacio entre palabras
                continue;
            }

            // --- RENDERIZADO DE MATRIZ DE CARACTER ---
            int[][] matriz = ALFABETO.get(c);
            if (matriz == null) {
                continue; // Ignora caracteres no soportados
            }

            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 5; k++) {
                    int color = (matriz[j][k] == 1) ? colorFg : colorBg;

                    // Si el fondo es transparente (-1), omite dibujar
                    if (color == -1) {
                        continue;
                    }

                    // Dibujar bloque escalado
                    for (int dy = 0; dy < scale; dy++) {
                        for (int dx = 0; dx < scale; dx++) {
                            graphicsDevice.setPixel(curX + (k * scale) + dx, curY + (j * scale) + dy, color);
                        }
                    }
                }
            }

            // Avanza la posición X para la siguiente letra (5 de ancho + 1 de separación)
            curX += (5 + 1) * scale;
        }
    }
}

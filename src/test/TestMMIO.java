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
public class TestMMIO {

    public static final String test = """

; ============================================================
; T3ISA MMIO GRAPHICS TEST
; ============================================================
; ============================================================
; T3OS - Prueba de todas las syscalls gráficas
;
; 20 GRAPHICS_PIXEL
; 21 GRAPHICS_CLEAR
; 22 GRAPHICS_LINE
; 23 GRAPHICS_RECT
; 24 GRAPHICS_FILL_RECT
; 25 GRAPHICS_CIRCLE
; 26 GRAPHICS_FILL_CIRCLE
; 27 GRAPHICS_GET_PIXEL
; 28 GRAPHICS_WIDTH
; 29 GRAPHICS_HEIGHT
; 30 GRAPHICS_COLOR
; 31 GRAPHICS_PRESENT
; ============================================================

; ------------------------------------------------------------
; 21 - GRAPHICS_CLEAR
; Limpia pantalla a color 0
; ------------------------------------------------------------

MOVI R1, 21
MOVI R2, 0
SYS


; ------------------------------------------------------------
; 20 - GRAPHICS_PIXEL
; Pixel azul en 100,100
; ------------------------------------------------------------

MOVI R1, 20
MOVI R2, 100
MOVI R3, 100
MOVI R4, 255
SYS


; ------------------------------------------------------------
; 22 - GRAPHICS_LINE
; Línea azul
; (150,100) -> (400,200)
; ------------------------------------------------------------

MOVI R1, 22
MOVI R2, 150
MOVI R3, 100
MOVI R4, 400
MOVI R5, 200
MOVI R6, 255
SYS
                                      

MOVI R1, 32
MOVI R2, 1000
SYS
; ------------------------------------------------------------
; 23 - GRAPHICS_RECT
; Rectángulo sin relleno
; (100,250) 300x150
; ------------------------------------------------------------

MOVI R1, 23
MOVI R2, 100
MOVI R3, 250
MOVI R4, 300
MOVI R5, 150
MOVI R6, 255
SYS

; ------------------------------------------------------------
; 24 - GRAPHICS_FILL_RECT
; Rectángulo relleno
; (500,100) 250x150
; ------------------------------------------------------------

MOVI R1, 24
MOVI R2, 500
MOVI R3, 100
MOVI R4, 250
MOVI R5, 150
MOVI R6, 255
SYS
                                     
MOVI R1, 32
MOVI R2, 1000
SYS
; ------------------------------------------------------------
; 25 - GRAPHICS_CIRCLE
; Círculo sin relleno
; centro 300,450 radio 100
; ------------------------------------------------------------

MOVI R1, 25
MOVI R2, 300
MOVI R3, 450
MOVI R4, 100
CONST R5, 0xFF0000
                                     
SYS

                                      
; ------------------------------------------------------------
; 26 - GRAPHICS_FILL_CIRCLE
; Círculo relleno
; centro 650,450 radio 100
; ------------------------------------------------------------

MOVI R1, 26
MOVI R2, 650
MOVI R3, 450
MOVI R4, 100
CONST R5, 0x00FF00
SYS

MOVI R1, 32
MOVI R2, 1000
SYS                                      
; ------------------------------------------------------------
; 27 - GRAPHICS_GET_PIXEL
;
; Lee el pixel que escribimos inicialmente.
;
; R7 debe recibir 255.
; ------------------------------------------------------------

MOVI R1, 27
MOVI R2, 100
MOVI R3, 100
SYS

                                      
; ------------------------------------------------------------
; 28 - GRAPHICS_WIDTH
;
; R7 debe recibir 1024.
; ------------------------------------------------------------

MOVI R1, 28
SYS

                                      
; ------------------------------------------------------------
; 29 - GRAPHICS_HEIGHT
;
; R7 debe recibir 768.
; ------------------------------------------------------------

MOVI R1, 29
SYS

; ------------------------------------------------------------
; 30 - GRAPHICS_COLOR
;
; IMPLEMENTACIÓN ACTUAL:
; limpia la pantalla con el color indicado.
;
; Usamos 8192.
; ------------------------------------------------------------

MOVI R1, 30
MOVI R2, 8192
SYS

; ------------------------------------------------------------
; 31 - GRAPHICS_PRESENT
;
; Actualmente no requiere operación adicional.
; ------------------------------------------------------------

MOVI R1, 31
SYS


; ------------------------------------------------------------
; EXIT
; ------------------------------------------------------------

MOVI R1, 12
SYS
""";
}

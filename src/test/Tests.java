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
public class Tests {

    public static final String test = """
              
; ============================================================
; SETUN-OS / T3ISA CPU STRESS TEST
; ============================================================

; ------------------------------------------------------------
; REGISTROS
;
; R1  = contador
; R2  = límite
; R3  = acumulador
; R4  = constante 1
; R5  = temporal
; R6  = dirección de memoria
; R7  = resultado factorial
; R8  = resultado multiplicación
; R9  = resultado TAND
; R10 = resultado TOR
; R11 = resultado TXOR
; R12 = resultado TNOT
; ------------------------------------------------------------


; ============================================================
; INICIALIZACIÓN
; ============================================================

MOVI R1, 1
MOVI R2, 10
MOVI R3, 0
MOVI R4, 1

; ============================================================
; SUMA 1..10
;
; R3 = 1 + 2 + ... + 10 = 55
; ============================================================

sum_loop:

ADD R3, R3, R1
ADD R1, R1, R4

CMP R1, R2
JPOS sum_done

JMP sum_loop


sum_done:

; ============================================================
; GUARDAR RESULTADO EN MEMORIA
;
; MEM[1000] = 55
; ============================================================

MOVI R6, 1000

STORE R3, R6, 0

; Limpiamos R5 para comprobar LOAD.

MOVI R5, 0

LOAD R5, R6, 0


; ============================================================
; COMPROBAR QUE LOAD DEVOLVIÓ 55
; ============================================================

CMP R5, R3
JZERO memory_ok

HALT


memory_ok:


; ============================================================
; PROBAR PUSH / POP
; ============================================================

MOVI R5, 123
MOVI R6, 456

PUSH R5
PUSH R6

POP R10
POP R11

; Debe quedar:
;
; R10 = 456
; R11 = 123


CMP R10, R6
JZERO stack_first_ok

HALT


stack_first_ok:

CMP R11, R5
JZERO stack_ok

HALT


stack_ok:


; ============================================================
; PROBAR CALL / RET
;
; La subrutina multiplica:
;
; R1 * R2 -> R8
;
; 7 * 6 = 42
; ============================================================

MOVI R1, 7
MOVI R2, 6

CALL multiply_test

JMP multiply_done


multiply_test:

MUL R8, R1, R2

RET


multiply_done:


; ============================================================
; COMPROBAR MULTIPLICACIÓN
; ============================================================

MOVI R5, 42

CMP R8, R5
JZERO multiply_ok

HALT


multiply_ok:


; ============================================================
; OPERACIONES LÓGICAS TERNARIAS
; ============================================================

MOVI R1, 1
MOVI R2, -1

TAND R9, R1, R2

TOR R10, R1, R2

TXOR R11, R1, R2

TNOT R12, R1


; ============================================================
; RESULTADOS ESPERADOS
;
; TAND(+1,-1) = -1
; TOR(+1,-1)  =  0
; TXOR(+1,-1) =  0
; TNOT(+1)    = -1
; ============================================================

MOVI R5, -1

CMP R9, R5
JZERO tand_ok

HALT


tand_ok:

MOVI R5, 0

CMP R10, R5
JZERO tor_ok

HALT


tor_ok:

CMP R11, R5
JZERO txor_ok

HALT


txor_ok:

MOVI R5, -1

CMP R12, R5
JZERO logic_ok

HALT


logic_ok:


; ============================================================
; SEGUNDA PRUEBA DE MEMORIA
;
; Escribir varios valores consecutivos.
; ============================================================

MOVI R6, 2000

MOVI R1, 111
MOVI R2, 222
MOVI R3, 333

STORE R1, R6, 0
STORE R2, R6, 1
STORE R3, R6, 2

MOVI R1, 0
MOVI R2, 0
MOVI R3, 0

LOAD R1, R6, 0
LOAD R2, R6, 1
LOAD R3, R6, 2


; ============================================================
; VALIDAR MEMORIA
; ============================================================

MOVI R5, 111

CMP R1, R5
JZERO mem1_ok

HALT


mem1_ok:

MOVI R5, 222

CMP R2, R5
JZERO mem2_ok

HALT


mem2_ok:

MOVI R5, 333

CMP R3, R5
JZERO mem3_ok

HALT


mem3_ok:


; ============================================================
; PRUEBA FINAL
; ============================================================

HALT
""";
}

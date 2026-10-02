/*
 * The MIT License
 *
 * Copyright 2025 Allan (Slam).
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
package t3isa;

/**
 *
 * @author Slam
 */
public final class TAssembler {

    private TAssembler() {
    }

    public static TWord encode(TOpcode opcode, int dst, int src1, int src2, int immediate) {
        validateRegister(dst);
        validateRegister(src1);
        validateRegister(src2);

        if (immediate < -9841 || immediate > 9841) {
            throw new IllegalArgumentException("Immediate fuera de rango: " + immediate);
        }

        TWord word = new TWord();

        writeUnsigned(word, opcode.code, 0, 9);
        writeUnsigned(word, dst, 9, 3);
        writeUnsigned(word, src1, 12, 3);
        writeUnsigned(word, src2, 15, 3);
        /*
         * Los saltos utilizan el campo inmediato como
         * dirección absoluta unsigned.
         */
        if (isJump(opcode)) {
            if (immediate < 0 || immediate >= TCPU.MEMORY_SIZE) {
                throw new IllegalArgumentException("Dirección fuera de rango: " + immediate);
            }
            writeUnsigned(word, immediate, 18, 9);
        } else {
            /*
             * Inmediato signed de 9 trits:
             *
             * -(3^9-1)/2 .. +(3^9-1)/2
             * -9841 .. +9841
             */
            writeSigned(word, immediate, 18, 9);
        }

        return word;
    }

    private static boolean isJump(TOpcode opcode) {
        return opcode == TOpcode.JMP || opcode == TOpcode.JNEG || opcode == TOpcode.JZERO || opcode == TOpcode.JPOS || opcode == TOpcode.CALL;
    }

    public static TWord nop() {
        return encode(TOpcode.NOP, 0, 0, 0, 0);
    }

    public static TWord halt() {
        return encode(TOpcode.HALT, 0, 0, 0, 0);
    }

    public static TWord mov(int dst, int src) {
        return encode(TOpcode.MOV, dst, src, 0, 0);
    }

    public static TWord movi(int dst, int value) {
        return encode(TOpcode.MOVI, dst, 0, 0, value);
    }

    public static TWord add(int dst, int src1, int src2) {
        return encode(TOpcode.ADD, dst, src1, src2, 0);
    }

    public static TWord sub(int dst, int src1, int src2) {
        return encode(TOpcode.SUB, dst, src1, src2, 0);
    }

    public static TWord neg(int dst, int src) {
        return encode(TOpcode.NEG, dst, src, 0, 0);
    }

    public static TWord cmp(int src1, int src2) {
        return encode(TOpcode.CMP, 0, src1, src2, 0);
    }

    public static TWord mul(int dst, int src1, int src2) {
        return encode(TOpcode.MUL, dst, src1, src2, 0);
    }

    public static TWord div(int dst, int src1, int src2) {
        return encode(TOpcode.DIV, dst, src1, src2, 0);
    }

    public static TWord mod(int dst, int src1, int src2) {
        return encode(TOpcode.MOD, dst, src1, src2, 0);
    }

    public static TWord shl(int dst, int src1, int src2) {
        return encode(TOpcode.SHL, dst, src1, src2, 0);
    }

    public static TWord shr(int dst, int src1, int src2) {
        return encode(TOpcode.SHR, dst, src1, src2, 0);
    }

    public static TWord tand(int dst, int src1, int src2) {
        return encode(TOpcode.TAND, dst, src1, src2, 0);
    }

    public static TWord tor(int dst, int src1, int src2) {
        return encode(TOpcode.TOR, dst, src1, src2, 0);
    }

    public static TWord txor(int dst, int src1, int src2) {
        return encode(TOpcode.TXOR, dst, src1, src2, 0);
    }

    public static TWord tnot(int dst, int src) {
        return encode(TOpcode.TNOT, dst, src, 0, 0);
    }

    public static TWord jmp(int address) {
        return encode(TOpcode.JMP, 0, 0, 0, address);
    }

    public static TWord jneg(int address) {
        return encode(TOpcode.JNEG, 0, 0, 0, address);
    }

    public static TWord jzero(int address) {
        return encode(TOpcode.JZERO, 0, 0, 0, address);
    }

    public static TWord jpos(int address) {
        return encode(TOpcode.JPOS, 0, 0, 0, address);
    }

    public static TWord load(int dst, int base, int offset) {
        return encode(TOpcode.LOAD, dst, base, 0, offset);
    }

    public static TWord store(int src, int base, int offset) {
        return encode(TOpcode.STORE, src, base, 0, offset);
    }

    public static TWord push(int src) {
        return encode(TOpcode.PUSH, src, 0, 0, 0);
    }

    public static TWord pop(int dst) {
        return encode(TOpcode.POP, dst, 0, 0, 0);
    }

    public static TWord call(int address) {
        return encode(TOpcode.CALL, 0, 0, 0, address);
    }

    public static TWord ret() {
        return encode(TOpcode.RET, 0, 0, 0, 0);
    }

    private static void validateRegister(int register) {
        if (register < 0 || register >= TCPU.REGISTERS) {
            throw new IllegalArgumentException("Registro inválido: R" + register);
        }
    }

    private static void writeUnsigned(TWord word, int value, int start, int length) {
        int max = pow3(length);

        if (value < 0 || value >= max) {
            throw new IllegalArgumentException("Valor fuera de rango: " + value);
        }

        for (int i = 0; i < length; i++) {
            int digit = value % 3;
            value /= 3;
            word.set(start + i, Trit.fromInt(digit - 1));
        }
    }

    private static void writeSigned(TWord word, int value, int start, int length) {
        int max = (pow3(length) - 1) / 2;

        if (value < -max || value > max) {
            throw new IllegalArgumentException("Valor signed fuera de rango: " + value);
        }

        for (int i = 0; i < length; i++) {
            int remainder = value % 3;
            value /= 3;
            if (remainder == 2) {
                remainder = -1;
                value++;
            }

            if (remainder == -2) {
                remainder = 1;
                value--;
            }

            word.set(start + i, Trit.fromInt(remainder));
        }
    }

    private static int pow3(int n) {
        int result = 1;
        for (int i = 0; i < n; i++) {
            result *= 3;
        }
        return result;
    }
}

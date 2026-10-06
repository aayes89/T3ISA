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
package t3isa.ISA;

import t3isa.Core.TWord;
import t3isa.Core.Trit;

/**
 *
 * @author Slam
 */
public final class TInstruction {

    public static final int OPCODE_TRITS = 9;
    public static final int REGISTER_TRITS = 3;
    public static final int IMMEDIATE_TRITS = 9;

    private final TOpcode opcode;
    private final int dst;
    private final int src1;
    private final int src2;
    private final int immediate;

    private TInstruction(TOpcode opcode, int dst, int src1, int src2, int immediate) {
        this.opcode = opcode;
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
        this.immediate = immediate;
    }

    public TOpcode getOpcode() {
        return opcode;
    }

    public int getDst() {
        return dst;
    }

    public int getSrc1() {
        return src1;
    }

    public int getSrc2() {
        return src2;
    }

    public int getImmediate() {
        return immediate;
    }

    public static TInstruction decode(TWord word) {
        int opcodeValue = readUnsigned(word, 0, 9);
        TOpcode opcode = TOpcode.fromCode(opcodeValue);

        if (opcode == null) {
            throw new IllegalStateException("Opcode inválido: " + opcodeValue);
        }
        int dst = readUnsigned(word, 9, 3);
        int src1 = readUnsigned(word, 12, 3);
        int src2 = readUnsigned(word, 15, 3);
        int immediate;
        if (isJump(opcode)) {
            immediate = readUnsigned(word, 18, 9);
        } else {
            immediate = readSigned(word, 18, 9);
        }

        return new TInstruction(opcode, dst, src1, src2, immediate);
    }


    private static boolean isJump(TOpcode opcode) {
        return opcode == TOpcode.JMP || opcode == TOpcode.JNEG || opcode == TOpcode.JZERO || opcode == TOpcode.JPOS || opcode == TOpcode.CALL;
    }

    private static int readUnsigned(TWord word, int start, int length) {
        int result = 0;
        int power = 1;

        for (int i = 0; i < length; i++) {
            int encoded = word.get(start + i).value + 1;
            result += encoded * power;
            power *= 3;
        }

        return result;
    }

    private static int readSigned(TWord word, int start, int length) {
        int result = 0;
        int power = 1;

        for (int i = 0; i < length; i++) {
            result += word.get(start + i).value * power;
            power *= 3;
        }

        return result;
    }

    private static void writeUnsigned(TWord word, int start, int length, int value) {
        for (int i = 0; i < length; i++) {
            int digit = value % 3;
            value /= 3;
            word.set(start + i, Trit.fromInt(digit - 1));
        }

        if (value != 0) {
            throw new IllegalArgumentException("Valor fuera de rango: " + value);
        }
    }

    private static void writeSigned(TWord word, int start, int length, int value) {
        int remaining = value;

        for (int i = 0; i < length; i++) {
            int remainder = remaining % 3;
            remaining /= 3;

            if (remainder == 2) {
                remainder = -1;
                remaining++;
            } else if (remainder == -2) {
                remainder = 1;
                remaining--;
            }

            word.set(start + i, Trit.fromInt(remainder));
        }

        if (remaining != 0) {
            throw new IllegalArgumentException("Inmediato fuera de rango: " + value);
        }
    }

    @Override

    public String toString() {
        return opcode + " dst=" + dst + " src1=" + src1 + " src2=" + src2 + " imm=" + immediate;
    }
}

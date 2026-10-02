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
public enum TOpcode {

    NOP(0),
    HALT(1),
    MOV(10),
    MOVI(11),
    ADD(20),
    SUB(21),
    NEG(22),
    MUL(23),
    CMP(30),
    TAND(40),
    TOR(41),
    TXOR(42),
    TNOT(43),
    JMP(50),
    JNEG(51),
    JZERO(52),
    JPOS(53),
    LOAD(60),
    STORE(61),
    PUSH(70),
    POP(71),
    CALL(80),
    RET(81),
    SYS(90);

    public final int code;

    TOpcode(int code) {
        this.code = code;
    }

    public static TOpcode fromCode(int code) {

        for (TOpcode opcode : values()) {
            if (opcode.code == code) {
                return opcode;
            }
        }

        return null;
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package t3isa;

/**
 *
 * @author sistemas
 */
public final class TAssembler {

    private TAssembler() {
    }

    public static TWord encode(TOpcode opcode, int dst, int src1, int src2, int immediate) {

        validateRegister(dst);
        validateRegister(src1);
        validateRegister(src2);

        if (immediate < -9842 || immediate > 9842) {
            throw new IllegalArgumentException(
                    "Immediate fuera de rango: " + immediate
            );
        }

        TWord word = new TWord();

        writeUnsigned(word, opcode.code, 0, 9);
        writeUnsigned(word, dst, 9, 3);
        writeUnsigned(word, src1, 12, 3);
        writeUnsigned(word, src2, 15, 3);
        writeSigned(word, immediate, 18, 9);

        return word;
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

    private static void validateRegister(int register) {
        if (register < 0 || register >= 27) {
            throw new IllegalArgumentException(
                    "Registro inválido: R" + register
            );
        }
    }

    private static void writeUnsigned(TWord word, int value, int start, int length) {

        int max = pow3(length);

        if (value < 0 || value >= max) {
            throw new IllegalArgumentException(
                    "Valor fuera de rango: " + value
            );
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
            throw new IllegalArgumentException(
                    "Valor signed fuera de rango: " + value
            );
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

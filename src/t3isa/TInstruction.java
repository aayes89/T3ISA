/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package t3isa;

/**
 *
 * @author sistemas
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

    private TInstruction(
            TOpcode opcode,
            int dst,
            int src1,
            int src2,
            int immediate) {

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

        int opcode = readUnsigned(word, 0, 9);
        int dst = readUnsigned(word, 9, 3);
        int src1 = readUnsigned(word, 12, 3);
        int src2 = readUnsigned(word, 15, 3);
        int immediate = readSigned(word, 18, 9);

        TOpcode operation = TOpcode.fromCode(opcode);

        if (operation == null) {
            throw new IllegalStateException(
                    "Opcode inválido: " + opcode
            );
        }

        return new TInstruction(
                operation,
                dst,
                src1,
                src2,
                immediate
        );
    }

    private static int readUnsigned(
            TWord word,
            int start,
            int length) {

        int result = 0;
        int power = 1;

        for (int i = 0; i < length; i++) {

            int value = word.get(start + i).value;

            /*
             * Los campos de codificación usan
             * 0, 1, 2 internamente.
             */
            int encoded = value + 1;

            result += encoded * power;

            power *= 3;
        }

        return result;
    }

    private static int readSigned(
            TWord word,
            int start,
            int length) {

        int result = 0;
        int power = 1;

        for (int i = 0; i < length; i++) {

            result
                    += word.get(start + i).value
                    * power;

            power *= 3;
        }

        return result;
    }

    @Override
    public String toString() {

        return opcode
                + " dst=" + dst
                + " src1=" + src1
                + " src2=" + src2
                + " imm=" + immediate;
    }
}

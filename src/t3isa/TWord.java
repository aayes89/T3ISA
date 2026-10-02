/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package t3isa;

/**
 *
 * @author sistemas
 */

public final class TWord {

    public static final int TRITS = 27;

    private final Trit[] data;

    public TWord() {
        data = new Trit[TRITS];

        for (int i = 0; i < TRITS; i++) {
            data[i] = Trit.ZERO;
        }
    }

    public TWord(Trit[] value) {
        if (value.length != TRITS) {
            throw new IllegalArgumentException("TWord requiere 27 trits");
        }

        data = value.clone();
    }

    public static TWord zero() {
        return new TWord();
    }

    public static TWord fromLong(long value) {

        TWord word = new TWord();

        for (int i = 0; i < TRITS; i++) {

            long remainder = value % 3;
            value /= 3;

            if (remainder == 2) {
                remainder = -1;
                value++;
            }

            word.data[i] = Trit.fromInt((int) remainder);
        }

        return word;
    }

    public long toLong() {

        long result = 0;
        long power = 1;

        for (int i = 0; i < TRITS; i++) {

            result += data[i].value * power;
            power *= 3;
        }

        return result;
    }

    public Trit get(int index) {
        return data[index];
    }

    public void set(int index, Trit value) {
        data[index] = value;
    }

    public TWord copy() {
        return new TWord(data);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder(TRITS);

        for (int i = TRITS - 1; i >= 0; i--) {

            switch (data[i]) {

                case NEG:
                    sb.append('-');
                    break;

                case ZERO:
                    sb.append('0');
                    break;

                case POS:
                    sb.append('+');
                    break;
            }
        }

        return sb.toString();
    }
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package t3isa;

/**
 *
 * @author sistemas
 */
public final class TALU {

    private TALU() {
    }

    public static TWord add(TWord a, TWord b) {
        TWord result = new TWord();
        int carry = 0;

        for (int i = 0; i < TWord.TRITS; i++) {

            int value = a.get(i).value + b.get(i).value + carry;

            if (value > 1) {
                value -= 3;
                carry = 1;
            } else if (value < -1) {
                value += 3;
                carry = -1;
            } else {
                carry = 0;
            }

            result.set(i, Trit.fromInt(value));
        }

        return result;
    }

    public static TWord negate(TWord value) {

        TWord result = new TWord();

        for (int i = 0; i < TWord.TRITS; i++) {
            result.set(i, Trit.negate(value.get(i)));
        }

        return result;
    }

    public static TWord subtract(TWord a, TWord b) {
        return add(a, negate(b));
    }

    public static TWord and(TWord a, TWord b) {

        TWord result = new TWord();

        for (int i = 0; i < TWord.TRITS; i++) {

            int x = a.get(i).value;
            int y = b.get(i).value;

            int value = x * y;

            result.set(i, Trit.fromInt(value));
        }

        return result;
    }

    public static TWord or(TWord a, TWord b) {

        TWord result = new TWord();

        for (int i = 0; i < TWord.TRITS; i++) {

            int x = a.get(i).value;
            int y = b.get(i).value;

            int value = Integer.signum(x + y);

            result.set(i, Trit.fromInt(value));
        }

        return result;
    }

    public static TWord not(TWord value) {

        return negate(value);
    }

    public static int compare(TWord a, TWord b) {

        for (int i = TWord.TRITS - 1; i >= 0; i--) {

            int x = a.get(i).value;
            int y = b.get(i).value;

            if (x < y) {
                return -1;
            }

            if (x > y) {
                return 1;
            }
        }

        return 0;
    }

    public static TWord increment(TWord value) {
        return add(value, TWord.fromLong(1));
    }

    public static TWord decrement(TWord value) {
        return subtract(value, TWord.fromLong(1));
    }
}

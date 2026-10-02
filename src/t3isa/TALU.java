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

    public static TWord multiply(TWord a, TWord b) {
        TWord result = TWord.zero();
        TWord multiplicand = a.copy();

        for (int i = 0; i < TWord.TRITS; i++) {
            int trit = b.get(i).value;

            if (trit == 1) {
                result = add(result, multiplicand);
            } else if (trit == -1) {
                result = subtract(result, multiplicand);
            }

            /*  
            * Siguiente potencia de 3:
            *
            * multiplicand *= 3
            *
            * En ternario:
            * a + a + a = a * 3
             */
            multiplicand = add(add(multiplicand, multiplicand), multiplicand);
        }

        return result;
    }

    public static TWord divide(TWord a, TWord b) {
        long divisor = b.toLong();

        if (divisor == 0) {
            throw new ArithmeticException("División entre cero");
        }
        long dividend = a.toLong();
        return TWord.fromLong(dividend / divisor);
    }

    public static TWord modulo(TWord a, TWord b) {
        long divisor = b.toLong();

        if (divisor == 0) {
            throw new ArithmeticException("Módulo entre cero");
        }
        long dividend = a.toLong();
        return TWord.fromLong(dividend % divisor);
    }

    public static TWord xor(TWord a, TWord b) {
        TWord result = new TWord();

        for (int i = 0; i < TWord.TRITS; i++) {
            int value = a.get(i).value + b.get(i).value;

            if (value > 1) {
                value -= 3;
            } else if (value < -1) {
                value += 3;
            }

            result.set(i, Trit.fromInt(value));
        }

        return result;
    }

    public static TWord shiftLeft(TWord value, int amount) {
        TWord result = TWord.zero();

        if (amount <= 0) {
            return value.copy();
        }
        if (amount >= TWord.TRITS) {
            return result;
        }
        for (int i = amount; i < TWord.TRITS; i++) {
            result.set(i, value.get(i - amount));
        }

        return result;
    }

    public static TWord shiftRight(TWord value, int amount) {
        TWord result = TWord.zero();

        if (amount <= 0) {
            return value.copy();
        }
        if (amount >= TWord.TRITS) {
            return result;
        }
        for (int i = 0; i < TWord.TRITS - amount; i++) {
            result.set(i, value.get(i + amount));
        }

        return result;
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

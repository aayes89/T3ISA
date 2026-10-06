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
package t3isa.Core;

/**
 *
 * @author Slam
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
            } else if (remainder == -2) {
                remainder = 1;
                value--;
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

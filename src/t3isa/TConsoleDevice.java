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
package t3isa;

/**
 *
 * @author Slam
 */
import java.io.BufferedReader;
import java.io.IOException;

import java.io.InputStreamReader;

public final class TConsoleDevice implements TDevice {

    private final BufferedReader input;

    public TConsoleDevice() {
        input = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void write(TWord value) {
        System.out.println("DEVICE OUT = " + value.toLong());
    }

    @Override
    public TWord read() {
        System.out.print("DEVICE IN > ");

        try {
            String line = input.readLine();

            if (line == null || line.trim().isEmpty()) {
                return TWord.zero();
            }

            long value = Long.parseLong(line.trim());
            
            return TWord.fromLong(value);
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo dispositivo", e);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Entrada no numérica", e);
        }
    }
}

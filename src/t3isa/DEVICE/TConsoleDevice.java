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
package t3isa.DEVICE;

/**
 *
 * @author Slam
 */
import t3isa.CORE.TWord;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;

import java.util.Queue;

public final class TConsoleDevice implements TDevice {

    private final BufferedReader input;
    private final Queue<TWord> inputQueue;

    public TConsoleDevice() {
        input = new BufferedReader(new InputStreamReader(System.in));
        inputQueue = new ArrayDeque<>();
    }

    @Override
    public void write(TWord value) {
        long code = value.toLong();

        if (code == 10) {
            System.out.println();
        } else if (code >= 32 && code <= 126) {
            System.out.print((char) code);
        } else {
            System.out.print("[" + code + "]");
        }
    }

    public void writeText(String text) {
        System.out.print(text);
    }

    public void writeLine(String text) {
        System.out.println(text);
    }

    public String readLine() {
        try {
            return input.readLine();
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo consola", e);
        }
    }

    public void enqueueInput(long value) {
        inputQueue.add(TWord.fromLong(value));
    }

    @Override
    public TWord read() {
        if (inputQueue.isEmpty()) {
            readLineIntoQueue();
        }

        if (inputQueue.isEmpty()) {
            return TWord.zero();
        }

        return inputQueue.poll();
    }

    @Override
    public boolean hasInput() {
        return !inputQueue.isEmpty();
    }

    private void readLineIntoQueue() {
        System.out.print("DEVICE IN > ");

        try {
            String line = input.readLine();
            if (line == null) {
                return;
            }

            for (int i = 0; i < line.length(); i++) {
                inputQueue.add(TWord.fromLong(line.charAt(i)));
            }
            inputQueue.add(TWord.fromLong(10));
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo consola", e);
        }
    }
}

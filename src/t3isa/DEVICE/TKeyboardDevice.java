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
import java.util.ArrayDeque;

import java.util.Queue;
import t3isa.Core.TWord;
import t3isa.HARDWARE.TDevice;

// Representación de un teclado a nivel de hardware
public class TKeyboardDevice implements TDevice {

    // Tecla física del teclado
    public static final class Key {

        private final int code;
        private final char character;

        public Key(int code, char character) {
            this.code = code;
            this.character = character;
        }

        public int getCode() {
            return code;
        }

        public char getCharacter() {
            return character;
        }
    }

    // Cola del teclado
    private final Queue<Key> queue;

    public TKeyboardDevice() {
        queue = new ArrayDeque<>();
    }

    // Almacenar y asignar código de tecla a caracter (Cola)
    public synchronized void push(int code, char character) {
        queue.add(new Key(code, character));
    }

    // Liberar recursos del teclado obteniendolos
    public synchronized Key poll() {
        return queue.poll();
    }

    // Está vacía la cola?
    public synchronized boolean hasKey() {
        return !queue.isEmpty();
    }

    // Limpiar cola
    public synchronized void clear() {
        queue.clear();
    }

    // NO son necesarios por ahora
    @Override
    public void write(TWord value) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public TWord read() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean hasInput() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}

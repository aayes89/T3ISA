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
import t3isa.Core.TWord;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

/**
 *
 * @author Slam
 */
public final class TNetworkDevice implements TDevice {

    private static final int MAC_LENGTH = 6;
    private static final int MAX_FRAME_SIZE = 1518;

    private final byte[] macAddress;
    private final Queue<byte[]> rxQueue;

    private byte[] txFrame;

    public TNetworkDevice(byte[] macAddress) {
        if (macAddress == null || macAddress.length != MAC_LENGTH) {
            throw new IllegalArgumentException("La dirección MAC debe tener 6 bytes");
        }

        this.macAddress = Arrays.copyOf(macAddress, MAC_LENGTH);
        this.rxQueue = new ArrayDeque<>();
    }

    public byte[] getMAC() {
        return Arrays.copyOf(macAddress, MAC_LENGTH);
    }

    public void sendFrame(byte[] frame) {
        if (frame == null) {
            throw new IllegalArgumentException("El frame no puede ser null");
        }

        if (frame.length == 0 || frame.length > MAX_FRAME_SIZE) {
            throw new IllegalArgumentException("Tamaño de frame inválido: " + frame.length);
        }

        txFrame = Arrays.copyOf(frame, frame.length);
    }

    public byte[] receiveFrame() {
        byte[] frame = rxQueue.poll();

        if (frame == null) {
            return null;
        }

        return Arrays.copyOf(frame, frame.length);
    }

    public boolean hasPacket() {
        return !rxQueue.isEmpty();
    }

    // Entrada de frames desde el medio de red.
    public void injectFrame(byte[] frame) {
        if (frame == null) {
            throw new IllegalArgumentException("El frame no puede ser null");
        }

        if (frame.length == 0 || frame.length > MAX_FRAME_SIZE) {
            throw new IllegalArgumentException("Tamaño de frame inválido: " + frame.length);
        }

        rxQueue.add(Arrays.copyOf(frame, frame.length));
    }

    // Obtiene el último frame transmitido.
    public byte[] getTransmittedFrame() {
        if (txFrame == null) {
            return null;
        }

        return Arrays.copyOf(txFrame, txFrame.length);
    }

    @Override
    public void write(TWord value) {
        /*
         * La interfaz TDevice trabaja con TWord.
         *
         * La interfaz Ethernet utilizará sendFrame()
         * directamente para transmitir frames.
         */
    }

    @Override
    public TWord read() {
        /*
         * La interfaz de frames utiliza receiveFrame().
         */
        return TWord.zero();
    }

    @Override
    public boolean hasInput() {
        return hasPacket();
    }
}

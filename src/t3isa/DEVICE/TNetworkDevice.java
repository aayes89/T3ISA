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
import java.util.Arrays;

public final class TNetworkDevice implements TDevice {

    private static final int MAC_LENGTH = 6;
    private final byte[] macAddress;
    private final TNetworkBackend backend;

    public TNetworkDevice(TNetworkBackend backend) {
        if (backend == null) {
            throw new IllegalArgumentException("El backend no puede ser null");
        }

        macAddress = backend.getMAC();

        if (macAddress == null || macAddress.length != MAC_LENGTH) {
            throw new IllegalArgumentException("MAC inválida");
        }
        this.backend = backend;

    }

    public byte[] getMAC() {
        return Arrays.copyOf(macAddress, MAC_LENGTH);
    }

    public void open() {
        backend.open();
    }

    public void close() {
        backend.close();
    }

    public void sendFrame(byte[] frame) {
        if (frame == null) {
            throw new IllegalArgumentException("El frame no puede ser null");
        }

        if (frame.length == 0 || frame.length > 1518) {
            throw new IllegalArgumentException("Tamaño de frame inválido: " + frame.length);
        }

        backend.writeFrame(frame);
    }

    public byte[] receiveFrame() {
        return backend.readFrame();
    }

    public boolean hasPacket() {
        return backend.hasFrame();
    }

    @Override
    public void write(TWord value) {
        // Reservado para acceso por registros/I/O
    }

    @Override
    public TWord read() {
        return TWord.zero();
    }

    @Override
    public boolean hasInput() {
        return backend.hasFrame();
    }
}

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
import java.util.Arrays;
import java.util.Queue;

public final class TNetworkLinkBackend implements TNetworkBackend {

    private final Queue<byte[]> rxQueue = new ArrayDeque<>();
    private TNetworkLinkBackend peer;
    private boolean opened;

    public void connect(TNetworkLinkBackend peer) {
        if (peer == null) {
            throw new IllegalArgumentException("El backend remoto no puede ser null");
        }

        if (peer == this) {
            throw new IllegalArgumentException("No se puede conectar consigo mismo");
        }
        this.peer = peer;
    }

    @Override
    public void open() {
        opened = true;
    }

    @Override
    public void close() {
        opened = false;
        rxQueue.clear();
    }

    @Override
    public void writeFrame(byte[] frame) {
        if (!opened) {
            throw new IllegalStateException("El backend no está abierto");
        }

        if (peer == null) {
            return;
            //throw new IllegalStateException("El backend no está conectado");
        }

        if (frame == null || frame.length == 0) {
            throw new IllegalArgumentException("Frame inválido");
        }

        peer.rxQueue.add(Arrays.copyOf(frame, frame.length));
    }

    @Override
    public byte[] readFrame() {
        if (!opened) {
            throw new IllegalStateException("El backend no está abierto");
        }

        byte[] frame = rxQueue.poll();

        if (frame == null) {
            return null;
        }

        return Arrays.copyOf(frame, frame.length);
    }

    @Override
    public boolean hasFrame() {
        return !rxQueue.isEmpty();
    }

    @Override
    public byte[] getMAC() {
        // Harcodeada por ahora, tocará obtenerla de adaptador físico
        return new byte[]{
            0x02, 0x54, 0x33, 0x00, 0x00, 0x01
        };
    }
}

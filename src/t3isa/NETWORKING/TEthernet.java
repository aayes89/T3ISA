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
package t3isa.NETWORKING;

/**
 *
 * @author Slam
 */
import java.util.Arrays;
import t3isa.DEVICE.TNetworkDevice;

public final class TEthernet {

    public static final int HEADER_SIZE = 14;
    public static final int MIN_FRAME_SIZE = 60;
    public static final int MAX_FRAME_SIZE = 1518;

    public static final int TYPE_IPV4 = 0x0800;
    public static final int TYPE_ARP = 0x0806;

    private static final byte[] BROADCAST_MAC = {
        (byte) 0xFF, // 255
        (byte) 0xFF, // 255
        (byte) 0xFF, // 255
        (byte) 0xFF, // 255
        (byte) 0xFF, // 255
        (byte) 0xFF // 255
    };

    private final TNetworkDevice device;

    public TEthernet(TNetworkDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("El dispositivo de red no puede ser null");
        }

        this.device = device;
    }

    public void send(byte[] destination, int etherType, byte[] payload) {
        checkMAC(destination);

        if (etherType < 0 || etherType > 0xFFFF) {
            throw new IllegalArgumentException("EtherType inválido: " + etherType);
        }

        if (payload == null) {
            payload = new byte[0];
        }

        int frameSize = HEADER_SIZE + payload.length;

        if (frameSize > MAX_FRAME_SIZE) {
            throw new IllegalArgumentException("Frame Ethernet demasiado grande");
        }

        int paddedSize = Math.max(frameSize, MIN_FRAME_SIZE);
        byte[] frame = new byte[paddedSize];

        System.arraycopy(destination, 0, frame, 0, 6);

        byte[] source = device.getMAC();
        System.arraycopy(source, 0, frame, 6, 6);

        frame[12] = (byte) ((etherType >>> 8) & 0xFF);
        frame[13] = (byte) (etherType & 0xFF);

        System.arraycopy(payload, 0, frame, HEADER_SIZE, payload.length);
        device.sendFrame(frame);
    }

    public Frame receive(byte[] frame) {
        if (frame == null) {
            return null;
        }

        if (frame.length < MIN_FRAME_SIZE || frame.length > MAX_FRAME_SIZE) {
            throw new IllegalStateException("Frame Ethernet inválido: " + frame.length);
        }

        byte[] destination = Arrays.copyOfRange(frame, 0, 6);
        byte[] source = Arrays.copyOfRange(frame, 6, 12);

        int etherType = ((frame[12] & 0xFF) << 8) | (frame[13] & 0xFF);
        byte[] payload = Arrays.copyOfRange(frame, HEADER_SIZE, frame.length);

        return new Frame(destination, source, etherType, payload);
    }

    public Frame receive() {
        byte[] frame = device.receiveFrame();

        if (frame == null) {
            return null;
        }

        if (frame.length < MIN_FRAME_SIZE || frame.length > MAX_FRAME_SIZE) {
            throw new IllegalStateException("Frame Ethernet inválido: " + frame.length);
        }

        byte[] destination = Arrays.copyOfRange(frame, 0, 6);
        byte[] source = Arrays.copyOfRange(frame, 6, 12);

        int etherType = ((frame[12] & 0xFF) << 8) | (frame[13] & 0xFF);

        byte[] payload = Arrays.copyOfRange(frame, HEADER_SIZE, frame.length);

        return new Frame(destination, source, etherType, payload);
    }

    public boolean hasFrame() {
        return device.hasPacket();
    }

    public static boolean isBroadcast(byte[] mac) {
        return Arrays.equals(mac, BROADCAST_MAC);
    }

    public static boolean sameMAC(byte[] a, byte[] b) {
        return a != null && b != null && Arrays.equals(a, b);
    }

    private static void checkMAC(byte[] mac) {
        if (mac == null || mac.length != 6) {
            throw new IllegalArgumentException("MAC inválida");
        }
    }

    public static final class Frame {

        private final byte[] destination;
        private final byte[] source;
        private final int etherType;
        private final byte[] payload;

        private Frame(byte[] destination, byte[] source, int etherType, byte[] payload) {
            this.destination = Arrays.copyOf(destination, 6);
            this.source = Arrays.copyOf(source, 6);
            this.etherType = etherType;
            this.payload = Arrays.copyOf(payload, payload.length);
        }

        public byte[] getDestination() {
            return Arrays.copyOf(destination, destination.length);
        }

        public byte[] getSource() {
            return Arrays.copyOf(source, source.length);
        }

        public int getEtherType() {
            return etherType;
        }

        public byte[] getPayload() {
            return Arrays.copyOf(payload, payload.length);
        }
    }
}

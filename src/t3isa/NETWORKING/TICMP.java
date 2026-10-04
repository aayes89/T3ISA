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

public final class TICMP {

    public static final int ECHO_REPLY = 0;
    public static final int ECHO_REQUEST = 8;

    private final TIPv4 ipv4;

    private int sequence;

    public TICMP(TIPv4 ipv4) {
        if (ipv4 == null) {
            throw new IllegalArgumentException("IPv4 no puede ser null");
        }

        this.ipv4 = ipv4;
    }

    public void ping(byte[] destinationIP) {
        checkIP(destinationIP);

        int currentSequence = sequence++ & 0xFFFF;
        byte[] payload = new byte[4];

        write16(payload, 0, 0x5433);
        write16(payload, 2, currentSequence);

        byte[] packet = buildPacket(ECHO_REQUEST, 0, 0x5433, currentSequence, payload);

        ipv4.send(destinationIP, TIPv4.PROTOCOL_ICMP, packet);
    }

    public Echo receive() {
        TIPv4.Packet packet;

        while ((packet = ipv4.receive()) != null) {
            if (packet.getProtocol() != TIPv4.PROTOCOL_ICMP) {
                continue;
            }

            byte[] data = packet.getPayload();
            if (data.length < 8) {
                continue;
            }

            int type = data[0] & 0xFF;
            int code = data[1] & 0xFF;

            if (checksum(data, 0, data.length) != 0) {
                continue;
            }

            int identifier = read16(data, 4);
            int sequence = read16(data, 6);
            byte[] payload = Arrays.copyOfRange(data, 8, data.length);

            // Responder de Echo Request.
            if (type == ECHO_REQUEST && code == 0) {
                byte[] reply = buildPacket(ECHO_REPLY, 0, identifier, sequence, payload);
                ipv4.send(packet.getSourceIP(), TIPv4.PROTOCOL_ICMP, reply);
                continue;
            }

            // Respuesta a nuestro Echo Request.
            if (type == ECHO_REPLY && code == 0) {
                return new Echo(packet.getSourceIP(), identifier, sequence, payload);
            }
        }

        return null;
    }

    private static byte[] buildPacket(int type, int code, int identifier, int sequence, byte[] payload) {
        byte[] packet = new byte[8 + payload.length];

        packet[0] = (byte) type;
        packet[1] = (byte) code;

        // Checksum inicialmente cero.
        packet[2] = 0;
        packet[3] = 0;

        write16(packet, 4, identifier);
        write16(packet, 6, sequence);

        if (payload.length > 0) {
            System.arraycopy(payload, 0, packet, 8, payload.length);
        }

        int checksum = checksum(packet, 0, packet.length);
        write16(packet, 2, checksum);

        return packet;
    }

    private static int checksum(byte[] data, int offset, int length) {
        long sum = 0;
        for (int i = 0; i < length; i += 2) {
            int high = data[offset + i] & 0xFF;
            int low = 0;

            if (i + 1 < length) {
                low = data[offset + i + 1] & 0xFF;
            }

            sum += (high << 8) | low;
            while ((sum >>> 16) != 0) {
                sum = (sum & 0xFFFF) + (sum >>> 16);
            }
        }

        return (int) ((~sum) & 0xFFFF);
    }

    private static int read16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static void write16(byte[] data, int offset, int value) {
        data[offset] = (byte) ((value >>> 8) & 0xFF);
        data[offset + 1] = (byte) (value & 0xFF);
    }

    private static void checkIP(byte[] ip) {
        if (ip == null || ip.length != 4) {
            throw new IllegalArgumentException("IPv4 inválida");
        }
    }

    public static final class Echo {

        private final byte[] sourceIP;
        private final int identifier;
        private final int sequence;
        private final byte[] payload;

        private Echo(byte[] sourceIP, int identifier, int sequence, byte[] payload) {
            this.sourceIP = Arrays.copyOf(sourceIP, 4);
            this.identifier = identifier;
            this.sequence = sequence;
            this.payload = Arrays.copyOf(payload, payload.length);
        }

        public byte[] getSourceIP() {
            return Arrays.copyOf(sourceIP, 4);
        }

        public int getIdentifier() {
            return identifier;
        }

        public int getSequence() {
            return sequence;
        }

        public byte[] getPayload() {
            return Arrays.copyOf(payload, payload.length);
        }
    }
}

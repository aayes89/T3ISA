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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public final class TIPv4 {

    public static final int HEADER_SIZE = 20;

    public static final int PROTOCOL_ICMP = 1;
    public static final int PROTOCOL_TCP = 6;
    public static final int PROTOCOL_UDP = 17;

    private final TEthernet ethernet;
    private final TARP arp;

    private final byte[] localIP;
    private final byte[] netmask;
    private final byte[] gateway;

    private Queue<Packet> rxQueue = new ArrayDeque<>();

    public TIPv4(TEthernet ethernet, TARP arp, byte[] localIP, byte[] netmask, byte[] gateway) {

        if (ethernet == null) {
            throw new IllegalArgumentException("Ethernet no puede ser null");
        }

        if (arp == null) {
            throw new IllegalArgumentException("ARP no puede ser null");
        }

        checkIP(localIP);
        checkIP(netmask);
        checkIP(gateway);

        this.ethernet = ethernet;
        this.arp = arp;

        this.localIP = Arrays.copyOf(localIP, 4);
        this.netmask = Arrays.copyOf(netmask, 4);
        this.gateway = Arrays.copyOf(gateway, 4);
        this.rxQueue = new ArrayDeque<>();
    }

    public byte[] getLocalIP() {
        return Arrays.copyOf(localIP, 4);
    }

    public byte[] getNetmask() {
        return Arrays.copyOf(netmask, 4);
    }

    public byte[] getGateway() {
        return Arrays.copyOf(gateway, 4);
    }

    public void send(byte[] destinationIP, int protocol, byte[] payload) {
        checkIP(destinationIP);

        if (protocol < 0 || protocol > 255) {
            throw new IllegalArgumentException("Protocolo IPv4 inválido: " + protocol);
        }

        if (payload == null) {
            payload = new byte[0];
        }

        int totalLength = HEADER_SIZE + payload.length;
        if (totalLength > 0xFFFF) {
            throw new IllegalArgumentException("Paquete IPv4 demasiado grande");
        }

        // Si el destino está fuera de nuestra red, se utiliza el gateway.
        byte[] nextHop;

        if (sameNetwork(localIP, destinationIP)) {
            nextHop = destinationIP;
        } else {
            nextHop = gateway;
        }

        byte[] destinationMAC = arp.resolve(nextHop);
        if (destinationMAC == null) {
            arp.request(nextHop);

            throw new IllegalStateException("MAC desconocida para " + ipToString(nextHop));
        }

        byte[] packet = new byte[totalLength];

        // Version = 4 IHL = 5
        packet[0] = 0x45;

        // DSCP / ECN
        packet[1] = 0;
        write16(packet, 2, totalLength);

        // Identification
        write16(packet, 4, 0);

        // Flags + fragment offset. DF = 1
        write16(packet, 6, 0x4000);

        // TTL
        packet[8] = 64;

        // Protocol
        packet[9] = (byte) protocol;

        // Checksum inicialmente cero.
        packet[10] = 0;
        packet[11] = 0;

        // Source IP
        System.arraycopy(localIP, 0, packet, 12, 4);

        // Destination IP
        System.arraycopy(destinationIP, 0, packet, 16, 4);

        // Payload
        System.arraycopy(payload, 0, packet, HEADER_SIZE, payload.length);
        int checksum = checksum(packet, 0, HEADER_SIZE);

        write16(packet, 10, checksum);
        ethernet.send(destinationMAC, TEthernet.TYPE_IPV4, packet);
    }

    public Packet receive() {
        return rxQueue.poll();
    }

    public void receive(TEthernet.Frame frame) {
        if (frame == null || frame.getEtherType() != TEthernet.TYPE_IPV4) {
            return;
        }

        byte[] packet = frame.getPayload();
        if (packet.length < HEADER_SIZE) {
            return;
        }

        int version = (packet[0] >>> 4) & 0x0F;
        int ihl = packet[0] & 0x0F;
        if (version != 4 || ihl < 5) {
            return;
        }

        int headerLength = ihl * 4;
        if (packet.length < headerLength) {
            return;
        }

        int totalLength = read16(packet, 2);
        if (totalLength < headerLength || totalLength > packet.length) {
            return;
        }

        if (checksum(packet, 0, headerLength) != 0) {
            return;
        }

        byte[] sourceIP = Arrays.copyOfRange(packet, 12, 16);
        byte[] destinationIP = Arrays.copyOfRange(packet, 16, 20);

        if (!Arrays.equals(destinationIP, localIP)) {
            return;
        }

        int protocol = packet[9] & 0xFF;

        byte[] payload = Arrays.copyOfRange(packet, headerLength, totalLength);
        rxQueue.add(new Packet(sourceIP, destinationIP, protocol, payload));
    }

    private boolean sameNetwork(byte[] a, byte[] b) {
        for (int i = 0; i < 4; i++) {
            if ((a[i] & netmask[i]) != (b[i] & netmask[i])) {
                return false;
            }
        }

        return true;
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

        return (int) (~sum) & 0xFFFF;
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

    public static String ipToString(byte[] ip) {
        checkIP(ip);
        return (ip[0] & 0xFF) + "." + (ip[1] & 0xFF) + "." + (ip[2] & 0xFF) + "." + (ip[3] & 0xFF);
    }

    public static final class Packet {

        private final byte[] sourceIP;
        private final byte[] destinationIP;
        private final int protocol;
        private final byte[] payload;

        private Packet(byte[] sourceIP, byte[] destinationIP, int protocol, byte[] payload) {
            this.sourceIP = Arrays.copyOf(sourceIP, 4);
            this.destinationIP = Arrays.copyOf(destinationIP, 4);
            this.protocol = protocol;
            this.payload = Arrays.copyOf(payload, payload.length);
        }

        public byte[] getSourceIP() {
            return Arrays.copyOf(sourceIP, 4);
        }

        public byte[] getDestinationIP() {
            return Arrays.copyOf(destinationIP, 4);
        }

        public int getProtocol() {
            return protocol;
        }

        public byte[] getPayload() {
            return Arrays.copyOf(payload, payload.length);
        }
    }
}

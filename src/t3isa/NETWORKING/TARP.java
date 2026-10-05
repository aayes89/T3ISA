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
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public final class TARP {

    public static final int HARDWARE_ETHERNET = 1;
    public static final int PROTOCOL_IPV4 = 0x0800;

    public static final int REQUEST = 1;
    public static final int REPLY = 2;

    private static final int PACKET_SIZE = 28;

    private final TEthernet ethernet;
    private final byte[] localMAC;
    private final byte[] localIP;

    private final Map<Integer, byte[]> cache;
    private final Queue<TEthernet.Frame> rxQueue = new ArrayDeque<>();

    public TARP(TEthernet ethernet, byte[] localMAC, byte[] localIP) {
        if (ethernet == null) {
            throw new IllegalArgumentException("Ethernet no puede ser null");
        }

        checkMAC(localMAC);
        checkIP(localIP);

        this.ethernet = ethernet;
        this.localMAC = Arrays.copyOf(localMAC, 6);
        this.localIP = Arrays.copyOf(localIP, 4);
        this.cache = new HashMap<>();
    }

    public void request(byte[] targetIP) {
        checkIP(targetIP);

        byte[] packet = buildPacket(REQUEST, localMAC, localIP, new byte[6], targetIP);
        ethernet.send(broadcastMAC(), TEthernet.TYPE_ARP, packet);
    }

    public void receive() {
        while (ethernet.hasFrame()) {
            TEthernet.Frame frame = ethernet.receive();
            if (frame != null) {
                receive(frame);
            }
        }
    }

    public void receive(TEthernet.Frame frame) {
        if (frame == null || frame.getEtherType() != TEthernet.TYPE_ARP) {
            return;
        }

        rxQueue.add(frame);
        processFrame(frame);
    }

    private void processFrame(TEthernet.Frame frame) {
        byte[] packet = frame.getPayload();
        if (packet.length < PACKET_SIZE) {
            return;
        }
        parsePacket(frame.getSource(), packet);
    }

    public byte[] resolve(byte[] ip) {
        checkIP(ip);
        byte[] mac = cache.get(ipToInt(ip));
        if (mac == null) {
            return null;
        }

        return Arrays.copyOf(mac, 6);
    }

    public boolean hasAddress(byte[] ip) {
        return resolve(ip) != null;
    }

    public static String intToIP(int value) {
        return ((value >>> 24) & 0xFF) + "."
                + ((value >>> 16) & 0xFF) + "."
                + ((value >>> 8) & 0xFF) + "."
                + (value & 0xFF);
    }

    private void parsePacket(byte[] ethernetSource, byte[] packet) {
        int hardwareType = read16(packet, 0);
        int protocolType = read16(packet, 2);
        int hardwareLength = packet[4] & 0xFF;
        int protocolLength = packet[5] & 0xFF;
        int operation = read16(packet, 6);

        if (hardwareType != HARDWARE_ETHERNET
                || protocolType != PROTOCOL_IPV4
                || hardwareLength != 6
                || protocolLength != 4) {
            return;
        }

        byte[] senderMAC = Arrays.copyOfRange(packet, 8, 14);
        byte[] senderIP = Arrays.copyOfRange(packet, 14, 18);
        byte[] targetMAC = Arrays.copyOfRange(packet, 18, 24);
        byte[] targetIP = Arrays.copyOfRange(packet, 24, 28);
        System.out.println(
                "ARP RX: op=" + operation
                + " sender=" + intToIP(ipToInt(senderIP))
                + " target=" + intToIP(ipToInt(targetIP))
                + " senderMAC=" + macToString(senderMAC)
                + " ethernetSource=" + macToString(ethernetSource)
        );

        // Aprender siempre la asociación del emisor.
        cache.put(ipToInt(senderIP), senderMAC);

        if (operation == REQUEST && Arrays.equals(targetIP, localIP)) {
            byte[] reply = buildPacket(REPLY, localMAC, localIP, senderMAC, senderIP);
            ethernet.send(ethernetSource, TEthernet.TYPE_ARP, reply);
        }
    }

    private static String macToString(byte[] mac) {
        if (mac == null || mac.length != 6) {
            return "N/A";
        }

        return String.format(
                "%02X:%02X:%02X:%02X:%02X:%02X",
                mac[0] & 0xFF,
                mac[1] & 0xFF,
                mac[2] & 0xFF,
                mac[3] & 0xFF,
                mac[4] & 0xFF,
                mac[5] & 0xFF
        );
    }

    private static byte[] buildPacket(int operation, byte[] senderMAC, byte[] senderIP, byte[] targetMAC, byte[] targetIP) {
        byte[] packet = new byte[PACKET_SIZE];

        write16(packet, 0, HARDWARE_ETHERNET);
        write16(packet, 2, PROTOCOL_IPV4);

        packet[4] = 6;
        packet[5] = 4;

        write16(packet, 6, operation);

        System.arraycopy(senderMAC, 0, packet, 8, 6);
        System.arraycopy(senderIP, 0, packet, 14, 4);
        System.arraycopy(targetMAC, 0, packet, 18, 6);
        System.arraycopy(targetIP, 0, packet, 24, 4);

        return packet;
    }

    private static int ipToInt(byte[] ip) {
        return ((ip[0] & 0xFF) << 24) | ((ip[1] & 0xFF) << 16) | ((ip[2] & 0xFF) << 8) | (ip[3] & 0xFF);
    }

    private static int read16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static void write16(byte[] data, int offset, int value) {
        data[offset] = (byte) ((value >>> 8) & 0xFF);
        data[offset + 1] = (byte) (value & 0xFF);
    }

    private static byte[] broadcastMAC() {
        byte[] mac = new byte[6];
        Arrays.fill(mac, (byte) 0xFF);
        return mac;
    }

    private static void checkMAC(byte[] mac) {
        if (mac == null || mac.length != 6) {
            throw new IllegalArgumentException("MAC inválida");
        }
    }

    private static void checkIP(byte[] ip) {
        if (ip == null || ip.length != 4) {
            throw new IllegalArgumentException("IPv4 inválida");
        }
    }

    public Map<Integer, byte[]> getCache() {
        Map<Integer, byte[]> result = new HashMap<>();

        for (Map.Entry<Integer, byte[]> entry : cache.entrySet()) {
            result.put(entry.getKey(), Arrays.copyOf(entry.getValue(), 6));
        }

        return result;
    }
}

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

/**
 *
 * TCP básico sobre TIPv4.
 *
 * Implementa:
 *
 * SYN SYN-ACK ACK PSH/ACK FIN/ACK RST
 *
 * No implementa todavía:
 *
 * retransmisiones ventana deslizante congestion control SACK opciones TCP
 */
public final class TTCP {

    public static final int HEADER_SIZE = 20;

    public static final int FLAG_FIN = 0x001;
    public static final int FLAG_SYN = 0x002;
    public static final int FLAG_RST = 0x004;
    public static final int FLAG_PSH = 0x008;
    public static final int FLAG_ACK = 0x010;

    private static final int MAX_SEGMENT = 1460;

    private final TIPv4 ipv4;
    private final Queue<Segment> rxQueue;
    private final Queue<Connection> connections;
    private final Queue<Listener> listeners;

    private int nextEphemeralPort = 49152;
    private int nextSequence = 1;

    public TTCP(TIPv4 ipv4) {
        if (ipv4 == null) {
            throw new IllegalArgumentException("IPv4 no puede ser null");
        }

        this.ipv4 = ipv4;
        this.rxQueue = new ArrayDeque<>();
        this.connections = new ArrayDeque<>();
        this.listeners = new ArrayDeque<>();
    }

    public TIPv4 getIPv4() {
        return ipv4;
    }

    /*
     * Procesa los paquetes IPv4 disponibles.
     *
     * Debe llamarse desde el servicio de red del kernel.
     */
    public void receive() {
        TIPv4.Packet packet;

        while ((packet = ipv4.receive()) != null) {
            if (packet.getProtocol() != TIPv4.PROTOCOL_TCP) {
                continue;
            }

            Segment segment = decode(packet.getSourceIP(), packet.getDestinationIP(), packet.getPayload());
            if (segment == null) {
                continue;
            }

            process(segment);
        }
    }

    // Abre una conexión TCP como cliente.
    public Connection connect(byte[] destinationIP, int destinationPort) {
        checkIP(destinationIP);
        checkPort(destinationPort);

        int localPort = allocateEphemeralPort();

        Connection connection = new Connection(this, ipv4.getLocalIP(), localPort, destinationIP, destinationPort, true);
        connections.add(connection);

        int sequence = nextSequence();

        connection.sendSequence = sequence;
        connection.state = State.SYN_SENT;

        sendSegment(connection, sequence, 0, FLAG_SYN, new byte[0]);

        return connection;
    }

    // Crea un socket servidor.
    public Listener listen(int port) {
        checkPort(port);
        for (Listener listener : listeners) {
            if (listener.port == port) {
                throw new IllegalStateException("Puerto TCP ya está escuchando: " + port);
            }
        }

        Listener listener = new Listener(this, port);
        listeners.add(listener);

        return listener;
    }

    private Listener findListener(int port) {
        for (Listener listener : listeners) {
            if (listener.port == port) {
                return listener;
            }
        }

        return null;
    }

    private void process(Segment segment) {
        Connection connection = findConnection(segment);
        if (connection == null) {
            /*
             * SYN inicial para una conexión
             * que todavía no existe.
             */
            if ((segment.flags & FLAG_SYN) != 0 && (segment.flags & FLAG_ACK) == 0) {
                Listener listener = findListener(segment.destinationPort);

                if (listener == null) {
                    return;
                }

                connection = new Connection(this, ipv4.getLocalIP(), segment.destinationPort, segment.sourceIP, segment.sourcePort, false);
                connection.receiveSequence = segment.sequence + 1;
                connection.sendSequence = nextSequence();
                connection.state = State.SYN_RECEIVED;

                connections.add(connection);

                listener.pending.add(connection);

                sendSegment(connection, connection.sendSequence, connection.receiveSequence, FLAG_SYN | FLAG_ACK, new byte[0]);

                return;
            }
            return;
        }

        // RST
        if ((segment.flags & FLAG_RST) != 0) {
            connection.state = State.CLOSED;
            return;
        }

        // SYN-ACK recibido por cliente.
        if (connection.state == State.SYN_SENT && (segment.flags & FLAG_SYN) != 0 && (segment.flags & FLAG_ACK) != 0) {
            connection.receiveSequence = segment.sequence + 1;
            connection.sendSequence = segment.acknowledgment;
            connection.state = State.ESTABLISHED;

            sendSegment(connection, connection.sendSequence, connection.receiveSequence, FLAG_ACK, new byte[0]);
            return;
        }

        // ACK final del servidor.
        if (connection.state == State.SYN_RECEIVED && (segment.flags & FLAG_ACK) != 0) {
            connection.state = State.ESTABLISHED;
        }

        // Datos recibidos.
        if (segment.payload.length > 0) {
            if (segment.sequence == connection.receiveSequence) {
                connection.receiveSequence += segment.payload.length;
                connection.rxQueue.add(Arrays.copyOf(segment.payload, segment.payload.length));
                sendSegment(connection, connection.sendSequence, connection.receiveSequence, FLAG_ACK, new byte[0]);
            }
            return;
        }

        // FIN.
        if ((segment.flags & FLAG_FIN) != 0) {
            connection.receiveSequence = segment.sequence + 1;
            sendSegment(connection, connection.sendSequence, connection.receiveSequence, FLAG_ACK, new byte[0]);
            connection.state = State.CLOSE_WAIT;
        }
    }

    private Connection findConnection(Segment segment) {
        for (Connection connection : connections) {
            if (connection.localPort != segment.destinationPort) {
                continue;
            }

            if (connection.remotePort != segment.sourcePort) {
                continue;
            }

            if (!Arrays.equals(connection.remoteIP, segment.sourceIP)) {
                continue;
            }

            return connection;
        }
        return null;
    }

    private void sendSegment(Connection connection, int sequence, int acknowledgment, int flags, byte[] payload) {
        if (payload == null) {
            payload = new byte[0];
        }

        if (payload.length > MAX_SEGMENT) {
            throw new IllegalArgumentException("Segmento TCP demasiado grande");
        }

        byte[] segment = encode(connection.localPort, connection.remotePort, sequence, acknowledgment, flags, payload, connection.remoteIP);

        ipv4.send(connection.remoteIP, TIPv4.PROTOCOL_TCP, segment);
        int consumed = payload.length;

        if ((flags & FLAG_SYN) != 0) {
            consumed++;
        }

        if ((flags & FLAG_FIN) != 0) {
            consumed++;
        }

        connection.sendSequence = sequence + consumed;
    }

    private byte[] encode(int sourcePort, int destinationPort, int sequence, int acknowledgment, int flags, byte[] payload, byte[] destinationIP) {
        int length = HEADER_SIZE + payload.length;
        byte[] segment = new byte[length];

        write16(segment, 0, sourcePort);
        write16(segment, 2, destinationPort);
        write32(segment, 4, sequence);
        write32(segment, 8, acknowledgment);

        // Data offset = 5, Reserved = 0
        segment[12] = (byte) (5 << 4);
        write16(segment, 12, ((5 << 12) | flags));
        write16(segment, 14, 65535);

        // Checksum inicialmente cero.
        write16(segment, 16, 0);

        // Urgent pointer.
        write16(segment, 18, 0);

        System.arraycopy(payload, 0, segment, HEADER_SIZE, payload.length);
        int checksum = checksum(ipv4.getLocalIP(), destinationIP, segment);

        write16(segment, 16, checksum);

        return segment;
    }

    private Segment decode(byte[] sourceIP, byte[] destinationIP, byte[] data) {
        if (data == null || data.length < HEADER_SIZE) {
            return null;
        }

        int sourcePort = read16(data, 0);
        int destinationPort = read16(data, 2);
        int sequence = read32(data, 4);
        int acknowledgment = read32(data, 8);

        int dataOffset = ((data[12] >>> 4) & 0x0F) * 4;
        if (dataOffset < HEADER_SIZE || dataOffset > data.length) {
            return null;
        }

        int flags = read16(data, 12) & 0x01FF;
        int window = read16(data, 14);
        int checksum = read16(data, 16);
        int urgent = read16(data, 18);

        // Validación checksum TCP.
        if (checksum(sourceIP, destinationIP, data) != 0) {
            return null;
        }

        byte[] payload = Arrays.copyOfRange(data, dataOffset, data.length);

        return new Segment(sourceIP, destinationIP, sourcePort, destinationPort, sequence, acknowledgment, flags, window, urgent, payload);
    }

    private static int checksum(byte[] sourceIP, byte[] destinationIP, byte[] tcp) {
        long sum = 0;

        sum += word(sourceIP, 0);
        sum += word(sourceIP, 2);

        sum += word(destinationIP, 0);
        sum += word(destinationIP, 2);

        sum += TIPv4.PROTOCOL_TCP;
        sum += tcp.length;

        for (int i = 0; i < tcp.length; i += 2) {
            int high = tcp[i] & 0xFF;
            int low = i + 1 < tcp.length ? tcp[i + 1] & 0xFF : 0;

            sum += (high << 8) | low;

            while ((sum >>> 16) != 0) {
                sum = (sum & 0xFFFF) + (sum >>> 16);
            }
        }

        return (int) (~sum) & 0xFFFF;
    }

    private static int word(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static int read16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static int read32(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private static void write16(byte[] data, int offset, int value) {
        data[offset] = (byte) ((value >>> 8) & 0xFF);
        data[offset + 1] = (byte) (value & 0xFF);
    }

    private static void write32(byte[] data, int offset, int value) {
        data[offset] = (byte) ((value >>> 24) & 0xFF);
        data[offset + 1] = (byte) ((value >>> 16) & 0xFF);
        data[offset + 2] = (byte) ((value >>> 8) & 0xFF);
        data[offset + 3] = (byte) (value & 0xFF);
    }

    private int allocateEphemeralPort() {
        int port = nextEphemeralPort++;

        if (nextEphemeralPort > 65535) {
            nextEphemeralPort = 49152;
        }
        return port;
    }

    private int nextSequence() {
        int value = nextSequence;
        nextSequence += 1000;

        if (nextSequence <= 0) {
            nextSequence = 1;
        }

        return value;
    }

    private static void checkIP(byte[] ip) {
        if (ip == null || ip.length != 4) {
            throw new IllegalArgumentException("IPv4 inválida");
        }
    }

    private static void checkPort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Puerto inválido: " + port);
        }
    }

    private enum State {
        CLOSED,
        SYN_SENT,
        SYN_RECEIVED,
        ESTABLISHED,
        CLOSE_WAIT
    }

    private static final class Segment {

        private final byte[] sourceIP;
        private final byte[] destinationIP;

        private final int sourcePort;
        private final int destinationPort;

        private final int sequence;
        private final int acknowledgment;

        private final int flags;
        private final int window;
        private final int urgent;

        private final byte[] payload;

        private Segment(byte[] sourceIP, byte[] destinationIP, int sourcePort, int destinationPort, int sequence, int acknowledgment, int flags, int window, int urgent, byte[] payload) {
            this.sourceIP = Arrays.copyOf(sourceIP, 4);
            this.destinationIP = Arrays.copyOf(destinationIP, 4);

            this.sourcePort = sourcePort;
            this.destinationPort = destinationPort;

            this.sequence = sequence;
            this.acknowledgment = acknowledgment;

            this.flags = flags;
            this.window = window;
            this.urgent = urgent;

            this.payload = Arrays.copyOf(payload, payload.length);
        }
    }

    public static final class Connection {

        private final TTCP tcp;

        private final byte[] localIP;
        private final byte[] remoteIP;

        private final int localPort;
        private final int remotePort;

        private int sendSequence;
        private int receiveSequence;

        private State state;
        private final Queue<byte[]> rxQueue = new ArrayDeque<>();

        private Connection(TTCP tcp, byte[] localIP, int localPort, byte[] remoteIP, int remotePort, boolean client) {
            this.tcp = tcp;
            this.localIP = Arrays.copyOf(localIP, 4);
            this.remoteIP = Arrays.copyOf(remoteIP, 4);
            this.localPort = localPort;
            this.remotePort = remotePort;
            this.state = client ? State.CLOSED : State.SYN_RECEIVED;
        }

        public boolean isEstablished() {
            return state == State.ESTABLISHED;
        }

        public boolean isClosed() {
            return state == State.CLOSED;
        }

        public byte[] getRemoteIP() {
            return Arrays.copyOf(remoteIP, 4);
        }

        public int getLocalPort() {
            return localPort;
        }

        public int getRemotePort() {
            return remotePort;
        }

        public void send(byte[] data) {
            if (!isEstablished()) {
                throw new IllegalStateException("Conexión TCP no establecida");
            }

            if (data == null) {
                data = new byte[0];
            }

            int offset = 0;
            while (offset < data.length) {
                int length = Math.min(MAX_SEGMENT, data.length - offset);
                byte[] payload = Arrays.copyOfRange(data, offset, offset + length);
                tcp.sendSegment(this, sendSequence, receiveSequence, FLAG_PSH | FLAG_ACK, payload);
                offset += length;
            }
        }

        public byte[] receive() {
            return rxQueue.poll();
        }

        public boolean hasData() {
            return !rxQueue.isEmpty();
        }

        public void close() {
            if (state == State.CLOSED) {
                return;
            }

            tcp.sendSegment(this, sendSequence, receiveSequence, FLAG_FIN | FLAG_ACK, new byte[0]);
            state = State.CLOSED;
        }
    }

    public static final class Listener {

        private final TTCP tcp;
        private final int port;
        private final Queue<Connection> pending = new ArrayDeque<>();

        private Listener(TTCP tcp, int port) {
            this.tcp = tcp;
            this.port = port;
        }

        public int getPort() {
            return port;
        }

        public Connection accept() {
            Connection connection = pending.peek();
            if (connection == null) {
                return null;
            }

            if (connection.state != State.ESTABLISHED) {
                return null;
            }
            return pending.poll();
        }
    }
}

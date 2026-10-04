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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class TNetworkHostBackend implements TNetworkBackend {

    private static final int MAX_FRAME = 1518;

    private final String helper;
    private final String interfaceName;

    private Process process;
    private InputStream input;
    private OutputStream output;

    private Thread rxThread;

    private final ConcurrentLinkedQueue<byte[]> rxQueue = new ConcurrentLinkedQueue<>();

    private volatile boolean opened;

    private byte[] mac;

    public TNetworkHostBackend(String helper, String interfaceName) {
        if (helper == null || helper.isEmpty()) {
            throw new IllegalArgumentException("Helper inválido");
        }

        if (interfaceName == null || interfaceName.isEmpty()) {
            throw new IllegalArgumentException("Interfaz inválida");
        }

        this.helper = helper;
        this.interfaceName = interfaceName;

        try {
            NetworkInterface ni = NetworkInterface.getByName(interfaceName);

            if (ni == null) {
                throw new IllegalArgumentException("Interfaz no encontrada: " + interfaceName);
            }

            byte[] hardwareAddress = ni.getHardwareAddress();
            if (hardwareAddress == null || hardwareAddress.length != 6) {
                throw new IllegalArgumentException("MAC inválida para " + interfaceName);
            }

            this.mac = Arrays.copyOf(hardwareAddress, hardwareAddress.length);
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo obtener la MAC de " + interfaceName, e);
        }
    }

    @Override
    public synchronized void open() {
        if (opened) {
            return;
        }

        try {
            process = new ProcessBuilder(helper, interfaceName)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();

            input = new BufferedInputStream(process.getInputStream());

            output = new BufferedOutputStream(process.getOutputStream());
            opened = true;

            rxThread = new Thread(this::receiveLoop, "TNetworkHostBackend-RX");

            rxThread.setDaemon(true);
            rxThread.start();
        } catch (IOException e) {
            close();
            throw new IllegalStateException("No se pudo abrir backend de red", e);
        }
    }

    @Override
    public synchronized void close() {
        opened = false;

        if (rxThread != null) {
            rxThread.interrupt();
            rxThread = null;
        }

        if (process != null) {
            process.destroy();
            process = null;
        }

        input = null;
        output = null;
        rxQueue.clear();
    }

    @Override
    public synchronized void writeFrame(byte[] frame) {
        if (!opened) {
            throw new IllegalStateException("El backend no está abierto");
        }

        if (frame == null || frame.length < 14 || frame.length > MAX_FRAME) {
            throw new IllegalArgumentException("Frame inválido");
        }

        try {
            output.write((frame.length >>> 8) & 0xFF);
            output.write(frame.length & 0xFF);
            output.write(frame);
            output.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Error enviando frame", e);
        }
    }

    @Override
    public byte[] readFrame() {
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
        if (mac == null) {
            return null;
        }

        return Arrays.copyOf(mac, mac.length);
    }

    private void receiveLoop() {
        try {
            while (opened) {
                int high = input.read();
                if (high < 0) {
                    break;
                }

                int low = input.read();
                if (low < 0) {
                    break;
                }

                int length = (high << 8) | low;
                if (length < 14 || length > MAX_FRAME) {
                    throw new IOException("Longitud de frame inválida: " + length);
                }

                byte[] frame = input.readNBytes(length);
                if (frame.length != length) {
                    throw new IOException("Frame incompleto");
                }

                rxQueue.add(frame);
            }
        } catch (IOException e) {
            if (opened) {
                System.err.println("NETWORK RX ERROR: " + e.getMessage());
            }
        } finally {
            opened = false;
        }
    }
}

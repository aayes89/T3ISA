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
package t3isa.HARDWARE;

import t3isa.Core.TWord;

/**
 *
 * @author Slam
 */
public final class TDeviceBus {

    private final TDevice[] devices;

    /**
     * Crea un bus con la cantidad indicada de dispositivos.
     *
     * @param deviceCount cantidad máxima de dispositivos
     */
    public TDeviceBus(int deviceCount) {
        if (deviceCount <= 0) {
            throw new IllegalArgumentException("deviceCount debe ser mayor que cero");
        }

        devices = new TDevice[deviceCount];
    }

    /**
     * Registra un dispositivo en un puerto.
     *
     * @param port puerto del dispositivo
     * @param device dispositivo
     */
    public void attach(int port, TDevice device) {
        checkPort(port);

        if (device == null) {
            throw new IllegalArgumentException("El dispositivo no puede ser null");
        }

        devices[port] = device;
    }

    /**
     * Desconecta el dispositivo de un puerto.
     *
     * @param port puerto
     */
    public void detach(int port) {
        checkPort(port);
        devices[port] = null;
    }

    /**
     * Obtiene el dispositivo conectado a un puerto.
     *
     * @param port puerto
     * @return dispositivo o null
     */
    public TDevice get(int port) {
        checkPort(port);
        return devices[port];
    }

    /**
     * Indica si existe un dispositivo en el puerto.
     *
     * @param port puerto
     * @return true si existe
     */
    public boolean hasDevice(int port) {
        checkPort(port);
        return devices[port] != null;
    }

    /**
     * Escribe en un dispositivo.
     *
     * @param port puerto
     * @param value palabra ternaria
     */
    public void write(int port, TWord value) {
        TDevice device = requireDevice(port);
        device.write(value);
    }

    /**
     * Lee de un dispositivo.
     *
     * @param port puerto
     * @return palabra ternaria
     */
    public TWord read(int port) {
        TDevice device = requireDevice(port);
        TWord value = device.read();
        if (value == null) {
            return TWord.zero();
        }
        return value;
    }

    /**
     * Cantidad de puertos disponibles.
     *
     * @return cantidad de puertos
     */
    public int size() {
        return devices.length;
    }

    private TDevice requireDevice(int port) {
        checkPort(port);
        TDevice device = devices[port];
        if (device == null) {
            throw new IllegalStateException("No hay dispositivo conectado en el puerto: " + port);
        }

        return device;
    }

    private void checkPort(int port) {
        if (port < 0 || port >= devices.length) {
            throw new IllegalArgumentException("Puerto de dispositivo inválido: " + port);
        }
    }

    public boolean hasInput(int port) {
        TDevice device = get(port);

        if (device == null) {
            throw new IllegalStateException("No hay dispositivo conectado en el puerto: " + port);
        }

        return device.hasInput();
    }
}

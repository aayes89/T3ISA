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

/**
 *
 * @author Slam
 */
import t3isa.CORE.TWord;

public final class TMMIOBus {

    private static final class Mapping {

        final int base;
        final int size;
        final TMMIODevice device;

        Mapping(int base, int size, TMMIODevice device) {
            this.base = base;
            this.size = size;
            this.device = device;
        }

        boolean contains(int address) {
            return address >= base && address < base + size;
        }
    }

    private final Mapping[] mappings;
    private int count;

    public TMMIOBus(int mappingCount) {
        if (mappingCount <= 0) {
            throw new IllegalArgumentException("mappingCount debe ser mayor que cero");
        }

        mappings = new Mapping[mappingCount];
    }

    public void map(int base, int size, TMMIODevice device) {
        if (base < 0) {
            throw new IllegalArgumentException("Base MMIO inválida");
        }

        if (size <= 0) {
            throw new IllegalArgumentException("Tamaño MMIO inválido");
        }

        if (device == null) {
            throw new IllegalArgumentException("Device no puede ser null");
        }

        if (count >= mappings.length) {
            throw new IllegalStateException("Bus MMIO lleno");
        }

        int end = base + size;

        if (end < base) {
            throw new IllegalArgumentException("Rango MMIO inválido");
        }

        for (int i = 0; i < count; i++) {
            Mapping mapping = mappings[i];

            int mappingEnd = mapping.base + mapping.size;

            if (base < mappingEnd && end > mapping.base) {
                throw new IllegalArgumentException("Rango MMIO solapado");
            }
        }

        mappings[count++] = new Mapping(base, size, device);
    }

    public boolean contains(int address) {
        return find(address) != null;
    }

    public TWord read(int address) {
        Mapping mapping = find(address);

        if (mapping == null) {
            throw new IllegalArgumentException("Dirección MMIO inválida: " + address);
        }

        return mapping.device.read(address - mapping.base);
    }

    public void write(int address, TWord value) {
        Mapping mapping = find(address);

        if (mapping == null) {
            throw new IllegalArgumentException("Dirección MMIO inválida: " + address);
        }

        mapping.device.write(address - mapping.base, value);
    }

    private Mapping find(int address) {
        for (int i = 0; i < count; i++) {
            if (mappings[i].contains(address)) {
                return mappings[i];
            }
        }

        return null;
    }
}

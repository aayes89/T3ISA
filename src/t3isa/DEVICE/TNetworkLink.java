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
public final class TNetworkLink {

    private final TNetworkDevice a;
    private final TNetworkDevice b;

    public TNetworkLink(TNetworkDevice a, TNetworkDevice b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Los dispositivos no pueden ser null");
        }

        if (a == b) {
            throw new IllegalArgumentException("Los dispositivos deben ser diferentes");
        }

        this.a = a;
        this.b = b;
    }

    public void transmitA() {
        byte[] frame = a.getTransmittedFrame();

        if (frame != null) {
            b.injectFrame(frame);
        }
    }

    public void transmitB() {
        byte[] frame = b.getTransmittedFrame();

        if (frame != null) {
            a.injectFrame(frame);
        }
    }

    public void pump() {
        transmitA();
        transmitB();
    }
}

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
package test;

/**
 *
 * @author Slam
 */
import t3isa.CORE.TCPU;
import t3isa.CORE.TWord;
import t3isa.HOST.TNetworkLinkBackend;
import t3isa.ISA.TAssemblerText;
import t3isa.ISA.TSyscall;

import t3os.KERNEL.TKernel;

public final class TNetworkSyscallTest {

    private TNetworkSyscallTest() {
    }

    public static void run() {
        TNetworkLinkBackend backendA = new TNetworkLinkBackend();
        TNetworkLinkBackend backendB = new TNetworkLinkBackend();

        backendA.connect(backendB);
        backendB.connect(backendA);

        TCPU cpuA = new TCPU();
        TCPU cpuB = new TCPU();

        TKernel kernelA = new TKernel(cpuA, 10, backendA, new byte[]{
            (byte) 192, (byte) 168, 1, 100
        });
        TKernel kernelB = new TKernel(cpuB, 10, backendB, new byte[]{
            (byte) 192, (byte) 168, 1, 101
        });

        cpuA.setKernel(kernelA);
        cpuB.setKernel(kernelB);

        /*
         * -------------------------------------------------
         * FRAME
         * -------------------------------------------------
         */
        int frameAddress = 1200;

        byte[] frame = {
            0x02, 0x54, 0x33, 0x00, 0x00, 0x02,
            0x02, 0x54, 0x33, 0x00, 0x00, 0x01,
            0x08, 0x00,
            'H', 'o', 'l', 'a'
        };

        for (int i = 0; i < frame.length; i++) {
            cpuA.writeMemory(frameAddress + i, TWord.fromLong(frame[i] & 0xFF));
        }

        /*
         * -------------------------------------------------
         * NET_SEND
         * -------------------------------------------------
         */
        int programAddress = 500;
        cpuA.loadProgram(programAddress, TAssemblerText.assemble("SYS\n"));

        cpuA.setRegister(1, TWord.fromLong(TSyscall.NET_SEND));
        cpuA.setRegister(2, TWord.fromLong(frameAddress));
        cpuA.setRegister(3, TWord.fromLong(frame.length));

        cpuA.setPC(programAddress);

        cpuA.step();

        int sent = (int) cpuA.getRegister(7).toLong();

        if (sent != frame.length) {
            throw new IllegalStateException("NET_SEND falló. Retornó: " + sent);
        }

        System.out.println("NET_SEND OK -> " + sent + " bytes");
        /*
         * -------------------------------------------------
         * NET_STATUS
         * -------------------------------------------------
         */
        cpuB.loadProgram(programAddress, TAssemblerText.assemble("SYS\n"));

        /*
        * -------------------------------------------------
        * PROCESAR RX DEL KERNEL
        * -------------------------------------------------
         */
        System.out.println("backendB hasFrame = " + backendB.hasFrame());

        kernelB.step();

        System.out.println("NET RX disponible = " + kernelB.hasNetworkFrame());

        /*
        * -------------------------------------------------
        * NET_STATUS
        * -------------------------------------------------
         */
        cpuB.setRegister(1, TWord.fromLong(TSyscall.NET_STATUS));
        cpuB.setPC(programAddress);
        cpuB.step();

        int status = (int) cpuB.getRegister(7).toLong();
        if (status != 1) {
            throw new IllegalStateException("NET_STATUS falló. Retornó: " + status);
        }

        System.out.println("NET_STATUS OK -> paquete disponible");

        /*
         * -------------------------------------------------
         * NET_RECV
         * -------------------------------------------------
         */
        int receiveAddress = 1400;

        cpuB.setRegister(1, TWord.fromLong(TSyscall.NET_RECV));
        cpuB.setRegister(2, TWord.fromLong(receiveAddress));
        cpuB.setRegister(3, TWord.fromLong(1518));

        cpuB.setPC(programAddress);

        cpuB.step();

        int received = (int) cpuB.getRegister(7).toLong();
        if (received != frame.length) {
            throw new IllegalStateException("NET_RECV falló. Retornó: " + received);
        }

        for (int i = 0; i < frame.length; i++) {
            int value = (int) cpuB.readMemory(receiveAddress + i).toLong();
            if (value != (frame[i] & 0xFF)) {
                throw new IllegalStateException("NET_RECV corrupción en byte " + i);
            }
        }

        System.out.println("NET_RECV OK -> " + received + " bytes");

        /*
         * -------------------------------------------------
         * RESULTADO
         * -------------------------------------------------
         */
        System.out.println("NETWORK SYSCALL TEST OK");
    }
}

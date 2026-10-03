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
package t3isa;

/**
 *
 * @author Slam
 *
 * Punto de entrada de T3OS.
 */
public final class T3OS {

    private final TCPU cpu;
    private final TKernel kernel;
    private final TBoot boot;

    public T3OS() {
        cpu = new TCPU();
        boot = new TBoot(cpu);
        kernel = new TKernel(cpu, 10);
    }

    public void boot() {
        // Primero reset del hardware.
        boot.reset();

        // Después instalar el contenido del boot.
        boot.install();

        // El hardware arranca desde el reset vector.
        boot.resetVector();
        System.out.println("T3OS BOOT PC=" + cpu.getPC());

        // Ejecutar BOOT.
        cpu.step();
        System.out.println("T3OS KERNEL ENTRY PC=" + cpu.getPC());

        if (cpu.getPC() != TCPU.OS_START) {
            throw new IllegalStateException("BOOT no transfirió control a T3OS");
        }
    }

    public TCPU getCPU() {
        return cpu;
    }

    public TKernel getKernel() {
        return kernel;
    }

    public TBoot getBoot() {
        return boot;
    }
}

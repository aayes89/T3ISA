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
 * Boot loader de T3OS.
 *
 * Mapa:
 *
 * 0 RESET 1..6 TRAP VECTORS 7..23 BOOT 24..26 INTERRUPT VECTORS 27.. T3OS
 */
public final class TBoot {

    private final TCPU cpu;

    public TBoot(TCPU cpu) {
        if (cpu == null) {
            throw new IllegalArgumentException("CPU no puede ser null");
        }

        this.cpu = cpu;
    }

    public void install() {

        /*
         * RESET VECTOR
         *
         * La dirección 0 contiene la entrada del boot.
         */
        cpu.writeMemory(0, TWord.fromLong(TCPU.BOOT_START));

        /*
         * BOOT
         *
         * Por ahora el boot hace únicamente:
         *
         *   JMP OS_START
         *
         * Esto permite probar la transferencia real
         * de control antes de cargar un kernel binario.
         */
        TWord[] boot = TAssemblerText.assemble("JMP " + TCPU.OS_START + "\n");
        cpu.loadProgram(TCPU.BOOT_START, boot);

        // El resto del área BOOT queda en cero.
        for (int i = TCPU.BOOT_START + boot.length; i < TCPU.BOOT_START + TCPU.BOOT_SIZE; i++) {
            cpu.writeMemory(i, TWord.zero());
        }
    }

    public void reset() {
        cpu.reset();
        // El hardware arranca leyendo el reset vector.
        cpu.setPC((int) cpu.readMemory(0).toLong());
    }

    public void resetVector() {
        cpu.setPC((int) cpu.readMemory(0).toLong());
    }
}

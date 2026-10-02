/*
 * The MIT License
 *
 * Copyright 2025 Allan (Slam).
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
 * T3ISA - Ternary 3-State Instruction Set Architecture
 */
public class T3ISA {

    public static void main(String[] args) {

        // BOOT SECTOR
        String bootSource = "JMP 27";

        TWord[] boot = TAssemblerText.assemble(bootSource);

        /*
         * TRAP VECTORS
         *
         * Por ahora todos apuntan a OS_START.
         * Los handlers reales los construiremos
         * después.
         */
        TWord[] vectors = {
            TWord.fromLong(TCPU.OS_START),
            TWord.fromLong(TCPU.OS_START),
            TWord.fromLong(TCPU.OS_START),
            TWord.fromLong(TCPU.OS_START),
            TWord.fromLong(TCPU.OS_START),
            TWord.fromLong(TCPU.OS_START)
        };

        // T3OS
        String osSource = """
                MOVI R2, 1234

                MOVI R1, 6
                SYS

                MOVI R1, 7
                SYS

                HALT
                """;

        TWord[] os = TAssemblerText.assemble(osSource);

        // CPU
        TCPU cpu = new TCPU();
        TConsoleDevice console = new TConsoleDevice();
        cpu.setDevice(console);

        // Boot
        cpu.loadBootSector(boot, vectors);

        // OS
        cpu.loadProgram(os, TCPU.OS_START);

        // START
        cpu.setPC(TWord.zero());

        while (!cpu.isHalted()) {
            cpu.step();
        }

        // DEBUG
        for (int i = 0; i < 27; i++) {
            System.out.println("R" + i + " = " + cpu.getRegister(i).toLong());
        }
    }
}

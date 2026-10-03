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

        TCPU cpu = new TCPU();
        TKernel kernel = new TKernel(cpu, 100);

        TConsoleDevice console = new TConsoleDevice();
        cpu.getDeviceBus().attach(0, console);

        /*
     * PID 1:
     *
     * DEVICE_IN puerto 0
     * Si no hay entrada -> BLOCK
     * Cuando despierta:
     *   R7 = dato
     *   YIELD
     *   EXIT
         */
        TWord[] program = TAssemblerText.assemble(
                "MOVI R2, 0\n"
                + "MOVI R1, 7\n"
                + "SYS\n"
                + "MOVI R1, 11\n"
                + "SYS\n"
                + "MOVI R1, 12\n"
                + "SYS\n"
        );

        TPCB process = kernel.createProcess(program);

        kernel.getScheduler().schedule(cpu);

        int steps = 0;

        while (!cpu.isHalted() && steps < 50) {

            System.out.println(
                    "ANTES STEP="
                    + steps
                    + " PID="
                    + (kernel.getScheduler().getCurrentProcess() == null
                    ? -1
                    : kernel.getScheduler().getCurrentProcess().getPid())
                    + " PC="
                    + cpu.getPC()
                    + " R7="
                    + cpu.getRegister(7).toLong()
            );

            kernel.step();

            System.out.println(
                    "DESPUES STEP="
                    + steps
                    + " PID="
                    + (kernel.getScheduler().getCurrentProcess() == null
                    ? -1
                    : kernel.getScheduler().getCurrentProcess().getPid())
                    + " PC="
                    + cpu.getPC()
                    + " R7="
                    + cpu.getRegister(7).toLong()
            );

            /*
         * Después de que DEVICE_IN haya bloqueado
         * al proceso, inyectamos la entrada.
             */
            if (steps == 3) {
                System.out.println(">>> INYECTANDO DEVICE INPUT = 123");

                console.enqueueInput(123);
            }

            steps++;
        }

        System.out.println();
        System.out.println("===== RESULTADO =====");
        System.out.println("HALTED=" + cpu.isHalted());
        System.out.println("STEPS=" + steps);
    }
}

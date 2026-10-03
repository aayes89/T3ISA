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

        /*
     * Programa del hijo después de EXEC:
     *
     * GETPID
     * YIELD
     * EXIT
         */
        TWord[] execProgram
                = TAssemblerText.assemble(
                        "MOVI R1, 10\n"
                        + "SYS\n"
                        + "MOVI R1, 11\n"
                        + "SYS\n"
                        + "MOVI R1, 12\n"
                        + "SYS\n"
                );

        /*
     * Programa principal:
     *
     * FORK
     * YIELD
     * FORK
     * WAIT
     * WAIT
     * EXIT
     *
     * El hijo detecta R7 == 0 y hace EXEC.
         */
        String source
                = "MOVI R1, 14\n"
                + "SYS\n"
                + "CMP R7, R0\n"
                + "JZERO hijo\n"
                // PADRE
                + "MOVI R1, 11\n"
                + "SYS\n"
                + "MOVI R1, 14\n"
                + "SYS\n"
                + "CMP R7, R0\n"
                + "JZERO hijo\n"
                // PADRE espera hijo 1
                + "MOVI R1, 15\n"
                + "SYS\n"
                // PADRE espera hijo 2
                + "MOVI R1, 15\n"
                + "SYS\n"
                // PADRE termina
                + "MOVI R1, 12\n"
                + "SYS\n"
                // HIJO
                + "hijo:\n"
                + "MOVI R1, 16\n"
                + "SYS\n";

        TWord[] binary
                = TAssemblerText.assemble(source);

        /*
     * Reservamos espacio adicional para EXEC.
         */
        TWord[] complete
                = new TWord[binary.length + execProgram.length];

        System.arraycopy(
                binary,
                0,
                complete,
                0,
                binary.length
        );

        System.arraycopy(
                execProgram,
                0,
                complete,
                binary.length,
                execProgram.length
        );

        TPCB parent
                = kernel.createProcess(complete);

        kernel.getScheduler().schedule(cpu);

        boolean[] execDone
                = new boolean[32];

        int steps = 0;

        while (!cpu.isHalted() && steps < 200) {

            TPCB current
                    = kernel.getScheduler().getCurrentProcess();

            if (current != null) {

                int pid = current.getPid();

                /*
             * Todo hijo que llegue a su SYS EXEC
             * recibe la dirección del programa preparado.
                 */
                if (pid != parent.getPid()
                        && !execDone[pid]) {

                    int address
                            = current.getMemoryBase()
                            + binary.length;

                    cpu.setRegister(
                            2,
                            TWord.fromLong(address)
                    );

                    cpu.setRegister(
                            3,
                            TWord.fromLong(
                                    execProgram.length
                            )
                    );

                    execDone[pid] = true;
                }
            }

            kernel.step();

            current
                    = kernel.getScheduler().getCurrentProcess();

            if (current != null) {

                System.out.println(
                        "STEP=" + steps
                        + " PID=" + current.getPid()
                        + " PC=" + cpu.getPC()
                        + " R7=" + cpu.getRegister(7).toLong()
                        + " STATE=" + current.getState()
                );

            } else {

                System.out.println(
                        "STEP=" + steps
                        + " SIN PROCESO"
                );
            }

            steps++;
        }

        System.out.println();
        System.out.println("===== RESULTADO =====");
        System.out.println(
                "HALTED=" + cpu.isHalted()
        );
        System.out.println(
                "STEPS=" + steps
        );
    }
}

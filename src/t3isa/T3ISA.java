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

        String programA
                = "MOVI R1, 13\n"
                + "SYS\n"
                + "MOVI R1, 12\n"
                + "SYS\n";

        String programB
                = "MOVI R1, 11\n"
                + "SYS\n"
                + "MOVI R1, 12\n"
                + "SYS\n";

        String timerSource
                = "MOVI R7, 1234\n"
                + "IRET\n";

        TWord[] binaryA = TAssemblerText.assemble(programA, 1000);
        TWord[] binaryB = TAssemblerText.assemble(programB, 5000);
        TWord[] timer = TAssemblerText.assemble(timerSource);

        TWord[] syscallHandler = TAssemblerText.assemble(
                "IRET\n"
        );

        TKernel kernel = new TKernel(cpu, 10);

        TPCB process1 = kernel.loadProcess(
                binaryA,
                TCPU.USER_MEMORY_START,
                4999
        );

        TPCB process2 = kernel.loadProcess(
                binaryB,
                5000,
                9999
        );

        cpu.loadProgram(TCPU.INTERRUPT_HANDLER_TIMER, timer);
        cpu.loadProgram(TCPU.TRAP_HANDLER_SYSCALL, syscallHandler);

        System.out.println(
                "Proceso 1 creado (PID: "
                + process1.getPid() + ")"
        );

        System.out.println(
                "Proceso 2 creado (PID: "
                + process2.getPid() + ")"
        );

        for (int i = 0; i < 20; i++) {

            kernel.step();

            TPCB current
                    = kernel.getScheduler().getCurrentProcess();

            if (process1.getState() == TPCB.ProcessState.BLOCKED) {
                System.out.println(
                        "PID " + process1.getPid()
                        + " -> BLOCKED"
                );

                kernel.unblockProcess(process1);

                System.out.println(
                        "PID " + process1.getPid()
                        + " -> READY"
                );
            }

            if (current != null) {
                System.out.println(
                        "Paso " + i
                        + " -> Proceso Ejecutando PID: "
                        + current.getPid()
                        + " | PC: "
                        + cpu.getPC()
                );
            }
        }
    }
}

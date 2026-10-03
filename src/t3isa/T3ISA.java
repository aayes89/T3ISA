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

        String program
                = "MOVI R1, 14\n"
                + "SYS\n"
                + "CMP R7, R0\n"
                + "JZERO child_exit\n"
                + "MOVI R1, 15\n"
                + "SYS\n"
                + "MOVI R1, 12\n"
                + "SYS\n"
                + "child_exit:\n"
                + "MOVI R1, 12\n"
                + "SYS\n";

        TWord[] binary
                = TAssemblerText.assemble(program);

        kernel.createProcess(binary);

        for (int i = 0; i < 20; i++) {

            kernel.step();

            TPCB current
                    = kernel.getScheduler().getCurrentProcess();

            if (current != null) {

                System.out.println(
                        "Paso " + i
                        + " -> PID=" + current.getPid()
                        + " STATE=" + current.getState()
                        + " PCB_PC=" + current.getPc()
                        + " CPU_PC=" + cpu.getPC()
                        + " R7=" + cpu.getRegister(7).toLong()
                        + " SP=" + cpu.getSP()
                );

            } else {

                System.out.println(
                        "Paso " + i + " -> SIN PROCESO"
                );
            }
        }
    }
}

/* public static void main(String[] args) {

        TCPU cpu = new TCPU();
        TConsoleDevice console = new TConsoleDevice();
        cpu.getDeviceBus().attach(0, console);

        String programA
                = "MOVI R1, 7\n"
                + "MOVI R2, 0\n"
                + "SYS\n"
                + "MOVI R1, 12\n"
                + "SYS\n";

        String programB
                = "MOVI R7, 222\n"
                + "MOVI R7, 333\n"
                + "MOVI R7, 444\n"
                + "MOVI R7, 555\n"
                + "MOVI R7, 666\n"
                + "MOVI R7, 777\n"
                + "MOVI R1, 12\n"
                + "SYS\n";

        String timerSource
                = "MOVI R7, 1234\n"
                + "IRET\n";
        String programBad
                = "MOVI R1, 100\n"
                + "MOVI R2, 0\n"
                + "DIV R3, R1, R2\n"
                + "MOVI R1, 12\n"
                + "SYS\n";
        String programBadMemory
                = "MOVI R1, 1000\n"
                + "LOAD R3, R1, 0\n"
                + "MOVI R1, 12\n"
                + "SYS\n";
        String programBadStack
                = "POP R3\n"
                + "POP R3\n"
                + "MOVI R1, 12\n"
                + "SYS\n";

        TKernel kernel = new TKernel(cpu, 3);

        TWord[] binaryA = TAssemblerText.assemble(programA);
        TWord[] binaryB = TAssemblerText.assemble(programB);
        TWord[] binaryBad = TAssemblerText.assemble(programBad);
        TWord[] timer = TAssemblerText.assemble(timerSource);
        TWord[] binaryBadMemory = TAssemblerText.assemble(programBadMemory);
        TWord[] binaryBadInstruction = {TWord.fromLong(2)};
        TWord[] binaryBadStack = TAssemblerText.assemble(programBadStack);

        cpu.loadProgram(TCPU.INTERRUPT_HANDLER_TIMER, timer);
        cpu.loadProgram(TCPU.INTERRUPT_HANDLER_DEVICE, TAssemblerText.assemble("IRET\n"));
        kernel.createProcess(binaryA);
        kernel.createProcess(binaryB);
        kernel.createProcess(binaryBad);
        kernel.createProcess(binaryBadMemory);
        kernel.createProcess(binaryBadInstruction);
        kernel.createProcess(binaryBadStack);

        kernel.step(); // 0
        kernel.step(); // 1
        kernel.step(); // 2

        for (int i = 0; i < 20; i++) {

            if (i == 17) {
                System.out.println("=== INYECTANDO ENTRADA ===");
                console.enqueueInput(1234);
            }

            kernel.step();

            TPCB current = kernel.getScheduler().getCurrentProcess();

            if (current != null) {
                System.out.println(
                        "Paso " + i
                        + " -> PID: " + current.getPid()
                        + " | PCB PC: " + current.getPc()
                        + " | CPU PC: " + cpu.getPC()
                );
            }
        }
    }
}
 */

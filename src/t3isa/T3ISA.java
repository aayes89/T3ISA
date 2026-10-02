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

        String userKernelWriteTest = """
        MOVI R1, 8
        SYS

        MOVI R2, 27
        MOVI R3, 999
        STORE R2, R3, 0

        HALT
        """;

        // =================================
        // BOOT SECTOR
        // =================================
        String bootSource = """
                 MOVI R1, 2
                        SYS
                        MOVI R0, 777
                        JMP 27
                """;

        TWord[] boot = TAssemblerText.assemble(bootSource);

        // =================================
        // T3OS
        // =================================
        String osSource = """
       MOVI R1, 8
       SYS
       
       MOVI R2, 500
       LOAD R3, R2, 0
       HALT
       """;
        String memorySource = """
        MOVI R7, 999
        IRET
        """;

        TWord[] os = TAssemblerText.assemble(osSource);

        // =================================
        // TRAP HANDLERS
        // =================================
        String divZeroSource = """
                MOVI R7, 999 
                IRET
                """;

        String instructionSource = """
                MOVI R2, 2
                MOVI R1, 6
                SYS
                HALT
                """;

        String syscallSource = """
                MOVI R1, 99
                SYS
                HALT
                """;

        String deviceSource = """
                MOVI R2, 4
                MOVI R1, 6
                SYS
                HALT
                """;

        String stackSource = """ 
                             POP R1 
                             HALT 
                             """;

        TWord[] divZero = TAssemblerText.assemble(divZeroSource);
        TWord[] memory = TAssemblerText.assemble(memorySource);
        TWord[] instruction = TAssemblerText.assemble(instructionSource);
        TWord[] syscall = TAssemblerText.assemble(syscallSource);
        TWord[] device = TAssemblerText.assemble(deviceSource);
        TWord[] stack = TAssemblerText.assemble(stackSource);

        // =================================
        // CPU
        // =================================
        TCPU cpu = new TCPU();
        TConsoleDevice console = new TConsoleDevice();
        cpu.attachDevice(console);

        // =================================
        // TRAP VECTOR TABLE
        // =================================
        cpu.loadTrapVector(TTrap.DIVIDE_BY_ZERO, TCPU.TRAP_HANDLER_DIV_ZERO);
        cpu.loadTrapVector(TTrap.INVALID_MEMORY, TCPU.TRAP_HANDLER_MEMORY);
        cpu.loadTrapVector(TTrap.INVALID_INSTRUCTION, TCPU.TRAP_HANDLER_INSTRUCTION);
        cpu.loadTrapVector(TTrap.INVALID_SYSCALL, TCPU.TRAP_HANDLER_SYSCALL);
        cpu.loadTrapVector(TTrap.DEVICE_ERROR, TCPU.TRAP_HANDLER_DEVICE);
        cpu.loadTrapVector(TTrap.STACK_ERROR, TCPU.TRAP_HANDLER_STACK);

// =================================
// LOAD BOOT
// =================================
        cpu.loadProgram(TCPU.BOOT_START, boot);
        TWord[] userKernelWrite = TAssemblerText.assemble(userKernelWriteTest);

        cpu.loadProgram(TCPU.OS_START, userKernelWrite);

// =================================
// LOAD T3OS
// =================================
        //cpu.loadProgram(TCPU.OS_START, os);

// =================================
// LOAD TRAP HANDLERS
// =================================
        cpu.loadProgram(TCPU.TRAP_HANDLER_DIV_ZERO, divZero);
        cpu.loadProgram(TCPU.TRAP_HANDLER_MEMORY, memory);
        cpu.loadProgram(TCPU.TRAP_HANDLER_INSTRUCTION, instruction);
        cpu.loadProgram(TCPU.TRAP_HANDLER_SYSCALL, syscall);
        cpu.loadProgram(TCPU.TRAP_HANDLER_DEVICE, device);
        cpu.loadProgram(TCPU.TRAP_HANDLER_STACK, stack);

// =================================
// RESET / BOOT
// =================================
        cpu.setPC(TCPU.BOOT_START);

// =================================
// RUN
// =================================
        cpu.run();
        System.out.println("PC = " + cpu.getPC());
        System.out.println("SP = " + cpu.getSP());
        System.out.println("USER SP = " + cpu.getUserSP());
        System.out.println("KERNEL SP = " + cpu.getKernelSP());
        System.out.println("KERNEL MODE = " + cpu.isKernelMode());
        System.out.println("VECTOR MEMORY = " + cpu.readMemory(2).toLong());

        // =================================
        // DEBUG
        // =================================
        System.out.println();
        System.out.println("TRAP = " + cpu.getTrap());
        System.out.println();

        for (int i = 0; i < 27; i++) {
            System.out.println("R" + i + " = " + cpu.getRegister(i).toLong());
        }
    }

}

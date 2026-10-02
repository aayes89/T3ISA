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
        // 1. Instanciar la CPU
        TCPU cpu = new TCPU();

        // 2. Definir dos programas de prueba sencillos en ensamblador textual T3ISA
        // Programa A: Incrementa R0 en un bucle infinito
        String programA
                = "inicio:\n"
                + "MOVI R0, 100\n"
                + "MOVI R1, 11\n"
                + "SYS\n"
                + "MOVI R0, 101\n"
                + "MOVI R1, 12\n"
                + "SYS\n";

        String programB
                = "inicio:\n"
                + "MOVI R2, 200\n"
                + "MOVI R1, 11\n"
                + "SYS\n"
                + "MOVI R2, 201\n"
                + "MOVI R1, 11\n"
                + "SYS\n"
                + "MOVI R2, 202\n"
                + "JMP inicio\n";

        String timerSource
                = "MOVI R7, 1234\n"
                + "IRET\n";

        // 3. Compilar los programas a arrays de TWord usando tu TAssemblerText
        TWord[] binaryA = TAssemblerText.assemble(programA, 1000);
        TWord[] binaryB = TAssemblerText.assemble(programB, 5000);
        TWord[] timer = TAssemblerText.assemble(timerSource);

        // 4. Instanciar el Kernel con un Quántum de 10 instrucciones por proceso
        TKernel kernel = new TKernel(cpu, 10);

        // 5. Cargar ambos procesos en regiones de memoria de usuario separadas
        // Proceso 1: memoria 1000 a 4999
        TPCB process1 = kernel.loadProcess(binaryA, TCPU.USER_MEMORY_START, 4999);

        // Proceso 2: memoria 5000 a 9999
        TPCB process2 = kernel.loadProcess(binaryB, 5000, 9999);
        cpu.loadProgram(TCPU.INTERRUPT_HANDLER_TIMER, timer);

        System.out.println("Proceso 1 creado (PID: " + process1.getPid() + ")");
        System.out.println("Proceso 2 creado (PID: " + process2.getPid() + ")");

        // 6. Ejecutar por 50 ciclos para ver cómo conmuta entre los dos programas
        for (int i = 0; i < 50; i++) {
            kernel.step();

            TPCB current = kernel.getScheduler().getCurrentProcess();
            if (current != null) {
                System.out.println("Paso " + i + " -> Proceso Ejecutando PID: " + current.getPid() + " | PC: " + cpu.getPC());
            }
        }
    }
}

/*

    public static void main(String[] args) {

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
        // T3OS / USER TEST
        // =================================
        String osSource = """
               MOVI R1, 8
                SYS
                
                MOVI R1, 0
                MOVI R2, 123
                MOVI R1, 0
                SYS
                
                HALT
                """;

        TWord[] os = TAssemblerText.assemble(osSource);

        // =================================
        // TRAP HANDLERS
        // =================================
        String divZeroSource = """
                MOVI R7, 999
                IRET
                """;

        String memorySource = """
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
                MOVI R7, 999
                HALT
                """;

        String deviceSource = """
                MOVI R1, 6
                MOVI R2, 0
                MOVI R3, 123
                SYS
                HALT
                """;

        String stackSource = """
                POP R1
                HALT
                """;

        // =================================
        // TIMER INTERRUPT HANDLER
        // =================================
        String timerSource = """
                MOVI R7, 1234
                IRET
                """;

        TWord[] divZero = TAssemblerText.assemble(divZeroSource);
        TWord[] memory = TAssemblerText.assemble(memorySource);
        TWord[] instruction = TAssemblerText.assemble(instructionSource);
        TWord[] syscall = TAssemblerText.assemble(syscallSource);
        TWord[] device = TAssemblerText.assemble(deviceSource);
        TWord[] stack = TAssemblerText.assemble(stackSource);
        TWord[] timer = TAssemblerText.assemble(timerSource);

        // =================================
        // CPU
        // =================================
        TCPU cpu = new TCPU();

        TConsoleDevice console = new TConsoleDevice();
        cpu.attachDevice(0, console);

        // =================================
        // TRAP VECTOR TABLE
        // =================================
        cpu.loadTrapVector(
                TTrap.DIVIDE_BY_ZERO,
                TCPU.TRAP_HANDLER_DIV_ZERO
        );

        cpu.loadTrapVector(
                TTrap.INVALID_MEMORY,
                TCPU.TRAP_HANDLER_MEMORY
        );

        cpu.loadTrapVector(
                TTrap.INVALID_INSTRUCTION,
                TCPU.TRAP_HANDLER_INSTRUCTION
        );

        cpu.loadTrapVector(
                TTrap.INVALID_SYSCALL,
                TCPU.TRAP_HANDLER_SYSCALL
        );

        cpu.loadTrapVector(
                TTrap.DEVICE_ERROR,
                TCPU.TRAP_HANDLER_DEVICE
        );

        cpu.loadTrapVector(
                TTrap.STACK_ERROR,
                TCPU.TRAP_HANDLER_STACK
        );

        // =================================
        // INTERRUPT VECTOR TABLE
        // =================================
        cpu.loadInterruptVector(
                TInterrupt.TIMER,
                TCPU.INTERRUPT_HANDLER_TIMER
        );

        // =================================
        // LOAD BOOT
        // =================================
        cpu.loadProgram(
                TCPU.BOOT_START,
                boot
        );

        // =================================
        // LOAD T3OS
        // =================================
        cpu.loadProgram(
                TCPU.OS_START,
                os
        );
        System.out.println("OS MEMORY:");

        for (int i = 27; i <= 31; i++) {
            System.out.println(
                    i + " = " + cpu.readMemory(i).toLong()
            );
        }

        // =================================
        // LOAD TRAP HANDLERS
        // =================================
        cpu.loadProgram(
                TCPU.TRAP_HANDLER_DIV_ZERO,
                divZero
        );

        cpu.loadProgram(
                TCPU.TRAP_HANDLER_MEMORY,
                memory
        );

        cpu.loadProgram(
                TCPU.TRAP_HANDLER_INSTRUCTION,
                instruction
        );

        cpu.loadProgram(
                TCPU.TRAP_HANDLER_SYSCALL,
                syscall
        );

        cpu.loadProgram(
                TCPU.TRAP_HANDLER_DEVICE,
                device
        );

        cpu.loadProgram(
                TCPU.TRAP_HANDLER_STACK,
                stack
        );

        // =================================
        // LOAD TIMER HANDLER
        // =================================
        cpu.loadProgram(
                TCPU.INTERRUPT_HANDLER_TIMER,
                timer
        );

        // =================================
        // RESET / BOOT
        // =================================
        cpu.setPC(TCPU.BOOT_START);

        // =================================
        // EJECUTAR BOOT
        // =================================
        //
        // 7  MOVI R1, 2
        // 8  SYS
        // 9  MOVI R0, 777
        // 10 JMP 27
        //
        // Después de estas cuatro instrucciones:
        // PC = 27
        // KERNEL MODE = true
        //
        cpu.step();
        cpu.step();
        cpu.step();
        cpu.step();

        // =================================
        // ENTRAR EN USER
        // =================================
        //
        // 27 MOVI R1, 8
        // 28 SYS
        //
        cpu.step();
        cpu.step();

        // Ahora:
        //
        // PC = 29
        // KERNEL MODE = false
        //
        // =================================
        // SOLICITAR INTERRUPCIÓN TIMER
        // =================================
        cpu.requestInterrupt(TInterrupt.TIMER);

        // =================================
        // RUN
        // =================================
        cpu.run();

        // =================================
        // DEBUG
        // =================================
        System.out.println("PC = " + cpu.getPC());
        System.out.println("SP = " + cpu.getSP());
        System.out.println("USER SP = " + cpu.getUserSP());
        System.out.println("KERNEL SP = " + cpu.getKernelSP());
        System.out.println("KERNEL MODE = " + cpu.isKernelMode());

        System.out.println(
                "TIMER VECTOR = "
                + cpu.readMemory(
                        TCPU.INTERRUPT_VECTOR_BASE
                        + TInterrupt.TIMER.code
                ).toLong()
        );

        System.out.println();

        System.out.println("TRAP = " + cpu.getTrap());
        System.out.println(
                "PENDING INTERRUPT = "
                + cpu.getPendingInterrupt()
        );

        System.out.println();

        for (int i = 0; i < 27; i++) {
            System.out.println(
                    "R" + i + " = "
                    + cpu.getRegister(i).toLong()
            );
        }
    }
     }
 */

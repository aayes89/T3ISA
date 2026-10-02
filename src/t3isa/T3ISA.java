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

        // 2. Definir dos programas de prueba sencillos
        String programA
                = "inicio:\n"
                + "MOVI R0, 123\n"
                + "MOVI R1, 2000\n"
                + "STORE R1, R0, 0\n"
                + "MOVI R1, 2000\n"
                + "LOAD R0, R1, 0\n"
                + "MOVI R1, 5000\n"
                + "JMP 5000\n";

        String programB
                = "inicio:\n"
                + "MOVI R2, 200\n"
                + "MOVI R1, 11\n"
                + "SYS\n"
                + "MOVI R1, 12\n"
                + "SYS\n";

        String timerSource
                = "MOVI R7, 1234\n"
                + "IRET\n";

        // 3. Compilar los programas
        TWord[] binaryA = TAssemblerText.assemble(programA, 1000);
        TWord[] binaryB = TAssemblerText.assemble(programB, 5000);
        TWord[] timer = TAssemblerText.assemble(timerSource);

        // 4. Instanciar el Kernel
        TKernel kernel = new TKernel(cpu, 10);

        // 5. Cargar los dos procesos en regiones separadas
        TPCB process1 = kernel.loadProcess(binaryA, TCPU.USER_MEMORY_START, 4999);
        TPCB process2 = kernel.loadProcess(binaryB, 5000, 9999);

        // Cargar el manejador de interrupción del timer
        cpu.loadProgram(TCPU.INTERRUPT_HANDLER_TIMER, timer);

        System.out.println("Proceso 1 creado (PID: " + process1.getPid() + ")");
        System.out.println("Proceso 2 creado (PID: " + process2.getPid() + ")");

        // 6. Ejecutar por 50 ciclos
        for (int i = 0; i < 50; i++) {
            kernel.step();

            TPCB current = kernel.getScheduler().getCurrentProcess();

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

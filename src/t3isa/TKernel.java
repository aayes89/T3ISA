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
 */
public class TKernel {

    private final TCPU cpu;
    private final TScheduler scheduler;
    private int timerTicks;
    private final int quantumTicks; // Cuántos pasos de CPU equivalen a 1 quántum/tic de temporizador

    public TKernel(TCPU cpu, int quantumTicks) {
        this.cpu = cpu;
        this.scheduler = new TScheduler();
        this.quantumTicks = quantumTicks;
        this.timerTicks = 0;

        // Configurar los vectores de interrupción/trap en TCPU
        setupInterruptVectors();
        setupTrapHandlers();
    }

    private void setupInterruptVectors() {
        // Cargar las direcciones de los handlers en la CPU
        cpu.loadInterruptVector(TInterrupt.TIMER, TCPU.INTERRUPT_HANDLER_TIMER);

        // Cargar traps básicos (ej. división por cero, error de memoria)
        cpu.loadTrapVector(TTrap.DIVIDE_BY_ZERO, TCPU.TRAP_HANDLER_DIV_ZERO);
        cpu.loadTrapVector(TTrap.INVALID_MEMORY, TCPU.TRAP_HANDLER_MEMORY);
    }

    /**
     * Carga un programa binario en el espacio de usuario y crea su proceso.
     *
     * @param binary
     * @param memoryBase
     * @param memoryLimit
     * @return
     */
    public TPCB loadProcess(TWord[] binary, int memoryBase, int memoryLimit) {
        // Cargar las instrucciones del programa en la memoria física a partir de memoryBase
        cpu.loadProgram(memoryBase, binary);

        // Crear el PCB con punto de entrada en memoryBase
        return scheduler.createProcess(memoryBase, memoryBase, memoryLimit);
    }

    // Avanza la CPU un ciclo de instrucción y simula el temporizador.
    public void step() {
        if (cpu.isHalted()) {
            return;
        }

        if (scheduler.getCurrentProcess() == null && scheduler.hasReadyProcesses()) {
            scheduler.schedule(cpu);
        }

        timerTicks++;

        if (timerTicks >= quantumTicks) {
            timerTicks = 0;

            TPCB current = scheduler.getCurrentProcess();

            if (current != null && !cpu.isKernelMode()) {
                current.saveContext(cpu);
            }

            cpu.requestInterrupt(TInterrupt.TIMER);
        }

        cpu.step();

        int action = cpu.getPendingProcessAction();

        System.out.println("PENDING ACTION = " + action);

        if (action == TSyscall.YIELD) {
            cpu.clearPendingProcessAction();
            scheduler.schedule(cpu);

        } else if (action == TSyscall.EXIT) {
            cpu.clearPendingProcessAction();
            scheduler.terminateCurrentProcess(cpu);

            if (scheduler.getCurrentProcess() == null) {
                cpu.halt();
                return;
            }
        }

        if (cpu.isKernelMode() && cpu.getPC() == TCPU.INTERRUPT_HANDLER_TIMER) {
            scheduler.scheduleAfterInterrupt(cpu);
        }
    }

    // Bucle principal de ejecución del sistema operativo.
    public void run() {
        while (!cpu.isHalted() && scheduler.hasReadyProcesses()) {
            step();
        }
    }

    private void setupTrapHandlers() {
        TWord[] memoryHandler = TAssemblerText.assemble(
                "MOVI R1, 1\n"
                + "IRET\n"
        );

        cpu.loadProgram(TCPU.TRAP_HANDLER_MEMORY, memoryHandler);
    }

    public TScheduler getScheduler() {
        return scheduler;
    }
}

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
    private final TMemoryManager memoryManager;
    private int timerTicks;
    private final int quantumTicks; // Cuántos pasos de CPU equivalen a 1 quántum/tic de temporizador

    public TKernel(TCPU cpu, int quantumTicks) {
        this.cpu = cpu;
        this.scheduler = new TScheduler();
        this.quantumTicks = quantumTicks;
        this.memoryManager = new TMemoryManager();
        this.timerTicks = 0;

        // Configurar los vectores de interrupción/trap en TCPU
        setupInterruptVectors();
        setupTrapHandlers();
    }

    private void setupInterruptVectors() {
        cpu.loadInterruptVector(TInterrupt.TIMER, TCPU.INTERRUPT_HANDLER_TIMER);
        cpu.loadInterruptVector(TInterrupt.DEVICE, TCPU.INTERRUPT_HANDLER_DEVICE);

        cpu.loadTrapVector(TTrap.DIVIDE_BY_ZERO, TCPU.TRAP_HANDLER_DIV_ZERO);
        cpu.loadTrapVector(TTrap.INVALID_MEMORY, TCPU.TRAP_HANDLER_MEMORY);
        cpu.loadTrapVector(TTrap.INVALID_SYSCALL, TCPU.TRAP_HANDLER_SYSCALL);
        cpu.loadTrapVector(TTrap.DEVICE_ERROR, TCPU.TRAP_HANDLER_DEVICE);
        cpu.loadTrapVector(TTrap.INVALID_INSTRUCTION, TCPU.TRAP_HANDLER_INSTRUCTION);
        cpu.loadTrapVector(TTrap.STACK_ERROR, TCPU.TRAP_HANDLER_STACK);
    }

    /**
     * Carga un programa binario en el espacio de usuario y crea su proceso.
     *
     * @param binary
     * @return
     */
    public TPCB loadProcess(TWord[] binary) {
        return createProcess(binary);
    }

    public TPCB createProcess(TWord[] binary) {
        if (binary == null || binary.length == 0) {
            throw new IllegalArgumentException("El programa está vacío");
        }

        TMemoryManager.MemoryBlock block = memoryManager.allocate(binary.length);
        TMemoryManager.MemoryBlock stack = memoryManager.allocateStack(256);

        try {
            cpu.loadProgram(block.getBase(), binary);
            return scheduler.createProcess(block.getBase(), block.getBase(), block.getLimit(), stack.getBase(), stack.getLimit());
        } catch (RuntimeException e) {
            memoryManager.free(block.getBase());
            memoryManager.freeStack(stack.getBase());
            throw e;
        }
    }

    // Avanza la CPU un ciclo de instrucción y simula el temporizador.
    public void step() {
        if (cpu.isHalted()) {
            return;
        }

        boolean deviceInterrupt = checkDeviceInterrupts();

        if (scheduler.getCurrentProcess() == null && scheduler.hasReadyProcesses()) {
            scheduler.schedule(cpu);
        }

        timerTicks++;
        boolean timerInterrupt = false;

        if (timerTicks >= quantumTicks) {
            timerTicks = 0;

            TPCB current = scheduler.getCurrentProcess();

            if (!deviceInterrupt && current != null && !cpu.isKernelMode()) {
                timerInterrupt = true;
                cpu.requestInterrupt(TInterrupt.TIMER);
            }
        }

        cpu.step();

        System.out.println("DEBUG CPU: PC=" + cpu.getPC() + " SP=" + cpu.getSP() + " KERNEL=" + cpu.isKernelMode() + " TRAP=" + cpu.getTrap());

        if (deviceInterrupt) {
            cpu.clearInterruptReturned();
            while (cpu.isKernelMode() && !cpu.isHalted()) {
                cpu.step();
            }

            if (cpu.wasInterruptReturned()) {
                TPCB current = scheduler.getCurrentProcess();

                if (current != null) {
                    current.saveContext(cpu);
                }
                cpu.clearInterruptReturned();
                scheduler.scheduleAfterInterrupt(cpu);
            }
            return;
        }

        if (timerInterrupt) {
            cpu.clearInterruptReturned();
            while (cpu.isKernelMode() && !cpu.isHalted()) {
                cpu.step();
            }

            if (cpu.wasInterruptReturned()) {
                TPCB current = scheduler.getCurrentProcess();
                if (current != null) {
                    current.saveContext(cpu);
                }

                cpu.clearInterruptReturned();
                scheduler.scheduleAfterInterrupt(cpu);
            }

            return;
        }

        int action = cpu.getPendingProcessAction();

        System.out.println("PENDING ACTION = " + action);

        switch (action) {

            case TSyscall.YIELD:
                cpu.clearPendingProcessAction();
                scheduler.schedule(cpu);
                break;

            case TSyscall.BLOCK:
                int devicePort = cpu.getPendingDevicePort();

                cpu.clearPendingProcessAction();
                cpu.clearPendingDevicePort();

                scheduler.blockCurrentProcess(cpu, devicePort);
                break;

            case TSyscall.EXIT:
                cpu.clearPendingProcessAction();

                TPCB process = scheduler.getCurrentProcess();

                if (process != null) {
                    memoryManager.free(process.getMemoryBase());
                    memoryManager.freeStack(process.getStackBase());
                }

                scheduler.terminateCurrentProcess(cpu);

                if (scheduler.getCurrentProcess() == null) {
                    cpu.halt();
                    return;
                }
                break;

            case TSyscall.FORK:
                cpu.clearPendingProcessAction();
                forkCurrentProcess();
                break;

            case TSyscall.WAIT:
                cpu.clearPendingProcessAction();
                handleWait();
                break;

            default:
                break;
        }
    }

    // Bucle principal de ejecución del sistema operativo.
    public void run() {
        while (!cpu.isHalted() && scheduler.hasReadyProcesses()) {
            step();
        }
    }

    private boolean checkDeviceInterrupts() {
        for (int port = 0; port < cpu.getDeviceBus().size(); port++) {
            if (!cpu.getDeviceBus().hasDevice(port)) {
                continue;
            }

            if (!cpu.getDeviceBus().hasInput(port)) {
                continue;
            }

            if (scheduler.hasBlockedProcesses(port)) {
                scheduler.unblockDevice(port);
                cpu.requestInterrupt(TInterrupt.DEVICE);
                return true;
            }
        }
        return false;
    }

    private void setupTrapHandlers() {

        TWord[] fatalHandler = TAssemblerText.assemble(
                "MOVI R1, 12\n"
                + "SYS\n"
        );

        cpu.loadProgram(TCPU.TRAP_HANDLER_DIV_ZERO, fatalHandler);
        cpu.loadProgram(TCPU.TRAP_HANDLER_MEMORY, fatalHandler);
        cpu.loadProgram(TCPU.TRAP_HANDLER_INSTRUCTION, fatalHandler);
        cpu.loadProgram(TCPU.TRAP_HANDLER_SYSCALL, fatalHandler);
        cpu.loadProgram(TCPU.TRAP_HANDLER_DEVICE, fatalHandler);
        cpu.loadProgram(TCPU.TRAP_HANDLER_STACK, fatalHandler);
    }

    public TScheduler getScheduler() {
        return scheduler;
    }

    private void forkCurrentProcess() {
        TPCB parent = scheduler.getCurrentProcess();

        if (parent == null) {
            cpu.clearPendingProcessAction();
            return;
        }

        int memorySize = parent.getMemoryLimit() - parent.getMemoryBase() + 1;
        int stackSize = parent.getStackLimit() - parent.getStackBase() + 1;

        TMemoryManager.MemoryBlock memoryBlock = null;
        TMemoryManager.MemoryBlock stackBlock = null;

        try {
            memoryBlock = memoryManager.allocate(memorySize);
            stackBlock = memoryManager.allocateStack(stackSize);

            cpu.copyMemoryRange(parent.getMemoryBase(), parent.getMemoryLimit(), memoryBlock.getBase());
            cpu.copyProcessStack(parent.getStackBase(), parent.getStackLimit(), stackBlock.getBase());

            TPCB child = scheduler.forkProcess(parent, memoryBlock.getBase(), memoryBlock.getLimit(), stackBlock.getBase(), stackBlock.getLimit());

            int codeOffset = cpu.getPC() - parent.getMemoryBase();
            child.setPc(memoryBlock.getBase() + codeOffset);

            int childSP = stackBlock.getBase() + (cpu.getUserSP() - parent.getStackBase());
            child.setUserSP(childSP);

            for (int i = 0; i < TCPU.REGISTER_COUNT; i++) {
                child.setRegister(i, cpu.getRegister(i));
            }

            child.setRegister(7, TWord.zero());
            cpu.setRegister(7, TWord.fromLong(child.getPid()));
        } catch (RuntimeException e) {
            if (memoryBlock != null) {
                memoryManager.free(memoryBlock.getBase());
            }

            if (stackBlock != null) {
                memoryManager.freeStack(stackBlock.getBase());
            }
            throw e;
        }
    }

    private void handleWait() {
        TPCB parent = scheduler.getCurrentProcess();
        if (parent == null) {
            return;
        }

        TPCB child = scheduler.findTerminatedChild(parent);
        if (child != null) {
            cpu.setRegister(7, TWord.fromLong(child.getPid()));

            memoryManager.free(child.getMemoryBase());
            memoryManager.freeStack(child.getStackBase());

            scheduler.removeProcess(child);

            return;
        }

        if (!scheduler.hasChild(parent)) {
            cpu.setRegister(7, TWord.fromLong(-1));
            return;
        }
        parent.setWaitingForPid(-1);
        scheduler.blockCurrentProcess(cpu);
    }
}

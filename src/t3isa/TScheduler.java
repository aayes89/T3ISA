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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 *
 * @author Slam
 */
public class TScheduler {

    private final Queue<TPCB> readyQueue;
    private final List<TPCB> processTable;
    private TPCB currentProcess;
    private int nextPid;

    public TScheduler() {
        this.readyQueue = new ArrayDeque<>();
        this.processTable = new ArrayList<>();
        this.currentProcess = null;
        this.nextPid = 1;
    }

    /**
     * Crea un nuevo proceso y lo agrega a la cola de listos.
     *
     * @param entryPoint
     * @param memoryBase
     * @param memoryLimit
     * @return
     */
    public TPCB createProcess(int entryPoint, int memoryBase, int memoryLimit) {
        TPCB pcb = new TPCB(nextPid++, entryPoint, memoryBase, memoryLimit);
        pcb.setState(TPCB.ProcessState.READY);
        processTable.add(pcb);
        readyQueue.add(pcb);
        return pcb;
    }

    /**
     * Realiza la conmutación de contexto (Context Switch) usando un algoritmo
     * Round-Robin.
     *
     * @param cpu
     * @return
     */
    public TPCB schedule(TCPU cpu) {
        // Respaldar el proceso que estaba corriendo
        if (currentProcess != null && currentProcess.getState() == TPCB.ProcessState.RUNNING) {
            currentProcess.saveContext(cpu);
            currentProcess.setState(TPCB.ProcessState.READY);
            readyQueue.add(currentProcess);
        }

        // Extraer el siguiente proceso de la cola de listos
        currentProcess = readyQueue.poll();
        
        if (currentProcess != null) {
            currentProcess.setState(TPCB.ProcessState.RUNNING);
            cpu.setCurrentPid(currentProcess.getPid());
            currentProcess.restoreContext(cpu);
        }
        return currentProcess;
    }

    /**
     * Finaliza la ejecución del proceso actual y conmuta al siguiente.
     *
     * @param cpu
     */
    public void terminateCurrentProcess(TCPU cpu) {
        if (currentProcess != null) {
            currentProcess.setState(TPCB.ProcessState.TERMINATED);
            currentProcess = null;
        }
        schedule(cpu);
    }

    /**
     * Bloquea el proceso actual (por ejemplo, en espera de E/S).
     *
     * @param cpu
     */
    public void blockCurrentProcess(TCPU cpu) {
        if (currentProcess != null) {
            currentProcess.saveContext(cpu);
            currentProcess.setState(TPCB.ProcessState.BLOCKED);
            currentProcess = null;
        }
        schedule(cpu);
    }

    /**
     * Desbloquea un proceso y lo reincorpora a la cola de listos.
     *
     * @param pcb
     */
    public void unblockProcess(TPCB pcb) {
        if (pcb != null && pcb.getState() == TPCB.ProcessState.BLOCKED) {
            pcb.setState(TPCB.ProcessState.READY);
            readyQueue.add(pcb);
        }
    }

    public TPCB scheduleAfterInterrupt(TCPU cpu) {
        if (currentProcess != null && currentProcess.getState() == TPCB.ProcessState.RUNNING) {
            currentProcess.setState(TPCB.ProcessState.READY);
            readyQueue.add(currentProcess);
        }

        currentProcess = readyQueue.poll();

        if (currentProcess != null) {
            currentProcess.setState(TPCB.ProcessState.RUNNING);
            cpu.setCurrentPid(currentProcess.getPid());
            cpu.restoreProcessContext(currentProcess.getPc(), currentProcess.getRegisters(), currentProcess.getUserSP(), currentProcess.getCompare());
        }

        return currentProcess;
    }

    public TPCB getCurrentProcess() {
        return currentProcess;
    }

    public boolean hasReadyProcesses() {
        return !readyQueue.isEmpty() || currentProcess != null;
    }
}

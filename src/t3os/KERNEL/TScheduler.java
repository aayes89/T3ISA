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
package t3os.KERNEL;

import t3isa.Core.TWord;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import t3isa.HARDWARE.TMachine;

/**
 *
 * @author Slam
 */
public class TScheduler {

    private final Queue<TPCB> readyQueue;
    private final List<TPCB> processTable;
    private final Map<Integer, Queue<TPCB>> blockedByDevice;
    private TPCB currentProcess;
    private int nextPid;

    public TScheduler() {
        this.readyQueue = new ArrayDeque<>();
        this.processTable = new ArrayList<>();
        blockedByDevice = new HashMap<>();
        this.currentProcess = null;
        this.nextPid = 1;
    }

    /**
     * Crea un nuevo proceso y lo agrega a la cola de listos.
     *
     * @param entryPoint
     * @param memoryBase
     * @param memoryLimit
     * @param stackBase
     * @param stackLimit
     * @return
     */
    public TPCB createProcess(int entryPoint, int memoryBase, int memoryLimit, int stackBase, int stackLimit) {
        TPCB pcb = new TPCB(nextPid++, entryPoint, memoryBase, memoryLimit, stackBase, stackLimit);
        pcb.setState(TPCB.ProcessState.READY);
        processTable.add(pcb);
        readyQueue.add(pcb);
        return pcb;
    }

    public TPCB createProcess(TWord[] binary, TMachine machine) {
        if (binary == null || binary.length == 0) {
            throw new IllegalArgumentException("El programa está vacío");
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Realiza la conmutación de contexto (Context Switch) usando un algoritmo
     * Round-Robin.
     *
     * @param machine
     * @return
     */
    public TPCB schedule(TMachine machine) {
        // Respaldar el proceso que estaba corriendo
        if (currentProcess != null && currentProcess.getState() == TPCB.ProcessState.RUNNING) {
            currentProcess.saveContext(machine);
            currentProcess.setState(TPCB.ProcessState.READY);
            readyQueue.add(currentProcess);
        }

        // Extraer el siguiente proceso de la cola de listos
        currentProcess = readyQueue.poll();

        if (currentProcess != null) {
            currentProcess.setState(TPCB.ProcessState.RUNNING);
            machine.setCurrentPid(currentProcess.getPid());
            currentProcess.restoreContext(machine);
        }
        return currentProcess;
    }

    /**
     * Finaliza la ejecución del proceso actual y conmuta al siguiente.
     *
     * @param machine
     * @return
     */
    public TPCB terminateCurrentProcess(TMachine machine) {
        TPCB terminated = currentProcess;

        if (terminated != null) {
            terminated.setState(TPCB.ProcessState.TERMINATED);
            TPCB parent = findParent(terminated);

            currentProcess = null;

            if (parent != null && parent.getState() == TPCB.ProcessState.BLOCKED && (parent.getWaitingForPid() == -1 || parent.getWaitingForPid() == terminated.getPid())) {
                parent.setRegister(7, TWord.fromLong(terminated.getPid()));
                parent.setWaitingForPid(terminated.getPid());
                parent.setState(TPCB.ProcessState.READY);
                readyQueue.add(parent);
            }
        }

        schedule(machine);
        return terminated;
    }

    /**
     * Bloquea el proceso actual (por ejemplo, en espera de E/S).
     *
     * @param machine
     */
    public void blockCurrentProcess(TMachine machine) {
        if (currentProcess != null) {
            currentProcess.saveContext(machine);
            currentProcess.setState(TPCB.ProcessState.BLOCKED);
            currentProcess = null;
        }

        schedule(machine);
    }

    /**
     * Bloquea el proceso actual (por ejemplo, en espera de E/S).
     *
     * @param machine
     * @param devicePort
     */
    public void blockCurrentProcess(TMachine machine, int devicePort) {
        if (currentProcess != null) {
            currentProcess.saveContext(machine);
            currentProcess.setState(TPCB.ProcessState.BLOCKED);

            blockedByDevice.computeIfAbsent(devicePort, k -> new ArrayDeque<>()).add(currentProcess);
            currentProcess = null;
        }
        schedule(machine);
    }

    public void unblockDevice(int devicePort) {
        Queue<TPCB> queue = blockedByDevice.get(devicePort);

        if (queue == null || queue.isEmpty()) {
            return;
        }

        TPCB process = queue.poll();

        process.setState(TPCB.ProcessState.READY);
        readyQueue.add(process);

        if (queue.isEmpty()) {
            blockedByDevice.remove(devicePort);
        }
    }

    public boolean hasBlockedProcesses(int devicePort) {
        Queue<TPCB> queue = blockedByDevice.get(devicePort);
        return queue != null && !queue.isEmpty();
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

    public TPCB scheduleAfterInterrupt(TMachine machine) {
        if (currentProcess != null && currentProcess.getState() == TPCB.ProcessState.RUNNING) {
            currentProcess.setState(TPCB.ProcessState.READY);
            readyQueue.add(currentProcess);
        }

        currentProcess = readyQueue.poll();

        if (currentProcess != null) {
            currentProcess.setState(TPCB.ProcessState.RUNNING);
            machine.setCurrentPid(currentProcess.getPid());
            currentProcess.restoreContext(machine);
        }

        return currentProcess;
    }

    public TPCB forkProcess(TPCB parent, int memoryBase, int memoryLimit, int stackBase, int stackLimit) {
        TPCB child = new TPCB(nextPid++, parent.getPc(), memoryBase, memoryLimit, stackBase, stackLimit);

        child.setState(TPCB.ProcessState.READY);
        child.setParentPid(parent.getPid());
        processTable.add(child);
        readyQueue.add(child);
        return child;
    }

    public boolean hasChild(TPCB parent) {
        int parentPid = parent.getPid();

        for (TPCB process : processTable) {
            if (process.getParentPid() == parentPid) {
                return true;
            }
        }

        return false;
    }

    public TPCB getCurrentProcess() {
        return currentProcess;
    }

    public boolean hasProcesses() {
        return !processTable.isEmpty();
    }

    public boolean hasReadyProcesses() {
        return !readyQueue.isEmpty() || currentProcess != null;
    }

    private TPCB findParent(TPCB child) {
        int parentPid = child.getParentPid();
        if (parentPid < 0) {
            return null;
        }

        for (TPCB process : processTable) {
            if (process.getPid() == parentPid) {
                return process;
            }
        }
        return null;
    }

    public TPCB findTerminatedChild(TPCB parent) {
        int parentPid = parent.getPid();

        for (TPCB process : processTable) {
            if (process.getParentPid() == parentPid && process.getState() == TPCB.ProcessState.TERMINATED) {
                return process;
            }
        }
        return null;
    }

    public void removeProcess(TPCB process) {
        if (process == null) {
            return;
        }
        processTable.remove(process);
    }

    public TPCB findParentOf(TPCB child) {
        return findParent(child);
    }

    public TPCB[] getProcesses() {
        return processTable.toArray(new TPCB[0]);
    }

}

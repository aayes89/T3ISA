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
public class TPCB {

    public enum ProcessState {
        NEW,
        READY,
        RUNNING,
        BLOCKED,
        TERMINATED
    }

    private final int pid;
    private ProcessState state;

    // Estado del contexto guardado según los tipos primitivos/clases reales de TCPU
    private int pc;
    private int userSP;
    private int compare;
    private final TWord[] registers; // R0 a R26

    // Límites del espacio de direcciones asignado al proceso
    private final int memoryBase;
    private final int memoryLimit;

    public TPCB(int pid, int entryPoint, int memoryBase, int memoryLimit) {
        this.pid = pid;
        this.state = ProcessState.NEW;
        this.pc = entryPoint;
        this.memoryBase = memoryBase;
        this.memoryLimit = memoryLimit;

        // La pila inicial de usuario por defecto se ubica al final de su segmento asignado
        this.userSP = memoryLimit;
        this.compare = 0;

        this.registers = new TWord[TCPU.REGISTER_COUNT];
        for (int i = 0; i < TCPU.REGISTER_COUNT; i++) {
            this.registers[i] = TWord.zero();
        }
    }

    /**
     * Guarda el contexto actual del proceso desde TCPU.
     *
     * @param cpu
     */
    public void saveContext(TCPU cpu) {
        this.pc = cpu.getPC();
        this.userSP = cpu.getUserSP();
        this.compare = cpu.getCompare();

        for (int i = 0; i < TCPU.REGISTER_COUNT; i++) {
            this.registers[i] = cpu.getRegister(i);
        }
    }

    /**
     * Restaura el contexto del proceso en TCPU.
     *
     * @param cpu
     */
    public void restoreContext(TCPU cpu) {
        cpu.setPC(this.pc);

        for (int i = 0; i < TCPU.REGISTER_COUNT; i++) {
            cpu.setRegister(i, this.registers[i]);
        }

        cpu.setUserSP(userSP);
        cpu.setCompare(compare);
        cpu.setProcessMemoryRange(memoryBase, memoryLimit);
        cpu.setKernelMode(false);
    }

    public TWord[] getRegisters() {
        return registers;
    }

    public int getPid() {
        return pid;
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public int getPc() {
        return pc;
    }

    public int getUserSP() {
        return userSP;
    }

    public int getCompare() {
        return compare;
    }

    public int getMemoryBase() {
        return memoryBase;
    }

    public int getMemoryLimit() {
        return memoryLimit;
    }
}

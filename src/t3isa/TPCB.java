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

    private int pc;
    private int userSP;
    private int compare;
    private final TWord[] registers;

    private final int memoryBase;
    private final int memoryLimit;

    private final int stackBase;
    private final int stackLimit;

    public TPCB(
            int pid,
            int entryPoint,
            int memoryBase,
            int memoryLimit,
            int stackBase,
            int stackLimit) {

        this.pid = pid;
        this.state = ProcessState.NEW;

        this.pc = entryPoint;

        this.memoryBase = memoryBase;
        this.memoryLimit = memoryLimit;

        this.stackBase = stackBase;
        this.stackLimit = stackLimit;

        this.userSP = stackLimit;
        this.compare = 0;

        this.registers = new TWord[TCPU.REGISTER_COUNT];

        for (int i = 0; i < TCPU.REGISTER_COUNT; i++) {
            this.registers[i] = TWord.zero();
        }
    }

    public void restoreContext(TCPU cpu) {
        cpu.restoreProcessContext(this.pc, this.registers, this.userSP, this.compare, this.stackBase, this.stackLimit);
        cpu.setProcessMemoryRange(this.memoryBase, this.memoryLimit);
    }

    public void setUserSP(int userSP) {
        if (userSP < stackBase || userSP > stackLimit) {
            throw new IllegalArgumentException(
                    "User SP fuera del stack del proceso: " + userSP
            );
        }

        this.userSP = userSP;
    }

    public void saveContext(TCPU cpu) {
        this.pc = cpu.isKernelMode() ? cpu.getInterruptedPC() : cpu.getPC();
        this.userSP = cpu.getUserSP();
        this.compare = cpu.getCompare();

        for (int i = 0; i < TCPU.REGISTER_COUNT; i++) {
            this.registers[i] = cpu.getRegister(i);
        }
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

    public int getStackBase() {
        return stackBase;
    }

    public int getStackLimit() {
        return stackLimit;
    }
}

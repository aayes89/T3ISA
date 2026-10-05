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
package t3isa.HARDWARE;

/**
 *
 * @author Slam
 */
import t3isa.CORE.TWord;
import t3isa.DEVICE.TDeviceBus;
import t3isa.ISA.TInterrupt;
import t3isa.ISA.TTrap;
import t3os.KERNEL.TKernel;

public interface TMachine {

    int REGISTER_COUNT = 27;
    int REGISTERS = REGISTER_COUNT;
    int MEMORY_SIZE = 19683;
    int KERNEL_MEMORY_END = 999;
    int USER_MEMORY_START = 1000;
    int KERNEL_STACK_TOP = 15999;
    int KERNEL_STACK_BOTTOM = 15000;

    int USER_STACK_TOP = 19682;
    int USER_STACK_BOTTOM = 16000;
    int TRAP_VECTOR_BASE = 1;
    int TRAP_VECTOR_COUNT = 6;
    int BOOT_START = 7;
    int BOOT_SIZE = 17;
    int INTERRUPT_VECTOR_BASE = 24;
    int INTERRUPT_VECTOR_COUNT = 3;

    int STACK_TOP = MEMORY_SIZE - 1;
    int OS_START = 27;

    int TRAP_HANDLER_DIV_ZERO = 100;
    int TRAP_HANDLER_MEMORY = 110;
    int TRAP_HANDLER_INSTRUCTION = 120;
    int TRAP_HANDLER_SYSCALL = 130;
    int TRAP_HANDLER_DEVICE = 140;
    int TRAP_HANDLER_STACK = 150;
    int INTERRUPT_HANDLER_TIMER = 160;
    int INTERRUPT_HANDLER_DEVICE = 170;
    int INTERRUPT_HANDLER_KEYBOARD = 180;

    int STACK_BOTTOM = 16000;

    void loadProgram(int address, TWord[] program);

    TWord readMemory(int address);

    void writeMemory(int address, TWord value);

    void copyProcessStack(int sourceBase, int sourceLimit, int destinationBase);

    TWord getRegister(int index);

    void setRegister(int index, TWord value);

    int getPC();

    int getSP();

    int getUserSP();

    void setCurrentPid(int pid);

    void setProcessMemoryRange(int base, int limit);

    void restoreProcessContext(
            int pc,
            TWord[] registers,
            int sp,
            int compare,
            int stackBase,
            int stackLimit
    );

    boolean isHalted();

    boolean isKernelMode();

    void step();

    void requestInterrupt(TInterrupt interrupt);

    void loadInterruptVector(TInterrupt interrupt, int address);

    void loadTrapVector(TTrap trap, int address);

    boolean wasInterruptReturned();

    void clearInterruptReturned();

    int getPendingProcessAction();

    void clearPendingProcessAction();

    int getPendingDevicePort();

    void clearPendingDevicePort();

    TDeviceBus getDeviceBus();

    Object getTrap();

    public int getInterruptedPC();

    public int getCompare();

    public void reset();

    public void setPC(int i);

    public void setKernel(TKernel kernel);

    public void halt();

    public int getCurrentPid();
}

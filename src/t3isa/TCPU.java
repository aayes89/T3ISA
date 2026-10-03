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

import t3isa.Exceptions.TMemoryException;

/**
 *
 * @author Slam
 */
/*
    * Memory map
    *
    * 0      - Boot sector
    * 1..6   - Trap vectors
    * 7..26  - Reserved
    * 27..   - T3OS / programs
    * 7..9   - interrupt vectors
    * 10..26 - reservado
    * 27...  - T3OS
    * 100    - DIVIDE_BY_ZERO
    * 110    - INVALID_MEMORY
    * 120    - INVALID_INSTRUCTION
    * 130    - INVALID_SYSCALL
    * 140    - DEVICE_ERROR
    * 150    - STACK_ERROR
    * 160    - timer interrupt
    * 170    - device interrupt
    * 180    - keyboard interrupt
    *
    * 0..999       KERNEL
    * 1000..15999  USER
    * 16000..19682 STACK
    *
    * Stack grows downward.
 */
public final class TCPU {

    public static final int REGISTER_COUNT = 27;
    public static final int REGISTERS = REGISTER_COUNT;
    public static final int MEMORY_SIZE = 19683;
    public static final int KERNEL_MEMORY_END = 999;
    public static final int USER_MEMORY_START = 1000;
    public static final int KERNEL_STACK_TOP = 15999;
    public static final int KERNEL_STACK_BOTTOM = 15000;

    public static final int USER_STACK_TOP = 19682;
    public static final int USER_STACK_BOTTOM = 16000;
    private int userSP;
    private int kernelSP;
    public static final int TRAP_VECTOR_BASE = 1;
    public static final int TRAP_VECTOR_COUNT = 6;
    public static final int BOOT_START = 7;
    public static final int BOOT_SIZE = 17;
    public static final int OS_START = 27;
    public static final int INTERRUPT_VECTOR_BASE = 24;
    public static final int INTERRUPT_VECTOR_COUNT = 3;

    public static final int INTERRUPT_HANDLER_TIMER = 160;
    public static final int INTERRUPT_HANDLER_DEVICE = 170;
    public static final int INTERRUPT_HANDLER_KEYBOARD = 180;

    public static final int TRAP_HANDLER_DIV_ZERO = 100;
    public static final int TRAP_HANDLER_MEMORY = 110;
    public static final int TRAP_HANDLER_INSTRUCTION = 120;
    public static final int TRAP_HANDLER_SYSCALL = 130;
    public static final int TRAP_HANDLER_DEVICE = 140;
    public static final int TRAP_HANDLER_STACK = 150;

    public static final int STACK_TOP = MEMORY_SIZE - 1;
    public static final int STACK_BOTTOM = 16000;

    private TInterrupt pendingInterrupt;
    private final TWord[] registers;
    private final TWord[] memory;

    private int pc;
    private int sp;
    private int currentPid;

    private boolean kernelMode;

    private TTrap trap;
    private int compare;
    private int currentMemoryBase;
    private int currentMemoryLimit;

    private boolean halted;
    private int pendingProcessAction = -1;

    //private TDevice device;   // Ya no es necesario
    private final TDeviceBus deviceBus;

    public TCPU() {
        registers = new TWord[REGISTER_COUNT];
        memory = new TWord[MEMORY_SIZE];
        currentPid = 0;
        pendingProcessAction = -1;
        reset();
        deviceBus = new TDeviceBus(16);
    }

    public void reset() {
        for (int i = 0; i < REGISTER_COUNT; i++) {
            registers[i] = TWord.zero();
        }

        for (int i = 0; i < MEMORY_SIZE; i++) {
            memory[i] = TWord.zero();
        }

        pc = BOOT_START;
        kernelSP = KERNEL_STACK_TOP;
        userSP = USER_STACK_TOP;
        sp = kernelSP;
        kernelMode = true;
        pendingInterrupt = null;
        trap = null;
        currentMemoryBase = USER_MEMORY_START;
        currentMemoryLimit = USER_MEMORY_START;
        compare = 0;
        currentPid = 0;
        pendingProcessAction = -1;
        halted = false;
    }

    public int getCurrentPid() {
        return currentPid;
    }

    public void setCurrentPid(int pid) {
        currentPid = pid;
    }

    public int getPendingProcessAction() {
        return pendingProcessAction;
    }

    public void clearPendingProcessAction() {
        pendingProcessAction = -1;
    }

    public TInterrupt getPendingInterrupt() {
        return pendingInterrupt;
    }

    public void requestInterrupt(TInterrupt interrupt) {
        if (interrupt == null) {
            throw new IllegalArgumentException("Interrupt null");
        }
        pendingInterrupt = interrupt;
    }

    public void loadInterruptVector(TInterrupt interrupt, int handlerAddress) {
        if (interrupt == null) {
            throw new IllegalArgumentException("Interrupt null");
        }

        checkAddress(handlerAddress);

        int vectorAddress = INTERRUPT_VECTOR_BASE + interrupt.code;
        memory[vectorAddress] = TWord.fromLong(handlerAddress);
    }

    public TWord getRegister(int index) {
        checkRegister(index);
        return registers[index].copy();
    }

    public void setRegister(int index, TWord value) {
        checkRegister(index);

        if (value == null) {
            throw new IllegalArgumentException("Registro no puede ser null");
        }

        registers[index] = value.copy();
    }

    public void setUserSP(int value) {
        checkAddress(value);
        userSP = value;
    }

    public void setCompare(int value) {
        compare = value;
    }

    public int getPC() {
        return pc;
    }

    public void setPC(int value) {
        checkAddress(value);
        pc = value;
    }

    public int getSP() {
        return sp;
    }

    public String getTrap() {
        return trap == null ? null : trap.toString();
    }

    public TTrap getTrapCode() {
        return trap;
    }

    public boolean isHalted() {
        return halted;
    }

    public int getCompare() {
        return compare;
    }

    public void setProcessMemoryRange(int base, int limit) {
        checkAddress(base);
        checkAddress(limit);

        if (base > limit) {
            throw new IllegalArgumentException("Rango de memoria inválido");
        }

        currentMemoryBase = base;
        currentMemoryLimit = limit;
    }

    public void attachDevice(int port, TDevice device) {
        deviceBus.attach(port, device);
    }

    public TWord readMemory(int address) {
        return memory[checkedAddress(address)].copy();
    }

    public void writeMemory(int address, TWord value) {
        if (value == null) {
            throw new IllegalArgumentException("Valor de memoria no puede ser null");
        }

        memory[checkedAddress(address)] = value.copy();
    }

    public void loadBootSector(TWord[] boot) {
        if (boot == null) {
            throw new IllegalArgumentException("Boot sector null");
        }
        loadProgram(BOOT_START, boot);
    }

    public void loadProgram(int startAddress, TWord[] program) {
        if (program == null) {
            throw new IllegalArgumentException("Programa null");
        }

        checkAddress(startAddress);

        if (program.length > MEMORY_SIZE - startAddress) {
            throw new TMemoryException("Programa fuera de memoria");
        }

        for (int i = 0; i < program.length; i++) {
            if (program[i] == null) {
                throw new IllegalArgumentException("Palabra de instrucción null en posición " + i);
            }

            memory[startAddress + i] = program[i].copy();
        }

    }

    public void loadTrapVector(TTrap trap, int handlerAddress) {
        if (trap == null) {
            throw new IllegalArgumentException("Trap null");
        }

        checkAddress(handlerAddress);

        int vectorAddress = TRAP_VECTOR_BASE + trap.code;
        memory[vectorAddress] = TWord.fromLong(handlerAddress);
    }

    public void step() {

        if (halted) {
            return;
        }

        if (pendingInterrupt != null) {
            handleInterrupt();
            return;
        }

        try {
            if (pc < 0 || pc >= MEMORY_SIZE) {
                System.out.println("INVALID PC: " + pc);
                raiseTrap(TTrap.INVALID_MEMORY);
                return;
            }

            if (!kernelMode && (pc < currentMemoryBase || pc > currentMemoryLimit)) {
                System.out.println("PROCESS MEMORY VIOLATION PC=" + pc + " RANGE=" + currentMemoryBase + ".." + currentMemoryLimit);
                raiseTrap(TTrap.INVALID_MEMORY);

                // Violación de ejecución: el proceso no puede continuar.
                pendingProcessAction = TSyscall.EXIT;
                return;
            }

            int instructionAddress = pc;
            System.out.println("FETCH PC=" + pc + " WORD=" + memory[pc].toLong());
            TInstruction instruction;

            try {
                instruction = TInstruction.decode(memory[pc]);
            } catch (IllegalStateException e) {
                System.out.println("DECODE ERROR PC=" + pc + " WORD=" + memory[pc].toLong() + " MSG=" + e.getMessage());
                raiseTrap(TTrap.INVALID_INSTRUCTION);
                return;
            }

            if (instruction == null) {
                System.out.println("DECODE NULL PC=" + pc);
                raiseTrap(TTrap.INVALID_INSTRUCTION);
                return;
            }

            System.out.println("DECODE OK PC=" + pc + " OPCODE=" + instruction.getOpcode());
            pc = instructionAddress;

            execute(instruction);
        } catch (TMemoryException e) {
            System.out.println("MEMORY EXCEPTION PC=" + pc);
            raiseTrap(TTrap.INVALID_MEMORY);
        } catch (ArithmeticException e) {
            System.out.println("ARITHMETIC EXCEPTION PC=" + pc);
            raiseTrap(TTrap.DIVIDE_BY_ZERO);
        } catch (IllegalArgumentException e) {
            System.out.println("ILLEGAL ARGUMENT PC=" + pc + " MSG=" + e.getMessage());
            raiseTrap(TTrap.INVALID_INSTRUCTION);
        }
    }

    public void run() {
        while (!halted) {
            step();
        }
    }

    private void execute(TInstruction instruction) {

        TOpcode opcode = instruction.getOpcode();

        if (opcode == null) {
            raiseTrap(TTrap.INVALID_INSTRUCTION);
            return;
        }

        switch (opcode) {

            case NOP:
                incrementPC();
                break;

            case HALT:
                halted = true;
                break;

            case MOV:
                setRegister(instruction.getDst(), getRegister(instruction.getSrc1()));
                incrementPC();
                break;

            case MOVI:
                setRegister(instruction.getDst(), TWord.fromLong(instruction.getImmediate()));
                incrementPC();
                break;

            case ADD:
                setRegister(instruction.getDst(), TALU.add(getRegister(instruction.getSrc1()), getRegister(instruction.getSrc2())));
                incrementPC();
                break;

            case SUB:
                setRegister(instruction.getDst(), TALU.subtract(getRegister(instruction.getSrc1()), getRegister(instruction.getSrc2())));
                incrementPC();
                break;

            case NEG:
                setRegister(instruction.getDst(), TALU.negate(getRegister(instruction.getSrc1())));
                incrementPC();
                break;

            case MUL:
                setRegister(instruction.getDst(), TALU.multiply(getRegister(instruction.getSrc1()), getRegister(instruction.getSrc2())));
                incrementPC();
                break;

            case DIV:
                setRegister(instruction.getDst(), TALU.divide(getRegister(instruction.getSrc1()), getRegister(instruction.getSrc2())));
                incrementPC();
                break;

            case MOD:
                setRegister(instruction.getDst(), TALU.modulo(getRegister(instruction.getSrc1()), getRegister(instruction.getSrc2())));
                incrementPC();
                break;

            case SHL:
                setRegister(instruction.getDst(), TALU.shiftLeft(getRegister(instruction.getSrc1()), instruction.getImmediate()));
                incrementPC();
                break;

            case SHR:
                setRegister(instruction.getDst(), TALU.shiftRight(getRegister(instruction.getSrc1()), instruction.getImmediate()));
                incrementPC();
                break;

            case CMP:
                compare = TALU.compare(getRegister(instruction.getSrc1()), getRegister(instruction.getSrc2()));
                incrementPC();
                break;

            case TAND:
                setRegister(instruction.getDst(), TALU.and(getRegister(instruction.getSrc1()), getRegister(instruction.getSrc2())));
                incrementPC();
                break;

            case TOR:
                setRegister(instruction.getDst(), TALU.or(getRegister(instruction.getSrc1()), getRegister(instruction.getSrc2())));
                incrementPC();
                break;

            case TXOR:
                setRegister(instruction.getDst(), TALU.xor(getRegister(instruction.getSrc1()), getRegister(instruction.getSrc2())));
                incrementPC();
                break;

            case TNOT:
                setRegister(instruction.getDst(), TALU.not(getRegister(instruction.getSrc1())));
                incrementPC();
                break;

            case JMP:
                pc = instruction.getImmediate();
                break;

            case JNEG:
                if (compare < 0) {
                    pc = instruction.getImmediate();
                } else {
                    incrementPC();
                }
                break;

            case JZERO:
                if (compare == 0) {
                    pc = instruction.getImmediate();
                } else {
                    incrementPC();
                }
                break;

            case JPOS:
                if (compare > 0) {
                    pc = instruction.getImmediate();
                } else {
                    incrementPC();
                }
                break;

            case LOAD:
                executeLoad(instruction);
                break;

            case STORE:
                executeStore(instruction);
                break;

            case PUSH:
                push(getRegister(instruction.getSrc1()));
                incrementPC();
                break;

            case POP:
                setRegister(instruction.getDst(), pop());
                incrementPC();
                break;

            case CALL:
                executeCall(instruction);
                break;

            case RET:
                pc = (int) pop().toLong();
                break;

            case IRET:
                executeIRet();
                break;

            case SYS:
                executeSys();
                break;

            default:
                raiseTrap(TTrap.INVALID_INSTRUCTION);
                break;
        }
    }

    private void executeLoad(TInstruction instruction) {
        int address = checkedUserAddress(getRegister(instruction.getSrc1()).toLong());
        if (address < 0) {
            return;
        }
        setRegister(instruction.getDst(), memory[address]);
        incrementPC();
    }

    private void executeStore(TInstruction instruction) {
        int address = checkedUserAddress(getRegister(instruction.getDst()).toLong());
        if (address < 0) {
            return;
        }
        memory[address] = getRegister(instruction.getSrc1()).copy();
        incrementPC();
    }

    private void executeCall(TInstruction instruction) {
        push(TWord.fromLong(pc + 1));
        pc = instruction.getImmediate();
    }

    private void executeIRet() {
        System.out.println("=== IRET ===");
        System.out.println("SP antes = " + sp);
        System.out.println("trap = " + trap);
        System.out.println("memory[sp+1] = " + memory[sp + 1].toLong());
        System.out.println("memory[sp+2] = " + memory[sp + 2].toLong());

        if (!kernelMode) {
            halted = true;
            return;
        }

        int trapAddress = sp + 1;
        int pcAddress = sp + 2;

        int code = (int) memory[trapAddress].toLong();

        System.out.println("IRET code = " + code);
        System.out.println("IRET PC = " + memory[pcAddress].toLong());

        if (trap != null) {

            if (code < 0 || code >= TRAP_VECTOR_COUNT) {
                halted = true;
                return;
            }

            pc = (int) memory[pcAddress].toLong();
            sp = userSP;
            kernelMode = false;
            trap = null;

            System.out.println("IRET -> USER");
            System.out.println("PC = " + pc);
            System.out.println("SP = " + sp);

            return;
        }

        if (code < 0 || code >= INTERRUPT_VECTOR_COUNT) {
            halted = true;
            return;
        }

        pc = (int) memory[pcAddress].toLong();
        sp = userSP;
        kernelMode = false;

        System.out.println("IRET INTERRUPT -> USER");
        System.out.println("PC = " + pc);
        System.out.println("SP = " + sp);
    }

    private void executeSys() {
        System.out.println(
                "SYS -> R1=" + getRegister(1).toLong()
                + " R2=" + getRegister(2).toLong()
                + " R3=" + getRegister(3).toLong()
                + " PC=" + pc
        );
        int service = (int) getRegister(1).toLong();
        switch (service) {
            case TSyscall.HALT:
                if (!kernelMode) {
                    raiseTrap(TTrap.INVALID_SYSCALL);
                    return;
                }

                halted = true;
                break;

            case TSyscall.GET_PC:
                setRegister(7, TWord.fromLong(pc));
                incrementPC();
                break;

            case TSyscall.GET_SP:
                setRegister(7, TWord.fromLong(sp));
                incrementPC();
                break;

            case TSyscall.GET_CMP:
                setRegister(7, TWord.fromLong(compare));
                incrementPC();
                break;

            case TSyscall.MEM_READ:
                if (!kernelMode) {
                    raiseTrap(TTrap.INVALID_SYSCALL);
                    return;
                }

                int readAddress = checkedAddress(getRegister(2).toLong());
                setRegister(7, memory[readAddress]);
                incrementPC();
                break;

            case TSyscall.MEM_WRITE:
                if (!kernelMode) {
                    raiseTrap(TTrap.INVALID_SYSCALL);
                    return;
                }

                int writeAddress = checkedAddress(getRegister(2).toLong());
                memory[writeAddress] = getRegister(3);
                incrementPC();
                break;

            case TSyscall.DEVICE_OUT:
                try {
                    int port = (int) getRegister(2).toLong();
                    deviceBus.write(port, getRegister(3));
                } catch (IllegalArgumentException | IllegalStateException e) {
                    raiseTrap(TTrap.DEVICE_ERROR);
                    return;
                }
                incrementPC();
                break;

            case TSyscall.DEVICE_IN:
                try {
                    int port = (int) getRegister(2).toLong();
                    setRegister(7, deviceBus.read(port));
                } catch (IllegalArgumentException | IllegalStateException e) {
                    raiseTrap(TTrap.DEVICE_ERROR);
                    return;
                }
                incrementPC();
                break;

            case TSyscall.ENTER_USER:
                if (!kernelMode) {
                    raiseTrap(TTrap.INVALID_SYSCALL);
                    return;
                }

                kernelMode = false;
                sp = userSP;
                incrementPC();
                break;

            case TSyscall.EXIT_USER:
                if (kernelMode) {
                    raiseTrap(TTrap.INVALID_SYSCALL);
                    return;
                }

                userSP = sp;
                kernelMode = true;
                sp = kernelSP;
                incrementPC();
                break;

            case TSyscall.GETPID:
                setRegister(7, TWord.fromLong(currentPid));
                incrementPC();
                break;

            case TSyscall.YIELD:
                incrementPC();
                pendingProcessAction = TSyscall.YIELD;
                break;

            case TSyscall.EXIT:
                pendingProcessAction = TSyscall.EXIT;
                break;

            case TSyscall.BLOCK:
                if (kernelMode) {
                    raiseTrap(TTrap.INVALID_SYSCALL);
                    return;
                }

                incrementPC();
                pendingProcessAction = TSyscall.BLOCK;
                break;
                
            default:
                raiseTrap(TTrap.INVALID_SYSCALL);
                break;
        }
    }

    private void push(TWord value) {

        int bottom = kernelMode
                ? KERNEL_STACK_BOTTOM
                : USER_STACK_BOTTOM;

        if (sp < bottom) {
            raiseTrap(TTrap.STACK_ERROR);
            return;
        }

        memory[sp] = value.copy();
        sp--;
    }

    private TWord pop() {

        int bottom = kernelMode
                ? KERNEL_STACK_BOTTOM
                : USER_STACK_BOTTOM;

        int top = kernelMode
                ? KERNEL_STACK_TOP
                : USER_STACK_TOP;

        if (sp >= top) {
            raiseTrap(TTrap.STACK_ERROR);
            return TWord.zero();
        }

        sp++;

        if (sp > top || sp < bottom) {
            raiseTrap(TTrap.STACK_ERROR);
            return TWord.zero();
        }

        return memory[sp].copy();
    }

    private void raiseTrap(TTrap cause) {

        if (cause == null) {
            halted = true;
            return;
        }

        if (trap != null) {
            halted = true;
            return;
        }

        trap = cause;

        if (!kernelMode) {
            userSP = sp;
            kernelMode = true;
            kernelSP = KERNEL_STACK_TOP;
            sp = kernelSP;
        }

        if (sp < KERNEL_STACK_BOTTOM + 2) {
            halted = true;
            return;
        }

        // Frame:
        // [sp]  = PC de retorno
        // [sp]  = código del trap
        memory[sp] = TWord.fromLong(pc);
        sp--;

        memory[sp] = TWord.fromLong(cause.code);
        sp--;

        int vectorAddress = TRAP_VECTOR_BASE + cause.code;

        int handler = checkedAddress(
                memory[vectorAddress].toLong()
        );

        pc = handler;
    }

    private void switchToKernelStack() {
        if (!kernelMode) {
            userSP = sp;
            kernelMode = true;
            kernelSP = KERNEL_STACK_TOP;
            sp = kernelSP;
        }
    }

    private void switchToUserStack() {
        kernelSP = sp;
        sp = userSP;
    }

    private TWord pcWord() {
        return TWord.fromLong(pc);
    }

    private void incrementPC() {
        pc++;
        if (pc >= MEMORY_SIZE) {
            raiseTrap(TTrap.INVALID_MEMORY);
        }
    }

    private int getTrapVector(TTrap cause) {
        if (cause == null) {
            throw new IllegalArgumentException("Trap null");
        }
        return TRAP_VECTOR_BASE + cause.code;
    }

    private int checkedAddress(long address) {
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new TMemoryException("Dirección de memoria inválida: " + address);
        }
        return (int) address;
    }

    private void checkAddress(int address) {
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new TMemoryException("Dirección de memoria inválida: " + address);
        }
    }

    private void checkRegister(int index) {
        if (index < 0 || index >= REGISTER_COUNT) {
            throw new IllegalArgumentException("Registro inválido: R" + index);
        }
    }

    private int checkedUserAddress(long address) {
        int value = checkedAddress(address);

        if (!kernelMode) {
            if (value < currentMemoryBase || value > currentMemoryLimit) {
                raiseTrap(TTrap.INVALID_MEMORY);
                return -1;
            }
        }

        return value;
    }

    public boolean isKernelMode() {
        return kernelMode;
    }

    public int getUserSP() {
        return userSP;
    }

    public int getKernelSP() {
        return kernelSP;
    }

    private void handleInterrupt() {
        if (pendingInterrupt == null) {
            return;
        }

        TInterrupt interrupt = pendingInterrupt;
        pendingInterrupt = null;

        if (!kernelMode) {
            userSP = sp;
            kernelMode = true;
            kernelSP = KERNEL_STACK_TOP;
            sp = kernelSP;
        }

        if (sp < KERNEL_STACK_BOTTOM + 2) {
            halted = true;
            return;
        }

        // Frame de interrupción:
        // [sp]     = PC de retorno
        // [sp - 1] = código de interrupción
        memory[sp] = TWord.fromLong(pc);
        sp--;

        memory[sp] = TWord.fromLong(interrupt.code);
        sp--;

        int vectorAddress = INTERRUPT_VECTOR_BASE + interrupt.code;
        int handler = checkedAddress(memory[vectorAddress].toLong());
        pc = handler;
    }

    public void setKernelMode(boolean value) {
        kernelMode = value;
    }

    public void setSP(int value) {
        checkAddress(value);
        sp = value;
    }

    public void halt() {
        halted = true;
    }

    public void restoreProcessContext(int pc, TWord[] registers, int userSP, int compare) {
        this.pc = pc;

        for (int i = 0; i < REGISTER_COUNT; i++) {
            this.registers[i] = registers[i].copy();
        }

        this.userSP = userSP;
        this.compare = compare;

        this.sp = userSP;
        this.kernelMode = false;
        this.trap = null;
    }

    public void resume() {
        halted = false;
    }
}

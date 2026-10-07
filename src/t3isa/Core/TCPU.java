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
package t3isa.Core;

import t3isa.DEVICE.TGraphicsDevice;
import t3isa.DEVICE.TMouseDevice;
import t3isa.Exceptions.TMemoryException;
import t3isa.T3ISA;
import t3isa.HARDWARE.TDevice;
import t3isa.HARDWARE.TDeviceBus;
import t3isa.HARDWARE.TMMIOBus;
import t3isa.HARDWARE.TMachine;
import t3isa.ISA.TInstruction;
import t3isa.ISA.TInterrupt;
import t3isa.ISA.TOpcode;
import t3isa.ISA.TSyscall;
import t3isa.ISA.TTrap;
import t3os.KERNEL.TKernel;

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
    * 10..16 - reservados
    * 17     - SYS_NET_STATUS
    * 18     - SYS_NET_SEND
    * 19     - SYS_NET_RECV
    * 20..26 - reservados
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
    * 0..999    KERNEL
    *      FONT_BASE
    *           FONT_WORDS
    * ..    FONTS 8x16
    * 1000..15999  USER
    * 16000..19682 STACK
    * 19683  MMIO_GRAPHICS_X
    * 19684  MMIO_GRAPHICS_Y
    * 19685  MMIO_GRAPHICS_COLOR
    * 19686  MMIO_GRAPHICS_COMMAND
    *
    * Stack grows downward.
 */
public final class TCPU implements TMachine {

    private int userSP;
    private int kernelSP;

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
    private int currentStackBase;
    private int currentStackLimit;

    private boolean halted;
    private int pendingProcessAction = -1;
    private int pendingDevicePort = -1;
    private int interruptedPC;
    private boolean interruptReturned;

    //private TDevice device;   // Ya no es necesario
    private final TDeviceBus deviceBus;
    private final TMMIOBus mmioBus;
    private final TGraphicsDevice graphicsDevice;
    private final TMouseDevice mouseDevice;
    private TKernel kernel;

    public TCPU() {
        registers = new TWord[REGISTER_COUNT];
        memory = new TWord[MEMORY_SIZE];
        currentPid = 0;
        pendingProcessAction = -1;

        deviceBus = new TDeviceBus(16);
        graphicsDevice = new TGraphicsDevice(1024, 768, 60, 32);
        mouseDevice = new TMouseDevice();

        mmioBus = new TMMIOBus(8);
        mmioBus.map(MMIO_BASE, 4, graphicsDevice);
        mmioBus.map(MMIO_BASE + 4, 4, mouseDevice);

        reset();
    }

    @Override
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
        currentStackBase = USER_STACK_BOTTOM;
        currentStackLimit = USER_STACK_TOP;
        compare = 0;
        currentPid = 0;
        pendingProcessAction = -1;
        pendingDevicePort = -1;
        interruptReturned = false;
        halted = false;
    }

    @Override
    public boolean wasInterruptReturned() {
        return interruptReturned;
    }

    @Override
    public void clearInterruptReturned() {
        interruptReturned = false;
    }

    @Override
    public int getCurrentPid() {
        return currentPid;
    }

    @Override
    public void setCurrentPid(int pid) {
        currentPid = pid;
    }

    @Override
    public int getPendingProcessAction() {
        return pendingProcessAction;
    }

    @Override
    public void clearPendingProcessAction() {
        pendingProcessAction = -1;
    }

    @Override
    public int getPendingDevicePort() {
        return pendingDevicePort;
    }

    @Override
    public void clearPendingDevicePort() {
        pendingDevicePort = -1;
    }

    public TInterrupt getPendingInterrupt() {
        return pendingInterrupt;
    }

    @Override
    public void requestInterrupt(TInterrupt interrupt) {
        if (interrupt == null) {
            throw new IllegalArgumentException("Interrupt null");
        }
        pendingInterrupt = interrupt;
    }

    @Override
    public void loadInterruptVector(TInterrupt interrupt, int handlerAddress) {
        if (interrupt == null) {
            throw new IllegalArgumentException("Interrupt null");
        }

        checkAddress(handlerAddress);

        int vectorAddress = INTERRUPT_VECTOR_BASE + interrupt.code;
        memory[vectorAddress] = TWord.fromLong(handlerAddress);
    }

    @Override
    public TWord getRegister(int index) {
        checkRegister(index);
        return registers[index].copy();
    }

    @Override
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

    @Override
    public int getPC() {
        return pc;
    }

    @Override
    public void setPC(int value) {
        checkAddress(value);
        pc = value;
    }

    @Override
    public int getSP() {
        return sp;
    }

    @Override
    public String getTrap() {
        return trap == null ? null : trap.toString();
    }

    public TTrap getTrapCode() {
        return trap;
    }

    @Override
    public boolean isHalted() {
        return halted;
    }

    @Override
    public int getCompare() {
        return compare;
    }

    @Override
    public void setProcessMemoryRange(int base, int limit) {
        checkAddress(base);
        checkAddress(limit);

        if (base > limit) {
            throw new IllegalArgumentException("Rango de memoria inválido");
        }

        currentMemoryBase = base;
        currentMemoryLimit = limit;
    }

    public void setProcessStackRange(int base, int limit) {
        checkAddress(base);
        checkAddress(limit);

        if (base > limit) {
            throw new IllegalArgumentException("Rango de stack inválido");
        }

        currentStackBase = base;
        currentStackLimit = limit;
    }

    public void attachDevice(int port, TDevice device) {
        deviceBus.attach(port, device);
    }

    public void loadBootSector(TWord[] boot) {
        if (boot == null) {
            throw new IllegalArgumentException("Boot sector null");
        }
        loadProgram(BOOT_START, boot);
    }

    @Override
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

    @Override
    public void loadTrapVector(TTrap trap, int handlerAddress) {
        if (trap == null) {
            throw new IllegalArgumentException("Trap null");
        }

        checkAddress(handlerAddress);

        int vectorAddress = TRAP_VECTOR_BASE + trap.code;
        memory[vectorAddress] = TWord.fromLong(handlerAddress);
    }

    @Override
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
                if (T3ISA.isDEBUG) {
                    System.out.println("INVALID PC: " + pc);
                }
                raiseTrap(TTrap.INVALID_MEMORY);
                return;
            }

            if (!kernelMode && (pc < currentMemoryBase || pc > currentMemoryLimit)) {
                if (T3ISA.isDEBUG) {
                    System.out.println("PROCESS MEMORY VIOLATION PC=" + pc + " RANGE=" + currentMemoryBase + ".." + currentMemoryLimit);
                }
                raiseTrap(TTrap.INVALID_MEMORY);

                // Violación de ejecución: el proceso no puede continuar.
                pendingProcessAction = TSyscall.EXIT;
                return;
            }

            int instructionAddress = pc;
            if (T3ISA.isDEBUG) {
                System.out.println("FETCH PC=" + pc + " WORD=" + memory[pc].toLong());
            }
            TInstruction instruction;

            try {
                instruction = TInstruction.decode(memory[pc]);
            } catch (IllegalStateException e) {
                if (T3ISA.isDEBUG) {
                    System.out.println("DECODE ERROR PC=" + pc + " WORD=" + memory[pc].toLong() + " MSG=" + e.getMessage());
                }
                raiseTrap(TTrap.INVALID_INSTRUCTION);
                return;
            }

            if (instruction == null) {
                if (T3ISA.isDEBUG) {
                    System.out.println("DECODE NULL PC=" + pc);
                }
                raiseTrap(TTrap.INVALID_INSTRUCTION);
                return;
            }
            if (T3ISA.isDEBUG) {
                System.out.println("DECODE OK PC=" + pc + " OPCODE=" + instruction.getOpcode());
            }
            pc = instructionAddress;

            execute(instruction);
        } catch (TMemoryException e) {
            if (T3ISA.isDEBUG) {
                System.out.println("MEMORY EXCEPTION PC=" + pc);
            }
            raiseTrap(TTrap.INVALID_MEMORY);
        } catch (ArithmeticException e) {
            if (T3ISA.isDEBUG) {
                System.out.println("ARITHMETIC EXCEPTION PC=" + pc);
            }
            raiseTrap(TTrap.DIVIDE_BY_ZERO);
        } catch (IllegalArgumentException e) {
            if (T3ISA.isDEBUG) {
                System.out.println("ILLEGAL ARGUMENT PC=" + pc + " MSG=" + e.getMessage());
            }
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

            case CONST:
                int constAddress = pc + 1;

                if (T3ISA.isDEBUG) {
                    System.out.println(
                            "CONST EXEC PC=" + pc
                            + " DST=R" + instruction.getDst()
                            + " CONST_ADDR=" + constAddress
                            + " VALUE=" + memory[constAddress].toLong()
                    );
                }

                if (constAddress < 0 || constAddress >= MEMORY_SIZE) {
                    raiseTrap(TTrap.INVALID_MEMORY);
                    return;
                }

                setRegister(
                        instruction.getDst(),
                        memory[constAddress].copy()
                );

                if (T3ISA.isDEBUG) {
                    System.out.println(
                            "CONST SET OK R" + instruction.getDst()
                            + " VALUE=" + getRegister(instruction.getDst()).toLong()
                    );
                }

                pc += 2;

                if (T3ISA.isDEBUG) {
                    System.out.println("CONST PC NEXT=" + pc);
                }

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
                int jumpAddress = checkedUserCodeAddress(instruction.getImmediate());
                if (jumpAddress < 0) {
                    return;
                }
                pc = jumpAddress;
                break;

            case JNEG:
                if (compare < 0) {
                    int target = checkedUserCodeAddress(instruction.getImmediate());
                    if (target < 0) {
                        return;
                    }
                    pc = target;
                } else {
                    incrementPC();
                }
                break;

            case JZERO:
                if (compare == 0) {
                    int target = checkedUserCodeAddress(instruction.getImmediate());
                    if (target < 0) {
                        return;
                    }
                    pc = target;
                } else {
                    incrementPC();
                }
                break;

            case JPOS:
                if (compare > 0) {
                    int target = checkedUserCodeAddress(instruction.getImmediate());
                    if (target < 0) {
                        return;
                    }
                    pc = target;
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
                if (trap != null) {
                    return;
                }
                incrementPC();
                break;

            case POP:
                setRegister(instruction.getDst(), pop());
                if (trap != null) {
                    return;
                }
                incrementPC();
                break;

            case CALL:
                executeCall(instruction);
                break;

            case RET:
                TWord returnAddress = pop();

                if (trap != null) {
                    return;
                }

                int returnPC = checkedUserCodeAddress(returnAddress.toLong());

                if (returnPC < 0) {
                    return;
                }

                pc = returnPC;
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
        long base = getRegister(instruction.getSrc1()).toLong();
        long rawAddress = base + instruction.getImmediate();

        if (rawAddress >= MMIO_BASE) {
            if (!kernelMode) {
                raiseTrap(TTrap.INVALID_MEMORY);
                return;
            }
            setRegister(instruction.getDst(), mmioBus.read((int) rawAddress));
            incrementPC();
            return;
        }

        int address = checkedUserAddress(rawAddress);
        if (address < 0) {
            return;
        }

        setRegister(instruction.getDst(), memory[address]);
        incrementPC();
    }

    private void executeStore(TInstruction instruction) {
        long base = getRegister(instruction.getSrc1()).toLong();
        long rawAddress = base + instruction.getImmediate();
        if (T3ISA.isDEBUG) {
            System.out.println(
                    "STORE dst=R" + instruction.getDst()
                    + " src1=R" + instruction.getSrc1()
                    + " base=" + base
                    + " imm=" + instruction.getImmediate()
                    + " address=" + rawAddress
                    + " value=" + getRegister(instruction.getDst()).toLong()
            );
        }

        if (rawAddress >= MMIO_BASE) {
            System.out.println(
                    "MMIO WRITE -> " + rawAddress
            );
            if (!kernelMode) {
                raiseTrap(TTrap.INVALID_MEMORY);
                return;
            }
            mmioBus.write((int) rawAddress, getRegister(instruction.getDst()));
            incrementPC();
            return;
        }

        int address = checkedUserAddress(rawAddress);
        if (address < 0) {
            return;
        }

        memory[address] = getRegister(instruction.getDst()).copy();
        incrementPC();
    }

    private void executeCall(TInstruction instruction) {
        int target = checkedUserCodeAddress(instruction.getImmediate());

        if (target < 0) {
            return;
        }

        push(TWord.fromLong(pc + 1));

        if (trap != null) {
            return;
        }

        pc = target;
    }

    private void executeIRet() {
        if (!kernelMode) {
            halted = true;
            return;
        }

        int trapAddress = sp + 1;
        int pcAddress = sp + 2;

        if (trapAddress < KERNEL_STACK_BOTTOM || trapAddress > KERNEL_STACK_TOP || pcAddress < KERNEL_STACK_BOTTOM || pcAddress > KERNEL_STACK_TOP) {
            raiseTrap(TTrap.STACK_ERROR);
            return;
        }

        int code = (int) memory[trapAddress].toLong();
        int returnPC = (int) memory[pcAddress].toLong();

        if (trap != null) {
            if (code < 0 || code >= TRAP_VECTOR_COUNT) {
                halted = true;
                return;
            }
        } else {
            if (code < 0 || code >= INTERRUPT_VECTOR_COUNT) {
                halted = true;
                return;
            }
        }

        if (returnPC < currentMemoryBase || returnPC > currentMemoryLimit) {
            raiseTrap(TTrap.INVALID_MEMORY);
            return;
        }

        pc = returnPC;
        sp = userSP;
        kernelMode = false;

        if (trap != null) {
            trap = null;
        } else {
            interruptReturned = true;
        }
    }

    private void executeSys() {
        if (T3ISA.isDEBUG) {
            System.out.println(
                    "SYS -> R1=" + getRegister(1).toLong()
                    + " R2=" + getRegister(2).toLong()
                    + " R3=" + getRegister(3).toLong()
                    + " PC=" + pc
            );
        }
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
                    incrementPC();

                } catch (IllegalArgumentException | IllegalStateException e) {
                    raiseTrap(TTrap.DEVICE_ERROR);
                }
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

            case TSyscall.FORK:
                if (kernelMode) {
                    raiseTrap(TTrap.INVALID_SYSCALL);
                    return;
                }

                incrementPC();
                pendingProcessAction = TSyscall.FORK;
                break;

            case TSyscall.WAIT:
                if (kernelMode) {
                    raiseTrap(TTrap.INVALID_SYSCALL);
                    return;
                }

                incrementPC();
                pendingProcessAction = TSyscall.WAIT;
                break;

            case TSyscall.EXEC:
                if (kernelMode) {
                    raiseTrap(TTrap.INVALID_SYSCALL);
                    return;
                }

                int execAddress = (int) getRegister(2).toLong();
                int execSize = (int) getRegister(3).toLong();

                if (execAddress < currentMemoryBase
                        || execAddress > currentMemoryLimit
                        || execSize <= 0
                        || execSize > currentMemoryLimit - execAddress + 1) {

                    raiseTrap(TTrap.INVALID_MEMORY);
                    return;
                }

                incrementPC();
                pendingProcessAction = TSyscall.EXEC;
                break;

            case TSyscall.NET_STATUS:
                registers[7] = TWord.fromLong(kernel.hasNetworkFrame() ? 1 : 0);
                incrementPC();
                break;

            case TSyscall.NET_SEND:
                if (!kernelMode) {
                    raiseTrap(TTrap.INVALID_SYSCALL);
                    return;
                }

                int sendAddress = checkedAddress(getRegister(2).toLong());
                int sendLength = (int) getRegister(3).toLong();

                if (sendLength < 0 || sendLength > 1518 || sendAddress > MEMORY_SIZE - sendLength) {
                    raiseTrap(TTrap.INVALID_MEMORY);
                    return;
                }

                byte[] sendFrame = new byte[sendLength];
                for (int i = 0; i < sendLength; i++) {
                    sendFrame[i] = (byte) memory[sendAddress + i].toLong();
                }

                try {
                    kernel.getNetworkDevice().sendFrame(sendFrame);
                    setRegister(7, TWord.fromLong(sendLength));
                    incrementPC();
                } catch (IllegalArgumentException | IllegalStateException e) {
                    raiseTrap(TTrap.DEVICE_ERROR);
                }
                break;

            case TSyscall.NET_RECV:

                if (!kernelMode) {
                    raiseTrap(TTrap.INVALID_SYSCALL);
                    return;
                }

                int recvAddress = checkedAddress(getRegister(2).toLong());
                int recvLength = (int) getRegister(3).toLong();
                if (recvLength < 0 || recvLength > 1518 || recvAddress > MEMORY_SIZE - recvLength) {
                    raiseTrap(TTrap.INVALID_MEMORY);
                    return;
                }

                if (!kernel.hasNetworkFrame()) {
                    setRegister(7, TWord.fromLong(-1));
                    incrementPC();
                    break;
                }

                try {
                    byte[] recvFrame = kernel.receiveNetworkFrame();

                    if (recvFrame == null) {
                        setRegister(7, TWord.fromLong(-1));
                        incrementPC();
                        break;
                    }

                    int copyLength = Math.min(recvFrame.length, recvLength);
                    for (int i = 0; i < copyLength; i++) {
                        memory[recvAddress + i] = TWord.fromLong(recvFrame[i] & 0xFF);
                    }

                    setRegister(7, TWord.fromLong(copyLength));
                    incrementPC();
                } catch (IllegalArgumentException | IllegalStateException e) {
                    raiseTrap(TTrap.DEVICE_ERROR);
                }
                break;

            case TSyscall.GRAPHICS_PIXEL:
                int x = (int) getRegister(2).toLong();
                int y = (int) getRegister(3).toLong();
                int color = (int) getRegister(4).toLong();

                if (x < 0 || x >= graphicsDevice.getWidth() || y < 0 || y >= graphicsDevice.getHeight()) {
                    raiseTrap(TTrap.INVALID_MEMORY);
                    return;
                }

                mmioBus.write(MMIO_GRAPHICS_X, TWord.fromLong(x));
                mmioBus.write(MMIO_GRAPHICS_Y, TWord.fromLong(y));
                mmioBus.write(MMIO_GRAPHICS_COLOR, TWord.fromLong(color));
                mmioBus.write(MMIO_GRAPHICS_COMMAND, TWord.fromLong(1));

                incrementPC();
                break;

            case TSyscall.GRAPHICS_CLEAR: {
                int colorc = (int) getRegister(2).toLong();
                graphicsDevice.clear(colorc);
                incrementPC();
                break;
            }

            case TSyscall.GRAPHICS_LINE: {
                int x1 = (int) getRegister(2).toLong();
                int y1 = (int) getRegister(3).toLong();
                int x2 = (int) getRegister(4).toLong();
                int y2 = (int) getRegister(5).toLong();
                int colorl = (int) getRegister(6).toLong();
                graphicsDevice.drawLine(x1, y1, x2, y2, colorl);
                incrementPC();
                break;
            }

            case TSyscall.GRAPHICS_RECT: {
                int xr = (int) getRegister(2).toLong();
                int yr = (int) getRegister(3).toLong();
                int width = (int) getRegister(4).toLong();
                int height = (int) getRegister(5).toLong();
                int colorr = (int) getRegister(6).toLong();
                graphicsDevice.drawRect(xr, yr, width, height, colorr);
                incrementPC();
                break;
            }

            case TSyscall.GRAPHICS_FILL_RECT: {
                int xfr = (int) getRegister(2).toLong();
                int yfr = (int) getRegister(3).toLong();
                int width = (int) getRegister(4).toLong();
                int height = (int) getRegister(5).toLong();
                int colorfr = (int) getRegister(6).toLong();
                graphicsDevice.fillRect(xfr, yfr, width, height, colorfr);
                incrementPC();
                break;
            }
            case TSyscall.GRAPHICS_CIRCLE: {
                int xc = (int) getRegister(2).toLong();
                int yc = (int) getRegister(3).toLong();
                int radius = (int) getRegister(4).toLong();
                int colorgc = (int) getRegister(5).toLong();
                graphicsDevice.drawCircle(xc, yc, radius, colorgc);
                incrementPC();
                break;
            }
            case TSyscall.GRAPHICS_FILL_CIRCLE: {
                int xfc = (int) getRegister(2).toLong();
                int yfc = (int) getRegister(3).toLong();
                int radius = (int) getRegister(4).toLong();
                int colorfc = (int) getRegister(5).toLong();
                graphicsDevice.fillCircle(xfc, yfc, radius, colorfc);
                incrementPC();
                break;
            }
            case TSyscall.GRAPHICS_GET_PIXEL: {
                int xgp = (int) getRegister(2).toLong();
                int ygp = (int) getRegister(3).toLong();
                if (xgp < 0 || xgp >= graphicsDevice.getWidth() || ygp < 0 || ygp >= graphicsDevice.getHeight()) {
                    raiseTrap(TTrap.INVALID_MEMORY);
                    return;
                }
                setRegister(7, TWord.fromLong(graphicsDevice.getPixel(xgp, ygp)));
                incrementPC();
                break;
            }

            case TSyscall.GRAPHICS_WIDTH:
                setRegister(7, TWord.fromLong(graphicsDevice.getWidth()));
                incrementPC();
                break;

            case TSyscall.GRAPHICS_HEIGHT:
                setRegister(7, TWord.fromLong(graphicsDevice.getHeight()));
                incrementPC();
                break;

            case TSyscall.GRAPHICS_COLOR:
                graphicsDevice.clear((int) getRegister(2).toLong());
                incrementPC();
                break;

            case TSyscall.GRAPHICS_PRESENT:
                /* 
                * El framebuffer ya es visible para el backend. 
                * Actualmente no requiere ninguna operación adicional. */
                incrementPC();
                break;

            case TSyscall.SLEEP:
                int milliseconds = (int) getRegister(2).toLong();

                if (milliseconds > 0) {
                    try {
                        Thread.sleep(milliseconds);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                incrementPC();
                break;

            case TSyscall.GRAPHICS_CHAR: {
                int character = (int) getRegister(2).toLong();
                int xgc = (int) getRegister(3).toLong();
                int ygc = (int) getRegister(4).toLong();
                int scale = (int) getRegister(5).toLong();
                int foreground = (int) getRegister(6).toLong();
                int background = (int) getRegister(7).toLong();

                if (character < 0 || character > 255 || scale <= 0) {
                    raiseTrap(TTrap.INVALID_SYSCALL);
                    return;
                }

                if (xgc < 0 || ygc < 0 || xgc + 8 * scale > graphicsDevice.getWidth() || ygc + 16 * scale > graphicsDevice.getHeight()) {
                    raiseTrap(TTrap.INVALID_MEMORY);
                    return;
                }

                graphicsDevice.drawChar(character, xgc, ygc, scale, foreground, background);

                incrementPC();
                return;
            }

            default:
                raiseTrap(TTrap.INVALID_SYSCALL);
                break;
        }
    }

    private void push(TWord value) {
        int bottom = kernelMode ? KERNEL_STACK_BOTTOM : currentStackBase;
        if (sp < bottom) {
            raiseTrap(TTrap.STACK_ERROR);
            return;
        }

        memory[sp] = value.copy();
        sp--;
    }

    private TWord pop() {
        int bottom = kernelMode ? KERNEL_STACK_BOTTOM : currentStackBase;
        int top = kernelMode ? KERNEL_STACK_TOP : currentStackLimit;

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

        memory[sp] = TWord.fromLong(pc);
        sp--;

        memory[sp] = TWord.fromLong(cause.code);
        sp--;

        int vectorAddress = TRAP_VECTOR_BASE + cause.code;

        int handler = checkedAddress(
                memory[vectorAddress].toLong()
        );

        pc = handler;

        // El trap ya está atendido por el CPU.
        // El handler decidirá si el proceso termina.
    }

    public void requestDeviceInterrupt() {
        pendingInterrupt = TInterrupt.DEVICE;
    }

    private void incrementPC() {
        pc++;
        if (pc >= MEMORY_SIZE) {
            raiseTrap(TTrap.INVALID_MEMORY);
        }
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

    private int checkedUserAddress(long rawAddress) {
        if (rawAddress < 0 || rawAddress >= MEMORY_SIZE) {
            raiseTrap(TTrap.INVALID_MEMORY);
            return -1;
        }

        int address = (int) rawAddress;

        if (kernelMode) {
            return address;
        }

        boolean inProcessMemory = address >= currentMemoryBase && address <= currentMemoryLimit;
        boolean inProcessStack = address >= currentStackBase && address <= currentStackLimit;

        if (!inProcessMemory && !inProcessStack) {
            raiseTrap(TTrap.INVALID_MEMORY);
            return -1;
        }

        return address;
    }

    private int checkedUserCodeAddress(long rawAddress) {
        if (rawAddress < 0 || rawAddress >= MEMORY_SIZE) {
            raiseTrap(TTrap.INVALID_MEMORY);
            return -1;
        }

        int address = (int) rawAddress;

        if (kernelMode) {
            return address;
        }

        if (address < currentMemoryBase || address > currentMemoryLimit) {
            raiseTrap(TTrap.INVALID_MEMORY);
            return -1;
        }

        return address;
    }

    @Override
    public boolean isKernelMode() {
        return kernelMode;
    }

    @Override
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
            interruptedPC = pc;

            userSP = sp;
            kernelMode = true;
            kernelSP = KERNEL_STACK_TOP;
            sp = kernelSP;
        }

        if (sp < KERNEL_STACK_BOTTOM + 2) {
            halted = true;
            return;
        }

        memory[sp] = TWord.fromLong(pc);
        sp--;

        memory[sp] = TWord.fromLong(interrupt.code);
        sp--;

        int vectorAddress = INTERRUPT_VECTOR_BASE + interrupt.code;
        int handler = checkedAddress(memory[vectorAddress].toLong());

        pc = handler;
    }

    @Override
    public int getInterruptedPC() {
        return interruptedPC;
    }

    public void setKernelMode(boolean value) {
        kernelMode = value;
    }

    public void setSP(int value) {
        checkAddress(value);
        sp = value;
    }

    @Override
    public void halt() {
        halted = true;
    }

    @Override
    public void restoreProcessContext(int pc, TWord[] registers, int userSP, int compare, int stackBase, int stackLimit) {
        this.pc = pc;

        for (int i = 0; i < REGISTER_COUNT; i++) {
            this.registers[i] = registers[i].copy();
        }

        this.userSP = userSP;
        this.compare = compare;

        this.currentStackBase = stackBase;
        this.currentStackLimit = stackLimit;

        this.sp = userSP;
        this.kernelMode = false;
        this.trap = null;
    }

    public int getProcessMemoryBase() {
        return currentMemoryBase;
    }

    public int getProcessMemoryLimit() {
        return currentMemoryLimit;
    }

    public int getProcessStackBase() {
        return currentStackBase;
    }

    public int getProcessStackLimit() {
        return currentStackLimit;
    }

    @Override
    public TWord readMemory(int address) {
        checkAddress(address);
        return memory[address].copy();
    }

    @Override
    public void writeMemory(int address, TWord value) {
        checkAddress(address);

        if (value == null) {
            throw new IllegalArgumentException("Valor de memoria null");
        }

        memory[address] = value.copy();
    }

    public void copyMemoryRange(int sourceBase, int sourceLimit, int destinationBase) {
        if (sourceBase < 0 || sourceLimit >= MEMORY_SIZE || sourceBase > sourceLimit) {
            throw new IllegalArgumentException("Rango origen inválido");
        }

        int size = sourceLimit - sourceBase + 1;

        if (destinationBase < 0 || destinationBase + size > MEMORY_SIZE) {
            throw new IllegalArgumentException("Rango destino inválido");
        }

        for (int i = 0; i < size; i++) {
            memory[destinationBase + i] = memory[sourceBase + i].copy();
        }
    }

    @Override
    public void copyProcessStack(int sourceBase, int sourceLimit, int destinationBase) {
        if (sourceBase < 0 || sourceLimit >= MEMORY_SIZE || sourceBase > sourceLimit) {
            throw new IllegalArgumentException("Stack origen inválido");
        }

        int size = sourceLimit - sourceBase + 1;

        if (destinationBase < 0 || destinationBase + size > MEMORY_SIZE) {
            throw new IllegalArgumentException("Stack destino inválido");
        }

        for (int i = 0; i < size; i++) {
            memory[destinationBase + i] = memory[sourceBase + i].copy();
        }
    }

    public void resume() {
        halted = false;
    }

    @Override
    public TDeviceBus getDeviceBus() {
        return deviceBus;
    }

    @Override
    public void setKernel(TKernel kernel) {
        if (kernel == null) {
            throw new IllegalArgumentException("El kernel no puede ser null");
        }
        this.kernel = kernel;
    }

    public TMMIOBus getMMIOBus() {
        return mmioBus;
    }

    public TGraphicsDevice getGraphicsDevice() {
        return graphicsDevice;
    }

    public TMouseDevice getMouseDevice() {
        return mouseDevice;
    }

    @Override
    public void graphics(int command, int a, int b, int c, int d, int e) {
        graphicsDevice.graphics(command, a, b, c, d, e);
    }

    @Override
    public void drawChar(int character, int x, int y, int scale, int foreground, int background) {
        graphicsDevice.drawChar(character, x, y, scale, foreground, background);
    }

    @Override
    public int getMouseX() {
        return mouseDevice.getX();
    }

    @Override
    public int getMouseY() {
        return mouseDevice.getY();
    }

    @Override
    public int getMouseButtons() {
        return mouseDevice.getButtons();
    }
}

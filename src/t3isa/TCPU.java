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
 */
public final class TCPU {

    private TDevice device;
    public static final int REGISTERS = 27;
    public static final int MEMORY_SIZE = 19683;

    private final TWord[] registers;
    private final TWord[] memory;

    private TWord pc;
    private TWord sp;

    private int compare;
    private int programSize;

    private boolean halted;

    public TCPU() {
        device = null;
        registers = new TWord[REGISTERS];
        memory = new TWord[MEMORY_SIZE];

        for (int i = 0; i < REGISTERS; i++) {
            registers[i] = TWord.zero();
        }

        for (int i = 0; i < MEMORY_SIZE; i++) {
            memory[i] = TWord.zero();
        }
        pc = TWord.zero();
        sp = TWord.fromLong(MEMORY_SIZE - 1);
        compare = 0;
        programSize = 0;
        halted = false;
    }

    public void reset() {
        for (int i = 0; i < REGISTERS; i++) {
            registers[i] = TWord.zero();
        }
        pc = TWord.zero();
        sp = TWord.fromLong(MEMORY_SIZE - 1);
        compare = 0;
        programSize = 0;
        halted = false;
    }

    public TWord getRegister(int index) {
        checkRegister(index);
        if (index == 0) {
            return TWord.zero();
        }
        return registers[index].copy();
    }

    public void setRegister(int index, TWord value) {
        checkRegister(index);
        if (index == 0) {
            return;
        }
        registers[index] = value.copy();
    }

    public void setDevice(TDevice device) {
        this.device = device;
    }

    public TWord getPC() {
        return pc.copy();
    }

    public TWord getSP() {
        return sp.copy();
    }

    public int getCompare() {
        return compare;
    }

    public boolean isHalted() {
        return halted;
    }

    public void loadProgram(TWord[] program) {
        if (program == null) {
            throw new IllegalArgumentException("Programa null");
        }

        if (program.length > MEMORY_SIZE) {
            throw new IllegalArgumentException("Programa demasiado grande: " + program.length);
        }

        /*
         * Limpiar memoria del programa anterior.
         */
        for (int i = 0; i < programSize; i++) {
            memory[i] = TWord.zero();
        }

        /*
         * Cargar nuevo programa.
         */
        for (int i = 0; i < program.length; i++) {
            memory[i] = program[i].copy();
        }

        programSize = program.length;
        pc = TWord.zero();
        halted = false;
    }

    public void load(int address, TWord value) {
        checkAddress(address);
        memory[address] = value.copy();
    }

    public TWord read(int address) {
        checkAddress(address);
        return memory[address].copy();
    }

    public void run() {
        while (!halted) {
            step();
        }
    }

    public void step() {
        if (halted) {
            return;
        }

        int address = addressOf(pc);
        if (address >= programSize) {
            throw new IllegalStateException("PC fuera del programa: " + address + " / tamaño=" + programSize);
        }

        TInstruction instruction = TInstruction.decode(memory[address]);
        execute(instruction);
    }

    private void execute(TInstruction instruction) {
        TOpcode opcode = instruction.getOpcode();

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
                setRegister(instruction.getDst(), TALU.shiftLeft(getRegister(instruction.getSrc1()), (int) getRegister(instruction.getSrc2()).toLong()));
                incrementPC();
                break;

            case SHR:
                setRegister(instruction.getDst(), TALU.shiftRight(getRegister(instruction.getSrc1()), (int) getRegister(instruction.getSrc2()).toLong()));
                incrementPC();
                break;

            case NEG:
                setRegister(instruction.getDst(), TALU.negate(getRegister(instruction.getSrc1())));
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

            case TNOT:
                setRegister(instruction.getDst(), TALU.not(getRegister(instruction.getSrc1())));
                incrementPC();
                break;

            case TXOR:
                setRegister(instruction.getDst(), TALU.xor(getRegister(instruction.getSrc1()), getRegister(instruction.getSrc2())));
                incrementPC();
                break;

            case LOAD:
                executeLoad(instruction);
                break;

            case STORE:
                executeStore(instruction);
                break;

            case PUSH:
                push(getRegister(instruction.getDst()));
                incrementPC();
                break;

            case POP:
                setRegister(instruction.getDst(), pop());
                incrementPC();
                break;

            case JMP:
                jump(instruction.getImmediate());
                break;

            case JNEG:
                if (compare < 0) {
                    jump(instruction.getImmediate());
                } else {
                    incrementPC();
                }
                break;

            case JZERO:
                if (compare == 0) {
                    jump(instruction.getImmediate());
                } else {
                    incrementPC();
                }
                break;

            case JPOS:
                if (compare > 0) {
                    jump(instruction.getImmediate());
                } else {
                    incrementPC();
                }
                break;

            case CALL:
                executeCall(instruction.getImmediate());
                break;

            case RET:
                pc = pop();
                break;

            case SYS:
                executeSys();
                break;

            default:
                throw new IllegalStateException("Instrucción aún no implementada: " + opcode);
        }
    }

    private void incrementPC() {
        pc = TALU.increment(pc);
    }

    private void executeSys() {
        int service = (int) getRegister(1).toLong();

        switch (service) {
            case 0: // Sys 0 - Halt
                halted = true;
                break;

            case 1: // Sys 1 - getpc R7 = PC
                setRegister(7, pc);
                incrementPC();
                break;

            case 2: // Sys 2 - getsp R7 = SP
                setRegister(7, sp);
                incrementPC();
                break;

            case 3: // Sys 3 - getcmp R7 = -1, 0, +1
                setRegister(7, TWord.fromLong(compare));
                incrementPC();
                break;
            case 4:
                // Sys 4 - read
                // R2 = dirección
                // R7 = valor
                int readAddress = checkedAddress(getRegister(2).toLong());
                setRegister(7, memory[readAddress]);
                incrementPC();
                break;

            case 5:
                // SYS 5 - write
                // R2 = dirección
                // R3 = valor
                int writeAddress = checkedAddress(getRegister(2).toLong());
                memory[writeAddress] = getRegister(3);
                incrementPC();
                break;
            case 6: // Sys 6 - device
                if (device == null) {
                    throw new IllegalStateException("SYS 6 requiere un dispositivo");
                }

                device.write(getRegister(2));
                incrementPC();
                break;

            default:
                throw new IllegalStateException("SYS desconocido: " + service);
        }
    }

    private void executeLoad(TInstruction instruction) {
        long base = getRegister(instruction.getSrc1()).toLong();
        long address = base + instruction.getImmediate();

        checkAddress(checkedAddress(address));
        setRegister(instruction.getDst(), memory[(int) address]);
        incrementPC();
    }

    private void executeStore(TInstruction instruction) {
        long base = getRegister(instruction.getSrc1()).toLong();
        long address = base + instruction.getImmediate();
        int index = checkedAddress(address);
        memory[index] = getRegister(instruction.getDst());
        incrementPC();
    }

    private void executeCall(int address) {
        /*
         * Guardar la dirección de retorno.
         *
         * CALL está en PC.
         * La siguiente instrucción es PC + 1.
         */
        TWord returnAddress = TALU.increment(pc);
        push(returnAddress);
        jump(address);
    }

    private void push(TWord value) {
        int address = addressOf(sp);
        memory[address] = value.copy();
        sp = TALU.decrement(sp);
    }

    private TWord pop() {
        sp = TALU.increment(sp);
        int address = addressOf(sp);
        return memory[address].copy();
    }

    private void jump(int address) {
        checkAddress(address);
        pc = TWord.fromLong(address);
    }

    private int addressOf(TWord value) {
        long address = value.toLong();
        return checkedAddress(address);
    }

    private int checkedAddress(long address) {
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new IllegalStateException("Dirección de memoria inválida: " + address);
        }
        return (int) address;
    }

    private void checkRegister(int index) {
        if (index < 0 || index >= REGISTERS) {
            throw new IllegalArgumentException("Registro inválido: R" + index);
        }
    }

    private void checkAddress(int address) {
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new IllegalArgumentException("Dirección inválida: " + address);
        }
    }
}

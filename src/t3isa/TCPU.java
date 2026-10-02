/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package t3isa;

/**
 *
 * @author sistemas
 */
public final class TCPU {

    public static final int REGISTERS = 27;
    public static final int MEMORY_SIZE = 19683;

    private final TWord[] registers;
    private final TWord[] memory;

    private TWord pc;
    private TWord sp;

    private int compare;

    private boolean halted;

    public TCPU() {
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
        halted = false;
    }

    public void reset() {
        for (int i = 0; i < REGISTERS; i++) {
            registers[i] = TWord.zero();
        }
        pc = TWord.zero();
        sp = TWord.fromLong(MEMORY_SIZE - 1);
        compare = 0;
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

    public void load(int address, TWord value) {
        checkAddress(address);
        memory[address] = value.copy();
    }

    public TWord read(int address) {
        checkAddress(address);
        return memory[address].copy();
    }

    public void step() {
        if (halted) {
            return;
        }

        int address = (int) pc.toLong();
        TWord rawInstruction = read(address);
        TInstruction instruction = TInstruction.decode(rawInstruction);
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
            default:
                throw new IllegalStateException("Instrucción aún no implementada: " + opcode);
        }
    }

    private void incrementPC() {
        pc = TALU.increment(pc);
    }

    private void jump(int address) {
        checkAddress(address);
        pc = TWord.fromLong(address);
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

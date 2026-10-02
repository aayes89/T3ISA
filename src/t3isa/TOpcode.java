/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package t3isa;

/**
 *
 * @author sistemas
 */
public enum TOpcode {

    NOP(0),
    HALT(1),
    MOV(10),
    MOVI(11),
    ADD(20),
    SUB(21),
    NEG(22),
    MUL(23),
    CMP(30),
    TAND(40),
    TOR(41),
    TXOR(42),
    TNOT(43),
    JMP(50),
    JNEG(51),
    JZERO(52),
    JPOS(53),
    LOAD(60),
    STORE(61),
    PUSH(70),
    POP(71),
    CALL(80),
    RET(81),
    SYS(90);

    public final int code;

    TOpcode(int code) {
        this.code = code;
    }

    public static TOpcode fromCode(int code) {

        for (TOpcode opcode : values()) {
            if (opcode.code == code) {
                return opcode;
            }
        }

        return null;
    }
}

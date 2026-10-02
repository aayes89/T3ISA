/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package t3isa;

/**
 *
 * @author sistemas
 */

public enum Trit {

    NEG(-1),
    ZERO(0),
    POS(1);

    public final int value;

    Trit(int value) {
        this.value = value;
    }

    public static Trit fromInt(int value) {
        if (value < 0) return NEG;
        if (value > 0) return POS;
        return ZERO;
    }

    public static Trit negate(Trit t) {
        return fromInt(-t.value);
    }
}
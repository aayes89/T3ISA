/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package t3isa;

/**
 *
 * @author sistemas T3ISA - Ternary 3-State Instruction Set Architecture
 */
public class T3ISA {

    public static void main(String[] args) {

        TCPU cpu = new TCPU();

        /*
         * R1 = 10
         * R2 = 20
         * R3 = R1 + R2
         */
        cpu.load(
                0,
                TAssembler.movi(1, 10)
        );

        cpu.load(
                1,
                TAssembler.movi(2, 20)
        );

        cpu.load(
                2,
                TAssembler.add(3, 1, 2)
        );

        cpu.load(
                3,
                TAssembler.halt()
        );

        while (!cpu.isHalted()) {
            cpu.step();
        }

        System.out.println(
                "R1 = "
                + cpu.getRegister(1).toLong()
        );

        System.out.println(
                "R2 = "
                + cpu.getRegister(2).toLong()
        );

        System.out.println(
                "R3 = "
                + cpu.getRegister(3).toLong()
        );
    }

}

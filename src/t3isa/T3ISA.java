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
 *
 * T3ISA - Ternary 3-State Instruction Set Architecture
 */
public class T3ISA {

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println(" TEST ENTER_USER / EXIT_USER");
        System.out.println("========================================");

        TCPU cpu = new TCPU();
        TKernel kernel = new TKernel(cpu, 100);

        TPCB process = kernel.createProcess(
                // USER
                "MOVI R7, 111\n"
                // USER -> KERNEL
                + "MOVI R1, 9\n"
                + "SYS\n"
                // KERNEL
                + "MOVI R7, 222\n"
                + "MOVI R1, 8\n"
                + "SYS\n"
                // USER
                + "MOVI R7, 333\n"
                + "MOVI R1, 12\n"
                + "SYS\n"
        );

        kernel.getScheduler().schedule(cpu);

        System.out.println("\n--- INICIO ---");
        System.out.println("PID=" + process.getPid());
        System.out.println("PC=" + cpu.getPC());
        System.out.println("SP=" + cpu.getSP());
        System.out.println("USER_SP=" + cpu.getUserSP());
        System.out.println("KERNEL=" + cpu.isKernelMode());

        boolean sawKernel = false;
        boolean returnedUser = false;

        for (int i = 0; i < 20 && !cpu.isHalted(); i++) {

            System.out.println(
                    "\nSTEP=" + i
                    + " PC=" + cpu.getPC()
                    + " SP=" + cpu.getSP()
                    + " USER_SP=" + cpu.getUserSP()
                    + " R7=" + cpu.getRegister(7).toLong()
                    + " KERNEL=" + cpu.isKernelMode()
            );

            kernel.step();

            if (cpu.isKernelMode()) {
                sawKernel = true;
            }

            if (sawKernel && !cpu.isKernelMode()) {
                returnedUser = true;
            }
        }

        System.out.println("\n========================================");
        System.out.println(" RESULTADO");
        System.out.println("========================================");

        System.out.println("HALTED=" + cpu.isHalted());
        System.out.println("TRAP=" + cpu.getTrap());
        System.out.println("KERNEL=" + cpu.isKernelMode());
        System.out.println("SAW KERNEL=" + sawKernel);
        System.out.println("RETURNED USER=" + returnedUser);
        System.out.println("R7=" + cpu.getRegister(7).toLong());

        if (!cpu.isHalted()
                || cpu.getTrap() != null
                || !sawKernel
                || !returnedUser) {

            throw new IllegalStateException(
                    "FALLO ENTER_USER / EXIT_USER"
            );
        }

        System.out.println("\nENTER_USER / EXIT_USER OK");
        System.out.println("BUILD SUCCESSFUL");
    }
}

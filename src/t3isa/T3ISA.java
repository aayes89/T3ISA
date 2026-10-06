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

import t3os.T3OS;
import t3isa.Core.TCPU;
import t3isa.Core.TWord;
import t3isa.DEVICE.TGraphicsDevice;
import t3isa.HARDWARE.TMachine;
import t3isa.HOST.TGraphicsHostBackend;
import test.TRenderizadorTexto;

/**
 *
 * @author Slam
 *
 * T3ISA - Ternary 3-State Instruction Set Architecture
 */
public class T3ISA {

    public static final boolean isDEBUG = true;

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println(" T3OS");
        System.out.println("========================================");

        TCPU cpu = new TCPU();
        TGraphicsDevice graphicsDevice = cpu.getGraphicsDevice();

        //TRenderizadorTexto renderText = new TRenderizadorTexto();
        //renderText.renderizarTexto(graphicsDevice, "T3SO - A 3state computer\nMade by Slam 2026\n\nGithub: aayes89/t3isa", 10, 100, 2, 0x00FF0000, 0x000000);
        TGraphicsHostBackend graphics = new TGraphicsHostBackend(graphicsDevice);

        graphics.open();
        T3OS os = new T3OS(cpu);

        // RESET -> BOOT -> KERNEL
        os.boot();

        TMachine machine = os.getMachine();

        System.out.println("Kernel entry: PC=" + machine.getPC());

        if (machine.getPC() != TMachine.OS_START) {
            throw new IllegalStateException("T3OS no llegó al kernel");
        }

        /*
        * Entrar directamente al shell.
        *
        * NO ejecutar os.run() aquí:
        * todavía no queremos que INIT termine
        * y haga halt de la máquina.
         */
        // Prueba de redes
        //TNetworkSyscallTest.run();
        // Lanzando shell
        os.shell();
    }
}

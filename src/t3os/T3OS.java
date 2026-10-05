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
package t3os;

import t3os.SHELL.TShell;
import t3isa.DEVICE.TConsoleDevice;
import t3os.KERNEL.TPCB;
import t3os.KERNEL.TKernel;
import t3os.BOOT.TBoot;
import t3os.FS.TFileSystem;
import t3isa.HARDWARE.TMachine;

/**
 *
 * @author Slam
 *
 * Punto de entrada de T3OS.
 */
public final class T3OS {

    private final TMachine machine;
    private final TKernel kernel;
    private final TBoot boot;
    private final TConsoleDevice console;
    private final TFileSystem fileSystem;

    public T3OS(TMachine machine) {
        if (machine == null) {
            throw new IllegalArgumentException("Machine no puede ser null");
        }

        this.machine = machine;
        boot = new TBoot(machine);
        kernel = new TKernel(machine, 10);
        machine.setKernel(kernel);
        console = new TConsoleDevice();
        fileSystem = new TFileSystem();
        machine.getDeviceBus().attach(0, console);
    }

    public void boot() {
        // Primero reset del hardware.
        boot.reset();

        // Después instalar el contenido del boot.
        boot.install();

        // Configurar los vectores de interrupción/trap en TMachine
        kernel.initialize();

        // El hardware arranca desde el reset vector.
        boot.resetVector();
        System.out.println("T3OS BOOT PC=" + machine.getPC());

        // Ejecutar BOOT.
        machine.step();
        System.out.println("T3OS KERNEL ENTRY PC=" + machine.getPC());

        if (machine.getPC() != TMachine.OS_START) {
            throw new IllegalStateException("BOOT no transfirió control a T3OS");
        }
    }

    public TPCB startInit(String source) {
        if (machine.getPC() != TMachine.OS_START) {
            throw new IllegalStateException("T3OS todavía no está en OS_START");
        }

        if (!machine.isKernelMode()) {
            throw new IllegalStateException("INIT debe crearse desde kernel mode");
        }

        // El kernel crea el primer proceso.
        TPCB init = kernel.createProcess(source);

        // El scheduler selecciona INIT.
        kernel.getScheduler().schedule(machine);

        if (kernel.getScheduler().getCurrentProcess() != init) {
            throw new IllegalStateException("INIT no fue seleccionado");
        }

        /*
         * El contexto restaurado por el scheduler
         * coloca la CPU en USER MODE.
         */
        if (machine.isKernelMode()) {
            throw new IllegalStateException("INIT no entró en USER MODE");
        }

        return init;
    }

    public void shell() {
        TShell shell = new TShell(machine, kernel, console);
        shell.start();
    }

    public void run() {
        kernel.run();
    }

    public TKernel getKernel() {
        return kernel;
    }

    public TBoot getBoot() {
        return boot;
    }

    public TFileSystem getFileSystem() {
        return fileSystem;
    }

    public TMachine getMachine() {
        return machine;
    }
}

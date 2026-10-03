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
package t3isa;

/**
 *
 * @author Slam
 */
public final class TShell {

    private final TCPU cpu;
    private final TKernel kernel;
    private final TConsoleDevice console;

    public TShell(TCPU cpu, TKernel kernel, TConsoleDevice console) {
        this.cpu = cpu;
        this.kernel = kernel;
        this.console = console;
    }

    public void start() {
        console.writeLine("");
        console.writeLine("================================");
        console.writeLine(" T3OS");
        console.writeLine("================================");
        console.writeLine("Kernel initialized.");
        console.writeLine("");

        while (!cpu.isHalted()) {
            console.writeText("t3os> ");
            String line = console.readLine();
            if (line == null) {
                break;
            }

            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            if (!execute(line)) {
                break;
            }
        }
    }

    private boolean execute(String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0].toLowerCase();
        switch (command) {

            case "help":
                help();
                return true;

            case "ps":
                ps();
                return true;

            case "mem":
                mem();
                return true;

            case "echo":
                echo(line);
                return true;

            case "clear":
                clear();
                return true;

            case "run":
                run(parts);
                return true;

            case "exit":
                console.writeLine("shutdown");
                cpu.halt();
                return false;

            default:
                console.writeLine(
                        "command not found: " + command
                );
                return true;
        }
    }

    private void help() {
        console.writeLine("");
        console.writeLine("commands:");
        console.writeLine("  help");
        console.writeLine("  ps");
        console.writeLine("  mem");
        console.writeLine("  echo <text>");
        console.writeLine("  run <program>");
        console.writeLine("  clear");
        console.writeLine("  exit");
        console.writeLine("");
    }

    private void ps() {
        console.writeLine("");
        console.writeLine("PID     STATE");

        for (TPCB pcb : kernel.getScheduler().getProcesses()) {
            console.writeLine(pcb.getPid() + "       " + pcb.getState());
        }

        console.writeLine("");
    }

    private void mem() {
        console.writeLine("");
        console.writeLine("KERNEL    0.." + TCPU.KERNEL_MEMORY_END);
        console.writeLine("USER      " + TCPU.USER_MEMORY_START + ".." + (TCPU.KERNEL_STACK_TOP - 1));
        console.writeLine("KSTACK    " + TCPU.KERNEL_STACK_BOTTOM + ".." + TCPU.KERNEL_STACK_TOP);
        console.writeLine("USTACK    " + TCPU.USER_STACK_BOTTOM + ".." + TCPU.USER_STACK_TOP);
        console.writeLine("");
    }

    private void echo(String line) {

        if (line.length() <= 4) {
            console.writeLine("");
            return;
        }

        console.writeLine(line.substring(5));
    }

    private void clear() {
        for (int i = 0; i < 40; i++) {
            console.writeLine("");
        }
    }

    private void run(String[] parts) {

        if (parts.length < 2) {
            console.writeLine("usage: run <program>");
            return;
        }

        String program = parts[1].toLowerCase();
        String source;

        switch (program) {
            case "hello":
                source
                        = "MOVI R2, 0\n"
                        + "MOVI R3, 72\n"
                        + "MOVI R1, 6\n"
                        + "SYS\n"
                        + "MOVI R3, 101\n"
                        + "MOVI R1, 6\n"
                        + "SYS\n"
                        + "MOVI R3, 108\n"
                        + "MOVI R1, 6\n"
                        + "SYS\n"
                        + "MOVI R3, 108\n"
                        + "MOVI R1, 6\n"
                        + "SYS\n"
                        + "MOVI R3, 111\n"
                        + "MOVI R1, 6\n"
                        + "SYS\n"
                        + "MOVI R3, 10\n"
                        + "MOVI R1, 6\n"
                        + "SYS\n"
                        + "MOVI R1, 12\n"
                        + "SYS\n";

                break;

            default:
                console.writeLine("program not found: " + program);
                return;
        }

        kernel.createProcess(source);
        console.writeLine("program '" + program + "' started");

        if (kernel.getScheduler().getCurrentProcess() == null) {
            kernel.getScheduler().schedule(cpu);
        }

        while (!cpu.isHalted() && kernel.getScheduler().hasReadyProcesses()) {
            kernel.step();
        }

        console.writeLine("");
    }
}

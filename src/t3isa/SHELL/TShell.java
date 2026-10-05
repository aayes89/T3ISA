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
package t3isa.SHELL;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import t3isa.DEVICE.TConsoleDevice;
import t3isa.KERNEL.TPCB;
import t3isa.KERNEL.TKernel;
import t3isa.Core.TCPU;
import t3isa.Core.TWord;
import t3isa.DEVICE.TNetworkDevice;
import t3isa.Exceptions.TMemoryException;
import t3isa.FS.TFileSystem;
import t3isa.FS.TVFS;
import t3isa.NETWORKING.TARP;
import t3isa.NETWORKING.THostNetwork;
import t3isa.NETWORKING.TICMP;
import t3isa.NETWORKING.TIPv4;
import t3isa.NETWORKING.TTCP;

/**
 *
 * @author Slam
 */
public final class TShell {

    private final TCPU cpu;
    private final TKernel kernel;
    private final TConsoleDevice console;
    private String currentDirectory = "/";

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

            case "peek":
                peek(parts);
                return true;

            case "poke":
                poke(parts);
                return true;

            case "regs":
                regs();
                return true;

            case "cpu":
                cpu();
                return true;

            case "ls":
                ls();
                return true;

            case "touch":
                touch(parts);
                return true;

            case "write":
                write(parts);
                return true;

            case "cat":
                cat(parts);
                return true;

            case "rm":
                rm(parts);
                return true;

            case "fs":
                fs();
                return true;

            case "mkdir":
                mkdir(parts);
                return true;

            case "cd":
                cd(parts);
                return true;

            case "pwd":
                pwd();
                return true;

            case "clear":
                clear();
                return true;

            case "run":
                run(parts);
                return true;

            case "ifconfig":
                ifconfig();
                return true;

            case "ping":
                ping(parts);
                return true;

            case "nslookup":
                nslookup(parts);
                return true;

            case "whois":
                whois(parts);
                return true;

            case "wget":
                wget(parts);
                return true;

            case "arp":
                arp(parts);
                return true;

            case "nc":
                nc(parts);
                return true;

            case "exit":
                console.writeLine("shutdown");
                cpu.halt();
                return false;

            default:
                console.writeLine("command not found: " + command);
                return true;
        }
    }

    private void help() {
        console.writeLine("");
        console.writeLine("Commands:");
        console.writeLine("  help");
        console.writeLine("  ps");
        console.writeLine("  mem");
        console.writeLine("  peek <address>");
        console.writeLine("  poke <address> <value>");
        console.writeLine("  regs");
        console.writeLine("  cpu");
        console.writeLine("  echo <text>");
        console.writeLine("  run <program>");
        console.writeLine("  mkdir <directory>");
        console.writeLine("  cd <directory>");
        console.writeLine("  pwd");
        console.writeLine("  ls");
        console.writeLine("  touch <file>");
        console.writeLine("  write <file> <text>");
        console.writeLine("  cat <file>");
        console.writeLine("  rm <file>");
        console.writeLine("  fs");
        console.writeLine("  clear");
        console.writeLine("  ifconfig");
        console.writeLine("  arp");
        console.writeLine("  arp <ip>");
        console.writeLine("  ping <host>");
        console.writeLine("  nslookup <host>");
        console.writeLine("  whois <domain>");
        console.writeLine("  wget <url>");
        console.writeLine("  nc <host> <port>");
        console.writeLine("  nc -l <port>");
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
        console.writeLine("MEMORY MAP");
        console.writeLine("KERNEL    0.." + TCPU.KERNEL_MEMORY_END);
        console.writeLine("USER      " + TCPU.USER_MEMORY_START + ".." + (TCPU.KERNEL_STACK_TOP - 1));
        console.writeLine("KERNEL STACK    " + TCPU.KERNEL_STACK_BOTTOM + ".." + TCPU.KERNEL_STACK_TOP);
        console.writeLine("USER STACK    " + TCPU.USER_STACK_BOTTOM + ".." + TCPU.USER_STACK_TOP);
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
        String source = "";

        switch (program) {
            case "hello":
                source = "MOVI R2, 0\n"
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

    private void peek(String[] parts) {
        if (parts.length != 2) {
            console.writeLine("usage: peek <address>");
            return;
        }

        try {
            int address = Integer.parseInt(parts[1]);
            TWord value = cpu.readMemory(address);
            console.writeLine("[" + address + "] = " + value.toLong());
        } catch (NumberFormatException e) {
            console.writeLine("invalid address");
        } catch (TMemoryException e) {
            console.writeLine("invalid memory address");
        }
    }

    private void poke(String[] parts) {
        if (parts.length != 3) {
            console.writeLine("usage: poke <address> <value>");
            return;
        }

        try {
            int address = Integer.parseInt(parts[1]);
            long value = Long.parseLong(parts[2]);
            cpu.writeMemory(address, TWord.fromLong(value));
            console.writeLine("[" + address + "] <- " + value);

        } catch (NumberFormatException e) {
            console.writeLine("invalid number");
        } catch (TMemoryException e) {
            console.writeLine("invalid memory address");
        }
    }

    private void regs() {
        console.writeLine("");
        console.writeLine("REGISTERS");

        for (int i = 0; i < TCPU.REGISTER_COUNT; i++) {
            console.writeLine("R" + i + " = " + cpu.getRegister(i).toLong());
        }

        console.writeLine("");
    }

    private void cpu() {
        console.writeLine("");
        console.writeLine("CPU");
        console.writeLine("PC      = " + cpu.getPC());
        console.writeLine("SP      = " + cpu.getSP());
        console.writeLine("PID     = " + cpu.getCurrentPid());
        console.writeLine("KERNEL  = " + cpu.isKernelMode());
        console.writeLine("TRAP    = " + cpu.getTrap());
        console.writeLine("HALTED  = " + cpu.isHalted());
        console.writeLine("");
    }

    private void cd(String[] parts) {
        if (parts.length != 2) {
            console.writeLine("usage: cd <directory>");
            return;
        }

        String path = resolvePath(parts[1]);

        if (!kernel.getVFS().isDirectory(path)) {
            console.writeLine("directory not found: " + path);
            return;
        }

        currentDirectory = path;
    }

    private void ls() {
        try {
            TVFS.TFileInfo[] files = kernel.getVFS().list(currentDirectory);

            if (files.length == 0) {
                console.writeLine("filesystem empty");
                return;
            }

            console.writeLine("");
            console.writeLine("NAME                 TYPE      SIZE      BLOCKS");

            for (TVFS.TFileInfo file : files) {
                String type = file.isDirectory() ? "DIR" : "FILE";
                console.writeLine(String.format("%-20s %-9s %-9d %d", file.getName(), type, file.getSize(), file.getBlocks()));
            }

            console.writeLine("");

        } catch (RuntimeException e) {
            console.writeLine(e.getMessage());
        }
    }

    private void mkdir(String[] parts) {
        if (parts.length != 2) {
            console.writeLine("usage: mkdir <directory>");
            return;
        }

        try {
            String path = resolvePath(parts[1]);
            kernel.getVFS().mkdir(path);
            console.writeLine("directory created: " + path);
        } catch (RuntimeException e) {
            console.writeLine(e.getMessage());
        }
    }

    private void touch(String[] parts) {
        if (parts.length != 2) {
            console.writeLine("usage: touch <file>");
            return;
        }

        try {
            String path = resolvePath(parts[1]);
            kernel.getVFS().create(path);
            console.writeLine("created: " + path);
        } catch (RuntimeException e) {
            console.writeLine(e.getMessage());
        }
    }

    private void write(String[] parts) {
        if (parts.length < 3) {
            console.writeLine("usage: write <file> <text>");
            return;
        }

        StringBuilder text = new StringBuilder();
        for (int i = 2; i < parts.length; i++) {
            if (i > 2) {
                text.append(' ');
            }
            text.append(parts[i]);
        }

        String content = text.toString();
        if (content.length() >= 2 && content.startsWith("\"") && content.endsWith("\"")) {
            content = content.substring(1, content.length() - 1);
        }

        try {
            String path = resolvePath(parts[1]);
            kernel.getVFS().write(path, content);
            console.writeLine("written: " + path);
        } catch (RuntimeException e) {
            console.writeLine(e.getMessage());
        }
    }

    private void cat(String[] parts) {
        if (parts.length != 2) {
            console.writeLine("usage: cat <file>");
            return;
        }

        try {
            console.writeLine(kernel.getVFS().read(resolvePath(parts[1])));
        } catch (RuntimeException e) {
            console.writeLine(e.getMessage());
        }
    }

    private void rm(String[] parts) {
        if (parts.length != 2) {
            console.writeLine("usage: rm <file>");
            return;
        }

        try {
            String path = resolvePath(parts[1]);
            kernel.getVFS().delete(path);
            console.writeLine("removed: " + path);
        } catch (RuntimeException e) {
            console.writeLine(e.getMessage());
        }
    }

    private void fs() {
        TVFS vfs = kernel.getVFS();

        console.writeLine("");
        console.writeLine("FILESYSTEM");
        console.writeLine("BLOCK SIZE  = " + TFileSystem.BLOCK_SIZE);
        console.writeLine("BLOCKS      = " + vfs.getTotalBlocks());
        console.writeLine("USED        = " + vfs.getUsedBlocks());
        console.writeLine("FREE        = " + vfs.getFreeBlocks());
        console.writeLine("");
    }

    private void pwd() {
        console.writeLine(currentDirectory);
    }

    private String resolvePath(String path) {
        if (path == null || path.isEmpty()) {
            return currentDirectory;
        }

        if (path.equals(".")) {
            return currentDirectory;
        }

        if (path.equals("..")) {
            return parentDirectory(currentDirectory);
        }

        if (path.equals("/")) {
            return "/";
        }

        String result;

        if (path.startsWith("/")) {
            result = path;
        } else if ("/".equals(currentDirectory)) {
            result = "/" + path;
        } else {
            result = currentDirectory + "/" + path;
        }

        while (result.contains("//")) {
            result = result.replace("//", "/");
        }

        if (result.length() > 1 && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    private String parentDirectory(String path) {
        if ("/".equals(path)) {
            return "/";
        }

        int index = path.lastIndexOf('/');

        if (index <= 0) {
            return "/";
        }

        return path.substring(0, index);
    }

    private void ifconfig_old() {
        // Para la abstracción de red cuando esté en modo ASM
        TNetworkDevice device = kernel.getNetworkDevice();
        byte[] mac = device.getMAC();

        console.writeLine("");
        console.writeLine("NETWORK INTERFACE");
        console.writeLine("  MAC      = " + formatMAC(mac));
        console.writeLine("  STATUS   = " + (device.hasPacket() ? "RX" : "UP"));
        console.writeLine("");
    }

    private void ifconfig() {
        try {
            List<NetworkInterface> interfaces = THostNetwork.getInterfaces();

            console.writeLine("");
            console.writeLine("HOST NETWORK INTERFACES");

            for (NetworkInterface ni : interfaces) {
                console.writeLine("");
                console.writeLine(ni.getName() + "  " + ni.getDisplayName());
                String mac = THostNetwork.getMAC(ni);

                console.writeLine("  MAC      = " + (mac != null ? mac : "N/A"));

                for (String address : THostNetwork.getAddresses(ni)) {
                    console.writeLine("  ADDRESS  = " + address);
                }
            }

            console.writeLine("");

        } catch (IOException e) {
            console.writeLine("ifconfig: " + e.getMessage());
        }
    }

    private void ping(String[] args) {
        if (args.length < 2) {
            console.writeLine("usage: ping <host>");
            return;
        }

        String host = args[1];

        try {
            InetAddress address = InetAddress.getByName(host);
            byte[] destinationIP = address.getAddress();

            if (destinationIP.length != 4) {
                console.writeLine("ping: IPv4 requerida");
                return;
            }

            console.writeLine("");
            console.writeLine("PING " + host + " (" + TIPv4.ipToString(destinationIP) + ")");

            TICMP icmp = kernel.getICMP();
            for (int i = 0; i < 4; i++) {
                long start = System.nanoTime();

                try {
                    TARP arp = kernel.getARP();
                    byte[] mac = arp.resolve(destinationIP);

                    if (mac == null) {
                        arp.request(destinationIP);
                        long arpDeadline = System.currentTimeMillis() + 3000;

                        while (System.currentTimeMillis() < arpDeadline) {
                            kernel.step();

                            mac = arp.resolve(destinationIP);
                            if (mac != null) {
                                break;
                            }
                            Thread.yield();
                        }
                    }

                    if (mac == null) {
                        console.writeLine("Request timeout.");
                        continue;
                    }

                    icmp.ping(destinationIP);
                    long deadline = System.currentTimeMillis() + 3000;

                    TICMP.Echo reply = null;

                    while (System.currentTimeMillis() < deadline) {
                        kernel.step();
                        reply = icmp.receive();

                        if (reply != null) {
                            break;
                        }

                        Thread.yield();
                    }
                    long elapsed = (System.nanoTime() - start) / 1_000_000;

                    if (reply != null) {
                        console.writeLine("Reply from " + TIPv4.ipToString(reply.getSourceIP()) + ": time=" + elapsed + " ms");
                    } else {
                        console.writeLine("Request timeout.");
                    }
                } catch (Exception e) {
                    console.writeLine("ping: " + e.getMessage());
                }
            }
            console.writeLine("");
        } catch (IOException e) {
            console.writeLine("ping: " + e.getMessage());
        }
    }

    private void nslookup(String[] args) {

        if (args.length < 1) {
            console.writeLine("usage: nslookup <host>");
            return;
        }

        try {
            InetAddress[] addresses = THostNetwork.resolveAll(args[1]);

            console.writeLine("");
            console.writeLine("NSLOOKUP " + args[1]);

            for (InetAddress address : addresses) {
                console.writeLine("  " + address.getHostAddress());
            }

            console.writeLine("");
        } catch (Exception e) {
            console.writeLine("nslookup: " + e.getMessage());
        }
    }

    private void whois(String[] args) {
        if (args.length < 1) {
            console.writeLine("usage: whois <domain>");
            return;
        }

        try {
            String result = THostNetwork.whois(args[1], "whois.iana.org", 43);
            console.writeLine(result);
        } catch (IOException e) {
            console.writeLine("whois: " + e.getMessage());
        }
    }

    private void wget(String[] args) {
        if (args.length < 1) {
            console.writeLine("usage: wget <url>");
            return;
        }

        try {
            String result = THostNetwork.wget(args[1]);
            console.writeLine(result);
        } catch (Exception e) {
            console.writeLine("wget: " + e.getMessage());
        }
    }

    private void nc(String[] parts) {

        boolean listen = false;
        boolean verbose = false;
        boolean reverse = false;

        int port = -1;
        String host = null;
        String inputFile = null;
        String outputFile = null;

        for (int i = 1; i < parts.length; i++) {
            String arg = parts[i];
            if ("-l".equals(arg)) {
                listen = true;
                continue;
            }

            if ("-p".equals(arg)) {
                if (++i >= parts.length) {
                    console.writeLine("nc: missing port");
                    return;
                }

                try {
                    port = Integer.parseInt(parts[i]);
                } catch (NumberFormatException e) {
                    console.writeLine("nc: invalid port");
                    return;
                }
                continue;
            }

            if ("-v".equals(arg)) {
                verbose = true;
                continue;
            }

            if ("-r".equals(arg)) {
                reverse = true;
                continue;
            }

            if ("<".equals(arg)) {
                if (++i >= parts.length) {
                    console.writeLine("nc: missing input file");
                    return;
                }
                inputFile = resolvePath(parts[i]);
                continue;
            }

            if (">".equals(arg)) {
                if (++i >= parts.length) {
                    console.writeLine("nc: missing output file");
                    return;
                }
                outputFile = resolvePath(parts[i]);
                continue;
            }

            // Opciones agrupadas: -l -lp -lv -lpv -v -r
            if (arg.startsWith("-") && !"-".equals(arg)) {
                if (arg.length() > 2 && !arg.startsWith("-p")) {
                    boolean valid = true;

                    for (int j = 1; j < arg.length(); j++) {
                        char option = arg.charAt(j);
                        switch (option) {

                            case 'l':
                                listen = true;
                                break;

                            case 'v':
                                verbose = true;
                                break;

                            case 'r':
                                reverse = true;
                                break;

                            case 'p':
                                //-lpv no puede contener -p sin valor dentro del mismo argumento.
                                if (j + 1 < arg.length()) {
                                    try {
                                        port = Integer.parseInt(arg.substring(j + 1));
                                    } catch (NumberFormatException e) {
                                        console.writeLine("nc: invalid port");
                                        return;
                                    }

                                    j = arg.length();
                                } else {
                                    if (++i >= parts.length) {
                                        console.writeLine("nc: missing port");
                                        return;
                                    }

                                    try {
                                        port = Integer.parseInt(parts[i]);
                                    } catch (NumberFormatException e) {
                                        console.writeLine("nc: invalid port");
                                        return;
                                    }
                                }
                                break;

                            default:
                                valid = false;
                                break;
                        }

                        if (!valid) {
                            break;
                        }
                    }

                    if (valid) {
                        continue;
                    }
                }

                console.writeLine("nc: unknown option " + arg);
                return;
            }

            if (host == null) {
                host = arg;
                continue;
            }

            if (port == -1) {
                try {
                    port = Integer.parseInt(arg);
                } catch (NumberFormatException e) {
                    console.writeLine("nc: invalid port");
                    return;
                }
                continue;
            }
            console.writeLine("nc: too many arguments");
            return;
        }

        if (reverse) {
            console.writeLine("nc: -r not implemented");
            return;
        }

        if (port < 1 || port > 65535) {
            console.writeLine("nc: invalid port");
            return;
        }

        if (listen) {
            if (host != null) {
                console.writeLine("nc: host not valid in listen mode");
                return;
            }

            ncListen(port, verbose, outputFile);
            return;
        }

        if (host == null) {
            console.writeLine("nc: missing host");
            return;
        }

        ncConnect(host, port, verbose, inputFile, outputFile);
    }

    private void ncTransfer(TTCP.Connection connection, String inputFile, String outputFile) {
        byte[] inputData = null;
        if (inputFile != null) {
            try {
                String content = kernel.getVFS().read(inputFile);
                inputData = content.getBytes(StandardCharsets.UTF_8);
            } catch (RuntimeException e) {
                console.writeLine("nc: " + e.getMessage());
                connection.close();
                return;
            }
        }

        if (outputFile != null) {
            try {
                touch(new String[]{"touch", outputFile});
            } catch (RuntimeException e) {
                console.writeLine("nc: " + e.getMessage());
                connection.close();
                return;
            }
        }

        StringBuilder outputContent = new StringBuilder();

        int offset = 0;
        boolean inputFinished = inputData == null;

        while (!connection.isClosed()) {
            kernel.step();

            if (inputData != null && offset < inputData.length) {
                int length = Math.min(1024, inputData.length - offset);
                byte[] block = new byte[length];

                System.arraycopy(inputData, offset, block, 0, length);

                connection.send(block);

                offset += length;

                if (offset >= inputData.length) {
                    inputData = null;
                    inputFinished = true;
                }
            }

            while (connection.hasData()) {
                byte[] data = connection.receive();
                if (data == null) {
                    break;
                }

                if (outputFile != null) {
                    outputContent.append(new String(data, StandardCharsets.UTF_8));
                } else {
                    console.writeText(new String(data, StandardCharsets.UTF_8));
                }
            }

            // Cerrar cuando terminó de enviarlo.
            if (inputFinished && inputFile != null) {
                connection.close();
                inputFile = null;
            }
        }

        if (outputFile != null) {
            try {
                kernel.getVFS().write(outputFile, outputContent.toString());
            } catch (RuntimeException e) {
                console.writeLine("nc: " + e.getMessage());
            }
        }
    }

    private void ncConnect(String host, int port, boolean verbose, String inputFile, String outputFile) {
        try {
            byte[] ip = InetAddress.getByName(host).getAddress();
            if (ip.length != 4) {
                console.writeLine("nc: IPv4 requerida");
                return;
            }

            TTCP tcp = kernel.getTCP();
            TTCP.Connection connection = tcp.connect(ip, port);

            if (verbose) {
                console.writeLine("Connection to " + host + " " + port + " initiated.");
            }

            while (!connection.isEstablished() && !connection.isClosed()) {
                kernel.step();
            }

            if (!connection.isEstablished()) {
                console.writeLine("nc: connection failed");
                return;
            }

            if (verbose) {
                console.writeLine("Connection to " + host + " " + port + " established.");
            }

            ncTransfer(connection, inputFile, outputFile);
        } catch (UnknownHostException e) {
            console.writeLine("nc: " + e.getMessage());
        }
    }

    private void ncListen(int port, boolean verbose, String outputFile) {

        try {
            TTCP tcp = kernel.getTCP();
            TTCP.Listener listener = tcp.listen(port);

            if (verbose) {
                console.writeLine("Listening on port " + port + "...");
            }

            TTCP.Connection connection;
            while ((connection = listener.accept()) == null) {
                kernel.step();
            }

            if (verbose) {
                console.writeLine("Connection accepted from " + TIPv4.ipToString(connection.getRemoteIP()) + ":" + connection.getRemotePort());
            }

            ncTransfer(connection, null, outputFile);
        } catch (Exception e) {
            console.writeLine("nc: " + e.getMessage());
        }
    }

    private String formatMAC(byte[] mac) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            if (i > 0) {
                result.append(':');
            }

            int value = mac[i] & 0xFF;
            if (value < 16) {
                result.append('0');
            }
            result.append(Integer.toHexString(value));
        }
        return result.toString();
    }

    private void arp(String[] args) {
        TARP arp = kernel.getARP();
        if (args.length == 1) {
            Map<Integer, byte[]> cache = arp.getCache();

            console.writeLine("");
            console.writeLine("ARP TABLE");

            if (cache.isEmpty()) {
                console.writeLine("  <empty>");
            } else {
                for (Map.Entry<Integer, byte[]> entry : cache.entrySet()) {
                    console.writeLine("  " + TARP.intToIP(entry.getKey()) + " -> " + formatMAC(entry.getValue()));
                }
            }

            console.writeLine("");
            return;
        }

        if (args.length == 2) {
            byte[] ip = parseIP(args[1]);
            arp.request(ip);
            console.writeLine("ARP request enviado para " + TIPv4.ipToString(ip));

            return;
        }

        console.writeLine("Uso: arp");
        console.writeLine("     arp <ip>");
    }

    private byte[] parseIP(String value) {
        String[] parts = value.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("IPv4 inválida: " + value);
        }

        byte[] ip = new byte[4];
        for (int i = 0; i < 4; i++) {
            int n;

            try {
                n = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("IPv4 inválida: " + value);
            }

            if (n < 0 || n > 255) {
                throw new IllegalArgumentException("IPv4 inválida: " + value);
            }

            ip[i] = (byte) n;
        }
        return ip;
    }
}

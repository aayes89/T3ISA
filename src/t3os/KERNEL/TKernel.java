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
package t3os.KERNEL;

import java.util.ArrayDeque;
import java.util.Queue;
import t3os.MEMORY.TMemoryManager;
import t3isa.ISA.TInterrupt;
import t3isa.ISA.TTrap;
import t3isa.ISA.TOpcode;
import t3isa.ISA.TSyscall;
import t3isa.ISA.TAssembler;
import t3isa.ISA.TInstruction;
import t3isa.ISA.TAssemblerText;
import t3isa.CORE.TWord;
import t3isa.HOST.TNetworkBackend;
import t3isa.DEVICE.TNetworkDevice;
import t3isa.HOST.TNetworkHostBackend;
import t3os.FS.TFileSystem;
import t3os.FS.TVFS;
import t3isa.HARDWARE.TMachine;
import t3isa.NETWORKING.TARP;
import t3isa.NETWORKING.TEthernet;
import t3isa.NETWORKING.TICMP;
import t3isa.NETWORKING.TIPv4;
import t3isa.NETWORKING.TTCP;
import t3isa.T3ISA;

/**
 *
 * @author Slam
 */
public class TKernel {

    private final TMachine cpu;
    private final TScheduler scheduler;
    private final TMemoryManager memoryManager;
    private final TFileSystem fileSystem;
    private final TVFS vfs;
    private final TNetworkDevice networkDevice;
    private final TEthernet ethernet;
    private final TARP arp;
    private final TIPv4 ipv4;
    private final TICMP icmp;
    private final TTCP ttcp;
    private final Queue<byte[]> networkRxQueue = new ArrayDeque<>();
    private int timerTicks;
    private int networkTicks;
    private final int quantumTicks; // Cuántos pasos de CPU equivalen a 1 quántum/tic de temporizador

    public TKernel(TMachine cpu, int quantumTicks) {
        this(cpu, quantumTicks, new TNetworkHostBackend("./t3bpf", "en0"), new byte[]{(byte) 10, (byte) 10, (byte) 10, (byte) 100});
    }

    public TKernel(TMachine cpu, int quantumTicks, TNetworkBackend networkBackend, byte[] localIP) {
        this.cpu = cpu;
        this.scheduler = new TScheduler();
        this.quantumTicks = quantumTicks;
        this.memoryManager = new TMemoryManager();
        this.timerTicks = 0;
        this.networkTicks = 0;

        fileSystem = new TFileSystem();
        vfs = new TVFS(fileSystem);

        this.networkDevice = new TNetworkDevice(networkBackend);
        this.networkDevice.open();

        byte[] localMAC = networkDevice.getMAC();
        byte[] netmask = {(byte) 255, (byte) 255, (byte) 255, 0};
        byte[] gateway = {(byte) 10, (byte) 10, (byte) 10, (byte) 254};

        this.ethernet = new TEthernet(networkDevice);
        this.arp = new TARP(ethernet, localMAC, localIP);

        this.ipv4 = new TIPv4(ethernet, arp, localIP, netmask, gateway);
        this.icmp = new TICMP(ipv4);
        this.ttcp = new TTCP(ipv4);
    }

    // Configurar los vectores de interrupción/trap en TMachine
    public void initialize() {
        setupInterruptVectors();
        setupTrapHandlers();
        setupInterruptHandlers();
    }

    private void setupInterruptVectors() {
        cpu.loadInterruptVector(TInterrupt.TIMER, TMachine.INTERRUPT_HANDLER_TIMER);
        cpu.loadInterruptVector(TInterrupt.DEVICE, TMachine.INTERRUPT_HANDLER_DEVICE);

        cpu.loadTrapVector(TTrap.DIVIDE_BY_ZERO, TMachine.TRAP_HANDLER_DIV_ZERO);
        cpu.loadTrapVector(TTrap.INVALID_MEMORY, TMachine.TRAP_HANDLER_MEMORY);
        cpu.loadTrapVector(TTrap.INVALID_SYSCALL, TMachine.TRAP_HANDLER_SYSCALL);
        cpu.loadTrapVector(TTrap.DEVICE_ERROR, TMachine.TRAP_HANDLER_DEVICE);
        cpu.loadTrapVector(TTrap.INVALID_INSTRUCTION, TMachine.TRAP_HANDLER_INSTRUCTION);
        cpu.loadTrapVector(TTrap.STACK_ERROR, TMachine.TRAP_HANDLER_STACK);
    }

    private void setupInterruptHandlers() {
        TWord[] deviceHandler = TAssemblerText.assemble("IRET\n");
        TWord[] timerHandler = TAssemblerText.assemble("IRET\n");

        cpu.loadProgram(TMachine.INTERRUPT_HANDLER_DEVICE, deviceHandler);
        cpu.loadProgram(TMachine.INTERRUPT_HANDLER_TIMER, timerHandler);
    }

    /**
     * Carga un programa binario en el espacio de usuario y crea su proceso.
     *
     * @param binary
     * @return
     */
    public TPCB loadProcess(TWord[] binary) {
        return createProcess(binary);
    }

    // Crear procesos
    public TPCB createProcess(String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("El programa está vacío");
        }

        int estimatedSize = source.split("\\R").length;

        TMemoryManager.MemoryBlock block = memoryManager.allocate(estimatedSize);
        TMemoryManager.MemoryBlock stack = null;

        try {
            TWord[] binary = TAssemblerText.assemble(source, block.getBase());

            memoryManager.free(block.getBase());
            block = memoryManager.allocate(binary.length);
            stack = memoryManager.allocateStack(256);

            TWord[] relocated = TAssemblerText.assemble(source, block.getBase());
            cpu.loadProgram(block.getBase(), relocated);
            return scheduler.createProcess(block.getBase(), block.getBase(), block.getLimit(), stack.getBase(), stack.getLimit());
        } catch (RuntimeException e) {
            if (block != null) {
                memoryManager.free(block.getBase());
            }
            if (stack != null) {
                memoryManager.freeStack(stack.getBase());
            }
            throw e;
        }
    }

    public TPCB createProcess(TWord[] binary) {
        if (binary == null || binary.length == 0) {
            throw new IllegalArgumentException("El programa está vacío");
        }

        TMemoryManager.MemoryBlock block = memoryManager.allocate(binary.length);
        TMemoryManager.MemoryBlock stack = null;

        try {
            TWord[] relocated = relocateProgram(binary, block.getBase());

            stack = memoryManager.allocateStack(256);

            cpu.loadProgram(block.getBase(), relocated);
            return scheduler.createProcess(block.getBase(), block.getBase(), block.getLimit(), stack.getBase(), stack.getLimit());
        } catch (RuntimeException e) {
            memoryManager.free(block.getBase());

            if (stack != null) {
                memoryManager.freeStack(stack.getBase());
            }

            throw e;
        }
    }

    // Servicio de redes
    private void networkService() {
        if (!networkDevice.hasPacket()) {
            return;
        }

        byte[] frame = networkDevice.receiveFrame();
        if (frame == null || frame.length < 14) {
            return;
        }

        int etherType = ((frame[12] & 0xFF) << 8) | (frame[13] & 0xFF);
        if (T3ISA.isDEBUG) {
            System.out.println("NETWORK SERVICE: frame=" + frame.length + " etherType=0x" + Integer.toHexString(etherType));
        }

        switch (etherType) {
            case TEthernet.TYPE_ARP:
                if (T3ISA.isDEBUG) {
                    System.out.println("NETWORK SERVICE: ARP frame");
                }

                // ARP no requiere el mínimo Ethernet de 60 bytes
                if (frame.length < 42) {
                    if (T3ISA.isDEBUG) {
                        System.out.println("NETWORK SERVICE: ARP frame inválido: " + frame.length);
                    }
                    return;
                }

                byte[] arpFrame = frame;
                TEthernet.Frame arpEthernet = ethernet.receive(padEthernetFrame(arpFrame));
                arp.receive(arpEthernet);
                break;

            case TEthernet.TYPE_IPV4:
                if (T3ISA.isDEBUG) {
                    System.out.println("NETWORK SERVICE: IPv4 frame");
                }
                if (frame.length < TEthernet.MIN_FRAME_SIZE) {
                    if (T3ISA.isDEBUG) {
                        System.out.println("NETWORK SERVICE: IPv4 frame inválido: " + frame.length);
                    }
                    return;
                }

                TEthernet.Frame ipv4Frame = ethernet.receive(frame);
                ipv4.receive(ipv4Frame);
                //ttcp.receive();
                break;
            case TEthernet.TYPE_IPV6:
                // Ignorar paquetes IPv6
                break;

            default:
                if (T3ISA.isDEBUG) {
                    System.out.println("NETWORK SERVICE: EtherType desconocido 0x" + Integer.toHexString(etherType));
                }
                break;
        }

        networkRxQueue.add(frame);
    }

    private static byte[] padEthernetFrame(byte[] frame) {
        if (frame.length >= TEthernet.MIN_FRAME_SIZE) {
            return frame;
        }

        byte[] padded = new byte[TEthernet.MIN_FRAME_SIZE];
        System.arraycopy(frame, 0, padded, 0, frame.length);

        return padded;
    }

    // Avanza la CPU un ciclo de instrucción y simula el temporizador.
    public void step() {
        if (cpu.isHalted()) {
            return;
        }

        networkTicks++;

        if (networkTicks >= 1) {
            networkTicks = 0;
            networkService();
        }

        boolean deviceInterrupt = checkDeviceInterrupts();

        if (scheduler.getCurrentProcess() == null && scheduler.hasReadyProcesses()) {
            scheduler.schedule(cpu);
        }
        if (scheduler.getCurrentProcess() == null) {
            return;
        }

        timerTicks++;
        boolean timerInterrupt = false;

        if (timerTicks >= quantumTicks) {
            timerTicks = 0;

            TPCB current = scheduler.getCurrentProcess();

            if (!deviceInterrupt && current != null && !cpu.isKernelMode()) {
                timerInterrupt = true;
                cpu.requestInterrupt(TInterrupt.TIMER);
            }
        }

        cpu.step();
        if (T3ISA.isDEBUG) {
            System.out.println("DEBUG CPU: PC=" + cpu.getPC() + " SP=" + cpu.getSP() + " KERNEL=" + cpu.isKernelMode() + " TRAP=" + cpu.getTrap());
        }
        if (deviceInterrupt) {
            cpu.clearInterruptReturned();
            while (cpu.isKernelMode() && !cpu.isHalted()) {
                cpu.step();
            }

            if (cpu.wasInterruptReturned()) {
                TPCB current = scheduler.getCurrentProcess();

                if (current != null) {
                    current.saveContext(cpu);
                }
                cpu.clearInterruptReturned();
                scheduler.scheduleAfterInterrupt(cpu);
            }
            return;
        }

        if (timerInterrupt) {
            cpu.clearInterruptReturned();
            while (cpu.isKernelMode() && !cpu.isHalted()) {
                cpu.step();
            }

            if (cpu.wasInterruptReturned()) {
                TPCB current = scheduler.getCurrentProcess();
                if (current != null) {
                    current.saveContext(cpu);
                }

                cpu.clearInterruptReturned();
                scheduler.scheduleAfterInterrupt(cpu);
            }

            return;
        }

        int action = cpu.getPendingProcessAction();

        if (T3ISA.isDEBUG) {
            System.out.println("PENDING ACTION = " + action);
        }
        switch (action) {

            case TSyscall.YIELD:
                cpu.clearPendingProcessAction();
                scheduler.schedule(cpu);
                break;

            case TSyscall.BLOCK:
                int devicePort = cpu.getPendingDevicePort();

                cpu.clearPendingProcessAction();
                cpu.clearPendingDevicePort();

                scheduler.blockCurrentProcess(cpu, devicePort);
                break;

            case TSyscall.EXIT:
                cpu.clearPendingProcessAction();

                TPCB terminated = scheduler.terminateCurrentProcess(cpu);

                if (terminated != null) {
                    TPCB parent = scheduler.findParentOf(terminated);

                    if (parent != null && parent.getWaitingForPid() == terminated.getPid()) {
                        memoryManager.free(terminated.getMemoryBase());
                        memoryManager.freeStack(terminated.getStackBase());
                        scheduler.removeProcess(terminated);
                    }
                }

                if (scheduler.getCurrentProcess() == null) {
                    return;
                }
                break;

            case TSyscall.FORK:
                cpu.clearPendingProcessAction();
                forkCurrentProcess();
                break;

            case TSyscall.WAIT:
                cpu.clearPendingProcessAction();
                handleWait();
                break;

            case TSyscall.EXEC:
                cpu.clearPendingProcessAction();
                int execAddress = (int) cpu.getRegister(2).toLong();
                int execSize = (int) cpu.getRegister(3).toLong();

                TWord[] execBinary = new TWord[execSize];

                for (int i = 0; i < execSize; i++) {
                    execBinary[i] = cpu.readMemory(execAddress + i);
                }

                execCurrentProcess(execBinary);
                break;

            default:
                break;
        }
    }

    // Bucle principal de ejecución del sistema operativo.
    public void run() {
        while (!cpu.isHalted() && scheduler.hasReadyProcesses()) {
            step();
        }
    }

    private boolean checkDeviceInterrupts() {
        for (int port = 0; port < cpu.getDeviceBus().size(); port++) {
            if (!cpu.getDeviceBus().hasDevice(port)) {
                continue;
            }

            if (!cpu.getDeviceBus().hasInput(port)) {
                continue;
            }

            if (scheduler.hasBlockedProcesses(port)) {
                scheduler.unblockDevice(port);
                cpu.requestInterrupt(TInterrupt.DEVICE);
                return true;
            }
        }
        return false;
    }

    private void setupTrapHandlers() {

        TWord[] fatalHandler = TAssemblerText.assemble(
                "MOVI R1, 0\n"
                + "SYS\n"
        );

        cpu.loadProgram(TMachine.TRAP_HANDLER_DIV_ZERO, fatalHandler);
        cpu.loadProgram(TMachine.TRAP_HANDLER_MEMORY, fatalHandler);
        cpu.loadProgram(TMachine.TRAP_HANDLER_INSTRUCTION, fatalHandler);
        cpu.loadProgram(TMachine.TRAP_HANDLER_SYSCALL, fatalHandler);
        cpu.loadProgram(TMachine.TRAP_HANDLER_DEVICE, fatalHandler);
        cpu.loadProgram(TMachine.TRAP_HANDLER_STACK, fatalHandler);
    }

    public TScheduler getScheduler() {
        return scheduler;
    }

    private void forkCurrentProcess() {
        TPCB parent = scheduler.getCurrentProcess();

        if (parent == null) {
            cpu.clearPendingProcessAction();
            return;
        }

        int memorySize = parent.getMemoryLimit() - parent.getMemoryBase() + 1;
        int stackSize = parent.getStackLimit() - parent.getStackBase() + 1;

        TMemoryManager.MemoryBlock memoryBlock = null;
        TMemoryManager.MemoryBlock stackBlock = null;

        try {
            memoryBlock = memoryManager.allocate(memorySize);
            stackBlock = memoryManager.allocateStack(stackSize);

            // Copiar memoria del proceso y relocalizar
            // las direcciones absolutas de JMP/JNEG/JZERO/JPOS/CALL.
            int codeOffset = memoryBlock.getBase() - parent.getMemoryBase();

            for (int address = parent.getMemoryBase(); address <= parent.getMemoryLimit(); address++) {
                TWord word = cpu.readMemory(address);
                TInstruction instruction = TInstruction.decode(word);
                TOpcode opcode = instruction.getOpcode();

                if (opcode == TOpcode.JMP
                        || opcode == TOpcode.JNEG
                        || opcode == TOpcode.JZERO
                        || opcode == TOpcode.JPOS
                        || opcode == TOpcode.CALL) {

                    int target = instruction.getImmediate() + codeOffset;
                    word = TAssembler.encode(opcode, instruction.getDst(), instruction.getSrc1(), instruction.getSrc2(), target);
                }

                int destination = memoryBlock.getBase() + (address - parent.getMemoryBase());
                cpu.writeMemory(destination, word);
            }

            // Copiar stack.
            cpu.copyProcessStack(parent.getStackBase(), parent.getStackLimit(), stackBlock.getBase());

            // Crear PCB hijo.
            TPCB child = scheduler.forkProcess(parent, memoryBlock.getBase(), memoryBlock.getLimit(), stackBlock.getBase(), stackBlock.getLimit());

            // Relocalizar PC.
            int pcOffset = cpu.getPC() - parent.getMemoryBase();
            child.setPc(memoryBlock.getBase() + pcOffset);

            // Relocalizar SP.
            int childSP = stackBlock.getBase() + (cpu.getUserSP() - parent.getStackBase());
            child.setUserSP(childSP);

            // Copiar registros.
            for (int i = 0; i < TMachine.REGISTER_COUNT; i++) {
                child.setRegister(i, cpu.getRegister(i));
            }

            /*
         * Convención fork():
         * hijo recibe 0.
         * padre recibe PID del hijo.
             */
            child.setRegister(7, TWord.zero());
            cpu.setRegister(7, TWord.fromLong(child.getPid()));

        } catch (RuntimeException e) {
            if (memoryBlock != null) {
                memoryManager.free(memoryBlock.getBase());
            }
            if (stackBlock != null) {
                memoryManager.freeStack(stackBlock.getBase());
            }
            throw e;
        }
    }

    private void handleWait() {
        TPCB parent = scheduler.getCurrentProcess();
        if (parent == null) {
            return;
        }

        TPCB child = scheduler.findTerminatedChild(parent);
        if (child != null) {
            cpu.setRegister(7, TWord.fromLong(child.getPid()));

            memoryManager.free(child.getMemoryBase());
            memoryManager.freeStack(child.getStackBase());

            scheduler.removeProcess(child);

            return;
        }

        if (!scheduler.hasChild(parent)) {
            cpu.setRegister(7, TWord.fromLong(-1));
            return;
        }
        parent.setWaitingForPid(-1);
        scheduler.blockCurrentProcess(cpu);
    }

    private TWord[] relocateProgram(TWord[] binary, int memoryBase) {
        TWord[] relocated = new TWord[binary.length];

        for (int i = 0; i < binary.length; i++) {
            TInstruction instruction = TInstruction.decode(binary[i]);
            TOpcode opcode = instruction.getOpcode();

            if (opcode == TOpcode.JMP || opcode == TOpcode.JNEG || opcode == TOpcode.JZERO || opcode == TOpcode.JPOS || opcode == TOpcode.CALL) {
                int target = instruction.getImmediate() + memoryBase;
                relocated[i] = TAssembler.encode(opcode, instruction.getDst(), instruction.getSrc1(), instruction.getSrc2(), target);
            } else {
                relocated[i] = binary[i].copy();
            }
        }

        return relocated;
    }

    public void execCurrentProcess(TWord[] binary) {
        TPCB process = scheduler.getCurrentProcess();

        if (process == null) {
            throw new IllegalStateException("No hay proceso ejecutándose");
        }

        if (binary == null || binary.length == 0) {
            throw new IllegalArgumentException("El programa está vacío");
        }

        TMemoryManager.MemoryBlock newBlock = memoryManager.allocate(binary.length);

        try {
            TWord[] relocated = relocateProgram(binary, newBlock.getBase());
            cpu.loadProgram(newBlock.getBase(), relocated);
            int oldBase = process.getMemoryBase();

            process.setMemoryBase(newBlock.getBase());
            process.setMemoryLimit(newBlock.getLimit());
            process.setPc(newBlock.getBase());

            for (int i = 0; i < TMachine.REGISTER_COUNT; i++) {
                process.setRegister(i, TWord.zero());
            }

            process.setRegister(0, TWord.zero());
            process.setUserSP(process.getStackLimit());
            process.setCompare(0);

            cpu.restoreProcessContext(newBlock.getBase(), process.getRegisters(), process.getStackLimit(), 0, process.getStackBase(), process.getStackLimit());
            cpu.setProcessMemoryRange(newBlock.getBase(), newBlock.getLimit());
            memoryManager.free(oldBase);
        } catch (RuntimeException e) {
            memoryManager.free(newBlock.getBase());
            throw e;
        }
    }

    public TFileSystem getFileSystem() {
        return fileSystem;
    }

    public TVFS getVFS() {
        return vfs;
    }

    public TNetworkDevice getNetworkDevice() {
        return networkDevice;
    }

    public TARP getARP() {
        return arp;
    }

    public TIPv4 getIPv4() {
        return ipv4;
    }

    public TICMP getICMP() {
        return icmp;
    }

    public boolean hasNetworkFrame() {
        return !networkRxQueue.isEmpty();
    }

    public byte[] receiveNetworkFrame() {
        return networkRxQueue.poll();
    }

    public TTCP getTCP() {
        return ttcp;
    }
}

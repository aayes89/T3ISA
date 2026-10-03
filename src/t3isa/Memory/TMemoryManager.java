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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package t3isa.MEMORY;

import t3isa.Core.TCPU;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author Slam
 */
public class TMemoryManager {

    public static final class MemoryBlock {

        private final int base;
        private final int limit;

        public MemoryBlock(int base, int limit) {
            this.base = base;
            this.limit = limit;
        }

        public int getBase() {
            return base;
        }

        public int getLimit() {
            return limit;
        }

        public int getSize() {
            return limit - base + 1;
        }
    }

    private final int memoryStart;
    private final int memoryEnd;

    private final int stackStart;
    private final int stackEnd;

    private final List<MemoryBlock> allocatedBlocks;
    private final List<MemoryBlock> allocatedStacks;

    public TMemoryManager() {
        this(
                TCPU.USER_MEMORY_START,
                TCPU.KERNEL_STACK_BOTTOM - 1,
                TCPU.USER_STACK_BOTTOM,
                TCPU.USER_STACK_TOP
        );
    }

    public TMemoryManager(int memoryStart, int memoryEnd, int stackStart, int stackEnd) {
        if (memoryStart < 0 || memoryEnd >= TCPU.MEMORY_SIZE || memoryStart > memoryEnd) {
            throw new IllegalArgumentException("Rango de memoria inválido");
        }

        if (stackStart < 0 || stackEnd >= TCPU.MEMORY_SIZE || stackStart > stackEnd) {
            throw new IllegalArgumentException("Rango de stack inválido");
        }

        if (memoryEnd >= stackStart) {
            throw new IllegalArgumentException("Los rangos de memoria y stack se superponen");
        }

        this.memoryStart = memoryStart;
        this.memoryEnd = memoryEnd;
        this.stackStart = stackStart;
        this.stackEnd = stackEnd;

        this.allocatedBlocks = new ArrayList<>();
        this.allocatedStacks = new ArrayList<>();
    }

    /**
     * Reserva memoria para código/datos.
     */
    public MemoryBlock allocate(int size) {
        return allocate(size, memoryStart, memoryEnd, allocatedBlocks);
    }

    /**
     * Reserva memoria para el stack de un proceso.
     */
    public MemoryBlock allocateStack(int size) {
        return allocate(size, stackStart, stackEnd, allocatedStacks);
    }

    private MemoryBlock allocate(int size, int start, int end, List<MemoryBlock> blocks) {
        if (size <= 0) {
            throw new IllegalArgumentException("El tamaño debe ser mayor que cero");
        }

        blocks.sort(Comparator.comparingInt(MemoryBlock::getBase));
        int candidate = start;

        for (MemoryBlock block : blocks) {
            int candidateLimit = candidate + size - 1;

            if (candidateLimit < block.getBase()) {
                MemoryBlock allocated = new MemoryBlock(candidate, candidateLimit);
                blocks.add(allocated);
                return allocated;
            }

            candidate = block.getLimit() + 1;
        }

        int candidateLimit = candidate + size - 1;
        if (candidateLimit > end) {
            throw new IllegalStateException("Memoria insuficiente");
        }

        MemoryBlock allocated = new MemoryBlock(candidate, candidateLimit);
        blocks.add(allocated);

        return allocated;
    }

    /**
     * Libera un bloque de código/datos.
     */
    public void free(int base) {
        free(base, allocatedBlocks);
    }

    /**
     * Libera un bloque de stack.
     */
    public void freeStack(int base) {
        free(base, allocatedStacks);
    }

    private void free(int base, List<MemoryBlock> blocks) {

        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).getBase() == base) {
                blocks.remove(i);
                return;
            }
        }

        throw new IllegalArgumentException("No existe un bloque con base: " + base);
    }

    public boolean isAllocated(int address) {
        return isAllocated(address, allocatedBlocks) || isAllocated(address, allocatedStacks);
    }

    private boolean isAllocated(int address, List<MemoryBlock> blocks) {
        for (MemoryBlock block : blocks) {
            if (address >= block.getBase() && address <= block.getLimit()) {
                return true;
            }
        }

        return false;
    }

    public int getFreeMemory() {
        return getFreeMemory(memoryStart, memoryEnd, allocatedBlocks);
    }

    public int getFreeStack() {
        return getFreeMemory(stackStart, stackEnd, allocatedStacks);
    }

    private int getFreeMemory(int start, int end, List<MemoryBlock> blocks) {
        int free = end - start + 1;

        for (MemoryBlock block : blocks) {
            free -= block.getSize();
        }

        return free;
    }

    public int getAllocatedMemory() {
        int allocated = 0;

        for (MemoryBlock block : allocatedBlocks) {
            allocated += block.getSize();
        }

        return allocated;
    }

    public int getAllocatedStack() {
        int allocated = 0;

        for (MemoryBlock block : allocatedStacks) {
            allocated += block.getSize();
        }

        return allocated;
    }

    public List<MemoryBlock> getAllocatedBlocks() {
        return new ArrayList<>(allocatedBlocks);
    }

    public List<MemoryBlock> getAllocatedStacks() {
        return new ArrayList<>(allocatedStacks);
    }

    public int getMemoryStart() {
        return memoryStart;
    }

    public int getMemoryEnd() {
        return memoryEnd;
    }

    public int getStackStart() {
        return stackStart;
    }

    public int getStackEnd() {
        return stackEnd;
    }
}

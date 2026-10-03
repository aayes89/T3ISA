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
package t3isa.ISA;

/**
 *
 * @author Slam
 */
public final class TSyscall {

    private TSyscall() {
    }

    public static final int HALT = 0;
    public static final int GET_PC = 1;
    public static final int GET_SP = 2;
    public static final int GET_CMP = 3;
    public static final int MEM_READ = 4;
    public static final int MEM_WRITE = 5;
    public static final int DEVICE_OUT = 6;
    public static final int DEVICE_IN = 7;
    public static final int ENTER_USER = 8;
    public static final int EXIT_USER = 9;
    public static final int GETPID = 10;
    public static final int YIELD = 11;
    public static final int EXIT = 12;
    public static final int BLOCK = 13;
    public static final int FORK = 14;
    public static final int WAIT = 15;
    public static final int EXEC = 16;
}

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
import java.util.ArrayList;
import java.util.List;

public final class TAssemblerText {

    private TAssemblerText() {
    }

    public static TWord[] assemble(String source) {
        String[] lines = source.split("\\R");
        List<TWord> program = new ArrayList<>();
        for (String line : lines) {
            line = removeComment(line).trim();
            if (line.isEmpty()) {
                continue;
            }
            TWord instruction = parseInstruction(line);
            program.add(instruction);
        }
        return program.toArray(new TWord[0]);
    }

    private static String removeComment(String line) {
        int index = line.indexOf(';');
        if (index >= 0) {
            return line.substring(0, index);
        }
        return line;
    }

    private static TWord parseInstruction(String line) {
        String[] tokens = tokenize(line);
        if (tokens.length == 0) {
            throw new IllegalArgumentException("Instrucción vacía");
        }

        String mnemonic = tokens[0].toUpperCase();
        switch (mnemonic) {
            case "NOP":
                require(tokens, 1);
                return TAssembler.nop();

            case "HALT":
                require(tokens, 1);
                return TAssembler.halt();

            case "MOV":
                require(tokens, 3);
                return TAssembler.mov(register(tokens[1]), register(tokens[2]));

            case "MOVI":
                require(tokens, 3);
                return TAssembler.movi(register(tokens[1]), integer(tokens[2]));

            case "ADD":
                require(tokens, 4);
                return TAssembler.add(register(tokens[1]), register(tokens[2]), register(tokens[3]));

            case "SUB":
                require(tokens, 4);
                return TAssembler.sub(register(tokens[1]), register(tokens[2]), register(tokens[3]));

            case "NEG":
                require(tokens, 3);
                return TAssembler.neg(register(tokens[1]), register(tokens[2]));

            case "CMP":
                require(tokens, 3);
                return TAssembler.cmp(register(tokens[1]), register(tokens[2]));

            case "TAND":
                require(tokens, 4);
                return TAssembler.encode(TOpcode.TAND, register(tokens[1]), register(tokens[2]), register(tokens[3]), 0);

            case "TOR":
                require(tokens, 4);
                return TAssembler.encode(TOpcode.TOR, register(tokens[1]), register(tokens[2]), register(tokens[3]), 0);

            case "TNOT":
                require(tokens, 3);
                return TAssembler.encode(TOpcode.TNOT, register(tokens[1]), register(tokens[2]), 0, 0);

            case "JMP":
                require(tokens, 2);
                return TAssembler.jmp(integer(tokens[1]));

            case "JNEG":
                require(tokens, 2);
                return TAssembler.jneg(integer(tokens[1]));

            case "JZERO":
                require(tokens, 2);
                return TAssembler.jzero(integer(tokens[1]));

            case "JPOS":
                require(tokens, 2);
                return TAssembler.jpos(integer(tokens[1]));

            default:
                throw new IllegalArgumentException("Mnemonic desconocido: " + mnemonic);
        }
    }

    private static String[] tokenize(String line) {
        return line.replace(",", " ").trim().split("\\s+");
    }

    private static int register(String token) {
        token = token.toUpperCase();
        if (!token.startsWith("R")) {
            throw new IllegalArgumentException("Registro inválido: " + token);
        }

        try {
            int number = Integer.parseInt(token.substring(1));
            if (number < 0 || number >= 27) {
                throw new IllegalArgumentException("Registro fuera de rango: " + token);
            }
            return number;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Registro inválido: " + token);
        }
    }

    private static int integer(String token) {
        try {
            if (token.startsWith("0x") || token.startsWith("0X")) {
                return Integer.parseInt(token.substring(2), 16);
            }
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Número inválido: " + token);
        }
    }

    private static void require(String[] tokens, int expected) {
        if (tokens.length != expected) {
            throw new IllegalArgumentException("Número incorrecto de operandos. " + "Esperados: " + (expected - 1) + ", recibidos: " + (tokens.length - 1));
        }
    }
}

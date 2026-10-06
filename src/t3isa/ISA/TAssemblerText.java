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
import t3isa.Core.TWord;
import java.util.ArrayList;
import java.util.List;

public final class TAssemblerText {

    private TAssemblerText() {
    }

    public static TWord[] assemble(String source) {
        return assemble(source, 0);
    }

    public static TWord[] assemble(String source, int memoryBase) {
        int address = memoryBase;
        String[] lines = source.split("\\R");
        List<String> instructions = new ArrayList<>();
        java.util.Map<String, Integer> labels = new java.util.HashMap<>();

        /*
         * PASS 1
         *
         * Encontrar etiquetas.
         */
        for (String original : lines) {
            String line = removeComment(original).trim();

            if (line.isEmpty()) {
                continue;
            }

            while (line.contains(":")) {
                int colon = line.indexOf(':');
                String label = line.substring(0, colon).trim().toUpperCase();

                if (label.isEmpty()) {
                    throw new IllegalArgumentException("Etiqueta vacía");
                }

                if (labels.containsKey(label)) {
                    throw new IllegalArgumentException("Etiqueta duplicada: " + label);
                }

                labels.put(label, address);
                //labels.put(label, memoryBase + instructions.size());
                line = line.substring(colon + 1).trim();
                if (line.isEmpty()) {
                    break;
                }
            }

            /*if (!line.isEmpty()) {
                instructions.add(line);
            }*/
            if (!line.isEmpty()) {
                instructions.add(line);

                String[] tokens = tokenize(line);

                if (tokens[0].equalsIgnoreCase("CONST")) {
                    address += 2;
                } else {
                    address++;
                }
            }
        }

        /*
         * PASS 2
         */
        List<TWord> program = new ArrayList<>();
        for (String line : instructions) {
            String[] tokens = tokenize(line);
            String mnemonic = tokens[0].toUpperCase();

            if (isJump(mnemonic) && tokens.length == 2) {
                String operand = tokens[1].toUpperCase();

                if (labels.containsKey(operand)) {
                    tokens[1] = Integer.toString(labels.get(operand));
                    line = rebuild(tokens);
                }
            }

            //program.add(parseInstruction(line));
            program.addAll(parseInstruction(line));
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

    private static List<TWord> single(TWord word) {
        List<TWord> result = new ArrayList<>(1);
        result.add(word);
        return result;
    }

    private static long longInteger(String token) {
        try {
            if (token.startsWith("0x") || token.startsWith("0X")) {
                return Long.parseLong(token.substring(2), 16);
            }
            return Long.parseLong(token);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Número inválido: " + token);
        }
    }

    private static List<TWord> parseInstruction(String line) {
        String[] tokens = tokenize(line);
        if (tokens.length == 0) {
            throw new IllegalArgumentException("Instrucción vacía");
        }

        String mnemonic = tokens[0].toUpperCase();
        switch (mnemonic) {
            case "NOP":
                require(tokens, 1);
                return single(TAssembler.nop());

            case "HALT":
                require(tokens, 1);
                return single(TAssembler.halt());

            case "MOV":
                require(tokens, 3);
                return single(TAssembler.mov(register(tokens[1]), register(tokens[2])));

            case "MOVI":
                require(tokens, 3);
                return single(TAssembler.movi(register(tokens[1]), integer(tokens[2])));

            case "CONST":
                require(tokens, 3);
                List<TWord> result = new ArrayList<>(2);
                result.add(TAssembler.constInstruction(register(tokens[1])));
                result.add(TWord.fromLong(longInteger(tokens[2])));
                return result;
            case "ADD":
                require(tokens, 4);
                return single(TAssembler.add(register(tokens[1]), register(tokens[2]), register(tokens[3])));

            case "SUB":
                require(tokens, 4);
                return single(TAssembler.sub(register(tokens[1]), register(tokens[2]), register(tokens[3])));

            case "MUL":
                require(tokens, 4);
                return single(TAssembler.mul(register(tokens[1]), register(tokens[2]), register(tokens[3])));

            case "DIV":
                require(tokens, 4);
                return single(TAssembler.div(register(tokens[1]), register(tokens[2]), register(tokens[3])));

            case "MOD":
                require(tokens, 4);
                return single(TAssembler.mod(register(tokens[1]), register(tokens[2]), register(tokens[3])));

            case "SHL":
                require(tokens, 4);
                return single(TAssembler.shl(register(tokens[1]), register(tokens[2]), register(tokens[3])));

            case "SHR":
                require(tokens, 4);
                return single(TAssembler.shr(register(tokens[1]), register(tokens[2]), register(tokens[3])));

            case "NEG":
                require(tokens, 3);
                return single(TAssembler.neg(register(tokens[1]), register(tokens[2])));

            case "CMP":
                require(tokens, 3);
                return single(TAssembler.cmp(register(tokens[1]), register(tokens[2])));

            case "TAND":
                require(tokens, 4);
                return single(TAssembler.encode(TOpcode.TAND, register(tokens[1]), register(tokens[2]), register(tokens[3]), 0));

            case "TOR":
                require(tokens, 4);
                return single(TAssembler.encode(TOpcode.TOR, register(tokens[1]), register(tokens[2]), register(tokens[3]), 0));

            case "TNOT":
                require(tokens, 3);
                return single(TAssembler.encode(TOpcode.TNOT, register(tokens[1]), register(tokens[2]), 0, 0));

            case "JMP":
                require(tokens, 2);
                return single(TAssembler.jmp(integer(tokens[1])));

            case "JNEG":
                require(tokens, 2);
                return single(TAssembler.jneg(integer(tokens[1])));

            case "JZERO":
                require(tokens, 2);
                return single(TAssembler.jzero(integer(tokens[1])));

            case "JPOS":
                require(tokens, 2);
                return single(TAssembler.jpos(integer(tokens[1])));

            case "TXOR":
                require(tokens, 4);
                return single(TAssembler.txor(register(tokens[1]), register(tokens[2]), register(tokens[3])));

            case "LOAD":
                require(tokens, 4);
                return single(TAssembler.load(register(tokens[1]), register(tokens[2]), integer(tokens[3])));

            case "STORE":
                require(tokens, 4);
                return single(TAssembler.store(register(tokens[1]), register(tokens[2]), integer(tokens[3])));

            case "PUSH":
                require(tokens, 2);
                return single(TAssembler.push(register(tokens[1])));

            case "POP":
                require(tokens, 2);
                return single(TAssembler.pop(register(tokens[1])));

            case "CALL":
                require(tokens, 2);
                return single(TAssembler.call(integer(tokens[1])));

            case "SYS":
                require(tokens, 1);
                return single(TAssembler.encode(TOpcode.SYS, 0, 0, 0, 0));

            case "RET":
                require(tokens, 1);
                return single(TAssembler.ret());

            case "IRET":
                //require(tokens, 1);
                return single(TAssembler.encode(TOpcode.IRET, 0, 0, 0, 0));

            case "EXIT":
                require(tokens, 1);
                return single(TAssembler.encode(TOpcode.SYS, 0, 0, 0, TSyscall.EXIT));

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

    private static boolean isJump(String mnemonic) {
        return mnemonic.equals("JMP") || mnemonic.equals("JNEG") || mnemonic.equals("JZERO") || mnemonic.equals("JPOS") || mnemonic.equals("CALL");
    }

    private static String rebuild(String[] tokens) {
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(token);
        }
        return sb.toString();
    }
}

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
package t3os.UI;

import java.util.ArrayList;
import java.util.List;
import t3isa.DEVICE.TConsoleDevice;
import t3isa.DEVICE.TKeyboardDevice;
import t3isa.HARDWARE.TMachine;
import t3os.KERNEL.TKernel;
import t3os.SHELL.TShell;

/**
 *
 * @author Slam
 */
// Componente Terminal
public final class TUITerminal extends TUIElement {

    private static final int TITLE_HEIGHT = 24;
    private static final int PADDING = 8;
    private static final int LINE_HEIGHT = 16;
    private static final int MAX_OUTPUT = 30000;
    private TShell.ShellContext context = TShell.ShellContext.TERMINAL;

    private final TKernel kernel;
    private final TMachine machine;
    private final TKeyboardDevice keyboardDevice;
    private final TShell shell;

    private final StringBuilder output = new StringBuilder();
    private final StringBuilder inputLine = new StringBuilder();
    private final List<String> history = new ArrayList<>();

    private int historyIndex = -1;
    private int bgColor;
    private boolean focused;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public TUITerminal(int x, int y, int width, int height, int bgColor, TKernel kernel) {
        super(x, y, width, height);

        if (kernel == null) {
            throw new IllegalArgumentException("Kernel no puede ser null");
        }

        this.kernel = kernel;
        this.machine = kernel.getMachine();
        this.keyboardDevice = machine.getKeyboardDevice();
        this.bgColor = bgColor;

        // La salida de TShell se redirige al historial gráfico.
        this.shell = new TShell(machine, kernel, new TConsoleDevice(this::appendOutput));
        this.shell.setContext(TShell.ShellContext.TERMINAL);
        this.shell.setCloseTerminalAction(() -> {
            setKeyboardFocus(false);
            setVisible(false);
        });

        appendOutput("================================\n");
        appendOutput(" T3OS\n");
        appendOutput("================================\n");
        appendOutput("Kernel initialized.\n\n");
    }

    private void appendOutput(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        output.append(text);

        if (output.length() > MAX_OUTPUT) {
            output.delete(0, output.length() - MAX_OUTPUT);
        }
    }

    @Override
    public void draw(TUI ui) {
        if (!visible) {
            return;
        }

        // Fondo de la ventana.
        ui.fillRect(x, y, width, height, 0x00D0D0D0);
        ui.drawRect(x, y, width, height, 0x00000000);

        // Barra de título.
        ui.fillRect(x, y, width, TITLE_HEIGHT, 0x00008080);
        ui.drawText("T3Terminal", x + 6, y + 4, 0x00FFFFFF);

        // Botón cerrar.
        int closeX = x + width - 22;
        ui.fillRect(closeX, y + 4, 16, 16, 0x00C0C0C0);
        ui.drawRect(closeX, y + 4, 16, 16, 0x00000000);
        ui.drawText("X", closeX + 4, y + 4, 0x00000000);

        int contentY = y + TITLE_HEIGHT;
        int contentHeight = height - TITLE_HEIGHT;

        ui.fillRect(x + 1, contentY, width - 2, contentHeight - 1, bgColor);

        // Historial visible.
        int textX = x + PADDING;
        int textY = contentY + PADDING;
        int textWidth = width - PADDING * 2;

        int maxChars = Math.max(1, (textWidth - 8) / 8);
        List<String> lines = wrapOutput(maxChars);

        int promptRows = 1;
        int availableRows = Math.max(1, (contentHeight - PADDING * 2) / LINE_HEIGHT);
        int outputRows = Math.max(0, availableRows - promptRows);
        int first = Math.max(0, lines.size() - outputRows);

        for (int i = first; i < lines.size(); i++) {
            ui.drawText(lines.get(i), textX, textY, 0x0000FF00);
            textY += LINE_HEIGHT;
        }

        // Prompt y línea de entrada.
        String prompt = "t3os> " + inputLine;
        if (focused) {
            prompt += "_";
        }

        if (prompt.length() > maxChars) {
            prompt = prompt.substring(prompt.length() - maxChars);
        }

        int promptY = y + height - LINE_HEIGHT - 6;

        ui.drawText(prompt, textX, promptY, 0x0000FF00);
    }

    private List<String> wrapOutput(int maxChars) {
        List<String> lines = new ArrayList<>();
        String[] source = output.toString().split("\\n", -1);

        for (String line : source) {
            if (line.isEmpty()) {
                lines.add("");
                continue;
            }

            for (int start = 0; start < line.length(); start += maxChars) {
                int end = Math.min(start + maxChars, line.length());
                lines.add(line.substring(start, end));
            }
        }

        return lines;
    }

    public void keyPressed(TKeyboardDevice.Key key) {
        if (!visible || !enabled || !focused || key == null) {
            return;
        }

        int code = key.getCode();
        char character = key.getCharacter();

        // Enter: ejecutar comando.
        if (code == 10 || code == 13 || character == '\n' || character == '\r') {
            executeCurrentLine();
            return;
        }

        // Backspace.
        if (code == 8 || code == 127 || character == '\b') {
            if (inputLine.length() > 0) {
                inputLine.deleteCharAt(inputLine.length() - 1);
            }
            return;
        }

        // Escape: limpiar la entrada actual.
        if (code == 27) {
            inputLine.setLength(0);
            return;
        }

        // Solo caracteres imprimibles.
        if (!Character.isISOControl(character)) {
            inputLine.append(character);
        }
    }

    private void executeCurrentLine() {
        String line = inputLine.toString().trim();

        appendOutput("t3os> " + line + "\n");

        inputLine.setLength(0);
        if (line.isEmpty()) {
            return;
        }

        history.add(line);
        historyIndex = -1;

        try {
            shell.execute(line);
        } catch (RuntimeException e) {
            appendOutput("Error: " + e.getMessage() + "\n");
        }

        if (machine.isHalted()) {
            focused = false;
        }
    }

    @Override
    public void mouseDown(int button, int mouseX, int mouseY) {
        if (!visible || !enabled || button != 1) {
            return;
        }

        focused = true;

        // Cerrar ventana.
        if (mouseX >= x + width - 22 && mouseX < x + width - 6 && mouseY >= y + 4 && mouseY < y + 20) {
            focused = false;
            setVisible(false);
            return;
        }

        // Arrastrar desde la barra de título.
        if (mouseY >= y && mouseY < y + TITLE_HEIGHT) {
            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
        }
    }

    @Override
    public void mouseMove(int mouseX, int mouseY) {
        if (dragging) {
            setPosition(mouseX - dragOffsetX, mouseY - dragOffsetY);
        }
    }

    @Override
    public void mouseUp(int button) {
        if (button == 1) {
            dragging = false;
        }
    }

    @Override
    public void setPosition(int x, int y) {
        super.setPosition(x, y);
    }

    public boolean hasKeyboardFocus() {
        return visible && enabled && focused;
    }

    public void setKeyboardFocus(boolean focused) {
        this.focused = focused;
    }

    public String getCurrentInput() {
        return inputLine.toString();
    }

    public void clearOutput() {
        output.setLength(0);
    }

    public void setContext(TShell.ShellContext shellContext) {
        if (shellContext == null) {
            throw new IllegalArgumentException("El contexto no puede ser null");
        }

        this.context = shellContext;
        this.shell.setContext(shellContext);
    }

    public TShell.ShellContext getContext() {
        return context;
    }
}

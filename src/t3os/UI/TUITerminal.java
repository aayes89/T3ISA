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
public class TUITerminal extends TUIElement {

    private static final int TITLE_HEIGHT = 24;
    private static final int TOOLBAR_HEIGHT = 28;
    private StringBuilder prompt;
    private String path;
    private int bgColor;
    TUITextArea screenPrompt;
    TPanel panel;
    TKernel kernel;
    TMachine machine;
    TShell shell;
    TKeyboardDevice keyboardDevice;

    // Constructor
    public TUITerminal(int x, int y, int width, int height, int bgColor, TKernel kernel) {
        super(x, y, width, height);
        this.bgColor = bgColor;
        if (kernel == null) {
            throw new IllegalArgumentException("Kernel no puede ser null");
        }
        this.kernel = kernel;
        this.machine = kernel.getMachine();
        this.keyboardDevice = machine.getKeyboardDevice();
        this.shell = new TShell(this.machine, this.kernel, new TConsoleDevice());

        this.screenPrompt = new TUITextArea(
                x + 4,
                y + TITLE_HEIGHT + TOOLBAR_HEIGHT + 4,
                width - 8,
                height - TITLE_HEIGHT - TOOLBAR_HEIGHT - 8,
                "",
                bgColor
        );
        this.path = "/";
        prompt = new StringBuilder();

    }

    @Override
    public void draw(TUI ui) {
        if (!visible) {
            return;
        }

        // Ventana
        ui.fillRect(x, y, width, height, 0x00D0D0D0);

        // Barra de título
        ui.fillRect(x, y, width, TITLE_HEIGHT, 0x00008080);

        String title = "T3Terminal";

        if (path != null && !path.isEmpty()) {
            int slash = path.lastIndexOf('/');
            if (slash >= 0 && slash + 1 < path.length()) {
                title = path.substring(slash + 1);
            }
        }

        ui.drawText(title, x + 6, y + 4, 0x00FFFFFF);

        // Botón cerrar
        ui.fillRect(x + width - 22, y + 4, 16, 16, 0x00C0C0C0);
        ui.drawText("X", x + width - 18, y + 4, 0x00000000);

        // Toolbar
        int toolbarY = y + TITLE_HEIGHT;
        ui.fillRect(x, toolbarY, width, TOOLBAR_HEIGHT, 0x00B0B0B0);

        // Editor
        screenPrompt.setPosition(x + 4, y + TITLE_HEIGHT + TOOLBAR_HEIGHT + 4);
        screenPrompt.draw(ui);

        // Shell
        StartShell();
    }

    public void StartShell() {
        screenPrompt.append("");
        screenPrompt.append("================================");
        screenPrompt.append(" T3OS");
        screenPrompt.append("================================");
        screenPrompt.append("Kernel initialized.");
        screenPrompt.append("");

        while (!machine.isHalted()) {
            screenPrompt.append("t3os> ");
            prompt.append(keyboardDevice.read()); // va un readline aquí
            if (prompt == null) {
                break;
            }

            if (prompt.isEmpty()) {
                continue;
            }

            if (!shell.execute(prompt.toString())) {
                // Limpiar la cache
                prompt.delete(0, prompt.length());
                break;
            }
        }
    }

}

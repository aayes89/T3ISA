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

import t3os.KERNEL.TKernel;

/**
 *
 * @author Slam
 */
public final class TUIDesktop extends TUIElement {

    private final TUIClock clock;
    private final TUITaskbar taskbar;
    private final TUIStartMenu startMenu;
    private final TUIPopupMenu desktopMenu;
    private TUIFileExplorer t3explorer;
    private TUIEditor editor;
    private final TKernel kernel;

    private int backgroundColor;

    private boolean showClock;

    private int clockX;
    private int clockY;

    private int clockColor;

    private TUI ui;

    public TUIDesktop(int width, int height, TKernel kernel) {
        super(0, 0, width, height);
        if (kernel == null) {
            throw new IllegalArgumentException(
                    "Kernel no puede ser null"
            );
        }
        this.kernel = kernel;

        backgroundColor = 0x00202020;
        showClock = true;
        clockX = width - 88;
        clockY = height - 28;
        clockColor = 0x00FFFFFF;
        taskbar = new TUITaskbar(0, height - 40, width, 40);
        startMenu = new TUIStartMenu(0, height - 340, 220, 300);
        clock = new TUIClock(clockX, clockY, 20, 40);
        desktopMenu = new TUIPopupMenu(0, 0, 190, 300);

        createDesktopMenu();
        createStartMenu();

        taskbar.setStartAction(startMenu::toggle);
    }

    private void createDesktopMenu() {
        desktopMenu.addItem("Cambiar fondo", this::changeBackground);
        desktopMenu.addItem("Explorador", this::openExplorer);
        desktopMenu.addItem("Editor", this::openEditor);
        desktopMenu.addItem("Terminal", this::openTerminal);
        desktopMenu.addItem("Reiniciar", this::reboot);
        desktopMenu.addItem("Apagar", this::shutdown);
    }

    private void createStartMenu() {
        int itemHeight = 32;
        int itemWidth = 220;
        int startX = 0;
        int startY = height - 340;

        TUIButton btn_explorer = new TUIButton(startX, startY, itemWidth, itemHeight, "Explorador");
        btn_explorer.setAction(() -> {
            startMenu.close();
            openExplorer();
        });

        TUIButton btn_editor = new TUIButton(startX, startY, itemWidth, itemHeight, "T3Editor");
        btn_editor.setAction(() -> {
            startMenu.close();
            openEditor();
        });

        TUIButton btn_terminal = new TUIButton(startX, startY + itemHeight, itemWidth, itemHeight, "Terminal");
        btn_terminal.setAction(() -> {
            startMenu.close();
            openTerminal();
        });

        TUIButton btn_settings = new TUIButton(startX, startY + itemHeight * 2, itemWidth, itemHeight, "Configuración");
        btn_settings.setAction(() -> {
            startMenu.close();
            openSettings();
        });

        TUIButton btn_reboot = new TUIButton(startX, startY + itemHeight * 3, itemWidth, itemHeight, "Reiniciar");
        btn_reboot.setAction(() -> {
            startMenu.close();
            reboot();
        });

        TUIButton btn_shutdown = new TUIButton(startX, startY + itemHeight * 4, itemWidth, itemHeight, "Apagar");
        btn_shutdown.setAction(() -> {
            startMenu.close();
            shutdown();
        });

        startMenu.addButton(btn_explorer);
        startMenu.addButton(btn_editor);
        startMenu.addButton(btn_terminal);
        startMenu.addButton(btn_settings);
        startMenu.addButton(btn_reboot);
        startMenu.addButton(btn_shutdown);
    }

    private void openSettings() {
        // Se implementará posteriormente.
    }

    @Override

    public void draw(TUI ui) {
        this.ui = ui;
        // Fondo.
        ui.fillRect(x, y, width, height, backgroundColor);
        // Taskbar.
        taskbar.draw(ui);
        // Start.
        if (startMenu.isVisible()) {
            startMenu.draw(ui);
        }
        // Reloj.
        if (showClock) {
            clock.draw(ui);
        }
    }

    @Override
    public void mouseMove(int mouseX, int mouseY) {
        taskbar.mouseMove(mouseX, mouseY);
        if (startMenu.isVisible()) {
            startMenu.mouseMove(mouseX, mouseY);
        }
    }

    @Override
    public void mouseDown(int button, int mouseX, int mouseY) {
        if (button == 3) {
            if (ui != null) {
                ui.openPopup(mouseX, mouseY);
            }
            return;
        }

        if (button != 1) {
            return;
        }

        // Start Menu.
        if (startMenu.isVisible() && startMenu.contains(mouseX, mouseY)) {
            startMenu.mouseDown(button, mouseX, mouseY);
            return;
        }

        // Taskbar.
        if (taskbar.contains(mouseX, mouseY)) {
            taskbar.mouseDown(button, mouseX, mouseY);
            return;
        }

        // Clic fuera del Start Menu.
        if (startMenu.isVisible()) {
            startMenu.close();
        }
    }

    @Override
    public void mouseUp(int button) {
        if (button != 1) {
            return;
        }

        taskbar.mouseUp(button);
        if (startMenu.isVisible()) {
            startMenu.mouseUp(button);
        }
    }

    private void changeBackground() {
        // Por ahora cambio entre fondos predefinidos.
        // Añadiré mandelbrot o cargar imágenes
        if (backgroundColor == 0x00202020) {
            backgroundColor = 0x00000080;
        } else {
            backgroundColor = 0x00202020;
        }
    }

    private void openExplorer() {
        if (ui == null) {
            return;
        }

        if (t3explorer == null) {
            t3explorer = new TUIFileExplorer(80, 60, 500, 400, kernel);
            ui.add(t3explorer);
        } else {
            t3explorer.setVisible(true);
            t3explorer.refresh();
        }

        ui.bringToFront(t3explorer);
    }

    private void openEditor() {
        if (ui == null) {
            return;
        }

        if (editor == null) {
            editor = new TUIEditor(80, 60, 500, 400, kernel, "/archivo.txt", "");
            ui.add(editor);
        } else {
            editor.setVisible(true);
            editor.refresh();
        }

        ui.bringToFront(t3explorer);
    }

    private void openTerminal() {
        // Se conectará con TShell.
    }

    private void reboot() {
        // Se implementará cuando T3OS tenga reinicio completo.
    }

    private void shutdown() {
        if (ui != null) {
            ui.closePopup();
        }
        // El apagado real lo conectare al kernel/máquina.
        System.exit(0);
    }

    public TUIPopupMenu getDesktopMenu() {
        return desktopMenu;
    }

    public TUITaskbar getTaskbar() {
        return taskbar;
    }

    public TUIStartMenu getStartMenu() {
        return startMenu;
    }

    public TUIClock getClock() {
        return clock;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int color) {
        backgroundColor = color;
    }

    public boolean isClockVisible() {
        return showClock;
    }

    public void setClockVisible(boolean visible) {
        showClock = visible;
    }

    public int getClockX() {
        return clockX;
    }

    public int getClockY() {
        return clockY;
    }

    public void setClockPosition(int x, int y) {
        clockX = x;
        clockY = y;
        clock.setPosition(x, y);
    }

    public int getClockColor() {
        return clockColor;
    }

    public void setClockColor(int color) {
        clockColor = color;
    }
}

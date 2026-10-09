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
import t3os.SHELL.TShell;

/**
 *
 * @author Slam
 */
// Implementación del escritorio para el UI
public final class TUIDesktop extends TUIElement {

    private final TUIClock clock;
    private final TUITaskbar taskbar;
    private final TUIStartMenu startMenu;
    private final TUIPopupMenu desktopMenu;
    private TUIFileExplorer t3explorer;
    private TUIEditor editor;
    private final TKernel kernel;

    private int backgroundColor;
    private int bgMode;

    private boolean showClock;

    private int clockX;
    private int clockY;

    private int clockColor;
    private static final int START_MENU_WIDTH = 220;
    private static final int START_ITEM_HEIGHT = 32;
    private static final int START_MENU_PADDING = 4;
    private static final int TASKBAR_HEIGHT = 40;

    private TUI ui;

    // Constructor
    public TUIDesktop(int width, int height, TKernel kernel) {
        super(0, 0, width, height);
        if (kernel == null) {
            throw new IllegalArgumentException("Kernel no puede ser null");
        }
        this.kernel = kernel;

        backgroundColor = 0x00202020;
        bgMode = 0; // 0 - fondo inicial 1 segundo fondo

        showClock = true;
        clockX = width - 88;
        clockY = height - 28;
        clockColor = 0x00FFFFFF;
        taskbar = new TUITaskbar(0, height - TASKBAR_HEIGHT, width, TASKBAR_HEIGHT);
        startMenu = new TUIStartMenu(0, height - 340, 220, 300);
        clock = new TUIClock(clockX, clockY, 20, 40);
        desktopMenu = new TUIPopupMenu(0, 0, 190, 300);

        createDesktopMenu();
        createStartMenu();

        taskbar.setStartAction(startMenu::toggle);
    }

    // Generar elementos del menu clic derecho
    private void createDesktopMenu() {
        desktopMenu.addItem("Cambiar fondo", this::changeBackground);
        desktopMenu.addItem("Explorador", this::openExplorer);
        desktopMenu.addItem("Editor", this::openEditor);
        desktopMenu.addItem("Terminal", this::openTerminal);
        desktopMenu.addItem("Reiniciar", this::reboot);
        desktopMenu.addItem("Apagar", this::shutdown);
    }

    // Generar elementos del menú de inicio
    private void createStartMenu() {
        int itemWidth = 220;
        int itemHeight = 32;

        TUIButton btnExplorer = new TUIButton(0, 0, itemWidth, itemHeight, "Explorador");
        btnExplorer.setAction(() -> {
            startMenu.close();
            openExplorer();
        });

        TUIButton btnEditor = new TUIButton(0, 0, itemWidth, itemHeight, "T3Editor");
        btnEditor.setAction(() -> {
            startMenu.close();
            openEditor();
        });

        TUIButton btnTerminal = new TUIButton(0, 0, itemWidth, itemHeight, "Terminal");
        btnTerminal.setAction(() -> {
            startMenu.close();
            openTerminal();
        });

        TUIButton btnSettings = new TUIButton(0, 0, itemWidth, itemHeight, "Configuración");
        btnSettings.setAction(() -> {
            startMenu.close();
            openSettings();
        });

        TUIButton btnReboot = new TUIButton(0, 0, itemWidth, itemHeight, "Reiniciar");
        btnReboot.setAction(() -> {
            startMenu.close();
            reboot();
        });

        TUIButton btnShutdown = new TUIButton(0, 0, itemWidth, itemHeight, "Apagar");
        btnShutdown.setAction(() -> {
            startMenu.close();
            shutdown();
        });

        startMenu.addButton(btnExplorer);
        startMenu.addButton(btnEditor);
        startMenu.addButton(btnTerminal);
        startMenu.addButton(btnSettings);
        startMenu.addButton(btnReboot);
        startMenu.addButton(btnShutdown);
    }

    // Dibujar los componentes del escritorio
    @Override
    public void draw(TUI ui) {
        this.ui = ui;
        // Fondo.
        switch (bgMode) {
            case 0: // Gradiente azul
                gradiente();
                break;
            case 1: // Fractal mandelbrot
                mandelbrot();
                break;
            default:
                // fondo fijo
                ui.fillRect(x, y, width, height, backgroundColor);
        }

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

    // Eventos al mover el mouse
    @Override
    public void mouseMove(int mouseX, int mouseY) {
        taskbar.mouseMove(mouseX, mouseY);
        if (startMenu.isVisible()) {
            startMenu.mouseMove(mouseX, mouseY);
        }
    }

    // Eventos al presionar clic del mouse
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

    // Eventos al soltar clic del mouse
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

    // Cambiar fondo de pantalla (Colores por ahora)
    private void changeBackground() {
        // Por ahora cambio entre fondos predefinidos.
        // Añadiré cargar imágenes, por ahora es rotativo
        if (bgMode == 0) {
            bgMode = 1;
        } else if (bgMode == 1) {
            bgMode = 2;
        } else {
            gradiente();
            bgMode = 0;
        }
        if (backgroundColor == 0x00202020) {
            backgroundColor = 0x00000080;
        } else {
            backgroundColor = 0x00202020;
        }

    }

    // Gradiente en azul
    public void gradiente() {
        for (int y = 0; y < 728; y += 16) {
            int b = 60 + (y / 3);
            if (b > 255) {
                b = 255;
            }
            int col = ((y / 8) << 16) | ((y / 4) << 8) | b;
            ui.fillRect(0, y, 1024, 16, col);
        }
    }

    // Conjunto Mandelbrot usando punto fijo
    public void mandelbrot() {
        for (int py = 0; py < 728; py += 4) {
            for (int px = 0; px < 1024; px += 4) {
                int x0 = ((px - 600) * 4096) / 300;
                int y0 = ((py - 364) * 4096) / 300;
                int cx = 0, cy = 0, iter = 0;
                while (iter < 24) {
                    int nx2 = (cx * cx) >> 12;
                    int ny2 = (cy * cy) >> 12;
                    if (nx2 + ny2 > 16384) {
                        break; // 4.0 << 12
                    }
                    int xtemp = nx2 - ny2 + x0;
                    cy = ((2 * cx * cy) >> 12) + y0;
                    cx = xtemp;
                    iter++;
                }
                int color = (iter == 24) ? 0x00000000 : (0x000000FF | (iter * 10 << 8) | (iter * 5));

                ui.fillRect(px, py, 4, 4, color);
            }
        }
    }

    // Abrir Panel de control
    private void openSettings() {
        // Se implementará posteriormente.
    }

    // Abrir explorador
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

    // Abrir editor
    private void openEditor() {
        if (ui == null) {
            return;
        }

        if (editor == null) {
            editor = new TUIEditor(80, 60, 500, 400, kernel, "/archivo.txt");
            ui.add(editor);
        } else {
            editor.setVisible(true);
            editor.refresh();
        }

        ui.bringToFront(editor);
    }

    // Abrir terminal
    private void openTerminal() {
        if (ui == null) {
            return;
        }

        TUITerminal terminal = new TUITerminal(100, 80, 700, 450, 0x00000000, kernel);
        terminal.setContext(TShell.ShellContext.TERMINAL);

        ui.add(terminal);
        ui.bringToFront(terminal);
        terminal.setKeyboardFocus(true);
    }

    // Reiniciar sistema
    private void reboot() {
        // Se implementará cuando T3OS tenga reinicio completo.
    }

    // Apagar sistema
    private void shutdown() {
        if (ui != null) {
            ui.closePopup();
        }
        // El apagado real lo conectare al kernel/máquina.
        System.exit(0);
    }

    // === GETTER y SETTERS ===
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

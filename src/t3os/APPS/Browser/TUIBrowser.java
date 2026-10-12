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
package t3os.APPS.Browser;

/**
 *
 * @author Slam
 */
import java.awt.event.KeyEvent;
import t3isa.DEVICE.TKeyboardDevice;
import t3os.UI.TUI;
import t3os.UI.TUIElement;

/**
 * Navegador web de T3OS.
 *
 * Interfaz gráfica basada exclusivamente en TUI. El motor HTML se integrará de
 * forma independiente.
 *
 * @author Slam
 */
public final class TUIBrowser extends TUIElement {

    private static final int TITLE_HEIGHT = 24;
    private static final int TOOLBAR_HEIGHT = 32;
    private static final int STATUS_HEIGHT = 20;

    private static final int COLOR_TITLE = 0x00303030;
    private static final int COLOR_TOOLBAR = 0x00404040;
    private static final int COLOR_BACKGROUND = 0x00181818;
    private static final int COLOR_BORDER = 0x00808080;
    private static final int COLOR_TEXT = 0x00FFFFFF;
    private static final int COLOR_FIELD = 0x00080808;
    private static final int COLOR_BUTTON = 0x00505050;
    private static final int COLOR_DISABLED = 0x00707070;
    private static final int COLOR_STATUS = 0x00282828;
    private static final int COLOR_CURSOR = 0x00FFFFFF;

    private static final int BUTTON_WIDTH = 32;
    private static final int GO_WIDTH = 36;
    private static final int CLOSE_WIDTH = 24;

    private String address = "about:blank";
    private String status = "Listo";
    private String pageTitle = "T3Browser";

    private boolean addressFocused;
    private boolean dragging;
    private boolean closeRequested;

    private int dragOffsetX;
    private int dragOffsetY;
    private int addressCursor;

    private Button pressedButton = Button.NONE;

    private enum Button {
        NONE,
        BACK,
        FORWARD,
        RELOAD,
        GO,
        CLOSE
    }

    public TUIBrowser(int x, int y, int width, int height) {
        super(x, y, width, height);

        if (width < 360 || height < 240) {
            throw new IllegalArgumentException(
                    "TUIBrowser requiere al menos 360x240 píxeles"
            );
        }

        addressCursor = address.length();
    }

    @Override
    public void draw(TUI ui) {
        if (!visible) {
            return;
        }

        drawWindow(ui);
        drawTitleBar(ui);
        drawToolbar(ui);
        drawContent(ui);
        drawStatusBar(ui);
    }

    private void drawWindow(TUI ui) {
        ui.fillRect(x, y, width, height, COLOR_BACKGROUND);
        ui.drawRect(x, y, width, height, COLOR_BORDER);
    }

    private void drawTitleBar(TUI ui) {
        ui.fillRect(x + 1, y + 1, width - 2,
                TITLE_HEIGHT, COLOR_TITLE);

        ui.drawText(pageTitle, x + 8, y + 7, COLOR_TEXT);

        int closeX = x + width - CLOSE_WIDTH;
        int closeY = y + 1;

        ui.fillRect(closeX, closeY, CLOSE_WIDTH - 1,
                TITLE_HEIGHT - 1, COLOR_BUTTON);

        int cx = closeX + 8;
        int cy = y + 7;

        ui.drawLine(cx, cy, cx + 7, cy + 7, COLOR_TEXT);
        ui.drawLine(cx + 7, cy, cx, cy + 7, COLOR_TEXT);
    }

    private void drawToolbar(TUI ui) {
        int toolbarY = y + TITLE_HEIGHT;

        ui.fillRect(x + 1, toolbarY, width - 2,
                TOOLBAR_HEIGHT, COLOR_TOOLBAR);

        int bx = x + 5;
        int by = toolbarY + 4;
        int bh = TOOLBAR_HEIGHT - 8;

        drawButton(ui, bx, by, BUTTON_WIDTH, bh, "<", COLOR_BUTTON);
        bx += BUTTON_WIDTH + 3;

        drawButton(ui, bx, by, BUTTON_WIDTH, bh, ">", COLOR_BUTTON);
        bx += BUTTON_WIDTH + 3;

        drawButton(ui, bx, by, BUTTON_WIDTH, bh, "R", COLOR_BUTTON);
        bx += BUTTON_WIDTH + 6;

        int goX = x + width - GO_WIDTH - 5;
        int fieldX = bx;
        int fieldWidth = goX - fieldX - 4;

        if (fieldWidth < 40) {
            fieldWidth = 40;
        }

        ui.fillRect(fieldX, by, fieldWidth, bh, COLOR_FIELD);
        ui.drawRect(fieldX, by, fieldWidth, bh,
                addressFocused ? COLOR_TEXT : COLOR_BORDER);

        int textX = fieldX + 5;
        int textY = by + 8;

        ui.drawText(address, textX, textY, COLOR_TEXT);

        if (addressFocused) {
            int visibleCursor = Math.min(addressCursor, address.length());
            int cursorX = textX + visibleCursor * 8;

            int rightLimit = fieldX + fieldWidth - 3;
            if (cursorX <= rightLimit) {
                ui.drawLine(cursorX, by + 5,
                        cursorX, by + bh - 5, COLOR_CURSOR);
            }
        }

        drawButton(ui, goX, by, GO_WIDTH, bh, "Ir", COLOR_BUTTON);
    }

    private void drawButton(
            TUI ui,
            int bx,
            int by,
            int bw,
            int bh,
            String label,
            int color
    ) {
        ui.fillRect(bx, by, bw, bh, color);
        ui.drawRect(bx, by, bw, bh, COLOR_BORDER);

        int textX = bx + (bw - label.length() * 8) / 2;
        int textY = by + (bh - 16) / 2 + 4;

        ui.drawText(label, textX, textY, COLOR_TEXT);
    }

    private void drawContent(TUI ui) {
        int contentY = y + TITLE_HEIGHT + TOOLBAR_HEIGHT;
        int contentHeight = height
                - TITLE_HEIGHT
                - TOOLBAR_HEIGHT
                - STATUS_HEIGHT;

        ui.fillRect(
                x + 1,
                contentY,
                width - 2,
                contentHeight,
                COLOR_BACKGROUND
        );

        ui.drawText(
                "T3Browser",
                x + 12,
                contentY + 20,
                COLOR_TEXT
        );

        ui.drawText(
                "Direccion: " + address,
                x + 12,
                contentY + 44,
                COLOR_TEXT
        );

        ui.drawText(
                status,
                x + 12,
                contentY + 68,
                COLOR_TEXT
        );

        ui.drawText(
                "Motor HTML pendiente de integrar",
                x + 12,
                contentY + 92,
                COLOR_BORDER
        );
    }

    private void drawStatusBar(TUI ui) {
        int sy = y + height - STATUS_HEIGHT;

        ui.fillRect(
                x + 1,
                sy,
                width - 2,
                STATUS_HEIGHT - 1,
                COLOR_STATUS
        );

        ui.drawLine(
                x + 1,
                sy,
                x + width - 2,
                sy,
                COLOR_BORDER
        );

        ui.drawText(status, x + 7, sy + 3, COLOR_TEXT);
    }

    @Override
    public void mouseDown(int button, int mx, int my) {
        if (button != 1) {
            return;
        }

        int localX = mx - x;
        int localY = my - y;

        pressedButton = Button.NONE;

        if (localY < TITLE_HEIGHT) {
            if (localX >= width - CLOSE_WIDTH) {
                pressedButton = Button.CLOSE;
                return;
            }

            dragging = true;
            dragOffsetX = localX;
            dragOffsetY = localY;
            return;
        }

        int toolbarY = TITLE_HEIGHT;
        if (localY < toolbarY || localY >= toolbarY + TOOLBAR_HEIGHT) {
            addressFocused = false;
            return;
        }

        int by = toolbarY + 4;
        int bh = TOOLBAR_HEIGHT - 8;

        if (localY < by || localY >= by + bh) {
            addressFocused = false;
            return;
        }

        int bx = 5;

        if (localX >= bx && localX < bx + BUTTON_WIDTH) {
            pressedButton = Button.BACK;
            addressFocused = false;
            return;
        }

        bx += BUTTON_WIDTH + 3;

        if (localX >= bx && localX < bx + BUTTON_WIDTH) {
            pressedButton = Button.FORWARD;
            addressFocused = false;
            return;
        }

        bx += BUTTON_WIDTH + 3;

        if (localX >= bx && localX < bx + BUTTON_WIDTH) {
            pressedButton = Button.RELOAD;
            addressFocused = false;
            return;
        }

        int goX = width - GO_WIDTH - 5;

        if (localX >= goX && localX < goX + GO_WIDTH) {
            pressedButton = Button.GO;
            addressFocused = false;
            return;
        }

        int fieldX = bx;// + BUTTON_WIDTH + 6;
        if (localX >= fieldX && localX < goX) {
            addressFocused = true;

            int charPosition = (localX - fieldX - 5) / 8;
            addressCursor = Math.max(
                    0,
                    Math.min(charPosition, address.length())
            );
        } else {
            addressFocused = false;
        }
    }

    @Override
    public void mouseMove(int mx, int my) {
        if (!dragging) {
            return;
        }

        setPosition(mx - dragOffsetX, my - dragOffsetY);
    }

    @Override
    public void mouseUp(int button) {
        if (button != 1) {
            return;
        }

        dragging = false;

        Button action = pressedButton;
        pressedButton = Button.NONE;

        switch (action) {
            case CLOSE:
                closeRequested = true;
                visible = false;
                break;

            case BACK:
                status = "Historial anterior pendiente";
                break;

            case FORWARD:
                status = "Historial siguiente pendiente";
                break;

            case RELOAD:
                navigate();
                break;

            case GO:
                navigate();
                break;

            case NONE:
            default:
                break;
        }
    }

    public void keyPressed(TKeyboardDevice.Key key) {
        if (!addressFocused || key == null) {
            return;
        }

        int code = key.getCode();
        char ch = key.getCharacter();

        if (code == KeyEvent.VK_ENTER || ch == '\n' || ch == '\r') {
            addressFocused = false;
            navigate();
            return;
        }

        if (code == KeyEvent.VK_ESCAPE) {
            addressFocused = false;
            return;
        }

        if (code == KeyEvent.VK_BACK_SPACE || ch == '\b') {
            if (addressCursor > 0 && !address.isEmpty()) {
                address = address.substring(0, addressCursor - 1)
                        + address.substring(addressCursor);
                addressCursor--;
            }
            return;
        }

        if (code == KeyEvent.VK_DELETE) {
            if (addressCursor < address.length()) {
                address = address.substring(0, addressCursor)
                        + address.substring(addressCursor + 1);
            }
            return;
        }

        if (code == KeyEvent.VK_LEFT) {
            if (addressCursor > 0) {
                addressCursor--;
            }
            return;
        }

        if (code == KeyEvent.VK_RIGHT) {
            if (addressCursor < address.length()) {
                addressCursor++;
            }
            return;
        }

        if (code == KeyEvent.VK_HOME) {
            addressCursor = 0;
            return;
        }

        if (code == KeyEvent.VK_END) {
            addressCursor = address.length();
            return;
        }

        if (!Character.isISOControl(ch)) {
            address = address.substring(0, addressCursor)
                    + ch
                    + address.substring(addressCursor);

            addressCursor++;
        }
    }

    private void navigate() {
        String target = address.trim();

        if (target.isEmpty()) {
            status = "Introduce una direccion";
            return;
        }

        address = target;
        addressCursor = address.length();

        if ("about:blank".equalsIgnoreCase(address)) {
            pageTitle = "T3Browser";
            status = "Pagina en blanco";
            return;
        }

        if (!address.contains("://")
                && !address.startsWith("about:")) {
            address = "http://" + address;
            addressCursor = address.length();
        }

        status = "Solicitud pendiente: " + address;
        pageTitle = "T3Browser";
    }

    public void refresh() {
        System.out.println("Refrescando");
    }

    public boolean hasAddressFocus() {
        return addressFocused;
    }

    public boolean isCloseRequested() {
        return closeRequested;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        if (address == null) {
            return;
        }

        this.address = address;
        this.addressCursor = address.length();
    }
}

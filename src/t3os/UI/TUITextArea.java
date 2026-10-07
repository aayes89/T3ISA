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

/**
 *
 * @author Slam
 */
import java.awt.event.KeyEvent;

public class TUITextArea extends TUIElement {

    private final StringBuilder sb;
    private final int color;

    private int cursor;
    private boolean focused;

    public TUITextArea(int x, int y, int width, int height, String text, int color) {
        super(x, y, width, height);

        sb = new StringBuilder();

        if (text != null) {
            sb.append(text);
        }

        this.color = color;
        cursor = sb.length();
        focused = false;
    }

    @Override
    public void draw(TUI ui) {
        ui.fillRect(x, y, width, height, 0x00FFFFFF);
        ui.drawRect(x, y, width, height, 0x00000000);

        String text = sb.toString();

        int lineY = y + 4;
        int lineStart = 0;

        for (int i = 0; i <= text.length(); i++) {
            if (i == text.length() || text.charAt(i) == '\n') {
                String line = text.substring(lineStart, i);

                ui.drawText(line, x + 4, lineY, color);

                lineY += 16;
                lineStart = i + 1;

                if (lineY >= y + height) {
                    break;
                }
            }
        }

        if (focused) {

            int cursorX = x + 4;
            int cursorY = y + 4;

            int lineStartCursor = 0;

            for (int i = 0; i < cursor; i++) {
                if (sb.charAt(i) == '\n') {
                    cursorX = x + 4;
                    cursorY += 16;

                    lineStartCursor = i + 1;
                }
            }

            cursorX += (cursor - lineStartCursor) * 8;
            ui.fillRect(cursorX, cursorY, 1, 14, color);
        }
    }

    @Override
    public void mouseDown(int button, int mouseX, int mouseY) {
        if (button != 1) {
            return;
        }

        if (!contains(mouseX, mouseY)) {
            focused = false;
            return;
        }

        focused = true;
        int line = (mouseY - y - 4) / 16;
        int column = (mouseX - x - 4) / 8;
        cursor = getCursorFromPosition(line, column);
    }

    private int getCursorFromPosition(int line, int column) {
        int currentLine = 0;
        int position = 0;

        while (position < sb.length()) {
            if (currentLine == line) {
                int lineEnd = position;
                while (lineEnd < sb.length() && sb.charAt(lineEnd) != '\n') {
                    lineEnd++;
                }

                return Math.min(position + column, lineEnd);
            }

            if (sb.charAt(position) == '\n') {
                currentLine++;
            }

            position++;
        }

        return sb.length();
    }

    public void keyPressed(int key, char character) {
        switch (key) {
            case KeyEvent.VK_LEFT:
                moveLeft();
                return;

            case KeyEvent.VK_RIGHT:
                moveRight();
                return;

            case KeyEvent.VK_HOME:
                moveHome();
                return;

            case KeyEvent.VK_END:
                moveEnd();
                return;

            case KeyEvent.VK_BACK_SPACE:
                backspace();
                return;

            case KeyEvent.VK_DELETE:
                delete();
                return;

            case KeyEvent.VK_ENTER:
                addString("\n");
                return;

            case KeyEvent.VK_TAB:
                addString("    ");
                return;
        }

        if (!Character.isISOControl(character)) {
            addString(String.valueOf(character));
        }
    }

    public void addString(String text) {
        if (text == null) {
            return;
        }

        sb.insert(cursor, text);
        cursor += text.length();
    }

    public void append(String text) {
        if (text == null) {
            return;
        }

        sb.append(text);
        cursor = sb.length();
    }

    public void backspace() {
        if (cursor <= 0) {
            return;
        }

        sb.deleteCharAt(cursor - 1);
        cursor--;
    }

    public void delete() {
        if (cursor >= sb.length()) {
            return;
        }

        sb.deleteCharAt(cursor);
    }

    public void moveLeft() {
        if (cursor > 0) {
            cursor--;
        }
    }

    public void moveRight() {
        if (cursor < sb.length()) {
            cursor++;
        }
    }

    public void moveHome() {
        while (cursor > 0 && sb.charAt(cursor - 1) != '\n') {
            cursor--;
        }
    }

    public void moveEnd() {
        while (cursor < sb.length() && sb.charAt(cursor) != '\n') {
            cursor++;
        }
    }

    public String getText() {
        return sb.toString();
    }

    public void setText(String text) {
        sb.setLength(0);
        if (text != null) {
            sb.append(text);
        }

        cursor = sb.length();
    }

    public int getCursor() {
        return cursor;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public boolean isFocused() {
        return focused;
    }
}

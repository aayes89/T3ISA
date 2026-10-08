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

// Componente Área de texto
public class TUITextArea extends TUIElement {

    private static final int CHAR_WIDTH = 8;
    private static final int LINE_HEIGHT = 16;
    private static final int PADDING_X = 4;
    private static final int PADDING_Y = 4;

    private final StringBuilder sb;
    private final int color;

    private int cursor;
    private boolean focused;

    // Desplazamiento vertical de las líneas visuales
    private int scrollY;

    private static final class CursorPosition {

        final int line;
        final int column;
        final int lineStart;
        final int lineEnd;

        CursorPosition(int line, int column, int lineStart, int lineEnd) {
            this.line = line;
            this.column = column;
            this.lineStart = lineStart;
            this.lineEnd = lineEnd;
        }
    }

    // Constructor
    public TUITextArea(int x, int y, int width, int height, String text, int color) {
        super(x, y, width, height);

        sb = new StringBuilder();

        if (text != null) {
            sb.append(text);
        }

        this.color = color;
        cursor = sb.length();
        focused = false;
        scrollY = 0;
    }

    // Dibujar área de texto
    @Override
    public void draw(TUI ui) {
        ui.fillRect(x, y, width, height, 0x00FFFFFF);
        ui.drawRect(x, y, width, height, 0x00000000);

        int charsPerLine = getCharsPerLine();

        int visualLine = 0;
        int lineY = y + PADDING_Y - scrollY;

        int lineStart = 0;

        while (lineStart <= sb.length()) {
            int lineEnd = getVisualLineEnd(lineStart, charsPerLine);

            if (lineY + LINE_HEIGHT > y && lineY < y + height) {
                drawLine(ui, lineStart, lineEnd, lineY);
            }

            if (lineEnd >= sb.length()) {
                break;
            }

            lineStart = lineEnd;

            // Si el carácter que terminó la línea es '\n', se consume aquí.
            if (lineStart < sb.length() && sb.charAt(lineStart) == '\n') {
                lineStart++;
            }

            visualLine++;
            lineY += LINE_HEIGHT;

            if (lineY >= y + height) {
                break;
            }
        }

        if (focused) {
            drawCursor(ui, charsPerLine);
        }
    }

    // Obtener cantidad máxima de caracteres por línea visual
    private int getCharsPerLine() {
        int availableWidth = width - (PADDING_X * 2);

        int chars = availableWidth / CHAR_WIDTH;

        if (chars < 1) {
            chars = 1;
        }

        return chars;
    }

    // Obtener final de una línea visual
    private int getVisualLineEnd(int start, int charsPerLine) {

        if (start >= sb.length()) {
            return start;
        }

        int position = start;
        int count = 0;

        while (position < sb.length() && count < charsPerLine) {
            char c = sb.charAt(position);
            if (c == '\n') {
                break;
            }

            position++;
            count++;
        }

        return position;
    }

    // Dibujar una línea visual
    private void drawLine(TUI ui, int start, int end, int lineY) {
        if (start >= end) {
            return;
        }

        String line = sb.substring(start, end);
        ui.drawText(line, x + PADDING_X, lineY, color);
    }

    // Dibujar cursor
    private void drawCursor(TUI ui, int charsPerLine) {
        CursorPosition position = getCursorPosition();
        int cursorX = x + PADDING_X + position.column * CHAR_WIDTH;
        int cursorY = y + PADDING_Y + position.line * LINE_HEIGHT - scrollY;
        if (cursorY >= y && cursorY < y + height) {
            ui.fillRect(cursorX, cursorY, 1, 14, color);
        }
    }

    // Capturar eventos del mouse
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

        int charsPerLine = getCharsPerLine();

        int visualLine = (mouseY - y - PADDING_Y + scrollY) / LINE_HEIGHT;
        int column = (mouseX - x - PADDING_X) / CHAR_WIDTH;

        if (visualLine < 0) {
            visualLine = 0;
        }

        if (column < 0) {
            column = 0;
        }

        cursor = getCursorFromPosition(visualLine, column, charsPerLine);
        ensureCursorVisible();
    }

    // Convertir posición visual a posición real del StringBuilder
    private int getCursorFromPosition(int targetLine, int targetColumn, int charsPerLine) {
        int line = 0;
        int position = 0;

        while (position < sb.length()) {
            if (line == targetLine) {
                int lineEnd = getVisualLineEnd(position, charsPerLine);
                return Math.min(position + targetColumn, lineEnd);
            }

            int lineEnd = getVisualLineEnd(position, charsPerLine);

            // Línea terminada por salto real.
            if (lineEnd < sb.length() && sb.charAt(lineEnd) == '\n') {
                position = lineEnd + 1;
                line++;
                continue;
            }

            // Línea terminada por wrapping.
            if (lineEnd < sb.length()) {
                position = lineEnd;
                line++;
                continue;
            }

            break;
        }

        return sb.length();
    }

    // Gestión del teclado
    public void keyPressed(int key, char character) {
        if (!focused) {
            return;
        }

        switch (key) {
            case KeyEvent.VK_LEFT:
                moveLeft();
                return;

            case KeyEvent.VK_RIGHT:
                moveRight();
                return;

            case KeyEvent.VK_UP:
                moveUp();
                return;

            case KeyEvent.VK_DOWN:
                moveDown();
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
                ensureCursorVisible();
                return;

            case KeyEvent.VK_TAB:
                addString("    ");
                return;
        }

        if (!Character.isISOControl(character)) {
            addString(String.valueOf(character));
        }

        ensureCursorVisible();
    }

    // Insertar texto en la posición del cursor
    public void addString(String text) {
        if (text == null) {
            return;
        }

        sb.insert(cursor, text);
        cursor += text.length();

        ensureCursorVisible();
    }

    // Añadir texto al final
    public void append(String text) {
        if (text == null) {
            return;
        }

        sb.append(text);
        cursor = sb.length();

        ensureCursorVisible();
    }

    // Backspace
    public void backspace() {
        if (cursor <= 0) {
            return;
        }

        sb.deleteCharAt(cursor - 1);
        cursor--;

        ensureCursorVisible();
    }

    // Delete
    public void delete() {
        if (cursor >= sb.length()) {
            return;
        }

        sb.deleteCharAt(cursor);

        ensureCursorVisible();
    }

    // Mover cursor izquierda
    public void moveLeft() {
        if (cursor > 0) {
            cursor--;
        }

        ensureCursorVisible();
    }

    // Mover cursor derecha
    public void moveRight() {
        if (cursor < sb.length()) {
            cursor++;
        }

        ensureCursorVisible();
    }

    // Mover cursor una línea visual arriba
    public void moveUp() {
        int charsPerLine = getCharsPerLine();

        int line = 0;
        int lineStart = 0;

        while (lineStart < cursor) {
            int lineEnd = getVisualLineEnd(lineStart, charsPerLine);
            if (cursor <= lineEnd) {
                break;
            }

            if (lineEnd < sb.length() && sb.charAt(lineEnd) == '\n') {
                lineStart = lineEnd + 1;
            } else if (lineEnd < sb.length()) {
                lineStart = lineEnd;
            } else {
                break;
            }

            line++;
        }

        if (line <= 0) {
            return;
        }

        int column = cursor - lineStart;

        int previousLineStart = 0;
        int currentLine = 0;

        while (previousLineStart < sb.length() && currentLine < line - 1) {
            int end = getVisualLineEnd(previousLineStart, charsPerLine);

            if (end < sb.length() && sb.charAt(end) == '\n') {
                previousLineStart = end + 1;
            } else if (end < sb.length()) {
                previousLineStart = end;
            } else {
                break;
            }

            currentLine++;
        }

        int previousEnd = getVisualLineEnd(previousLineStart, charsPerLine);

        cursor = Math.min(previousLineStart + column, previousEnd);
        ensureCursorVisible();
    }

    // Mover cursor una línea visual abajo
    public void moveDown() {
        int charsPerLine = getCharsPerLine();

        int line = 0;
        int lineStart = 0;

        while (lineStart < cursor) {
            int lineEnd = getVisualLineEnd(lineStart, charsPerLine);
            if (cursor <= lineEnd) {
                break;
            }

            if (lineEnd < sb.length() && sb.charAt(lineEnd) == '\n') {
                lineStart = lineEnd + 1;
            } else if (lineEnd < sb.length()) {
                lineStart = lineEnd;
            } else {
                break;
            }

            line++;
        }

        int currentColumn = cursor - lineStart;
        int nextLineStart;
        int currentEnd = getVisualLineEnd(lineStart, charsPerLine);

        if (currentEnd < sb.length() && sb.charAt(currentEnd) == '\n') {
            nextLineStart = currentEnd + 1;
        } else if (currentEnd < sb.length()) {
            nextLineStart = currentEnd;
        } else {
            return;
        }

        int nextEnd = getVisualLineEnd(nextLineStart, charsPerLine);

        cursor = Math.min(nextLineStart + currentColumn, nextEnd);
        ensureCursorVisible();
    }

    // Home
    public void moveHome() {
        int charsPerLine = getCharsPerLine();
        int lineStart = cursor;

        // Buscar inicio de la línea lógica o visual.
        while (lineStart > 0) {
            if (sb.charAt(lineStart - 1) == '\n') {
                break;
            }

            int candidateStart = lineStart - 1;
            int visualOffset = cursor - candidateStart;
            if (visualOffset > charsPerLine) {
                break;
            }

            lineStart--;
        }

        cursor = lineStart;
        ensureCursorVisible();
    }

    // End
    public void moveEnd() {
        int charsPerLine = getCharsPerLine();
        int lineStart = cursor;
        while (lineStart > 0 && sb.charAt(lineStart - 1) != '\n') {
            int candidate = lineStart - 1;
            if (cursor - candidate > charsPerLine) {
                break;
            }

            lineStart--;
        }

        int lineEnd = getVisualLineEnd(lineStart, charsPerLine);
        cursor = lineEnd;

        ensureCursorVisible();
    }

    // Mantener visible el cursor
    private void ensureCursorVisible() {
        CursorPosition position = getCursorPosition();
        int cursorY = y + PADDING_Y + position.line * LINE_HEIGHT;
        if (cursorY < y) {
            scrollY = position.line * LINE_HEIGHT;
        } else if (cursorY + LINE_HEIGHT > y + height) {
            scrollY = position.line * LINE_HEIGHT - height + LINE_HEIGHT + PADDING_Y;
        }

        if (scrollY < 0) {
            scrollY = 0;
        }
    }

    // Obtener texto
    public String getText() {
        return sb.toString();
    }

    // Establecer texto
    public void setText(String text) {
        sb.setLength(0);
        if (text != null) {
            sb.append(text);
        }

        cursor = sb.length();
        scrollY = 0;
    }

    // Obtener cursor
    public int getCursor() {
        return cursor;
    }

    private CursorPosition getCursorPosition() {
        int charsPerLine = getCharsPerLine();

        int position = 0;
        int line = 0;

        while (true) {
            int lineStart = position;
            if (position >= sb.length()) {
                return new CursorPosition(line, 0, position, position);
            }

            int lineEnd = getVisualLineEnd(position, charsPerLine);

            // Cursor dentro de esta línea.
            if (cursor <= lineEnd) {
                int column = cursor - lineStart;
                return new CursorPosition(line, column, lineStart, lineEnd);
            }

            // La línea termina explícitamente con ENTER.
            if (lineEnd < sb.length() && sb.charAt(lineEnd) == '\n') {
                /*
                * El cursor inmediatamente después del '\n'
                * pertenece a la siguiente línea.
                 */
                if (cursor == lineEnd + 1) {
                    return new CursorPosition(line + 1, 0, lineEnd + 1, lineEnd + 1);
                }

                position = lineEnd + 1;
                line++;
                continue;
            }

            // La línea terminó por wrapping.
            if (lineEnd < sb.length()) {
                position = lineEnd;
                line++;
                continue;
            }

            // Fin absoluto del documento.
            return new CursorPosition(line, cursor - lineStart, lineStart, lineEnd);
        }
    }

    // Establecer foco
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    // Obtener foco
    public boolean isFocused() {
        return focused;
    }
}

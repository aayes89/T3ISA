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
import java.util.ArrayList;
import java.util.List;

public final class TUIPopupMenu extends TUIElement {

    private final List<TUIButton> items;
    private int backgroundColor;
    private int borderColor;
    private static final int ITEM_HEIGHT = 24;

    public TUIPopupMenu(int x, int y, int width, int height) {
        super(x, y, width, height);
        items = new ArrayList<>();
        backgroundColor = 0x00D0D0D0;
        borderColor = 0x00000000;
        visible = false;
    }

    @Override
    public void draw(TUI ui) {
        if (!visible) {
            return;
        }
        height = items.size() * ITEM_HEIGHT;
        ui.fillRect(x, y, width, height, backgroundColor);
        ui.drawRect(x, y, width, height, borderColor);
        for (int i = 0; i < items.size(); i++) {
            TUIButton item = items.get(i);
            if (!item.isVisible()) {
                continue;
            }
            item.draw(ui);
        }
    }

    @Override
    public void mouseMove(int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        for (TUIButton item : items) {
            item.mouseMove(mouseX, mouseY);
        }
    }

    @Override
    public void mouseDown(int button, int mouseX, int mouseY) {
        if (!visible || (button != 1 && button != 3)) {
            return;
        }
        for (TUIButton item : items) {
            if (!item.contains(mouseX, mouseY)) {
                continue;
            }
            item.mouseDown(button, mouseX, mouseY);
            return;
        }
    }

    @Override
    public void mouseUp(int button) {
        if (!visible || button != 1) {
            return;
        }
        for (TUIButton item : items) {
            item.mouseUp(button);
        }
    }

    public void addItem(String text, Runnable action) {
        int itemY = y + items.size() * ITEM_HEIGHT;
        TUIButton item = new TUIButton(x, itemY, width, ITEM_HEIGHT, text);
        item.setAction(() -> {
            if (action != null) {
                action.run();
            }
            close();
        });
        items.add(item);
        height = items.size() * ITEM_HEIGHT;
    }

    public void removeItem(TUIButton item) {
        if (!items.remove(item)) {
            return;
        }
        repositionItems();
    }

    private void repositionItems() {
        for (int i = 0; i < items.size(); i++) {
            TUIButton item = items.get(i);
            // Los botones se mantienen en coordenadas absolutas. 
            item.setPosition(x, y + i * ITEM_HEIGHT);
        }
        height = items.size() * ITEM_HEIGHT;
    }

    public void open(int x, int y) {
        this.x = x;
        this.y = y;
        repositionItems();
        visible = true;
    }

    public void close() {
        visible = false;
    }

    public boolean isOpen() {
        return visible;
    }

    public int getItemHeight() {
        return ITEM_HEIGHT;
    }

    public int getItemCount() {
        return items.size();
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int color) {
        backgroundColor = color;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int color) {
        borderColor = color;
    }
}

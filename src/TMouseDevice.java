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
/**
 *
 * @author Slam
 */

import t3isa.Core.TWord;
import t3isa.HARDWARE.TDevice;
import t3isa.HARDWARE.TMMIODevice;

public final class TMouseDevice implements TDevice, TMMIODevice {

    public static final int X = 0;
    public static final int Y = 1;
    public static final int BUTTONS = 2;
    public static final int EVENT = 3;

    public static final int EVENT_NONE = 0;
    public static final int EVENT_MOVE = 1;
    public static final int EVENT_BUTTON = 2;

    private int x;
    private int y;
    private int buttons;
    private int event;

    @Override
    public void write(TWord value) {
    }

    @Override
    public TWord read() {
        return TWord.fromLong(0);
    }

    @Override
    public boolean hasInput() {
        return event != EVENT_NONE;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        event = EVENT_MOVE;
    }

    public void setButtons(int buttons) {
        this.buttons = buttons;
        event = EVENT_BUTTON;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getButtons() {
        return buttons;
    }

    public void clearEvent() {
        event = EVENT_NONE;
    }

    @Override
    public TWord read(int offset) {

        switch (offset) {

            case X:
                return TWord.fromLong(x);

            case Y:
                return TWord.fromLong(y);

            case BUTTONS:
                return TWord.fromLong(buttons);

            case EVENT:
                int value = event;
                event = EVENT_NONE;
                return TWord.fromLong(value);

            default:
                return TWord.zero();
        }
    }

    @Override
    public void write(int offset, TWord value) {
    }
}

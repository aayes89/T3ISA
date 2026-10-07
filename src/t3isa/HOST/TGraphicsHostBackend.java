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
package t3isa.HOST;

/**
 *
 * @author Slam
 */
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import t3isa.DEVICE.TGraphicsDevice;
import t3isa.DEVICE.TKeyboardDevice;
import t3isa.DEVICE.TMouseDevice;

public final class TGraphicsHostBackend {

    private final TGraphicsDevice device;
    private final TMouseDevice mouseDevice;
    private final TKeyboardDevice keyboardDevice;

    private JFrame frame;
    private JPanel panel;
    private BufferedImage image;
    private Timer timer;

    public TGraphicsHostBackend(TGraphicsDevice device, TMouseDevice mouseDevice, TKeyboardDevice keyboardDevice) {
        if (device == null) {
            throw new IllegalArgumentException("Graphics device no puede ser null");
        }
        if (mouseDevice == null) {
            throw new IllegalArgumentException("Mouse device no puede ser null");
        }
        if (keyboardDevice == null) {
            throw new IllegalArgumentException("Keyboard device no puede ser null");
        }

        this.device = device;
        this.mouseDevice = mouseDevice;
        this.keyboardDevice = keyboardDevice;

        image = new BufferedImage(device.getWidth(), device.getHeight(), BufferedImage.TYPE_INT_RGB);
    }

    public void open() {
        SwingUtilities.invokeLater(() -> {

            frame = new JFrame("T3ISA - Virtual Monitor");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            panel = new JPanel() {

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.drawImage(image, 0, 0, null);
                    //g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
                }
            };
            panel.setPreferredSize(
                    new java.awt.Dimension(
                            device.getWidth(),
                            device.getHeight()
                    )
            );
            panel.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    mouseDevice.setPosition(e.getX(), e.getY());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    mouseDevice.setPosition(e.getX(), e.getY());
                }
            }
            );

            panel.addMouseListener(new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    int buttons = mouseDevice.getButtons();

                    if (e.getButton() == MouseEvent.BUTTON1) {
                        buttons |= 1;
                    }

                    if (e.getButton() == MouseEvent.BUTTON2) {
                        buttons |= 2;
                    }

                    if (e.getButton() == MouseEvent.BUTTON3) {
                        buttons |= 4;
                    }

                    mouseDevice.setButtons(buttons);
                }

                @Override
                public void mouseReleased(MouseEvent e) {

                    int buttons = mouseDevice.getButtons();

                    if (e.getButton() == MouseEvent.BUTTON1) {
                        buttons &= ~1;
                    }

                    if (e.getButton() == MouseEvent.BUTTON2) {
                        buttons &= ~2;
                    }

                    if (e.getButton() == MouseEvent.BUTTON3) {
                        buttons &= ~4;
                    }

                    mouseDevice.setButtons(buttons);
                }
            }
            );

            panel.setFocusable(true);

            panel.addKeyListener(new KeyAdapter() {

                @Override
                public void keyPressed(KeyEvent e) {
                    keyboardDevice.push(e.getKeyCode(), e.getKeyChar());
                }
            });

            panel.setDoubleBuffered(true);
            frame.setContentPane(panel);
            //frame.setSize(device.getWidth(), device.getHeight());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.requestFocusInWindow();
            frame.pack();

            int interval = Math.max(1, 1000 / device.getRefreshRate());
            timer = new Timer(interval, e -> refresh());
            timer.start();
        });
    }

    private void refresh() {
        int[] framebuffer = device.getFramebuffer();
        int width = device.getWidth();
        int height = device.getHeight();

        image.setRGB(0, 0, width, height, framebuffer, 0, width);
        panel.repaint();
    }

    public void close() {
        SwingUtilities.invokeLater(() -> {

            if (timer != null) {
                timer.stop();
                timer = null;
            }

            if (frame != null) {
                frame.dispose();
                frame = null;
            }
        });
    }

    public TGraphicsDevice getDevice() {
        return device;
    }
}

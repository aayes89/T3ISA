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
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import t3isa.DEVICE.TGraphicsDevice;

public final class TGraphicsHostBackend {

    private final TGraphicsDevice device;

    private JFrame frame;
    private JPanel panel;
    private BufferedImage image;
    private Timer timer;

    public TGraphicsHostBackend(TGraphicsDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("Graphics device no puede ser null");
        }

        this.device = device;
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

            panel.setDoubleBuffered(true);
            frame.setContentPane(panel);
            //frame.setSize(device.getWidth(), device.getHeight());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
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

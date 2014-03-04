package pl.eurekin.cv.segmentation;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class GUI {

    public static final Color FOREGROUND = Color.RED;
    public static final Color BACKGROUND = Color.BLUE;
    public static BufferedImage image;

    public static void main(String[] args) throws IOException, InvocationTargetException, InterruptedException {
        String imageFile = "/flying-eagle-wallpaper.jpg";
        image = ImageIO.read(GUI.class.getResourceAsStream(imageFile));

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame();

                JLabel label = new JLabel(new ImageIcon(image));
                final LayerUi layerUi = new LayerUi();
                JLayer<JLabel> layer = new JLayer<JLabel>(label, layerUi);

                frame.add(layer);
                JButton start = new JButton("START");
                start.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        segment(layerUi.getOverlay());
                    }
                });
                frame.add(start, BorderLayout.SOUTH);
                frame.pack();

                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLocationByPlatform(true);
                frame.setVisible(true);

            }
        });
    }

    private static void segment(final BufferedImage overlay) {

        new Segmentation(image, new Segmentation.FgOrBg() {
            @Override
            public boolean isBackground(int x, int y) {
                return overlay.getRGB(x, y) == BACKGROUND.getRGB();
            }

            @Override
            public boolean isForeground(int x, int y) {
                return overlay.getRGB(x, y) == FOREGROUND.getRGB();
            }
        });
    }

    public static final class LayerUi<T extends JComponent> extends LayerUI<T> {
        BufferedImage overlay = new BufferedImage(256, 256, BufferedImage.TYPE_4BYTE_ABGR);

        public BufferedImage getOverlay() {
            return overlay;
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            super.paint(g, c);

            Graphics2D g2 = (Graphics2D) g;

            AlphaComposite ac = java.awt.AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5F);
            g2  .setComposite(ac);
            g.setColor(Color.RED);
            g.drawRect(100, 100, 10, 10);
            g.drawImage(overlay, 0, 0, null);
        }

        public void installUI(JComponent c) {
            super.installUI(c);
            // enable mouse motion events for the layer's subcomponents
            ((JLayer) c).setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }

        public void uninstallUI(JComponent c) {
            super.uninstallUI(c);
            // reset the layer event mask
            ((JLayer) c).setLayerEventMask(0);
        }

        @Override
        public void eventDispatched(AWTEvent e, JLayer<? extends T> l) {
            super.eventDispatched(e, l);
            if (e instanceof MouseEvent) {
                MouseEvent event = (MouseEvent) e;

                if ((event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
                    paintDotAt(event.getPoint(), FOREGROUND);
                } else if ((event.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
                    paintDotAt(event.getPoint(), BACKGROUND);
                }
                l.repaint();
            }
        }

        private void paintDotAt(Point point, Color color) {
            int d = 15;
            int x = point.x, y = point.y, w = d, h = d;
            Graphics2D g = (Graphics2D) overlay.getGraphics();
            g.setColor(color);
            g.fillOval(x, y, w, h);
            g.dispose();
        }
    }
}

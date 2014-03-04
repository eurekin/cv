package pl.eurekin.cv.segmentation;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;

public class Segmentation {
    private static final double A_LOT_BUT_SHOULD_BE_K = 1e10;
    Node source, terminal;
    Terminal t;

    Node[][] nodes;
    int[] histogramBg = new int[255];

    int sumFg = 0, sumBg = 0;
    int maxFg = 0, maxBg = 0;
    int[] histogramFg = new int[255];

    // debug weights
    double [][] weightProbFg;
    double [][] weightProbBg;

    public Segmentation(BufferedImage image, FgOrBg decider) {
        int w = image.getWidth();
        int h = image.getHeight();

        //histogram
        calculateHistograms(image, decider, w, h);

        displayHistogramsToUser();

        nodes = new Node[w][h];
        weightProbFg = new double[w][h];
        weightProbBg = new double[w][h];

        source = new Node();
        terminal = new Node();
        int histVal;
        for (int x = 0; x < w; x++) {
            nodes[x] = new Node[h];
            for (int y = 0; y < h; y++) {
                Node node = new Node();
                node.x = x;
                node.y = y;
                nodes[x][y] = node;

                if (decider.isForeground(x, y)) {
                    // no question here, belongs to fg so connect to S
                    weightProbFg[x][y] = 1;
                    new Edge(source, node, A_LOT_BUT_SHOULD_BE_K);
                } else if (decider.isBackground(x, y)) {
                    // no doubt here as well, background connect to T
                    weightProbBg[x][y] = 1;
                    new Edge(terminal, node, A_LOT_BUT_SHOULD_BE_K);
                } else {
                    if ((histVal = histogramFg[intensity(image.getRGB(x, y))]) > 0) {
                        double prob = (double) histVal / (double) sumBg;
                        weightProbFg[x][y] = prob;
                        addEdge(terminal, node, Math.max(-Math.log(prob), 0d));
                    }
                    if ((histVal = histogramBg[intensity(image.getRGB(x, y))]) > 0) {
                        double prob = (double) histVal / (double) sumBg;
                        weightProbBg[x][y] = prob;
                        addEdge(source, node, Math.max(-Math.log(prob), 0d));

                        System.out.println("-Math.log(prob) = " + Math.max(-Math.log(prob), 0d));
                    }
                }

            }
        }

        displayDebugProbDensityOf(weightProbFg);
        displayDebugProbDensityOf(weightProbBg);


        for (int x = 0; x < image.getWidth() - 1; x++) {
            for (int y = 0; y < image.getHeight() - 1; y++) {

                /// new Edge()


            }
        }
    }

    private void displayDebugProbDensityOf(final double[][] probarray) {
        JFrame jFrame = new JFrame("Terminal Node weights");
        jFrame.add(new JPanel(){

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(probarray.length, probarray[0].length);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                for (int x = 0; x < probarray.length; x++) {
                    for (int y = 0; y < probarray[0].length; y++) {
                        int v = (int) (probarray[x][y] * 255);
                        g.setColor(new Color(v, v,v));
                        g.fillRect(x, y, 1, 1);
                    }
                }
            }
        });
        jFrame.setResizable(false);
        jFrame.pack();
        jFrame.setVisible(true);
    }

    private void calculateHistograms(BufferedImage image, FgOrBg decider, int w, int h) {
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (decider.isBackground(i, j)) {
                    int v = intensity(image.getRGB(i, j));
                    histogramBg[v] += 1;
                    sumBg += 1;

                    if (histogramBg[v] > maxBg) {
                        maxBg = histogramBg[v];
                    }
                } else if (decider.isForeground(i, j)) {
                    int v = intensity(image.getRGB(i, j));
                    histogramFg[v] += 1;
                    sumFg += 1;

                    if (histogramFg[v] > maxFg) {
                        maxFg = histogramFg[v];
                    }
                }
            }
        }
    }

    private void displayHistogramsToUser() {
        JFrame jFrame = new JFrame("Foreground histogram");
        jFrame.add(new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(256, 100);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(GUI.FOREGROUND);
                for (int x = 0; x < 255; x++) {
                    if(histogramFg[x]==0) continue;
                    int h = (int) ((((double) histogramFg[x]) / maxFg) * 100);
                    g.drawLine(x, 99, x, 99 - h);
                }
            }
        });
        jFrame.setLocationByPlatform(true);
        jFrame.setResizable(false);
        jFrame.pack();
        jFrame.setVisible(true);

        JFrame jFrameb = new JFrame("Background histogram");
        jFrameb.add(new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(256, 100);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(GUI.BACKGROUND);
                for (int x = 0; x < 255; x++) {
                    if(histogramBg[x]==0) continue;

                    int h = (int) ((((double) histogramBg[x]) / maxBg) * 100);

                    g.drawLine(x, 99, x, 99 - h);
                }
            }
        });
        jFrameb.setLocationByPlatform(true);
        jFrameb.setResizable(false);
        jFrameb.pack();
        jFrameb.setVisible(true);
    }

    private int intensity(int rgb) {
        Color color = new Color(rgb);
        return
                Math.min(255, (int) (
                        0.2126 * color.getRed()
                                + 0.7152 * color.getGreen()
                                + 0.0722 * color.getBlue()));
    }

    void addEdge(Node a, Node b, double capacity) {

        Edge forward = new Edge(a, b, capacity);
        Edge reverse = new Edge(a, b, capacity);

        forward.reverse = reverse;
        reverse.reverse = forward; // :)
    }

    public static interface FgOrBg {
        boolean isBackground(int x, int y);

        boolean isForeground(int i, int j);
    }

    public static class Edge {
        Node a, b;
        Edge reverse;
        double capacity;
        double flow;

        public Edge(Node a, Node b, double capacity) {
            a.addOutgoing(this);

            this.a = a;
            this.b = b;
            this.capacity = capacity;
        }

        public double residualCapacity() {
            return capacity - flow;
        }

        public void pushFlow(double value) {
            flow += value;
            reverse.flow = -this.flow;

            this.updateNode();
            reverse.updateNode();
        }

        private void updateNode() {
            if (residualCapacity() <= 0)
                a.removeOutgoing(this);
        }
    }

    public static class Node {
        int x, y;
        ArrayList<Edge> outgoing = new ArrayList<Edge>(4);

        public Collection<Edge> outgoingResiduals() {
            return outgoing;
        }

        public void addOutgoing(Edge edge) {
            outgoing.add(edge);
        }

        public void removeOutgoing(Edge edge) {
            outgoing.remove(edge);
        }
    }

    public static class Source extends Node {

    }

    public static class Terminal extends Node {

    }
}

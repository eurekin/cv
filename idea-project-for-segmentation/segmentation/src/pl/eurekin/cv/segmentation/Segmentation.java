package pl.eurekin.cv.segmentation;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

public class Segmentation {
    private static final double A_LOT_BUT_SHOULD_BE_K = 1e10;
    Node source, terminal;
    Node[][] nodes;
    int[] histogramBg = new int[255];
    int sumFg = 0, sumBg = 0;
    int maxFg = 0, maxBg = 0;
    int[] histogramFg = new int[255];
    // debug weights
    double[][] weightProbFg;
    double[][] weightProbBg;
    double[][] debugWeightsOnEdges;
    private double debugWeightsOnEdgesMax;

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
                    addEdge(source, node, A_LOT_BUT_SHOULD_BE_K);
                } else if (decider.isBackground(x, y)) {
                    // no doubt here as well, background connect to T
                    weightProbBg[x][y] = 1;
                    addEdge(terminal, node, A_LOT_BUT_SHOULD_BE_K);
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
                    }
                }

            }
        }

        displayDebugProbDensityOf(weightProbFg);
        displayDebugProbDensityOf(weightProbBg);

        debugWeightsOnEdges = new double[(image.getWidth()) * 2][image.getHeight()];
        debugWeightsOnEdgesMax = 0d;

        int width = image.getWidth();
        int height = image.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                Node n = nodes[x][y];

                if (x < width - 1) {
                    Node r = nodes[x + 1][y];
                    double norm1 = norm(intensity(image.getRGB(x, y)), intensity(image.getRGB(x + 1, y)));
                    debugWeightsOnEdges[x * 2][y] = norm1;
                    addEdge(n, r, norm1);

                    if (norm1 > debugWeightsOnEdgesMax) {
                        debugWeightsOnEdgesMax = norm1;
                    }

                }

                if (y < height - 1) {

                    Node d = nodes[x][y + 1];
                    double norm2 = norm(intensity(image.getRGB(x, y)), intensity(image.getRGB(x, y + 1)));
                    addEdge(n, d, norm2);
                    debugWeightsOnEdges[x * 2 + 1][y] = norm2;

                    if (norm2 > debugWeightsOnEdgesMax) {
                        debugWeightsOnEdgesMax = norm2;
                    }
                }
            }
        }

        displayDebugDoubleArrayWithMax(debugWeightsOnEdges, debugWeightsOnEdgesMax);


        findMinCutMaxFlow();
    }

    private void findMinCutMaxFlow() {
        HashSet<Edge> visitedEdges = new HashSet<Edge>();
        HashSet<Node> visitedNodesSet = new HashSet<Node>();
        LinkedList<Node> visitedNodesStack = new LinkedList<Node>();

        LinkedList<Integer> pickedIndices = new LinkedList<Integer>();
        boolean termination = false;

        double maxFlow;
        Edge visitingEdge = null;
        Node visitingNode = source;
        int iterations = 0;

        try {
            do {


                do {
                    maxFlow = Double.MAX_VALUE;



                    int childCount = visitingNode.outgoingResiduals().size();
                    int nextIndex = pickedIndices.peekLast() + 1;
                    if(nextIndex > childCount) {
                        // backtrack
                        pickedIndices.removeLast();
                        Node backtrackedNode = visitedNodesStack.removeLast();
                        visitingNode = backtrackedNode;
                        continue;
                    }
                    visitingEdge = visitingNode.outgoingResiduals().get(   );
                    visitedEdges.add(visitingEdge);
                    maxFlow = Math.min(visitingEdge.capacity, maxFlow);

                    visitingNode = visitingEdge.b;
                } while (!termination);

                for (Edge edge : visitedEdges)
                    edge.pushFlow(maxFlow);

                iterations += 1;

            } while(true); // :)
        } catch (RuntimeException e) {
            System.out.println("iterations = " + iterations);
            System.out.println("visitingEdge = " + visitingEdge);
            System.out.println("visitingNode = " + visitingNode);
            throw e;
        }
    }

    private int randomBelow(int max) {
        return (int) (Math.random() * max);
    }

    private double norm(int a, int b) {
        double d = a - b;
        d /= 256;
        return Math.exp(
                ((double) -(d * d)));
    }

    private void displayDebugDoubleArrayWithMax(final double[][] probarray, final double max) {
        JFrame jFrame = new JFrame("Terminal Node weights");
        jFrame.add(new JPanel() {

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(probarray.length, probarray[0].length);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                for (int x = 0; x < probarray.length; x++) {
                    for (int y = 0; y < probarray[0].length; y++) {
                        int v = (int) (probarray[x][y] / max * 255);
                        g.setColor(new Color(v, v, v));
                        g.fillRect(x, y, 1, 1);
                    }
                }
            }
        });
        jFrame.setResizable(false);
        jFrame.pack();
        jFrame.setVisible(true);
    }

    private void displayDebugProbDensityOf(final double[][] probarray) {
        JFrame jFrame = new JFrame("Terminal Node weights");
        jFrame.add(new JPanel() {

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
                        g.setColor(new Color(v, v, v));
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
                    if (histogramFg[x] == 0) continue;
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
                    if (histogramBg[x] == 0) continue;

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
        Edge reverse = new Edge(b, a, capacity);

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

        @Override
        public String toString() {
            return "Edge{" +
                    "a=" + a +
                    ", b=" + b +
                    ", capacity=" + capacity +
                    ", flow=" + flow +
                    '}';
        }
    }

    public static class Node {
        int x, y;
        ArrayList<Edge> outgoing = new ArrayList<Edge>(4);

        public ArrayList<Edge> outgoingResiduals() {
            return outgoing;
        }

        public void addOutgoing(Edge edge) {
            outgoing.add(edge);
        }

        public void removeOutgoing(Edge edge) {
            outgoing.remove(edge);
        }

        @Override
        public String toString() {
            return "Node{" +
                    "x=" + x +
                    ", y=" + y +
                    ", outgoing=" + outgoing.size() +
                    '}';
        }
    }

}

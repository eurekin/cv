package pl.eurekin.cv.segmentation;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class Segmentation {
    private static final double A_LOT = 1e10;
    Node source;
    Terminal t;

    Node[][] nodes = new Node[w][h];

    public Segmentation(BufferedImage image, FgOrBg decider) {
        int w = image.getWidth();
        int h = image.getHeight();

        for (int x = 0; x < w; x++) {
            nodes[x] = new Node[h];
            for (int y = 0; y < h; y++) {
                Node node = new Node();
                node.x = x;
                node.y = y;
                nodes[x][y] = node;

                if(decider.isBackground(x, y)) {
                    new Edge(source, node, A_LOT);
                }

            }
        }


        for (int x = 0; x < image.getWidth() -1; x++) {
            for (int y = 0; y < image.getHeight()-1; y++) {

                new Edge()


            }
        }
    }

    void addEdge(Node a, Node b, double capacity) {

        Edge forward = new Edge(a, b, capacity);
        Edge reverse = new Edge(a, b, capacity);

        forward.reverse = reverse;
        reverse.reverse = forward; // :)
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
            if(residualCapacity()<= 0)
                a.removeOutgoing(this);
        }
    }

    public static class Node {
        int x,y;
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

    public static interface FgOrBg {
        boolean isBackground(int x, int y);
        boolean isForeground();
    }
}

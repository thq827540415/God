package com.ivi.basic.datastruct.graph;

import java.util.ArrayList;
import java.util.List;

public class AdjacencyList<T> {

    private static class Edge {
        int u;
        int v;
        double weight = -1.0D;

        public Edge(int u, int v) {
            this.u = u;
            this.v = v;
        }
    }

    private List<T> vertices = new ArrayList<>();
    private List<List<Edge>> neighbors = new ArrayList<>();

    public static void main(String[] args) {

    }
}

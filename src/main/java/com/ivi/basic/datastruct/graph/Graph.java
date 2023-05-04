package com.ivi.basic.datastruct.graph;

import java.util.List;

public interface Graph<V> {

    // return the number of vertices in the graph
    int getSize();

    // return the vertices in the graph
    List<V> getVertices();

    // return the object for the specified vertex index
    V getVertex(int index);

    // return the index for the specified vertex object
    int getIndex(V v);

    // return the neighbors of vertex with the specified index
    List<Integer> getNeighbors(int index);

    // return the degree for a specified vertex
    int getDegree(int v);

    // print the edges
    void printEdges();

    // clear the graph
    void clear();

    // add a vertex to the graph
    boolean addVertex(V vertex);

    // add an edge to the graph
    void addEdge(int u, int v);

    // remove a vertex v from the graph, return true if successful
    boolean remove(V v);

    // remove an edge(u, v) from the graph, return true if successful
    boolean remove(int u, int v);
}
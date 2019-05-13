package edu.buffalo.cse.cse486586.simpledynamo;


class Node {
    String id;
    String port;
    Node Successor;
    Node Predecessor;

    Node(String id, String port) {
        this.id = id;
        this.port = port;
        Successor = null;
        Predecessor = null;
    }

    void setSuccessor(String successor_id, String successor_port){
        this.Successor.id = successor_id;
        this.Successor.port = successor_port;
    }

    void setPredecessor(String predecessor_id, String predecessor_port){
        this.Predecessor.id = predecessor_id;
        this.Predecessor.port = predecessor_port;
    }
}
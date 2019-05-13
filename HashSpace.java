package edu.buffalo.cse.cse486586.simpledynamo;

import android.util.Log;

import edu.buffalo.cse.cse486586.simpledynamo.Node;

import static edu.buffalo.cse.cse486586.simpledynamo.SimpleDynamoProvider.TAG;

class HashSpace {
    Node first;
    Node last;

    HashSpace() {
        first = null;
        last = null;
    }

    void add (Node n) {
        if (first == null) {
            n.Successor = n;
            n.Predecessor = n;
            first = n;
            last = n;
        }
        else {
            if (n.id.compareTo(first.id) < 0) {
                if(first == last) {
                    first.Successor = n;
                    first.Predecessor = n;
                    n.Successor = first;
                    n.Predecessor = first;
                    first = n;
                    System.out.println(last);
                }
                else {
                    Node temp = first;
                    first = n;
                    first.Successor = temp;
                    first.Predecessor = last;
                    temp.Predecessor = first;
                    last.Successor = first;
                    System.out.println();
                }
            }
            else if(n.id.compareTo(last.id) > 0) {
                Node temp = last;
                last = n;
                last.Predecessor = temp;
                temp.Successor = last;
                last.Successor = first;
                first.Predecessor = last;
                System.out.println();
            }
            else{
                Node temp1 = null;
                Node temp2 = first;
                while(temp2.id.compareTo(n.id) < 0){
                    temp1 = temp2;
                    temp2 = temp2.Successor;
                }
                temp1.Successor = n;
                n.Predecessor = temp1;
                temp2.Predecessor = n;
                n.Successor = temp2;
                System.out.println("");
            }
        }
    }

    Node getMyNode(String myport) {
        Node temp = first;
        while(temp != last) {
            if (temp.port.equals(myport))
                return temp;
//            temp = temp.Successor;
        }
        return temp;
    }

    String[] getPredecessor_details(Node n){
        String result[] = new String[2];
        result[0] = n.Predecessor.id.toString();
        result[1] = n.Predecessor.port;
        return result;
    }

    String[] getSuccessor_details(Node n){
        String result[] = new String[2];
        result[0] = n.Successor.id.toString();
        result[1] = n.Successor.port;
        return result;
    }

    void displayHashSpace(){
        Node temp = first;
        Log.e(TAG, "Hashspace:\n");
        while(temp != last){
            Log.e(TAG,"Node = "+temp.id.toString()+"("+temp.port+")");
            temp = temp.Successor;
        }
        Log.e(TAG,"Node = "+temp.id.toString()+"("+temp.port+")");
    }

    boolean isPresent (Node n) {
        Node temp = first;
        while(temp != last) {
            if (temp.id.compareTo(n.id) == 0)
                return true;
        }
        if (temp.id.compareTo(n.id) == 0)
            return true;
        return false;
    }
}
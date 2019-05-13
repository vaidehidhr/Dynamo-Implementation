package edu.buffalo.cse.cse486586.simpledynamo;

import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static edu.buffalo.cse.cse486586.simpledynamo.SimpleDynamoProvider.TAG;

class ClientTask extends AsyncTask<String, Void, Void> {

    @Override
    protected Void doInBackground(String... msgs) {
        String strings = "";
        if(msgs[0].contains("insert request")) {
            String destination_port = msgs[2];
//            Log.e(TAG,"Client side: Insert: Destination port = "+destination_port);
            try {
                String request_port = msgs[1];
              Log.e(TAG,"Client side: Insert request: Request port: "+msgs[1]);
//                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(destination_port));
                // Reference: https://stackoverflow.com/questions/14777391/making-a-connection-with-socket-connect-using-timeout/14779241#14779241
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(destination_port)), 500);
                String msgToSend = msgs[0] + msgs[1] + ";" + msgs[3] + ";" + msgs[4] + ";" + msgs[5];
//                Log.e(TAG,"Client side: Insert request: "+msgToSend+" sent to Destination node: "+destination_port);
                DataOutputStream dout_client = new DataOutputStream(socket.getOutputStream());
                dout_client.writeUTF(msgToSend);
                DataInputStream din_client = new DataInputStream(socket.getInputStream());
                String response = din_client.readUTF();
                if (response.equals("Message received")) {
//                    Log.e(TAG, "Client side: Insert request: Insert successful for destination "+destination_port);
                    dout_client.flush();
                    socket.close();
//                    SimpleDynamoProvider s = new SimpleDynamoProvider();
//                    Log.e(TAG, "Client side: Insert: Setting insertion complete to true");
//                    s.insertion_complete = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
//                Log.e(TAG,"Client side: Socket timeout: destination port "+destination_port+" dead");
            }
        }
        else if(msgs[0].contains("query request")) {
//            Log.e(TAG,"Client side: Query request: Request received successfully at client side for "+msgs);
            String destination_node = msgs[2];
            String request_port = msgs[1];
//            Log.e(TAG,"Client side: query request: destination node = "+destination_node);
            try {
//                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(destination_node));
                // Reference: https://stackoverflow.com/questions/14777391/making-a-connection-with-socket-connect-using-timeout/14779241#14779241
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(destination_node)), 500);
                String msgToSend = msgs[0]+msgs[1]+";"+msgs[3]+";"+msgs[4];
//                Log.e(TAG,"Query: Client side: request successfully sent: "+msgToSend+" to port "+destination_node);
                DataOutputStream dout_client = new DataOutputStream(socket.getOutputStream());
                dout_client.writeUTF(msgToSend);
                DataInputStream din_client = new DataInputStream(socket.getInputStream());
                String response = din_client.readUTF();
                if (response.contains("Message received")) {
//                    Log.e(TAG,"Client Side: query: Query returned successfully for selection "+msgs[4]+" from destination port "+destination_node);
                    dout_client.flush();
                    socket.close();
                    response = response.replace("Message received", "");
//                    Log.e(TAG,"Query: Client side: Response received for query "+msgs[4]+" at client side "+response+" from server "+destination_node);
                    SimpleDynamoProvider s = new SimpleDynamoProvider();
                    s.setResponseCursor(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
//                Log.e(TAG,"Client side: Query: Connection request of myport " + request_port + " with destination node " + destination_node + " failed");
//                if(msgs[0].contains("on create")) {
//                    SimpleDynamoProvider s = new SimpleDynamoProvider();
//                    s.socketerrorinoncreate = true;
//                    Log.e(TAG, "Client side: Query: Connection request of myport " + request_port + " with destination node " + destination_node + " failed in oncreate, no action is taken.");
//                }
//                else {
//                    Log.e(TAG, "Client side: Query: Connection request of myport " + request_port + " with destination node " + destination_node + " NOT failed in oncreate, action is taken.");
                    String alternate_port = msgs[5];
                    if ((alternate_port.equals(request_port) && msgs[4].contains("*"))) {
//                        Log.e(TAG,"Client side: Query: For * request, connection to destination "+destination_node+" failed and alternate node is request node "+alternate_port);
                        SimpleDynamoProvider s = new SimpleDynamoProvider();
                        s.alt_node_req_node = true;
                    }
                    else {
//                        Log.e(TAG, "Client side: Query: Connection request of myport " + request_port + " with destination node " + destination_node + " failed. Forwarding request to alternate port " + alternate_port);
//                        Log.e(TAG, "******************************************** Alternate port ***********************************************");
                        try {
//                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(destination_node));
                            // Reference: https://stackoverflow.com/questions/14777391/making-a-connection-with-socket-connect-using-timeout/14779241#14779241
                            Socket socket = new Socket();
                            socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(alternate_port)), 500);
                            String msgToSend = msgs[0] + msgs[1] + ";" + msgs[3] + ";" + msgs[4];
//                            Log.e(TAG, "Query: Client side: request sent: " + msgToSend);
                            DataOutputStream dout_client = new DataOutputStream(socket.getOutputStream());
                            dout_client.writeUTF(msgToSend);
                            DataInputStream din_client = new DataInputStream(socket.getInputStream());
                            String response = din_client.readUTF();
                            if (response.contains("Message received")) {
//                                Log.e(TAG, "Client Side: query: Query returned successfully for selection "+msgs[4]+" from alternate node "+alternate_port);
                                dout_client.flush();
                                socket.close();
                                response = response.replace("Message received", "");
//                                Log.e(TAG, "Query: Client side: Response received at client side " + response + " from server " + destination_node);
                                SimpleDynamoProvider s = new SimpleDynamoProvider();
                                s.setResponseCursor(response);
                            }
                        } catch (IOException e2) {
                            e.printStackTrace();
                            if(msgs[0].contains("on create")) {
                                SimpleDynamoProvider s = new SimpleDynamoProvider();
                                s.socketerrorinoncreate = true;
//                                Log.e(TAG,"Client side: Setting socketerrorinoncreate true");
                            }
//                            Log.e(TAG, "Client side: Query: Connection failed to alternate port too");
                        }
                    }
//                }
            }
        }
        else if(msgs[0].contains("delete request")) {
            String destination_node = msgs[2];
            String request_port = msgs[1];
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(destination_node)), 500);
                String msgToSend = msgs[0] + msgs[1] + ";" + msgs[3] + ";" + msgs[4];
//                Log.e(TAG, "Query: Client side: request successfully sent: " + msgToSend + " to port " + destination_node);
                DataOutputStream dout_client = new DataOutputStream(socket.getOutputStream());
                dout_client.writeUTF(msgToSend);
                DataInputStream din_client = new DataInputStream(socket.getInputStream());
                String response = din_client.readUTF();
                if (response.contains("Message received")) {
//                    Log.e(TAG, "Client Side: query: Query deleted successfully at destination node " + destination_node);
                    dout_client.flush();
                    socket.close();
                }
            } catch (IOException e) {
//                e.printStackTrace();
                Log.e(TAG,"Destination node "+destination_node+" is dead. So couldn't send query request.");
            }
        }
        return null;
    }
}
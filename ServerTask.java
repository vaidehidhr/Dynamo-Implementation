package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

class ServerTask extends AsyncTask<Void, String, Void> {
    static final String REMOTE_PORT[]= {"11108", "11112", "11116", "11120", "11124"};
    ServerSocket serverSocket;
    String myPort, hashed_port;
    Uri mUri;
    ContentValues mContentValues;
    ContentResolver cr;
    HashSpace h;
    Node mynode;
    final String TAG = ServerTask.class.getSimpleName();

    ServerTask(ServerSocket serverSocket, HashSpace h, String myPort, Node mynode, ContentResolver cr){
        this.serverSocket = serverSocket;
        this.h = h;
        this.myPort = myPort;
        this.mynode = mynode;
        this.cr = cr;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            while (true) {
                Socket socket_s = serverSocket.accept();
                DataInputStream din_server = new DataInputStream(socket_s.getInputStream());
                String strings= din_server.readUTF();

                if (strings.contains("insert request")){
//                    Log.e(TAG,"Server side: insert request: Received insert request from client: "+strings);
                    strings = strings.replace("insert request","");
                    int separator = strings.indexOf(";");
                    String request_port = strings.substring(0, separator);
                    strings = strings.replace(request_port,"");
                    strings = strings.replaceFirst(";","");
                    separator = strings.indexOf(";");
                    String uri_str = strings.substring(0,separator);
                    strings = strings.replace(uri_str,"");
                    strings = strings.replaceFirst(";","");
                    separator = strings.indexOf(";");
                    String old_key = strings.substring(0,separator);
                    strings = strings.replace(old_key,"");
                    String msg = strings.replace(";","");
//                    Log.e(TAG,"Server side: Insert request: My port "+myPort+" request_port: "+request_port+" uri_str "+uri_str+" old_key "+old_key+" msg "+msg);
                    SimpleDynamoProvider s = new SimpleDynamoProvider();
                    mUri = s.buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");
//                    Log.e(TAG, "Server side: initializing key and value");
                    mContentValues = s.initTestValues("Sent from server"+old_key, msg);
                    cr.insert(mUri, mContentValues).toString();
//                    Log.e(TAG, "Server side: Insert request: Insert successful");
                    DataOutputStream dout_server = new DataOutputStream(socket_s.getOutputStream());
                    dout_server.writeUTF("Message received");
                }

                if (strings.contains("query request")) {
                    strings = strings.replace("query request", "");
                    int separator = strings.indexOf(";");
                    String request_port = strings.substring(0, separator);
                    strings = strings.replace(request_port, "");
                    strings = strings.replaceFirst(";", "");
                    separator = strings.indexOf(";");
                    String uri_str = strings.substring(0, separator);
                    strings = strings.replace(uri_str, "");
                    String selection_key = strings.replace(";", "");
                    SimpleDynamoProvider s = new SimpleDynamoProvider();
//                    Log.e(TAG,"Server side: Query: Query request received at server side: "+strings+" from client "+request_port);
                    mUri = s.buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");
                    Cursor resultCursor = cr.query(mUri, null, selection_key+";"+request_port, null, null);
                    DataOutputStream dout_server = new DataOutputStream(socket_s.getOutputStream());
                    resultCursor.moveToFirst();
                    // Reference: https://stackoverflow.com/questions/18863816/putting-cursor-data-into-an-array
                    ArrayList<String> keys = new ArrayList<String>();
                    ArrayList<String> values = new ArrayList<String>();
                    while(!resultCursor.isAfterLast()) {
                        keys.add(resultCursor.getString(0));
                        values.add(resultCursor.getString(1));
                        resultCursor.moveToNext();
                    }
                    resultCursor.close();
                    String msgtosend = "Message received"+keys.toString()+";"+values.toString();
//                    Log.e(TAG,"Server side: query: response returned from server side: "+msgtosend+" to client "+request_port);
                    dout_server.writeUTF(msgtosend);
//                    Log.e(TAG,"Server side: query: Cursor is returned to client: "+request_port+" key "+keys.toString()+" value "+values.toString()+" from server "+myPort);
                }
                if (strings.contains("delete request")) {
                    strings = strings.replace("delete request", "");
                    int separator = strings.indexOf(";");
                    String request_port = strings.substring(0, separator);
                    strings = strings.replace(request_port, "");
                    strings = strings.replaceFirst(";", "");
                    separator = strings.indexOf(";");
                    String uri_str = strings.substring(0, separator);
                    strings = strings.replace(uri_str, "");
                    String selection_key = strings.replace(";", "");

                    SimpleDynamoProvider s = new SimpleDynamoProvider();
                    mUri = s.buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");
                    try {
                        cr.delete(mUri, selection_key, null);
                    }
                    catch (NullPointerException e){
                        System.out.println("NullPointerException");
                    }
                    DataOutputStream dout_server = new DataOutputStream(socket_s.getOutputStream());
                    dout_server.writeUTF("Message received");
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
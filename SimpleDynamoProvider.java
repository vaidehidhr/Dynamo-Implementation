package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import static android.content.ContentValues.TAG;

public class SimpleDynamoProvider extends ContentProvider {

    String myPort;
    static final String REMOTE_PORT[] = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;
    static final String TAG = SimpleDynamoProvider.class.getSimpleName();
    HashSpace h;
    Node mynode;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    String columnnames[] = new String[] {"key","value"};
    static MatrixCursor responseCursor= null;
//    boolean insertion_complete_oncreate = false;
    static boolean set = false;
    static boolean ready_for_next_query = true;
    static boolean socketerrorinoncreate = false;
    static boolean alt_node_req_node = false;
    boolean ready_for_next_delete;

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
//        Log.e(TAG, "Entering onCreate");
        Context context = getContext();
        TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
//        Log.e(TAG,"SimpleDynamoProvider: onCreate: myport = "+myPort);
        ContentResolver cr = (ContentResolver)context.getContentResolver();
        h = new HashSpace();
        String hashed_port = null;
        try {
            hashed_port = genHash(portStr);
            mynode = new Node(hashed_port, myPort);
            h.add(mynode);
            for (int i = 0; i < REMOTE_PORT.length; i++) {
                String port = REMOTE_PORT[i];
                if (!port.equals(myPort)) {
                    String half_port = Integer.toString((Integer.parseInt(port)/2));
                    String hash = genHash(half_port);
                    Node n = new Node(hash, port);
                    h.add(n);
                }
            }
//            Log.e(TAG,"SimpleDynamoProvider: Hashspace for port:"+myPort);
            Node temp = h.first;
            while(temp != h.last) {
//                Log.e(TAG,temp.port);
                temp = temp.Successor;
            }
//            Log.e(TAG,temp.port);

//            Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");
//            delete(mUri, "ntaGSwCB28ehGALa1wrFzW2t7Xj4oA6x", null);
//            Uri uri2 = buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");
//            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "query request", "11120", mynode.Successor.port, uri2.toString(), "*", mynode.Successor.Successor.port);
            /************************ Deleting old files on creation of a node ***********************/
//            Log.e(TAG,"SimpleDynamoProvider: OnCreate: Deleting old files ");
            String file_list[]=getContext().fileList();
            int p = 0;
            while (p < file_list.length) {
                boolean success = context.deleteFile(file_list[p]);
                p++;
            }
            /************************** Inserting correct keys/ files on creation of a node **************************/
//            Log.e(TAG,"SimpleDynamoProvider: OnCreate: Inserting correct files/ keys");
            String destination_ports[] = {mynode.Predecessor.port, mynode.Predecessor.Predecessor.port, mynode.Successor.port, mynode.Successor.Successor.port};
            Uri uri = buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");
            String columnnames[] = new String[]{"key", "value"};
            MatrixCursor cursor = new MatrixCursor(columnnames);
            ArrayList<String> keys = new ArrayList<String>();
            ArrayList<String> values = new ArrayList<String>();
            while(!ready_for_next_query) {
//                Log.e(TAG,"SimpleDynamo: onCreate: Not yet ready for next query. Putting thread to sleep");
                Thread.sleep(100);
            }
            ready_for_next_query = false;
            for (int k = 0; k < destination_ports.length; k++) {
                if (k == 0 || k == 2){
//                Log.e(TAG, "SimpleDynamoProvider: OnCreate: getting replicas from " + destination_ports[k]);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "query request on create", myPort, destination_ports[k], uri.toString(), "@", destination_ports[k+1]);
                while (set == false && socketerrorinoncreate == false) {
//                    Log.e(TAG,"SimpleDynamoProvider: oncreate: set and socketerrorinoncreate false. Putting thread to sleep");
                    Thread.sleep(100);
                }
                if (socketerrorinoncreate) {
//                    Log.e(TAG, "SimpleDynamoProvider: socketerrorinoncreate set by client");
                    socketerrorinoncreate = false;
                    ready_for_next_query = true;
                    continue;
                }
//                Log.e(TAG, "SimpleDynamoProvider: onCreate: Setting cursor to response cursor ");
                cursor = responseCursor;
                responseCursor = null;
                set = false;
//                Log.e(TAG, "SimpleDynamoProvider onCreate: cursor is set in main thread ");
                cursor.moveToFirst();
                // Reference: https://stackoverflow.com/questions/18863816/putting-cursor-data-into-an-array
                while (!cursor.isAfterLast()) {
                    String key_iter = cursor.getString(0);
                    String value_iter = cursor.getString(1);
//                    Log.e(TAG,"onCreate: Processing key "+key_iter);
                    if (!keys.contains(key_iter)) {
                        keys.add(key_iter);
                        String filecontents = key_iter + "." + value_iter;
                        String hashed_key = genHash(key_iter);
                        Node n = h.first;
                        while (n != h.last) {
                            boolean Successor_check = isSuccessor(hashed_key, n);
                            if (Successor_check == true)
                                break;
                            else
                                n = n.Successor;
                        }
                        String destination_ports2[] = {mynode.Predecessor.Predecessor.port, mynode.Predecessor.port, mynode.port};
                        for (int i = 0; i < destination_ports2.length; i++) {
//                            Log.e(TAG, "SimpleDynamoProvider: destination_ports2 list: "+destination_ports2[i]);
                            if (n.port.equals(destination_ports2[i])) {
                                FileOutputStream outputStream;
                                try {
                                    outputStream = context.openFileOutput(hashed_key, Context.MODE_PRIVATE);
                                    outputStream.write(filecontents.getBytes());
                                    outputStream.close();
                                } catch (Exception e) {
                                    Log.e(TAG, "SimpleDynamoProvider: File write failed");
                                }
                                break;
                            }
                        }
//                        Log.e(TAG,"onCreate: key "+key_iter+" processed successfully");
                    }
                    cursor.moveToNext();
                }
                cursor.close();
                ready_for_next_query = true;
            }
            }
//            Log.e(TAG,"onCreate: All relevant keys successfully inserted on port "+myPort);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        set = true;
//        insertion_complete_oncreate = true;
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask(serverSocket, h, myPort, mynode, cr).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (android.os.NetworkOnMainThreadException e) {
            Log.e(TAG, "Can't create a ServerSocket");
        } catch (IOException e) {
            e.printStackTrace();
        }
//        Log.e(TAG,"OnCreate complete");
        return false;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
//        Log.e(TAG, "SimpleDynamoProvider: insert: trying to insert "+values);
//        while(!insertion_complete_oncreate){
//            Log.e(TAG,"SimpleDynamo: Insert: Oncreate insertion is still going on. Puttong thread to sleep.");
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
        boolean sent_from_server = false;
        boolean local_storage = false;

        String arg = values.toString();
//        Log.e(TAG,"SimpleDynamoProvider: arg = "+arg);
        if (arg.contains("Sent from server")) {
//            Log.e(TAG, "Sent from server is true");
            sent_from_server = true;
            arg = arg.replace("Sent from server","");
//            Log.e(TAG,"SimpleDynamoProvider: arg after removing sent from server = "+arg);
        }

        String temp[] = arg.split("=");
        String old_key = temp[2];
        try {
            String hashed_key = genHash(old_key);
            String msg = temp[1].replace(" key", "");
            String filecontents = old_key + "." + msg;

            Node n = h.first;
            while (n != h.last) {
                boolean Successor_check = isSuccessor(hashed_key, n);
                if (Successor_check == true)
                    break;
                else
                    n = n.Successor;
            }
//            Log.e(TAG, "SimpleDynamoProvider: insert: Successor of hashed key "+hashed_key+", key "+old_key+" is "+n.port+", "+n.id);
//            Log.e(TAG, "SimpleDynamoProvider: myport = "+myPort);
            String destination_ports[] = {n.port, n.Successor.port, n.Successor.Successor.port};
            for (int i = 0; i < destination_ports.length; i++){
//                Log.e(TAG, "SimpleDynamoProvider: destination_ports list: "+destination_ports[i]);
                if(myPort.equals(destination_ports[i]))
                    local_storage = true;
            }
            if (local_storage) {
//                Log.e(TAG,"SimpleDynamoProvider: insert: local storage true. Hashed key "+hashed_key+", key "+old_key+" inserted to node "+mynode.id);
                FileOutputStream outputStream;
                Context context = getContext();
                try {
                    outputStream = context.openFileOutput(hashed_key, Context.MODE_PRIVATE);
                    outputStream.write(filecontents.getBytes());
                    outputStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "SimpleDynamoProvider: File write failed");
                }
                if(!sent_from_server){
//                    Log.e(TAG, "SimpleDynamoProvider: insert request for LOCAL key "+old_key+", hash "+hashed_key+" generated LOCALLY at port "+myPort);
                    for (int i = 0; i<destination_ports.length; i++) {
                        if(destination_ports[i] != myPort) {
//                            Log.e(TAG, "SimpleDynamoProvider: Destination port = " + destination_ports[i]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "insert request", myPort, destination_ports[i], uri.toString(), old_key, msg);
                        }
                    }
                }
//                else {
//                    Log.e(TAG, "SimpleDynamoProvider: insert request for LOCAL key "+old_key+", hash "+hashed_key+" invoked from server at port "+myPort);
//                    sent_from_server = false;
//                    Log.e(TAG,"SimpleDynamoProvider: Sent from server is set back to false");
//                }
                Log.v("insert", values.toString());
                return uri;
            } else {
//                Log.e(TAG, "SimpleDynamoProvider: insert: Local storage false: Sending key to other nodes for insertion");
                for (int i = 0; i<destination_ports.length; i++) {
//                    Log.e(TAG,"SimpleDynamoProvider: insert: Destination port = "+destination_ports[i]);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "insert request", myPort, destination_ports[i], uri.toString(), old_key, msg);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    boolean isSuccessor(String hashed_key, Node mynode) {
        if (mynode.Successor == mynode)
            return true;
        else {
            if (mynode.id.compareTo(hashed_key) > 0 && mynode.Predecessor.id.compareTo(hashed_key) < 0) {
                return true;
            }
            else if (mynode.Predecessor.id.compareTo(mynode.id) > 0 && hashed_key.compareTo(mynode.Predecessor.id) > 0)
                return true;
            else if (mynode.Predecessor.id.compareTo(mynode.id) > 0 && mynode.id.compareTo(hashed_key) > 0)
                return true;
        }
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // TODO Auto-generated method stub
//        Log.e(TAG,"SimpleDynamo: Query: "+selection);
//        while(!insertion_complete_oncreate){
//            try {
//                Log.e(TAG,"SimpleDynamoProvider: Query: Sleep on create");
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        Log.e(TAG, "Querying " + selection);
        while(!ready_for_next_query) {
            try {
//                Log.e(TAG,"SimpleDynamoProvider: Query: not ready for next query "+selection+". Putting thread to sleep");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ready_for_next_query = false;
//        Log.e(TAG,"SimpleDynamoProvider: Query: ready for next query true, setting it to false");
        String request_port = "";
        int separator = selection.indexOf(";");
        if (separator != -1) {
            request_port = selection.substring((separator + 1));
            selection = selection.replace(request_port, "");
            selection = selection.replace(";", "");
//            Log.e(TAG,"SimpleDynamoProvider: Query: Query request for selection "+selection+" received by main thread from server requested from client port "+request_port);
        }
        String columnnames[] = new String[]{"key", "value"};
        MatrixCursor cursor = new MatrixCursor(columnnames);

        String file_list[] = getContext().fileList();
        int i = 0;
        if (selection.equals("@")) {
//            Log.e(TAG, "Check for @");
                while (i < file_list.length) {
                    FileInputStream temp;
                    Context context = getContext();
                    try {
                        // Reference: https://stackoverflow.com/questions/9095610/android-fileinputstream-read-txt-file-to-string
                        temp = context.openFileInput(file_list[i]);
                        StringBuffer fileContent = new StringBuffer("");
                        byte[] buffer = new byte[1024];
                        int n;
                        while ((n = temp.read(buffer)) != -1) {
                            fileContent.append(new String(buffer, 0, n));
                        }
                        int separator_index = fileContent.indexOf(".");
                        String old_key = fileContent.substring(0, separator_index);
                        String msg = fileContent.substring(separator_index + 1);
                        cursor.addRow(new Object[]{old_key, msg});
                        i++;
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
//            ready_for_next_query = true;
            } else if (selection.equals("*")) {
//                Log.e(TAG, "SimpleDynamoProvider: Query: Check for * on global storage");
                if (!mynode.Successor.port.equals(request_port)) {
                    if (request_port.equals(""))
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "query request", myPort, mynode.Successor.port, uri.toString(), selection, mynode.Successor.Successor.port);
                    else
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "query request", request_port, mynode.Successor.port, uri.toString(), selection, mynode.Successor.Successor.port);
                    while ((set == false) && (alt_node_req_node == false)) {
                        try {
                            Thread.sleep(100);
//                            Log.e(TAG,"SimpleDynamo: Query: set and alt_node_req_node false. Putting thread to sleep");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if(set == true) {
                        cursor = responseCursor;
                        responseCursor = null;
                        set = false;
//                        Log.e(TAG, "SimpleDynamoProvider: Query: Set was true, reset back to false");
                    }

                    if(alt_node_req_node == true){
                        alt_node_req_node = false;
//                        Log.e(TAG, "SimpleDynamoProvider: Query: alt_node_req_node was true, reset back to false");
                    }
//                    Log.e(TAG, "Just checking");
                    int f = 0;
                    while (f < file_list.length) {
                        FileInputStream temp;
                        Context context = getContext();
                        try {
                            // Reference: https://stackoverflow.com/questions/9095610/android-fileinputstream-read-txt-file-to-string
                            temp = context.openFileInput(file_list[f]);
                            StringBuffer fileContent = new StringBuffer("");
                            byte[] buffer = new byte[1024];
                            int n;
                            while ((n = temp.read(buffer)) != -1) {
                                fileContent.append(new String(buffer, 0, n));
                            }
                            int separator_index = fileContent.indexOf(".");
                            String old_key = fileContent.substring(0, separator_index);
                            String msg = fileContent.substring(separator_index + 1);
                            cursor.addRow(new Object[]{old_key, msg});
                            f++;
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
            try {
                String hashed_selection = genHash(selection);
                boolean local_storage = false;
                Node n = h.first;
                while (n != h.last) {
                    boolean Successor_check = isSuccessor(hashed_selection, n);
                    if (Successor_check == true)
                        break;
                    else
                        n = n.Successor;
                }
//                Log.e(TAG, "SimpleDynamoProvider: query: Successor of hashed selection " + hashed_selection + ", key " + selection + " is " + n.port + ", " + n.id);
//                Log.e(TAG, "SimpleDynamoProvider: myport = " + myPort);
                String destination_ports[] = {n.port, n.Successor.port, n.Successor.Successor.port};
                for (int j = 0; j < destination_ports.length; j++) {
                    if (myPort.equals(destination_ports[j])) {
//                        Log.e(TAG, "SimpleDynamoProvider: Query: Setting local storage true for selection " + selection + " for port " + destination_ports[j] + " myport " + myPort);
                        local_storage = true;
                    }
                }
                if (local_storage) {
//                    Log.e(TAG,"SimpleDynamoProvider: query: Inside Local storage for selection "+selection);
                    while (i < file_list.length) {
                        if (file_list[i].equals(hashed_selection)) {
                            FileInputStream temp;
                            Context context = getContext();
                            try {
                                //https://stackoverflow.com/questions/9095610/android-fileinputstream-read-txt-file-to-string
                                temp = context.openFileInput(file_list[i]);
                                StringBuffer fileContent = new StringBuffer("");
                                byte[] buffer = new byte[1024];
                                int m;
                                while ((m = temp.read(buffer)) != -1) {
                                    fileContent.append(new String(buffer, 0, m));
                                }
                                int separator_index = fileContent.indexOf(".");
                                String old_key = fileContent.substring(0, separator_index);
                                String msg = fileContent.substring(separator_index + 1);
                                cursor.addRow(new Object[]{old_key, msg});
//                                Log.e(TAG, "SimpleDynamoProvider: query: found: key " + old_key + " message"+msg);
                                break;
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else
                            i++;
                    }
                } else {
//                    Log.e(TAG, "SimpleDynamoProvider: query: local storage false, forwarding query request "+selection+" to correct node "+n.port);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "query request", myPort, n.port, uri.toString(), selection, n.Successor.port);
                    while (set == false) {
//                        Log.e(TAG,"SimpleDynamoProvider: Query: Set is false. Waiting for response from main node "+n.port+". Putting thread to sleep.");
                        Thread.sleep(100);
                    }

//                    Log.e(TAG, "SimpleDynamoProvider: Query: Setting cursor to response cursor ");
                    cursor = responseCursor;
//                    Log.e(TAG, "SimpleDynamoProvider Query: cursor is set in main thread ");
                    responseCursor = null;
                    set = false;
//                    ready_for_next_query = true;
//                    Log.e(TAG, "SimpleDynamoProvider: returning from main thread " + cursor + " at port " + myPort+" for client "+request_port+" key "+selection);
//                    Log.e(TAG,"SimpleDynamoProvider: Query: query operation complete for query "+selection+", port "+request_port+" setting ready for next query to true");
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ready_for_next_query = true;
//        Log.e(TAG,"SimpleDynamoProvider: Query: query operation complete for query "+selection+", port "+request_port+" setting ready for next query to true");
        Log.v("query", selection);
        return cursor;
    }

    void setResponseCursor(String cursor_string){
//        Log.e(TAG, "setresponsecursor: Query: Response received from the client: "+myPort+": "+cursor_string);
        responseCursor = new MatrixCursor(columnnames);
        int separator = cursor_string.indexOf(";");
        String old_key = cursor_string.substring(0, separator);
        String value = cursor_string.substring(separator+1);
        old_key = old_key.replace(" ","").replace("[","").replace("]","");
        value = value.replace(" ","").replace("[","").replace("]","");
        String old_key_arr[] = old_key.split(",");
        String values_arr[] = value.split(",");
        for (int i = 0; i < old_key_arr.length; i++) {
            responseCursor.addRow(new Object[]{old_key_arr[i], values_arr[i]});
//            Log.e(TAG, "setResponseCursor: Query: responseCursor set" + old_key_arr[i] + ";" + values_arr[i] + ", setting set to true");
        }
        set = true;
    }

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
//		Log.e(TAG,"SimpleDynamoProvider: Deleting key "+selection);
        String file_list[]=getContext().fileList();
        int i = 0;
        if(selection.equals("@")) {
//            Log.e(TAG, "delete for @/ delete for local");
            while (i < file_list.length) {
                Context context = getContext();
                boolean success = context.deleteFile(file_list[i]);
                i++;
            }
        }
        else {
            try {
                boolean received_from_server = false;
                if(selection.contains("Delete replica")){
                    selection = selection.replace("Delete replica","");
                    received_from_server = true;
                }
                String hashed_selection_str = genHash(selection);
                while (i < file_list.length) {
                    if (file_list[i].equals(hashed_selection_str)) {
                        Context context = getContext();
                        boolean success = context.deleteFile(file_list[i]);
                        break;
                    }
                    else
                        i++;
                }
                if (!received_from_server) {
//                    Log.e(TAG,"SimpleDynamoProvider: delete: request for key "+selection+" hash "+hashed_selection_str+" generated locally");
                    Node n_tem = h.first;
                    while (n_tem != h.last) {
                        boolean Successor_check = isSuccessor(hashed_selection_str, n_tem);
                        if (Successor_check == true)
                            break;
                        else
                            n_tem = n_tem.Successor;
                    }
//            Log.e(TAG, "SimpleDynamoProvider: insert: Successor of hashed key "+hashed_key+", key "+old_key+" is "+n.port+", "+n.id);
//            Log.e(TAG, "SimpleDynamoProvider: myport = "+myPort);
                    String destination_ports[] = {n_tem.port, n_tem.Successor.port, n_tem.Successor.Successor.port};
//                    Log.e(TAG,"Working till here perfectly");
                    for (int del_ite = 0; del_ite < destination_ports.length; del_ite++) {
//                Log.e(TAG, "SimpleDynamoProvider: destination_ports list: "+destination_ports[i]);
                        if (!myPort.equals(destination_ports[del_ite])) {
//                            Log.e(TAG, "SimpleDynamoProvider: delete: request for key "+selection+" forwarding to node "+destination_ports[del_ite]);
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "delete request", myPort, destination_ports[del_ite], uri.toString(), selection + "Delete replica");
                        }
                    }
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
//		Log.e(TAG, "Updating selection");
		return 0;
	}

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    ContentValues initTestValues(String old_key, String msg){
//        Log.e(TAG,"initTestValues: old_key = "+old_key+" , msg = "+msg);
        ContentValues cv = new ContentValues();
        cv.put(KEY_FIELD, old_key);
        cv.put(VALUE_FIELD, msg);
        return cv;
    }
}
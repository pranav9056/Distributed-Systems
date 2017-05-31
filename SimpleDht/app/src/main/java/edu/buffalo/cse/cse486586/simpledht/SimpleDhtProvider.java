package edu.buffalo.cse.cse486586.simpledht;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import static java.lang.Math.abs;
import static java.lang.Math.log;
import static java.lang.Thread.sleep;

public class SimpleDhtProvider extends ContentProvider {


    static final String TAG = SimpleDhtProvider.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    Uri mUri;
    String singleresult;
    String myPort;
    String emno;
    String hashid;
    String sucid;
    String predid;
    String sucidpn;
    String predidpn;
    Map<String,String> multipleresult = new HashMap<String, String>();
    Map<String,String> joinorder = new TreeMap();
    int flag = 0;
    int f=0;

    Map<String,String> stored = new HashMap();
    // To send msgs
    public void sendmsg(String mtype){
        /*
         * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
         * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
         * the difference, please take a look at
         * http://developer.android.com/reference/android/os/AsyncTask.html
         */
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, mtype,myPort);

    }
    //for single query
    public void sendmsgq(String mtype){
        /*
         * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
         * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
         * the difference, please take a look at
         * http://developer.android.com/reference/android/os/AsyncTask.html
         */
        try {
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, mtype,myPort).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        //return tempo.resultvalue;
    }
    // To find out port and emulator number
    public void getport(Context con) {
        TelephonyManager tel = (TelephonyManager) con.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        if(myPort.equals(REMOTE_PORT0)){
            emno = "5554";
        }
        else if(myPort.equals(REMOTE_PORT1)){
            emno = "5556";
        }
        else if(myPort.equals(REMOTE_PORT2)){
            emno = "5558";
        }
        else if(myPort.equals(REMOTE_PORT3)){
            emno = "5560";
        }
        else if(myPort.equals(REMOTE_PORT4)){
            emno = "5562";
        }
        try {
            hashid = genHash(emno);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String hash_key=null;
        try {
            hash_key = genHash(selection);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Context context = getContext();
        SharedPreferences sharedPref = context.getSharedPreferences("Data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        if(flag==1){
            editor.remove(hash_key);
            stored.remove(hash_key);
            editor.commit();
            Log.d(TAG,""+stored);
        }
        else{
            if(lookup(hash_key)) {
                editor.remove(hash_key);
                stored.remove(hash_key);
                editor.commit();
                Log.d(TAG,""+stored);
            }
            else{
                sendmsg("5"+","+selection);
            }
        }




        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }
    public boolean lookup(String id){
        if(id.compareTo(predid)>0 && id.compareTo(hashid)<=0){
            return true;
        }
        else if(id.compareTo(predid)<0 && id.compareTo(hashid)<=0 && predid.compareTo(hashid)>0){
            return true;
        }
        else if(id.compareTo(predid)>0 && id.compareTo(hashid)>=0 && predid.compareTo(hashid)>0){
            return true;
        }
        else{
            return false;
        }
    }
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        //Log.d(TAG,"flag: "+flag);
        // TODO Auto-generated method stub

        // case where only one process is there
        if (flag==1){
            Context context = getContext();
            SharedPreferences sharedPref = context.getSharedPreferences("Data", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            String hash_key = null;
            try {
                hash_key = genHash(values.getAsString("key"));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                Log.e("SHA", "Could not hash" + values.getAsString("key") );
            }
            editor.putString(hash_key,values.getAsString("value"));
            editor.commit();
            stored.put(hash_key,values.getAsString("key"));
            //Log.d(TAG,stored.toString());
            Log.v("insert", values.toString());
            return uri;
        }
        else if (flag==2){
            String hash_key = null;
            try {
                hash_key = genHash(values.getAsString("key"));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                Log.e("SHA", "Could not hash" + values.getAsString("key") );
            }
            if(lookup(hash_key)){
                Context context = getContext();
                SharedPreferences sharedPref = context.getSharedPreferences("Data", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(hash_key,values.getAsString("value"));
                editor.commit();
                stored.put(hash_key,values.getAsString("key"));
                //Log.d(TAG,stored.toString());
                Log.v("insert", values.toString());
                return uri;

            }
            else{
                sendmsg("2"+","+values.getAsString("key")+","+values.getAsString("value"));
            }



        }
        /*

        Context context = getContext();
        SharedPreferences sharedPref = context.getSharedPreferences("Data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String hash_key = null;
        try {
            hash_key = genHash(values.getAsString("key"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e("SHA", "Could not hash" + values.getAsString("key") );
        }
        editor.putString(hash_key,values.getAsString("value"));
        editor.commit();
        Log.v("insert", values.toString());
        */return uri;

    }

    @Override
    public boolean onCreate() {
        Context con = getContext();
        getport(con);
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
        //Log.d(TAG,"got port");
        // TODO Auto-generated method stub
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return false; // added false
        }



        sendmsg("0");
        /*try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub


            Context context = getContext();
            SharedPreferences sharedPref = context.getSharedPreferences("Data", Context.MODE_PRIVATE);
            String[] colName = {"key", "value"};
            MatrixCursor sendCursor = new MatrixCursor(colName);
            //Set<String> getkeys;
            if (selection.equals("@") ) {
                //Log.d(TAG,"in @");

                Set<String> getkeys = stored.keySet();
               // Log.d(TAG,"get key "+getkeys);
                for (String key : getkeys) {
                    String valStore = sharedPref.getString(key, "");
                    //Log.d(TAG,"value got: "+valStore);
                    String keyUnhash = stored.get(key);
                    //Log.d(TAG,"key got: "+keyUnhash);
                    Object[] row = {keyUnhash, valStore};
                    sendCursor.addRow(row);
                }
                return sendCursor;
            }
            else if (selection.equals("*")) {
                Log.d(TAG,"in *");

                Set<String> getkeys = stored.keySet();
                for (String key : getkeys) {
                    String valStore = sharedPref.getString(key, "");
                    String keyUnhash = stored.get(key);
                    Object[] row = {keyUnhash, valStore};
                    sendCursor.addRow(row);
                }
                    if (flag == 1) {
                        Log.d(TAG,"here flag 1");
                        return sendCursor;
                    }
                    else{

                        sendmsgq("4"+","+hashid);
                        Set<String> putter=multipleresult.keySet();
                        for(String key: putter){
                            String valstore = multipleresult.get(key);
                            Object[] row = {key,valstore};
                            sendCursor.addRow(row);
                        }
                        return sendCursor;
                    }


                }


             else {
               // Log.d(TAG,"in local");
                String hash_key = null;
                try {
                    hash_key = genHash(selection);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    Log.e("SHA", "Could not hash" + selection);
                }
                String valStore = sharedPref.getString(hash_key, "");
                if (valStore.equals("")){
                    //fatne
                    sendmsgq("3"+","+hash_key+","+selection+","+myPort);
                    valStore = singleresult;
                    Log.d(TAG,"got"+valStore);
                }

                Object[] row = {selection, valStore};
                sendCursor.addRow(row);


                Log.v("query", "sel-------" + selection + "fetch-------" + valStore + "hk-------" + hash_key);
                return sendCursor;


            }


               // return sendCursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
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
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            while(true) {
                try {
                    Log.d(TAG, "server done");
                    Socket s = serverSocket.accept();
                    Log.d(TAG, "socket accepted");
                    DataInputStream ro = new DataInputStream(s.getInputStream());
                    Log.d(TAG, "data input created");
                    String abc="Random String";
                    try {
                        abc = ro.readUTF();
                        Log.d(TAG,"message recieved "+abc);
                        String[] gotmsg =abc.split(",");
                        DataOutputStream wo = new DataOutputStream(s.getOutputStream());
                        String sendMsg;

                        //  node join requests only for 5554
                        if (gotmsg[0].equals("0")) {
                            if(gotmsg[1].equals(emno)){
                                sendMsg="ok";
                            }
                            else {

                                flag = 2;
                                String hk = genHash(gotmsg[1]);
                                joinorder.put(hk, gotmsg[2]);
                                Set<String> temp = joinorder.keySet();
                                int cnt = 0;
                                for (String chk : temp) {
                                    if (chk.equals(hk)) {
                                        break;
                                    }
                                    cnt++;
                                }
                                Object[] ne =  temp.toArray();
                                int prev=cnt-1;
                                if(prev==-1){
                                    prev = ne.length-1;
                                }
                                sendMsg = ne[prev] + "," +joinorder.get(ne[prev])+"," + ne[abs(cnt + 1) % ne.length]+","+joinorder.get(ne[abs(cnt + 1) % ne.length]);
                            }
                            wo.writeUTF(sendMsg);
                            wo.flush();


                        }
                        // Succ change or pred change
                        else if(gotmsg[0].equals("1")){
                            //Log.d("why","!!!!!!!!!!!!!!!!!!!!!!");
                            if(gotmsg[1].equals("1")){
                                sucid = gotmsg[2];
                                sucidpn = gotmsg[3];
                                Log.d(TAG,"Success changed "+sucid+sucidpn);
                            }
                            else{
                                predid = gotmsg[2];
                                predidpn = gotmsg[3];
                                Log.d(TAG,"pred changed  "+predid+predidpn);


                            }
                            wo.writeUTF("ok");
                            wo.flush();
                        }
                        // getting insertion request
                        else if(gotmsg[0].equals("2")){
                            ContentValues tempval = new ContentValues();
                            tempval.put("key",gotmsg[1]);
                            tempval.put("value",gotmsg[2]);
                            insert(mUri,tempval);
                            wo.writeUTF("ok");
                            wo.flush();
                        }
                        // getting single query
                        else if(gotmsg[0].equals("3")){
                            String resval="";
                            if (lookup(gotmsg[1])){
                                Cursor result =  query(mUri,null,gotmsg[2],null,null);
                                result.moveToFirst();

                                int index = result.getColumnIndex("value");

                                resval = result.getString(index);
                                Log.d(TAG,"fetched correctly"+resval);
                            }
                            else{
                                //fatne
                                sendmsgq(abc);
                                resval = singleresult;
                            }
                            wo.writeUTF(resval);
                            wo.flush();

                        }
                        else if(gotmsg[0].equals("4")){
                            if(!sucid.equals(gotmsg[1])){
                                sendmsgq(abc);
                            }
                            Cursor result = query(mUri,null,"@",null,null);
                            int keyindex = result.getColumnIndex("key");
                            int valueindex = result.getColumnIndex("value");
                            result.moveToFirst();
                            while(!result.isAfterLast()){
                                multipleresult.put(result.getString(keyindex),result.getString(valueindex));
                                result.moveToNext();
                            }
                            ObjectOutputStream WO = new ObjectOutputStream(s.getOutputStream());
                            WO.writeObject(multipleresult);
                            WO.flush();
                            WO.close();

                        }
                        else if(gotmsg[0].equals("5")){
                            delete(mUri,gotmsg[1],null);
                            wo.writeUTF("ok");
                            wo.flush();

                        }




                        publishProgress("go");


                        //Log.d(TAG, "readUTF");
                        //  messageSequencer.set(Integer.parseInt(getMsg[1]),recSeqNo);

                        //}

                    } catch (IOException e) {
                        Log.e(TAG, "not able to read"+e.getMessage());

                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }

                    ro.close();
                    s.close();

                } catch (IOException e) {
                    Log.e(TAG, "Can't accept connection" + e.getMessage());

                } catch (NullPointerException r) {
                    Log.e(TAG, r.getMessage());
                }

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            }
            //return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            //TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            //remoteTextView.append(strReceived + "\t\n");


            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            String filename = "GroupMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;
            /*
            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }
            */
            return;

        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {
        //String resultvalue;

        @Override
        protected Void doInBackground(String... msgs) {
            ArrayList<String> rempotePorts = new ArrayList<String>();
            rempotePorts.add(REMOTE_PORT0);
            rempotePorts.add(REMOTE_PORT1);
            rempotePorts.add(REMOTE_PORT2);
            rempotePorts.add(REMOTE_PORT3);
            rempotePorts.add(REMOTE_PORT4);


            String[] mtypes = msgs[0].split(",");
            String mtype = mtypes[0];
            // initializing message
            if (mtype.equals("0")){
                Log.d(TAG,"first message");
                String ack = null;
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt("11108"));
                    socket.setSoTimeout(500);
                    //Log.d(TAG,"message sent 4");

                    String msgToSend = "0"+','+emno+","+myPort;
                    //Log.d(TAG,"message sent 0");

                    DataOutputStream wo = new DataOutputStream(socket.getOutputStream());
                    //Log.d(TAG,"message sent 1");
                    wo.writeUTF(msgToSend);

                    Log.d(TAG, "message"+msgToSend);
                    wo.flush();
                    DataInputStream ro = new DataInputStream(socket.getInputStream());
                    ack = ro.readUTF();
                    Log.d(TAG,"ACK"+ack);
                    /*
                    if (emno.equals("5554")){
                        flag = 1;
                        sucid = hashid;
                        predid = hashid;
                        joinorder.put(hashid,emno);
                    }else{
                        flag = 2;
                        String[] getnbs = ack.split(",");
                        sucid = getnbs[2];
                        predid = getnbs[0];
                        String sucidpn = getnbs[3];
                        String predidpn = getnbs[1];


                    }
                    */

                    wo.close();
                    ro.close();
                    socket.close();
                }catch (SocketTimeoutException e) {
                    Log.e(TAG, "Socket timeout exception");


                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                    Log.e(TAG,"cant sense 5554");
                    flag = 1;


                }
                if (emno.equals("5554")){
                    flag = 1;
                    sucid = hashid;
                    predid = hashid;
                    sucidpn = myPort;
                    predidpn = myPort;
                    joinorder.put(hashid,myPort);
                }else if(flag==0){
                    flag = 2;
                    String[] getnbs = ack.split(",");
                    sucid = getnbs[2];
                    predid = getnbs[0];
                    sucidpn = getnbs[3];
                    predidpn = getnbs[1];
                    Log.d(TAG,"Succ first time "+sucid+sucidpn);
                    Log.d(TAG,"pred first time"+predid+predidpn);
                    try {
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(predidpn));
                        String msgToSend = "1"+','+"1"+','+hashid+","+myPort;
                        //Log.d(TAG,"message sent 0");

                        DataOutputStream wo = new DataOutputStream(socket.getOutputStream());
                        //Log.d(TAG,"message sent 1");
                        wo.writeUTF(msgToSend);

                        //Log.d(TAG, "message"+msgToSend);
                        wo.flush();
                        DataInputStream ro = new DataInputStream(socket.getInputStream());
                        String acks = ro.readUTF();
                        //Log.d(TAG,"ACK"+acks);
                        wo.close();
                        ro.close();
                        socket.close();

                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException updating pred");
                    } catch (IOException e) {
                        Log.e(TAG, "ClientTask socket IOException updating suc");

                    }
                    try {
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(sucidpn));
                        String msgToSend = "1"+','+"0"+','+hashid+","+myPort;
                        //Log.d(TAG,"message sent 0");

                        DataOutputStream wo = new DataOutputStream(socket.getOutputStream());
                        //Log.d(TAG,"message sent 1");
                        wo.writeUTF(msgToSend);

                        Log.d(TAG, "message"+msgToSend);
                        wo.flush();
                        DataInputStream ro = new DataInputStream(socket.getInputStream());
                        String acks = ro.readUTF();
                        Log.d(TAG,"ACK"+acks);
                        wo.close();
                        ro.close();
                        socket.close();

                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException updating pred");
                    } catch (IOException e) {
                        Log.e(TAG, "ClientTask socket IOException updating suc");

                    }



                }
                //Log.d("flag ------------",""+flag);


            }
            // for sending insertion to successor
            else if(mtype.equals("2")){
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(sucidpn));
                    String msgToSend = msgs[0];
                    //Log.d(TAG,"message sent 0");

                    DataOutputStream wo = new DataOutputStream(socket.getOutputStream());
                    //Log.d(TAG,"message sent 1");
                    wo.writeUTF(msgToSend);

                    //Log.d(TAG, "message"+msgToSend);
                    wo.flush();
                    DataInputStream ro = new DataInputStream(socket.getInputStream());
                    String acks = ro.readUTF();
                    Log.d(TAG,"ACK"+acks);
                    wo.close();
                    ro.close();
                    socket.close();

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException updating pred");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException updating suc");

                }

            }
            //for querying single row
            else if (mtype.equals("3")){
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(sucidpn));
                    String msgToSend = msgs[0];
                    //Log.d(TAG,"message sent 0");

                    DataOutputStream wo = new DataOutputStream(socket.getOutputStream());
                    //Log.d(TAG,"message sent 1");
                    wo.writeUTF(msgToSend);

                    Log.d(TAG, "message"+msgToSend);
                    wo.flush();
                    DataInputStream ro = new DataInputStream(socket.getInputStream());
                    String acks = ro.readUTF();
                    singleresult = acks;
                    //fatne
                    //resultvalue=acks;
                    Log.d(TAG,"ACK"+acks);
                    wo.close();
                    ro.close();
                    socket.close();

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException updating pred");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException updating suc");

                }


            }else if(mtype.equals("4")){
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(sucidpn));
                    String msgToSend = msgs[0];
                    //Log.d(TAG,"message sent 0");

                    DataOutputStream wo = new DataOutputStream(socket.getOutputStream());
                    //Log.d(TAG,"message sent 1");
                    wo.writeUTF(msgToSend);

                    Log.d(TAG, "message"+msgToSend);
                    wo.flush();
                    ObjectInputStream RO = new ObjectInputStream(socket.getInputStream());
                    try {
                        multipleresult =(Map)RO.readObject();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    //singleresult = acks;
                    //fatne
                    //resultvalue=acks;
                    //Log.d(TAG,"*"+multipleresult);
                    wo.close();
                    RO.close();
                    socket.close();

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException updating pred");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException updating suc");

                }



            }
            else if(mtype.equals("5")){
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(sucidpn));
                    String msgToSend = msgs[0];
                    //Log.d(TAG,"message sent 0");

                    DataOutputStream wo = new DataOutputStream(socket.getOutputStream());
                    //Log.d(TAG,"message sent 1");
                    wo.writeUTF(msgToSend);

                    Log.d(TAG, "message"+msgToSend);
                    wo.flush();
                    DataInputStream ro = new DataInputStream(socket.getInputStream());
                    String acks = ro.readUTF();
                    Log.d(TAG,"ACK"+acks);
                    wo.close();
                    ro.close();
                    socket.close();

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException updating pred");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException updating suc");

                }


            }




            return null;
        }
    }
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

}

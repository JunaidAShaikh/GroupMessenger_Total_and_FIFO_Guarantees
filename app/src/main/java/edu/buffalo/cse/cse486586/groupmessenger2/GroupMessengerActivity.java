package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String First = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static String myPort=null;
    static final int SERVER_PORT = 10000;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
    private ContentValues mContentValues = new ContentValues();
    private static int printSequence = 0;
    private static int sequence = 0;
    private static final int proposed=0;
    private static Comparator<String> sorting = new CompareClass();
    private static PriorityQueue<String> pq = new PriorityQueue<String>(25, sorting);
    private static HashMap<String, String> msgMap = new HashMap<String, String>();


    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        sequence=Integer.parseInt(myPort);

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            e.printStackTrace();
            return;
        }
        /*
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        final EditText editText = (EditText) findViewById(R.id.editText1);
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 /*
                     * If the key is pressed (i.e., KeyEvent.ACTION_DOWN) and it is an enter key
                     * (i.e., KeyEvent.KEYCODE_ENTER), then we display the string. Then we create
                     * an AsyncTask that sends the string to the remote AVD.
                     */
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                tv.append("\t" + msg); // This is one way to display a string.

                    /*
                     * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                     * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                     * the difference, please take a look at
                     * http://developer.android.com/reference/android/os/AsyncTask.html
                     */
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                return;

            }
        });
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket inputSocket = null;
            try {
                while (true) {
                    inputSocket = serverSocket.accept();
                    DataInputStream input = new DataInputStream(inputSocket.getInputStream());
                    DataOutputStream output = new DataOutputStream(inputSocket.getOutputStream());

                    String msgReceived = input.readUTF();
                    Log.v(TAG, "Msg Recieved: " + msgReceived);
                        String msgKey = msgReceived.split(":")[0];
                        String proposed = msgReceived.split(":")[1];
                        String port = msgReceived.split(":")[2];
                        String msg = msgReceived.split(":")[3];
                    String isFinal = msgReceived.split(":")[4];
                        String newMsg = proposed + ":" + port + ":" + msg + ":" + isFinal;

                        if (isFinal.equalsIgnoreCase("true")) {
                            output.writeUTF(msgReceived);
                            if (pq.contains(msgMap.get(msgKey))) {
                                Log.v(TAG, "Final Msg Recieved: " + msgReceived+" key present: "+msgMap.get(msgKey));
                                pq.remove(msgMap.get(msgKey));
                                pq.offer(newMsg);
                                Log.v(TAG, "Publishing new msg " + newMsg);
                                publishProgress(newMsg);
                            }
                        } else {
                            Log.v(TAG, "First Msg Recieved at : "+myPort+" : " + msgReceived);
                            msgMap.put(msgKey, newMsg);
                            pq.offer(newMsg);
                            sequence++;
                            Log.v(TAG, "Sequence to be suggested: " + sequence);

                                output.writeUTF(sequence + "," + msgKey + "," + myPort);

                        }
                }
            }catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }


        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */


           /* if(strReceived.contains(","))
                new ProposalClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, strReceived, myPort);
            else if(strReceived.contains(":")){*/
            if(!pq.isEmpty()) {
                String frontOfQueue = pq.peek();
                if (frontOfQueue.split(":")[3].equals("false")) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (pq.peek() == frontOfQueue) {
                        pq.remove(frontOfQueue);
                        Log.d(TAG, "Removed element: " + frontOfQueue);
                    }
                }
            }
            Log.d(TAG, "Next element in queue: "+pq.peek());
                while(!pq.isEmpty() && pq.peek().split(":")[3].equals("true")) {
                    {Log.d(TAG, "Printing msg: "+pq.peek());
                    strReceived = pq.poll();
                    remoteTextView.append(strReceived.split(":")[2] + "\t\n");
                    //mContentValues.put(KEY_FIELD, strReceived.split(":")[0]);
                        mContentValues.put(KEY_FIELD, printSequence);
                    mContentValues.put(VALUE_FIELD, strReceived.split(":")[2]);
                    getContentResolver().insert(mUri, mContentValues);
                        printSequence++;
                        Log.d(TAG, "Print Sequence: "+printSequence+" : "+" : "+strReceived);
                    }
                }
           /* }
            else
                new FinalClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, strReceived, myPort);*/
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ClientTask extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String[] remotePort = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};
                //String[] remotePort = {REMOTE_PORT0, REMOTE_PORT1};

                ArrayList<String> suggested = new ArrayList<String>();
                int agreed = 0;
                String msgText = msgs[0];
                String msgToSend = null;
                String msgKey = null;

                msgToSend = sequence + ":" + myPort + ":" + msgs[0] + ":" + "false";
                msgKey = sequence + myPort + msgs[0] + "false";
                msgMap.put(msgKey, msgToSend);
                Log.i(TAG, "First Message: " + msgKey + ":" + msgToSend);
                for (int i = 0; i < remotePort.length; i++) {
                    try {
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort[i]));


                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                        OutputStream msgOut = socket.getOutputStream();
                        DataOutputStream send = new DataOutputStream(msgOut);

                        InputStream msgIn = socket.getInputStream();
                        DataInputStream receieve = new DataInputStream(msgIn);

                        send.writeUTF(msgKey + ":" + msgToSend);

                        while (true) {
                            String msgReceived = receieve.readUTF();
                            Log.v(TAG, "Suggestion recieved: " + msgReceived);
                            suggested.add(msgReceived.split(",")[0]);
                            msgKey = msgReceived.split(",")[1];
                            break;
                        }
                    }
                    catch (SocketTimeoutException e) {
                        Log.e(TAG, "First Msg: Socket TimeOut Exception");
                    }
                    catch (StreamCorruptedException e)
                    {
                        Log.e(TAG, "First Msg: StreamCorrupted Exception");

                    }
                    catch(EOFException e) {
                        Log.e(TAG, "First Msg: EOF socket timeout");
                    }catch (IOException e) {
                        Log.e(TAG, "First Msg: ClientTask socket IOException");
                    }
                    catch (Exception e)
                    {
                        Log.e(TAG, "First Msg: Exception socket IOException");
                    }

                }

                Log.v(TAG, "receiving suggestions: " + suggested.toString());
                    for (int j = 0; j < suggested.size(); j++) {
                        if (Integer.parseInt(suggested.get(j)) > agreed)
                            agreed = Integer.parseInt(suggested.get(j));
                    }
                    suggested.clear();

                for (int i = 0; i < remotePort.length; i++) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort[i]));


                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                    try {
                        OutputStream msgOut = socket.getOutputStream();
                        DataOutputStream send = new DataOutputStream(msgOut);

                        InputStream msgIn = socket.getInputStream();
                        DataInputStream receieve = new DataInputStream(msgIn);
                        msgToSend = msgKey + ":" + agreed + ":" + myPort + ":" + msgText + ":" + "true";
                        send.writeUTF(msgToSend);

                        while (true) {
                            String msgReceived = receieve.readUTF();
                            Log.v(TAG, "Ack recieved: " + msgReceived);
                            if (msgReceived.equals(msgToSend))
                                Log.v(TAG, "Ack recieved: " + msgReceived);
                            send.close();
                            receieve.close();
                            socket.close();
                            break;
                        }
                    } catch (SocketTimeoutException e) {
                        Log.e(TAG, "Final Msg: Socket TimeOut Exception");
                    }
                    catch (StreamCorruptedException e)
                    {
                        Log.e(TAG, "First Msg: StreamCorrupted Exception");
                    }
                    catch(EOFException e) {
                        Log.e(TAG, "Final Msg: EOF socket timeout");
                    }catch (IOException e) {
                        Log.e(TAG, "Final Msg: ClientTask socket IOException");
                    }
                    catch (Exception e)
                    {
                        Log.e(TAG, "Final Msg: Exception socket IOException");
                    }
                }
            }catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }
    
}

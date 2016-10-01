package mmsl.BluetoothLibrary;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

public abstract class Connection extends Handler {

    private static final String TAG = "Connection";

    // Event
    public static final int EVENT_CONNECT_COMPLETE      = 1;
    
    public static final int EVENT_DATA_RECEIVED         = 2;
    public static final int EVENT_DATA_SEND_COMPLETE    = 3;
    public static final int EVENT_DATA_RECIEVING       = 4;
    public static final int EVENT_FRAME_ERROR       = 5;
    public static final int EVENT_TIMEOUT       = 6;
    public static final int EVENT_CONNECTION_FAIL       = 101;
    
    protected ConnectionCallback mCallback;

    // communication thread
    protected ConnectionThread mConnectionThread;
    protected CommandReceiveThread mReceiveThread;
    protected CommandSendThread mSendThread;

    // stream
    protected InputStream mInput;
    protected OutputStream mOutput;

    // send/close flag
    protected boolean isSending = false;
    protected boolean forceStop = false;

    // send data queue
    protected final boolean canQueueing;
    protected LinkedList<PendingData> mQueue = null;
   

    @Override
    public void handleMessage(Message msg) {

        if (forceStop) {
            mConnectionThread.close();
            return;
        }

        switch (msg.what) {
        case EVENT_CONNECT_COMPLETE:
            Log.i(TAG, "connect complete");            
            mInput = mConnectionThread.getInputStream();
            mOutput = mConnectionThread.getOutputStream();
            String devicename = (String) msg.obj;
            mCallback.onConnectComplete(devicename);

            // receive thread starting
            mReceiveThread = new CommandReceiveThread(mInput, obtainMessage(EVENT_DATA_RECEIVED));
            mReceiveThread.start();
            break;

        case EVENT_DATA_RECEIVED:
          //  Log.i(TAG, "data received");
            ConnectionCommand cmd = (ConnectionCommand) msg.obj;
            mCallback.onCommandReceived(cmd);

            // receive thread starting
            mReceiveThread = null;
            mReceiveThread = new CommandReceiveThread(mInput, obtainMessage(EVENT_DATA_RECEIVED));
            mReceiveThread.start();
            break;

        case EVENT_DATA_SEND_COMPLETE:
            int id = msg.arg1;
            Log.i(TAG, "data send complete, id : " + id);
            mSendThread = null;
            isSending = false;
            mCallback.onDataSendComplete(id);

            // if queueing data exists, send first data
            if (canQueueing) {
                sendPendingData();
            }
            break;

        case EVENT_DATA_RECIEVING:
            int percentage = msg.arg1;
            Log.i(TAG, "data recieved : " + percentage + "%");           
            mCallback.onDataRecieving(percentage);

           
            break;
        case EVENT_TIMEOUT:
        case EVENT_FRAME_ERROR:
            // receive thread starting
            mReceiveThread = null;
            mReceiveThread = new CommandReceiveThread(mInput, obtainMessage(EVENT_DATA_RECEIVED));
            mReceiveThread.start();
        	break;
        	
        case EVENT_CONNECTION_FAIL:
            Log.e(TAG, "connection failed");
            mSendThread = null;
            isSending = false;
            mCallback.onConnectionFailed();
            break;

        default:
            Log.e(TAG, "Unknown Event");
        }
    }

    

    /**
     * Constructor
     *
     * @param cb
     *            callback for communication result
     * @param canQueueing
     *            true if can queue sending data    
     */
    protected Connection(ConnectionCallback cb, boolean canQueueing) {
        mCallback = cb;

        this.canQueueing = canQueueing;
        if (canQueueing) {
            mQueue = new LinkedList<PendingData>();
        }

       ;
    }

    /**
     * stop connection. this method must be called when application will stop
     * connection
     */
    public void stopConnection() {

        forceStop = true;

        // stop connection thread
        mConnectionThread.close();

        // stop receive thread
        if (mReceiveThread != null) {
            mReceiveThread.forceStop();
            mReceiveThread = null;
        }

        // stop send thread
        mSendThread = null;
        clearQueuedData();

        mInput = null;
        mOutput = null;
        
    }

    /**

     * @param data
     *            option data
     * @param id
     *            send id
     * @return return true if success sending or queueing data. if "canQueueing"
     *         is false and sending any data, return false.
     */
    public boolean sendData(byte[] data, int id) {

        // if sending data, queueing...
        if (isSending) {
            if (canQueueing) {
                synchronized (mQueue) {
                    PendingData p = new PendingData(id, new ConnectionCommand((byte)0,(byte)0, data));
                    mQueue.offer(p);
                }
                Log.i(TAG, "sendData(), pending...");
                return true;
            } else {
                return false;
            }
        }

        Message msg = obtainMessage(EVENT_DATA_SEND_COMPLETE);
        msg.arg1 = id;
        ConnectionCommand command = new ConnectionCommand((byte)0,(byte)0, data);
        mSendThread = new CommandSendThread(mOutput, command, msg);
        mSendThread.start();

        isSending = true;
        return true;
    }


    /**
     * send data internal.
     *
     * @param pendingData
     *            pending data
     * @return always true
     * @hide
     */
    private boolean sendData(PendingData pendingData) {

        Log.i(TAG, "send PendingData");
        Message msg = obtainMessage(EVENT_DATA_SEND_COMPLETE);
        msg.arg1 = pendingData.id;

        mSendThread = new CommandSendThread(mOutput, pendingData.command, msg);
        mSendThread.start();

        isSending = true;
        return true;
    }

    /**
     * send pending data if exists.
     *
     * @hide
     */
    private void sendPendingData() {
        PendingData pendingData = null;
        synchronized (mQueue) {
            if (mQueue.size() > 0) {
                pendingData = mQueue.poll();
            }
        }
        if (pendingData != null) {
            sendData(pendingData);
        }
    }

    /**
     * clear queue data
     *
     * @hide
     */
    private void clearQueuedData() {
        if (canQueueing) {
            synchronized (mQueue) {
                mQueue.clear();
            }
        }
    }

    /**
     * pending data
     *
     * @hide
     */
    private class PendingData {
        int id;
        ConnectionCommand command;

        PendingData(int id, ConnectionCommand command) {
            this.id = id;
            this.command = command;
        }
    }

    abstract public void startConnection();
}

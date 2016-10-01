package mmsl.BluetoothLibrary.bluetooth;

import android.os.Message;

import mmsl.BluetoothLibrary.ConnectionCallback;

public class ServerBluetoothConnection extends BluetoothConnection {

    public ServerBluetoothConnection(ConnectionCallback cb, boolean canQueueing) {
        super(cb, canQueueing);
    }


    @Override
    public void startConnection() {
        Message msg = obtainMessage(EVENT_CONNECT_COMPLETE);
        mConnectionThread = new ServerBluetoothConnectionThread(msg);
        mConnectionThread.start();
    }
    
    
}

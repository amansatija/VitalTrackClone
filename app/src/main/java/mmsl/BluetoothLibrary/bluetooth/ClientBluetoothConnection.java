package mmsl.BluetoothLibrary.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Message;

import mmsl.BluetoothLibrary.ConnectionCallback;

public class ClientBluetoothConnection extends BluetoothConnection {

    private final BluetoothDevice mDevice;

    /**
     * create Bluetooth socket
     */
    public ClientBluetoothConnection(ConnectionCallback callback,
            boolean canQueueing, BluetoothDevice device) {
        super(callback, canQueueing);
        mDevice = device;
    }

   
    public void startConnection() {
        Message msg = obtainMessage(EVENT_CONNECT_COMPLETE);
        mConnectionThread = new ClientBluetoothConnectionThread(mDevice, msg);
        mConnectionThread.start();
    }
}

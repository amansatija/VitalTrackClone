package mmsl.BluetoothLibrary.bluetooth;

import android.os.Message;

import java.util.UUID;

import mmsl.BluetoothLibrary.Connection;
import mmsl.BluetoothLibrary.ConnectionCallback;

public abstract class BluetoothConnection extends Connection {

    // SPP UUID
    protected static final UUID SERVICE_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

 
    public BluetoothConnection(ConnectionCallback cb, boolean canQueueing) {
        super(cb, canQueueing);
    }

    @Override
    public void handleMessage(Message msg) {

        super.handleMessage(msg);
    }
}

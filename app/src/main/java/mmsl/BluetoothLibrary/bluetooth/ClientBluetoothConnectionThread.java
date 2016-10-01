package mmsl.BluetoothLibrary.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Message;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ClientBluetoothConnectionThread extends BluetoothConnectionThread {

	protected final BluetoothDevice mDevice;

	public ClientBluetoothConnectionThread(BluetoothDevice device, Message msg) {
		super(msg);
		mDevice = device;
	}

	@Override
	protected void getSocket() {

		// boolean gotuuid = mDevice.fetchUuidsWithSdp();
		// UUID uuid = mDevice.getUuids()[0].getUuid();
		// mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
		// mSocket =
		// mDevice.createRfcommSocketToServiceRecord(BluetoothConnection.SERVICE_UUID);
		Method m;
		try {
			m = mDevice.getClass().getMethod("createRfcommSocket",
					new Class[] { int.class });
			try {
				mSocket = (BluetoothSocket) m.invoke(mDevice, 1);
			} catch (IllegalArgumentException e) {

				System.out.println("CBCT catch1");
				e.printStackTrace();
			} catch (IllegalAccessException e) {

				System.out.println("CBCT catch2");
				e.printStackTrace();
			} catch (InvocationTargetException e) {

				e.printStackTrace();
				System.out.println("CBCT catch3");

			}
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int count = 0;
		do {
			try {
				if (mSocket != null) {
					mSocket.connect();
				}
				break;
			} catch (IOException e) {
				// DO NOTHING
			}
			// retry
		} while (count++ < 5);
	}
}

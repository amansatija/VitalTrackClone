package mmsl.BluetoothLibrary.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Message;
import android.util.Log;

import java.io.IOException;

import mmsl.BluetoothLibrary.ConnectionThread;

public abstract class BluetoothConnectionThread extends ConnectionThread {

	private static final String TAG = "BluetoothConnection";

	protected BluetoothSocket mSocket;

	protected BluetoothConnectionThread(Message msg) {
		super(msg);
	}

	/**
	 * get connection
	 */
	public void run() {
		Log.i(TAG, "start connecting");

		getSocket();

		if (mSocket == null) {
			Log.e(TAG, "Failed to connect");
			mMessage.what = BluetoothConnection.EVENT_CONNECTION_FAIL;
			mMessage.sendToTarget();
			return;
		}

		boolean result = setupConnection();

		if (!result) {
			Log.e(TAG, "Failed to connect");
			mMessage.what = BluetoothConnection.EVENT_CONNECTION_FAIL;
			mMessage.sendToTarget();
			return;
		}
		mMessage.obj = mSocket.getRemoteDevice().getName();
		mMessage.sendToTarget();
	}

	@Override
	public boolean close() {
		boolean ret = super.close();

		if (mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				ret = false;
			}
		}
		Log.e(TAG, "Socket Closed..");
		return ret;
	}

	private boolean setupConnection() {
		if (mSocket == null) {
			Log.e(TAG, "Socket not available..");
			return false;
		}

		try {
			mInput = mSocket.getInputStream();
			mOutput = mSocket.getOutputStream();
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	abstract protected void getSocket();
}

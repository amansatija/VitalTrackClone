/**
 * 
 */
package metsl.Util;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import metsl.UsbHostLibrary.Cp2102USBDriver;
import metsl.UsbHostLibrary.UsbDriver;

/**
 * @author sangeeta
 * 
 */
public class UsbInputOutputManager implements Runnable {
	private static final String TAG = UsbInputOutputManager.class
			.getSimpleName();
	private static final boolean DEBUG = false;
	public static final int HEADER_LENGTH = Double.SIZE / Integer.SIZE;//Integer.SIZE / Byte.SIZE;

	private static final int READ_WAIT_MILLIS = 200;
	private static final int BUFSIZ = 4096;

	private final UsbDriver mDriver;

	private final ByteBuffer mReadBuffer = ByteBuffer.allocate(BUFSIZ);

	// Synchronized by 'mWriteBuffer'
	private final ByteBuffer mWriteBuffer = ByteBuffer.allocate(BUFSIZ);

	private enum State {
		STOPPED, RUNNING, STOPPING
	}

	// Synchronized by 'this'
	private State mState = State.STOPPED;

	// Synchronized by 'this'
	private Listener mListener;

	public interface Listener {
		/**
		 * Called when new incoming data is available.
		 * @return 
		 */
		public void onNewData(int commandstatus, byte[] data);
		/**
		 * Called when {@link UsbInputOutputManager#run()} aborts due to an
		 * error.
		 */
		public void onRunError(Exception e);
	}

	/*
	 * Creates a new instance with no listener.
	 */
	public UsbInputOutputManager(UsbDevice device,
			UsbDeviceConnection connection) {
		this(device, connection, null);
	}

	/**
	 * Creates a new instance with the provided listener.
	 */
	public UsbInputOutputManager(UsbDevice device,
			UsbDeviceConnection connection, Listener listener) {
		mDriver = new Cp2102USBDriver(device, connection);
		mListener = listener;
		try {
			mDriver.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized void setListener(Listener listener) {
		mListener = listener;
	}

	public synchronized Listener getListener() {
		return mListener;
	}

	public void writeAsync(byte[] data) {
		synchronized (mWriteBuffer) {
			mWriteBuffer.put(data);
		}
	}

	public synchronized void stop() {
		if (getState() == State.RUNNING) {
			Log.i(TAG, "Stop requested");
			mState = State.STOPPING;
			
			try {
				mDriver.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}

	private synchronized State getState() {
		return mState;
	}

	/**
	 * Continuously services the read and write buffers until {@link #stop()} is
	 * called, or until a driver exception is raised.
	 * 
	 * NOTE(mikey): Uses inefficient read/write-with-timeout. TODO(mikey): Read
	 * asynchronously with {@link UsbRequest#queue(ByteBuffer, int)}
	 */
	public void run() {
		synchronized (this) {
			if (getState() != State.STOPPED) {
				throw new IllegalStateException("Already running.");
			}
			mState = State.RUNNING;
		}

		Log.i(TAG, "Running ..");
		try {
			while (true) {
				if (getState() != State.RUNNING) {
					Log.i(TAG, "Stopping mState=" + getState());
					break;
				}
				step();
			}
		} catch (Exception e) {
			Log.w(TAG, "Run ending due to exception: " + e.getMessage(), e);
			final Listener listener = getListener();
			if (listener != null) {
				listener.onRunError(e);
			}
		} finally {
			synchronized (this) {
				mState = State.STOPPED;
				Log.i(TAG, "Stopped.");
			}
		}
	}

	private void step() throws IOException {
		// Handle incoming data.
		int len = mDriver.read(mReadBuffer.array(), READ_WAIT_MILLIS);
		if (len > 0) {
			if (DEBUG)
				Log.d(TAG, "Read data len=" + len);
			final Listener listener = getListener();
			if (listener != null) {
				final byte[] data = new byte[len];
				mReadBuffer.get(data, 0, len);
				listener.onNewData(0,data);
			}
			mReadBuffer.clear();
		}

		// Handle outgoing data.
		byte[] outBuff = null;
		synchronized (mWriteBuffer) {
			if (mWriteBuffer.position() > 0) {
				len = mWriteBuffer.position();
				outBuff = new byte[len];
				mWriteBuffer.rewind();
				mWriteBuffer.get(outBuff, 0, len);
				mWriteBuffer.clear();
			}
		}
		if (outBuff != null) {
			if (DEBUG) {
				Log.d(TAG, "Writing data len=" + len);
			}
			mDriver.write(outBuff, READ_WAIT_MILLIS);
		}
	}

}

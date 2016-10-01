/**
 * 
 */
package metsl.UsbHostLibrary;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;

import java.io.IOException;

/**
 * @author sangeeta
 * 
 */
abstract class CommonUsbDriver implements UsbDriver {
	
	public static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
	public static final int DEFAULT_WRITE_BUFFER_SIZE = 16 * 1024;

	protected final UsbDevice mDevice;
	protected final UsbDeviceConnection mConnection;

	protected final Object mReadBufferLock = new Object();
	protected final Object mWriteBufferLock = new Object();

	/** Internal read buffer. Guarded by {@link #mReadBufferLock}. */
	protected byte[] mReadBuffer;

	/** Internal write buffer. Guarded by {@link #mWriteBufferLock}. */
	protected byte[] mWriteBuffer;

	public CommonUsbDriver(UsbDevice device,
			UsbDeviceConnection connection) {
		mDevice = device;
		mConnection = connection;

		mReadBuffer = new byte[DEFAULT_READ_BUFFER_SIZE];
		mWriteBuffer = new byte[DEFAULT_WRITE_BUFFER_SIZE];
	}

	/*
	 * Returns the currently-bound USB device.
	 * 
	 * @return the device
	 */
	public final UsbDevice getDevice() {
		return mDevice;
	}

 /* * Sets the size of the internal buffer used to exchange data with the USB
     * stack for read operations.  Most users should not need to change this.
     *
     * @param bufferSize the size in bytes
     */
    public final void setReadBufferSize(int bufferSize) {
        synchronized (mReadBufferLock) {
            if (bufferSize == mReadBuffer.length) {
                return;
            }
            mReadBuffer = new byte[bufferSize];
        }
    }

    /**
     * Sets the size of the internal buffer used to exchange data with the USB
     * stack for write operations.  Most users should not need to change this.
     *
     * @param bufferSize the size in bytes
     */
    public final void setWriteBufferSize(int bufferSize) {
        synchronized (mWriteBufferLock) {
            if (bufferSize == mWriteBuffer.length) {
                return;
            }
            mWriteBuffer = new byte[bufferSize];
        }
    }

    public abstract void open() throws IOException;

    public abstract void close() throws IOException;

    public abstract int read(final byte[] dest, final int timeoutMillis) throws IOException;

    public abstract int write(final byte[] src, final int timeoutMillis) throws IOException;

    public abstract void setParameters(
            int baudRate, int dataBits, int stopBits, int parity) throws IOException;

    public abstract boolean getCD() throws IOException;

    public abstract boolean getCTS() throws IOException;

    public abstract boolean getDSR() throws IOException;

    public abstract boolean getDTR() throws IOException;

    public abstract void setDTR(boolean value) throws IOException;

    public abstract boolean getRI() throws IOException;

    public abstract boolean getRTS() throws IOException;

    public abstract void setRTS(boolean value) throws IOException;

}

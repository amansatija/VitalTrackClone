/**
 * 
 */
package metsl.UsbHostLibrary;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author sangeeta
 * 
 */
public class Cp2102USBDriver extends CommonUsbDriver {

	private static final String TAG = Cp2102USBDriver.class.getSimpleName();

	private static final int DEFAULT_BAUD_RATE = 115200;

	private static final int USB_WRITE_TIMEOUT_MILLIS = 5000;

	/*
	 * Configuration Request Types
	 */
	private static final int REQTYPE_HOST_TO_DEVICE = 0x41;

	/*
	 * Configuration Request Codes
	 */
	private static final int SILABSER_IFC_ENABLE_REQUEST_CODE = 0x00;
	private static final int SILABSER_SET_BAUDDIV_REQUEST_CODE = 0x01;
	private static final int SILABSER_SET_LINE_CTL_REQUEST_CODE = 0x03;
	private static final int SILABSER_SET_MHS_REQUEST_CODE = 0x07;
	private static final int SILABSER_SET_BAUDRATE = 0x1E;

	/*
	 * SILABSER_IFC_ENABLE_REQUEST_CODE
	 */
	private static final int UART_ENABLE = 0x0001;
	private static final int UART_DISABLE = 0x0000;

	/*
	 * SILABSER_SET_BAUDDIV_REQUEST_CODE
	 */
	private static final int BAUD_RATE_GEN_FREQ = 0x384000;

	/*
	 * SILABSER_SET_MHS_REQUEST_CODE
	 */
	private static final int MCR_DTR = 0x0001;
	private static final int MCR_RTS = 0x0002;
	private static final int MCR_ALL = 0x0003;
	int TimeoutCounter = 0;
	private static final int CONTROL_WRITE_DTR = 0x0100;
	private static final int CONTROL_WRITE_RTS = 0x0200;

	private UsbEndpoint mReadEndpoint;
	private UsbEndpoint mWriteEndpoint;
	public static final int HEADER_LENGTH = Double.SIZE / Integer.SIZE;// Integer.SIZE
																		// /
																		// Byte.SIZE;
	private boolean forceStop = false;
	public static final int EVENT_CONNECT_COMPLETE = 1;

	public static final int EVENT_DATA_RECEIVED = 2;
	public static final int EVENT_DATA_SEND_COMPLETE = 3;
	public static final int EVENT_DATA_RECIEVING = 4;
	public static final int EVENT_FRAME_ERROR = 5;
	public static final int EVENT_TIMEOUT = 6;
	public static final int EVENT_CONNECTION_FAIL = 101;

	public Cp2102USBDriver(UsbDevice device, UsbDeviceConnection connection) {
		super(device, connection);
	}

	private int setConfigSingle(int request, int value) {
		return mConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE, request,
				value, 0, null, 0, USB_WRITE_TIMEOUT_MILLIS);
	}

	@Override
	public void open() throws IOException {
		boolean opened = false;
		try {
			for (int i = 0; i < mDevice.getInterfaceCount(); i++) {
				UsbInterface usbIface = mDevice.getInterface(i);
				if (mConnection.claimInterface(usbIface, true)) {
					Log.d(TAG, "claimInterface " + i + " SUCCESS");
				} else {
					Log.d(TAG, "claimInterface " + i + " FAIL");
				}
			}

			UsbInterface dataIface = mDevice.getInterface(mDevice
					.getInterfaceCount() - 1);
			for (int i = 0; i < dataIface.getEndpointCount(); i++) {
				UsbEndpoint ep = dataIface.getEndpoint(i);
				if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
					if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
						mReadEndpoint = ep;
					} else {
						mWriteEndpoint = ep;
					}
				}
			}

			setConfigSingle(SILABSER_IFC_ENABLE_REQUEST_CODE, UART_ENABLE);
			setConfigSingle(SILABSER_SET_MHS_REQUEST_CODE, MCR_ALL
					| CONTROL_WRITE_DTR | CONTROL_WRITE_RTS);
			setConfigSingle(SILABSER_SET_BAUDDIV_REQUEST_CODE,
					BAUD_RATE_GEN_FREQ / DEFAULT_BAUD_RATE);
			// setParameters(DEFAULT_BAUD_RATE, DEFAULT_DATA_BITS,
			// DEFAULT_STOP_BITS, DEFAULT_PARITY);
			opened = true;
		} finally {
			if (!opened) {
				close();
			}
		}
	}

	@Override
	public void close() throws IOException {
		setConfigSingle(SILABSER_IFC_ENABLE_REQUEST_CODE, UART_DISABLE);
		mConnection.close();
	}

	@Override
	public int read(byte[] dest, int timeoutMillis) throws IOException {
		final int numBytesRead;
		byte[] rawHeader = new byte[HEADER_LENGTH];

		int receivedSize = 0;
		int optionLen = 0;
		synchronized (mReadBufferLock) {
			while (!forceStop && (receivedSize < HEADER_LENGTH)) {
				int length = 0;
				try {

					length = mConnection.bulkTransfer(mReadEndpoint, rawHeader,
							HEADER_LENGTH - receivedSize, timeoutMillis);
					TimeoutCounter = 0;

				} catch (Exception e) {
					e.printStackTrace();
					if (!e.getMessage().contains("Try again")) {

						return 0;
					}
				}
				if (length != -1) {
					receivedSize += length;
				}

				if (length == 0) {
					Log.e("Library", "No Data.. In Sleep....");
				}
			}
			 optionLen = 0;
			if ((rawHeader[0] & 0xFF) > 0xF0)
				optionLen = rawHeader[1];
			else if ((rawHeader[1] & 0xFF) > 0xF0) {
				// System.out.println("Frame Error");

				try {
					rawHeader[0] = rawHeader[1];
					byte[] data = new byte[1];
					mConnection.bulkTransfer(mReadEndpoint, data, 1,
							timeoutMillis);
					rawHeader[1] = data[0];
					optionLen = rawHeader[1];
					TimeoutCounter = 0;
				} catch (Exception e) {
					e.printStackTrace();
					if (!e.getMessage().contains("Try again")) {

						return 0;
					}
				}

			} else {
				System.out.println("Frame Error");

				return 0;
			}

			// System.arraycopy(mReadBuffer, 0, dest, 0, numBytesRead);
			if (optionLen > 0) {
				byte[] rawOption = new byte[optionLen];
				receivedSize = 0;

				// receive option
				while (!forceStop && (receivedSize < optionLen)) {

					int length = 0;
					try {

						length = mConnection.bulkTransfer(mReadEndpoint,
								rawHeader, HEADER_LENGTH - receivedSize,
								timeoutMillis);
						TimeoutCounter = 0;
					} catch (Exception e) {
						e.printStackTrace();
						if (!e.getMessage().contains("Try again")) {

							return 0;
						}
					}
					if (length != -1) {
						receivedSize += length;
					}
				}

			}
		}
		return (optionLen + 2);
	}

	@Override
	public int write(byte[] src, int timeoutMillis) throws IOException {
		int offset = 0;

		while (offset < src.length) {
			final int writeLength;
			final int amtWritten;

			synchronized (mWriteBufferLock) {
				final byte[] writeBuffer;

				writeLength = Math
						.min(src.length - offset, mWriteBuffer.length);
				if (offset == 0) {
					writeBuffer = src;
				} else {
					// bulkTransfer does not support offsets, make a copy.
					System.arraycopy(src, offset, mWriteBuffer, 0, writeLength);
					writeBuffer = mWriteBuffer;
				}

				amtWritten = mConnection.bulkTransfer(mWriteEndpoint,
						writeBuffer, writeLength, timeoutMillis);
			}
			if (amtWritten <= 0) {
				throw new IOException("Error writing " + writeLength
						+ " bytes at offset " + offset + " length="
						+ src.length);
			}

			Log.d(TAG, "Wrote amt=" + amtWritten + " attempted=" + writeLength);
			offset += amtWritten;
		}
		return offset;
	}

	private void setBaudRate(int baudRate) throws IOException {
		byte[] data = new byte[] { (byte) (baudRate & 0xff),
				(byte) ((baudRate >> 8) & 0xff),
				(byte) ((baudRate >> 16) & 0xff),
				(byte) ((baudRate >> 24) & 0xff) };
		int ret = mConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE,
				SILABSER_SET_BAUDRATE, 0, 0, data, 4, USB_WRITE_TIMEOUT_MILLIS);
		if (ret < 0) {
			throw new IOException("Error setting baud rate.");
		}
	}

	@Override
	public void setParameters(int baudRate, int dataBits, int stopBits,
			int parity) throws IOException {
		setBaudRate(baudRate);

		int configDataBits = 0;
		switch (dataBits) {
		case DATABITS_5:
			configDataBits |= 0x0500;
			break;
		case DATABITS_6:
			configDataBits |= 0x0600;
			break;
		case DATABITS_7:
			configDataBits |= 0x0700;
			break;
		case DATABITS_8:
			configDataBits |= 0x0800;
			break;
		default:
			configDataBits |= 0x0800;
			break;
		}
		setConfigSingle(SILABSER_SET_LINE_CTL_REQUEST_CODE, configDataBits);

		int configParityBits = 0; // PARITY_NONE
		switch (parity) {
		case PARITY_ODD:
			configParityBits |= 0x0010;
			break;
		case PARITY_EVEN:
			configParityBits |= 0x0020;
			break;
		}
		setConfigSingle(SILABSER_SET_LINE_CTL_REQUEST_CODE, configParityBits);

		int configStopBits = 0;
		switch (stopBits) {
		case STOPBITS_1:
			configStopBits |= 0;
			break;
		case STOPBITS_2:
			configStopBits |= 2;
			break;
		}
		setConfigSingle(SILABSER_SET_LINE_CTL_REQUEST_CODE, configStopBits);
	}

	@Override
	public boolean getCD() throws IOException {
		return false;
	}

	@Override
	public boolean getCTS() throws IOException {
		return false;
	}

	@Override
	public boolean getDSR() throws IOException {
		return false;
	}

	@Override
	public boolean getDTR() throws IOException {
		return true;
	}

	@Override
	public void setDTR(boolean value) throws IOException {
	}

	@Override
	public boolean getRI() throws IOException {
		return false;
	}

	@Override
	public boolean getRTS() throws IOException {
		return true;
	}

	@Override
	public void setRTS(boolean value) throws IOException {
	}

	public static Map<Integer, int[]> getSupportedDevices() {
		final Map<Integer, int[]> supportedDevices = new LinkedHashMap<Integer, int[]>();

		supportedDevices.put(Integer.valueOf(UsbId.VENDOR_MMSL),
				new int[] { UsbId.MMSL_CP2102 });

		supportedDevices.put(Integer.valueOf(UsbId.VENDOR_MMSL_NEW),
				new int[] { UsbId.MMSL_CP2102_NEW });

		supportedDevices.put(Integer.valueOf(UsbId.VENDOR_MMSL_NEW),
				new int[] { UsbId.MMSL_CP2102_APC2 });
		supportedDevices.put(Integer.valueOf(UsbId.VENDOR_MMSL_VT),
				new int[] { UsbId.MMSL_CP2102_VT });
		return supportedDevices;
	}

	protected void forceStop() {
		forceStop = true;
	}
}

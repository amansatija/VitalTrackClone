package mmsl.BluetoothLibrary;

import android.os.Message;
import android.util.Log;

import java.io.InputStream;

public class CommandReceiveThread extends Thread {

	private boolean forceStop = false;

	private final InputStream mInput;
	private Message mMessage;

	int previousvalue = 0;
	int currentvalue = 0;

	int TimeoutCounter = 0;

	public CommandReceiveThread(InputStream in, Message msg) {
		mInput = in;
		mMessage = msg;

	}


	public void run() {
		// System.out.println("In Read <---------");
		byte[] rawHeader = new byte[ConnectionCommand.HEADER_LENGTH];

		int receivedSize = 0;

		// receive header
		while (!forceStop && (receivedSize < ConnectionCommand.HEADER_LENGTH)) {
			int length = 0;
			try {

				length = mInput.read(rawHeader, receivedSize,
						ConnectionCommand.HEADER_LENGTH - receivedSize);
				TimeoutCounter = 0;
				/*
				 * if (rawHeader[0] != 0) { ConnectionCommand command =
				 * ConnectionCommand .fromHeaderAndOption(rawHeader[0], (byte)
				 * 0, new byte[1]); try { mMessage.obj = command;
				 * mMessage.sendToTarget(); } catch (Exception e) { // TODO:
				 * handle exception }
				 * 
				 * }
				 */

			} catch (Exception e) {
				e.printStackTrace();
				if (!e.getMessage().contains("Try again")) {
					try {
						/*
						 * TimeoutCounter++; if (TimeoutCounter > 5) {
						 * System.err.println("Timeout occured in library");
						 */
						// e.printStackTrace();
						mMessage.what = Connection.EVENT_CONNECTION_FAIL;
						mMessage.sendToTarget();
						/* } */

					} catch (Exception e2) {
						// TODO: handle exception
					}

					return;
				}
			}
			if (length != -1) {
				receivedSize += length;
			}

			if (length == 0) {
				try {
					sleep(2);
					Log.e("Library", "No Data.. In Sleep....");
				} catch (InterruptedException e) {
					// DO NOTHING
				}
			}
		}

		int optionLen = 0;
		if ((rawHeader[0] & 0xFF) > 0xF0)
			optionLen = rawHeader[1];
		else if ((rawHeader[1] & 0xFF) > 0xF0) {
			// System.out.println("Frame Error");

			try {
				rawHeader[0] = rawHeader[1];
				rawHeader[1] = (byte) mInput.read();
				optionLen = rawHeader[1];
				TimeoutCounter = 0;
			} catch (Exception e) {
				e.printStackTrace();
				if (!e.getMessage().contains("Try again")) {
					try {
						/*
						 * TimeoutCounter++; if (TimeoutCounter > 5) {
						 * System.err.println("Timeout occured in library");
						 */
						// e.printStackTrace();
						mMessage.what = Connection.EVENT_CONNECTION_FAIL;
						mMessage.sendToTarget();
						/*
						 * }
						 */

					} catch (Exception e2) {
					}

					return;
				}
			}

		} else {
			System.out.println("Frame Error");
			try {
				mMessage.what = Connection.EVENT_FRAME_ERROR;
				mMessage.sendToTarget();
			} catch (Exception e2) {
				e2.printStackTrace();
			}

			return;
		}
		if (optionLen > 0) {
			byte[] rawOption = new byte[optionLen];
			receivedSize = 0;

			// receive option
			while (!forceStop && (receivedSize < optionLen)) {

				int length = 0;
				try {

					length = mInput.read(rawOption, receivedSize, optionLen
							- receivedSize);
					TimeoutCounter = 0;
				} catch (Exception e) {
					e.printStackTrace();
					if (!e.getMessage().contains("Try again")) {
						try {
							/*
							 * TimeoutCounter++; if (TimeoutCounter > 5) { //
							 * e.printStackTrace();
							 * System.err.println("Timeout occured in library");
							 */
							mMessage.what = Connection.EVENT_CONNECTION_FAIL;
							mMessage.sendToTarget();
							/*
							 * }else{ mMessage.what = Connection.EVENT_TIMEOUT;
							 * mMessage.sendToTarget(); }
							 */
						} catch (Exception e2) {
							// TODO: handle exception
						}

						return;
					}
				}
				if (length != -1) {
					receivedSize += length;
				}
			}

			ConnectionCommand command = ConnectionCommand.fromHeaderAndOption(
					rawHeader[0], rawHeader[1], rawOption);

			try {
				mMessage.obj = command;
				mMessage.sendToTarget();
			} catch (Exception e) {
			}

		}

		// System.out.println("Out of Read --------->");

	}

	protected void forceStop() {
		forceStop = true;
	}
}

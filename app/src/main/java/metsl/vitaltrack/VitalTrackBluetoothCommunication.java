package metsl.vitaltrack;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;

import mmsl.BluetoothLibrary.ConnectionCallback;
import mmsl.BluetoothLibrary.ConnectionCommand;
import mmsl.BluetoothLibrary.bluetooth.ClientBluetoothConnection;

public class VitalTrackBluetoothCommunication {

	// Member fields
	private ClientBluetoothConnection mBluetoothConnection;
	private VitalTrackCallback mVitalTrackCallback;
	private int FrameCount = 0;
	private int SPO2FrameCount = 0;
	private ArrayList<ArrayList<Integer>> ECGData = new ArrayList<ArrayList<Integer>>();
	private ArrayList<Byte> SPO2Data = new ArrayList<Byte>();

	private class connectionCallback implements ConnectionCallback {

		@Override
		public void onConnectionFailed() {
			mVitalTrackCallback.onConnectionFailed();
		}

		@Override
		public void onCommandReceived(ConnectionCommand command) {

			parsedata(command.commandstatus, command.option,
					command.option.length);
		}

		@Override
		public void onConnectComplete(String devicename) {

			mVitalTrackCallback.onConnectComplete(devicename);

		}

		@Override
		public void onDataRecieving(int percentage) {

		}

		@Override
		public void onDataSendComplete(int id) {
			// TODO Auto-generated method stub

		}

	}

	public void StartConnection(VitalTrackCallback vitaltrackCallback,
			BluetoothDevice device) {
		this.mVitalTrackCallback = vitaltrackCallback;

		mBluetoothConnection = new ClientBluetoothConnection(
				new connectionCallback(), true, device);
		mBluetoothConnection.startConnection();
	}

	public void StopConnection() {
		if (mBluetoothConnection != null)
			mBluetoothConnection.stopConnection();
	}

	// To Pasrse Data received from server.

	private void parsedata(int commandstatus, byte[] databyte, int datalength) {

		switch (commandstatus & 0xFF) {

		case 0xF1:
			// Live ECG
			ArrayList<Integer> ecgbyte = new ArrayList<Integer>();
			int[] ecgdata = new int[13];

			if (databyte.length == 6 || databyte.length == 8) {
				ecgdata[1] = (databyte[0] * 128) + databyte[1] - 3000;
				ecgdata[2] = (databyte[2] * 128) + databyte[3] - 3000;
				ecgdata[6] = (databyte[4] * 128) + databyte[5] - 3000;

				ecgdata[0] = (ecgdata[1] - ecgdata[2]);
				ecgdata[5] = ((ecgdata[1] + ecgdata[2]) / 2);
				ecgdata[4] = ((ecgdata[0] - ecgdata[2]) / 2);
				ecgdata[3] = ((-ecgdata[0] - ecgdata[1]) / 2);

			} else {
				ecgdata[1] = (databyte[0] * 128) + databyte[1] - 3000;
				ecgdata[2] = (databyte[2] * 128) + databyte[3] - 3000;
				ecgdata[6] = (databyte[4] * 128) + databyte[5] - 3000;
				ecgdata[7] = (databyte[6] * 128) + databyte[7] - 3000;
				ecgdata[8] = (databyte[8] * 128) + databyte[9] - 3000;
				ecgdata[9] = (databyte[10] * 128) + databyte[11] - 3000;
				ecgdata[10] = (databyte[12] * 128) + databyte[13] - 3000;
				ecgdata[11] = (databyte[14] * 128) + databyte[15] - 3000;

				ecgdata[0] = (ecgdata[1] - ecgdata[2]);
				ecgdata[5] = ((ecgdata[1] + ecgdata[2]) / 2);
				ecgdata[4] = ((ecgdata[0] - ecgdata[2]) / 2);
				ecgdata[3] = ((-ecgdata[0] - ecgdata[1]) / 2);
			}

			for (int i = 0; i < ecgdata.length; i++) {
				ecgbyte.add(ecgdata[i]);
			}

			ECGData.add(ecgbyte);
			FrameCount++;
			if (FrameCount >= 100) {
				FrameCount = 0;
				mVitalTrackCallback.onLiveECG(ECGData);
				ECGData.clear();

				break;
			}

			break;
		case 0xF2:

			mVitalTrackCallback.onHeartRate((databyte[0] & 0xFF));
			break;
		case 0xF3:
			SPO2Data.add(databyte[0]);
			SPO2FrameCount++;
			if (SPO2FrameCount >= 100) {
				SPO2FrameCount = 0;
				mVitalTrackCallback.onLiveSPO2(SPO2Data);
				SPO2Data.clear();

			}
			break;
		case 0xF4:

			mVitalTrackCallback.onSPO2((databyte[0] & 0xFF));

			break;

		case 0xF5: // handle NIBP Data
			if (databyte.length >= 4) {
				int _Systolic, _Diastolic;
				_Systolic = databyte[1] * 128 + databyte[2];
				_Diastolic = databyte[3];

				mVitalTrackCallback.onNIBPData(_Systolic, _Diastolic);

			}
			break;
		case 0xFE: // handle SPIRO Values

			byte[] tempbyte = new byte[databyte.length];
			for (int i = 0; i < tempbyte.length; i++) {
				tempbyte[i] = (byte) databyte[i];
			}
			String spirovals = null;
			try {
				spirovals = new String(tempbyte, "US-ASCII");
				String[] _values = spirovals.split(",");
				Float _FVC, _FEV1, _PEFR, _Ratio;
				_FVC = Float.valueOf(_values[0]);
				_FEV1 = Float.valueOf(_values[1]);
				_PEFR = Float.valueOf(_values[2]);
				_Ratio = Float.valueOf(_values[3]);

				mVitalTrackCallback.onSPIROValue(_FVC, _FEV1, _PEFR, _Ratio);

			} catch (Exception ex) {
				System.err.println(ex.getMessage());
			}

			break;
		case 0xFB:
			switch (databyte[0]) {
			case 2:
				switch (databyte[1]) {
				case 0: // All ok
					mVitalTrackCallback.onReady();
					break;
				case 1: // Probe Absent
					mVitalTrackCallback.onProbeAbsent();
					break;
				case 2: // No finger
					mVitalTrackCallback.onNoFinger();
					break;
				case 3: // Pulse search
					mVitalTrackCallback.onPulseSearch();

					break;
				case 4: // low perfusion
					mVitalTrackCallback.onLowPerfusion();
					break;
				case 5: // Weak Pulse
					mVitalTrackCallback.onWeakPulse();
					break;
				default:
					break;
				}
				break;

			case 7:
				switch (databyte[1]) {
				case 0: // All ok
					mVitalTrackCallback.onNIBPReady();

					break;
				case 1: // Busy
					mVitalTrackCallback.onCalibrating();

					break;
				case 2: // Leak
					mVitalTrackCallback.onLeak();

					break;
				case 3: // over prewsure
					mVitalTrackCallback.onOverPressure();
					break;
				case 4: // error
					mVitalTrackCallback.onBPError();
					break;
				case 5: // VALID
					// statusLabel.setText("Valid");
					// statusLabel.setForeground(Color.black);

					break;
				case 6: // machine reading status
					mVitalTrackCallback.onNIBPReading();
					break;

				case 7:
					mVitalTrackCallback.onCuffOversize();

					break;

				case 8:
					mVitalTrackCallback.onCuffUndersize();

					break;
				case 9:
					mVitalTrackCallback.onWeakPulse();

					break;
				case 10:
					mVitalTrackCallback.onLeak();

					break;
				case 11:

					mVitalTrackCallback.onCuffMissing();
					break;
				case 12:

					mVitalTrackCallback.onExcessiveMotion();

					break;
				case 13:

					mVitalTrackCallback.onSignaltoostrong();

					break;
				case 14:
					mVitalTrackCallback.onNIBPHardwareerror();
					break;
				case 15:
					mVitalTrackCallback.onNIBPTimeout();

					break;
				case 16:
					mVitalTrackCallback.onNIBPFlowError();

					break;
				case 17:
					mVitalTrackCallback.onProjectedSystolic();

					break;
				case 18:
					mVitalTrackCallback.onProjectedDiastolic();

					break;
				case 19:
					mVitalTrackCallback.onTubeBlocked();
					break;

				case 20:
					mVitalTrackCallback.onCheckingCuff();

					break;

				case 21:
					mVitalTrackCallback.onCheckingLeak();

					break;
				case 22:
					mVitalTrackCallback.onInflating();

					break;
				case 23:
					mVitalTrackCallback.onCheckingPulse();

					break;
				case 24:
					mVitalTrackCallback.onReleasing();

					break;

				default:

					break;

				}
				break;
			case 8:
				mVitalTrackCallback.onMode(databyte[1]);
				break;

			case 9:
				switch (databyte[1]) {
				case 0:
					mVitalTrackCallback.onReady();
					break;
				case 1:
					mVitalTrackCallback.onLeadsOFF();
					break;
				}
				break;
			case 12: // receiving the device ID from the VT2
				tempbyte = new byte[databyte.length];

				for (int i = 0; i < tempbyte.length; i++) {
					tempbyte[i] = (byte) databyte[i];
				}
				String _DeviceID = (new String(tempbyte)).trim();

				mVitalTrackCallback.onDeviceID(_DeviceID);

				break;
			}
			break;

		}
	}

	public void Startspiroreading() {
		byte[] moutput = { (byte) 0x96, (byte) 0xAA, (byte) 0xA1, (byte) 0x55 };
		mBluetoothConnection.sendData(moutput, 1);
	}

	public void Getspirovalues() {
		byte[] moutput = { (byte) 0x96, (byte) 0xAA, (byte) 0xA2, (byte) 0x55 };
		mBluetoothConnection.sendData(moutput, 1);
	}

	public void SetIDLEMode() {
		byte[] moutput = { (byte) 0x96, (byte) 0xAA, (byte) 0xFF, (byte) 0x55 };
		mBluetoothConnection.sendData(moutput, 1);
	}

	public void SetECGMode() {
		SetIDLEMode();
		byte[] moutput = { (byte) 0x96, (byte) 0xAA, (byte) 0xF1, (byte) 0x55 };
		mBluetoothConnection.sendData(moutput, 1);
	}

	public void SetNIBPMode() {
		SetIDLEMode();
		byte[] moutput = { (byte) 0xF5, (byte) 0x96, (byte) 0xAA, (byte) 0xF3,
				(byte) 0x55 };
		mBluetoothConnection.sendData(moutput, 1);
	}

	public void SetSPO2Mode() {
		SetIDLEMode();
		byte[] moutput = { (byte) 0x96, (byte) 0xAA, (byte) 0xF2, (byte) 0x55 };
		mBluetoothConnection.sendData(moutput, 1);
	}

	public void SetSPIROMode() {
		SetIDLEMode();
		byte[] moutput = { (byte) 0x96, (byte) 0xAA, (byte) 0xF6, (byte) 0x55 };
		mBluetoothConnection.sendData(moutput, 1);
	}

	public void StartNIBP() {
		byte[] moutput = { (byte) 0xF5, (byte) 0x01, (byte) 0x01 };
		mBluetoothConnection.sendData(moutput, 1);
	}

	// To Exit Bluetooth Mode
	public void BluetoothExitMode() {
		byte[] moutput = { (byte) 0xfd, 1, (byte) 0x1C };
		mBluetoothConnection.sendData(moutput, 1);
	}

	public void StopNIBP() {
		byte[] moutput = { (byte) 0xF5, (byte) 0x01, (byte) 0x01 };
		mBluetoothConnection.sendData(moutput, 1);
	}

	// All Host(ECG) To Device(Mobile) Operations by Aakash

}

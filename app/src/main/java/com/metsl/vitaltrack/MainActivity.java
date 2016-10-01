package com.metsl.vitaltrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import metsl.vitaltrack.VitalTrackBluetoothCommunication;
import metsl.vitaltrack.VitalTrackCallback;
import metsl.vitaltrack.VitalTrackUSBCommunication;



import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements VitalTrackCallback {

	TextView mtvDeviceID, mtvHeartrate, mtvSPO2, mtvBP, mtvFEV, mtvFVC,
			mtvPERF, mtvRatio;
	TextView mtvMachineStatus;
	Button mbtConnect, mbtSave, mbtSaveAll, mbtNibp;
	VitalTrackBluetoothCommunication vBluetoothCommunication;
	public static VitalTrackUSBCommunication vitalTrackUSBCommunication;
	public static UsbManager mUsbManager;

	Mode _Mode;
	ECGView _MonitorView;
	LinearLayout LiveGraph;
	String patientID = "";
	private static int _FinalHR, _FinalSPO2Val, _FinalSystolic,
			_FinalDiastolic;
	private static float _FinalFEV, _FinalFVC, _FinalPEFR, _FinalRatio;
	private int _ECGData[][];
	private boolean _ECGSaveDataFlag = false;
	private int _ECGCounter = 0;
	FileIO fileop;
	Spinner mSpirospinner;
	Spinner mModespinner;
	boolean isConnected = false;
	String machinestatus = "Machine Status : ---";
	String currentmode = "Mode : ---";
	ConnectionType connectionType = ConnectionType.BLUETOOTH;

	// Bluetooth Variable Declaration Code Ends Here

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mtvMachineStatus = (TextView) findViewById(R.id.tv_top_overlay);
		SetMessage();
		
		try {
			if (getIntent().getStringExtra("patientid") != null)
				patientID = getIntent().getStringExtra("patientid");
		} catch (Exception e) {

		}
		// mtvMachineStatus.setEllipsize(TruncateAt.MARQUEE);

		mSpirospinner = (Spinner) findViewById(R.id.spinner_Spiro);
		mModespinner = (Spinner) findViewById(R.id.spinner_Mode);
		mbtNibp = (Button) findViewById(R.id.button_NIBP);
		mbtConnect = (Button) findViewById(R.id.button_Connect);
		mbtSave = (Button) findViewById(R.id.button_Save);
		mbtSaveAll = (Button) findViewById(R.id.button_SaveAll);
		mtvHeartrate = (TextView) findViewById(R.id.tv_HeartRateResponse);
		mtvSPO2 = (TextView) findViewById(R.id.tv_SPO2Response);
		mtvBP = (TextView) findViewById(R.id.tv_BPResponse);
		mtvFEV = (TextView) findViewById(R.id.tv_FEVResponse);
		mtvFVC = (TextView) findViewById(R.id.tv_FVCResponse);
		mtvPERF = (TextView) findViewById(R.id.tv_PERFResponse);
		mtvRatio = (TextView) findViewById(R.id.tv_RatioResponse);
		_ECGData = new int[2400][8];
		_Mode = Mode.IDLE;
		initView();
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		vitalTrackUSBCommunication = new VitalTrackUSBCommunication();

		VitalVariables.read_ptr = 0;
		VitalVariables.write_ptr = 0;
		VitalVariables.diff_pointer = 0;
		VitalVariables._ContiECGData = new int[30000][13];
		vBluetoothCommunication = new VitalTrackBluetoothCommunication();
		mbtNibp.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
					switch (connectionType) {
				case BLUETOOTH:
					vBluetoothCommunication.StartNIBP();
					break;
				case USB:
					vitalTrackUSBCommunication.StartNIBP();
					break;
				default:
					break;
				}

				if (mbtNibp.getText().toString().contains("Start")) {
					mbtSave.setEnabled(true);
					mbtNibp.setText("Stop NIBP");
				} else {
					mbtNibp.setText("Start NIBP");
				}
			}
		});
		mbtConnect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				if (mbtConnect.getTag().equals("connect")) {
					showConnectionDialog();
				} else {
					switch (connectionType) {
					case BLUETOOTH:
						vBluetoothCommunication.StopConnection();
						break;
					case USB:
						vitalTrackUSBCommunication.stopIoManager();
						break;
					default:
						break;
					}

					mbtConnect.setBackgroundResource(R.drawable.button_connect);
					mbtConnect.setTag("connect");
					mModespinner.setEnabled(false);
					mSpirospinner.setEnabled(false);
					mbtNibp.setEnabled(false);
					mbtSaveAll.setEnabled(false);
					mbtSave.setEnabled(false);
				}
			}
		});

		mbtSave.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
					switch (_Mode) {
				case SPO2:
					_FinalSPO2Val = VitalVariables.SPO2val;
					_FinalHR = VitalVariables.HeartRate;
					mtvHeartrate.setText(String.valueOf(_FinalHR));
					mtvSPO2.setText(String.valueOf(_FinalSPO2Val));
					mbtSaveAll.setEnabled(true);
					break;
				case NIBP:
					_FinalSystolic = VitalVariables._Systolic;
					_FinalDiastolic = VitalVariables._Diastolic;
					mtvBP.setText(String.valueOf(_FinalSystolic) + "/"
							+ String.valueOf(_FinalDiastolic));
					mbtSaveAll.setEnabled(true);
					break;
				case SPIRO:
					_FinalFVC = VitalVariables._FVC;
					_FinalFEV = VitalVariables._FEV1;
					_FinalPEFR = VitalVariables._PEFR;
					_FinalRatio = VitalVariables._Ratio;
					mtvFVC.setText(String.valueOf(_FinalFVC));
					mtvFEV.setText(String.valueOf(_FinalFEV));
					mtvPERF.setText(String.valueOf(_FinalPEFR));
					mtvRatio.setText(String.valueOf(_FinalRatio));
					mbtSaveAll.setEnabled(true);
					break;
				case ECG:
					_FinalHR = VitalVariables.HeartRate;
					//sampling rate is 240 so [2400]
					_ECGData = new int[2400][8];
					_ECGCounter = 0;
					_ECGSaveDataFlag = true;
					_MonitorView.SetisSaving(true);
					mtvHeartrate.setText(String.valueOf(_FinalHR));
					mModespinner.setEnabled(false);
					mbtSave.setEnabled(false);
					mbtSaveAll.setEnabled(false);
					break;
				default:
					break;
				}

			}
		});
		mbtSaveAll.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
						createFile();
			}
		});

		mModespinner.setEnabled(false);
		mSpirospinner.setEnabled(false);
		mbtNibp.setEnabled(false);
		mbtSaveAll.setEnabled(false);
		mbtSave.setEnabled(false);

		mModespinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
					if (isConnected) {

					switch (position) {
					case 0:
						_Mode = Mode.IDLE;
						_MonitorView.setmode(_Mode);
						switch (connectionType) {
						case BLUETOOTH:
							vBluetoothCommunication.SetIDLEMode();
							break;
						case USB:
							vitalTrackUSBCommunication.SetIDLEMode();
							break;
						default:
							break;
						}

						VitalVariables.read_ptr = 0;
						VitalVariables.write_ptr = 0;
						VitalVariables.diff_pointer = 0;
						_MonitorView.m_selectedmodechanged = true;
						mSpirospinner.setEnabled(false);
						mbtNibp.setEnabled(false);
						mbtSave.setEnabled(false);
						// mbtSaveAll.setEnabled(false);
						break;
					case 1:
						_Mode = Mode.ECG;
						switch (connectionType) {
						case BLUETOOTH:
							vBluetoothCommunication.SetECGMode();
							break;
						case USB:
							vitalTrackUSBCommunication.SetECGMode();
							break;
						default:
							break;
						}

						_MonitorView.setmode(_Mode);
						VitalVariables.read_ptr = 0;
						VitalVariables.write_ptr = 0;
						VitalVariables.diff_pointer = 0;
						_MonitorView.m_selectedmodechanged = true;
						mSpirospinner.setEnabled(false);
						mbtSave.setEnabled(true);
						mbtNibp.setEnabled(false);

						break;
					case 2:
						_Mode = Mode.SPO2;
						switch (connectionType) {
						case BLUETOOTH:
							vBluetoothCommunication.SetSPO2Mode();
							break;
						case USB:
							vitalTrackUSBCommunication.SetSPO2Mode();
							break;
						default:
							break;
						}
						_MonitorView.setmode(_Mode);
						VitalVariables.sporead_ptr = 0;
						VitalVariables.spowrite_ptr = 0;
						VitalVariables.spodiff_pointer = 0;
						_MonitorView.m_selectedmodechanged = true;
						mSpirospinner.setEnabled(false);
						mbtNibp.setEnabled(false);
						mbtSave.setEnabled(true);
						break;
					case 3:
						_Mode = Mode.NIBP;
						switch (connectionType) {
						case BLUETOOTH:
							vBluetoothCommunication.SetNIBPMode();
							break;
						case USB:
							vitalTrackUSBCommunication.SetNIBPMode();
							break;
						default:
							break;
						}

						_MonitorView.setmode(_Mode);
						mSpirospinner.setEnabled(false);
						mbtNibp.setEnabled(true);

						break;
					case 4:
						_Mode = Mode.SPIRO;
						switch (connectionType) {
						case BLUETOOTH:
							vBluetoothCommunication.SetSPIROMode();
							break;
						case USB:
							vitalTrackUSBCommunication.SetSPIROMode();
							break;
						default:
							break;
						}
						_MonitorView.setmode(_Mode);
						mSpirospinner.setEnabled(true);
						mbtNibp.setEnabled(false);
						mbtSave.setEnabled(true);
						break;
					default:
						break;
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
		
			}
		});
		mSpirospinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
					if (isConnected) {
					switch (position) {
					case 0:
						switch (connectionType) {
						case BLUETOOTH:
							vBluetoothCommunication.Startspiroreading();
							break;
						case USB:
							vitalTrackUSBCommunication.Startspiroreading();
							break;
						default:
							break;
						}

						break;
					case 1:
						switch (connectionType) {
						case BLUETOOTH:
							vBluetoothCommunication.Getspirovalues();
							break;
						case USB:
							vitalTrackUSBCommunication.Getspirovalues();
							break;
						default:
							break;
						}

						break;
					default:
						break;
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
		
			}
		});

	}

	public void showConnectionDialog() {
		LayoutInflater inflater = this.getLayoutInflater();
		final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				this);
		alertDialogBuilder.setCancelable(true);
		final View view = inflater.inflate(R.layout.chooseconnection_dialog,
				null);
		TextView _title = (TextView) view.findViewById(R.id.tv_title);
		TextView MSG = (TextView) view.findViewById(R.id.tv_alertMsg);
		_title.setText("Choose Connection");
		MSG.setText("Connect With");
		Button bToothButton = (Button) view.findViewById(R.id.bt_Bluetooth);
		Button usbButton = (Button) view.findViewById(R.id.bt_USB);
		alertDialogBuilder.setView(view);
		final AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
		bToothButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				alertDialog.cancel();
				connectionType = ConnectionType.BLUETOOTH;
				Intent intent = new Intent(MainActivity.this, BTPair.class);
				startActivityForResult(intent, 202);

			}
		});

		usbButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				alertDialog.cancel();
				connectionType = ConnectionType.USB;
				vitalTrackUSBCommunication.startIoManager(mUsbManager,
						MainActivity.this);
			}
		});

	}

	public void initView() {
		LiveGraph = (LinearLayout) findViewById(R.id.LiveGraph);
		_MonitorView = new ECGView(getBaseContext(), this);
		LiveGraph.addView(_MonitorView);
		_MonitorView.setmode(_Mode);
		_MonitorView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
		
				if (_MonitorView.gain_val < 3)
					_MonitorView.gain_val++;
				else
					_MonitorView.gain_val = 0;
			if (_MonitorView.gain_val==0){
				Toast.makeText(getBaseContext(), "Gain is : 0.5", Toast.LENGTH_LONG).show();
			}else if(_MonitorView.gain_val==1){
				Toast.makeText(getBaseContext(), "Gain is : 1", Toast.LENGTH_LONG).show();
			}else if(_MonitorView.gain_val==2){
				Toast.makeText(getBaseContext(), "Gain is : 1.5", Toast.LENGTH_LONG).show();
			}else {
				Toast.makeText(getBaseContext(), "Gain is : 2", Toast.LENGTH_LONG).show();
			}
				return false;
			}
		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			vBluetoothCommunication = new VitalTrackBluetoothCommunication();
			BluetoothDevice device = VitalVariables.mBluetoothAdapter
					.getRemoteDevice(data.getStringExtra("selectedDeviceName"));
			vBluetoothCommunication.StartConnection(this, device);
		}
	}

	public void createFile() {
		FileOutputStream fo;
		String path = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + File.separator + "VitalTrack";
		try {
			File f = new File(path);
			if (!f.exists())
				f.mkdir();
			Calendar cal = Calendar.getInstance();

			Date _date = cal.getTime();
			/*
			 * SimpleDateFormat sdf = new SimpleDateFormat(
			 * "yyyy-MM-dd HH:mm:ss");
			 */

			String Event_Datetime = _date.toString();
			if (!patientID.equals(""))
				Event_Datetime = patientID + "_" + Event_Datetime;
			fo = new FileOutputStream(path + File.separator + Event_Datetime
					+ ".txt");

			OutputStreamWriter out = new OutputStreamWriter(fo);

			out.write("HR = " + String.valueOf(_FinalHR));
			out.write("\nSPO2 = " + String.valueOf(_FinalSPO2Val));
			out.write("\nBP = " + String.valueOf(_FinalSystolic) + "/"
					+ String.valueOf(_FinalDiastolic));
			out.write("\nFEV1 =" + String.valueOf(_FinalFEV));
			out.write("\nFVC = " + String.valueOf(_FinalFVC));
			out.write("\nPEFR = " + String.valueOf(_FinalPEFR));
			out.write("\nRatio : " + String.valueOf(_FinalRatio));
			out.close();
			fo.close();
			List<Byte> temparr = new ArrayList<Byte>();

			for (int i = 0; i < 2400; i++) {
				temparr.add((byte) 0xF1);
				temparr.add((byte) 18);
				for (int j = 0; j < 8; j++) {
					int tmp_val = _ECGData[i][j] + 3000;
					int ecglsb = (tmp_val & 0x007f);
					int ecgmsb = (tmp_val >> 7) & 0x007f;

					temparr.add((byte) ecgmsb);
					temparr.add((byte) ecglsb);
				}
				temparr.add((byte) 0);
				temparr.add((byte) 0);
			}

			byte[] temBs = new byte[temparr.size()];
			int cnt = 0;
			for (Iterator<Byte> it = temparr.iterator(); it.hasNext();) {
				Byte byte1 = it.next();
				temBs[cnt] = byte1;
				cnt++;
			}
			FileIO fileIO = new FileIO();
			fileIO.SetFilename(path + File.separator + Event_Datetime + ".raw");
			fileIO.OpenFile();
			fileIO.writeToFile(temBs);
			fileIO.CloseFile();
			Toast.makeText(getApplicationContext(), "Data Saved successfully",
					Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@SuppressLint("NewApi")
	private void hideSystemUI() {
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
						| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
						// remove the following flag for version < API 19
						| View.SYSTEM_UI_FLAG_IMMERSIVE);
	}

	@Override
	public void onConnectComplete(String devicename) {
		isConnected = true;
		mtvMachineStatus.setSelected(true);
		mbtConnect.setBackgroundResource(R.drawable.button_stop);
		mbtConnect.setTag("stop");
		mModespinner.setEnabled(true);
		if (_Mode != Mode.IDLE)
			mbtSave.setEnabled(true);
		mbtSaveAll.setEnabled(false);

	}

	@Override
	public void onConnectionFailed() {
		isConnected = false;
		mbtConnect.setBackgroundResource(R.drawable.button_connect);
		mModespinner.setEnabled(false);
		mSpirospinner.setEnabled(false);
		mbtNibp.setEnabled(false);
		mbtSaveAll.setEnabled(false);
		mbtSave.setEnabled(false);

		mbtConnect.setTag("connect");
	}

	@Override
	public void onDeviceID(String device_ID) {

	}

	@Override
	public void onLiveECG(ArrayList<ArrayList<Integer>> LiveECG) {
		for (int i = 0; i < LiveECG.size(); i++) {
			int[] ecgdata = ArrayUtils.toPrimitive(LiveECG.get(i).toArray(
					new Integer[LiveECG.get(i).size()]));
			VitalVariables._ContiECGData[VitalVariables.write_ptr] = ecgdata;
			if (_ECGSaveDataFlag == true) {
				if (_ECGCounter < 2400) {
					_ECGData[_ECGCounter][0] = ecgdata[1];
					_ECGData[_ECGCounter][1] = ecgdata[2];
					_ECGData[_ECGCounter][2] = ecgdata[6];
					_ECGData[_ECGCounter][3] = ecgdata[7];
					_ECGData[_ECGCounter][4] = ecgdata[8];
					_ECGData[_ECGCounter][5] = ecgdata[9];
					_ECGData[_ECGCounter][6] = ecgdata[10];
					_ECGData[_ECGCounter][7] = ecgdata[11];
					_ECGCounter++;

				} else {
					_ECGCounter = 0;
					_ECGSaveDataFlag = false;
					_MonitorView.SetisSaving(false);
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							
							mbtSaveAll.setEnabled(true);
							mModespinner.setEnabled(true);
							mbtSave.setEnabled(true);
						}
					});

				}
			}
			VitalVariables.write_ptr++;
			VitalVariables.diff_pointer++;
			if (VitalVariables.write_ptr >= VitalVariables._ContiECGData.length) {
				VitalVariables.write_ptr = 0;
			}
		}
	}

	@Override
	public void onHeartRate(int HeartRate) {
		// mtvHeartrate.setText(String.valueOf(HeartRate));
		VitalVariables.HeartRate = HeartRate;
	}

	@Override
	public void onSPO2(int SPO2val) {
		// mtvSPO2.setText(String.valueOf(SPO2val));
		VitalVariables.SPO2val = SPO2val;
	}

	@Override
	public void onLiveSPO2(ArrayList<Byte> SPO2Wave) {
		for (int i = 0; i < SPO2Wave.size(); i++) {

			VitalVariables._ContiSPoData[VitalVariables.spowrite_ptr] = SPO2Wave
					.get(i);
			VitalVariables.spowrite_ptr++;
			VitalVariables.spodiff_pointer++;
			if (VitalVariables.spowrite_ptr >= VitalVariables._ContiSPoData.length) {
				VitalVariables.spowrite_ptr = 0;
			}
		}
	}

	@Override
	public void onNIBPData(int _Systolic, int _Diastolic) {
		/*
		 * mtvBP.setText(String.valueOf(_Systolic) + "/" +
		 * String.valueOf(_Diastolic));
		 */
		VitalVariables._Systolic = _Systolic;
		VitalVariables._Diastolic = _Diastolic;
	}

	public void onSave(int _FinalSystolic, int _FinalDiastolic) {
		/*
		 * mtvBP.setText(String.valueOf(_Systolic) + "/" +
		 * String.valueOf(_Diastolic));
		 */
		VitalVariables._Systolic = _FinalSystolic;
		VitalVariables._Diastolic = _FinalDiastolic;
	}

	@Override
	public void onSPIROValue(Float _FVC, Float _FEV1, Float _PEFR, Float _Ratio) {
		/*
		 * mtvFVC.setText(String.valueOf(_FVC));
		 * mtvFEV.setText(String.valueOf(_FEV1));
		 * mtvPERF.setText(String.valueOf(_PEFR));
		 * mtvRatio.setText(String.valueOf(_Ratio));
		 */
		VitalVariables._FVC = _FVC;
		VitalVariables._FEV1 = _FEV1;
		VitalVariables._PEFR = _PEFR;
		VitalVariables._Ratio = _Ratio;
	}

	@Override
	public void onReady() {
		machinestatus = "Machine Status : " + "Ready";
		SetMessage();
		mtvMachineStatus.setSelected(true);

	}

	@Override
	public void onProbeAbsent() {
		machinestatus = "Machine Status : " + "Probe Absent";
		SetMessage();

	}

	@Override
	public void onNoFinger() {
		machinestatus = "Machine Status : " + "No Finger";
		SetMessage();

	}

	@Override
	public void onPulseSearch() {
		machinestatus = "Machine Status : " + "Searching Pulse";
		SetMessage();

	}

	@Override
	public void onLowPerfusion() {
		machinestatus = "Machine Status : " + "Low Perfusion";
		SetMessage();

	}

	@Override
	public void onWeakPulse() {
		machinestatus = "Machine Status : " + "Weak Pulse";
		SetMessage();

	}

	@Override
	public void onNIBPReady() {
		machinestatus = "Machine Status : " + "NIBP Ready";
		SetMessage();

	}

	@Override
	public void onCalibrating() {
		machinestatus = "Machine Status : " + "Calibrating ...Please Wait";
		SetMessage();

	}

	@Override
	public void onLeak() {
		machinestatus = "Machine Status : " + "Leak";
		SetMessage();
	}

	@Override
	public void onOverPressure() {
		machinestatus = "Machine Status : " + "Over Pressure";
		SetMessage();
	}

	@Override
	public void onBPError() {
		machinestatus = "Machine Status : " + "BP Error";
		SetMessage();

	}

	@Override
	public void onNIBPReading() {
		machinestatus = "Machine Status : " + "NIBP Reading";
		SetMessage();

	}

	@Override
	public void onCuffOversize() {
		machinestatus = "Machine Status : " + "Cuff Oversize";
		SetMessage();
	}

	@Override
	public void onCuffUndersize() {
		machinestatus = "Machine Status : " + "Cuff Undersize";
		SetMessage();
	}

	@Override
	public void onCuffMissing() {
		machinestatus = "Machine Status : " + "Cuff Missing";
		SetMessage();
	}

	@Override
	public void onExcessiveMotion() {
		machinestatus = "Machine Status : " + "Excessive Motion";
		SetMessage();
	}

	@Override
	public void onSignaltoostrong() {
		machinestatus = "Machine Status : " + "Signal Too Strong";
		SetMessage();
	}

	@Override
	public void onNIBPHardwareerror() {
		machinestatus = "Machine Status : " + "NIBP Hardware Error";
		SetMessage();
	}

	@Override
	public void onNIBPTimeout() {
		machinestatus = "Machine Status : " + "NIBP Timeout";
		SetMessage();
	}

	@Override
	public void onNIBPFlowError() {
		machinestatus = "Machine Status : " + "NIBP Flow Error";
		SetMessage();
	}

	@Override
	public void onProjectedSystolic() {
		machinestatus = "Machine Status : " + "Projected Sysstolic";
		SetMessage();
	}

	@Override
	public void onProjectedDiastolic() {
		machinestatus = "Machine Status : " + "Projected Diastolic";
		SetMessage();
	}

	@Override
	public void onTubeBlocked() {
		machinestatus = "Machine Status : " + "Tube Blocked";
		SetMessage();
	}

	@Override
	public void onCheckingCuff() {
		machinestatus = "Machine Status : " + "Checking Cuff";
		SetMessage();
	}

	@Override
	public void onCheckingLeak() {
		machinestatus = "Machine Status : " + "Checking Leak";
		SetMessage();
	}

	@Override
	public void onInflating() {

	}

	@Override
	public void onCheckingPulse() {
		machinestatus = "Machine Status : " + "Checking Pulse";
		mtvMachineStatus.setText(machinestatus + currentmode);

	}

	@Override
	public void onReleasing() {
		machinestatus = "Machine Status : " + "Releasing";

		mtvMachineStatus.setText(machinestatus + currentmode);
	}

	@Override
	public void onLeadsOFF() {
		machinestatus = "Machine Status : " + "Leads off";
		mtvMachineStatus.setText(machinestatus + currentmode);

	}

	@Override
	public void onMode(int mode) {
		currentmode = "\tMode : " + Mode.values()[mode].name();
		SetMessage();
		if (_Mode.ordinal() != mode) {
			switch (_Mode) {
			case IDLE:
				switch (connectionType) {
				case BLUETOOTH:
					vBluetoothCommunication.SetIDLEMode();
					break;
				case USB:
					vitalTrackUSBCommunication.SetIDLEMode();
					break;
				default:
					break;
				}

				break;
			case ECG:
				switch (connectionType) {
				case BLUETOOTH:
					vBluetoothCommunication.SetECGMode();
					break;
				case USB:
					vitalTrackUSBCommunication.SetECGMode();
					break;
				default:
					break;
				}

				break;
			case NIBP:
				switch (connectionType) {
				case BLUETOOTH:
					vBluetoothCommunication.SetNIBPMode();
					break;
				case USB:
					vitalTrackUSBCommunication.SetNIBPMode();
					break;
				default:
					break;
				}

				break;
			case SPIRO:
				switch (connectionType) {
				case BLUETOOTH:
					vBluetoothCommunication.SetSPIROMode();
					break;
				case USB:
					vitalTrackUSBCommunication.SetSPIROMode();
					break;
				default:
					break;
				}

				break;
			case SPO2:
				switch (connectionType) {
				case BLUETOOTH:
					vBluetoothCommunication.SetSPO2Mode();
					break;
				case USB:
					vitalTrackUSBCommunication.SetSPO2Mode();
					break;
				default:
					break;
				}

				break;
			default:
				break;
			}
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		switch (connectionType) {
		case BLUETOOTH:
			if (vBluetoothCommunication != null)
				vBluetoothCommunication.StopConnection();
			break;
		case USB:
			if (vitalTrackUSBCommunication != null)
				vitalTrackUSBCommunication.stopIoManager();
			break;
		default:
			break;
		}
	}

	private void SetMessage() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				
				mtvMachineStatus.setText(machinestatus );

			}
		});
	}

}

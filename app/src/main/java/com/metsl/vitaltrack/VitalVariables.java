package com.metsl.vitaltrack;

import metsl.vitaltrack.VitalTrackBluetoothCommunication;
import metsl.vitaltrack.VitalTrackCallback;
import android.bluetooth.BluetoothAdapter;

public class VitalVariables {
	public static VitalTrackBluetoothCommunication vbluetoothCommunication;
	public static BluetoothAdapter mBluetoothAdapter;
	public static VitalTrackCallback mVitalTrackCallback = null;
	public static String device_ID;
	public static String devicename;
	public static boolean _IsLiveECG = false;
	public static int HeartRate;
	public static int SPO2val;
	public static boolean SPO2Wave = false;

	public static int _Systolic = 0;
	public static int _Diastolic = 0;
	public static float _FVC;
	public static float _FEV1;
	public static float _PEFR;
	public static float _Ratio;
	public static boolean IsConnected = false;
	public static boolean IsTrying = false;
 	
	public static int mode;
	
	public static int BluetoothStatus;

	public static int _ContiECGData[][] = new int[16000][15];
	public static int _ContiSPoData[]= new int[16000];
	public static int read_ptr = 0;
	public static int write_ptr = 0;
	public static int diff_pointer = 0;
	
	public static int sporead_ptr = 0;
	public static int spowrite_ptr = 0;
	public static int spodiff_pointer = 0;
	
	// public static CustomizedBluetoothDevice CBD;
	public static String PATIENT_ID = "";


	public static void resetvariables() {
		device_ID = "";
		devicename = "";
		 _IsLiveECG = false;
			HeartRate = 0;
		IsConnected = false;
		 IsTrying = false;
			BluetoothStatus = 0;
			PATIENT_ID = "";
		read_ptr = 0;
		 write_ptr = 0;
		diff_pointer = 0;
		_ContiECGData = new int[16000][15];

}
}
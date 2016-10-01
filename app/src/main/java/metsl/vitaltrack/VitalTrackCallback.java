package metsl.vitaltrack;

import java.util.ArrayList;

public interface VitalTrackCallback {
	//All ECG Device  to Host (Mobile)
	public void onConnectComplete(String devicename);
	public void onConnectionFailed();
	public void onDeviceID(String device_ID);
	public void onLiveECG(ArrayList<ArrayList<Integer>> LiveECG);
	public void onHeartRate(int HeartRate);
	public void onSPO2(int SPO2val);
	public void onLiveSPO2(ArrayList<Byte> SPO2Wave);
	public void onNIBPData(int _Systolic, int _Diastolic);
	public void onSPIROValue(Float _FVC, Float _FEV1, Float _PEFR, Float _Ratio);
	public void onReady();
	public void onProbeAbsent();
	public void onNoFinger();
	public void onPulseSearch();
	public void onLowPerfusion();
	public void onWeakPulse();
	public void onNIBPReady();
	public void onCalibrating();
	public void onLeak();
	public void onOverPressure();
	public void onBPError();
	public void onNIBPReading();
	public void onCuffOversize();
	public void onCuffUndersize();
	public void onCuffMissing();
	public void onExcessiveMotion();
	public void onSignaltoostrong();
	public void onNIBPHardwareerror();
	public void onNIBPTimeout();
	public void onNIBPFlowError();
	public void onProjectedSystolic();
	public void onProjectedDiastolic();
	public void onTubeBlocked();
	public void onCheckingCuff();
	public void onCheckingLeak();
	public void onInflating();
	public void onCheckingPulse();
	public void onReleasing();
	public void onLeadsOFF();
	public void onMode(int mode);

	
}

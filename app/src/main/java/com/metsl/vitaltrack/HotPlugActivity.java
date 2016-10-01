/**
 * 
 */
package com.metsl.vitaltrack;

import metsl.vitaltrack.VitalTrackUSBCommunication;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

/**
 * @author sangeeta
 * 
 */
public class HotPlugActivity extends Activity {

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MainActivity.mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

	}

	@Override
	public void onResume() {
		super.onResume();

		// Initialise the USB interface for getting data from PPVPM or VT2
		Intent intent = getIntent();

		String action = intent.getAction();
		UsbDevice device = (UsbDevice) intent
				.getParcelableExtra(UsbManager.EXTRA_DEVICE);

		if (MainActivity.vitalTrackUSBCommunication != null) {
			MainActivity.vitalTrackUSBCommunication = new VitalTrackUSBCommunication();
			MainActivity.vitalTrackUSBCommunication.SetUSBDevice(null);
		}

		if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
			Intent i = new Intent(getApplicationContext(), MainActivity.class);
			i.putExtra("start", 1);
			startActivity(i);
			if (MainActivity.vitalTrackUSBCommunication == null) {
				MainActivity.vitalTrackUSBCommunication = new VitalTrackUSBCommunication();
			}
			MainActivity.vitalTrackUSBCommunication.SetUSBDevice(device);

		} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
			MainActivity.vitalTrackUSBCommunication.SetUSBDevice(null);
		}

		this.finish();
	}

}
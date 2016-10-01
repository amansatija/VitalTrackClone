/**
 * 
 */
package metsl.Util;

import android.annotation.SuppressLint;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import java.util.Map;

import metsl.UsbHostLibrary.Cp2102USBDriver;

/**
 * @author sangeeta
 * 
 */
public class UsbConnectionManager {

	public UsbDevice mDevice;
	public UsbManager mUsbManager;
	public UsbDeviceConnection mConnection;
	// send/close flag
    protected boolean isSending = false;
    protected boolean forceStop = false;

    public UsbConnectionManager(UsbManager usbmanager) {
		mUsbManager = usbmanager;
	}



	public UsbConnectionManager(byte b, byte c, byte[] rawOption) {
		
	}



	@SuppressLint("NewApi")
	public boolean FindAndConnectDevice() {
		boolean isConnected = false;
		for (final UsbDevice usbDevice : mUsbManager.getDeviceList().values()) {
        	boolean isSupported = probe(mUsbManager, usbDevice);
			if (isSupported) {
				mDevice = usbDevice;
				return true;
			}

		}
		return isConnected;
	}

	public boolean ConnectToDevice(UsbDevice usbDevice){
		boolean isSupported = probe(mUsbManager, usbDevice);
		if (isSupported) {
			mDevice = usbDevice;
			return true;
		}
		return false;
	}
	/**
	* Returns true if the Supported Device is Found and if Connection is established
	* 
	* @param usbManager the {@link UsbManager} to use.
     * @param usbDevice the device to test against.
	* @return
	*/
	@SuppressLint("NewApi")
	private boolean probe(final UsbManager manager, final UsbDevice usbDevice) {
		if (!testIfSupported(usbDevice, Cp2102USBDriver.getSupportedDevices())) {
			return false;
		}
		final UsbDeviceConnection connection = manager.openDevice(usbDevice);
		if (connection == null) {
			return false;
		}
		mConnection = connection;
		return true;
	}
	

	/**
	* Returns {@code true} if the given device is found in the driver's
	* vendor/product map.
	* 
	* @param usbDevice
	*            the device to test
	* @param supportedDevices
	*            map of vendor IDs to product ID(s)
	* @return {@code true} if supported
	*/
	@SuppressLint("NewApi")
	private boolean testIfSupported(final UsbDevice usbDevice,
			final Map<Integer, int[]> supportedDevices) {
		final int[] supportedProducts = supportedDevices.get(Integer
				.valueOf(usbDevice.getVendorId()));
		if (supportedProducts == null) {
			return false;
		}

		final int productId = usbDevice.getProductId();
		for (int supportedProductId : supportedProducts) {
			if (productId == supportedProductId) {
				return true;
			}
		}
		return false;
	}
	
	public boolean sendData(byte[] data, int id) {
		if(FindAndConnectDevice())
		{
			if(ConnectToDevice(mDevice))
			{
				if(probe(mUsbManager, mDevice))
				{
					UsbInputOutputManager inputOutput=new UsbInputOutputManager(mDevice, mConnection);
					inputOutput.writeAsync(data);
					inputOutput.run();
					inputOutput.stop();
				}
			}
		}
	   return true;  
       }
}

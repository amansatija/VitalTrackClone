/**
 * 
 */
package metsl.UsbHostLibrary;

/**
 * @author sangeeta
 * 
 */
public final class UsbId {
	public static final int VENDOR_MMSL = 0xffff;
	public static final int MMSL_CP2102 = 0x002f;

	public static final int VENDOR_MMSL_NEW = 0x04d8;
	public static final int MMSL_CP2102_NEW = 0x000a;
	
	
	public static final int MMSL_CP2102_APC2 = 0x000c;
	
	
	public static final int VENDOR_MMSL_APC3 = 0x10c4;
	public static final int MMSL_CP2102_APC3 = 0xea60;
	
	
	public static final int VENDOR_MMSL_VT = 0xffff;
	public static final int MMSL_CP2102_VT = 0x0006;
	
	
	

	private UsbId() {
		throw new IllegalAccessError("Non-instantiable class.");
	}
}

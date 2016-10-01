package mmsl.BluetoothLibrary;


public class ConnectionCommand {

	// Header (type + optionLen) length
	public static final int HEADER_LENGTH = Double.SIZE / Integer.SIZE;//Integer.SIZE / Byte.SIZE;

	// Command fields
	public byte commandstatus;
	public byte identifier;
	public int optionLen;
	public byte[] option;

	/**
	 * Constructor Create BTCommand without option.
	 * 
	 * @param type
	 *            Commands type
	 */
	public ConnectionCommand(byte commandstatus, byte identifier) {
		this(commandstatus, identifier, null);
	}

	/**
	 * Constructor Create BTCommand with option.
	 * 
	 * @param type
	 *            Commands type
	 * @param option
	 *            Commands option
	 */
	public ConnectionCommand(byte commandstatus, byte identifier, byte[] option) {
		this.commandstatus = commandstatus;
		this.identifier = identifier;
		if (option != null) {
			optionLen = option.length;
			this.option = new byte[option.length];
			System.arraycopy(option, 0, this.option, 0, option.length);
		} else {
			optionLen = 0;
			this.option = new byte[0];
		}
	}

	/**
	 * Convert BTCommand to byte array
	 * 
	 * @param command
	 *            target command
	 * @return byte array
	 * @hide
	 */
	protected static byte[] toByteArray(ConnectionCommand command) {
		byte[] ret = new byte[command.optionLen];

		System.arraycopy(command.option, 0, ret, 0, command.optionLen);
		return ret;
	}

	/**
	 * create BTCommand from Header and Option
	 * 
	 * @param header
	 *            header(byte array)
	 * @param option
	 *            option(byte array)
	 * @return BTCommand
	 * @hide
	 */
	public static ConnectionCommand fromHeaderAndOption(byte commandstatus,
			byte identifier, byte[] option) {
		// byte[] data = new byte[option.length];

		// System.arraycopy(option, 0, data, header.length, option.length);

		return new ConnectionCommand((byte) (commandstatus & 0xff), identifier, option);// fromByteArray(data,
		// order);

	}

}

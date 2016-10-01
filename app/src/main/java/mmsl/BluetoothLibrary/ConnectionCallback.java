package mmsl.BluetoothLibrary;

public interface ConnectionCallback {

	public void onConnectComplete(String devicename);
	public void onConnectionFailed();
	public void onDataSendComplete(int id);
	public void onCommandReceived(ConnectionCommand command);
	public void onDataRecieving(int percentage);
}

package mmsl.BluetoothLibrary;

import android.os.Message;

import java.io.IOException;
import java.io.OutputStream;

public class CommandSendThread extends Thread {

    private OutputStream mOut;
    private Message mMessage;
    private ConnectionCommand mCommand;
   
    public CommandSendThread(OutputStream out, ConnectionCommand command,
            Message msg) {
        mOut = out;
        mCommand = command;
        mMessage = msg;
      
    }

    @Override
    public void run() {
        try {
            mOut.write(ConnectionCommand.toByteArray(mCommand));
        } catch (IOException e) {
        	e.printStackTrace();
            mMessage.what = Connection.EVENT_CONNECTION_FAIL;
        }catch (Exception e) {
			// TODO: handle exception
		}

        mMessage.sendToTarget();
    }
}

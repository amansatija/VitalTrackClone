package com.metsl.vitaltrack;

import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

public class CanvasTimer extends TimerTask {
	private SurfaceHolder _SurfaceHolder;
	private ECGView _EcgView;
	Lock _lock;
	int pointSkipCount = 0;
	int parameterCount = 0;
	public int[] _ECGData;
	private boolean _timerRunFlag = false; // Sangeeta changes

	// private int timerid ;

	public CanvasTimer(SurfaceHolder s, ECGView e) {
		_SurfaceHolder = s;
		_EcgView = e;
		_lock = new ReentrantLock();
		_ECGData = new int[13];
		// Random r = new Random();
		// timerid = r.nextInt();
	}

	private void ParseData() {
		switch (_EcgView.mode) {
		case IDLE:
		case ECG:
			int lcount = 0;
			while (VitalVariables.diff_pointer > 0) {

				if (VitalVariables._ContiECGData == null)
					break;

				_ECGData[1] = VitalVariables._ContiECGData[VitalVariables.read_ptr][1];
				_ECGData[2] = VitalVariables._ContiECGData[VitalVariables.read_ptr][2];

				_ECGData[0] = (_ECGData[1] - _ECGData[2]);

				_ECGData[6] = VitalVariables._ContiECGData[VitalVariables.read_ptr][6];

				_ECGData[7] = VitalVariables._ContiECGData[VitalVariables.read_ptr][7];

				_ECGData[8] = VitalVariables._ContiECGData[VitalVariables.read_ptr][8];

				_ECGData[9] = VitalVariables._ContiECGData[VitalVariables.read_ptr][9];

				_ECGData[10] = VitalVariables._ContiECGData[VitalVariables.read_ptr][10];

				_ECGData[11] = VitalVariables._ContiECGData[VitalVariables.read_ptr][11];

				_ECGData[5] = ((_ECGData[1] + _ECGData[2]) / 2);
				_ECGData[4] = ((_ECGData[0] - _ECGData[2]) / 2);
				_ECGData[3] = ((-_ECGData[0] - _ECGData[1]) / 2);

				_ECGData[12] = VitalVariables._ContiECGData[VitalVariables.read_ptr][12];

				// Tabactivity.mBluetoothService._lock.lock();
				_lock.lock();
				VitalVariables.read_ptr++;
				if (VitalVariables.read_ptr >= VitalVariables._ContiECGData.length)
					VitalVariables.read_ptr = 0;
				VitalVariables.diff_pointer--;
				_lock.unlock();
				// Tabactivity.mBluetoothService._lock.unlock();

				pointSkipCount++;
				if (pointSkipCount > ECGView.SkipCount) {
					pointSkipCount = 0;
					_EcgView.DrawECG(_ECGData);
				}

				if (++lcount > 25) {
					_EcgView.DrawHRValue();
					break;
				}
			}
			break;
		case SPO2:
			int scount = 0;
			while (VitalVariables.spodiff_pointer > 0) {

				if (VitalVariables._ContiSPoData == null)
					break;
				int spo = VitalVariables._ContiSPoData[VitalVariables.sporead_ptr];
				_lock.lock();
				VitalVariables.sporead_ptr++;
				if (VitalVariables.sporead_ptr >= VitalVariables._ContiSPoData.length)
					VitalVariables.sporead_ptr = 0;
				VitalVariables.spodiff_pointer--;
				_lock.unlock();
				_EcgView.DrawSPO2(spo);
				if (++scount > 25) {
					_EcgView.DrawSPO2Value();
					break;
				}
			}
			break;
		case NIBP:
			_EcgView.DrawNibp();
			break;
		case SPIRO:
			_EcgView.DrawSpiro();
			break;
		default:
			break;
		}

	}

	public String padString(String string, int length) {
		return String.format("%1$" + length + "s", string);
	}

	@Override
	public void run() {
		if (_timerRunFlag)
			return;
		_timerRunFlag = true;
		// System.err.println("Timer " + String.valueOf(timerid) + " called");
		Canvas c;

		try {
			// Parse the data
			ParseData();

		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Error", "Error while Parsing Data : " + e.getMessage());

		}

		if (_EcgView.updatedFlag) {
			_EcgView.updatedFlag = false;
			c = null;
			try {
				c = _SurfaceHolder.lockCanvas(null);
				synchronized (_SurfaceHolder) {
					c.drawBitmap(_EcgView.offscrBmp, 0, 0, _EcgView.scrPaint);
				}
			} catch (Exception e) {
				Log.e("Error", "Error while Drawing Bitmap : " + e.getMessage());

			} finally {
				if (c != null) {
					_SurfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
		_timerRunFlag = false;
	}

}

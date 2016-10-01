package com.metsl.vitaltrack;

import java.util.Timer;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import metsl.vitaltrack.VitalTrackBluetoothCommunication;

/**
 * @author sangeeta
 * 
 */
public class ECGView extends SurfaceView implements SurfaceHolder.Callback {
	private Paint lead_color = new Paint(Paint.ANTI_ALIAS_FLAG);
	int Y_axis[],// Save a sine wave Y axis points
			centerY;// Center line
	float oldX, oldY,// On a XY points
			currentX;// The current drawing to a point on the X axis
	int ecgPointCount;
	/**
	 * Array Current Y Values for Lead L1-V6
	 */
	int[] ecg_y = new int[16];

	/**
	 * Array Previous Y Values for Lead L1-V6
	 */
	int[] prev_ecg_y = new int[16];
	/**
	 * Offscreen Bitmap
	 */
	public Bitmap offscrBmp;

	public boolean updatedFlag = false;

	public boolean isSavingFlag = false;

	/**
	 * Plotting Canvas for plotting the final Image on SurfaceView
	 */
	Canvas offScrCanvas = null;
	Message msg;
	Bundle bundle;
	float ecg_prev_x, ecg_cur_x;
	float spo_cur_x, spo_prev_x;
	int spo_y, spo_prev_y;
	int spo2Offset, ecgHeight, ecgOffset;

	public static float[] gain = new float[] { 0.5f / 2, 1.0f / 2, 1.5f / 2, 2.0f / 2 };
	int gain_val = 1;

	public boolean m_selectedmodechanged = true;
	VitalTrackBluetoothCommunication vBluetoothCommunication;
	/**
	 * Plotting Offset
	 */
	int offset = 0;
	// public CanvasThread canvasthread;
	private CanvasTimer _updateListTask; // Sangeeta changes
	private Timer _PlotTimer; // Sangeeta changes
	/**
	 * Array of Offset for Different Leads
	 */
	int offsetArray[];
	SurfaceHolder _SurfaceHolder;
	private int leadcolor = Color.parseColor("#99FF99");
	private int backcolor = Color.parseColor("#000000");
	public Paint scrPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	public static int SkipCount = 2;
	public static int LeadConfig = 2;
	/**
	 * X Value for LHS Leads
	 */
	int xValLeft;
	private int spo2x;

	/**
	 * Plotting Height
	 */
	private int plottingheight;

	/**
	 * Plotting width
	 */
	private int plottingwidth;

	/**
	 * Previous X Value for RHS Leads
	 */
	private int prexValRight;

	/**
	 * Previous X Value for LHS Leads
	 */
	int prexValLeft;

	MainActivity liveecg;

	public static boolean runFlag = false;

	int pointSkipCount = 0;
	int parameterCount = 0;
	public Mode mode;

	/**
	 * @param context
	 */
	public ECGView(Context context, MainActivity liveecg) {
		super(context);

		getHolder().addCallback(this);
		_SurfaceHolder = getHolder();
		this.liveecg = liveecg;
		runFlag = true;

		setFocusable(true);

		// SkipCount = 4;
		LeadConfig = 3;
		SkipCount = 2;

	}

	public void setmode(Mode _mode) {
		mode = _mode;

	}

	/*
	 * private class _DisplayRunnables implements Runnable { public void run() {
	 * 
	 * } }
	 */
	// private Runnable _DisplayRunnable = new Runnable() {

	protected void DrawECG(int[] _ECGData) {
		// super.onDraw(canvas);
		try {

			if (m_selectedmodechanged) {
				lead_color = new Paint(Paint.ANTI_ALIAS_FLAG);
				leadcolor = Color.parseColor("#99FF99");
				backcolor = Color.parseColor("#000000");
				scrPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

				m_selectedmodechanged = false;
				offset = 0;
				plottingwidth = getWidth();
				plottingheight = getHeight();

				System.out.println("Plotting Height : " + plottingheight
						+ " Plotting Width : " + plottingwidth);

				offscrBmp = Bitmap.createBitmap(plottingwidth, plottingheight,
						Config.RGB_565);
				offScrCanvas = new Canvas(offscrBmp);
				offScrCanvas.drawColor(backcolor);

				offsetArray = new int[13];

				xValLeft = 0;

				xValLeft += 25;
				lead_color.setColor(Color.WHITE);
				lead_color.setTextSize(15);

				offsetArray[0] = (int) (plottingheight / 14);
				prev_ecg_y[0] = offsetArray[0];
				for (int i = 1; i < offsetArray.length; i++) {
					offsetArray[i] = offsetArray[i - 1]
							+ (int) (plottingheight / 14);
					prev_ecg_y[i] = offsetArray[i];
				}

				offScrCanvas.drawText("V2", xValLeft, offsetArray[8],
						lead_color);
				offScrCanvas.drawText("V3", xValLeft, offsetArray[9],
						lead_color);
				offScrCanvas.drawText("V4", xValLeft, offsetArray[10],
						lead_color);
				offScrCanvas.drawText("V5", xValLeft, offsetArray[11],
						lead_color);
				offScrCanvas.drawText("V6", xValLeft, offsetArray[12],
						lead_color);

				offScrCanvas.drawText("aVR", xValLeft, offsetArray[4],
						lead_color);
				offScrCanvas.drawText("aVL", xValLeft, offsetArray[5],
						lead_color);
				offScrCanvas.drawText("aVF", xValLeft, offsetArray[6],
						lead_color);

				offScrCanvas.drawText("V1", xValLeft, offsetArray[7],
						lead_color);

				offScrCanvas.drawText("  I", xValLeft, offsetArray[1],
						lead_color);
				offScrCanvas.drawText("  II", xValLeft, offsetArray[2],
						lead_color);
				offScrCanvas.drawText("  III", xValLeft, offsetArray[3],
						lead_color);

				lead_color.setColor(Color.YELLOW);

				xValLeft = 60;
				prexValLeft = xValLeft;

			}

			if (_PlotTimer == null) {
				System.out.println("Refresh Timer is null");
			}
			scrPaint.setColor(backcolor);
			try {
				offScrCanvas.drawRect(xValLeft, 75, xValLeft + 10,
						(int) (plottingheight), scrPaint);

			} catch (Exception e) {
				e.printStackTrace();
				Log.e("Error",
						"Error Drawing Rectangle on Canvas : " + e.getMessage());
			}

			if (isSavingFlag) {
				scrPaint.setColor(Color.YELLOW);
			} else {
				scrPaint.setColor(leadcolor);
			}
			int end = 12;

			for (int j = 0; j < end; j++) {
				offset = offsetArray[j + 1];

				ecg_y[j] = (short) (offset - (_ECGData[j] * gain[gain_val]) / 5.0f);

				offScrCanvas.drawLine(prexValLeft, prev_ecg_y[j], xValLeft,
						ecg_y[j], scrPaint);
				prev_ecg_y[j] = ecg_y[j];

			}

			prexValLeft = xValLeft;
			// prexValRight = xValRight;
			xValLeft += 2;
			// xValRight++;
			if (xValLeft >= (plottingwidth) - 10) {

				xValLeft = 60;
				prexValLeft = 60;
			}
			/*
			 * if (xValRight >= plottingwidth - 25) {
			 * 
			 * xValRight = (plottingwidth / 2) + 50;
			 * 
			 * prexValRight = (plottingwidth / 2) + 50;
			 * 
			 * }
			 */

			lcount++;
			if (lcount >= 5) {
				lcount = 0;
				updatedFlag = true;

			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			Log.e("Error", "Error while Drawing ECG : " + e.getMessage());
		}
	}

	public void SetisSaving(boolean flag) {
		isSavingFlag = flag;
	}

	int savecount = 0;

	int lcount = 0;

	public void DrawNibp() {
		scrPaint.setColor(backcolor);
		try {
			
			offScrCanvas.drawRect(0, 0, plottingwidth, (int) (plottingheight),
					scrPaint);
			lead_color.setColor(Color.MAGENTA);
			String text = "BP : " + String.valueOf(VitalVariables._Systolic)
					+ "/" + String.valueOf(VitalVariables._Diastolic);
			lead_color.setTextSize(50);

			offScrCanvas.drawText(text, plottingwidth / 3, plottingheight / 2,
					lead_color);

		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Error",
					"Error Drawing Rectangle on Canvas : " + e.getMessage());
		}

		updatedFlag = true;

	}

	public void DrawSpiro() {
		scrPaint.setColor(backcolor);
		try {
			offScrCanvas.drawRect(0, 0, plottingwidth, (int) (plottingheight),
					scrPaint);
			lead_color.setColor(Color.GREEN);
			lead_color.setTextSize(50);
			String textFVC = "FVC : " + String.valueOf(VitalVariables._FVC);
			float poffset = plottingheight / 7;
			offScrCanvas.drawText(textFVC, plottingwidth / 3, poffset,
					lead_color);
			poffset += plottingheight / 7;
			String textFEV = "FEV : " + String.valueOf(VitalVariables._FEV1);
			offScrCanvas.drawText(textFEV, plottingwidth / 3, poffset,
					lead_color);

			String textPEFR = "PEFR : " + String.valueOf(VitalVariables._PEFR);
			poffset += plottingheight / 7;
			offScrCanvas.drawText(textPEFR, plottingwidth / 3, poffset,
					lead_color);
			String textRATIO = "RATIO : "
					+ String.valueOf(VitalVariables._Ratio);
			poffset += plottingheight / 7;
			offScrCanvas.drawText(textRATIO, plottingwidth / 3, poffset,
					lead_color);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Error",
					"Error Drawing Rectangle on Canvas : " + e.getMessage());
		}
		updatedFlag = true;
	}

	public void DrawHRValue() {
		scrPaint.setColor(backcolor);

		offScrCanvas.drawRect(0, 0, plottingwidth, 75, scrPaint);

		lead_color.setTextSize(30);
		offScrCanvas.drawText(
				"HR : " + String.valueOf(VitalVariables.HeartRate),
				plottingwidth / 3, 50, lead_color);
	}

	public void DrawSPO2Value() {
		scrPaint.setColor(backcolor);

		offScrCanvas.drawRect(0, 0, plottingwidth, 150, scrPaint);

		lead_color.setTextSize(50);
		offScrCanvas.drawText(
				"SPO2 : " + String.valueOf(VitalVariables.SPO2val)
						+ "    HR : "
						+ String.valueOf(VitalVariables.HeartRate),
				plottingwidth / 6, 100, lead_color);
	}

	public void DrawSPO2(int spo2) {
		try {
			scrPaint.setColor(backcolor);

			offScrCanvas.drawRect(spo_cur_x, 150, spo_cur_x + 10,
					(int) (plottingheight), scrPaint);

			if (m_selectedmodechanged) {
				m_selectedmodechanged = false;
				offScrCanvas.drawRect(0, 0, plottingwidth,
						(int) (plottingheight), scrPaint);
			}

			scrPaint.setColor(Color.MAGENTA);
			scrPaint.setStrokeWidth(2);
			short spo = (short) (plottingheight / 2 - spo2);

			offScrCanvas.drawLine(spo_prev_x, spo_prev_y, spo_cur_x, spo,
					scrPaint);
			spo_prev_y = spo;
			spo_prev_x = spo_cur_x;
			spo_cur_x++;

			if (spo_cur_x >= (plottingwidth) - 10) {

				spo_cur_x = 10;
				spo_prev_x = 10;
			}
			lcount++;
			if (lcount >= 5) {
				lcount = 0;
				updatedFlag = true;

			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Error",
					"Error Drawing Rectangle on Canvas : " + e.getMessage());
		}
	}

	/*
	 * 
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.view.SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder
	 * , int, int, int)
	 */
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder
	 * )
	 */
	public void surfaceCreated(SurfaceHolder holder) {
		// new Thread(this).start();
		try {
			/*
			 * runFlag = true; if (displayThread == null) { displayThread = new
			 * Thread(_DisplayRunnable); }
			 * 
			 * displayThread.start();
			 */
			m_selectedmodechanged = true;
			StartTimer();
			// canvasthread = new CanvasThread(getHolder(), this);
			// canvasthread.start();
			/*
			 * if(canvasthread == null){ canvasthread = new
			 * CanvasThread(getHolder(), this);
			 * 
			 * }
			 */

		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Error", "Error Starting Timer : " + e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.
	 * SurfaceHolder)
	 */
	public void surfaceDestroyed(SurfaceHolder holder) {
		// System.err.print("Surface Destroyed " + _PlotTimer);
		StopECGTimer();
	}

	public void DestroyThread() {

		// boolean retry = true;

		/*
		 * while (retry) { try { if (canvasthread.getRunning()){
		 * canvasthread.setRunning(false); } retry = false; } catch (Exception
		 * e) { } }
		 */

	}

	private void StartTimer() {
		// TODO Auto-generated method stub
		// Start The timer -- Abhi change for sheduled timer start

		if (_updateListTask == null || _PlotTimer == null) {
			_updateListTask = new CanvasTimer(getHolder(), this);
			_PlotTimer = new Timer();
			_PlotTimer.schedule(_updateListTask, 500, 100);
		} // endif

	}

	private void StopECGTimer() {
		if (_PlotTimer != null) {

			_PlotTimer.cancel();
			_PlotTimer = null;
			_updateListTask = null;
			// System.err.println("Refresh Timer is closed");
		}
	}
}

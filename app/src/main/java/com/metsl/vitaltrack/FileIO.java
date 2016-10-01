package com.metsl.vitaltrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.util.Log;

public class FileIO {

	private String filename;
	FileOutputStream bos;

	public void SetFilename(String filename) {
		this.filename = filename;

	}

	public String GetFilename() {
		return this.filename;
	}

	public void OpenFile() {
		try {
			File f = new File(filename);
			if (f.exists()) {
				f.delete();
			}

			bos = new FileOutputStream(filename, true);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Log.e("Error", "Error File Not Found : " + e.getMessage());
		}
	}

	public void CloseFile() {
		try {
			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("Error", "Error Closing File : " + e.getMessage());
		}
	}

	public boolean writeToFile(byte[] Data) {

		try {
			// BufferedOutputStream bos = new BufferedOutputStream(
			// new FileOutputStream(filename, true));

			bos.write(Data);
			bos.flush();
			// bos.close();
			return true;
		} catch (java.io.IOException e) {
			e.printStackTrace();
			Log.e("Error", "Error Writing Data : " + e.getMessage());
			return false;
		}
	}

	public byte[] readFile(File file) {
		// Open file
		RandomAccessFile f;
		try {
			f = new RandomAccessFile(file, "r");

			try {
				// Get and check length
				long longlength = f.length();
				int length = (int) longlength;
				if (length != longlength)
					return null;
				// Read file and return data
				byte[] data = new byte[length];
				f.readFully(data);
				return data;
			} finally {
				f.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Error", "Error while Reading File : " + e.getMessage());
			return null;
		}
	}

}


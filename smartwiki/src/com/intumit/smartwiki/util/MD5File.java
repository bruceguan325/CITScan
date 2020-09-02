package com.intumit.smartwiki.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class MD5File {

	private File file;

	public MD5File(File file) {
		this.file = file;
	}

	public String getMD5String(String fileName) {

		String result = "";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while(line != null) {
				if(line.indexOf(fileName) != -1)
					return line.substring(0, line.indexOf(' ')).trim();

				line = reader.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

}

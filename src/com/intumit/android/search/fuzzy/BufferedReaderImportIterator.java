package com.intumit.android.search.fuzzy;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;

class BufferedReaderImportIterator implements ImportIterator {
	BufferedReader reader;
	
	public BufferedReaderImportIterator(BufferedReader reader) {
		super();
		this.reader = reader;
	}

	@Override
	public String next() {
		String line = null;
		try {
			line = StringUtils.trim(reader.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return line;
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}
	
	

}

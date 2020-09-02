package com.intumit.solr;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;


public class CJKPhoneticsReader extends Reader {
	private Reader reader;
	public CJKPhoneticsReader(Reader reader) {
		this.reader = reader;
	}


	@Override
	public void close() throws IOException {
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		char[] tmpbuf = new char[len];
		int realLen = reader.read(tmpbuf, off, len);
		
		for (int i=0; i < realLen; i++) {
			cbuf[i] = CJKPhoneticsUtil.getMappedWord(tmpbuf[i]);
			
//			if (CJKPhoneticsUtil.hasDiffPhonetics(tmpbuf[i]))
//				System.out.println("[" + tmpbuf[i] + "]" + CJKPhoneticsUtil.getDiffPhonetics(tmpbuf[i]));
		}
		
		return realLen;
	}

	
	public static void main(String[] args) {
		CJKPhoneticsReader r = new CJKPhoneticsReader(new StringReader("倚天屠龍記"));
		
		char[] buf = new char[255];
		try {
			int len = r.read(buf);
			
			System.out.println(new String(buf, 0, len));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		r = new CJKPhoneticsReader(new StringReader("李熬"));
		try {
			int len = r.read(buf);
			
			System.out.println(new String(buf, 0, len));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

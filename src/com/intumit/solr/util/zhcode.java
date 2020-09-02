package com.intumit.solr.util;

import java.lang.*;
import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.charset.*;

/* Copyright 2002 Erik Peterson 
 Code and program free for non-commercial use.
 Contact erik@mandarintools.com for fees and
 licenses for commercial use.
 */

class zhcode extends Encoding {
	// Simplfied/Traditional character equivalence hashes
	protected Hashtable s2thash, t2shash;
	private SinoDetect sinodetector;
	public boolean autodetect;
	private int unsupportedStrategy;

	public final static int DELETE = 0;
	public final static int HTMLESC = 1;
	public final static int UNIESC = 2;
	public final static int QUESTIONMARK = 3;
	public final static int TOTAL = 4;

	public final static String[] strategyNames = { "Delete", "HTML Escape",
			"Unicode Escape", "Question Mark" };

	public final static String[] strategyDescriptions = { "Delete",
			"Use HTML Escape (e.g. &#x4e00;)",
			"Use Unicode Escape (e.g. \\u4e00)", "Replace with Question Mark" };

	// Constructor
	public zhcode() {
		super();
		unsupportedStrategy = UNIESC;
		String dataline;
		sinodetector = new SinoDetect();
		autodetect = false;

		// Initialize and load in the simplified/traditional character hashses
		s2thash = new Hashtable();
		t2shash = new Hashtable();

		try {
			InputStream pydata = getClass().getResourceAsStream("hcutf8.txt");
			BufferedReader in = new BufferedReader(new InputStreamReader(
					pydata, "UTF8"));
			while ((dataline = in.readLine()) != null) {
				// Skip empty and commented lines
				if (dataline.length() == 0 || dataline.charAt(0) == '#') {
					continue;
				}

				// Simplified to Traditional, (one to many, but pick only one)
				s2thash.put(dataline.substring(0, 1), dataline.substring(1, 2));

				// Traditional to Simplified, (many to one)
				for (int i = 1; i < dataline.length(); i++) {
					t2shash.put(dataline.substring(i, i + 1), dataline
							.substring(0, 1));
				}
			}
		} catch (Exception e) {
			System.err.println(e);
		}

	}

	public void setUnsupportedStrategy(int strategy) {
		if (strategy >= 0 && strategy < TOTAL) {
			unsupportedStrategy = strategy;
		}
	}

	public int getUnsupportedStrategy() {
		return unsupportedStrategy;
	}

	public String convertString(String inline, int source_encoding,
			int target_encoding) {
		StringBuffer outline = new StringBuffer(inline);
		convertStringBuffer(outline, source_encoding, target_encoding);
		return outline.toString();

		/*
		 * int lineindex; String currchar; CharSequence dataline;
		 * 
		 * if (source_encoding == HZ) { dataline = hz2gb(inline.toString()); }
		 * else { dataline = inline; }
		 * 
		 * for (lineindex = 0; lineindex < dataline.length(); lineindex++) {
		 * currchar = "" + dataline.charAt(lineindex); if ((source_encoding ==
		 * GB2312 || source_encoding == GBK || source_encoding == ISO2022CN_GB
		 * || source_encoding == HZ || source_encoding == UNICODE ||
		 * source_encoding == UNICODES || source_encoding == UTF8 ||
		 * source_encoding == UTF8S) && (target_encoding == BIG5 ||
		 * target_encoding == CNS11643 || target_encoding == UNICODET ||
		 * target_encoding == UTF8T || target_encoding == ISO2022CN_CNS)) { if
		 * (s2thash.containsKey(currchar) == true) {
		 * outline.append(s2thash.get(currchar)); } else {
		 * outline.append(currchar); } } else if ((source_encoding == BIG5 ||
		 * source_encoding == CNS11643 || source_encoding == UNICODET ||
		 * source_encoding == UTF8 || source_encoding == UTF8T ||
		 * source_encoding == ISO2022CN_CNS || source_encoding == GBK ||
		 * source_encoding == UNICODE) && (target_encoding == GB2312 ||
		 * target_encoding == UNICODES || target_encoding == ISO2022CN_GB ||
		 * target_encoding == UTF8S || target_encoding == HZ)) { if
		 * (t2shash.containsKey(currchar) == true) {
		 * outline.append(t2shash.get(currchar)); } else {
		 * outline.append(currchar); } } else { outline.append(currchar); } }
		 * 
		 * if (target_encoding == HZ) { // Convert to look like HZ return
		 * gb2hz(outline.toString()); }
		 * 
		 * return outline.toString();
		 */
	}

	public void convertStringBuffer(StringBuffer dataline, int source_encoding,
			int target_encoding) {
		int lineindex;
		String currchar;
		char charvalue;

		if (source_encoding == HZ) {
			hz2gbStringBuffer(dataline);
		}

		for (lineindex = 0; lineindex < dataline.length(); lineindex++) {
			charvalue = dataline.charAt(lineindex);
			currchar = "" + charvalue;
			if (((int) charvalue == 0xfeff || (int) charvalue == 0xfffe)
					&& (target_encoding != UNICODE
							&& target_encoding != UNICODES
							&& target_encoding != UNICODET
							&& target_encoding != UTF8
							&& target_encoding != UTF8S && target_encoding != UTF8T)) {
				dataline.deleteCharAt(lineindex);
				continue;
			}

			if ((source_encoding == GB2312 || source_encoding == GBK
					|| source_encoding == ISO2022CN_GB || source_encoding == HZ
					|| source_encoding == GB18030 || source_encoding == UNICODE
					|| source_encoding == UNICODES || source_encoding == UTF8 || source_encoding == UTF8S)
					&& (target_encoding == BIG5 || target_encoding == CNS11643
							|| target_encoding == UNICODET
							|| target_encoding == UTF8T || target_encoding == ISO2022CN_CNS)) {
				if (s2thash.containsKey(currchar) == true) {
					dataline.replace(lineindex, lineindex + 1, (String) s2thash
							.get(currchar));
				}
			} else if ((source_encoding == BIG5 || source_encoding == CNS11643
					|| source_encoding == UNICODET || source_encoding == UTF8
					|| source_encoding == UTF8T
					|| source_encoding == ISO2022CN_CNS
					|| source_encoding == GBK || source_encoding == GB18030 || source_encoding == UNICODE)
					&& (target_encoding == GB2312
							|| target_encoding == UNICODES
							|| target_encoding == ISO2022CN_GB
							|| target_encoding == UTF8S || target_encoding == HZ)) {
				if (t2shash.containsKey(currchar) == true) {
					dataline.replace(lineindex, lineindex + 1, (String) t2shash
							.get(currchar));
				}
			}
		}

		if (target_encoding == HZ) {
			// Convert to look like HZ
			gb2hzStringBuffer(dataline);
		}

		Charset charset = Charset.forName(javaname[target_encoding]);
		CharsetEncoder encoder = charset.newEncoder();

		for (int i = 0; i < dataline.length(); i++) {
			if (encoder.canEncode(dataline.subSequence(i, i + 1)) == false) {
				// Replace or delete
				// Delete
				if (unsupportedStrategy == DELETE) {
					dataline.deleteCharAt(i);
					i--;
				} else if (unsupportedStrategy == HTMLESC) {
					// HTML Escape &#xNNNN;
					dataline.replace(i, i + 1, "&#x"
							+ Integer.toHexString((int) dataline.charAt(i))
							+ ";");
				} else if (unsupportedStrategy == UNIESC) {
					// Unicode Escape \\uNNNN
					dataline.replace(i, i + 1, "\\u"
							+ Integer.toHexString((int) dataline.charAt(i)));
				} else if (unsupportedStrategy == QUESTIONMARK) {
					// Unicode Escape \\uNNNN
					dataline.replace(i, i + 1, "?");
				}
			}
		}

	}

	public String hz2gb(String hzstring) {
		StringBuffer gbstring = new StringBuffer(hzstring);
		hz2gbStringBuffer(gbstring);
		return gbstring.toString();
	}

	public void hz2gbStringBuffer(StringBuffer hzstring) {
		byte[] hzbytes; // = new byte[2];
		byte[] gbchar = new byte[2];
		int i = 0;
		StringBuffer gbstring = new StringBuffer("");

		// Convert to look like equivalent Unicode of GB
		for (i = 0; i < hzstring.length(); i++) {
			if (hzstring.charAt(i) == '~') {
				if (hzstring.charAt(i + 1) == '{') {
					hzstring.delete(i, i + 2);
					while (i < hzstring.length()) {
						if (hzstring.charAt(i) == '~'
								&& hzstring.charAt(i + 1) == '}') {
							hzstring.delete(i, i + 2);
							i--;
							break;
						} else if (hzstring.charAt(i) == '\r'
								|| hzstring.charAt(i) == '\n') {
							break;
						}
						gbchar[0] = (byte) (hzstring.charAt(i) + 0x80);
						gbchar[1] = (byte) (hzstring.charAt(i + 1) + 0x80);
						try {
							hzstring.replace(i, i + 2, new String(gbchar,
									"GB2312"));
						} catch (Exception usee) {
							System.err.println(usee.toString());
						}
						i++;
					}
				} else if (hzstring.charAt(i + 1) == '~') { // ~~ becomes ~
					hzstring.replace(i, i + 2, "~");
				}
			}
		}

	}

	public String gb2hz(String gbstring) {
		StringBuffer hzbuffer = new StringBuffer(gbstring);
		gb2hzStringBuffer(hzbuffer);
		return hzbuffer.toString();
	}

	public void gb2hzStringBuffer(StringBuffer gbstring) {
		byte[] gbbytes = new byte[2];
		int i;
		boolean terminated = false;

		for (i = 0; i < gbstring.length(); i++) {
			if ((int) gbstring.charAt(i) > 0x7f) {
				gbstring.insert(i, "~{");
				terminated = false;
				while (i < gbstring.length()) {
					if (gbstring.charAt(i) == '\r'
							|| gbstring.charAt(i) == '\n') {
						gbstring.insert(i, "~}");
						i += 2;
						terminated = true;
						break;
					} else if ((int) gbstring.charAt(i) <= 0x7f) {
						gbstring.insert(i, "~}");
						i += 2;
						terminated = true;
						break;
					}
					try {
						gbbytes = gbstring.substring(i, i + 1).getBytes(
								"GB2312");
					} catch (UnsupportedEncodingException uee) {
						System.out.println(uee);
					}
					gbstring.delete(i, i + 1);
					gbstring.insert(i, (char) (gbbytes[0] + 256 - 0x80));
					gbstring.insert(i + 1, (char) (gbbytes[1] + 256 - 0x80));
					i += 2;
				}
				if (terminated == false) {
					gbstring.insert(i, "~}");
					i += 2;
				}
			} else {
				if (gbstring.charAt(i) == '~') {
					gbstring.replace(i, i + 1, "~~");
					i++;
				}
			}
		}

	}

	public void convertFile(String sourcedir, String targetdir,
			int source_encoding, int target_encoding) {
		BufferedReader srcbuffer;
		BufferedWriter outbuffer;
		String dataline;

		Vector inputfiles = new Vector();
		Vector outputfiles = new Vector();
		inputfiles.add(sourcedir);
		outputfiles.add(targetdir);
		int i, j, working_encoding;
		File tmpfile, tmpout;
		String dirfiles[];
		for (i = 0; i < inputfiles.size(); i++) {
			tmpfile = new File((String) inputfiles.get(i));
			if (tmpfile.exists() == false) {
				System.out.println("ERROR: Source file "
						+ (String) inputfiles.get(i) + " does not exist.\n");
				continue;
			}
			if (tmpfile.isDirectory() == true) {
				tmpout = new File((String) outputfiles.get(i));
				if (tmpout.exists() == false) {
					tmpout.mkdir();
				}

				dirfiles = tmpfile.list();
				if (dirfiles != null) {
					for (j = 0; j < dirfiles.length; j++) {
						inputfiles.add((String) inputfiles.get(i)
								+ File.separator + dirfiles[j]);
						outputfiles.add((String) outputfiles.get(i)
								+ File.separator + dirfiles[j]);

					}
				}
				continue;
			}

			System.err.println("Converting " + inputfiles.get(i) + " to "
					+ outputfiles.get(i) + " with encoding " + source_encoding);
			try {
				int codeguess = sinodetector.detectEncoding(tmpfile);
				if (codeguess == Encoding.ASCII || codeguess == Encoding.OTHER) {
					// Just copy file as is
					InputStream in = new FileInputStream(new File(
							(String) inputfiles.get(i)));
					OutputStream out = new FileOutputStream(new File(
							(String) outputfiles.get(i)));

					// Transfer bytes from in to out
					byte[] buf = new byte[1024];
					int len;
					while ((len = in.read(buf)) > 0) {
						out.write(buf, 0, len);
					}
					in.close();
					out.close();
					continue;
				}

				working_encoding = source_encoding;
				if (autodetect) {
					working_encoding = codeguess;
				}
				srcbuffer = new BufferedReader(new InputStreamReader(
						new FileInputStream((String) inputfiles.get(i)),
						javaname[working_encoding]));
				outbuffer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream((String) outputfiles.get(i)),
						javaname[target_encoding]));
				while ((dataline = srcbuffer.readLine()) != null) {
					outbuffer.write(convertString(dataline, working_encoding,
							target_encoding));
					outbuffer.newLine();
				}
				srcbuffer.close();
				outbuffer.close();
			} catch (Exception ex) {
				System.err.println(ex);
			}
			// convertFile((String)inputfiles.get(i),
			// (String)outputfiles.get(i),
			// source_encoding, target_encoding);
		}

	}

	/*
	 * public void convertFile(String sourcefile, String outfile, int
	 * source_encoding, int target_encoding) { BufferedReader srcbuffer;
	 * BufferedWriter outbuffer; String dataline;
	 * 
	 * try { srcbuffer = new BufferedReader(new InputStreamReader(new
	 * FileInputStream(sourcefile), javaname[source_encoding])); outbuffer = new
	 * BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile),
	 * javaname[target_encoding])); while ((dataline = srcbuffer.readLine()) !=
	 * null) { outbuffer.write(convertString(dataline, source_encoding,
	 * target_encoding)); outbuffer.newLine(); } srcbuffer.close();
	 * outbuffer.close(); } catch (Exception ex) { System.err.println(ex); } }
	 */

	public static void main(String argc[]) {
		int codetypes[];
		char codetype;
		Vector inputfiles = new Vector();
		zhcode zhcoder = new zhcode();

		// Determine source and target encodings, store in codetypes
		codetypes = new int[2];
		argc[0] = argc[0].toLowerCase();
		for (int i = 0; i < 2; i++) {
			codetype = argc[0].charAt(i + 1);
			// Print Help
			if (codetype == 'h') {
				System.out
						.println("Usage:  java zhcode -[gbc867ui2nkhra][gbc867uts2nkhr] in_file out_file");
				System.out
						.println("  g = GB2312, b = Big5, c = CNS11643, 8 = UTF-8, 6 = UTF-8 (simp),");
				System.out
						.println("  7 = UTF-8 (trad), u = Unicode, t = Unicode (traditional characters),");
				System.out.println("  s = Unicode (simplified characters),");
				System.out.println("  3 = GB18030,");
				System.out
						.println("  i = ISO-2022-CN, 2 = ISO-2022-CN-GB, n = ISO-2022-CN-CNS,");
				System.out
						.println("  k = GBK, h = HZ, r = EUC-KR, a = autodetect source encoding");
				System.exit(0);
			}

			if (codetype == 'g') {
				codetypes[i] = GB2312;
			} else if (codetype == 'h') {
				codetypes[i] = HZ;
			} else if (codetype == 'b') {
				codetypes[i] = BIG5;
			} else if (codetype == 'c') {
				codetypes[i] = CNS11643;
			} else if (codetype == '8') {
				codetypes[i] = UTF8;
			} else if (codetype == '6') {
				codetypes[i] = UTF8S;
			} else if (codetype == '7') {
				codetypes[i] = UTF8T;
			} else if (codetype == 'u') {
				codetypes[i] = UNICODE;
			} else if (codetype == 't') {
				codetypes[i] = UNICODET;
			} else if (codetype == 's') {
				codetypes[i] = UNICODES;
			} else if (codetype == 'i') {
				codetypes[i] = ISO2022CN;
			} else if (codetype == '2') {
				codetypes[i] = ISO2022CN_GB;
			} else if (codetype == 'n') {
				codetypes[i] = ISO2022CN_CNS;
			} else if (codetype == '3') {
				codetypes[i] = GB18030;
			} else if (codetype == 'k') {
				codetypes[i] = GBK;
			} else if (codetype == 'r') {
				codetypes[i] = EUC_KR;
			} else if (codetype == 'a') {
				codetypes[i] = ASCII;
				zhcoder.autodetect = true;
			}
			;
		}

		// Call the file convert function with appropriate arguments
		zhcoder.convertFile(argc[1], argc[2], codetypes[0], codetypes[1]);
	}

}

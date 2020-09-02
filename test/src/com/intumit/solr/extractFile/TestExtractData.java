package com.intumit.solr.extractFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.ContentHandler;

import com.intumit.solr.util.FileParser;

public class TestExtractData {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	/*
	@Test
	public void testWordParser() throws Exception {

		try {
			File aFile = new File("D:\\html\\2130.jpg");
			Reader reader = new ParsingReader(aFile);
			String str = IOUtils.toString(reader);
			FileUtils.writeStringToFile(new File("D:\\html\\3.txt"), str,
					"UTF-8");
			System.out.println(str);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	*/

	@Test
	public void testParser() throws Exception {
		File file = new File("E:/WiSe_Presale/ACER_EBOOK/intumit/test/test-files/parse/mi200701_0001");
		StringBuffer strBuf = new StringBuffer();
		
		recursive(file, strBuf, null);
		
		System.out.println(strBuf.toString());
	}
	
	/*
	@Test
	public void testEPubParser() throws Exception {
	    ParseContext context;
	    Detector detector;
	    AutoDetectParser parser;
	    Metadata metadata;
	    ContentHandler contentHandler;
	    ByteArrayOutputStream output = new ByteArrayOutputStream();
	    
		context = new ParseContext();
        detector = (new TikaConfig()).getMimeRepository();
        parser = new AutoDetectParser(detector);
        context.set(Parser.class, parser);
        
        contentHandler = new BodyContentHandler(output);//getTransformerHandler(output, "xml", null); ////
        metadata = new Metadata();
        
		URL url;
        File file = new File("./test-files/parse/mi200701_0001/OPS/fb.ncx");
		//File file = new File("./test-files/parse/0991209_搜尋引擎會議記錄.doc");
        if (file.isFile()) {
            url = file.toURI().toURL();
        } else {
            url = new URL("./test-files/parse/Wonderful_Stories_for_Children.epub");
        }
        InputStream input = TikaInputStream.get(url, metadata);
        try {
            parser.parse(
                    input, contentHandler,
                    metadata, context);
            String text = contentHandler.toString();
            System.out.println(output.toString().substring(0, 1000));
            //System.out.println(metadata.get("title"));
        } finally {
            input.close();
            System.out.flush();
        }
	}
	*/
	

    /**
     * Returns a transformer handler that serializes incoming SAX events
     * to XHTML or HTML (depending the given method) using the given output
     * encoding.
     *
     * @see <a href="https://issues.apache.org/jira/browse/TIKA-277">TIKA-277</a>
     * @param method "xml" or "html"
     * @param encoding output encoding,
     *                 or <code>null</code> for the platform default
     * @return {@link System#out} transformer handler
     * @throws TransformerConfigurationException
     *         if the transformer can not be created
     */
    private static TransformerHandler getTransformerHandler(
            OutputStream out, String method, String encoding)
            throws TransformerConfigurationException {
        SAXTransformerFactory factory = (SAXTransformerFactory)
                SAXTransformerFactory.newInstance();
        TransformerHandler handler = factory.newTransformerHandler();
        handler.getTransformer().setOutputProperty(OutputKeys.METHOD, method);
        handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
        if (encoding != null) {
            handler.getTransformer().setOutputProperty(
                    OutputKeys.ENCODING, encoding);
        }
        handler.setResult(new StreamResult(out));
        return handler;
    }

	private static void recursive(File file, StringBuffer strBuf, final String extensions) {
		if (file.exists()) {
			if (file.isDirectory()) {
				File[] files = file.listFiles(new FileFilter() {

					@Override
					public boolean accept(File pathname) {

						if (extensions == null) {
							return true;
						}
						
						if (pathname.isDirectory())
							return false;
						
						String fileName = pathname.getName();
						if (fileName.lastIndexOf(".") != -1) {
							String ext = fileName.substring(fileName.lastIndexOf("."));
							
							if (extensions.indexOf(ext) != -1) {
								return true;
							}
						}
						return false;
					}
					
				});

				FileParser parser = FileParser.getInstance();
				for (int i=0; i < files.length; i++) {
					strBuf.append(parser.autoParse(files[i]));
				}
				
				File[] dirs = file.listFiles(new FileFilter() {

					@Override
					public boolean accept(File pathname) {						
						if (pathname.isDirectory())
							return true;
						
						return false;
					}
					
				});
				
				for (int i=0; i < dirs.length; i++) {
					recursive(dirs[i], strBuf, extensions);
				}
			}
		}
	}
}

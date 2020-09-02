package com.intumit.solr.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

public class FileParser {
	private static FileParser parser = null;
	
	public static FileParser getInstance() {
		if (parser == null) {
			parser = new FileParser();
		}
		
		return parser;
	}

	public FileParser() {
		super();
	}
    
    public String autoParse(File file) {
        return autoParse(file, new Metadata());
    }

    public String autoParse(File file, Metadata md) {
        try {
            return autoParse(file.toURI().toURL(), md);
        } 
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
	
	public String autoParse(URL url, Metadata metadata) {
	    //ByteArrayOutputStream output = new ByteArrayOutputStream();
	    StringWriter output = new StringWriter();

        try {
			ParseContext context;
		    Detector detector;
		    AutoDetectParser parser;
		    ContentHandler contentHandler;
		    
			context = new ParseContext();
	        detector = (new TikaConfig()).getMimeRepository();
	        parser = new AutoDetectParser(detector);
	        context.set(Parser.class, parser);
	        
	        contentHandler = new BodyContentHandler(output);//getTransformerHandler(output, "xml", null); ////
	        if (metadata == null)
	            metadata = new Metadata();
	        
	        InputStream input = TikaInputStream.get(url, metadata);
	        try {
	            parser.parse(
	                    input, contentHandler,
	                    metadata, context);
	        }
	        finally {
	            input.close();
	        }
            //contentHandler.toString();
            //System.out.println(output.toString().substring(0, 1000));
            //System.out.println(metadata.get("title"));
        } 
        catch (Exception e) {
        	throw new RuntimeException(e);
        }
        
        return output.toString();
	}
    
    public String autoParse(byte[] bin) {
        //ByteArrayOutputStream output = new ByteArrayOutputStream();
        StringWriter output = new StringWriter();

        try {
            ParseContext context;
            Detector detector;
            AutoDetectParser parser;
            Metadata metadata;
            ContentHandler contentHandler;
            
            context = new ParseContext();
            detector = (new TikaConfig()).getMimeRepository();
            parser = new AutoDetectParser(detector);
            context.set(Parser.class, parser);
            
            contentHandler = new BodyContentHandler(output);//getTransformerHandler(output, "xml", null); ////
            metadata = new Metadata();
            InputStream input = TikaInputStream.get(bin, metadata);
            try {
                parser.parse(
                        input, contentHandler,
                        metadata, context);
            }
            finally {
                input.close();
            }
            //contentHandler.toString();
            //System.out.println(output.toString().substring(0, 1000));
            //System.out.println(metadata.get("title"));
        } 
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return output.toString();
    }
}

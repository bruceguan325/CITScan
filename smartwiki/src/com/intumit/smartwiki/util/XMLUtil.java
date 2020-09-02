package com.intumit.smartwiki.util;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class XMLUtil {

	private static final Logger log = Logger.getLogger(Util.class);


    public static Document readXml(File file) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder(false);

        return builder.build(file);
    }



}

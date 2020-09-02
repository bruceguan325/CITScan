package com.intumit.systemconfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.solr.util.WiSeEnv;

public class RobotImageFileConfig {

	private static final Logger LOG = LoggerFactory.getLogger(RobotImageFileConfig.class);

	private static Properties properties;

	private static final String FILE_NAME = "imagefile_config.properties";
	private static Path filePath = Paths
			.get(new StringBuilder().append(WiSeEnv.getHomePath()).append("/").append(FILE_NAME).toString());;

	private static RobotImageFileConfig instance;
	
	private static String[] info = new String[2];

	public static RobotImageFileConfig getInstance() {
		if (instance == null) {
			instance = new RobotImageFileConfig();
			info[0] = instance.getProperty("imageFilePath");
			info[1] = instance.getProperty("serverUUID");
		}
		return instance;
	}

	private RobotImageFileConfig() {
		try {
			if (Files.exists(filePath)) {
				BufferedReader reader = Files.newBufferedReader(filePath);
				properties = new Properties();
				properties.load(reader);
			}
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
	}

	public String getProperty(String key) {
		return properties.getProperty(key);
	}
	
	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}
	
	public void save() {
		try {
			if (Files.exists(filePath)) {
				OutputStream fos = Files.newOutputStream(filePath);
				properties.store(fos, "Properties");
				fos.close();
			}
			instance = null;
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
	}
	
	@SuppressWarnings("unused")
	public static String[] getImageFileConfig() {
		RobotImageFileConfig cfg = RobotImageFileConfig.getInstance();
		return info;
	}

}

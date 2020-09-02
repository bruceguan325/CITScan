package com.intumit.solr.util.fileupload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties({"content"})
public class FileMeta implements Serializable {
	
	private String fileName;
	private String fileSize;
	private String fileType;
	private String twitter;
	private boolean qaFile = false;
	
	private InputStream content;
	private byte[] contentCache = null;
	
	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public String getFileSize() {
		return fileSize;
	}
	
	public void setFileSize(String fileSize) {
		this.fileSize = fileSize;
	}
	
	public String getFileType() {
		return fileType;
	}
	
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	
	public boolean isQaFile() {
		return qaFile;
	}
	
	public void setQaFile(boolean qaFile) {
		this.qaFile = qaFile;
	}
	
	public InputStream getContent(){
		try {
			if (contentCache == null && content.available() > 0) {
				doContentCache();
			}
			if (contentCache != null) {
				return new ByteArrayInputStream(contentCache);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this.content;
	}
	
	public void setContent(InputStream content){
		this.content = content;
	}
	
	public String getTwitter(){
		return this.twitter;
	}
	
	public void setTwitter(String twitter){
		this.twitter = twitter;
	}
	
	@Override
	public String toString() {
		return "FileMeta [fileName=" + fileName + ", fileSize=" + fileSize
				+ ", fileType=" + fileType + "]";
	}
	
	public void doContentCache() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024 * 10];

		for (int length = 0; (length = content.read(buffer)) > 0;) {
			baos.write(buffer, 0, length);
		}

		baos.close();
		content.close();
		
		contentCache  = baos.toByteArray();
	}
	
}

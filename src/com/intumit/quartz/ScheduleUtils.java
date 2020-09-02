package com.intumit.quartz;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class ScheduleUtils {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SAXReader reader = new SAXReader();
		Document document;
		try {
			document = reader
					.read(new InputStreamReader(
							new FileInputStream(
									"D:\\Project\\WiSe_Dev\\wise\\WiSe\\tomcat\\webapps\\wise\\WEB-INF\\jobSchedule.xml"),
							"UTF-8"));
			String name = "";
			String url = "";
			String cron = "";

			// Node node =
			// document.selectSingleNode("//quartz/job/job-detail/name");
			List job = document.selectNodes("//quartz/job");

			for (int i = 0; i < job.size(); i++) {
				Node node = (Node) job.get(i);
				List entry = node.selectNodes("job-detail/job-data-map/entry");

				Node subNode = (Node) entry.get(1);

				if (node != null) {

					name = node.selectSingleNode("job-detail/name").getText();
					if (StringUtils.indexOf(name, "core0") == -1)
						continue;
					System.out.println(name);

					subNode = subNode.selectSingleNode("value");
					url = subNode.getText();
					System.out.println(url);

					cron = node
							.selectSingleNode("trigger/cron/cron-expression")
							.getText();
					System.out.println(cron);
				}
			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (DocumentException e) {
			e.printStackTrace();
		}

		Job job = new Job();
		job.setName("test_core1");
		job.setUrl("http://127.0.0.1:8080");
		job.setCron("0 0/30 1-23 * * ?");
		// addJobSchedule(job);

		
			//deleteJobSchedule(new File("../WiSe/tomcat/webapps/wise/WEB-INF/jobSchedule.xml").getCanonicalPath(), job);


	}

	public Document xmlDao(String file) {
		SAXReader reader = new SAXReader();
		Document document = null;
		try {
			document = reader.read(new InputStreamReader(new FileInputStream(
					file), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return document;

	}

	public void deleteJobSchedule(String file, Job job) {

		Document document = xmlDao(file);
		List jobList = new ArrayList();
		try {

			String name = "";

			// Node node =
			// document.selectSingleNode("//quartz/job/job-detail/name");
			List list = document.selectNodes("//quartz/job");

			for (int i = 0; i < list.size(); i++) {
				Node node = (Node) list.get(i);

				if (node != null) {

					name = node.selectSingleNode("job-detail/name").getText();
					// String[] str=StringUtils.split(name, "_");

					if (!job.getName().equals(name))
						continue;

					node.detach();
					//document.remove(node);
					writeXml(file, document, "UTF-8");
				}

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void addJobSchedule(String file, Job job) {

		try {
			Document document = xmlDao(file);

			// List list=document.selectNodes("//quartz");

			// Element root = document.addElement( "quartz" );
			Element root = (Element) document.selectSingleNode("//quartz");
			Element j = root.addElement("job");
			Element jd = j.addElement("job-detail");
			jd.addElement("name").addText(job.getName());
			jd.addElement("group").addText("DEFAULT");
			jd.addElement("description").addText(job.getName());
			jd.addElement("job-class").addText(
					"com.intumit.quartz.DataImportSchedule");
			Element jdm = jd.addElement("job-data-map").addAttribute(
					"allows-transient-data", "true");
			Element e1 = jdm.addElement("entry");
			e1.addElement("key").addText("name");
			e1.addElement("value").addText(job.getName());
			Element e2 = jdm.addElement("entry");
			e2.addElement("key").addText("url");
			e2.addElement("value").addText(job.getUrl());
			Element c = j.addElement("trigger").addElement("cron");
			c.addElement("name").addText(job.getName());
			c.addElement("group").addText("DEFAULT");
			c.addElement("job-name").addText(job.getName());
			c.addElement("job-group").addText("DEFALUT");
			c.addElement("cron-expression").addText(job.getCron());
			writeXml(file, document, "UTF-8");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public List getJobSchedule(String file, String core) {

		Document document = xmlDao(file);

		List jobList = new ArrayList();

		String name = "";
		String url = "";
		String cron = "";

		// Node node =
		// document.selectSingleNode("//quartz/job/job-detail/name");
		List job = document.selectNodes("//quartz/job");

		for (int i = 0; i < job.size(); i++) {
			Node node = (Node) job.get(i);
			List entry = node.selectNodes("job-detail/job-data-map/entry");

			Node subNode = (Node) entry.get(1);

			if (node != null) {

				name = node.selectSingleNode("job-detail/name").getText();
				String[] str = StringUtils.split(name, "_");

				if (!core.equals(str[str.length - 1]))
					continue;
				Job j = new Job();
				j.setName(name);
				// System.out.println(name);

				subNode = subNode.selectSingleNode("value");
				url = subNode.getText();
				j.setUrl(url);
				// System.out.println(url);

				cron = node.selectSingleNode("trigger/cron/cron-expression")
						.getText();
				j.setCron(cron);
				// System.out.println(cron);
				jobList.add(j);
			}
		}

		return jobList;
	}

	public static void writeXml(String xmlPath, Document jdoc, String encode)
			throws IOException, FileNotFoundException {

		OutputStream os = new FileOutputStream(xmlPath);
		try {
			// org.dom4j.io.XMLWriter
			org.dom4j.io.OutputFormat outputFormat = OutputFormat.createPrettyPrint();
			outputFormat.setEncoding(encode);
			XMLWriter writer = new XMLWriter(new FileWriter(xmlPath),
					outputFormat);
			writer.write(jdoc);
			writer.close();

			// outputter.setEncoding(encode);
			// outputter.setIndent("  ");
			// outputter.setNewlines(true);
			// outputter.output(jdoc, os);
		} finally {
			os.close();
		}
	}

}

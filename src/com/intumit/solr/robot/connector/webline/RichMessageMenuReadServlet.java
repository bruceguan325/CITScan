package com.intumit.solr.robot.connector.webline;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.Blob;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.intumit.solr.robot.RobotImageFile;
import com.intumit.solr.robot.RobotImageFilePath;
import com.intumit.solr.util.XssStringFilter;
import com.intumit.systemconfig.RobotImageFileConfig;

/**
 * Web圖文檔案存取
 * 
 * @author Dudamel
 */
@WebServlet("/img/webLine/*")
public class RichMessageMenuReadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static Logger log = Logger.getLogger(RichMessageMenuReadServlet.class.getName());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		XssStringFilter xssStr = new XssStringFilter();
		boolean error = true;
		String uri = xssStr.getCheck(req.getRequestURI());
		String contextPath = req.getContextPath().replace("/", "");
		String[] imgFileCfg = RobotImageFileConfig.getImageFileConfig();
		try {
			// replace ratio
			uri = uri.replaceAll("@[1-9]x", "");
			// uri => /wsie/img/webLine/XXX or /wsie/img/webLine/XXX/size
			String[] subPath = uri.split("/");
			Path path = Paths.get(new StringBuilder().append(RobotImageFilePath.getOldPath()).append(File.separator)
					.append(RichMessage.imgPath).append(File.separator).append(subPath[4]).toString());
			Path newPath = Paths.get(new StringBuilder().append(RobotImageFilePath.getNewPath()).append(File.separator)
					.append(RichMessage.imgPath).append(File.separator).append(subPath[4]).toString());
			String[] baseUrlSizeCollection = RichMessageServlet.getBaseUrlSizeCollection();
			String fileName = StringUtils.substringBeforeLast(subPath[4], ".");
			fileName = new String(Base64.getDecoder().decode(fileName.getBytes()));
			Integer tid = Integer.valueOf(StringUtils.substringBefore(fileName, "_"));
			
			RobotImageFile msgImg = null;
			RobotImageFile menuImg = null;
			switch (subPath.length) {
			case 5:
				msgImg = RobotImageFile.getBy(tid, subPath[4], RichMessage.class.getName(), "img/webLine", null);
				menuImg = RobotImageFile.getBy(tid, subPath[4], RichMenu.class.getName(), "img/webLine", null);
				break;
			case 6:
				msgImg = RobotImageFile.getBy(tid, subPath[5], RichMessage.class.getName(), "img/webLine/" + subPath[4],
						null);
				break;
			}
			Boolean markToClean = RobotImageFile.checkHostMarkForClean(imgFileCfg[1],
					msgImg != null ? msgImg : menuImg);
			if (markToClean) {
				if (Files.exists(path)) {
					FileUtils.forceDelete(path.toFile());
				}
				if (Files.exists(newPath)) {
					FileUtils.forceDelete(newPath.toFile());
				}
			}
			
			if (Files.isDirectory(newPath) || Files.exists(newPath)) {
				error = false;
			} else if (Files.isDirectory(path) && !Files.isDirectory(newPath)
					&& StringUtils.endsWithAny(uri, baseUrlSizeCollection)) {
				RichMessage.createFolder(subPath[4]);

				for (String size : baseUrlSizeCollection) {
					Path p = Paths.get(new StringBuilder().append(path).append(File.separator).append(size).toString());
					Path nP = Paths
							.get(new StringBuilder().append(newPath).append(File.separator).append(size).toString());
					copyToNewPath(p, nP);
				}
				Files.delete(path);
				error = false;
			} else if (Files.exists(path) && !Files.exists(newPath)
					&& !StringUtils.endsWithAny(uri, baseUrlSizeCollection)) {
				RichMessage.createFolder("");

				copyToNewPath(path, newPath);
				error = false;
			} else if (!Files.isDirectory(newPath) && StringUtils.endsWithAny(uri, baseUrlSizeCollection)) {
				for (String size : baseUrlSizeCollection) {
					Path nP = Paths
							.get(new StringBuilder().append(newPath).append(File.separator).append(size).toString());
					msgImg = RobotImageFile.getBy(tid, size, RichMessage.class.getName(),
							"img/webLine/" + subPath[4], null);
					if (msgImg != null && msgImg.getFileBody() != null) {
						try {
							RichMessage.createFolder(subPath[4]);

							copyImageFileToPath(msgImg, nP);
							error = false;
						} catch (Exception e) {
							log.log(Level.ERROR, e.toString());
						}
					}
				}
			} else if (!Files.exists(newPath) && !StringUtils.endsWithAny(uri, baseUrlSizeCollection)) {
				if ((msgImg != null && msgImg.getFileBody() != null)
						|| (menuImg != null && menuImg.getFileBody() != null)) {
					try {
						RichMessage.createFolder("");

						copyImageFileToPath(msgImg != null ? msgImg : menuImg, newPath);
						error = false;
					} catch (Exception e) {
						log.log(Level.ERROR, e.toString());
					}
				}
			}
		} catch (Exception e) {
			System.out.println("ERROR in : " + e);
		}
		SecureRandom secRandom = new SecureRandom();
		int rand = secRandom.nextInt();
		String target = "?rnd=" + rand;
		if (!error) {
			// 導向 /ImageFilePath/img/webLine/
			String page = "/" + imgFileCfg[0] + "/" + uri.replaceAll(contextPath + "/", "");
			req.getRequestDispatcher(page + target).forward(req, res);
		} else {
			String forward = "/ErrorPage403.jsp";
			req.getRequestDispatcher(forward + target).forward(req, res);
		}
	}

	void copyImageFileToPath(RobotImageFile imgFile, Path path) throws Exception {
		Blob blob = imgFile.getFileBody();
		InputStream in = blob.getBinaryStream();
		OutputStream os = Files.newOutputStream(path);
		IOUtils.copy(in, os);
		in.close();
		os.close();
	}

	void copyToNewPath(Path path, Path newPath) throws IOException {
		InputStream in = Files.newInputStream(path);
		OutputStream os = Files.newOutputStream(newPath);
		IOUtils.copy(in, os);
		in.close();
		os.close();
		Files.delete(path);
	}

}

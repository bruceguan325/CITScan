package com.intumit.solr.robot;

import java.io.File;

import com.intumit.solr.util.WiSeEnv;
import com.intumit.systemconfig.RobotImageFileConfig;
import com.intumit.systemconfig.WiseSystemConfig;

/**
 * 設定公用實體的路徑
 * 
 * @author dudamel
 *
 */
public class RobotImageFilePath {

	private static String oldPath = new StringBuilder().append(WiSeEnv.getHomePath().replace("kernel", "webapps"))
			.append(File.separator).append(WiseSystemConfig.get().getContextPath()).toString();
	private static String newPath = "";

	public static String getOldPath() {
		return oldPath;
	}

	public static void setNewPath(Boolean reset) {
		if (!reset) {
			newPath = new StringBuilder().append(oldPath).append(File.separator)
					.append(RobotImageFileConfig.getImageFileConfig()[0]).toString();
		} else {
			newPath = "";
		}
	}

	public static String getNewPath() {
		if (newPath.isEmpty())
			setNewPath(false);
		return newPath;
	}

}

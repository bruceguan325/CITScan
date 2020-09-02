/*
 * Created on 2004/8/16
 *
 */
package com.intumit.dir;

import java.io.File;
import java.io.FileFilter;
import java.net.UnknownHostException;
import java.util.ArrayList;

import jcifs.UniAddress;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;
import jcifs.smb.SmbSession;

/**
 * @author Tin Chen
 * 
 */
public class BrowseDirService {

	private static final FileFilter dirFileFilter = new FileFilter() {

		public boolean accept(File file) {
			return file.isDirectory();
		}
	};

	private static final BrowseDirService me = new BrowseDirService();

	private BrowseDirService() {
	}

	public static BrowseDirService getInstance() {
		return me;
	}

	/**
	 * @see com.intumit.smartkms.spider.folder.DirectoryBrowserService#listRoots()
	 */
	public File[] listRoots() {
		return File.listRoots();
	}

	/**
	 * @see com.intumit.smartkms.spider.folder.DirectoryBrowserService#list(java.lang.String)
	 */
	public File[] list(String dirPath) {
		return new File(dirPath).listFiles(dirFileFilter);
	}

	public boolean testConnect(String host, Authenticator auth) {
		try {
			UniAddress a = UniAddress.getByName(host);
			NtlmPasswordAuthentication auth2 = new NtlmPasswordAuthentication(
					auth.getDomain(), auth.getUserName(), auth.getPassword());
			SmbSession.logon(a, auth2);
		} catch (UnknownHostException e) {
			return false;
		} catch (SmbException e) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.intumit.smartkms.spider.folder.BrowseDirService#listRemoteDirName
	 * (java.lang.String, com.intumit.smartspider.folder.Authenticator)
	 */
	public String[] listRemote(String parentUncPath, Authenticator auth)
			throws SmbException {
		SmbFile startSmbFile = SmbUtil.toSmbFile(parentUncPath, auth);
		SmbFile[] dirs = startSmbFile.listFiles(new SmbFileFilter() {
			public boolean accept(SmbFile f) throws SmbException {
				return f.isDirectory() && (!f.isHidden());
			}
		});
		String[] dirNames = new String[dirs.length];
		for (int i = 0; i < dirNames.length; i++) {
			String dirName = dirs[i].getName();
			dirNames[i] = dirName.substring(0, dirName.length() - 1);
		}
		return dirNames;
	}

	public String[] listRemoteFile(String parentUncPath, Authenticator auth)
			throws SmbException {
		SmbFile startSmbFile = SmbUtil.toSmbFile(parentUncPath, auth);
		SmbFile[] dirs = startSmbFile.listFiles();
		String[] dirNames = new String[] {};
		String[] subDirNames = new String[] {};
		for (int i = 0; i < dirs.length; i++) {
			if (!dirs[i].isDirectory()) {
				String dirName = dirs[i].getName();
				dirNames[i] = dirName.substring(0, dirName.length() - 1);
			} else {
				String pathurl = parentUncPath + "\\" + dirs[i].getName();
				System.out.println("browser" + pathurl);
				int flag = i;
				subDirNames = listRemoteFile(pathurl, auth);
				System.out.println("subsize" + subDirNames.length);
				if (subDirNames.length > 0) {
					for (int k = 0; k < subDirNames.length; k++) {
						String dname = dirs[i].getName();
						dirNames[flag] = dname + "\\" + subDirNames[k];
						System.out.println("HHHHHH:" + dirNames[flag]);
						flag++;
					}
				}
			}
		}
		return dirNames;
	}

	public ArrayList<String> listRemoteFileForArrayList(String parentUncPath,
			Authenticator auth) throws SmbException {
		
		SmbFile startSmbFile = SmbUtil.toSmbFile(parentUncPath, auth);
		SmbFile[] dirs = startSmbFile.listFiles();
		ArrayList dirNames = new ArrayList<String>();
		ArrayList subDirNames = new ArrayList<String>();
		for (int i = 0; i < dirs.length; i++) {
			if (!dirs[i].isDirectory()) {
				String dirName = dirs[i].getName();
				dirNames.add(dirName);
			} else {
				String pathurl = parentUncPath + "\\" + dirs[i].getName();
				//System.out.println("browser" + pathurl);
				int flag = i;
				subDirNames = listRemoteFileForArrayList(pathurl, auth);
//				//System.out.println("subsize" + subDirNames.size());
				if (subDirNames.size() > 0) {
					for (int k = 0; k < subDirNames.size(); k++) {
						String dname = dirs[i].getName().substring(0, dirs[i].getName().length() - 1);
						dirNames.add(dname + "\\" + subDirNames.get(k));
						//System.out.println("HHHHHH:" + dirNames.get(dirNames.size()-1));
						flag++;
					}
				}
			}
		}
		return dirNames;
	}

	public static void main(String[] args) {
		String[] aa = new String[] {};
		for (int i = 0; i < 3; i++) {
			aa[i] = "sdada";
		}
		System.out.println(aa.length);
	}
	/**
	 * @see com.intumit.smartkms.spider.folder.DirectoryBrowserService#hasChild(java.lang.String)
	 */
	// public boolean hasChild(String dirPath) {
	// return new File(dirPath).listFiles(dirFileFilter).length > 0;
	// }
}

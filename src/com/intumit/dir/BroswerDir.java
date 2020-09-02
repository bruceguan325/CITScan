package com.intumit.dir;

import java.io.File;
import java.net.UnknownHostException;

import jcifs.UniAddress;
import jcifs.smb.*;


public class BroswerDir {
    private static final BroswerDir me = new BroswerDir();

    private BroswerDir() {
    }

    public static BroswerDir getInstance() {
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
    /*
    public File[] list(String dirPath) {
        return new File(dirPath).listFiles(dirFileFilter);
    }
*/
    public boolean testConnect(String host, Authenticator auth) {
        try {
            UniAddress a = UniAddress.getByName(host);
            NtlmPasswordAuthentication auth2 = new NtlmPasswordAuthentication(
                    auth.getDomain(), auth.getUserName(), auth.getPassword());
            SmbSession.logon(a, auth2);
        }
        catch (UnknownHostException e) {
            return false;
        }
        catch (SmbException e) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.intumit.smartkms.spider.folder.BrowseDirService#listRemoteDirName(java.lang.String,
     *      com.intumit.smartspider.folder.Authenticator)
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
    
    public static void main(String[] args){
    	Authenticator a=new Authenticator();
    	BroswerDir d=new BroswerDir();
    	//boolean state=d.testConnect("10.1.3.45", a);
    	try {
			String [] filelist=d.listRemote("10.1.3.124", a);
			//System.out.println(state+"scuess");
			System.out.println(filelist.length);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
	
}

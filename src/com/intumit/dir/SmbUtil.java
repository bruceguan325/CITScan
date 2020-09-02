package com.intumit.dir;

import java.net.MalformedURLException;
import jcifs.smb.SmbFile;
import com.intumit.util.Assert;

public class SmbUtil {

    public static String toSmbUrl(String uncPath, Authenticator auth) {
        uncPath = uncPath.replace('\\', '/');
        Assert.assertTrue(uncPath.startsWith("//"));
        uncPath = uncPath.substring(2);
        if (!uncPath.endsWith("/")) {
            uncPath = uncPath + "/";
        }
        int pos = uncPath.indexOf("/");
        String server = uncPath.substring(0, pos);
        String userInfo = auth != null ? auth.toString() : "";
        if (userInfo.length() > 0) {
            userInfo = userInfo + "@";
        }
        System.out.println("smb://" + userInfo + uncPath + "?server=" + server);
        return "smb://" + userInfo + uncPath + "?server=" + server;
    }

    public static SmbFile toSmbFile(String uncPath, Authenticator auth) {
        String smbUrl = SmbUtil.toSmbUrl(uncPath, auth);
        try {
            return new SmbFile(smbUrl);
        }
        catch (MalformedURLException e) {
            Assert.intoUnreachableCode();
            return null;
        }
    }

}

package com.intumit.dir;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.intumit.util.Assert;

public class Authenticator implements Serializable {

    private String domain;
    private String userName;
    private String password;

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (domain != null && !"".equals(domain)) {
            sb.append(domain + ";");
        }
        if (userName != null && !"".equals(userName)) {
            //            Assert.assertNotNull(password);
            //            Assert.assertTrue(!"".equals(password));
            sb.append(userName);
            sb.append(':');
            try {
                sb.append(URLEncoder.encode(password, "iso-8859-1"));
            }
            catch (UnsupportedEncodingException impossible) {
                Assert.intoUnreachableCode();
            }
        }
        return sb.toString();
    }

}

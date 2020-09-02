package com.citi.filter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.Globals;

import com.intumit.citi.CitiUtil;
import com.intumit.citi.SsoErrorException;
import com.intumit.citi.SsoErrorType;
import com.intumit.hithot.HitHotLocale;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;

@WebFilter(urlPatterns = { "/wiseadm/citiadlogin" }, asyncSupported = true)
public class SsoSecurityFilter implements Filter {

    private static final Logger log = Logger.getLogger("SsoSecurityFilter");

    public void doFilter(ServletRequest sreq, ServletResponse sres, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)sreq;

        AdminUser user = AdminUserFacade.getInstance().getFromSession(request.getSession());
        HttpServletResponse response = (HttpServletResponse)sres;
       
        String auth = request.getHeader("Authorization");
        log.info(String.format("Authorization header value [%s]", auth));
        if (!"".equals(auth) & auth != null) {
            auth = auth.replace("+", " ");
        }
        else {
            auth = null;
        }

        if (auth == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "NTLM");
            response.flushBuffer();
            return;
        }
        if (auth.startsWith("NTLM ")) {
            log.info(
                    String.format("Login from [%s] check auth with NTLM", request.getRemoteHost()));
            byte[] msg = new sun.misc.BASE64Decoder().decodeBuffer(auth.substring(5));
            int off = 0, length, offset;

            if (msg[8] == 1) {
                off = 18;
                byte z = 0;
                byte[] msg1 = { (byte)'N', (byte)'T', (byte)'L', (byte)'M', (byte)'S', (byte)'S',
                        (byte)'P', z, (byte)2, z, z, z, z, z, z, z, (byte)40, z, z, z, (byte)1,
                        (byte)130, (byte) 8, z, z, (byte)2, (byte)2, (byte)2, z, z, z, z, //
                        z, z, z, z, z, z, z, z };
                // send ntlm type2 msg

                response.setHeader("WWW-Authenticate",
                        "NTLM " + new sun.misc.BASE64Encoder().encodeBuffer(msg1).trim());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                log.info(String.format("check auth failed: [%s]:[%s]", "WWW-Authenticate",
                        "NTLM " + new sun.misc.BASE64Encoder().encodeBuffer(msg1).trim()));
                response.flushBuffer();
                return;
            }
            else if (msg[8] == 3) {
                log.info(String.format("Login from [%s] check auth with NTLM type3 message",
                        request.getRemoteHost()));
                off = 30;
                offset = (msg[off + 11] & 0x0FF) * 256 + (msg[off + 10] & 0x0FF);
                length = (msg[off + 9] & 0x0FF) * 256 + (msg[off + 8] & 0x0FF);
                String userIdWithDomain = new String(msg, offset, length, Charset.forName("UTF-16LE")).trim();
                log.info(String.format("check auth success: userId:[%s]", userIdWithDomain));
                String userId = userIdWithDomain.substring(userIdWithDomain.indexOf('\\') + 1);
                try {
                    response.addHeader("X-Frame-Options", "DENY");
                    Optional<AdminUser> ssoUser = CitiUtil.getAndUpdateAdminUser(userId);
                    if(ssoUser.isPresent()) {
                        HttpSession session = request.getSession(true);
                        session.invalidate();
                        session = request.getSession(true);
                        AdminUserFacade.getInstance().setSession(session, ssoUser.get());
                        session.setAttribute(Globals.LOCALE_KEY, HitHotLocale.zh_TW.getLocale());
                        response.sendRedirect(request.getContextPath() + "/wiseadm/qaAdmin.jsp");
                    }
                    else {
                        redirectLoginPage(request, response, SsoErrorType.userNoPermission.name(), userId);
                    }
                    return;
                }
                catch (Exception e) {
                    log.error("Login with ntlm failed: ", e);
                    String errTypeOrMsg = e.getMessage();
                    if(e instanceof SsoErrorException) {
                        errTypeOrMsg = ((SsoErrorException)e).getErrType().name();
                    }
                    redirectLoginPage(request, response, errTypeOrMsg, userId);
                    return;
                }
            }
        }
    }

    private void redirectLoginPage(HttpServletRequest request, HttpServletResponse response, String errTypeOrMsg, String userId)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/wiseadm/login.jsp?ssoErrorType=" + CitiUtil.getEncryptedSsoError(errTypeOrMsg, userId));
    }

    @Override
    public void destroy() {}

    @Override
    public void init(FilterConfig arg0) throws ServletException {}

}

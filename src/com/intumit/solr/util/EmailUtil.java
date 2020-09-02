package com.intumit.solr.util;

import java.io.UnsupportedEncodingException;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import com.intumit.systemconfig.WiseSystemConfig;

public class EmailUtil {

	public static String sendmail(InternetAddress[] to, InternetAddress[] bcc, String subject, String messageText, MimeBodyPart... attachments) {

		try {
			// 設定安全認證
			//Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
			final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

			WiseSystemConfig wsc = WiseSystemConfig.get();
			final String username = wsc.getMailUsername();
			final String sendFrom = wsc.getMailUsername();
			final String password = wsc.getMailPassword();

			// 設定所要用的 SMTP Mail 伺服器和所使用的傳送協定
			java.util.Properties props = System.getProperties();
			props.setProperty("mail.smtp.host", wsc.getMailServerHost());
			props.setProperty("mail.smtp.port", String.valueOf(wsc.getMailServerPort()));
			if(wsc.isEnableSsl()) {
				props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
				props.setProperty("mail.smtp.socketFactory.fallback", "false");
				props.setProperty("mail.smtp.socketFactory.port", String.valueOf(wsc.getMailServerPort()));
			}
			props.put("mail.smtp.auth", "true");

			// 產生新的Session 服務 new
			javax.mail.Session mailSession = Session.getDefaultInstance(props,
					new Authenticator() {
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(username,
									password);
						}
					});

			Message msg = new MimeMessage(mailSession);

			// 設定傳送郵件的發信人
			msg.setFrom(new InternetAddress(sendFrom));

			// 設定傳送郵件至收信人的信箱
			msg.setRecipients(Message.RecipientType.TO, to);
			msg.setRecipients(Message.RecipientType.BCC, bcc);

			// 設定信中的主題
			msg.setSubject(subject);

			// 設定傳送信的MIME Type
			MimeMultipart mm = new MimeMultipart();
			MimeBodyPart mbp = new MimeBodyPart();
			mbp.setContent(messageText, "text/html;charset=UTF8");
			mm.addBodyPart(mbp);


			// Attachments
			if (attachments != null) {
				for (MimeBodyPart attachPart: attachments) {
				  mm.addBodyPart(attachPart);
				}
			}


			msg.setContent(mm);

			// 送信
			Transport.send(msg);
			return "郵件己順利傳送";

		} catch (Exception ex) {
			ex.printStackTrace();
			com.intumit.syslog.SyslogEntity.log(null, "syslog:" + EmailUtil.class.getName(), ex.getClass().getName(), "errMsg=" + ex.getMessage() + "&subject" + subject , "failed");
			return "WRONG!!";
		}
	}

	public static String sendmail(String to, String bcc, String subject, String messageText) {
		try {
			return sendmail(InternetAddress.parse(to, false), InternetAddress.parse(bcc, false), subject, messageText, null);
		} catch (Exception mex) {
			System.out.println(mex.toString());
			return "WRONG!!";
		}
	}

	public static MimeBodyPart toAttachment(byte[] binary, String mimeType, String filename) throws MessagingException, UnsupportedEncodingException {
		MimeBodyPart abp = new MimeBodyPart();
		DataSource source = new ByteArrayDataSource(binary, mimeType);//"text/csv");
		abp.setDataHandler(new DataHandler(source));
		abp.setFileName(javax.mail.internet.MimeUtility.encodeText(filename));
		return abp;
	}

	public static void main(String[] args) {
		sendmail("herb@intumit.com", "", "Test from GmailUtil", "<h1>as title</h1><table><tr><td>AA</td><td>BB</td></tr></table>");
	}
}

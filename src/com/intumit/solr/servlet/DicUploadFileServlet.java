package com.intumit.solr.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.intumit.message.MessageUtil;
import com.intumit.solr.AuditAction;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.dictionary.DictionaryVersionService;
import com.intumit.solr.tenant.Tenant;
import com.intumit.syslog.OperationLogEntity;

public class DicUploadFileServlet extends HttpServlet {

	private static final long serialVersionUID = 6404059598535271725L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		Integer opLogId = (Integer) req.getAttribute("opLogId");
		OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
		boolean isMultipart = ServletFileUpload.isMultipartContent(req);
		res.setContentType("text/html; charset=UTF8");
		res.setCharacterEncoding("UTF-8");
		Locale locale = req.getLocale();
		PrintWriter out = res.getWriter();
		int addCount = 0, updateCount = 0;
		StringBuilder filenames = new StringBuilder();
		boolean dryRun = Boolean.parseBoolean(StringUtils.defaultString(req.getParameter("dryRun"), "true"));
		
		boolean isInAudit = false;
		boolean isNotPurpose = false;
		try {
			Tenant t = Tenant.getFromSession(req.getSession());
			DiskFileItemFactory factory = new DiskFileItemFactory();
			factory.setSizeThreshold(4096);
			ServletFileUpload upload = new ServletFileUpload(factory);
			upload.setSizeMax(10000000);
			List<FileItem> items = new ArrayList<FileItem>();

			if (dryRun) {
				if (isMultipart) {
					items.addAll(upload.parseRequest(req));
				}
			}
			else {
				items.add((FileItem) req.getSession().getAttribute("DicImportCsvTempFile"));
			}
			
			for (FileItem item : items) {
				if (item.isFormField()) {
					String name = item.getFieldName();
					String value = item.getString();
					System.out.println("name:" + name + " value:" + value);
				}
				else {
					req.getSession().setAttribute("DicImportCsvTempFile", item);
					filenames.append(item.getName()).append(",");

					InputStream input = item.getInputStream();

					if (input.available() == 0) {
						System.out.println("File content expired, please upload it again.");
					}
					else {
						final CellProcessor[] allProcessors = null;
						ICsvListReader listReader = null;

						try {
							String charset = com.intumit.solr.util.WiSeUtils.autoDetectCharset(input);
							System.out.println("Detected charset:" + charset);
							input = item.getInputStream();

							listReader = new CsvListReader(new InputStreamReader(input, charset),
									CsvPreference.EXCEL_PREFERENCE);

							String[] headers = null;

							List<String> cells = null;
							AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());
							try {
								if (dryRun) {
									isInAudit = checkIsInAudit(t, new CsvListReader(
											new InputStreamReader(item.getInputStream(), charset), CsvPreference.EXCEL_PREFERENCE));
								}
								while ((cells = listReader.read()) != null) {
									String keyword = cells.get(0).toLowerCase().replace("　", " ").trim();
									String purpose = cells.get(1) == null ? "" : cells.get(1).toUpperCase().replace("　", " ").trim();
									boolean enable = cells.size() > 2 ? Boolean.parseBoolean(StringUtils.defaultString(cells.get(2), "true").replaceAll("1", "true")) : true;
									String category = cells.size() > 3 ? cells.get(3) : "";
									boolean isNotPurpose_each = false;
									for(String p:purpose.split(",")) {
										if(DictionaryDatabase.Purpose.getPurpose(p) == null) {
											isNotPurpose = true;
											isNotPurpose_each = true;
										}
									}
									
									boolean enableQaScopeRestriction = cells.size() > 4 ? Boolean.parseBoolean(cells.get(4)) : Boolean.FALSE;
									DictionaryDatabase origDic = DictionaryDatabase.get(t.getId(), keyword);
									if (StringUtils.trimToNull(keyword) != null) {
										if (origDic == null) {
											if (!dryRun && !isInAudit && !isNotPurpose) {
												try {
													DictionaryDatabase word = DictionaryDatabase.save(t.getId(), keyword,	purpose, enable, category, enableQaScopeRestriction);
													DictionaryVersionService.getInstance().saveByUpload(t.getId(), user, AuditAction.ADD, word);
													log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
												}catch (Exception e) {
													log.setStatusMessage(OperationLogEntity.Status.FAILED);
													log.appendToMoreDetails(e.toString());
												}
											}
											String addDicWords = MessageUtil.getMessage(locale, "dictionary.add");
											if (DictionaryVersionService.getInstance().keywordInAudit(t.getId(), keyword)) {
												out.println("<small><span style='color:red;'>"  + MessageUtil.getMessage(locale, "global.audit.status.audit") + "</span></small>");
											} else if (isNotPurpose_each) {
												out.println("<small><span style='color:red;'>"  + MessageUtil.getMessage(locale, "global.usage") + "　</span></small>");
											} else {
												out.println("<small>　　　</small>");
											}
											out.println(addDicWords + "「" + keyword + "」 / " + purpose + " / " + (enable ? MessageUtil.getMessage(locale, "dictionary.enable") : MessageUtil.getMessage(locale, "dictionary.disable")) + " / " + category);
											out.println("<BR>");
											addCount++;
										}
										else {
											if (!dryRun && !isInAudit && !isNotPurpose) {
												try {
													DictionaryDatabase word = DictionaryDatabase.update(t.getId(), Long.parseLong(origDic.getId().toString()), keyword, purpose, enable, category, enableQaScopeRestriction);
													DictionaryVersionService.getInstance().saveByUpload(t.getId(), user, AuditAction.UPDATE, word);
													log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
												}catch (Exception e) {
													log.setStatusMessage(OperationLogEntity.Status.FAILED);
													log.appendToMoreDetails(e.toString());
												}
											}
											String updateDicWords = MessageUtil.getMessage(locale, "dictionary.update");
											if (DictionaryVersionService.getInstance().publicIdInAudit(t.getId(), origDic.getId()) || DictionaryVersionService.getInstance().keywordInAudit(t.getId(), keyword)) {
												out.println("<small><span style='color:red;'>"  + MessageUtil.getMessage(locale, "global.audit.status.audit") + "</span></small>");
											} else if (isNotPurpose_each) {
												out.println("<small><span style='color:red;'>"  + MessageUtil.getMessage(locale, "global.usage") + "　</span></small>");
											}  else {
												out.println("<small><span style='color:green;'>" + MessageUtil.getMessage(locale, "global.audit.in.force") + "</span></small>");
											}
											out.println(updateDicWords + "「" + keyword + "」 / " + purpose + " / " + (enable ? MessageUtil.getMessage(locale, "dictionary.enable") : MessageUtil.getMessage(locale, "dictionary.disable")) + " / " + category);
											out.println("<BR>");
											updateCount++;
										}

									}
								}

							}
							catch (Exception ex) {
								ex.printStackTrace();
							}

						}
						finally {
							if (listReader != null) {
								listReader.close();
							}
						}
					}
				}
			}

		}
		catch (Exception e) {
			e.printStackTrace();
		}

		out.println("<script>window.top.scroll(0,0);</script>");
		if (isInAudit || isNotPurpose) {
			if(isInAudit)
				out.println("<h2>" + MessageUtil.getMessage(locale, "import.audit.alert") + "</h2>");
			if(isNotPurpose)
				out.println("<h2>" + MessageUtil.getMessage(locale, "import.usage.alert") + "</h2>");
			if (!dryRun) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.update();
			}
			return;
		}
		if (dryRun) {
			out.println("<h2>");
			String expectAddSynWords = MessageUtil.getMessage(locale, "expect.add.dic.words");
			String groupSynWords = MessageUtil.getMessage(locale, "group.dic.words");
			String upate = MessageUtil.getMessage(locale, "global.update");
			String sureImport = MessageUtil.getMessage(locale, "sure.import");
			String backSynManger = MessageUtil.getMessage(locale, "back.dic.manger");
			String filename = StringUtils.strip(filenames.toString(), ",");
			out.println(expectAddSynWords + addCount + groupSynWords+"，"+upate + updateCount + groupSynWords+"<br>");
			out.println("<a href='" + req.getContextPath() + "/wiseadm/dicUploadFile?dryRun=false&action=import&filenames="+filename+"&addCount="+addCount+"&updateCount="+updateCount+"'>"+sureImport+"</a>");
			out.println("&nbsp;<a href='" + req.getContextPath() + "/wiseadm/dict'>"+backSynManger+"</a>");
			out.println("</h2>");
		}
		else {
			String totalAdd = MessageUtil.getMessage(locale, "total.add");
			String groupSynWords = MessageUtil.getMessage(locale, "group.dic.words");
			String upate = MessageUtil.getMessage(locale, "global.update");
			String backSynManger = MessageUtil.getMessage(locale, "back.dic.manger");
			out.println("<h2>");
			out.println(totalAdd + addCount + groupSynWords+"，"+upate + updateCount + groupSynWords+"<br>");
			out.println("<a href='" + req.getContextPath() + "/wiseadm/dict'>"+backSynManger+"</a>");
			out.println("</h2>");
			
			log.update();
		}
	}

	protected boolean checkIsInAudit(Tenant t, ICsvListReader listReader) throws IOException {
		try {
			List<String> cells = null;
			while ((cells = listReader.read()) != null) {
				String keyword = cells.get(0);
				DictionaryDatabase origDic = DictionaryDatabase.get(t.getId(), keyword);
				if ((origDic != null && DictionaryVersionService.getInstance().publicIdInAudit(t.getId(), origDic.getId()))
						|| DictionaryVersionService.getInstance().keywordInAudit(t.getId(), keyword)) {
					return true;
				}
			}
		} finally {
			if (listReader != null) {
				listReader.close();
			}
		}
		return false;
	}
}

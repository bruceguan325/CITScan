package com.intumit.solr.robot.entity;

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
import com.intumit.solr.admin.AdminGroup;
import com.intumit.solr.admin.AdminGroupFacade;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.tenant.Tenant;
import com.intumit.syslog.OperationLogEntity;

public class QAEntityUploadFileServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		Integer opLogId = (Integer) req.getAttribute("opLogId");
		OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
		res.setContentType("text/html; charset=UTF8");
		res.setCharacterEncoding("UTF-8");
		PrintWriter out = res.getWriter();
		if ((AdminGroupFacade.getInstance().getFromSession(req.getSession()).getSystemAdminCURD() & AdminGroup.O4) == 0) {
			out.println("Permission Denied");
			return ; // 沒有批次匯入權限
		}
		boolean isMultipart = ServletFileUpload.isMultipartContent(req);
		Locale locale = req.getLocale();
		int addCount = 0, updateCount = 0, invalidCount = 0;
		StringBuilder filenames = new StringBuilder();
		boolean dryRun = Boolean.parseBoolean(StringUtils.defaultString(req.getParameter("dryRun"), "true"));
		
		boolean isInAudit = false;
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
				items.add((FileItem) req.getSession().getAttribute("QAEntityImportCsvTempFile"));
			}

			for (FileItem item : items) {
				if (item.isFormField()) {
					String name = item.getFieldName();
					String value = item.getString();
					System.out.println("name:" + name + " value:" + value);
				}
				else {
					req.getSession().setAttribute("QAEntityImportCsvTempFile", item);
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
							try {
								if (dryRun) {
									isInAudit = checkIsInAudit(t, new CsvListReader(
											new InputStreamReader(item.getInputStream(), charset), CsvPreference.EXCEL_PREFERENCE));
								}
								while ((cells = listReader.read()) != null) {
									String category = cells.get(0).replace("　", " ").trim();
									String code = cells.get(1).replace("　", " ").trim();
									String name = cells.get(2);
									String subEntities = cells.get(3);
									String entityType = cells.get(4);
									String values = cells.get(5);
									boolean enabled = cells.size() > 6 ? Boolean.parseBoolean(StringUtils
											.defaultString(cells.get(6), "true").replaceAll("1", "true")) : true;
									boolean fromIndex = cells.size() > 7 ? Boolean.parseBoolean(StringUtils.defaultString(cells.get(7), "false").replaceAll("1", "true")) : false;

//									boolean isValid = QAEntity.isValidCodeOrCategory(category) && QAEntity.isValidCodeOrCategory(code);
									
//									if (isValid) {
										QAEntity entity = QAEntity.get(t.getId(), code);
										AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());
    									if (StringUtils.trimToNull(values) != null) {
    										if (entity == null) {
    											if (!dryRun && !isInAudit) {
    												try {
    													QAEntity word = QAEntity.save(t.getId(), category, code, name, subEntities, QAEntityType.valueOf(entityType), values, fromIndex, enabled);
    													EntityVersionService.getInstance().saveByUpload(t.getId(), user, AuditAction.ADD, word);
	    												log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
    												}catch (Exception e) {
    													log.setStatusMessage(OperationLogEntity.Status.FAILED);
    													log.appendToMoreDetails(e.toString());
    												}
    											}
    											String addSynWords = MessageUtil.getMessage(locale, "global.add");
    											//out.println("新增同義詞「" + keyword + "」：" + synonymKeywords);
    											if (EntityVersionService.getInstance().CodeInAudit(t.getId(), code)) {
    												out.println("<small><span style='color:red;'>"  + MessageUtil.getMessage(locale, "global.audit.status.audit") + "</span></small>");
    											} else {
    												out.println("<small>　　　</small>");
    											}
												out.println(addSynWords+"「" + category + "：" + code + "(" + name + ")」：" + values);
    											out.println("<BR>");
    											addCount++;
    										}
    										else {
    											if (!dryRun && !isInAudit) {
    												try {
	    												entity.setEnabled(enabled);
	    												entity.setName(name);
	    												entity.setEntityValues(values);
	    												entity.setSubEntities(subEntities);
	    												entity.setEntityType(QAEntityType.valueOf(entityType));
	    												QAEntity.saveOrUpdate(entity);
	    												EntityVersionService.getInstance().saveByUpload(t.getId(), user, AuditAction.UPDATE, entity);
	    												log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
    												}catch (Exception e) {
    													log.setStatusMessage(OperationLogEntity.Status.FAILED);
    													log.appendToMoreDetails(e.toString());
    												}
    											}
    											String updateSynWords = MessageUtil.getMessage(locale, "global.update");
    											//out.println("更新同義詞「" + keyword + "」：" + synonymKeywords);
    											if (EntityVersionService.getInstance().publicIdInAudit(t.getId(), entity.getId()) || EntityVersionService.getInstance().CodeInAudit(t.getId(), code)) {
    												out.println("<small><span style='color:red;'>"  + MessageUtil.getMessage(locale, "global.audit.status.audit") + "</span></small>");
    											} else {
    												out.println("<small><span style='color:green;'>" + MessageUtil.getMessage(locale, "global.audit.in.force") + "</span></small>");
    											}
    											out.println(updateSynWords+"「" + category + "：" + code + "(" + name + ")」：" + values);
    											out.println("<BR>");
    											updateCount++;
    										}
    
    									}
//									}
//									else {
//										out.println("Invalid category or code...「" + category + "：" + code + "(" + name + ")」：" + values);
//										out.println("<BR>");
//										invalidCount++;
//									}
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
		if (isInAudit) {
			out.println("<h2>" + MessageUtil.getMessage(locale, "import.audit.alert") + "</h2>");
			if (!dryRun) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.update();
			}
			return;
		}
		
		String dataName = MessageUtil.getMessage(locale, "global.entity");
		String backTo = MessageUtil.getMessage(locale, "entity.management");
		
		String update = MessageUtil.getMessage(locale, "global.update");
		String expectAddSynWords = MessageUtil.getMessage(locale, "expect.add");
		String totalAdd = MessageUtil.getMessage(locale, "total.add");
		
		if (dryRun) {
			out.println("<h2>");
			String sureImport = MessageUtil.getMessage(locale, "sure.import");
			String filename = StringUtils.strip(filenames.toString(), ",");
			//out.println("預計新增" + addCount + "組同義詞，更新" + updateCount + "組同義詞<br>");
			out.println(String.format("%s%d%s，%s%d%s<br>", expectAddSynWords, addCount, dataName, update, updateCount, dataName));
			out.println("<a href='" + req.getContextPath() + "/wiseadm/qaEntityUploadFile?dryRun=false&action=import&filename="+filename+"&addCount="+addCount+"&updateCount="+updateCount+"'>"+sureImport+"</a>");
			out.println("&nbsp;<a href='" + req.getContextPath() + "/wiseadm/entity'>"+backTo+"</a>");
			out.println("</h2>");
		}
		else {
			out.println("<h2>");
			//out.println("共新增" + addCount + "組同義詞，更新" + updateCount + "組同義詞<br>");
			out.println(String.format("%s%d%s，%s%d%s，%s%d%s<br>", totalAdd, addCount, dataName, update, updateCount, dataName, "Invalid", invalidCount, dataName));
			out.println("<a href='" + req.getContextPath() + "/wiseadm/entity'>"+backTo+"</a>");
			out.println("</h2>");
			
			log.update();
		}
	}

	protected boolean checkIsInAudit(Tenant t, ICsvListReader listReader) throws IOException {
		try {
			List<String> cells = null;
			while ((cells = listReader.read()) != null) {
				String category = StringUtils.trim(cells.get(0));
				String code = StringUtils.trim(cells.get(1));
				QAEntity entity = QAEntity.get(t.getId(), code);
				if ((entity != null && EntityVersionService.getInstance().publicIdInAudit(t.getId(), entity.getId()))
						|| EntityVersionService.getInstance().CodeInAudit(t.getId(), code)) {
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

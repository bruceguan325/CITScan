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
import org.apache.log4j.Logger;
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
import com.intumit.solr.robot.AuditStatus;
import com.intumit.solr.synonymKeywords.SynonymKeyword;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.synonymKeywords.SynonymKeywordVersion;
import com.intumit.solr.synonymKeywords.SynonymKeywordVersionDAO;
import com.intumit.solr.synonymKeywords.SynonymVersionService;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.XssHttpServletRequestWrapper;
import com.intumit.syslog.OperationLogEntity;

public class SynUploadFileServlet extends HttpServlet {
	static Logger infoLog = Logger.getLogger(SynUploadFileServlet.class.getName());
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(req);
		Integer opLogId = (Integer) xssReq.getFakeAttribute(OperationLogEntity.KEY_OPERATION_LOG_ID);
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
		int addCount = 0, updateCount = 0;
		StringBuilder filenames = new StringBuilder();
		boolean dryRun = Boolean.parseBoolean(StringUtils.defaultString(req.getParameter("dryRun"), "true"));

		boolean isInAudit = false;
		boolean synInTable = false;
		boolean isRepeat = false;
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
				items.add((FileItem) req.getSession().getAttribute("SynImportCsvTempFile"));
			}

			for (FileItem item : items) {
				if (item.isFormField()) {
					String name = item.getFieldName();
					String value = item.getString();
					System.out.println("name:" + name + " value:" + value);
				}
				else {
					req.getSession().setAttribute("SynImportCsvTempFile", item);
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
									synInTable = checkSynonymkeywordInTable(t, new CsvListReader(
											new InputStreamReader(item.getInputStream(), charset), CsvPreference.EXCEL_PREFERENCE));
								}
								while ((cells = listReader.read()) != null) {
									String keyword = cells.get(0).toLowerCase().replace("　", " ").trim();
									String synonymKeywords = cells.get(1).toLowerCase().replace("　", " ").trim();
									boolean bidirection = cells.size() > 2 ? Boolean.parseBoolean(StringUtils
											.defaultString(cells.get(2), "true").replaceAll("1", "true")) : true;
									String nature = cells.size() > 3 ? cells.get(3) : null;

									SynonymKeyword origSk = SynonymKeywordFacade.getInstance().get(t.getId(), keyword);
									SynonymKeyword nowSynEntity = SynonymKeywordFacade.getInstance().findSynonymKeywordByKeyword(t.getId(), keyword);
									String nowSynkeyword = nowSynEntity == null ? "" : nowSynEntity.getSynonymKeyword();

									if (StringUtils.trimToNull(synonymKeywords) != null) {
										if (origSk == null) {
											if (!dryRun && !isInAudit && !synInTable) {
												try {
													SynonymKeyword word = SynonymKeywordFacade.getInstance().save(t.getId(), keyword,
															synonymKeywords, bidirection, nature);
													SynonymVersionService.getInstance().saveByUpload(t.getId(), user, AuditAction.ADD, word);
													log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
												}catch (Exception e) {
													log.setStatusMessage(OperationLogEntity.Status.FAILED);
													log.appendToMoreDetails(e.toString());
												}
												out.println("<small>　　　</small>");
											} else {
												//out.println("新增同義詞「" + keyword + "」：" + synonymKeywords);
	                                            if(SynonymVersionService.getInstance().synonymKeywordRepeat(synonymKeywords)) {
	                                            	out.println("<small><span style='color:red;'>"  + MessageUtil.getMessage(locale, "global.audit.repeat") + "</span></small>");
	                                            	isRepeat = true;
	                                            } else if (SynonymVersionService.getInstance().keywordInAudit(t.getId(), keyword)
	                                             || SynonymKeywordVersionDAO.getInstance().findBySynonymKeywordAndStatus(t.getId(), synonymKeywords, AuditStatus.AUDIT, nowSynkeyword) != null) {
	                                                out.println("<small><span style='color:red;'>"  + MessageUtil.getMessage(locale, "global.audit.status.audit") + "</span></small>");
	                                            } else if (SynonymKeywordFacade.getInstance().findBySynonymKeyword(t.getId(), synonymKeywords, nowSynkeyword) != null ) {
	                                                out.println("<small><span style='color:red;'>"  + MessageUtil.getMessage(locale, "global.audit.repeat") + "</span></small>");
	                                            } else {
	                                                out.println("<small>　　　</small>");
	                                            }
											}
											String addSynWords = MessageUtil.getMessage(locale, "add.syn.words");
											out.println(addSynWords+"「" + keyword + "」：" + synonymKeywords);
											out.println("<BR>");
											addCount++;
										}
										else {
											if (!dryRun && !isInAudit && !synInTable) {
												try {
													SynonymKeyword word = SynonymKeywordFacade.getInstance().update(Long.parseLong(origSk.getId().toString()),
															keyword, synonymKeywords, bidirection, nature);
													SynonymVersionService.getInstance().saveByUpload(t.getId(), user, AuditAction.UPDATE, word);
													log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
												}catch (Exception e) {
													log.setStatusMessage(OperationLogEntity.Status.FAILED);
													log.appendToMoreDetails(e.toString());
												}
												out.println("<small><span style='color:green;'>" + MessageUtil.getMessage(locale, "global.audit.in.force") + "</span></small>");
											} else {
												//out.println("更新同義詞「" + keyword + "」：" + synonymKeywords);
												if(SynonymVersionService.getInstance().synonymKeywordRepeat(synonymKeywords)) {
	                                            	out.println("<small><span style='color:red;'>"  + MessageUtil.getMessage(locale, "global.audit.repeat") + "</span></small>");
	                                            	isRepeat = true;
	                                            } else if (SynonymVersionService.getInstance().publicIdInAudit(t.getId(), origSk.getId()) || SynonymVersionService.getInstance().keywordInAudit(t.getId(), keyword)
	                                             || SynonymKeywordVersionDAO.getInstance().findBySynonymKeywordAndStatus(t.getId(), synonymKeywords, AuditStatus.AUDIT, nowSynkeyword) != null ) {
	                                                out.println("<small><span style='color:red;'>"  + MessageUtil.getMessage(locale, "global.audit.status.audit") + "</span></small>");
	                                            } else if (SynonymKeywordFacade.getInstance().findBySynonymKeyword(t.getId(), synonymKeywords, nowSynkeyword) != null ) {
	                                                out.println("<small><span style='color:red;'>"  + MessageUtil.getMessage(locale, "global.audit.repeat") + "</span></small>");
	                                            } else {
	                                                out.println("<small><span style='color:green;'>" + MessageUtil.getMessage(locale, "global.audit.in.force") + "</span></small>");
	                                            }
											}
											String updateSynWords = MessageUtil.getMessage(locale, "update.syn.words");
											out.println(updateSynWords+"「" + keyword + "」：" + synonymKeywords);
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
		if (isInAudit || synInTable || isRepeat) {
			out.println("<h2>" + MessageUtil.getMessage(locale, "import.audit.alert") + "</h2>");
			if (!dryRun) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.update();
			}
			return;
		}
		if (dryRun) {
			out.println("<h2>");
			String expectAddSynWords = MessageUtil.getMessage(locale, "expect.add.syn.words");
			String groupSynWords = MessageUtil.getMessage(locale, "group.syn.words");
			String upate = MessageUtil.getMessage(locale, "global.update");
			String sureImport = MessageUtil.getMessage(locale, "sure.import");
			String backSynManger = MessageUtil.getMessage(locale, "back.syn.manger");
			String filename = StringUtils.strip(filenames.toString(), ",");
			//out.println("預計新增" + addCount + "組同義詞，更新" + updateCount + "組同義詞<br>");
			out.println(expectAddSynWords + addCount + groupSynWords+"，"+upate + updateCount + groupSynWords+"<br>");
			//out.println("<a href='" + req.getContextPath() + "/wiseadm/synUploadFile?dryRun=false'>確定匯入</a>");
			out.println("<a href='" + req.getContextPath() + "/wiseadm/synUploadFile?dryRun=false&action=import&filename="+filename+"&addCount="+addCount+"&updateCount="+updateCount+"'>"+sureImport+"</a>");
			//out.println("&nbsp;<a href='" + req.getContextPath() + "/wiseadm/syn'>回同義詞管理</a>");
			out.println("&nbsp;<a href='" + req.getContextPath() + "/wiseadm/syn'>"+backSynManger+"</a>");
			out.println("</h2>");
		}
		else {
			String totalAdd = MessageUtil.getMessage(locale, "total.add");
			String groupSynWords = MessageUtil.getMessage(locale, "group.syn.words");
			String upate = MessageUtil.getMessage(locale, "global.update");
			String backSynManger = MessageUtil.getMessage(locale, "back.syn.manger");
			out.println("<h2>");
			//out.println("共新增" + addCount + "組同義詞，更新" + updateCount + "組同義詞<br>");
			out.println(totalAdd + addCount + groupSynWords+"，"+upate + updateCount + groupSynWords+"<br>");
			//out.println("<a href='" + req.getContextPath() + "/wiseadm/syn'>回同義詞管理</a>");
			out.println("<a href='" + req.getContextPath() + "/wiseadm/syn'>"+backSynManger+"</a>");
			out.println("</h2>");
			
			log.update();
		}
	}

	protected boolean checkIsInAudit(Tenant t, ICsvListReader listReader) throws IOException {
		try {
			List<String> cells = null;
			while ((cells = listReader.read()) != null) {
				String keyword = cells.get(0);
				String synonymKeyword = cells.get(1);
				SynonymKeyword origSk = SynonymKeywordFacade.getInstance().get(t.getId(), keyword);
				// 判斷關鍵字是否審核中
				if ((origSk != null && SynonymVersionService.getInstance().publicIdInAudit(t.getId(), origSk.getId()))
						|| SynonymVersionService.getInstance().keywordInAudit(t.getId(), keyword)) {
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
	
	protected boolean checkSynonymkeywordInTable(Tenant t, ICsvListReader listReader) throws IOException {
		try {
			List<String> cells = null;
			while ((cells = listReader.read()) != null) {
				String keyword = cells.get(0);
				String synonymKeyword = cells.get(1);			
				SynonymKeyword nowSynEntity = SynonymKeywordFacade.getInstance().findSynonymKeywordByKeyword(t.getId(), keyword);
				String nowSynkeyword = nowSynEntity == null ? "" : nowSynEntity.getSynonymKeyword();
				List<SynonymKeyword> synonymInword = SynonymKeywordFacade.getInstance().findBySynonymKeyword(t.getId(), synonymKeyword, nowSynkeyword);
				List<SynonymKeywordVersion> synonymInAudit = SynonymKeywordVersionDAO.getInstance().findBySynonymKeywordAndStatus(t.getId(), synonymKeyword, AuditStatus.AUDIT, nowSynkeyword);
				// 判斷同義詞是否審核中或已存在 (若keyword已存在，原有同義詞不檢查，需過濾)
				if(synonymInword != null || synonymInAudit != null) {
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

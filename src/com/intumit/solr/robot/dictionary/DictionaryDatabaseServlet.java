package com.intumit.solr.robot.dictionary;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jmesa.facade.TableFacade;
import org.jmesa.facade.TableFacadeImpl;
import org.jmesa.limit.Limit;
import org.jmesa.view.editor.BasicCellEditor;
import org.jmesa.view.editor.CellEditor;
import org.jmesa.view.html.HtmlBuilder;
import org.jmesa.view.html.component.HtmlColumn;
import org.jmesa.view.html.component.HtmlRow;
import org.jmesa.view.html.component.HtmlTable;
import org.jmesa.view.html.toolbar.HtmlToolbar;

import com.google.gson.Gson;
import com.intumit.message.MessageUtil;
import com.intumit.solr.robot.AuditStatus;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.XssHttpServletRequestWrapper;
import com.intumit.syslog.OperationLogEntity;

public class DictionaryDatabaseServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	static Logger infoLog = Logger.getLogger(DictionaryDatabaseServlet.class.getName());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String id = req.getParameter("id");
		if (id != null) {
			DictionaryDatabase dic = DictionaryDatabase.get(Long.parseLong(id));
			Gson gson = new Gson();
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			try (PrintWriter writer = resp.getWriter()) {
				writer.write(gson.toJson(dic));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			doPost(req, resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(req, false, false);
		XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(req);
		Tenant t = Tenant.getFromSession(req.getSession());
		List<DictionaryDatabase> results = null;
		String action = req.getParameter("action");
		Integer opLogId = (Integer) req.getAttribute("opLogId");
		OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
		//新增
		if (log != null && action != null && action.equals("save")) {
			String keyword = (String)req.getParameter("keyword");
			String synonymKeyword = (String)req.getParameter("purposes");
			boolean enabled = (((String)req.getParameter("enabled")).equals("1")?true:false);
			String category = (String)req.getParameter("category");
			boolean enableQaScopeRestriction = "1".equals(req.getParameter("enableQaScopeRestriction"))?true:false;
			try {
				DictionaryDatabase.save(t.getId(), keyword, synonymKeyword, enabled, category, enableQaScopeRestriction);
				log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
			} catch (Exception e) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.appendToMoreDetails(e.toString());
				infoLog.error(e);
			}
			log.update();
		}

		//修改
		if (log != null && action != null && action.equals("update")) {
			String id = (String)xssReq.getParameter("hiddenId");
			String keyword = (String)xssReq.getParameter("keyword");
			String synonymKeyword = (String)xssReq.getParameter("purposes");
			boolean enabled = (((String)req.getParameter("enabled")).equals("1")?true:false);
			String category = (String)req.getParameter("category");
			boolean enableQaScopeRestriction = "1".equals(req.getParameter("enableQaScopeRestriction"))?true:false;

			try {
				DictionaryDatabase dd = DictionaryDatabase.get(new Integer(id));
				if (req.getParameter("enableQaScopeRestriction") == null) { // 更新時，若沒傳遞 true / false，沿用舊的
					enableQaScopeRestriction = dd.getEnableQaScopeRestriction(); 
				}
				DictionaryDatabase.update(t.getId(), Long.parseLong(id), keyword, synonymKeyword, enabled, category, enableQaScopeRestriction);
			} catch (Exception e) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.appendToMoreDetails(e.toString());
				infoLog.error(e);
			}
			log.update();
		}

		//刪除
		if (log != null && action != null && action.equals("delete")) {
			String id = (String)xssReq.getParameter("id");
			try {
				DictionaryDatabase.delete(t.getId(),Long.parseLong(id));
			} catch (Exception e) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.appendToMoreDetails(e.toString());
				infoLog.error(e);
			}
			log.update();
		}

		//查詢
		if (req.getRequestURI().contains("dict/search")) {
			String searchKeyword = (String)req.getParameter("searchKeyword");
			try {
				AuditStatus status = null;
				if (searchKeyword.equals(MessageUtil.getMessage(locale, "global.audit.status.audit"))) {
					status = AuditStatus.AUDIT;
				} else if (searchKeyword.equals(MessageUtil.getMessage(locale, "global.audit.in.force"))) {
					status = AuditStatus.HISTORY;
				}
				
				String pur = null;
				if (searchKeyword.equals(MessageUtil.getMessage(locale, "knowledge.point"))) {
					pur = "KNOWLEDGE_POINT";
				} else if (searchKeyword.equals(MessageUtil.getMessage(locale, "search.words"))) {
					pur = "SEARCH";
				} else if (searchKeyword.equals(MessageUtil.getMessage(locale, "avoid.search"))) {
					pur = "DONT_SEARCH";
				} else if (searchKeyword.equals(MessageUtil.getMessage(locale, "blacklist.keyword"))) {
					pur = "BLACKLIST";
				} else if (searchKeyword.equals(MessageUtil.getMessage(locale, "marketing.keyword"))) {
					pur = "MARKETING";
				}
				
//				results = DictionaryDatabase.search(t.getId(), searchKeyword);
				results = DictionaryDatabase.fullSearchBySQL(t.getId(), searchKeyword, status, pur);
				req.setAttribute("searchKeyword", searchKeyword);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (req.getRequestURI().contains("dict/list")) {
			String searchKeyword = (String)req.getParameter("searchKeyword");
			try {
				results = DictionaryDatabase.search(t.getId(), searchKeyword);
				req.setAttribute("searchKeyword", searchKeyword);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			results = DictionaryDatabase.listByTenantId(t.getId());
		}

		DictionaryVersionService service = DictionaryVersionService.getInstance();
//		for (DictionaryDatabase res : (List<DictionaryDatabase>) results) {
//			res.setInAudit(service.publicIdInAudit(t.getId(), res.getId()));
//			res.setPassDate(service.getLastPassDate(t.getId(), res.getId()));
//		}

		Map<Long, DictionaryDatabaseVersionStatusDto> statusResult = service.listStatusOfIds(t.getId(), results.stream().map(dic -> dic.getId()).collect(Collectors.toList()));
		
		results.forEach(dic -> {
		    DictionaryDatabaseVersionStatusDto dto = statusResult.getOrDefault(dic.getId(), new DictionaryDatabaseVersionStatusDto());
		    dic.setInAudit(dto.isAudit());    
		    dic.setPassDate(dto.getLastPassTime());    
		});

        TableFacade tableFacade = new TableFacadeImpl("tableId", req);

        tableFacade.setExportTypes(resp);
        tableFacade.setStateAttr("restore");
        tableFacade.setMaxRows(100);

        Limit limit = tableFacade.getLimit();
        if (!limit.isComplete()) {
        int totalRows = results.size();
        	tableFacade.setTotalRows(totalRows);
        }
        int rowEnd = limit.getRowSelect().getRowEnd();
        int rowStart = limit.getRowSelect().getRowStart();
        results = results.subList(rowStart, rowEnd);
        tableFacade.setItems(results);
//        if (limit.isExported()) {
//            export(tableFacade);
//            return null;
//        } else {
            String html = html(tableFacade, req);
            req.setAttribute("myhtml", html);
//        }

		RequestDispatcher requestdispatch=this.getServletConfig().getServletContext().getRequestDispatcher("/wiseadm/dictionary.jsp");
		requestdispatch.forward(req, resp);

	}

	 private String html(TableFacade tableFacade, final HttpServletRequest req) {
			Tenant t = Tenant.getFromSession(req.getSession());

	        if (t.getEnableRestrictToKnowledgePoint()) {
	        	tableFacade.setColumnProperties("id", "keyword", "purposes", "category", "enableQaScopeRestriction", "enabled", "inAudit", "passDate", "action");
	        }
	        else {
	        	tableFacade.setColumnProperties("id", "keyword", "purposes", "category", "enabled", "inAudit", "passDate", "action");
	        }
	        tableFacade.autoFilterAndSort(true);
	        HtmlToolbar toolbar = new HtmlToolbar();
	        toolbar.enablePageNumbers(true);
	        tableFacade.setToolbar(toolbar);
	        final Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(req, false, false);
	        String vocabularyList = MessageUtil.getMessage(locale, "vocabulary.list");
	        HtmlTable table = (HtmlTable) tableFacade.getTable();
	        //table.setCaption("詞庫列表");
	        table.setCaption(vocabularyList);
	        table.getTableRenderer().setStyle("width: 1000px;");

	        HtmlRow row = table.getRow();

	        HtmlColumn id = row.getColumn("id");
//	        id.getFilterRenderer().setFilterEditor(new DroplistFilterEditor());
	        id.setTitle("ID");

	        HtmlColumn keyword = row.getColumn("keyword");
	        String globalKeyword = MessageUtil.getMessage(locale, "global.keyword");
	        //keyword.setTitle("關鍵詞");
	        keyword.setTitle(globalKeyword);

	        HtmlColumn purposes = row.getColumn("purposes");
	        String usage = MessageUtil.getMessage(locale, "global.usage");
	        //purposes.setTitle("用途");
	        purposes.setTitle(usage);
	        purposes.getCellRenderer().setCellEditor(new CellEditor() {
	            public Object getValue(Object item, String property, int rowcount) {
	                Object value = new BasicCellEditor().getValue(item, property, rowcount);
	                String pstr = (String)value;
	                String[] parr = StringUtils.split(pstr, ",");
	                StringBuilder sb = new StringBuilder();
	                for (String p: parr) {
	                	DictionaryDatabase.Purpose pp = DictionaryDatabase.Purpose.valueOf(p);
	                	if (sb.length() > 0) {
	                		sb.append(",");
	                	}
	                	sb.append(MessageUtil.getMessage(locale, pp.getDesc()));
	                }
	                return sb.toString();
	            }
	        });

	        HtmlColumn reverse = row.getColumn("enabled");
	        String enable = MessageUtil.getMessage(locale, "is.enabled");
	        //reverse.setTitle("是否啟用");
	        reverse.setTitle(enable);
	        reverse.getCellRenderer().setCellEditor(new CellEditor() {
	            public Object getValue(Object item, String property, int rowcount) {
	                Object value = new BasicCellEditor().getValue(item, property,
	                        rowcount);
	                HtmlBuilder html = new HtmlBuilder();
	                if ((Boolean)value)
	                	html.img().src(req.getContextPath() + "/img/button_ok.gif").close();
	                else
	                	html.img().src(req.getContextPath() + "/img/button_no.gif").close();
	                return html.toString();
	            }
	        });

	        HtmlColumn action = row.getColumn("action");
	        action.setTitle(MessageUtil.getMessage(locale, "operation"));

	        int[] isAudit = new int[1]; // 判斷操作按鈕是否顯示
	        HtmlColumn inAudit = row.getColumn("inAudit");
	        inAudit.setTitle(MessageUtil.getMessage(locale, "global.status"));
	        inAudit.getCellRenderer().setCellEditor(new CellEditor() {
	            public Object getValue(Object item, String property, int rowcount) {
	                Object value = new BasicCellEditor().getValue(item, property,
	                        rowcount);
	                if ((Boolean)value) {
	                	isAudit[0] = 1;
		                return MessageUtil.getMessage(locale, "global.audit.status.audit");
	                }
	                else {
	                	isAudit[0] = 0;
		                return MessageUtil.getMessage(locale, "global.audit.in.force");
	                }
	            }
	        });
	        HtmlColumn passDate = row.getColumn("passDate");
	        passDate.setTitle(MessageUtil.getMessage(locale, "global.audit.date"));

	        HtmlColumn category = row.getColumn("category");
	        category.setTitle(MessageUtil.getMessage(locale, "category"));

	        if (t.getEnableRestrictToKnowledgePoint()) {
		        HtmlColumn enableQaScopeRestriction = row.getColumn("enableQaScopeRestriction");
		        String enableQaScopeRestrictionDesc = MessageUtil.getMessage(locale, "is.restrictToKnowledgePoint.enable");
		        enableQaScopeRestriction.setTitle(enableQaScopeRestrictionDesc);
		        enableQaScopeRestriction.getCellRenderer().setCellEditor(new CellEditor() {
		            public Object getValue(Object item, String property, int rowcount) {
		                Object value = new BasicCellEditor().getValue(item, property, rowcount);
		                HtmlBuilder html = new HtmlBuilder();
		                if ((Boolean)value)
		                	html.img().src(req.getContextPath() + "/img/button_ok.gif").close();
		                else
		                	html.img().src(req.getContextPath() + "/img/button_no.gif").close();
		                return html.toString();
		            }
		        });
	        }

	        // Using an anonymous class to implement a custom editor.
//	        firstName.getCellRenderer().setCellEditor(new CellEditor() {
//	            public Object getValue(Object item, String property, int rowcount) {
//	                Object value = new BasicCellEditor().getValue(item, property,
//	                        rowcount);
//	                HtmlBuilder html = new HtmlBuilder();
//	                html.a().href().quote().append("http://")
//	                        .quote().close();
//	                html.append(value);
//	                html.aEnd();
//	                return html.toString();
//	            }
//	        });

	        // Using an anonymous class to implement a custom editor.
	        action.getCellRenderer().setCellEditor(new CellEditor() {
	            public Object getValue(Object item, String property, int rowcount) {
	                HtmlBuilder html = new HtmlBuilder();
	                
	                if(isAudit[0] != 1) {
		                DictionaryDatabase word = (DictionaryDatabase) item;
		                long id = word.getId();
		                String keyword = word.getKeyword();
		                String purposes = word.getPurposes();
		                boolean enabled = word.isEnabled();
		                String category = word.getCategory();
		                boolean enableQaScopeRestriction = word.getEnableQaScopeRestriction();
	
		                String js =" onclick='javascript:select(" + id + ",\"" + keyword+ "\",\"" + purposes + "\",\"" + enabled + "\",\"" + category + "\",\"" + enableQaScopeRestriction + "\") '";
						html.div().quote().close();
						html.a().append(js).href().quote().append("#").quote().close();
						String modify = MessageUtil.getMessage(locale, "modify");
						html.append(modify);
						html.aEnd();
						html.divEnd();
						String js2 = " onclick='return del(\"tableId\"," + id + ") '";
						html.div().quote().close();
						html.a().append(js2).close();
						String delete = MessageUtil.getMessage(locale, "delete");
						html.append(delete);
						html.aEnd();
						html.divEnd();
	                }
	                return html.toString();
	            }

	        });

	        return tableFacade.render();
	    }
}

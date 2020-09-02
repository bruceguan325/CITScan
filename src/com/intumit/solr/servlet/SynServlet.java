package com.intumit.solr.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

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
import com.intumit.solr.synonymKeywords.SynonymKeyword;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;
import com.intumit.solr.synonymKeywords.SynonymVersionService;
import com.intumit.solr.util.WiSeEnv;
import com.intumit.syslog.OperationLogEntity;

public class SynServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	static Logger infoLog = Logger.getLogger(SynServlet.class.getName());

	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        if (id != null) {
            SynonymKeyword synonymKeyword = SynonymKeywordFacade.getInstance().get(Long.parseLong(id));
            Gson gson = new Gson();
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            try (PrintWriter writer = resp.getWriter()) {
                writer.write(gson.toJson(synonymKeyword));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            doPost(req, resp);
        }
	}

	@Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(req, false, false);
		List<?> results = null;
		com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(req.getSession());
		String action = req.getParameter("action");
		Integer opLogId = (Integer) req.getAttribute("opLogId");
		OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
		//新增
		if (log != null && action != null && action.equals("save")) {
            String keyword = (String) req.getParameter("keyword");
            String synonymKeyword = (String) req.getParameter("synonymKeyword");
            boolean reverse = (((String) req.getParameter("reverse")).equals("1") ? true : false);
            String nature = StringUtils.trimToNull((String) req.getParameter("nature"));
			try {
				SynonymKeywordFacade.getInstance().save(t.getId(), keyword, synonymKeyword, reverse, nature);
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
            String id = (String) req.getParameter("hiddenId");
            String keyword = (String) req.getParameter("keyword");
            String synonymKeyword = (String) req.getParameter("synonymKeyword");
            boolean reverse = (((String) req.getParameter("reverse")).equals("1") ? true : false);
            String nature = StringUtils.trimToNull((String) req.getParameter("nature"));
            
            if (keyword == null || synonymKeyword == null) return; // 不允許空值
            
			try {
                SynonymKeywordFacade.getInstance().update(Long.parseLong(id), keyword, synonymKeyword, reverse, nature);
				log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
			} catch (Exception e) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.appendToMoreDetails(e.toString());
				infoLog.error(e);
			}
			log.update();
		} 
		
		//刪除
		if (log != null && action != null && action.equals("delete")) {
            String id = (String) req.getParameter("id");
			try {
                SynonymKeywordFacade.getInstance().delete(t.getId(),Long.parseLong(id));
				log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
			} catch (Exception e) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.appendToMoreDetails(e.toString());
				infoLog.error(e);
			}
			log.update();
		} 
		
		//查詢
		if (req.getRequestURI().contains("syn/search")) {
            String searchKeyword = (String) req.getParameter("searchKeyword");
			try {
				AuditStatus status = null;
				if (searchKeyword.equals(MessageUtil.getMessage(locale, "global.audit.status.audit"))) {
					status = AuditStatus.AUDIT;
				} else if (searchKeyword.equals(MessageUtil.getMessage(locale, "global.audit.in.force"))) {
					status = AuditStatus.HISTORY;
				}
					
//				results = SynonymKeywordFacade.getInstance().fullSearch(t.getId(), searchKeyword);
				results = SynonymKeywordFacade.getInstance().fullSearchBySQL(t.getId(), searchKeyword, status);
				req.setAttribute("searchKeyword", searchKeyword);
			} catch (Exception e) {
				e.printStackTrace();
			}
        } else if (req.getRequestURI().contains("syn/list")) {
            String searchKeyword = (String) req.getParameter("searchKeyword");
			try {
				results = SynonymKeywordFacade.getInstance().fullSearch(t.getId(), searchKeyword);
				req.setAttribute("searchKeyword", searchKeyword);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			results = SynonymKeywordFacade.getInstance().listAll(t.getId());
		}
		
		SynonymVersionService service = SynonymVersionService.getInstance();
		for (SynonymKeyword res : (List<SynonymKeyword>) results) {
			res.setInAudit(service.publicIdInAudit(t.getId(), res.getId()));
			res.setPassDate(service.getLastPassDate(t.getId(), res.getId()));
		}

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

        String html = html(tableFacade, req, locale);
        req.setAttribute("myhtml", html);

        RequestDispatcher requestdispatch = this.getServletConfig().getServletContext()
                .getRequestDispatcher(WiSeEnv.getAdminContextPath() + "/syn.jsp");
        requestdispatch.forward(req, resp);

    }

    private String html(TableFacade tableFacade, final HttpServletRequest req, final Locale locale) {
        tableFacade.setColumnProperties("id", "keyword", "nature", "synonymKeyword", "reverse", "inAudit", "passDate", "action");
        tableFacade.autoFilterAndSort(true);
        HtmlToolbar toolbar = new HtmlToolbar();
        toolbar.enablePageNumbers(true);
        tableFacade.setToolbar(toolbar);
        String synonymList = MessageUtil.getMessage(locale, "synonym.list");
        HtmlTable table = (HtmlTable) tableFacade.getTable();
        table.setCaption(synonymList);
        table.getTableRenderer().setStyle("width: 1200px;");

        HtmlRow row = table.getRow();

        HtmlColumn id = row.getColumn("id");
        id.setTitle("ID");
        
        HtmlColumn keyword = row.getColumn("keyword");
        String keyWord = MessageUtil.getMessage(locale, "global.keyword");
        keyword.setTitle(keyWord);

        HtmlColumn synonymKeyword = row.getColumn("synonymKeyword");
        String synonymsWord = MessageUtil.getMessage(locale, "global.synonyms");
        synonymKeyword.setTitle(synonymsWord);

        // Using an anonymous class to implement a custom editor.
        synonymKeyword.getCellRenderer().setCellEditor(new CellEditor() {
            public Object getValue(Object item, String property, int rowcount) {
                SynonymKeyword word = (SynonymKeyword) item;
                String synonymKeyword = StringUtils.strip(word.getSynonymKeywordForEditingOrReading(), ","); // 只顯示非自動產生的同義詞們
                return synonymKeyword;
            }

        });

        HtmlColumn reverse = row.getColumn("reverse");
        String towWayWord = MessageUtil.getMessage(locale, "synonym.two.way");
        reverse.setTitle(towWayWord);
        reverse.getCellRenderer().setCellEditor(new CellEditor() {
            public Object getValue(Object item, String property, int rowcount) {
                Object value = new BasicCellEditor().getValue(item, property, rowcount);
                HtmlBuilder html = new HtmlBuilder();
                if ((Boolean) value)
                    html.img().src(req.getContextPath() + "/img/button_ok.gif").close();
                else
                    html.img().src(req.getContextPath() + "/img/button_no.gif").close();
                return html.toString();
            }
        });

        HtmlColumn nature = row.getColumn("nature");
        String natureTitle = MessageUtil.getMessage(locale, "synonym.nature");
        nature.setTitle(natureTitle);
        
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

        HtmlColumn action = row.getColumn("action");
        String operation = MessageUtil.getMessage(locale, "operation");
        action.setTitle(operation);

        // Using an anonymous class to implement a custom editor.
        action.getCellRenderer().setCellEditor(new CellEditor() {
            public Object getValue(Object item, String property, int rowcount) {
                Object value = new BasicCellEditor().getValue(item, property, rowcount);
                HtmlBuilder html = new HtmlBuilder();
                
                if(isAudit[0] != 1) {
	                SynonymKeyword word = (SynonymKeyword) item;
	                long id = word.getId();
	                String keyword = word.getKeyword();
	                String synonymKeyword = StringUtils.strip(word.getSynonymKeywordForEditingOrReading(), ",");
	                boolean reverse = word.isReverse();
	                String nature = StringUtils.trimToEmpty(word.getNature());
	
	                String js = " onclick='javascript:select(" + id + ",\"" + keyword + "\",\"" + synonymKeyword + "\",\""
	                        + reverse + "\",\"" + nature + "\") '";
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

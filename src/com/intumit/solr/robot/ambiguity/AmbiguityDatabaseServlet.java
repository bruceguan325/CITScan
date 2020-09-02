package com.intumit.solr.robot.ambiguity;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

import com.intumit.message.MessageUtil;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.XssHttpServletRequestWrapper;
import com.intumit.syslog.OperationLogEntity;

public class AmbiguityDatabaseServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	static Logger infoLog = Logger.getLogger(AmbiguityDatabaseServlet.class.getName());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(req);
		Tenant t = Tenant.getFromSession(req.getSession());
		
		List<?> results = null;
		String action = req.getParameter("action");
		Integer opLogId = (Integer) req.getAttribute("opLogId");
		OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
		//新增
		if (log != null && action != null && action.equals("save")) {
			String keyword = (String)req.getParameter("sentence");
			String synonymKeyword = (String)req.getParameter("disambiguation");
			boolean reverse = (((String)req.getParameter("enabled")).equals("1")?true:false);
			try {
				AmbiguityDatabase.save(t.getId(), keyword, synonymKeyword, reverse);
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
			String keyword = (String)xssReq.getParameter("sentence");
			String synonymKeyword = (String)xssReq.getParameter("disambiguation");
			boolean reverse = (((String)req.getParameter("enabled")).equals("1")?true:false);
			try {
				AmbiguityDatabase.update(t.getId(), id, keyword, synonymKeyword, reverse);
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
			String id = (String)xssReq.getParameter("id");
			try {
				AmbiguityDatabase.delete(t.getId(), id);
				log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
			} catch (Exception e) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.appendToMoreDetails(e.toString());
				infoLog.error(e);
			}
			log.update();
		} 
		
		//查詢
		if (req.getRequestURI().contains("ambiguity/search")) {
			String searchKeyword = (String)req.getParameter("searchKeyword");
			try {
				results = AmbiguityDatabase.search(t.getId(), searchKeyword);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else { 
			results = AmbiguityDatabase.listAll(t.getId());
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
//        if (limit.isExported()) {
//            export(tableFacade);
//            return null;
//        } else {
        	Locale locale = req.getLocale();
            String html = html(tableFacade, req,locale);
            req.setAttribute("myhtml", html);
//        }
		
		RequestDispatcher requestdispatch=this.getServletConfig().getServletContext().getRequestDispatcher("/wiseadm/ambiguity.jsp");   
		requestdispatch.forward(req, resp);   
		
	}
	
	 private String html(TableFacade tableFacade, final HttpServletRequest req,final Locale locale) {
	        tableFacade.setColumnProperties("sentence", "disambiguation", "enabled", "action");
	        tableFacade.autoFilterAndSort(true);
	        HtmlToolbar toolbar = new HtmlToolbar();
	        toolbar.enablePageNumbers(true);
	        tableFacade.setToolbar(toolbar); 

	        HtmlTable table = (HtmlTable) tableFacade.getTable();
	        String synonymList = "列表";//MessageUtil.getMessage(locale, "synonym.list");
	        table.setCaption(synonymList);
	        table.getTableRenderer().setWidth("600px");

	        HtmlRow row = table.getRow();

//	        HtmlColumn id = row.getColumn("id");
//	        id.getFilterRenderer().setFilterEditor(new DroplistFilterEditor());
//	        id.setTitle("Id");

	        HtmlColumn keyword = row.getColumn("sentence");
	        String globalSentence = MessageUtil.getMessage(locale, "global.sentence");
	        keyword.setTitle(globalSentence);

	        HtmlColumn synonymKeyword = row.getColumn("disambiguation");
	        String segmentationMethod = MessageUtil.getMessage(locale, "segmentation.method");
	        synonymKeyword.setTitle(segmentationMethod);

	        HtmlColumn reverse = row.getColumn("enabled");
	        String isEnabled = MessageUtil.getMessage(locale, "is.enabled");
	        reverse.setTitle(isEnabled);
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
	        String operation = MessageUtil.getMessage(locale, "operation");
	        action.setTitle(operation);

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
	                Object value = new BasicCellEditor().getValue(item, property,
	                        rowcount);
	                HtmlBuilder html = new HtmlBuilder();
//	                Object id = ItemUtils.getItemValue(item, "id");
	                AmbiguityDatabase word = (AmbiguityDatabase) item;
	                long id = word.getId();
	                String sentence = word.getSentence();
	                String disambiguation = word.getDisambiguation();
	                boolean enabled = word.isEnabled();
	                
	                String js =" onclick='javascript:select(" + id + ",\"" + sentence+ "\",\"" + disambiguation + "\",\"" + enabled + "\") '";
	                html.a().append(js).href().quote().append("#").quote().close();
	                String modify = MessageUtil.getMessage(locale, "modify");
	                html.append(modify);
	                html.aEnd();
	                html.append("&nbsp;&nbsp;");
	                String js2=" onclick='javascript:del(\"tableId\","+id+") '";
	                html.a().append(js2).href().quote().append(req.getContextPath()+"/wiseadm/ambiguity?action=delete&id="+id).quote().close();
	                String delete = MessageUtil.getMessage(locale, "delete");
	                html.append(delete);
	                html.aEnd();
	                
	               
	                return html.toString();
	            }
	            
	        });

	        return tableFacade.render(); 
	    }
}

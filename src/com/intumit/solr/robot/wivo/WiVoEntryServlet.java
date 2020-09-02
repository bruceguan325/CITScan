package com.intumit.solr.robot.wivo;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
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
import com.intumit.solr.admin.AdminGroup;
import com.intumit.solr.admin.AdminGroupFacade;
import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAAltBuildQueue;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.XssHttpServletRequestWrapper;
import com.intumit.syslog.OperationLogEntity;

public class WiVoEntryServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	static Logger infoLog = Logger.getLogger(WiVoEntryServlet.class.getName());

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
		boolean isDirty = false;
		String action = req.getParameter("action");
		Integer opLogId = (Integer) req.getAttribute("opLogId");
		OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
		//新增
		if (log != null && OperationLogEntity.SAVE.equalsIgnoreCase(action)) {
			String channel = (String)req.getParameter("channel");
			String keyword = (String)req.getParameter("keyword");
			String excludes = (String)req.getParameter("excludes");
			String includes = (String)req.getParameter("includes");
			boolean reverse = (((String)req.getParameter("enabled")).equals("1")?true:false);
			try {
				WiVoEntry.save(t.getId(), channel, keyword, excludes, includes, reverse);
				isDirty = true;
				log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
			} catch (Exception e) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.appendToMoreDetails(e.toString());
				infoLog.error(e);
			}
			log.update();
		}

		//修改
		if (log != null && OperationLogEntity.UPDATE.equalsIgnoreCase(action)) {
			String id = (String)xssReq.getParameter("hiddenId");
			String channel = (String)xssReq.getParameter("channel");
			String keyword = (String)xssReq.getParameter("keyword");
			String excludes = (String)xssReq.getParameter("excludes");
			String includes = (String)xssReq.getParameter("includes");
			boolean enable = (((String)req.getParameter("enabled")).equals("1")?true:false);
			try {
				WiVoEntry.update(t.getId(), id, channel, keyword, excludes, includes, enable);
				isDirty = true;
				log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
			} catch (Exception e) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.appendToMoreDetails(e.toString());
				infoLog.error(e);
			}
			log.update();
		}

		//刪除
		if (log != null && OperationLogEntity.DELETE.equalsIgnoreCase(action)) {
			String id = (String)xssReq.getParameter("id");
			try {
				WiVoEntry q = WiVoEntry.get(new Long(id));
				
				if (q.getTenantId() != t.getId())
					return ;
				
				WiVoEntry.delete(t.getId(), id);
				isDirty = true;
				log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
			} catch (Exception e) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.appendToMoreDetails(e.toString());
				infoLog.error(e);
			}
			log.update();
		}

        if (isDirty) {
        		/**
        		 * Do stuff when dirty
        		 */
        }

		//查詢
		if (req.getRequestURI().contains("wivoEntry/search")) {
			String searchKeyword = (String)xssReq.getParameter("searchKeyword");
			try {
				results = WiVoEntry.search(t.getId(), searchKeyword);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (req.getRequestURI().contains("wivoEntry/list")) {
			String searchKeyword = (String)xssReq.getParameter("searchKeyword");
			try {
				results = WiVoEntry.search(t.getId(), searchKeyword);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			results = WiVoEntry.listByTenantId(t.getId());
		}


        TableFacade tableFacade = new TableFacadeImpl("tableId", req);

        tableFacade.setExportTypes(resp);
        tableFacade.setStateAttr("restore");
        tableFacade.setMaxRows(50);

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

		RequestDispatcher requestdispatch=this.getServletConfig().getServletContext().getRequestDispatcher("/wiseadm/qaWiVoEntry-inner.jsp");
		requestdispatch.forward(req, resp);

	}

	 private String html(TableFacade tableFacade, final HttpServletRequest req) {
	        tableFacade.setColumnProperties("channel", "keyword", "excludes", "includes", "enabled", "action");
	        tableFacade.autoFilterAndSort(true);
	        HtmlToolbar toolbar = new HtmlToolbar();
	        toolbar.enablePageNumbers(true);
	        tableFacade.setToolbar(toolbar);
	        final Locale locale = req.getLocale();
	        String vocabularyList = MessageUtil.getMessage(locale, "wivoEntry.list");
	        HtmlTable table = (HtmlTable) tableFacade.getTable();
	        //table.setCaption("詞庫列表");
	        table.setCaption(vocabularyList);
	        table.getTableRenderer().setStyle("width: 1024px;");

	        HtmlRow row = table.getRow();

//	        HtmlColumn id = row.getColumn("id");
//	        id.getFilterRenderer().setFilterEditor(new DroplistFilterEditor());
//	        id.setTitle("Id");

	        HtmlColumn channel = row.getColumn("channel");
	        channel.setTitle(MessageUtil.getMessage(locale, "channel"));
	        
	        HtmlColumn keyword = row.getColumn("keyword");
	        keyword.setTitle(MessageUtil.getMessage(locale, "global.keyword"));
	        
	        HtmlColumn includes = row.getColumn("includes");
	        includes.setTitle(MessageUtil.getMessage(locale, "wivoEntry.includes"));

	        HtmlColumn excludes = row.getColumn("excludes");
	        excludes.setTitle(MessageUtil.getMessage(locale, "wivoEntry.excludes"));

	        HtmlColumn enabled = row.getColumn("enabled");
	        String enable = MessageUtil.getMessage(locale, "is.enabled");
	        
	        enabled.setTitle(enable);
	        enabled.getCellRenderer().setCellEditor(new CellEditor() {
	            public Object getValue(Object item, String property, int rowcount) {

	                WiVoEntry word = (WiVoEntry) item;
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

	        // Using an anonymous class to implement a custom editor.
	        action.getCellRenderer().setCellEditor(new CellEditor() {
	            public Object getValue(Object item, String property, int rowcount) {
	                Object value = new BasicCellEditor().getValue(item, property,
	                        rowcount);
	                HtmlBuilder html = new HtmlBuilder();
//	                Object id = ItemUtils.getItemValue(item, "id");
	                WiVoEntry word = (WiVoEntry) item;
	                long id = word.getId();
	                String category = word.getChannel();
	                String keyword = word.getKeyword();
	                String includes = StringUtils.trimToEmpty(word.getIncludes());
	                String excludes = StringUtils.trimToEmpty(word.getExcludes());
	                boolean enabled = word.isEnabled();

	                String js =" onclick='javascript:select(" + id + ",\"" + category + "\",\"" + keyword + "\",\"" + excludes + "\",\"" + includes+ "\",\"" + enabled + "\") '";
	                html.a().append(js).href().quote().append("#").quote().close();
	                String modify = MessageUtil.getMessage(locale, "modify");
	                //html.append("修改");
	                html.append(modify);
	                html.aEnd();
	                html.append("&nbsp;&nbsp;");
					String js2 = new StringBuilder()
							.append(" class='delWivo' class='btn btn-danger' onclick='return del(\"tableId\",").append(id)
							.append(") '").toString();
					html.a().append(js2).href().quote()
							.append(new StringBuilder().append(req.getContextPath())
									.append("/wiseadm/wivoEntry?action=").append(OperationLogEntity.DELETE)
									.append("&id=").append(id).toString())
							.quote().close();
					String delete = MessageUtil.getMessage(locale, "delete");
	                //html.append("删除");
	                html.append(delete);
	                html.aEnd();


	                return html.toString();
	            }

	        });

	        return tableFacade.render();
	    }
}

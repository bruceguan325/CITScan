package com.intumit.solr.robot.dictionary;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

public class EmbeddedFuzzyDatabaseServlet extends HttpServlet {
	
	private static final long serialVersionUID = 5600199076884895731L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		Tenant t = Tenant.getFromSession(req.getSession());
		List<EmbeddedFuzzyDictionaryDatabase> results = null;
		//新增
		if (req.getRequestURI().contains("embedded/save")) {
			String keyword = (String)req.getParameter("keyword");
			String type = (String)req.getParameter("type");
			EmbeddedFuzzyDictionaryDatabase.save(t.getId(), null, keyword, type);
		}

		//修改
		if (req.getRequestURI().contains("embedded/update")) {
			String id = (String)req.getParameter("hiddenId");
			String keyword = (String)req.getParameter("keyword");
			String type = (String)req.getParameter("type");
			try {
				EmbeddedFuzzyDictionaryDatabase.update(t.getId(), Long.parseLong(id), keyword, type);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		//刪除
		if (req.getRequestURI().contains("embedded/delete")) {
			String id = (String)req.getParameter("id");
			try {
				EmbeddedFuzzyDictionaryDatabase.delete(t.getId(), id);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		//查詢
		if (req.getRequestURI().contains("embedded/search")) {
			String searchKeyword = (String)req.getParameter("searchKeyword");
			String searchType = req.getParameter("searchType");
			try {
				results = EmbeddedFuzzyDictionaryDatabase.search(t.getId(), searchKeyword, searchType);
				req.setAttribute("searchKeyword", searchKeyword);
				req.setAttribute("searchType", searchType);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (req.getRequestURI().contains("embedded/list")) {
			String searchKeyword = (String)req.getParameter("searchKeyword");
			String searchType = req.getParameter("searchType");
			try {
				results = EmbeddedFuzzyDictionaryDatabase.search(t.getId(), searchKeyword, searchType);
				req.setAttribute("searchKeyword", searchKeyword);
				req.setAttribute("searchType", searchType);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			results = EmbeddedFuzzyDictionaryDatabase.listByTenantId(t.getId());
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
        String html = html(tableFacade, req);
        req.setAttribute("myhtml", html);

		RequestDispatcher requestdispatch = this.getServletConfig().getServletContext().getRequestDispatcher("/wiseadm/embeddedDictionary.jsp");
		requestdispatch.forward(req, resp);

	}

	 private String html(TableFacade tableFacade, final HttpServletRequest req) {
	        tableFacade.setColumnProperties("keyword", "type", "action");
	        tableFacade.autoFilterAndSort(true);
	        HtmlToolbar toolbar = new HtmlToolbar();
	        toolbar.enablePageNumbers(true);
	        tableFacade.setToolbar(toolbar);
	        final Locale locale = req.getLocale();
	        String vocabularyList = MessageUtil.getMessage(locale, "vocabulary.list");
	        HtmlTable table = (HtmlTable) tableFacade.getTable();
	        table.setCaption(vocabularyList);
	        table.getTableRenderer().setStyle("width: 800px;");

	        HtmlRow row = table.getRow();

	        HtmlColumn keyword = row.getColumn("keyword");
	        String globalKeyword = MessageUtil.getMessage(locale, "global.keyword");
	        keyword.setTitle(globalKeyword);

	        HtmlColumn type = row.getColumn("type");
	        String usage = MessageUtil.getMessage(locale, "global.type");
	        type.setTitle(usage);

	        HtmlColumn action = row.getColumn("action");
	        action.setTitle(MessageUtil.getMessage(locale, "operation"));

	        // Using an anonymous class to implement a custom editor.
	        action.getCellRenderer().setCellEditor(new CellEditor() {
	            public Object getValue(Object item, String property, int rowcount) {
	                Object value = new BasicCellEditor().getValue(item, property, rowcount);
	                HtmlBuilder html = new HtmlBuilder();
	                EmbeddedFuzzyDictionaryDatabase efd = (EmbeddedFuzzyDictionaryDatabase) item;
	                long id = efd.getId();
	                String keyword = efd.getKeyword();
	                String type = efd.getType();
	                String js =" onclick='javascript:select(" + id + ",\"" + keyword+ "\",\"" + type + "\") '";
	                html.a().append(js).href().quote().append("#").quote().close();
	                String modify = MessageUtil.getMessage(locale, "modify");
	                html.append(modify);
	                html.aEnd();
	                html.append("&nbsp;&nbsp;");
	                String js2=" onclick='javascript:del(\"tableId\","+id+") '";
	                html.a().append(js2).href().quote().append(req.getContextPath()+"/wiseadm/embedded/delete?id="+id).quote().close();
	                String delete = MessageUtil.getMessage(locale, "delete");
	                html.append(delete);
	                html.aEnd();
	                return html.toString();
	            }
	        });

	        return tableFacade.render();
	    }

}

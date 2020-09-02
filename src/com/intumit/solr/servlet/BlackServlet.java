package com.intumit.solr.servlet;

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
import com.intumit.solr.blackKeywords.BlackKeyword;
import com.intumit.solr.blackKeywords.BlackKeywordFacade;
import com.intumit.solr.util.WiSeEnv;


public class BlackServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		List results = null;
		//新增
		if (req.getRequestURI().contains("black/save")) {
			String keyword = (String)req.getParameter("keyword");
			BlackKeywordFacade.getInstance().save(keyword);
		}

		//修改
		if (req.getRequestURI().contains("black/update")) {
			String id = req.getParameter("hiddenId");
			String keyword = (String)req.getParameter("keyword");
			try {
				BlackKeywordFacade.getInstance().update(id, keyword);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		//刪除
		if (req.getRequestURI().contains("black/delete")) {
			String id = req.getParameter("id");
			try {
				BlackKeywordFacade.getInstance().delete(new Long(id));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		//查詢
		if (req.getRequestURI().contains("black/search")) {
			String searchKeyword = (String)req.getParameter("searchKeyword");
			try {
				results = BlackKeywordFacade.getInstance().listBlackKeywords(searchKeyword);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			results = BlackKeywordFacade.getInstance().listBlackKeywords();
		}


        TableFacade tableFacade = new TableFacadeImpl("tableId", req);

        tableFacade.setExportTypes(resp);
        tableFacade.setStateAttr("restore");

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

		RequestDispatcher requestdispatch=this.getServletConfig().getServletContext().getRequestDispatcher( WiSeEnv.getAdminContextPath() + "/black.jsp");
		requestdispatch.forward(req, resp);

	}

	 private String html(TableFacade tableFacade, final HttpServletRequest req) {
	        tableFacade.setColumnProperties("keyword", "action");
	        tableFacade.autoFilterAndSort(true);
	        HtmlToolbar toolbar = new HtmlToolbar();
	        toolbar.enablePageNumbers(true);
	        tableFacade.setToolbar(toolbar);
	        final Locale locale = req.getLocale();
	        HtmlTable table = (HtmlTable) tableFacade.getTable();
	        String vocabularyList = MessageUtil.getMessage(locale, "vocabulary.list");
	        //table.setCaption("詞庫列表");
	        table.setCaption(vocabularyList);
	        table.getTableRenderer().setWidth("600px");

	        HtmlRow row = table.getRow();

//	        HtmlColumn id = row.getColumn("id");
//	        id.getFilterRenderer().setFilterEditor(new DroplistFilterEditor());
//	        id.setTitle("Id");

	        HtmlColumn keyword = row.getColumn("keyword");
	        String ignoreWord = MessageUtil.getMessage(locale, "ignore.word");
	        //keyword.setTitle("忽略字");
	        keyword.setTitle(ignoreWord);

	        HtmlColumn action = row.getColumn("action");
	        String operation =MessageUtil.getMessage(locale, "operation");
	        //action.setTitle("操作");
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
	                BlackKeyword word = (BlackKeyword) item;
	                long id = word.getId();
	                String keyword = word.getKeyword();


	                String js =" onclick='javascript:select(" + id + ",\"" + keyword+ "\") '";
	                html.a().append(js).href().quote().append("#").quote().close();
	                String modify = MessageUtil.getMessage(locale, "modify");
	                //html.append("修改");
	                html.append(modify);
	                html.aEnd();
	                html.append("&nbsp;&nbsp;");
	                String js2=" onclick='javascript:del(\"tableId\","+id+") '";
	                html.a().append(js2).href().quote().append(req.getContextPath()+"" + WiSeEnv.getAdminContextPath() + "/black/delete?id="+id).quote().close();
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

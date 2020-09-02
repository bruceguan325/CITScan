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
import com.intumit.smartwiki.WikiWord;
import com.intumit.solr.whiteKeywords.WhiteWikiWordFacade;

public class WhiteServlet extends HttpServlet {
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
		if (req.getRequestURI().contains("white/save")) {
			String keyword = (String)req.getParameter("keyword");
			String nature = (String)req.getParameter("nature");
			WhiteWikiWordFacade.getInstance().save(keyword, nature);
		} 
		
		//修改
		if (req.getRequestURI().contains("white/update")) {
			int id = Integer.parseInt(req.getParameter("hiddenId"));
			String keyword = (String)req.getParameter("keyword");
			String nature = (String)req.getParameter("nature");
			try {
				WhiteWikiWordFacade.getInstance().update(id, keyword, nature);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} 
		
		//刪除
		if (req.getRequestURI().contains("white/delete")) {
			int id = Integer.parseInt(req.getParameter("id"));
			try {
				WhiteWikiWordFacade.getInstance().delete(id);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} 
		
		//查詢
		if (req.getRequestURI().contains("white/search")) {
			String searchKeyword = (String)req.getParameter("searchKeyword");
			try {
				results = WhiteWikiWordFacade.getInstance().listSynonymKeywords(searchKeyword);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else { 
			results = WhiteWikiWordFacade.getInstance().listSynonymKeywords();
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
            String html = html(tableFacade, req);
            req.setAttribute("myhtml", html);
//        }
		
		RequestDispatcher requestdispatch=this.getServletConfig().getServletContext().getRequestDispatcher("/wiseadm/white.jsp");   
		requestdispatch.forward(req, resp);   
		
	}
	
	 private String html(TableFacade tableFacade, final HttpServletRequest req) {
	        tableFacade.setColumnProperties("pageTitle", "nature", "action");
	        tableFacade.autoFilterAndSort(true);
	        HtmlToolbar toolbar = new HtmlToolbar();
	        toolbar.enablePageNumbers(true);
	        tableFacade.setToolbar(toolbar); 
	        final Locale locale = req.getLocale();
	        String vocabularyList = MessageUtil.getMessage(locale, "vocabulary.list");
	        HtmlTable table = (HtmlTable) tableFacade.getTable();
	        //table.setCaption("詞庫列表");
	        table.setCaption(vocabularyList);
	        table.getTableRenderer().setWidth("600px");

	        HtmlRow row = table.getRow();

//	        HtmlColumn id = row.getColumn("id");
//	        id.getFilterRenderer().setFilterEditor(new DroplistFilterEditor());
//	        id.setTitle("Id");

	        HtmlColumn keyword = row.getColumn("pageTitle");
	        String keyWord = MessageUtil.getMessage(locale, "global.keyword");
	        //keyword.setTitle("關鍵字");
	        keyword.setTitle(keyWord);
	        HtmlColumn nature = row.getColumn("nature");
	        String natureWord = MessageUtil.getMessage(locale, "nature.words");
	        //nature.setTitle("詞性");
	        nature.setTitle(natureWord);
	             
	        HtmlColumn action = row.getColumn("action");
	        String operation= MessageUtil.getMessage(locale, "operation");
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
	                WikiWord word = (WikiWord) item;
	                int id = word.getKeywordId();
	                String keyword = word.getPageTitle();

	                
	                String js =" onclick='javascript:select(" + id + ",\"" + keyword+ "\") '";
	                html.a().append(js).href().quote().append("#").quote().close();
	                String modify = MessageUtil.getMessage(locale, "modify");
	                //html.append("修改");
	                html.append(modify);
	                html.aEnd();
	                html.append("&nbsp;&nbsp;");
	                String js2=" onclick='javascript:del(\"tableId\","+id+") '";
	                html.a().append(js2).href().quote().append(req.getContextPath()+"/wiseadm/white/delete?id="+id).quote().close();
	                String delete = MessageUtil.getMessage(locale, "modify");
	                //html.append("删除");
	                html.append(delete);
	                html.aEnd();
	                
	               
	                return html.toString();
	            }
	            
	        });

	        return tableFacade.render(); 
	    }
}

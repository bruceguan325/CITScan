package com.intumit.solr.servlet;

import java.io.IOException;
import java.util.ArrayList;
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
import com.intumit.solr.recommendKeywords.RecommendKeyword;
import com.intumit.solr.recommendKeywords.RecommendKeywordFacade;
import com.intumit.solr.util.WiSeEnv;
import com.thoughtworks.xstream.XStream;

public class RecommendServlet extends HttpServlet {
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
		Status status = null;
		RecommendKeyword targetRK = null;
		// 新增
		if (req.getRequestURI().contains("recommend/save")) {
			String keyword = (String) req.getParameter("keyword");
			String sort = (String) req.getParameter("sort");
			String os = (String) req.getParameter("os");
			String url = (String) req.getParameter("url");
			String target = (String) req.getParameter("target");
			targetRK = RecommendKeywordFacade.getInstance().save(keyword, sort, os, url,
					target);
			status = new Status("Succeed");
		}

		// 修改
		if (req.getRequestURI().contains("recommend/update")) {
			String id = (String) req.getParameter("hiddenId");
			String keyword = (String) req.getParameter("keyword");
			String sort = (String) req.getParameter("sort");
			String os = (String) req.getParameter("os");
			String url = (String) req.getParameter("url");
			String target = (String) req.getParameter("target");
			try {
				targetRK = RecommendKeywordFacade.getInstance().update(id, keyword, sort,
						os, url, target);
				status = new Status("Succeed");
			} catch (Exception e) {
				e.printStackTrace();
				status = new Status("Failed", e.getMessage());
			}
		}

		// 刪除
		if (req.getRequestURI().contains("recommend/delete")) {
			String id = (String) req.getParameter("id");
			try {
				targetRK = RecommendKeywordFacade.getInstance().delete(id);
				status = new Status("Succeed");
			} catch (Exception e) {
				e.printStackTrace();
				status = new Status("Failed", e.getMessage());
			}
		}

		// 查詢
		if (req.getRequestURI().contains("recommend/search")) {
			String searchKeyword = (String) req.getParameter("searchKeyword");
			try {
				results = RecommendKeywordFacade.getInstance()
						.listRecommendKeywords(searchKeyword);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			results = RecommendKeywordFacade.getInstance()
					.listRecommendKeywords();
		}

		if ("xml".equalsIgnoreCase(req.getParameter("wt"))) {
			XStream outXML = new XStream();
			outXML.alias("keyword", RecommendKeyword.class);
			outXML.alias("result", Status.class);
			try {
				resp.setContentType("application/xml;charset=UTF-8");
				// JSONSerializer serializer = new JSONSerializer();
				String out = "";
				
				List<Object> rr = new ArrayList<Object>();
				if (status != null) { 
					rr.add(status);
					if (targetRK != null)
						rr.add(targetRK);
					rr.add(results);
					out = outXML.toXML(rr);
				}
				else {
					out = outXML.toXML(results);
				}
				
				resp.getWriter().write(out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
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
			// if (limit.isExported()) {
			// export(tableFacade);
			// return null;
			// } else {
			String html = html(tableFacade, req);
			req.setAttribute("myhtml", html);
			// }

			RequestDispatcher requestdispatch = this.getServletConfig()
					.getServletContext()
					.getRequestDispatcher(WiSeEnv.getAdminContextPath() + "/recommend.jsp");
			requestdispatch.forward(req, resp);
		}

	}

	private String html(TableFacade tableFacade, final HttpServletRequest req) {
		tableFacade.setColumnProperties("keyword", "os", "sort", "url",
				"target", "action");
		tableFacade.autoFilterAndSort(true);
		HtmlToolbar toolbar = new HtmlToolbar();
		toolbar.enablePageNumbers(true);
		tableFacade.setToolbar(toolbar);
		final Locale locale = req.getLocale();
		String recommendWordList = MessageUtil.getMessage(locale, "recommend.word.list");
		HtmlTable table = (HtmlTable) tableFacade.getTable();
		//table.setCaption("指定關鍵字列表");
		table.setCaption(recommendWordList);
		table.getTableRenderer().setWidth("600px");

		HtmlRow row = table.getRow();

		// HtmlColumn id = row.getColumn("id");
		// id.getFilterRenderer().setFilterEditor(new DroplistFilterEditor());
		// id.setTitle("Id");

		HtmlColumn keyword = row.getColumn("keyword");
		String keyWord = MessageUtil.getMessage(locale, "global.keyword");
		//keyword.setTitle("關鍵字");
		keyword.setTitle(keyWord);

		HtmlColumn sort = row.getColumn("os");
		String channel = MessageUtil.getMessage(locale, "global.channel");
		//sort.setTitle("頻道");
		sort.setTitle(channel);

		HtmlColumn os = row.getColumn("sort");
		String ranking = MessageUtil.getMessage(locale, "global.ranking");
		//os.setTitle("排名");
		os.setTitle(ranking);

		HtmlColumn url = row.getColumn("url");
		String linkUrl = MessageUtil.getMessage(locale, "link.url");
		url.setTitle(linkUrl);

		HtmlColumn target = row.getColumn("target");
		String webPageOpenTarget = MessageUtil.getMessage(locale, "web.page.open.target");
		target.setTitle(webPageOpenTarget);

		HtmlColumn action = row.getColumn("action");
		String operation= MessageUtil.getMessage(locale, "operation");
		action.setTitle(operation);

		// Using an anonymous class to implement a custom editor.
		// firstName.getCellRenderer().setCellEditor(new CellEditor() {
		// public Object getValue(Object item, String property, int rowcount) {
		// Object value = new BasicCellEditor().getValue(item, property,
		// rowcount);
		// HtmlBuilder html = new HtmlBuilder();
		// html.a().href().quote().append("http://")
		// .quote().close();
		// html.append(value);
		// html.aEnd();
		// return html.toString();
		// }
		// });

		// Using an anonymous class to implement a custom editor.
		action.getCellRenderer().setCellEditor(new CellEditor() {
			public Object getValue(Object item, String property, int rowcount) {
				Object value = new BasicCellEditor().getValue(item, property,
						rowcount);
				HtmlBuilder html = new HtmlBuilder();
				// Object id = ItemUtils.getItemValue(item, "id");
				RecommendKeyword word = (RecommendKeyword) item;
				long id = word.getId();
				String keyword = word.getKeyword();
				String os = word.getOs();
				String sort = word.getSort().toString();
				String url = word.getUrl();
				String target = word.getTarget();

				String js = " onclick='javascript:select(" + id + ",\""
						+ keyword + "\",\"" + os + "\",\"" + sort + "\",\""
						+ url + "\",\"" + target + "\") '";
				html.a().append(js).href().quote().append("#").quote().close();
				String modify = MessageUtil.getMessage(locale, "modify");
				//html.append("修改");
				html.append(modify);
				html.aEnd();
				html.append("<BR/>");
				String js2 = " onclick='javascript:del(\"tableId\"," + id
						+ ") '";
				html.a()
						.append(js2)
						.href()
						.quote()
						.append(req.getContextPath()
								+ "" + WiSeEnv.getAdminContextPath() + "/recommend/delete?id=" + id).quote()
						.close();
				String delete = MessageUtil.getMessage(locale, "delete");
				//html.append("删除");
				html.append(delete);
				html.aEnd();

				return html.toString();
			}

		});

		return tableFacade.render();
	}
	
	public static class Status {
		String status = null;
		String message = null;
		
		public Status(String status) {
			this.status = status;
		}
		
		public Status(String status, String message) {
			this.status = status;
			this.message = message;
		}
		
		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
}

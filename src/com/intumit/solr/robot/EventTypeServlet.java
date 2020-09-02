package com.intumit.solr.robot;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
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
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.XssHttpServletRequestWrapper;
import com.intumit.syslog.OperationLogEntity;

public class EventTypeServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

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
		
		if (!t.getEnableScenario() || (AdminGroupFacade.getInstance().getFromSession(req.getSession()).getSystemAdminCURD() & AdminGroup.O3) == 0) {
			RequestDispatcher requestdispatch=this.getServletConfig().getServletContext().getRequestDispatcher("/wiseadm/login.jsp");
			requestdispatch.forward(req, resp);
		}
		List<EventType> results = null;
		boolean isDirty = false;
		String action = req.getParameter("action");
		Integer opLogId = (Integer) req.getAttribute("opLogId");
		OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
		//新增
		if (log != null && action != null && action.equals("save")) {
			String code = (String)req.getParameter("code");
			String channel = (String)req.getParameter("channel");
			boolean enabled = (((String)req.getParameter("enabled")).equals("1")?true:false);
			boolean builtIn = false;
			if (builtIn) {
				if (AdminGroupFacade.getInstance().getFromSession(req.getSession()).getAdminAdminCURD() == 0) {
					// 非 ROOT 不允許儲存 "_" 開頭的 TAG
					log.setStatusMessage(OperationLogEntity.Status.FAILED);
					log.appendToMoreDetails("非 ROOT 不允許儲存 \"_\" 開頭的 TAG");
					log.update();
					return;
				}
			}
			String result = EventType.save(t.getId(), code, channel, builtIn, enabled);
			if (result.equals("success")) {
				isDirty = true;
				log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
			} else {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.appendToMoreDetails(result);
			}
			log.update();
		}

		//修改
		if (log != null && action != null && action.equals("update")) {
			String id = (String)xssReq.getParameter("hiddenId");
			String code = (String)xssReq.getParameter("code");
			String channel = (String)xssReq.getParameter("channel");
			boolean enabled = (((String)req.getParameter("enabled")).equals("1")?true:false);
			String builtInStr = (String) req.getParameter("builtIn");
			boolean builtIn = StringUtils.isBlank(builtInStr) ? false : (builtInStr.equals("1") ? true : false);

			try {
				EventType old = EventType.get(Integer.parseInt(id));
				if (old == null || old.getBuiltIn()) {
					if (AdminGroupFacade.getInstance().getFromSession(req.getSession()).getAdminAdminCURD() == 0) {
						// 非 ROOT 不允許儲存 "_" 開頭的 TAG
						log.setStatusMessage(OperationLogEntity.Status.FAILED);
						log.appendToMoreDetails("非 ROOT 不允許儲存 \"_\" 開頭的 TAG");
						log.update();
						return;
					}
				}
				String result = EventType.update(t.getId(), Integer.parseInt(id), code, channel, builtIn, enabled);
				if (result.equals("success")) {
					isDirty = true;
					log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
				} else {
					log.setStatusMessage(OperationLogEntity.Status.FAILED);
					log.appendToMoreDetails(result);
				}
				log.update();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		//刪除
		if (log != null && action != null && action.equals("delete")) {
			String id = (String)xssReq.getParameter("id");
			try {
				EventType q = EventType.get(new Integer(id));
				
				if (q.getTenantId() != t.getId())
					return ;
				
				String result = EventType.delete(t.getId(), new Integer(id));
				if (result.equals("success")) {
					isDirty = true;
					log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
				} else {
					log.setStatusMessage(OperationLogEntity.Status.FAILED);
					log.appendToMoreDetails(result);
				}
				log.update();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
        if (isDirty) {
        		// Do somthing if need
	    }

		//查詢
		if (req.getRequestURI().contains("eventType/search")) {
			String searchKeyword = (String)req.getParameter("searchKeyword");
			try {
				results = EventType.searchCodeAndChannel(t.getId(), searchKeyword, null);
				req.setAttribute("searchKeyword", searchKeyword);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (req.getRequestURI().contains("eventType/list")) {
			try {
				results = EventType.listAll(t.getId(), null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			results = EventType.listAll(t.getId(), null);
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

		RequestDispatcher requestdispatch=this.getServletConfig().getServletContext().getRequestDispatcher("/wiseadm/qaEventType-inner.jsp");
		requestdispatch.forward(req, resp);

	}

	 private String html(TableFacade tableFacade, final HttpServletRequest req) {
	        tableFacade.setColumnProperties("code", "channel", "builtIn", "enabled", "action");
	        tableFacade.autoFilterAndSort(true);
	        HtmlToolbar toolbar = new HtmlToolbar();
	        toolbar.enablePageNumbers(true);
	        tableFacade.setToolbar(toolbar);
	        final Locale locale = req.getLocale();
	        String vocabularyList = MessageUtil.getMessage(locale, "eventType.management");
	        HtmlTable table = (HtmlTable) tableFacade.getTable();
	        //table.setCaption("詞庫列表");
	        table.setCaption(vocabularyList);
	        table.getTableRenderer().setStyle("width: 800px;");

	        HtmlRow row = table.getRow();

//	        HtmlColumn id = row.getColumn("id");
//	        id.getFilterRenderer().setFilterEditor(new DroplistFilterEditor());
//	        id.setTitle("Id");

	        HtmlColumn category = row.getColumn("code");
	        category.setTitle(MessageUtil.getMessage(locale, "global.code"));
	        
	        HtmlColumn tag = row.getColumn("channel");
	        tag.setTitle(MessageUtil.getMessage(locale, "global.channel"));


	        HtmlColumn builtIn = row.getColumn("builtIn");
	        String builtInString = MessageUtil.getMessage(locale, "global.builtIn");
	        builtIn.setTitle(builtInString);
	        builtIn.getCellRenderer().setCellEditor(new CellEditor() {
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

	        HtmlColumn reverse = row.getColumn("enabled");
	        String enable = MessageUtil.getMessage(locale, "is.enabled");
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

	        // Using an anonymous class to implement a custom editor.
	        action.getCellRenderer().setCellEditor(new CellEditor() {
	            public Object getValue(Object item, String property, int rowcount) {
	                Object value = new BasicCellEditor().getValue(item, property,
	                        rowcount);
	                HtmlBuilder html = new HtmlBuilder();
//	                Object id = ItemUtils.getItemValue(item, "id");
	                EventType word = (EventType) item;
	                long id = word.getId();
	                String code = word.getCode();
	                String channel = word.getChannel();
	                boolean builtIn = word.getBuiltIn();
	                boolean enabled = word.getEnabled();
	                boolean editable = true;

            			// No edit buttons when it's a system reserved intent
	                if (builtIn) {
	                		editable = false;
	                }

	                if (editable) {
		                //html.append("修改");
		                String js =" onclick='javascript:select(" + id + ",\"" + code + "\",\"" + channel+ "\",\"" + builtIn + "\",\"" + enabled + "\") '";
		                html.a().append(js).href().quote().append("#").quote().close();
		                String modify = MessageUtil.getMessage(locale, "modify");
		                html.append(modify);
		                html.aEnd();
		                html.append("&nbsp;&nbsp;");
		                String js2=" onclick='return del(\"tableId\","+id+") '";
		                html.a().append(js2).href().quote().append(req.getContextPath()+"/wiseadm/eventType?action=delete&id="+id).quote().close();
		                String delete = MessageUtil.getMessage(locale, "delete");
		                //html.append("删除");
		                html.append(delete);
		                html.aEnd();
	                }

	                return html.toString();
	            }

	        });

	        return tableFacade.render();
	    }
}

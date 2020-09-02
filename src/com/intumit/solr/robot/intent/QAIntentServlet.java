package com.intumit.solr.robot.intent;

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
import com.intumit.solr.admin.AdminGroupFacade;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAAltBuildQueue;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.XssHttpServletRequestWrapper;
import com.intumit.syslog.OperationLogEntity;

public class QAIntentServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	static Logger infoLog = Logger.getLogger(QAIntentServlet.class.getName());

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
		AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());
		
		List<QAIntent> results = null;
		boolean isDirty = false;
		String dirtyTag = null;
		String action = req.getParameter("action");
		Integer opLogId = (Integer) req.getAttribute("opLogId");
		OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
		//新增
		if (log != null && action != null && action.equals("save")) {
			String category = (String)xssReq.getParameter("category");
			String tag = StringUtils.trimToNull((String)xssReq.getParameter("tag"));
			String[] altTemplates = xssReq.getParameterValues("altTemplates");
			String keywords = StringUtils.trimToNull((String)req.getParameter("keywords"));
			boolean reverse = (((String)req.getParameter("enabled")).equals("1")?true:false);

			if (tag == null || keywords == null) return; // 不允許空值
			
			if (StringUtils.startsWith(tag, "_")) {
				if (AdminGroupFacade.getInstance().getFromSession(req.getSession()).getAdminAdminCURD() == 0) {
					// 非 ROOT 不允許儲存 "_" 開頭的 TAG
					tag = tag.substring(1);
				}
			}
			try {
				QAIntent.save(t.getId(), keywords, reverse, category, tag, StringUtils.join(altTemplates, ","));
				log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
			} catch (Exception e) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.appendToMoreDetails(e.toString());
				infoLog.error(e);
			}
			log.update();
			isDirty = true;
			dirtyTag = tag;
		}

		//修改
		if (log != null && action != null && action.equals("update")) {
			String id = (String)xssReq.getParameter("hiddenId");
			String category = (String)xssReq.getParameter("category");
			String tag = StringUtils.trimToNull((String)xssReq.getParameter("tag"));
			String[] altTemplates = xssReq.getParameterValues("altTemplates");
			String keywords = StringUtils.trimToNull((String)xssReq.getParameter("keywords"));
			boolean enabled = (((String)xssReq.getParameter("enabled")).equals("1")?true:false);

			if (tag == null || keywords == null) return; // 不允許空值
			
			if (StringUtils.startsWith(tag, "_")) {
				if (AdminGroupFacade.getInstance().getFromSession(req.getSession()).getAdminAdminCURD() == 0) {
					// 非 ROOT 不允許儲存 "_" 開頭的 TAG
					tag = tag.substring(1);
				}
			}
			try {
				QAIntent intent = QAIntent.get(Long.parseLong(id));
				if (intent != null && intent.getTenantId().equals(t.getId())) {
					intent.setKeywords(keywords);
					intent.setCategory(category);
					intent.setTag(tag);
					intent.setAltTemplates(StringUtils.join(altTemplates, ","));
					intent.setEnabled(enabled);
					QAIntent.saveOrUpdate(intent);
				}
				log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
			} catch (Exception e) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.appendToMoreDetails(e.toString());
				infoLog.error(e);
			}
			log.update();
			isDirty = true;
			dirtyTag = tag;
		}

		//刪除
		if (log != null && action != null && action.equals("delete")) {
			String id = (String)xssReq.getParameter("id");
			try {
				QAIntent q = QAIntent.get(new Long(id));
				
				if (q.getTenantId() != t.getId())
					return ;
				
				QAIntent.delete(t.getId(), id);
				dirtyTag = q.getTag();
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
	        	int start = 0;
	        	int rows = 100;
	        	long totalEffect = 0;
	        	QAUtil qautil = QAUtil.getInstance(t);
	        	
	        	try {
		        	do {
		        		SolrDocumentList docs = qautil.lowLevelSearchCommonSense(QA.FN_EXPAND_INTENT_TAGS, dirtyTag, start, rows);
		        		totalEffect = docs.getNumFound();
		        		if (start == 0) {
		        			System.out.println("QAIntent changed, effected [" + totalEffect + "] docs, now start scheduling for recaluldate alts.");
		        		}
		        		
		        		for (SolrDocument doc: docs) {
		        			QA qa = new QA(doc);
		        			QAAltBuildQueue.add(t.getId(), qa.getId(), qa.getKid(), qa.getQuestionAltTemplates(), user.getLoginName());
		        		}
		        		
		        		start += rows;
		        	}
		        	while (totalEffect > start);
	        	}
	        	catch (Exception ex) {
	        		ex.printStackTrace();
	        	}
	    }

		//查詢
		if (req.getRequestURI().contains("intent/search")) {
			String searchKeyword = (String)req.getParameter("searchKeyword");
			try {
				results = QAIntent.search(t.getId(), searchKeyword);
				req.setAttribute("searchKeyword", searchKeyword);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (req.getRequestURI().contains("intent/list")) {
			String searchKeyword = (String)req.getParameter("searchKeyword");
			try {
				results = QAIntent.search(t.getId(), searchKeyword);
				req.setAttribute("searchKeyword", searchKeyword);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			results = QAIntent.listByTenantId(t.getId());
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
        
        for (QAIntent i: results) {
        		if (i.isSystemBuiltIn()) {
        			i.setKeywords(StringUtils.left(i.getKeywords(), 10) + "...");
        		}
        }
        tableFacade.setItems(results);
//        if (limit.isExported()) {
//            export(tableFacade);
//            return null;
//        } else {
            String html = html(tableFacade, req);
            req.setAttribute("myhtml", html);
//        }

		RequestDispatcher requestdispatch=this.getServletConfig().getServletContext().getRequestDispatcher("/wiseadm/intent.jsp");
		requestdispatch.forward(req, resp);

	}

	 private String html(TableFacade tableFacade, final HttpServletRequest req) {
	        tableFacade.setColumnProperties("category", "tag", "keywords", "enabled", "action");
	        tableFacade.autoFilterAndSort(true);
	        HtmlToolbar toolbar = new HtmlToolbar();
	        toolbar.enablePageNumbers(true);
	        tableFacade.setToolbar(toolbar);
	        final Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(req, false, false);
	        String vocabularyList = MessageUtil.getMessage(locale, "vocabulary.list");
	        HtmlTable table = (HtmlTable) tableFacade.getTable();
	        //table.setCaption("詞庫列表");
	        table.setCaption(vocabularyList);
	        table.getTableRenderer().setStyle("width: 800px;");

	        HtmlRow row = table.getRow();

//	        HtmlColumn id = row.getColumn("id");
//	        id.getFilterRenderer().setFilterEditor(new DroplistFilterEditor());
//	        id.setTitle("Id");

	        HtmlColumn category = row.getColumn("category");
	        category.setTitle(MessageUtil.getMessage(locale, "category"));
	        
	        HtmlColumn tag = row.getColumn("tag");
	        tag.setTitle(MessageUtil.getMessage(locale, "global.tag"));

	        HtmlColumn keywords = row.getColumn("keywords");
	        String globalKeyword = MessageUtil.getMessage(locale, "global.keyword");
	        keywords.setTitle(globalKeyword);

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
	                QAIntent word = (QAIntent) item;
	                long id = word.getId();
	                String category = word.getCategory();
	                String tag = word.getTag();
	                String keywords = word.getKeywords();
	                boolean enabled = word.isEnabled();
	                boolean editable = true;

            			// No edit buttons when it's a system reserved intent
	                if (word.isSystemBuiltIn()) {
	                		keywords = "";
	                		editable = false;
	                }

	                if (editable) {
		                //html.append("修改");
		                String js =" onclick='javascript:select(" + id + ",\"" + tag + "\",\"" + keywords+ "\",\"" + enabled + "\",\"" + category + "\") '";
		                html.a().append(js).href().quote().append("#").quote().close();
		                String modify = MessageUtil.getMessage(locale, "modify");
		                html.append(modify);
		                html.aEnd();
		                html.append("");
		                String js2=" class='btn btn-danger btn-xs' style='color:white;' onclick='return del(\"tableId\","+id+") '";
		                html.a().append(js2).href().quote().append(req.getContextPath()+"/wiseadm/intent?action=delete&id="+id).quote().close();
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

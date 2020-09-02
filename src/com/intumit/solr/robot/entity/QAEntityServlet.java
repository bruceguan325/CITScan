package com.intumit.solr.robot.entity;

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

import com.google.gson.Gson;
import com.intumit.message.MessageUtil;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.robot.AuditStatus;
import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAAltBuildQueue;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.XssHttpServletRequestWrapper;
import com.intumit.syslog.OperationLogEntity;

public class QAEntityServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	static Logger infoLog = Logger.getLogger(QAEntityServlet.class.getName());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String id = req.getParameter("id");
		if (id != null) {
			QAEntity dic = QAEntity.get(Long.parseLong(id));
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(req, false, false);
		XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(req);
		Tenant t = Tenant.getFromSession(req.getSession());
		AdminUser user = AdminUserFacade.getInstance().getFromSession(req.getSession());
		
		List<?> results = null;
		boolean isDirty = false;
		String dirtyCode = null;
		String action = req.getParameter("action");
		Integer opLogId = (Integer) req.getAttribute("opLogId");
		OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
		//新增
		if (log != null && action != null && action.equals("save")) {
			String category = StringUtils.trimToNull((String)req.getParameter("category"));
			String code = StringUtils.trimToNull((String)req.getParameter("code"));
			String name = StringUtils.trimToNull((String)req.getParameter("name"));

			if (category == null || code == null || name == null) return; // 不允許空值
			
			String subEntities = (String)req.getParameter("subEntities");
			String values = (String)req.getParameter("entityValues");
			QAEntityType entityType = QAEntityType.valueOf(req.getParameter("entityType"));
			boolean enable = (((String)req.getParameter("enabled")).equals("1")?true:false);
			boolean fromIndex = (((String)req.getParameter("fromIndex")).equals("1")?true:false);
			String refKP = (String)req.getParameter("refKP");
			try {
				QAEntity.save(t.getId(), category, code, name, refKP, subEntities, entityType, values, fromIndex, enable);
				log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
			} catch (Exception e) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.appendToMoreDetails(e.toString());
				infoLog.error(e);
			}
			log.update();
			isDirty = true;
			dirtyCode = code;
		}

		//修改
		if (log != null && action != null && action.equals("update")) {
			String id = (String)xssReq.getParameter("hiddenId");
			String category = StringUtils.trimToNull((String)req.getParameter("category"));
			String code = StringUtils.trimToNull((String)req.getParameter("code"));
			String name = StringUtils.trimToNull((String)req.getParameter("name"));

			if (category == null || code == null || name == null) return; // 不允許空值
			
			String subEntities = (String)req.getParameter("subEntities");
			QAEntityType entityType = QAEntityType.valueOf(req.getParameter("entityType"));
			String values = (String)req.getParameter("entityValues");
			String refKP = (String)req.getParameter("refKP");
			boolean enable = (((String)req.getParameter("enabled")).equals("1")?true:false);
			boolean fromIndex = (((String)req.getParameter("fromIndex")).equals("1")?true:false);
			try {
				QAEntity.update(t.getId(), id, category, code, name, refKP, subEntities, entityType, values, fromIndex, enable);
				log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
			} catch (Exception e) {
				log.setStatusMessage(OperationLogEntity.Status.FAILED);
				log.appendToMoreDetails(e.toString());
				infoLog.error(e);
			}
			log.update();
			isDirty = true;
			dirtyCode = code;
		}

		//刪除
		if (log != null && action != null && action.equals("delete")) {
			String id = (String)xssReq.getParameter("id");
			try {
				QAEntity q = QAEntity.get(new Long(id));
				
				if (q.getTenantId() != t.getId())
					return ;
				
				QAEntity.delete(t.getId(), id);
				dirtyCode = q.getCode();
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
		        		SolrDocumentList docs = qautil.lowLevelSearchCommonSense(QA.FN_EXPAND_ENTITY_CODES, dirtyCode, start, rows);
		        		totalEffect = docs.getNumFound();
		        		if (start == 0) {
		        			System.out.println("QAEntity changed, effected [" + totalEffect + "] docs, now start scheduling for recaluldate alts.");
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
		if (req.getRequestURI().contains("entity/search")) {
			String searchKeyword = (String)req.getParameter("searchKeyword");
			try {
				AuditStatus status = null;
				if (searchKeyword.equals(MessageUtil.getMessage(locale, "global.audit.status.audit"))) {
					status = AuditStatus.AUDIT;
				} else if (searchKeyword.equals(MessageUtil.getMessage(locale, "global.audit.in.force"))) {
					status = AuditStatus.HISTORY;
				}
				
//				results = QAEntity.search(t.getId(), searchKeyword);
				results = QAEntity.fullSearchBySQL(t.getId(), searchKeyword, status);
				req.setAttribute("searchKeyword", searchKeyword);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (req.getRequestURI().contains("entity/list")) {
			String searchKeyword = (String)req.getParameter("searchKeyword");
			try {
				results = QAEntity.search(t.getId(), searchKeyword);
				req.setAttribute("searchKeyword", searchKeyword);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			results = QAEntity.listByTenantId(t.getId());
		}
		
		EntityVersionService service = EntityVersionService.getInstance();
		for (QAEntity res : (List<QAEntity>) results) {
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
//        if (limit.isExported()) {
//            export(tableFacade);
//            return null;
//        } else {
            String html = html(tableFacade, req);
            req.setAttribute("myhtml", html);
//        }

		RequestDispatcher requestdispatch=this.getServletConfig().getServletContext().getRequestDispatcher("/wiseadm/entity.jsp");
		requestdispatch.forward(req, resp);

	}

	 private String html(TableFacade tableFacade, final HttpServletRequest req) {
	        tableFacade.setColumnProperties("id", "category", "code", "name", "subEntities", "entityType", "entityValues", "inAudit", "passDate", "action");
	        tableFacade.autoFilterAndSort(true);
	        HtmlToolbar toolbar = new HtmlToolbar();
	        toolbar.enablePageNumbers(true);
	        tableFacade.setToolbar(toolbar);
	        final Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(req, false, false);
	        String vocabularyList = MessageUtil.getMessage(locale, "entity.list");
	        HtmlTable table = (HtmlTable) tableFacade.getTable();
	        //table.setCaption("詞庫列表");
	        table.setCaption(vocabularyList);
	        table.getTableRenderer().setStyle("width: 1200px;");

	        HtmlRow row = table.getRow();

	        HtmlColumn id = row.getColumn("id");
//	        id.getFilterRenderer().setFilterEditor(new DroplistFilterEditor());
	        id.setTitle("ID");
	        
	        HtmlColumn category = row.getColumn("category");
	        category.setTitle(MessageUtil.getMessage(locale, "category"));
	        
	        HtmlColumn code = row.getColumn("code");
	        code.setTitle(MessageUtil.getMessage(locale, "global.code"));
	        
	        HtmlColumn name = row.getColumn("name");
	        name.setTitle(MessageUtil.getMessage(locale, "global.name"));
	        
	        HtmlColumn entityType = row.getColumn("entityType");
	        entityType.setTitle(MessageUtil.getMessage(locale, "global.entityType"));

	        HtmlColumn subEntities = row.getColumn("subEntities");
	        subEntities.setTitle(MessageUtil.getMessage(locale, "global.subEntities"));

	        HtmlColumn values = row.getColumn("entityValues");
	        values.setTitle(MessageUtil.getMessage(locale, "global.values"));
	        values.getCellRenderer().setCellEditor(new CellEditor() {
	            public Object getValue(Object item, String property, int rowcount) {
	                QAEntity word = (QAEntity) item;
	                HtmlBuilder html = new HtmlBuilder();
	                html.div().append(" style='word-wrap:break-word; width: 500px;'").close().append(word.getEntityValues()).divEnd();
	                return html.toString();
	            }
	        });
	        
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
	        action.setTitle(MessageUtil.getMessage(locale, "operation"));

	        // Using an anonymous class to implement a custom editor.
	        action.getCellRenderer().setCellEditor(new CellEditor() {
	            public Object getValue(Object item, String property, int rowcount) {
	                Object value = new BasicCellEditor().getValue(item, property,
	                        rowcount);
	                HtmlBuilder html = new HtmlBuilder();
	                
	                if(isAudit[0] != 1) {
	//	                Object id = ItemUtils.getItemValue(item, "id");
		                QAEntity word = (QAEntity) item;
		                long id = word.getId();
		                String category = word.getCategory();
		                String code = word.getCode();
		                String name = word.getName();
		                String subEntities = StringUtils.trimToEmpty(word.getSubEntities());
		                QAEntityType entityType = word.getEntityType();
		                String values = StringUtils.trimToEmpty(word.getEntityValues());
		                boolean enabled = word.isEnabled();
		                Boolean fromIndex = word.getFromIndex();
	
		                
		                String js =" onclick='javascript:select(" + id + ",\"" + category + "\",\"" + code + "\",\"" + name + "\",\"" + subEntities + "\",\"" + entityType.name() + "\",\"" + values+ "\",\"" + enabled + "\") '";
		                html.div().quote().close();
		                html.a().append(js).href().quote().append("#").quote().close();
		                String modify = MessageUtil.getMessage(locale, "modify");
		                html.append(modify);
		                html.aEnd();
		                html.divEnd();
		                String js2=" onclick='return del(\"tableId\"," + id + ") '";
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

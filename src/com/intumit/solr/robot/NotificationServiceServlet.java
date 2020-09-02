package com.intumit.solr.robot;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.hazelcast.core.ITopic;
import com.intumit.solr.NotificationEvent;
import com.intumit.solr.NotificationEvent.NotificationType;
import com.intumit.solr.NotificationEvent.StackType;
import com.intumit.solr.NotificationEvent.TargetType;
import com.intumit.solr.servlet.HazelcastUtil;
import com.intumit.solr.tenant.Tenant;

@WebServlet(urlPatterns = {"/nss"}, asyncSupported=true)
public class NotificationServiceServlet extends HttpServlet {

	private static Map<Integer,List<AsyncContext>> clientCtxs = new HashMap<>();

	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final AsyncContext asyncContext = request.startAsync(request, response);
        asyncContext.setTimeout(10 * 60 * 1000);

        Tenant t = Tenant.getFromSession(request.getSession());

        if (t != null) {
        	if (!clientCtxs.containsKey(t.getId())) {
        		clientCtxs.put(t.getId(), new LinkedList<AsyncContext>());
        	}

        	clientCtxs.get(t.getId()).add(asyncContext);
        }
    }

	public static void notifyEvent(NotificationEvent event) {
		try {
			if (event.getTargetType() == TargetType.TENANT) {
				Integer tenantId = (Integer)event.getTarget();
			
				if (clientCtxs.containsKey(tenantId)) {
					List<AsyncContext> asyncContexts = new ArrayList<>(clientCtxs.get(tenantId));
					clientCtxs.get(tenantId).clear();
					JSONObject jobj = new JSONObject();
	
					jobj.put("ntype", event.getStackType().name().toLowerCase());
					jobj.put("title", event.getTitle());
					jobj.put("content", event.getContent());
					jobj.put("type", event.getNotificationType().name().toLowerCase());
					jobj.put("hide", event.isHidden());
	
					for (AsyncContext asyncContext : asyncContexts) {
						asyncContext.getResponse().setContentType("application/json");
						asyncContext.getResponse().setCharacterEncoding("UTF-8");
	
					    try (PrintWriter writer = asyncContext.getResponse().getWriter()) {
					        writer.println(jobj.toString(2));
					        writer.flush();
					        asyncContext.complete();
					    } catch (Exception ex) {
					    }
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 *
	 * @param targetTenants
	 * @param type success / error / info
	 * @param msg
	 */
	public static void broadcast(List<Tenant> targetTenants, String type, String msg) {
		try {
			for (Tenant tenant: targetTenants) {
				if (clientCtxs.containsKey(tenant.getId())) {
				List<AsyncContext> asyncContexts = new ArrayList<>(clientCtxs.get(tenant.getId()));
				clientCtxs.get(tenant.getId()).clear();
				JSONObject jobj = new JSONObject();

				jobj.put("ntype", "stack");
				jobj.put("title", "系統廣播");
				jobj.put("content", msg);

				jobj.put("type", type);

				for (AsyncContext asyncContext : asyncContexts) {
					asyncContext.getResponse().setContentType("application/json");
					asyncContext.getResponse().setCharacterEncoding("UTF-8");

				    try (PrintWriter writer = asyncContext.getResponse().getWriter()) {
				        writer.println(jobj.toString(2));
				        writer.flush();
				        asyncContext.complete();
				    } catch (Exception ex) {
				    }
				}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    public static void doNotification(Tenant t, NotificationType type, String title, String msg) {
    	try {
    		NotificationEvent e = new NotificationEvent();
    		e.setSource(QAAltBuildQueue.class.getName());
    		e.setTargetType(TargetType.TENANT);
    		e.setTarget(t.getId());
    		e.setStackType(StackType.STACK);
    		e.setTitle(title);
    		e.setContent(msg);
    		e.setNotificationType(type);
    
    		ITopic topic = HazelcastUtil.getTopic( "system-notification" );
    		topic.publish(e);
    	}
    	catch (Exception e) {
    		HazelcastUtil.log().error("Cannot publish notification message", e);
    	}
    }
}

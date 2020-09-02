package com.intumit.systemconfig;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.WSUtil;

public class WiseSystemConfigFacade {
	static WiseSystemConfigFacade instance = null;


	WiseSystemConfig cache = null;

	/**
	 * Singleton
	 *
	 * @return
	 */
	public static WiseSystemConfigFacade getInstance() {
		if (instance == null) {
			instance = new WiseSystemConfigFacade();
		}
		return instance;
	}

	private WiseSystemConfigFacade(){

	}

	public WiseSystemConfig createInitValue() {

		WiseSystemConfig bean = new WiseSystemConfig();
		bean.setId(WiseSystemConfig.FIXED_ID);
		bean.setDefaultTenantId(WiseSystemConfig.DEFAULT_TENANT_ID);
		bean.setCoreMax(WiseSystemConfig.DEFAULT_MAX_CORES);
		bean.setDescriptionMax(WiseSystemConfig.DEFAULT_SNIPPET_SIZE);
		bean.setHotKeywordFrom(WiseSystemConfig.DEFAULT_HOT_KEYWORD_FROM);
		bean.setDefaultTenantId(WiseSystemConfig.DEFAULT_TENANT_ID);
		bean.setLbModeEnable(WiseSystemConfig.DEFAULT_LOAD_BALANCE_MODE_ENABLE);
		bean.setvLog(WiseSystemConfig.DEFAULT_LOG_PATH);
		//bean.setEnableFrontEndUser(false);
		bean.setProxyEnable(false);
		//bean.setBancsWsUrl(WSUtil.DEFAULT_BANCS_ENDPOINT);
		//bean.setCardWsUrl(WSUtil.DEFAULT_CARD_ENDPOINT);
		//bean.setFepWsUrl(WSUtil.DEFAULT_FEP_ENDPOINT);
		//bean.setiBankLoginUrl(WiseSystemConfig.DEFAULT_IBANK_LOGIN_URL);
		//bean.setiBankFuncBaseUrl(WiseSystemConfig.DEFAULT_IBANK_FUNC_BASE_URL);
		//bean.setAppUrlTransformPrefix(WiseSystemConfig.DEFAULT_APP_URL_TRANSFORM_PREFIX);
		//bean.setAppIbankUrlTransformPrefix(WiseSystemConfig.DEFAULT_APP_IBANK_URL_TRANSFORM_PREFIX);
		bean.setContextPath("/wise");
		bean.setHostname(null);

		int length = 8;
		String hs = "";

		while (length > 0) {
			hs += StringUtils.leftPad(Integer.toHexString((int)(Math.random() * 65536)), 4, '0');
			length -= 4;
		}
		bean.setSecretHash(hs);

		return bean;
	}

	public WiseSystemConfig loadFromRequest(HttpServletRequest req) {

		WiseSystemConfig bean = get();

		if (req.getParameter("hostname") != null)
			bean.setHostname(StringUtils.trimToNull(req.getParameter("hostname")));
		if (req.getParameter("localPort") != null)
			bean.setLocalPort(StringUtils.trimToNull(req.getParameter("localPort")));
		if (req.getParameter("coreMax") != null)
			bean.setCoreMax(Integer.parseInt(req.getParameter("coreMax")));
		if (req.getParameter("descriptionMax") != null)
			bean.setDescriptionMax(Integer.parseInt(req.getParameter("descriptionMax")));
		if (req.getParameter("hotKeywordFrom") != null)
			bean.setHotKeywordFrom(Integer.parseInt(req.getParameter("hotKeywordFrom")));
		if (req.getParameter("enableFrontEndUser") != null)
			bean.setEnableFrontEndUser(Boolean.parseBoolean(req.getParameter("enableFrontEndUser")));
		if (req.getParameter("lbModeEnable") != null)
			bean.setLbModeEnable(Boolean.parseBoolean(req.getParameter("lbModeEnable")));
		if (req.getParameter("lbModeSwitchable") != null)
			bean.setLbModeSwitchable(Boolean.parseBoolean(req.getParameter("lbModeSwitchable")));
		if (req.getParameter("lbErrorCount") != null)
			bean.setLbErrorCount(Integer.valueOf(req.getParameter("lbErrorCount")));
		if (req.getParameter("allowedReverseProxies") != null)
			bean.setAllowedReverseProxies(req.getParameter("allowedReverseProxies"));
		if (req.getParameter("proxyEnable") != null)
			bean.setProxyEnable(Boolean.parseBoolean(req.getParameter("proxyEnable")));
		if (req.getParameter("proxyHost") != null)
			bean.setProxyHost(StringUtils.trimToNull(req.getParameter("proxyHost")));
		if (req.getParameter("proxyPort") != null)
			bean.setProxyPort(Integer.parseInt(StringUtils.defaultString(StringUtils.trimToNull(req.getParameter("proxyPort")), "3128")));
		if (req.getParameter("mailUsername") != null)
			bean.setMailUsername(req.getParameter("mailUsername"));
		if (StringUtils.trimToNull(req.getParameter("mailPassword")) != null)
			bean.setMailPassword(req.getParameter("mailPassword"));
		if (req.getParameter("mailServerHost") != null)
			bean.setMailServerHost(req.getParameter("mailServerHost"));
		if (req.getParameter("mailServerPort") != null)
			bean.setMailServerPort(Integer.parseInt(req.getParameter("mailServerPort")));
		if (req.getParameter("enableSsl") != null)
			bean.setEnableSsl(Boolean.parseBoolean(req.getParameter("enableSsl")));
		if (req.getParameter("defaultTenantId") != null)
			bean.setDefaultTenantId(Integer.parseInt(req.getParameter("defaultTenantId")));
		if (req.getParameter("vLog") != null)
			bean.setvLog(StringUtils.trimToNull(req.getParameter("vLog")));
		/*
		if (req.getParameter("bancsWsUrl") != null)
			bean.setBancsWsUrl(req.getParameter("bancsWsUrl"));
		if (req.getParameter("cardWsUrl") != null)
			bean.setCardWsUrl(req.getParameter("cardWsUrl"));
		if (req.getParameter("fepWsUrl") != null)
			bean.setFepWsUrl(req.getParameter("fepWsUrl"));
		if (req.getParameter("iBankLoginUrl") != null)
			bean.setiBankLoginUrl(req.getParameter("iBankLoginUrl"));
		if (req.getParameter("iBankFuncBaseUrl") != null)
			bean.setiBankFuncBaseUrl(req.getParameter("iBankFuncBaseUrl"));
		if (req.getParameter("appUrlTransformPrefix") != null)
			bean.setAppUrlTransformPrefix(req.getParameter("appUrlTransformPrefix"));
		if (req.getParameter("appIbankUrlTransformPrefix") != null)
			bean.setAppIbankUrlTransformPrefix(req.getParameter("appIbankUrlTransformPrefix"));
		*/
		if (req.getParameter("contextPath") != null)
			bean.setContextPath(req.getParameter("contextPath"));
		if (req.getParameter("defaultTenant") != null)
			bean.setDefaultTenantId(Integer.parseInt(req.getParameter("defaultTenant")));
		bean.setSsoPermissionCheckUrl(StringUtils.trimToEmpty(req.getParameter("ssoPermissionCheckUrl")));

		return bean;
	}

	public synchronized void update(WiseSystemConfig bean) throws Exception {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(bean);
			tx.commit();
			cache = null;
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	/**
	public List list() {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses =HibernateUtil.getSessionFactory().getCurrentSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(WiseSystemConfig.class);
			result = ct.list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		}

		return result;
	}
	*/

	public WiseSystemConfig get() {
		if (cache == null) {
			WiseSystemConfig result = null;
			Session ses = null;
			try {
				ses = HibernateUtil.getSession();
				Criteria ct = ses.createCriteria(WiseSystemConfig.class);
				ct.add(Restrictions.eq("id", WiseSystemConfig.FIXED_ID));
				result = (WiseSystemConfig)ct.uniqueResult();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				ses.close();
			}

			cache = result;
		}

		return cache;
	}

	public static void main(String[] args){
		WiseSystemConfigFacade imp=new WiseSystemConfigFacade();
		System.out.println(imp.get());
	}

}

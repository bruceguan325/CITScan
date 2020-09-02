package com.intumit.solr.robot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeEnv;

import flexjson.JSONSerializer;

@Entity
public class RobotFormalAnswersSticker {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;

	@Index(name="keyNameIdx")
	private String keyName;

	@Index(name="tenantIdIdx")
	private Integer tenantId;

	private String answers;

	private Integer width;
	private Integer height;

	@Transient
	public static final Integer DEFAULT_SIZE = Integer.valueOf(100);

	public RobotFormalAnswersSticker(Integer tenantId, String key, String answers, Integer width, Integer height) {
		this.tenantId = tenantId;
		this.keyName = key;
		this.answers = answers;
		this.width = width;
		this.height = height;
	}

	public RobotFormalAnswersSticker() {
	}

	public void setKeyName(String column) {
		this.keyName = column;
	}

	public String getKeyName() {
		return keyName;
	}

	public void setAnswers(String name) {
		this.answers = name;
	}

	public String getAnswers() {
		return answers;
	}

	public Integer getWidth() {
		return width == null ? DEFAULT_SIZE : width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return height == null ? DEFAULT_SIZE : height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

	public String serializeToJsonString() {
		JSONSerializer serializer = new JSONSerializer();
		return serializer.serialize(this);
	}

	public static RobotFormalAnswersSticker giveMeAnswer(Integer tenantId, String key) {
		return RobotFormalAnswersSticker.getAnswers(tenantId, key);
	}


	static Map<Integer, Map<String, RobotFormalAnswersSticker>> keyVal = new HashMap<Integer, Map<String, RobotFormalAnswersSticker>>();
	//static List<String> EMPTY_LIST = Arrays.asList(new String[0]);
	public static Map<String, RobotFormalAnswersSticker> getKeyValueMap(Integer tenantId){
		if (!keyVal.containsKey(tenantId)) {
			reload(tenantId);
		}
		return keyVal.get(tenantId);
	}
	public static RobotFormalAnswersSticker getAnswers(Integer tenantId, String key){
		if (!keyVal.containsKey(tenantId)) {
			reload(tenantId);
		}
		if(StringUtils.isEmpty(key)){
			return null;
		}
		return keyVal.get(tenantId).get(key);
	}
	
	public static void saveFile(Tenant tenant, String valValue, String imgString, Integer width, Integer height) {
		try {
			Base64 base64 = new Base64();
			byte[] textByte = (tenant.getId()+"_"+valValue).getBytes("UTF-8");
			String encodedText = base64.encodeToString(textByte);
			OutputStream os = new FileOutputStream(
					WiSeEnv.getHomePath().replace("kernel", "") + "webapps" + File.separator + "wise" + File.separator
							+ "img" + File.separator + "sticker" + File.separator + encodedText);

			os.write(Base64.decodeBase64(StringUtils.substringAfter(imgString, ",")));
			os.flush();
			os.close();

			Map<String, RobotFormalAnswersSticker> columns = new HashMap<String, RobotFormalAnswersSticker>();
			RobotFormalAnswersSticker sticker = new RobotFormalAnswersSticker(tenant.getId(), valValue, encodedText, width, height);
			columns.put(valValue, sticker);
			saveMap(tenant.getId(), columns);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void saveMap(Integer tenantId, Map<String, RobotFormalAnswersSticker> map){
		for(String key : map.keySet()){
			if(StringUtils.isEmpty(key)){
				continue;
			}

			RobotFormalAnswersSticker value = map.get(key);

			if (value == null) {
				delete(tenantId, key);
			} else {
				save(tenantId, value, false);
			}
		}
		reload(tenantId);
	}

	public static RobotFormalAnswersSticker get(Integer tenantId, String key){
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			return (RobotFormalAnswersSticker)ses.createCriteria(RobotFormalAnswersSticker.class).add(Restrictions.eq("tenantId", tenantId)).add(Restrictions.eq("keyName", key)).uniqueResult();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}

	public static void delete(Integer tenantId, String key) {
		Session ses = null;
		Transaction tx = null;
		RobotFormalAnswersSticker cn = get(tenantId, key);
		if(cn==null) return;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.delete(cn);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
			reload(tenantId);
		}
	}
	synchronized static public void reload(Integer tenantId) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			List<RobotFormalAnswersSticker> result = ses.createCriteria(RobotFormalAnswersSticker.class).add(Restrictions.eq("tenantId", tenantId)).list();
			Map<String, RobotFormalAnswersSticker> map = new HashMap<String, RobotFormalAnswersSticker>();
			for(Object o : result){
				RobotFormalAnswersSticker cn = (RobotFormalAnswersSticker)o;
				map.put(cn.getKeyName(), cn);
			}
			keyVal.put(tenantId, map);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
	}
	static void save(Integer tenantId, RobotFormalAnswersSticker sticker, boolean reload){
		RobotFormalAnswersSticker cn = get(tenantId, sticker.getKeyName());
		if (cn == null) {
			cn = sticker;
		}
		else {
			cn.setAnswers(sticker.getAnswers());
			cn.setWidth(sticker.getWidth());
			cn.setHeight(sticker.getHeight());
		}

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(cn);
			tx.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
			if(reload)
				reload(tenantId);
		}
	}

	static void save(Integer tenantId, RobotFormalAnswersSticker sticker){
		save(tenantId, sticker, true);
	}

}

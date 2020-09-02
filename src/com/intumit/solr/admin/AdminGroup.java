package com.intumit.solr.admin;

import java.io.BufferedReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.util.WiSeEnv;

import flexjson.JSONSerializer;

@Entity
public class AdminGroup implements Serializable {
	
	public static Logger log = Logger.getLogger(AdminGroup.class.getName());
	
	private static final String FILE_NAME = "url-group-key-config.json";
	private static Path filePath = Paths
			.get(new StringBuilder().append(WiSeEnv.getHomePath()).append("/").append(FILE_NAME).toString());;
	
	public static final HashMap<String, String> urlPatternMaps = new HashMap<String, String>();
	public static final HashMap<String, String> translateMaps = new HashMap<String, String>();
	
	private static JSONObject urlGroupKeyConfig = new JSONObject();
	
	public static final String[] fixUrlPatternKey = { "QAApiAccess" };
	
	public static final int C = 0x0001;
	public static final int U = 0x0002;
	public static final int R = 0x0004;
	public static final int D = 0x0008;
	
	public static final int E1 = 0x0010; // 擴充1
	public static final int E2 = 0x0020; // 擴充2
	public static final int E3 = 0x0040; // 擴充3
	public static final int E4 = 0x0080; // 擴充4
	
	public static final int A1 = 0x0100; // 附加1
	public static final int A2 = 0x0200; // 附加2
	public static final int A3 = 0x0400; // 附加3
	public static final int A4 = 0x0800; // 附加4
	
	public static final int O1 = 0x1000; // 其他1
	public static final int O2 = 0x2000; // 其他2
	public static final int O3 = 0x4000; // 其他3
	public static final int O4 = 0x8000; // 其他4
	public static final int O5 = 0x10000; // 知識點審核
	public static final int O6 = 0x20000; // 匯出EAT
	public static final int O7 = 0x40000; // 知識點瀏覽
	public static final int O8 = 0x80000; // 生活用語
	public static final int E5 = 0x100000; // QA匯出
	
	public static final int SUPER_GROUP_ID = 1;
	public static final int SUPER_GROUP_PERM = C | U | R | D | E1 | E2 | E3 | E4 | A1 | A2 | A3 | A4 | O1 | O2 | O3 | O4 | O5 | O6 | O7 | O8 | E5;
	
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;
	
	private String name;
	
	private int coreAdminCURD;
	private int indexAdminCURD;
	private int dataSetAdminCURD;
	private int statisticsAdminCURD;
	private int adphraseAdminCURD;
	private int systemAdminCURD;
	private int adminAdminCURD;

	public AdminGroup() {
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Lob
	private String urlPatternAdminCRUD;
	@Transient
	private JSONObject urlPatternAdminCRUDJson = new JSONObject();

	public void ORoperation2(AdminGroup group) {
		try {
			this.urlPatternAdminCRUDJson = (group.urlPatternAdminCRUD == null ? defaultPermission() : new JSONObject(group.urlPatternAdminCRUD));
		} catch (JSONException e) {
			this.urlPatternAdminCRUDJson = defaultPermission();
		}
	}
	
	private JSONObject defaultPermission() {
		try {
			JSONObject defaultPermission = new JSONObject();
			for (String key : fixUrlPatternKey) {
				defaultPermission.put(key, SUPER_GROUP_PERM);
			}
			return defaultPermission;
		} catch (JSONException e) {
			return new JSONObject();
		}
	}
	
	public JSONObject getUrlPatternAdminCRUD() {
		return urlPatternAdminCRUDJson;
	}
	
	public int getCoreAdminCURD() {
		return coreAdminCURD;
	}

	public void setCoreAdminCURD(int coreAdminCURD) {
		this.coreAdminCURD = coreAdminCURD;
	}

	public int getIndexAdminCURD() {
		return indexAdminCURD;
	}

	public void setIndexAdminCURD(int indexAdminCURD) {
		this.indexAdminCURD = indexAdminCURD;
	}

	public int getDataSetAdminCURD() {
		return dataSetAdminCURD;
	}

	public void setDataSetAdminCURD(int dataSetAdminCURD) {
		this.dataSetAdminCURD = dataSetAdminCURD;
	}

	public int getStatisticsAdminCURD() {
		return statisticsAdminCURD;
	}

	public void setStatisticsAdminCURD(int statisticsAdminCURD) {
		this.statisticsAdminCURD = statisticsAdminCURD;
	}

	public int getAdphraseAdminCURD() {
		return adphraseAdminCURD;
	}

	public void setAdphraseAdminCURD(int adphraseAdminCURD) {
		this.adphraseAdminCURD = adphraseAdminCURD;
	}

	public int getSystemAdminCURD() {
		return systemAdminCURD;
	}

	public void setSystemAdminCURD(int systemAdminCURD) {
		this.systemAdminCURD = systemAdminCURD;
	}

	public int getAdminAdminCURD() {
		return adminAdminCURD;
	}

	public void setAdminAdminCURD(int adminAdminCURD) {
		this.adminAdminCURD = adminAdminCURD;
	}

	public void ORoperation(AdminGroup group) {
		this.coreAdminCURD |= group.coreAdminCURD;
		this.indexAdminCURD |= group.indexAdminCURD;
		this.dataSetAdminCURD |= group.dataSetAdminCURD;
		this.statisticsAdminCURD |= group.statisticsAdminCURD;
		this.adphraseAdminCURD |= group.adphraseAdminCURD;
		this.systemAdminCURD |= group.systemAdminCURD;
		this.adminAdminCURD |= group.adminAdminCURD;
	}
	
	public String serializeToJsonString() {
		JSONSerializer serializer = new JSONSerializer();
		return serializer.serialize(this);
	}
	
	public JSONObject getGroupAdminCRUD() {
		try {
			return urlPatternAdminCRUD == null ? defaultPermission() : new JSONObject(urlPatternAdminCRUD);
		} catch (JSONException e) {
			return defaultPermission();
		}
	}
	
	public void setUrlPatternAdminCRUD(String urlPatternAdminCRUD) {
		this.urlPatternAdminCRUD = urlPatternAdminCRUD;
	}
	
	public static Integer checkAdminVal(boolean isAdmin) {
		return isAdmin ? SUPER_GROUP_PERM : 0;
	}
	
	private static void reloadConfig() {
		if (Files.exists(filePath)) {
			
			urlPatternMaps.clear();
			translateMaps.clear();
			
			StringBuilder sb = new StringBuilder();
			try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
				urlGroupKeyConfig = new JSONObject(sb.toString());
				for (Object categoryKey : urlGroupKeyConfig.keySet()) {
					JSONObject categoryObj = urlGroupKeyConfig.optJSONObject(categoryKey.toString(), new JSONObject());
					for (Object key : categoryObj.keySet()) {
						JSONArray urls = categoryObj.optJSONArray(key.toString(), new JSONArray());
						for (int i = 0; i < urls.length(); i++) {
							Object obj = urls.get(i);
							if (obj instanceof String) {
								urlPatternMaps.put(obj.toString(), key.toString());
							} else if (obj instanceof JSONObject) {
								translateMaps.put(key.toString(), ((JSONObject)obj).optString("translate_key"));
							}
						}
					}
				}
			} catch (Exception e) {
				log.error(e);
			}
		}
	}
}

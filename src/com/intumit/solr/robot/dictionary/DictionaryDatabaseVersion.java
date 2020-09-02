package com.intumit.solr.robot.dictionary;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.hibernate.annotations.Index;

import com.intumit.solr.AuditAction;
import com.intumit.solr.robot.AuditStatus;

@Entity
public class DictionaryDatabaseVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Index(name = "tenantIdIdx")
	private Integer tenantId;

	@Index(name = "keyword")
	private String keyword;

	@Index(name = "purposes")
	private String purposes;

	@Index(name = "enabled")
	private boolean enabled;

	@Index(name = "category")
	private String category;

	@Index(name = "enableQaScopeRestriction")
	private Boolean enableQaScopeRestriction;

	@Column(nullable = false)
	private Integer editorId;

	@Transient
	private String editorName;

	private Integer auditorId;
	
	@Transient
	private String auditorName;

	@Column(nullable = false)
	private AuditStatus status;

	@Column(nullable = false)
	private AuditAction action;

	@Column(nullable = false)
	private Date createTime;

	@Column(nullable = false)
	private Date updateTime;
	
	@Lob
	private String updateLog;

	private Date passTime;

	@Column(nullable = false)
	private int version;

	private Long publicId;

	@Column(length = 256)
	private String message;

	public DictionaryDatabaseVersion() {
	}

	public DictionaryDatabaseVersion(String keyword, String purposes, String category, boolean enabled,
			boolean enableQaScopeRestriction) {
		this.keyword = keyword;
		this.purposes = purposes;
		this.category = category;
		this.enabled = enabled;
		this.enableQaScopeRestriction = enableQaScopeRestriction;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getPurposes() {
		return purposes;
	}

	public void setPurposes(String purposes) {
		this.purposes = purposes;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Boolean getEnableQaScopeRestriction() {
		return enableQaScopeRestriction;
	}

	public void setEnableQaScopeRestriction(Boolean enableQaScopeRestriction) {
		this.enableQaScopeRestriction = enableQaScopeRestriction;
	}

	public Integer getEditorId() {
		return editorId;
	}

	public void setEditorId(Integer editorId) {
		this.editorId = editorId;
	}

	public String getEditorName() {
		return editorName;
	}

	public void setEditorName(String editorName) {
		this.editorName = editorName;
	}

	public Integer getAuditorId() {
		return auditorId;
	}

	public void setAuditorId(Integer auditorId) {
		this.auditorId = auditorId;
	}

	public String getAuditorName() {
		return auditorName;
	}

	public void setAuditorName(String auditorName) {
		this.auditorName = auditorName;
	}

	public AuditStatus getStatus() {
		return status;
	}

	public void setStatus(AuditStatus status) {
		this.status = status;
	}

	public AuditAction getAction() {
		return action;
	}

	public void setAction(AuditAction action) {
		this.action = action;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getUpdateLog() {
		return updateLog;
	}

	public void setUpdateLog(String updateLog) {
		this.updateLog = updateLog;
	}

	public void addUpdateLog() {
		SimpleDateFormat SDF = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		JSONArray jsonArr;
		try {
			jsonArr = new JSONArray(getUpdateLog());
		} catch (Exception e) {
			jsonArr = new JSONArray();
		}
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("editorId", getEditorId());
			jsonObj.put("editorName", getEditorName());
			jsonObj.put("updateTime", SDF.format(getUpdateTime()));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		jsonArr.add(jsonObj);
		setUpdateLog(jsonArr.toString());
	}
	
	public boolean checkNotEditor(Integer userId) {
		JSONArray jsonArr;
		try {
			jsonArr = new JSONArray(updateLog);
			for (int i = 0; i < jsonArr.length(); i++) {
				if (userId.intValue() == jsonArr.getJSONObject(i).optInt("edtiorId", 0)) {
					return false;
				}
			}
		} catch (Exception e) {
			return userId.intValue() != editorId.intValue();
		}
		return true;
	}

	public Date getPassTime() {
		return passTime;
	}

	public void setPassTime(Date passTime) {
		this.passTime = passTime;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public Long getPublicId() {
		return publicId;
	}

	public void setPublicId(Long publicId) {
		this.publicId = publicId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}

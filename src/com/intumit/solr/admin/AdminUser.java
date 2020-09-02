package com.intumit.solr.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.intumit.solr.util.WiSeUtils;
import com.intumit.systemconfig.WiseSystemConfigFacade;

import flexjson.JSONSerializer;

@Entity
public class AdminUser implements Serializable {
	
	static Logger logManager = LogManager.getLogger(AdminUser.class.getName());
	
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;
	
	private String name;
	private String loginName;
	private String password;
	private String adminGroups;
	private String tenantIds;
	private String email;
	private String department;
	
	private boolean disabled;
	
	@Transient
	private boolean superAdmin = false;

	public AdminUser() {
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

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAdminGroups() {
		return adminGroups;
	}
	
	public void setAdminGroups(String adminGroups) {
		this.adminGroups = adminGroups;
	}

	public void setTenantIds(String tenantIds) {
		this.tenantIds = tenantIds;
	}

	public String getTenantIds() {
		return tenantIds;
	}
	
	public boolean isSuperAdmin() {
		return superAdmin;
	}
	public void setSuperAdmin(boolean superAdmin) {
		this.superAdmin = superAdmin;
	}

	public Set<Integer> getTenantIdSet() {
		Set<Integer> set = new HashSet<Integer>();
		
		if (StringUtils.trimToNull(tenantIds) != null) {
			for (String idStr: StringUtils.split(tenantIds, ",")) {
				set.add(new Integer(idStr));
			}
		}
		return set;
	}
	
	public AdminGroup getDefaultGroup() {
		String defaultAGID = getAdminGroups() != null ? StringUtils.split(getAdminGroups(), ",")[0] : null;
		AdminGroup defaultAG = defaultAGID != null ? AdminGroupFacade.getInstance().get(Integer.parseInt(defaultAGID)) : null;
		
		return defaultAG;
	}

	
	/**
	 * 
	 * @return empty collection even no admingroup
	 */
	public Collection<AdminGroup> toAdminGroupCollection() {
		ArrayList<AdminGroup> admGrpList = new ArrayList<AdminGroup>();
		if (adminGroups != null) {
			String[] arr = adminGroups.split(",");
			for (int i=0; i < arr.length; i++) {
				if (StringUtils.isEmpty(arr[i]))
					continue;
				Integer agId = new Integer(arr[i]);
				try {
					AdminGroup ag = AdminGroupFacade.getInstance().get(agId);
					admGrpList.add(ag);
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return admGrpList;
	}
	
	public String serializeToJsonString() {
		JSONSerializer serializer = new JSONSerializer();
		return serializer.serialize(this);
	}
	
	public static String encryptPassword(String plainPwd) {
		String secretHash = WiseSystemConfigFacade.getInstance().get().getSecretHash();
		
		if (secretHash == null)
			return plainPwd;
		
		String encrypted = WiSeUtils.sha256(plainPwd + secretHash);
		logManager.error("[" + plainPwd + secretHash + "] => " + encrypted);
		return encrypted;
	}
	
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}
	
	public static String getPrintableName(int id) {
		AdminUser u = AdminUserFacade.getInstance().get(id);
		String name = "Unknown";
		
		if (u != null) {
			name = u.getName();
		}
		
		return name + " (" + id + ")";
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
}

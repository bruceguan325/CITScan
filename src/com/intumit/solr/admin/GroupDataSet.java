package com.intumit.solr.admin;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.intumit.solr.dataset.DataSet;

import flexjson.JSONSerializer;



@Entity
@Table(name="GroupDataSet", uniqueConstraints = {@UniqueConstraint(columnNames={"groupId", "dataSetId"})})
public class GroupDataSet implements Serializable {
	public static final int C = 0x0001;
	public static final int U = 0x0002;
	public static final int R = 0x0004;
	public static final int D = 0x0008;
	public static final int E = 0x0010; // 審核
	public static final int O1 = 0x1000; // 附件權限
	public static final int O2 = 0x2000; // 其他2
	public static final int O3 = 0x4000; // 其他3
	public static final int O4 = 0x8000; // 其他4
	
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;
    int groupId;
    int dataSetId;
	int dataSetViewAdminCURD;

	public GroupDataSet() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getAdminGroupId() {
         return groupId;
    }

	public void setAdminGroupId(int adminGroupId) {
		this.groupId = adminGroupId;
	}

	public int getDataSetId() {
         return dataSetId;
    }

	public void setDataSetId(int dataSetId) {
		this.dataSetId = dataSetId;
	}

	public void setDataSetViewAdminCURD(int dataSetViewAdminCURD) {
		this.dataSetViewAdminCURD = dataSetViewAdminCURD;
	}

	public int getDataSetViewAdminCURD() {
		return dataSetViewAdminCURD;
	}


	
	public String serializeToJsonString() {
		JSONSerializer serializer = new JSONSerializer();
		return serializer.serialize(this);
	}
}

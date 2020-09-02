package com.intumit.solr.robot;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.wink.json4j.JSONObject;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.connector.line.RichMessageServlet;
import com.microsoft.ttshttpoxford.sample.TTSService;

/**
 * 圖片實體檔透過DB存取
 * 
 * @author dudamel
 *
 */
@Entity
public class RobotImageFile implements Serializable {

	public static Logger log = Logger.getLogger(RobotImageFile.class.getName());

	private static final long serialVersionUID = 1L;

	private static final HashMap<String, String> paths = new HashMap<>();

	static {
		//paths.put(RobotCkeditorUpload.class.getName(), "image.file.ckeditor");
		paths.put(RobotFormalAnswersSticker.class.getName(), "qa.formal.answer.sticker");
		paths.put(com.intumit.solr.robot.connector.web.RichMessage.class.getName(), "qa.richmessage.navbar.web");
		paths.put(com.intumit.solr.robot.connector.line.RichMessage.class.getName(), "qa.richmessage.navbar.line");

		paths.put(com.intumit.solr.robot.connector.web.RichMenu.class.getName(), "qa.richmenu.navbar.web");
		
		paths.put(TTSService.class.getName(), "image.file.texttospeech");
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;

	@Index(name = "tenantIdIdx")
	Integer tenantId;

	@Column(length = 128)
	@Index(name = "targetObjIdx")
	String targetObj;

	String targetId;

	@Column(length = 128)
	@Index(name = "mkeyIdx")
	String mkey;

	@Column(length = 128)
	@Index(name = "namespaceIdx")
	String namespace;

	@Lob
	Blob fileBody;

	/*
	 * used to record the image/file have been access by hostname, if host do not
	 * exist, local file should be delete. the structure is {"host1" : true, "host2"
	 * : false} the value of host1 is true means host1 should clean local files
	 */
	String markForClean = "{}";

	public RobotImageFile() {
	}

	public RobotImageFile(int tid, String namespace, String targetObj, String mkey, Blob fileBody) {
		this.tenantId = tid;
		this.targetObj = targetObj;
		this.namespace = namespace;
		this.mkey = mkey;
		this.fileBody = fileBody;
		save(this);
	}

	public RobotImageFile(int tid, String namespace, String targetObj, String mkey, String targetId, Blob fileBody) {
		this.tenantId = tid;
		this.namespace = namespace;
		this.targetObj = targetObj;
		this.mkey = mkey;
		this.targetId = targetId;
		this.fileBody = fileBody;
		save(this);
	}

	public static RobotImageFile getBy(Integer tenantId, String mkey, String targetObj, String namespace,
			String targetId) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(RobotImageFile.class);
			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (mkey != null) {
				ct.add(Restrictions.eq("mkey", mkey));
			}
			if (targetObj != null) {
				ct.add(Restrictions.eq("targetObj", targetObj));
			}
			if (namespace != null) {
				ct.add(Restrictions.eq("namespace", namespace));
			}
			if (targetId != null) {
				ct.add(Restrictions.eq("targetId", targetId));
			}
			return (RobotImageFile) ct.uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public static List<RobotImageFile> list(int tenantId, String targetObj) {
		List<RobotImageFile> result = new ArrayList<RobotImageFile>();

		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(RobotImageFile.class).add(Restrictions.eq("tenantId", tenantId))
					.add(Restrictions.eq("targetObj", targetObj));
			ct.addOrder(Order.asc("targetObj")).addOrder(Order.asc("id"));
			return ct.list();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static synchronized List<RobotImageFile> listBy(Integer tenantId, int start, int rows, String targetObj) {
		List<RobotImageFile> result = new ArrayList<RobotImageFile>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(RobotImageFile.class).addOrder(Order.asc("targetObj"))
					.addOrder(Order.asc("id"));
			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (targetObj != null) {
				ct.add(Restrictions.eq("targetObj", targetObj));
			}
			result = ct.setFirstResult(start).setMaxResults(rows).list();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return result;
	}

	public static synchronized Number countBy(Integer tenantId, String targetObj) {
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(RobotImageFile.class);
			if (tenantId != null) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (targetObj != null) {
				ct.add(Restrictions.eq("targetObj", targetObj));
			}
			return (Number) ct.setProjection(Projections.rowCount()).uniqueResult();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}

		return 0;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}

	public String getTargetObj() {
		return targetObj;
	}

	public void setTargetObj(String targetObj) {
		this.targetObj = targetObj;
	}

	public String getMkey() {
		return mkey;
	}

	public void setMkey(String mkey) {
		this.mkey = mkey;
	}

	public Blob getFileBody() {
		return fileBody;
	}

	public void setFileBody(Blob fileBody) {
		this.fileBody = fileBody;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getTargetId() {
		return targetId;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	public static String getPath(String path) {
		if (paths.containsKey(path)) {
			return paths.get(path);
		}
		return null;
	}

	public static Boolean isPathExist(String path) {
		if (paths.containsKey(path)) {
			return true;
		}
		return false;
	}

	public static HashMap<String, String> getPaths() {
		return paths;
	}

	public static synchronized void save(RobotImageFile imgFile) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			
			// 儲存之前先確認無重複的圖片
			RobotImageFile check = RobotImageFile.getBy(imgFile.getTenantId(), imgFile.getMkey(),
				imgFile.getTargetObj(), imgFile.getNamespace(), imgFile.getTargetId());
			if (check == null) {
				ses.saveOrUpdate(imgFile);
				tx.commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public void delete() {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.delete(this);
			tx.commit();

			String nameSpace = this.getNamespace().replace("/", File.separator);
			Path path = Paths.get(new StringBuilder().append(RobotImageFilePath.getOldPath()).append(File.separator)
					.append(nameSpace).append(File.separator).append(this.getMkey()).toString());
			Path newPath = Paths.get(new StringBuilder().append(RobotImageFilePath.getNewPath()).append(File.separator)
					.append(nameSpace).append(File.separator).append(this.getMkey()).toString());

			if (Files.exists(path)) {
				Files.delete(path);
			}
			if (Files.exists(newPath)) {
				Files.delete(newPath);
			}
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static List<RobotImageFile> listDeleteBy(Integer tenantId, String targetObj, String targetId,
			boolean delete) {
		List<RobotImageFile> result = new ArrayList<RobotImageFile>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(RobotImageFile.class).add(Restrictions.eq("tenantId", tenantId))
					.add(Restrictions.eq("targetObj", targetObj)).add(Restrictions.eq("targetId", targetId));
			ct.addOrder(Order.asc("targetObj")).addOrder(Order.asc("id"));
			result = ct.list();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		if (delete && result.size() > 0) {
			String[] baseUrlSizeCollection = RichMessageServlet.getBaseUrlSizeCollection();
			boolean deleteFolder = false;
			String folder = "";
			for (RobotImageFile imgFile : result) {
				imgFile.delete();
				if (StringUtils.equalsAny(imgFile.getMkey(), baseUrlSizeCollection) && !deleteFolder) {
					deleteFolder = true;
					folder = imgFile.getNamespace();
				}
			}
			if (deleteFolder) {
				deleteFolderPath(folder);
			}
		}
		return result;
	}

	public static void deleteFolderPath(String folder) {
		folder = folder.replace("/", File.separator);
		Path path = Paths.get(new StringBuilder().append(RobotImageFilePath.getOldPath()).append(File.separator)
				.append(folder).toString());
		Path newPath = Paths.get(new StringBuilder().append(RobotImageFilePath.getNewPath()).append(File.separator)
				.append(folder).toString());

		try {
			if (Files.isDirectory(path)) {
				Files.delete(path);
			}
			if (Files.isDirectory(newPath)) {
				Files.delete(newPath);
			}
		} catch (Exception e) {
			log.log(Level.ERROR, "ERROR in delete folder : " + e);
		}
	}

	public String getMarkForClean() {
		return markForClean == null ? "{}" : markForClean;
	}

	public void setMarkForClean(String markForClean) {
		this.markForClean = markForClean == null ? "{}" : markForClean;
	}

	public static Boolean checkHostMarkForClean(String hostname, RobotImageFile imgFile) {
		Boolean markToClean = false;
		try {
			if (imgFile != null) {
				JSONObject m = new JSONObject(imgFile.getMarkForClean());
				Boolean isHostExist = m.containsKey(hostname);
				// 若host不存在markToClean需回傳true要清掉local圖檔，但markForClean註記false以免下次又清
				if (isHostExist) {
					markToClean = m.optBoolean(hostname);
				} else {
					m.put(hostname, false);
					imgFile.setMarkForClean(m.toString());
					RobotImageFile.save(imgFile);
					markToClean = true;
				}
			} else {
				// 若imgFile不存在markToClean需回傳false，之後建立物件時markForClean會是空的
				markToClean = false;
			}
		} catch (Exception e) {
			log.error(e);
		}
		return markToClean;
	}
	
	@SuppressWarnings("deprecation")
	public static void updateMkeyInfo(String oldMkey, String newMkey, String targetObj, String namespace,
			Integer tenantId) {
		Connection con = null;
		PreparedStatement pstmt = null;
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			con = ses.connection();
			con.setAutoCommit(false);
			String sql = "UPDATE RobotImageFile set targetId = ? where targetId = ? and targetObj = ? and namespace LIKE ? and tenantId = ?";
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, newMkey);
			pstmt.setString(2, oldMkey);
			pstmt.setString(3, targetObj);
			pstmt.setString(4, namespace + "%");
			pstmt.setInt(5, tenantId);
			pstmt.executeUpdate();
			con.commit();
		} catch (SQLException e) {
			if (con != null)
				try {
					con.rollback();
					throw e;
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			e.printStackTrace();
		} finally {
			ses.close();
		}
	}

}

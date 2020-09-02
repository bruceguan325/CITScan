package com.intumit.solr.robot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.apache.log4j.Logger;
import org.elasticsearch.common.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.dto.MultiChannelAnswerDto;
import com.intumit.solr.robot.qaplugin.CustomQA;
import com.intumit.solr.robot.qaplugin.HierarchicalQA;
import com.intumit.solr.robot.qaplugin.QADialogPlugin;

@Entity
public class MultiChannelAnswer {
	
	public static Logger log = Logger.getLogger(MultiChannelAnswer.class.getName());
	
	public static final String ROBOT_ANSWER_GENERAL = "general";
	public static final String ROBOT_ANSWER_ADVANCE = "advance";
	
	public static final String ROBOT_MOOD_CURIOUS = "curious";
	public static final String ROBOT_MOODR_HAPPY = "happy";
	public static final String ROBOT_MOOD_JOY = "joy";
	
	public static final String LINE_ANSWER_TEXT = "line_answer_text";
	public static final String LINE_ANSWER_RICH_MESSAGE = "line_answer_rich_message";
	public static final String LINE_ANSWER_MULTIPLE_RICH_MESSAGES = "line_answer_multiple_rich_messages";  
	
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;
	
	@Index(name="tenantIdIdx")
	private int tenantId;
	
	@Index(name="qaIdIdx")
	private String qaId;
	
	// web / app / voice
	@Index(name="channelIdx")
	private String channel;
	
	// unknown / vip / internal
	@Index(name="userTypeIdx")
	private String userType;
	
	@Lob
	private String answer;
	
	private String answerType;
	
	private String answerVoice;
	
	private String answerMood;
	
	/**
	 * 針對 ChannelType 專屬的相關設定值，需為 JSON 格式
	 */
	@Lob
	private String channelTypeConfig;
	
	/**
	 * 額外附加參數，需為 JSON 格式
	 */
	@Lob
	private String extraParameters;
	
	
	/**
	 * 為了統計使用的標籤，用逗號分隔
	 */
	@Lob
	private String tags;
	
	private boolean disable;

	@Index(name="lastUpdated")
    private Date lastUpdated;
	
	/**
	 * 這個已經改成通用 rich message mkey，為了不動到既有客戶 DB 保持 lineMKey 名稱
	 */
	private String lineMKey;
    
	public MultiChannelAnswer(int tenantId, String qaId, String channel, String userType, String answer) {
		this.tenantId = tenantId;
		this.qaId = qaId;
		this.channel = channel;
		this.userType = userType;
		this.answer = answer;
	}
	
	public MultiChannelAnswer() {
	}
	
	public MultiChannelAnswer(int tenantId, String qaId, String channel, String userType, String answer,
			String answerType, String answerVoice, String answerMood) {
		this.tenantId = tenantId;
		this.qaId = qaId;
		this.channel = channel;
		this.userType = userType;
		this.answer = answer;
		this.answerType = answerType;
		this.answerVoice = answerVoice;
		this.answerMood = answerMood;
	}
	
	public static MultiChannelAnswer fromDto(MultiChannelAnswerDto dto) {
		MultiChannelAnswer mca = new MultiChannelAnswer();
		mca.tenantId = dto.getTenantId();
		mca.qaId = dto.getQaId();
		mca.channel = dto.getChannel();
		mca.userType = dto.getUserType();
		mca.answer = dto.getAnswer();
		mca.answerType = dto.getAnswerType();
		mca.answerVoice = dto.getAnswerVoice();
		mca.answerMood = dto.getAnswerMood();
		mca.lineMKey = dto.getLineMKey();
		return mca;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getTenantId() {
		return tenantId;
	}
	public void setTenantId(int tenantId) {
		this.tenantId = tenantId;
	}
	public String getQaId() {
		return qaId;
	}
	public void setQaId(String qaId) {
		this.qaId = qaId;
	}
	public String getChannel() {
		return channel;
	}
	public void setChannel(String channel) {
		this.channel = channel;
	}
	public QAChannel getQAChannel() {
		if (channel != null) {
			return QAChannel.get(tenantId, channel);
		}
		return null;
	}
	public String getUserType() {
		return userType;
	}
	public void setUserType(String userType) {
		this.userType = userType;
	}
	public String getAnswer() {
		return answer;
	}
	public void setAnswer(String answer) {
		this.answer = answer;
	}
	public String getChannelTypeConfig() {
		return channelTypeConfig;
	}
	public void setChannelTypeConfig(String channelTypeConfig) {
		this.channelTypeConfig = channelTypeConfig;
	}
	public String getExtraParameters() {
		return extraParameters;
	}
	public void setExtraParameters(String extraParameters) {
		this.extraParameters = extraParameters;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public boolean isDisable() {
		return disable;
	}
	public void setDisable(boolean disable) {
		this.disable = disable;
	}
	
    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

	@Override
	public String toString() {
		return "MultiChannelAnswer [id=" + id + ", tenantId=" + tenantId + ", qaId=" + qaId + ", channel=" + channel
				+ ", userType=" + userType + ", answer=" + answer + ", channelTypeConfig=" + channelTypeConfig
				+ ", extraParameters=" + extraParameters + ", tags=" + tags + ", disable=" + disable + ", lastUpdated="
				+ lastUpdated + "]";
	}

	public static MultiChannelAnswer get(int tenantId, String qaId, String channel, String userType){
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			List<MultiChannelAnswer> mcas = ses
					.createCriteria(MultiChannelAnswer.class)
					.add(Restrictions.eq("tenantId", tenantId))
					.add(Restrictions.eq("qaId", qaId))
					.add(Restrictions.eq("channel", channel))
					.add(Restrictions.eq("userType", userType))
					.add(Restrictions.ne("disable", true)).list();
			if (mcas.size()<1)
				return null;
			return mcas.get(0);
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}
	
	public static void delete(MultiChannelAnswer mca) {
		Session ses = null;
		Transaction tx = null;
		if(mca==null) return;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.delete(mca);
			tx.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
	}
	
	public static MultiChannelAnswer save(int tenantId, String qaId, String channel, String userType, String answer) {
		return save(tenantId, qaId, channel, userType, answer, null);
	}
	
	public static MultiChannelAnswer save(int tenantId, String qaId, String channel, String userType, String answer, String lineMKey){
		MultiChannelAnswer mca = get(tenantId, qaId, channel, userType);
		if (mca == null) {
			mca = new MultiChannelAnswer(tenantId, qaId, channel, userType, answer);
		}
		if (answer != null)
			mca.setAnswer(answer);
		
		if (lineMKey != null)
			mca.setLineMKey(lineMKey);
		
		mca.setLastUpdated(new Date());

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(mca);
			tx.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		
		return mca;
	}
	
	public static void saveOrUpdate(MultiChannelAnswer mca) {
		Session ses = null;
		Transaction tx = null;
		try {
			mca.setLastUpdated(new Date());
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(mca);
			tx.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
	}
	
	public static MultiChannelAnswer save(int tenantId, String qaId, String channel, String userType, String answer,
			String answerType, String answerVoice, String answerMood) {
		MultiChannelAnswer mca = get(tenantId, qaId, channel, userType);
		if (mca == null) {
			mca = new MultiChannelAnswer(tenantId, qaId, channel, userType, answer, answerType, answerVoice,
					answerMood);
		} else {
			mca.setAnswer(answer);
			mca.setAnswerType(answerType);
			mca.setAnswerVoice(answerVoice);
			mca.setAnswerMood(answerMood);
		}

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(mca);
			tx.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		
		return mca;
	}
	
	public static MultiChannelAnswer save(MultiChannelAnswerDto dto) {
		MultiChannelAnswer mca = get(dto.getTenantId(), dto.getQaId(), dto.getChannel(), dto.getUserType());
		if (mca == null) {
			mca = MultiChannelAnswer.fromDto(dto);
		} else {
			mca.setAnswer(dto.getAnswer());
			mca.setAnswerType(dto.getAnswerType());
			mca.setAnswerVoice(dto.getAnswerVoice());
			mca.setAnswerMood(dto.getAnswerMood());
			mca.setLineMKey(dto.getLineMKey());
			mca.setChannelTypeConfig(dto.getChannelTypeConfig());
		}

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(mca);
			tx.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		
		return mca;
	}
	
	/**
	 * 這邊需要嚴謹的判斷這個 MultiChannelAnswer 到底是不是有被設定
	 * 判斷邏輯的依序為
	 * 1. mkey 有沒有設定？	（單圖文）
	 * 2. channelTypeConfig 有沒有設定？	（多圖文）
	 * 3. answer 是不是空的？ （無圖文）
	 * 
	 * @return
	 */
	public boolean isEmpty() {
    	if (StringUtils.isNotBlank(getRichMessageMKey())) {
			return false;
		}

    	if (StringUtils.isNotBlank(getChannelTypeConfig())) {
    		return false;
		}
    	
    	if (StringUtils.isNotBlank(getAnswer())) {
    		return false;
    	}
    	
    	return true;
	}
	
	public static String channelToAnswer(int tenantId, String qaId, String channel, String userType) {
		MultiChannelAnswer mca = get(tenantId, qaId, channel, userType);
		if (mca==null) {
			return "";
		}
		return mca.getAnswer();
	}
	
	/**
	 * 同一個 Channel 內找答案（就是本 UserType 沒有找 Default User Type）
	 * 
	 * @param tenantId
	 * @param qaId
	 * @param channel
	 * @param userType
	 * @return
	 */
	private static MultiChannelAnswer findAnswerInChannel(QAContext ctx, String qaId, QAChannel channel, String userType) {
		MultiChannelAnswer mca = get(ctx.getTenant().getId(), qaId, channel.getCode(), userType);
		if (mca == null) {
			// 如果不是 default user type，就找 default user type 有沒有答案
			if (!StringUtils.equals(QAUserType.DEFAULT_USERTYPE_CODE, userType)) {
				mca = get(ctx.getTenant().getId(), qaId, channel.getCode(), QAUserType.DEFAULT_USERTYPE_CODE);
			}
		}
		return mca;
	}
	
	/**
	 * 遞廻尋找指定 channel 跟其上層 channel 直到都沒有為止
	 * 
	 * @param tenantId
	 * @param qaId
	 * @param channel
	 * @param userType
	 * @return
	 */
	public static MultiChannelAnswer findNonEmptyAnswer(QAContext ctx, String qaId, QAChannel channel, String userType) {
		MultiChannelAnswer mca = findAnswerInChannel(ctx, qaId, channel, userType);
		Set<String> dups = new HashSet<>();
		
		// 找上層 channel
		while (mca == null || mca.isEmpty()) {
			QAChannel parentCh = channel.getDefaultChannel();
			
			if (parentCh != null) {
				if (dups.contains(parentCh.getCode())) break;
				else dups.add(parentCh.getCode());
				
				channel = parentCh;
				mca = findAnswerInChannel(ctx, qaId, channel, userType);
			}
			else {
				break;
			}
		}
		return (mca == null || mca.isEmpty()) ? null : mca;
	}
	
	/**
	 * 問答時欲取得答案內容的 method，會自動判斷空值而尋找 default 答案。
	 * 
	 * @param tenantId
	 * @param qaId
	 * @param channel
	 * @param userType
	 * @return
	 */
	public static String qaRuleGetAnswer(QAContext ctx, String qaId, QAChannel channel, String userType) {
		MultiChannelAnswer mca = findNonEmptyAnswer(ctx, qaId, channel, userType);
		if (mca == null || mca.isEmpty()) return "";
		return mca.getAnswer();
	}
	
	@SuppressWarnings("unchecked")
	public static List<MultiChannelAnswer> listByPlugin(int tenantId, String answerPluginId, String qaId, String answerPluginConfig, boolean idFullMatch) {
		List<MultiChannelAnswer> result = new ArrayList<MultiChannelAnswer>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(MultiChannelAnswer.class);
			if (tenantId > 0) {
				ct.add(Restrictions.eq("tenantId", tenantId));
			}
			if (answerPluginId != null) {
				ct.add(Restrictions.eq("answerPluginId", answerPluginId));
			}
			if(idFullMatch && qaId != null) {
				ct.add(Restrictions.eq("qaId", qaId));
			}
			if(!idFullMatch && qaId != null) {
				ct.add(Restrictions.like("qaId", qaId, MatchMode.ANYWHERE));
			}
			if (answerPluginConfig != null) {
				ct.add(Restrictions.like("answerPluginConfig", answerPluginConfig, MatchMode.ANYWHERE));
			}
			ct.add(Restrictions.eq("disable", false));
			ct.addOrder(Order.asc("id"));
			result = ct.list();
		} catch (HibernateException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return result;
	}
	
	public static int updateCfgByMkey(String oldMkey, String newMkey, String answerPluginId, int tid) {
		int result = -1;
		Connection con = null;
		PreparedStatement pstmt = null;
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			con = ses.connection();
			con.setAutoCommit(false);

			String cfg = "";
			switch (answerPluginId) {
			case HierarchicalQA.ID:
				cfg = "\"" + HierarchicalQA.HIERARCHICAL_QA_FIELD_NAME + "\":\"MKEY\"";
				break;
			case CustomQA.ID:
				cfg = "\"" + CustomQA.QA_PATTERN_KEY_INDEX_FIELD + "\":\"MKEY\"";
				break;
			case QADialogPlugin.ID:
				cfg = "\"" + QADialogPlugin.QA_KEY_INDEX_FIELD + "\":\"MKEY\"";
				break;
			}

			String mssql = "UPDATE MultiChannelAnswer"
					+ " set answerPluginConfig = REPLACE(Cast(answerPluginConfig as varchar(4096)), '"
					+ cfg.replace("MKEY", oldMkey) + "', '" + cfg.replace("MKEY", newMkey) + "')" + " where tenantId = "
					+ tid + " and answerPluginId = " + answerPluginId + " and answerPluginConfig like '%"
					+ cfg.replace("MKEY", oldMkey) + "%'";
			String mysql = "UPDATE MultiChannelAnswer" + " set answerPluginConfig = REPLACE(answerPluginConfig, '"
					+ cfg.replace("MKEY", oldMkey) + "', '" + cfg.replace("MKEY", newMkey) + "')" + " where tenantId = "
					+ tid + " and answerPluginId = " + answerPluginId + " and answerPluginConfig like '%"
					+ cfg.replace("MKEY", oldMkey) + "%'";
			pstmt = null;
			switch (HibernateUtil.SQL_TYPE) {
			case MYSQL:
				pstmt = con.prepareStatement(mysql);
				break;
			case MSSQL:
				pstmt = con.prepareStatement(mssql);
				break;
			}
			result = pstmt.executeUpdate();
			con.commit();
		} catch (SQLException e) {
			result = -1;
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
		return result;
	}
	
	public static void disableAnswer(String channel, String userType) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			String hql = "update " + MultiChannelAnswer.class.getName() + " set disable = true ";
			if (channel != null) {
				hql += "where channel = :channel";
				ses.createQuery(hql)
			    	.setString("channel", channel)
		    		.executeUpdate();
			} else {
				hql += "where userType = :userType";
				ses.createQuery(hql)
			    	.setString("userType", userType)
		    		.executeUpdate();
			}
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<MultiChannelAnswer> list(int tenantId) {
        
        List<MultiChannelAnswer> result = new ArrayList<MultiChannelAnswer>();
        Session ses = null;
        try {
            ses = HibernateUtil.getSession();
            result = ses
                    .createCriteria(MultiChannelAnswer.class)
                    .add(Restrictions.eq("tenantId", tenantId))
                    .addOrder(Order.asc("id")).list();
            
        } catch (HibernateException e) {
            e.printStackTrace();
        } finally {
            ses.close();
        }
        return result;
    }

	
	@SuppressWarnings("unchecked")
	public static List<MultiChannelAnswer> list(int tenantId, String qaId, String userType) {
        
        List<MultiChannelAnswer> result = new ArrayList<MultiChannelAnswer>();
        Session ses = null;
        try {
            ses = HibernateUtil.getSession();
            result = ses
                    .createCriteria(MultiChannelAnswer.class)
                    .add(Restrictions.eq("tenantId", tenantId))
                    .add(Restrictions.eq("qaId", qaId))
					.add(Restrictions.eq("userType", userType))
                    .addOrder(Order.asc("id")).list();
            
        } catch (HibernateException e) {
            e.printStackTrace();
        } finally {
            ses.close();
        }
        return result;
    }
	
	public static void disableByQaId(String docId, int tid) {
		Connection con = null;
		PreparedStatement pstmt = null;
		Session ses = null;
		try {
			log.info("disable MultiChannelAnswer docId:" + docId + " ,tid:" + tid);
			ses = HibernateUtil.getSession();
			con = ses.connection();
			con.setAutoCommit(false);
			String sql = "UPDATE MultiChannelAnswer set disable = 1 where qaId in (?) and tenantId = ?";
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, docId);
			pstmt.setInt(2, tid);
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
	
	@SuppressWarnings("unchecked")
	public static List<MultiChannelAnswer> listByChannel(int tenantId, String channel) {
		List<MultiChannelAnswer> result = new ArrayList<MultiChannelAnswer>();
        Session ses = null;
        try {
            ses = HibernateUtil.getSession();
            result = ses
                    .createCriteria(MultiChannelAnswer.class)
                    .add(Restrictions.eq("tenantId", tenantId))
					.add(Restrictions.eq("channel", channel))
                    .addOrder(Order.asc("id")).list();
            
        } catch (HibernateException e) {
            e.printStackTrace();
        } finally {
            ses.close();
        }
        return result;
	}
	
	public String getAnswerType() {
		return answerType;
	}

	public void setAnswerType(String answerType) {
		this.answerType = answerType;
	}

	public String getAnswerVoice() {
		return answerVoice;
	}

	public void setAnswerVoice(String answerVoice) {
		this.answerVoice = answerVoice;
	}

	public String getAnswerMood() {
		return answerMood;
	}

	public void setAnswerMood(String answerMood) {
		this.answerMood = answerMood;
	}

	public String getRichMessageMKey() {
		return lineMKey;
	}

	public void setRichMessageMKey(String mkey) {
		this.lineMKey = mkey;
	}

	public String getLineMKey() {
		return lineMKey;
	}

	public void setLineMKey(String lineMKey) {
		this.lineMKey = lineMKey;
	}
}

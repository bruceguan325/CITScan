package com.intumit.hibernate;

import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.connection.C3P0ConnectionProvider;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.impl.SessionFactoryImpl;

import com.intumit.citi.CitiDBInfo;
import com.intumit.citi.CitiDBInfoApiResultDto;
import com.intumit.citi.CitiUtil;
import com.intumit.smartwiki.WikiWord;
import com.intumit.solr.blackKeywords.BlackKeyword;
import com.intumit.solr.robot.WiSeReplicationSwitch;
import com.intumit.solr.searchKeywords.SearchKeywordLog;
import com.intumit.solr.synonymKeywords.SynonymKeyword;
import com.intumit.solr.synonymKeywords.SynonymKeywordVersion;
import com.intumit.solr.util.WiSeEnv;
import com.intumit.solr.util.WiSeSnapPuller;
import com.intumit.systemconfig.WiseSystemConfig;
import com.intumit.systemconfig.WiseSystemConfigFacade;

public class HibernateUtil {

	private static SessionFactory sessionFactory;
	private static boolean initialized = false;
	private static AtomicReference<String> DB_NAME = new AtomicReference<>();
	private static final Pattern MSSQL_JDBC_URL_TEMPLATE = Pattern.compile("^jdbc:sqlserver://(?<ADDRESS>[\\w\\d_\\-\\.]+)(:(?<PORT>\\d*))?;databaseName=(?<DBNAME>[\\w\\-_]+);?.*+");
	private static final Pattern MYSQL_JDBC_URL_TEMPLATE = Pattern.compile("^jdbc:mysql://(?<ADDRESS>[\\w\\d_\\-\\.]+):(?<PORT>\\d*)/(?<DBNAME>[\\w\\-_]+)\\?.*+");

	public static enum SqlType {
		MYSQL, MSSQL;
	};

	public static SqlType SQL_TYPE = SqlType.MSSQL;

	public static Session getSession() {
		return getSessionFactory().openSession();
	}

	public static SessionFactory getSessionFactory() {
	    CitiDBInfoApiResultDto dto = CitiUtil.getCitiDBInfoResult(DB_NAME.get());
	    if(false) {
    	    System.out.println("change password " + new Date().toGMTString());
            if (sessionFactory instanceof SessionFactoryImpl) {
                SessionFactoryImpl sf = (SessionFactoryImpl) sessionFactory;
                ConnectionProvider conn = sf.getConnectionProvider();
                if (conn instanceof C3P0ConnectionProvider) {
                    ((C3P0ConnectionProvider) conn).close();
                }
            }
            sessionFactory.close();
            init();
	    }
		return sessionFactory;
	}

	public static void shutdown() {
		getSessionFactory().close();
	}

	public static void init() {
		try {
            AnnotationConfiguration conf = new AnnotationConfiguration().configure(Paths.get(System.getProperty("hibernate.config")).toFile()); //For Websphere
            //.configure(); For Jetty 如果要再上版記得改成 Websphere 版不然花旗 PRO DB 連線會出錯
            String jdbcUrl = conf.getProperty("hibernate.connection.url");
            String currentAddress = "";
            if(StringUtils.isBlank(DB_NAME.get())) {
                Matcher matcher = null;
                if(SQL_TYPE == SqlType.MSSQL) {
                    matcher = MSSQL_JDBC_URL_TEMPLATE.matcher(jdbcUrl);
                }
                else {
                    matcher = MYSQL_JDBC_URL_TEMPLATE.matcher(jdbcUrl);
                }
                if(matcher.find()) {
                    DB_NAME.set("robotcit");
                    currentAddress = "localhost";
                }
            }
            CitiDBInfo dbInfo = CitiUtil.getCurrentInited(DB_NAME.get());
            if(!dbInfo.equals(CitiDBInfo.EMPTY)) {
                conf.setProperty("hibernate.connection.username", dbInfo.getAccount());
                conf.setProperty("hibernate.connection.password", dbInfo.getPwd());
                conf.setProperty("hibernate.connection.url", jdbcUrl.replace(currentAddress, dbInfo.getAddress()));
            }
			conf.addClass(SearchKeywordLog.class);
			conf.addClass(com.intumit.solr.searchKeywords.ClickLog.class);
			conf.addClass(BlackKeyword.class);
			conf.addAnnotatedClass(SynonymKeyword.class);
			conf.addAnnotatedClass(SynonymKeywordVersion.class);
			conf.addAnnotatedClass(WiseSystemConfig.class);
			conf.addAnnotatedClass(WikiWord.class);
			conf.addAnnotatedClass(com.intumit.solr.searchKeywords.SearchKeywordLogStatisticsHourly.class);
			conf.addAnnotatedClass(com.intumit.solr.dataset.DataSet.class);
			conf.addAnnotatedClass(com.intumit.solr.dataset.ElevatorSet.class);
			conf.addAnnotatedClass(com.intumit.solr.admin.AdminGroup.class);
			conf.addAnnotatedClass(com.intumit.solr.admin.AdminUser.class);
			conf.addAnnotatedClass(com.intumit.solr.admin.AdminLocation.class);
			conf.addAnnotatedClass(com.intumit.solr.dataimport.CURDLog.class);
			conf.addAnnotatedClass(com.intumit.solr.admin.GroupDataSet.class);
			conf.addAnnotatedClass(com.intumit.solr.config.ColumnNameMapping.class);
			conf.addAnnotatedClass(com.intumit.solr.dataimport.DihLogEntity.class);
			conf.addAnnotatedClass(com.intumit.quartz.ScheduleLogEntity.class);
			conf.addAnnotatedClass(com.intumit.viewRecord.KeywordToIdRecord.class);
			conf.addAnnotatedClass(com.intumit.viewRecord.ViewRecordEntity.class);
			conf.addAnnotatedClass(com.intumit.solr.dataimport.DataConfig.class);
			conf.addAnnotatedClass(com.intumit.solr.dataimport.DataField.class);
			conf.addAnnotatedClass(com.intumit.syslog.SyslogEntity.class);
			conf.addAnnotatedClass(com.intumit.syslog.OperationLogEntity.class);

			conf.addAnnotatedClass(com.intumit.solr.user.User.class);
			conf.addAnnotatedClass(com.intumit.solr.tenant.Tenant.class);
			conf.addAnnotatedClass(com.intumit.solr.tenant.TenantAdminGroup.class);
			conf.addAnnotatedClass(com.intumit.solr.tenant.Apikey.class);
			conf.addAnnotatedClass(com.intumit.citi.CitiDeep.class);
			conf.addAnnotatedClass(com.intumit.citi.MisLogReport.class);
			conf.addAnnotatedClass(com.intumit.solr.robot.dictionary.DictionaryDatabaseVersion.class);
            conf.addAnnotatedClass(com.intumit.solr.robot.entity.EntityDatabaseVersion.class);
			conf.addAnnotatedClass(com.intumit.solr.robot.RobotFormalAnswersVersion.class);

			if (!WiSeEnv.isRobotIndexMode()) {
				// Replication Switch
				conf.addAnnotatedClass(com.intumit.solr.robot.WiSeReplicationSwitch.class);

				conf.addAnnotatedClass(com.intumit.solr.robot.QAPattern.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.QADialogConfig.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.QADialogConfigVersion.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.ServiceLogEntity.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.ServiceLogEntityLite.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.TextCrmServiceLogEntity.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.EvaluationLogEntity.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.AutoEvaluationLog.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.QAAltBuild.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.QAEvaluationLog.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.QAAltTemplate.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.ambiguity.AmbiguityDatabase.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.dictionary.DictionaryDatabase.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.DeviceBinding.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.RobotFormalAnswers.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.UserClue.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.UserClueTag.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.UrlShortener.class);

				// Push service
				conf.addAnnotatedClass(com.intumit.solr.robot.push.SingleUserPushTrigger.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.push.MultiUserPushTrigger.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.push.PushData.class);

				conf.addAnnotatedClass(com.intumit.solr.robot.EventType.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.RobotFormalAnswersSticker.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.intent.QAIntent.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.entity.QAEntity.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.wivo.WiVoEntry.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.MultiChannelAnswer.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.QAChannel.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.QAUserType.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.SegmentBatchTask.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.RobotImageFile.class);

				conf.addAnnotatedClass(com.intumit.solr.robot.EvaluationLogEntityUpdateLog.class);

				conf.addAnnotatedClass(com.intumit.solr.robot.connector.line.RichMessage.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.connector.web.RichMessage.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.connector.webline.RichMessage.class);
				conf.addAnnotatedClass(com.intumit.solr.robot.dictionary.bank.DepositInterestRateDict.class);

				conf.addAnnotatedClass(com.intumit.solr.form.Form.class);
			}

			sessionFactory = conf.buildSessionFactory();
			initialized = true;

		} catch (Throwable ex) {
			ex.printStackTrace();
			throw new ExceptionInInitializerError(ex);
		}

		// Some default data initailization.
		try {
			WiseSystemConfigFacade imp = WiseSystemConfigFacade.getInstance();
			WiseSystemConfig bean = imp.get();

			if (bean == null) {
				bean = imp.createInitValue();
				imp.update(bean);
			}

			if (bean.getLbModeEnable() && bean.getLbModeSwitchable()) {
				List<WiSeReplicationSwitch> master = WiSeReplicationSwitch.listNodes(null,
						WiSeReplicationSwitch.MASTER);
				if (master.size() == 0) {
					WiSeReplicationSwitch replication = new WiSeReplicationSwitch();
					replication.setHost(InetAddress.getLocalHost().getHostName());
					replication.setPort("12001");
					replication.setReplicationStauts(WiSeReplicationSwitch.MASTER);
					WiSeReplicationSwitch.saveOrUpdate(replication);

					WiSeSnapPuller.setMasterHost(replication.getHost());
					WiSeSnapPuller.setMasterPort(replication.getPort());
				} else if (master.size() == 1) {
					WiSeSnapPuller.setMasterHost(master.get(0).getHost());
					WiSeSnapPuller.setMasterPort(master.get(0).getPort());
				}
			}

			com.intumit.solr.admin.AdminUserFacade.getInstance();
			com.intumit.solr.admin.AdminLocationFacade.getInstance();

		} catch (Throwable ex) {
			ex.printStackTrace();
			throw new ExceptionInInitializerError(ex);
		}

	}

	public static boolean isInitialized() {
		return initialized;
	}
	
}
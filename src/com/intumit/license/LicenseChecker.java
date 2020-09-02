package com.intumit.license;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeEnv;
import com.intumit.util.DesUtil;

public class LicenseChecker implements Serializable {
	
	private static final long serialVersionUID = -8542252301187644461L;

	private static final Logger log = Logger.getLogger(LicenseChecker.class);
	
	private static License license;
	private static String VERSION = "robot";
	private static String LICENSE_START_FILE_NAME = "info";

	public static void check() {
		File licenseFile = new File(WiSeEnv.getHomePath(), "license.txt");
        String licenseText;
        try {
            licenseText = FileUtils.readFileToString(licenseFile);
            license = License.parse(licenseText);
            LicenseChecker.validate(license);
            invokeLicenseGuardian();
        }
        catch (IOException e) {
        	InitialReport.addError(InitialError.licence);
            log.error(e, e);
        }
        catch(IllegalLicenseException licE) {
        	InitialReport.addError(InitialError.licence);
            log.error(licE, licE);
            invokeLicenseGuardian();
        }
	}
	
	private static void invokeLicenseGuardian() {
        Timer timer = new Timer(true);
        if (license.isDevMode()) {
            Logger.getLogger("com.intumit").info("Development license.");
            timer.schedule(new TimerTask() {
                public void run() {
                    System.out.println("Time's up!  Debug faster next time!");
                    System.setProperty("intumit.exit.code", "1");
                    System.exit(1);
                }
            }, license.getDevDay() * 24 * 60 * 60 * 1000);
        }

    }

	public static void validate(License license) {
        if (!VERSION.equals(license.getVersion())) {
            throw new IllegalLicenseException("license 版本不合! 預期 " + VERSION + " 卻是 " + license.getVersion());
        }
        if (!license.isUseExpireDate() && !license.isNeverExpired()) {
            Date startDate = readStartDate(new File(WiSeEnv.getHomePath()));
            license.setStartDate(startDate);
        }
        if (license.isObsolete()) {
            throw new IllegalLicenseException("license 過期!");
        }
    }
	
	public static boolean hasModule(Module module) {
		return license.hasModule(module.name());
	}
	
	public static boolean isOverTenantLimit() {
		return (getTenantSize() + 1) > license.getMaxCompany(); 
	}

    static Date readStartDate(File cfgPath) {
        File licenseStartFile = new File(cfgPath, LICENSE_START_FILE_NAME);
        Date startDate;
        if (!licenseStartFile.exists()) {
            if (noTenantInSystem()) {
                startDate = new Date();
                createLicenseStartFile(licenseStartFile, startDate);
            }
            else {
                throw new IllegalLicenseException("file " + licenseStartFile.getAbsolutePath()
                        + " not found!");
            }
        }
        else {
            startDate = readStartDateFromFile(licenseStartFile);
        }
        return startDate;
    }

    private static Date readStartDateFromFile(File licenseStartFile) {
        String s;
        try {
            s = FileUtils.readFileToString(licenseStartFile);
        }
        catch (IOException e) {
            throw new IllegalLicenseException("read file " + licenseStartFile.getAbsolutePath()
                    + " error!", e);
        }
        s = DesUtil.decrypt(s);
        long t = Long.parseLong(s);
        return new Date(t);
    }

    private static void createLicenseStartFile(File licenseStartFile, Date startDate) {
        long t = startDate.getTime();
        String s = DesUtil.encrypt(t + "");
        try {
            FileUtils.writeStringToFile(licenseStartFile, s);
        }
        catch (IOException e) {
            throw new IllegalLicenseException("write file " + licenseStartFile.getAbsolutePath()
                    + " error!", e);
        }
    }

    private static boolean noTenantInSystem() {
    	return getTenantSize() == 0;
    }
    
    private static Long getTenantSize() {
    	Session ses = null;
    	try {
    		ses = HibernateUtil.getSession();
    		Criteria ct = ses.createCriteria(Tenant.class);
    		ct.setProjection(Projections.rowCount());
    		return (Long) ct.uniqueResult();
    	}
    	catch(Exception ignore) {
    		ignore.printStackTrace();
    		throw ignore;
    	}
    	finally {
    		ses.close();
    	}
    }
    
    public static boolean isExceedMaxUser() {
    	return (getTenantUserCount() + 1) > license.getMaxUser();
    }
    
    private static Long getTenantUserCount() {
    	Session ses = null;
    	try {
    		ses = HibernateUtil.getSession();
    		Criteria ct = ses.createCriteria(AdminUser.class);
    		ct.setProjection(Projections.rowCount());
    		return (Long) ct.uniqueResult();
    	}
    	catch(Exception ignore) {
    		ignore.printStackTrace();
    		throw ignore;
    	}
    	finally {
    		ses.close();
    	}
    }
    
}

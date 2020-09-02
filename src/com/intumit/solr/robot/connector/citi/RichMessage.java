package com.intumit.solr.robot.connector.citi;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Index;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.RobotFormalAnswers;
import com.intumit.solr.robot.TemplateUtil;
import com.intumit.solr.robot.NaverLineAnswerTransformer;
import com.intumit.solr.robot.NaverLineAnswerTransformer.TagLGQReplacer;
import com.intumit.solr.robot.QAUtil.FormalAnswerReplacer;
import com.intumit.solr.robot.function.FunctionUtil;
import com.intumit.solr.util.WiSeUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intumit.citi.CitiDeep;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 存放 WEB 特殊訊息格式的地方（理論上只要不是純文字，就這邊處理）
 * 
 * @author herb
 */
public class RichMessage extends CitiDeep {

    public static List<RichMessage> list(int tenantId) {
		Session ses = null;
		try {
		    ses = HibernateUtil.getSession();
			Criteria ct = ses.createCriteria(CitiDeep.class).addOrder(Order.asc("priority"));
			List<RichMessage> citiTable = (List<RichMessage>) ct.list();

            ObjectMapper mapper = new ObjectMapper();
            MsgTemplate msgtmp = new MsgTemplate();
            Template tmpl = new Template();
            Action act1 = new Action();
            Action act2 = new Action();
            Action act3 = new Action();
            
            for (CitiDeep citi:citiTable) {
			    
			    act1.setType("message");
			    act1.setLabel(citi.getOfferText1());
			    act1.setText(citi.getOfferName1());
			    
			    act2.setType("message");
                act2.setLabel(citi.getOfferText2());
                act2.setText(citi.getOfferName2());
                
                act3.setType("message");
                act3.setLabel(citi.getOfferText3());
                act3.setText(citi.getOfferName3());
                com.intumit.solr.robot.connector.citi.Column col = new com.intumit.solr.robot.connector.citi.Column();
			    col.setThumbnailImageUrl(RobotFormalAnswers.getAnswers(tenantId, "Card_Face_Url").get(0).toString() + citi.getImageUrl());
			    col.setText(citi.getApplyNow());
			    col.setTitle(citi.getKnowMore());
			    col.getActions().add(act1);
			    col.getActions().add(act2);
			    col.getActions().add(act3);
			    
			    tmpl.setType("carousel");
	            tmpl.setReward(citi.getReward());
	            tmpl.getColumns().add(col);
			    
	            msgtmp.setReward(citi.getReward());
	            msgtmp.setType("template");
	            msgtmp.setTemplate(tmpl);
	            
	            citi.setMsgTemplate(mapper.writeValueAsString(msgtmp));
	            tmpl.getColumns().clear();
	            col.getActions().clear();
	            
	            citi.setMsgType("carousel");
	        }
	        
			return citiTable;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
		return null;
	}	
	
}

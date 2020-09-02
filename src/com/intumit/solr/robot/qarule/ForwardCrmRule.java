package com.intumit.solr.robot.qarule;

import java.util.EnumSet;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.robot.QAContext;

public abstract class ForwardCrmRule implements PreQAMatchRule {

 private static final long serialVersionUID = 1L;
 
 public static final String REQ_ATTR_FORWARD_OPTIONS = "offerForwardOptions";
    public static final String REQ_ATTR_FORCE_CRM_FORWARD = "forceCrmForward";
    public static final String REQ_ATTR_ASK_USER_REDIRECT_TO_CRM = "askUserRedirectToCRM";
    public static final String REQ_ATTR_HAS_MARKETING_KEYWORDS = "marketingKeywords";
    public static final String REQ_ATTR_HAS_NEGATIVE_KEYWORDS = "negativeKeywords";

    public interface Cmd {
        String getText();

        Forward getForward();
    }

    public enum Forward {
        PERSONNEL,      // 轉入文字客服
        MESSAGE,        // 轉入文字客服留言功能
        SURVEY,         // 轉入文字客服問卷(滿意度調查)功能
        CONTINUE,       // 繼續跟機器人對話
        NETWORK_BANK,   //
        MESSAGE_BOARD   // 轉入外部留言板（非文字客服）

    }

    public static enum Status {
        ENTERING, ACTIVE, OFFLINE, CLOSED,;
    }

    static <T extends Enum<T> & Cmd> String getCmdInput(T cmd) {
        return StringUtils.substringAfter(cmd.name(), "_");
    }

    public static <T extends Enum<T> & Cmd> Cmd getCmdFromInput(Class<T> type, String input) {
        T cmd = null;
        for (T c : EnumSet.allOf(type)) {
            if (getCmdInput(c).equals(input)) {
                cmd = c;
                break;
            }
        }
        return cmd;
    }

    public abstract void processForward(QAContext ctx, Forward forward);

}

package com.intumit.citi;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "Code",
    "Message"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Result implements Serializable {

    @JsonProperty("Code")
    private int code;
    @JsonProperty("Message")
    private String message;
    
    /**
     * No args constructor for use in serialization
     * 
     */
    public Result() {
    }

    /**
     * 
     * @param code
     * @param postfix
     * @param message
     */
    public Result(int code, String message) {
        super();
        this.code = code;
        this.message = message;
    }

    @JsonProperty("Code")
    public int getCode() {
        return code;
    }

    @JsonProperty("Code")
    public void setCode(int code) {
        this.code=code;
    }

    @JsonProperty("Message")
    public String getMessage() {
        return message;
    }

    @JsonProperty("Message")
    public void setMessage(String message) {
        this.message=message;
    } 

    public enum Postfix {
        RESENDESTMT("resendestmt"),
        CARDINFO("cardinfo"),
        STATEMENT("statement"),
        STMTDETAIL("stmtdetail"),
        PYMRECORD("pymrecord");
        private final String value;
        private final static Map<String, Result.Postfix> CONSTANTS = new HashMap<String, Result.Postfix>();

        static {
            for (Result.Postfix c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Postfix(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static Result.Postfix fromValue(String value) {
            Result.Postfix constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}

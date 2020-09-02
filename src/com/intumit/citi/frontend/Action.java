
package com.intumit.citi.frontend;

import java.net.URI;
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
    "Type",
    "Text",
    "Url",
    "Attach"
})
public class Action {

    @JsonProperty("Type")
    private Action.Type type;
    @JsonProperty("Text")
    private String text;
    @JsonProperty("Url")
    private String url;
    @JsonProperty("Attach")
    private String attach;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Action() {
    }

    /**
     * 
     * @param text
     * @param attach
     * @param type
     * @param url
     */
    public Action(Action.Type type, String text, String url, String attach) {
        super();
        this.type = type;
        this.text = text;
        this.url = url;
        this.attach = attach;
    }

    @JsonProperty("Type")
    public Action.Type getType() {
        return type;
    }

    @JsonProperty("Type")
    public void setType(Action.Type type) {
        this.type = type;
    }

    @JsonProperty("Text")
    public String getText() {
        return text;
    }

    @JsonProperty("Text")
    public void setText(String text) {
        this.text = text;
    }

    @JsonProperty("Url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("Url")
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty("Attach")
    public String getAttach() {
        return attach;
    }

    @JsonProperty("Attach")
    public void setAttach(String attach) {
        this.attach = attach;
    }

    public enum Type {

        URL("url");
        private final String value;
        private final static Map<String, Action.Type> CONSTANTS = new HashMap<String, Action.Type>();

        static {
            for (Action.Type c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Type(String value) {
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
        public static Action.Type fromValue(String value) {
            Action.Type constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}

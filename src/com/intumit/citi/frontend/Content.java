
package com.intumit.citi.frontend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    "AttachAction",
    "Columns",
    "Rows"
})
public class Content {

    @JsonProperty("Type")
    private Content.Type type;
    @JsonProperty("Text")
    private String text;
    @JsonProperty("AttachAction")
    private Action attachAction;
    /**
     * 
     * Corresponds to the "Columns" property.
     * 
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("Columns")
    private List<Header> headers = new ArrayList<Header>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("Rows")
    private List<Row> rows = new ArrayList<Row>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public Content() {
    }

    /**
     * 
     * @param headers
     * @param text
     * @param type
     * @param rows
     * @param attachAction
     */
    public Content(Content.Type type, String text, Action attachAction, List<Header> headers, List<Row> rows) {
        super();
        this.type = type;
        this.text = text;
        this.attachAction = attachAction;
        this.headers = headers;
        this.rows = rows;
    }

    @JsonProperty("Type")
    public Content.Type getType() {
        return type;
    }

    @JsonProperty("Type")
    public void setType(Content.Type type) {
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

    @JsonProperty("AttachAction")
    public Action getAttachAction() {
        return attachAction;
    }

    @JsonProperty("AttachAction")
    public void setAttachAction(Action attachAction) {
        this.attachAction = attachAction;
    }

    /**
     * 
     * Corresponds to the "Columns" property.
     * 
     */
    @JsonProperty("Columns")
    public List<Header> getHeaders() {
        return headers;
    }

    /**
     * 
     * Corresponds to the "Columns" property.
     * 
     */
    @JsonProperty("Columns")
    public void setHeaders(List<Header> headers) {
        this.headers = headers;
    }

    @JsonProperty("Rows")
    public List<Row> getRows() {
        return rows;
    }

    @JsonProperty("Rows")
    public void setRows(List<Row> rows) {
        this.rows = rows;
    }

    public void addHeader(Header item)
    {
        this.headers.add(item);	
    }
    
    public void clearHeader()
    {
    	this.headers.clear();
    }
    
    public void addRow(Row item)
    {
        this.rows.add(item);	
    }
    
    public void clearRow()
    {
    	this.rows.clear();
    }
    
    public enum Type {

        TEXT("text"),
        GRID("grid");
        private final String value;
        private final static Map<String, Content.Type> CONSTANTS = new HashMap<String, Content.Type>();

        static {
            for (Content.Type c: values()) {
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
        public static Content.Type fromValue(String value) {
            Content.Type constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}

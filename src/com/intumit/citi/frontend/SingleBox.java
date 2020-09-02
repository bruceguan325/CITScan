
package com.intumit.citi.frontend;

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
    "Id",
    "Type",
    "Title"
})
public class SingleBox {

    @JsonProperty("Id")
    private String id;
    @JsonProperty("Type")
    private SingleBox.Type type;
    @JsonProperty("Title")
    private String title;

    /**
     * No args constructor for use in serialization
     * 
     */
    public SingleBox() {
    }

    /**
     * 
     * @param id
     * @param type
     * @param title
     */
    public SingleBox(String id, SingleBox.Type type, String title) {
        super();
        this.id = id;
        this.type = type;
        this.title = title;
    }

    @JsonProperty("Id")
    public String getId() {
        return id;
    }

    @JsonProperty("Id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("Type")
    public SingleBox.Type getType() {
        return type;
    }

    @JsonProperty("Type")
    public void setType(SingleBox.Type type) {
        this.type = type;
    }

    @JsonProperty("Title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("Title")
    public void setTitle(String title) {
        this.title = title;
    }

    public enum Type {

        ANNOUNCE("announce"),
        GRID("grid");
        private final String value;
        private final static Map<String, SingleBox.Type> CONSTANTS = new HashMap<String, SingleBox.Type>();

        static {
            for (SingleBox.Type c: values()) {
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
        public static SingleBox.Type fromValue(String value) {
            SingleBox.Type constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}

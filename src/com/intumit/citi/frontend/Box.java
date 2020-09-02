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
public class Box {

    @JsonProperty("Id")
    private String id;
    @JsonProperty("Type")
    private Box.Type type;
    @JsonProperty("Title")
    private String title;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Box() {
    }

    /**
     * 
     * @param id
     * @param type
     * @param title
     */
    public Box(String id, Box.Type type, String title) {
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
    public Box.Type getType() {
        return type;
    }

    @JsonProperty("Type")
    public void setType(Box.Type type) {
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
        private final static Map<String, Box.Type> CONSTANTS = new HashMap<String, Box.Type>();

        static {
            for (Box.Type c : values()) {
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
        public static Box.Type fromValue(String value) {
            Box.Type constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}

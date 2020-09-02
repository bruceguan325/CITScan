
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
    "Type"
})
public abstract class Message {

    @JsonProperty("Id")
    private String id;
    @JsonProperty("Type")
    private Message.Type type;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Message() {
    }

    /**
     * 
     * @param id
     * @param type
     */
    public Message(String id, Message.Type type) {
        super();
        this.id = id;
        this.type = type;
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
    public Message.Type getType() {
        return type;
    }

    @JsonProperty("Type")
    public void setType(Message.Type type) {
        this.type = type;
    }

    public enum Type {

        TEXT("text"),
        BUTTONS("buttons"),
        CAROUSEL("carousel");
        private final String value;
        private final static Map<String, Message.Type> CONSTANTS = new HashMap<String, Message.Type>();

        static {
            for (Message.Type c: values()) {
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
        public static Message.Type fromValue(String value) {
            Message.Type constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}


package com.intumit.citi.frontend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "Text"
})
public class MessageText
    extends Message
{

    @JsonProperty("Text")
    private String text;

    /**
     * No args constructor for use in serialization
     * 
     */
    public MessageText() {
    }

    /**
     * 
     * @param text
     * @param id
     * @param type
     */
    public MessageText(String text, String id, Message.Type type) {
        super(id, type);
        this.text = text;
    }

    @JsonProperty("Text")
    public String getText() {
        return text;
    }

    @JsonProperty("Text")
    public void setText(String text) {
        this.text = text;
    }

}

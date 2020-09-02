
package com.intumit.solr.robot.connector.citi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "label",
    "text",
    "type"
})
public class Action {

    @JsonProperty("label")
    private String label;
    @JsonProperty("text")
    private String text;
    @JsonProperty("type")
    private String type;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Action() {
    }

    /**
     * 
     * @param label
     * @param text
     * @param type
     */
    public Action(String label, String text, String type) {
        super();
        this.label = label;
        this.text = text;
        this.type = type;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    @JsonProperty("label")
    public void setLabel(String label) {
        this.label = label;
    }

    @JsonProperty("text")
    public String getText() {
        return text;
    }

    @JsonProperty("text")
    public void setText(String text) {
        this.text = text;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

}

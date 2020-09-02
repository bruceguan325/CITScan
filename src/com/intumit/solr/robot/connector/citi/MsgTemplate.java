
package com.intumit.solr.robot.connector.citi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "altText",
    "type",
    "template"
})
public class MsgTemplate {

    @JsonProperty("reward")
    private String reward;
    @JsonProperty("type")
    private String type;
    @JsonProperty("template")
    private Template template;

    /**
     * No args constructor for use in serialization
     * 
     */
    public MsgTemplate() {
    }

    /**
     * 
     * @param template
     * @param altText
     * @param type
     */
    public MsgTemplate(String reward, String type, Template template) {
        super();
        this.reward = reward;
        this.type = type;
        this.template = template;
    }

    @JsonProperty("reward")
    public String getReward() {
        return reward;
    }

    @JsonProperty("reward")
    public void setReward(String reward) {
        this.reward = reward;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("template")
    public Template getTemplate() {
        return template;
    }

    @JsonProperty("template")
    public void setTemplate(Template template) {
        this.template = template;
    }

}

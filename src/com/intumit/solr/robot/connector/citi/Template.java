
package com.intumit.solr.robot.connector.citi;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "reward",
    "columns",
    "type",
    "actions"
})
public class Template {

    @JsonProperty("reward")
    private String reward;
    @JsonProperty("columns")
    private List<Column> columns = new ArrayList<Column>();
    @JsonProperty("type")
    private String type;
    @JsonProperty("actions")
    private List<Object> actions = new ArrayList<Object>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public Template() {
    }

    /**
     * 
     * @param altText
     * @param columns
     * @param type
     * @param actions
     */
    public Template(String reward, List<Column> columns, String type, List<Object> actions) {
        super();
        this.reward = reward;
        this.columns = columns;
        this.type = type;
        this.actions = actions;
    }

    @JsonProperty("reward")
    public String getReward() {
        return reward;
    }

    @JsonProperty("reward")
    public void setReward(String reward) {
        this.reward = reward;
    }

    @JsonProperty("columns")
    public List<Column> getColumns() {
        return columns;
    }

    @JsonProperty("columns")
    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("actions")
    public List<Object> getActions() {
        return actions;
    }

    @JsonProperty("actions")
    public void setActions(List<Object> actions) {
        this.actions = actions;
    }

}


package com.intumit.solr.robot.connector.citi;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "thumbnailImageUrl",
    "text",
    "title",
    "actions"
})
public class Column {

    @JsonProperty("thumbnailImageUrl")
    private String thumbnailImageUrl;
    @JsonProperty("text")
    private String text;
    @JsonProperty("title")
    private String title;
    @JsonProperty("actions")
    private List<Action> actions = new ArrayList<Action>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public Column() {
    }

    /**
     * 
     * @param thumbnailImageUrl
     * @param text
     * @param title
     * @param actions
     */
    public Column(String thumbnailImageUrl, String text, String title, List<Action> actions) {
        super();
        this.thumbnailImageUrl = thumbnailImageUrl;
        this.text = text;
        this.title = title;
        this.actions = actions;
    }

    @JsonProperty("thumbnailImageUrl")
    public String getThumbnailImageUrl() {
        return thumbnailImageUrl;
    }

    @JsonProperty("thumbnailImageUrl")
    public void setThumbnailImageUrl(String thumbnailImageUrl) {
        this.thumbnailImageUrl = thumbnailImageUrl;
    }

    @JsonProperty("text")
    public String getText() {
        return text;
    }

    @JsonProperty("text")
    public void setText(String text) {
        this.text = text;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("actions")
    public List<Action> getActions() {
        return actions;
    }

    @JsonProperty("actions")
    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

}

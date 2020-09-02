package com.intumit.citi.frontend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "Title",
    "Text"
})
public class BoxCon {

    @JsonProperty("Title")
    private String title;
    @JsonProperty("Text")
    private String text;

    /**
     * No args constructor for use in serialization
     * 
     */
    public BoxCon() {
    }

    /**
     * 
     * @param text
     * @param title
     */
    public BoxCon(String title, String text) {
        super();
        this.title = title;
        this.text = text;
    }

    @JsonProperty("Title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("Title")
    public void setTitle(String title) {
        this.title = title;
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

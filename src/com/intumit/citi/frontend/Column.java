
package com.intumit.citi.frontend;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "ImageUrl",
    "ImageText",
    "ImageDesc",
    "Title",
    "TitleBackgroundColor",
    "Contents",
    "InternalActions",
    "ExternalActions"
})
public class Column {

    @JsonProperty("ImageUrl")
    private String imageUrl;
    @JsonProperty("ImageText")
    private String imageText;
    @JsonProperty("ImageDesc")
    private String imageDesc;
    @JsonProperty("Title")
    private String title;
    @JsonProperty("TitleBackgroundColor")
    private String titleBackgroundColor;
    @JsonProperty("Contents")
    private List<Content> contents = new ArrayList<Content>(5);
    @JsonProperty("InternalActions")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Action> internalActions = new ArrayList<Action>();
    @JsonProperty("ExternalActions")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Action> externalActions = new ArrayList<Action>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public Column() {
    }

    /**
     * 
     * @param imageText
     * @param contents
     * @param imageUrl
     * @param externalActions
     * @param imageDesc
     * @param title
     * @param internalActions
     * @param titleBackgroundColor
     */
    public Column(String imageUrl, String imageText, String imageDesc, String title, String titleBackgroundColor, List<Content> contents, List<Action> internalActions, List<Action> externalActions) {
        super();
        this.imageUrl = imageUrl;
        this.imageText = imageText;
        this.imageDesc = imageDesc;
        this.title = title;
        this.titleBackgroundColor = titleBackgroundColor;
        this.contents = contents;
        this.internalActions = internalActions;
        this.externalActions = externalActions;
    }

    @JsonProperty("ImageUrl")
    public String getImageUrl() {
        return imageUrl;
    }

    @JsonProperty("ImageUrl")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @JsonProperty("ImageText")
    public String getImageText() {
        return imageText;
    }

    @JsonProperty("ImageText")
    public void setImageText(String imageText) {
        this.imageText = imageText;
    }

    @JsonProperty("ImageDesc")
    public String getImageDesc() {
        return imageDesc;
    }

    @JsonProperty("ImageDesc")
    public void setImageDesc(String imageDesc) {
        this.imageDesc = imageDesc;
    }

    @JsonProperty("Title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("Title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("TitleBackgroundColor")
    public String getTitleBackgroundColor() {
        return titleBackgroundColor;
    }

    @JsonProperty("TitleBackgroundColor")
    public void setTitleBackgroundColor(String titleBackgroundColor) {
        this.titleBackgroundColor = titleBackgroundColor;
    }

    @JsonProperty("Contents")
    public List<Content> getContents() {
        return contents;
    }

    @JsonProperty("Contents")
    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    @JsonProperty("InternalActions")
    public List<Action> getInternalActions() {
        return internalActions;
    }

    @JsonProperty("InternalActions")
    public void setInternalActions(List<Action> internalActions) {
        this.internalActions = internalActions;
    }

    @JsonProperty("ExternalActions")
    public List<Action> getExternalActions() {
        return externalActions;
    }

    @JsonProperty("ExternalActions")
    public void setExternalActions(List<Action> externalActions) {
        this.externalActions = externalActions;
    }

    public void addContent(Content item) {
        this.contents.add(item);
    }
    
    public void clearContent() {
        this.contents.clear();
    }
    
    public void addInternalActions(Action item) {
        this.internalActions.add(item);
    }
    
    public void clearInternalActions() {
        this.internalActions.clear();
    }
    
    public void addExternalActions(Action item) {
        this.externalActions.add(item);
    }
    
    public void clearExternalActions() {
        this.externalActions.clear();
    }
}

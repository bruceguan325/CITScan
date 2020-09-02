
package com.intumit.citi.frontend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "Title",
    "Align"
})
public class Header {

    @JsonProperty("Title")
    private String title;
    @JsonProperty("Align")
    private String align;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Header() {
    }

    /**
     * 
     * @param title
     * @param align
     */
    public Header(String title, String align) {
        super();
        this.title = title;
        this.align = align;
    }

    @JsonProperty("Title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("Title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("Align")
    public String getAlign() {
        return align;
    }

    @JsonProperty("Align")
    public void setAlign(String align) {
        this.align = align;
    }

}

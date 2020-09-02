
package com.intumit.citi.frontend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "Text",
    "IsBold",
    "Align"
})
public class Field {

    @JsonProperty("Text")
    private String text;
    @JsonProperty("IsBold")
    private boolean isBold;
    @JsonProperty("Align")
    private String align;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Field() {
    }

    /**
     * 
     * @param text
     * @param align
     * @param isBold
     */
    public Field(String text, boolean isBold, String align) {
        super();
        this.text = text;
        this.isBold = isBold;
        this.align = align;
    }

    @JsonProperty("Text")
    public String getText() {
        return text;
    }

    @JsonProperty("Text")
    public void setText(String text) {
        this.text = text;
    }

    @JsonProperty("IsBold")
    public boolean isIsBold() {
        return isBold;
    }

    @JsonProperty("IsBold")
    public void setIsBold(boolean isBold) {
        this.isBold = isBold;
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


package com.intumit.citi.frontend;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "IsTitle",
    "Title",
    "IsAlternatingRow",
    "Fields"
})
public class GridRow {

    @JsonProperty("IsTitle")
    private boolean isTitle;
    @JsonProperty("Title")
    private String title;
    @JsonProperty("IsAlternatingRow")
    private boolean isAlternatingRow;
    @JsonProperty("Fields")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Field> fields = new ArrayList<Field>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public GridRow() {
    }

    /**
     * 
     * @param isTitle
     * @param title
     * @param isAlternatingRow
     * @param fields
     */
    public GridRow(boolean isTitle, String title, boolean isAlternatingRow, List<Field> fields) {
        super();
        this.isTitle = isTitle;
        this.title = title;
        this.isAlternatingRow = isAlternatingRow;
        this.fields = fields;
    }

    @JsonProperty("IsTitle")
    public boolean isIsTitle() {
        return isTitle;
    }

    @JsonProperty("IsTitle")
    public void setIsTitle(boolean isTitle) {
        this.isTitle = isTitle;
    }

    @JsonProperty("Title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("Title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("IsAlternatingRow")
    public boolean isIsAlternatingRow() {
        return isAlternatingRow;
    }

    @JsonProperty("IsAlternatingRow")
    public void setIsAlternatingRow(boolean isAlternatingRow) {
        this.isAlternatingRow = isAlternatingRow;
    }

    @JsonProperty("Fields")
    public List<Field> getFields() {
        return fields;
    }

    @JsonProperty("Fields")
    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public void addField(Field item)
    {
    	this.fields.add(item);
    }
    
    public void clearField()
    {
    	this.fields.clear();;
    }
}

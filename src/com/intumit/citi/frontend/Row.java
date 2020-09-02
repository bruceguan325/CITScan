
package com.intumit.citi.frontend;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "IsAlternatingRow",
    "Fields"
})
public class Row {

    @JsonProperty("IsAlternatingRow")
    private boolean isAlternatingRow;
    @JsonProperty("Fields")
    private List<Field> fields = new ArrayList<Field>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public Row() {
    }

    /**
     * 
     * @param isAlternatingRow
     * @param fields
     */
    public Row(boolean isAlternatingRow, List<Field> fields) {
        super();
        this.isAlternatingRow = isAlternatingRow;
        this.fields = fields;
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
    	this.fields.clear();
    }
}


package com.intumit.citi.frontend;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "Text",
    "Columns"
})
public class MessageCarousel
    extends Message
{

    @JsonProperty("Text")
    private String text;
    @JsonProperty("Columns")
    private List<Column> columns = new ArrayList<Column>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public MessageCarousel() {
    }

    /**
     * 
     * @param columns
     * @param text
     * @param id
     * @param type
     */
    public MessageCarousel(String text, List<Column> columns, String id, Message.Type type) {
        super(id, type);
        this.text = text;
        this.columns = columns;
    }

    @JsonProperty("Text")
    public String getText() {
        return text;
    }

    @JsonProperty("Text")
    public void setText(String text) {
        this.text = text;
    }

    @JsonProperty("Columns")
    public List<Column> getColumns() {
        return columns;
    }

    @JsonProperty("Columns")
    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public void addColumn(Column item) {
        this.columns.add(item);
    }
    
    public void clearAction() {
        this.columns.clear();
    }
}

package com.intumit.citi.frontend;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "Columns",
    "Rows"
})
public class BoxGrid
    extends Box
{

    /**
     * 

     * Corresponds to the "Columns" property.
     * 
     */
    @JsonProperty("Columns")
    private List<Header> headers = new ArrayList<Header>();
    /**
     * 

     * Corresponds to the "Rows" property.
     * 
     */
    @JsonProperty("Rows")
    private List<GridRow> gridRows = new ArrayList<GridRow>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public BoxGrid() {
    }

    /**
     * 
     * @param headers
     * @param gridRows
     * @param id
     * @param type
     * @param title
     */
    public BoxGrid(List<Header> headers, List<GridRow> gridRows, String id, Box.Type type, String title) {
        super(id, type, title);
        this.headers = headers;
        this.gridRows = gridRows;
    }

    /**
     * 

     * Corresponds to the "Columns" property.
     * 
     */
    @JsonProperty("Columns")
    public List<Header> getHeaders() {
        return headers;
    }

    /**
     * 

     * Corresponds to the "Columns" property.
     * 
     */
    @JsonProperty("Columns")
    public void setHeaders(List<Header> headers) {
        this.headers = headers;
    }

    /**
     * 

     * Corresponds to the "Rows" property.
     * 
     */
    @JsonProperty("Rows")
    public List<GridRow> getGridRows() {
        return gridRows;
    }

    /**
     * 

     * Corresponds to the "Rows" property.
     * 
     */
    @JsonProperty("Rows")
    public void setGridRows(List<GridRow> gridRows) {
        this.gridRows = gridRows;
    }

    public void addHeader(Header item)
    {
    	this.headers.add(item);
    }
    
    public void clearHeader()
    {
    	this.headers.clear();
    }
    
    public void addRow(GridRow item)
    {
    	this.gridRows.add(item);
    }
    
    public void clearRow()
    {
    	this.gridRows.clear();
    }
}

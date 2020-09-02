package com.intumit.citi.frontend;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "Contents"
})
public class BoxAnnounce
    extends Box
{

    /**
     * 

     * Corresponds to the "Contents" property.
     * 
     */
    @JsonProperty("Contents")
    private List<BoxCon> boxCons = new ArrayList<BoxCon>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public BoxAnnounce() {
    }

    /**
     * 
     * @param boxCons
     * @param id
     * @param type
     * @param title
     */
    public BoxAnnounce(List<BoxCon> boxCons, String id, Box.Type type, String title) {
        super(id, type, title);
        this.boxCons = boxCons;
    }

    /**
     * 

     * Corresponds to the "Contents" property.
     * 
     */
    @JsonProperty("Contents")
    public List<BoxCon> getBoxCons() {
        return boxCons;
    }

    /**
     * 

     * Corresponds to the "Contents" property.
     * 
     */
    @JsonProperty("Contents")
    public void setBoxCons(List<BoxCon> boxCons) {
        this.boxCons = boxCons;
    }
    
    public void addContent(BoxCon item)
    {
    	this.boxCons.add(item);
    }

    public void clearContent()
    {
    	this.boxCons.clear();
    }
}

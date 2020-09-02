
package com.intumit.citi.backend;

import java.util.*;
import com.fasterxml.jackson.annotation.*;
import com.intumit.citi.*;
import java.io.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "ID",
    "EMAIL",
    "Infos",
    "Result"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardInfo implements Serializable{

    @JsonProperty("ID")
    private String id;
    @JsonProperty("EMAIL")
    private String email;
    @JsonProperty("Infos")
    private List<Info> infos = new ArrayList<Info>();
    @JsonProperty("Result")
    private Result result;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public CardInfo() {
    }

    /**
     * 
     * @param result
     * @param id
     * @param email
     * @param infos
     */
    public CardInfo(String id, String email, List<Info> infos, Result result) {
        super();
        this.id = id;
        this.email = email;
        this.infos = infos;
        this.result = result;
    }

    @JsonProperty("ID")
    public String getId() {
        return id;
    }

    @JsonProperty("ID")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("EMAIL")
    public String getEmail() {
        return email;
    }

    @JsonProperty("EMAIL")
    public void setEmail(String email) {
        this.email = email;
    }

    @JsonProperty("Infos")
    public List<Info> getInfos() {
        return infos;
    }

    @JsonProperty("Infos")
    public void setInfos(List<Info> infos) {
        this.infos = infos;
        for(Info info: infos)
        {
        	this.setAdditionalProperty(info.getLogo(), info);
        }
    }

    @JsonProperty("Result")
    public Result getResult() {
        return result;
    }

    @JsonProperty("Result")
    public void setResult(Result result) {
        this.result = result;
    }

    public boolean hasAdditionalProperties(String name) {
        return this.additionalProperties.containsKey(name);
    }

    @JsonAnyGetter
    public Info getAdditionalProperties(String name) {
        return (Info)(this.additionalProperties.get(name));
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}

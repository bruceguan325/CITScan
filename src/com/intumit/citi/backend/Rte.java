
package com.intumit.citi.backend;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "AMOUNT",
    "INTEREST",
    "TENOR"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Rte implements Serializable{

    @JsonProperty("AMOUNT")
    private String amount;
    @JsonProperty("INTEREST")
    private String interest;
    @JsonProperty("TENOR")
    private String tenor;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Rte() {
    }

    /**
     * 
     * @param amount
     * @param tenor
     * @param interest
     */
    public Rte(String amount, String interest, String tenor) {
        super();
        this.amount = amount;
        this.interest = interest;
        this.tenor = tenor;
    }

    @JsonProperty("AMOUNT")
    public String getAmount() {
        return amount;
    }

    @JsonProperty("AMOUNT")
    public void setAmount(String amount) {
        this.amount = amount;
    }

    @JsonProperty("INTEREST")
    public String getInterest() {
        return interest;
    }

    @JsonProperty("INTEREST")
    public void setInterest(String interest) {
        this.interest = interest;
    }

    @JsonProperty("TENOR")
    public String getTenor() {
        return tenor;
    }

    @JsonProperty("TENOR")
    public void setTenor(String tenor) {
        this.tenor = tenor;
    }

}

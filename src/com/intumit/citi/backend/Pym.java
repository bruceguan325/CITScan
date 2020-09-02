
package com.intumit.citi.backend;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "TXNDATE",
    "DUEDATE",
    "TXNCODE",
    "TXNDESC",
    "TXNAMOUNT"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Pym implements Serializable{

    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd", timezone = "UTC+8")
    @JsonProperty("TXNDATE")
    private String txndate;
    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd", timezone = "UTC+8")
    @JsonProperty("DUEDATE")
    private String duedate;
    @JsonProperty("TXNCODE")
    private String txncode;
    @JsonProperty("TXNDESC")
    private String txndesc;
    @JsonProperty("TXNAMOUNT")
    private String txnamount;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Pym() {
    }

    /**
     * 
     * @param txncode
     * @param txnamount
     * @param duedate
     * @param txndate
     * @param txndesc
     */
    public Pym(String txndate, String duedate, String txncode, String txndesc, String txnamount) {
        super();
        this.txndate = txndate;
        this.duedate = duedate;
        this.txncode = txncode;
        this.txndesc = txndesc;
        this.txnamount = txnamount;
    }

    @JsonProperty("TXNDATE")
    public String getTxndate() {
        return txndate;
    }

    @JsonProperty("TXNDATE")
    public void setTxndate(String txndate) {
        this.txndate = txndate;
    }

    @JsonProperty("DUEDATE")
    public String getDuedate() {
        return duedate;
    }

    @JsonProperty("DUEDATE")
    public void setDuedate(String duedate) {
        this.duedate = duedate;
    }

    @JsonProperty("TXNCODE")
    public String getTxncode() {
        return txncode;
    }

    @JsonProperty("TXNCODE")
    public void setTxncode(String txncode) {
        this.txncode = txncode;
    }

    @JsonProperty("TXNDESC")
    public String getTxndesc() {
        return txndesc;
    }

    @JsonProperty("TXNDESC")
    public void setTxndesc(String txndesc) {
        this.txndesc = txndesc;
    }

    @JsonProperty("TXNAMOUNT")
    public String getTxnamount() {
        return txnamount;
    }

    @JsonProperty("TXNAMOUNT")
    public void setTxnamount(String txnamount) {
        this.txnamount = txnamount;
    }

}

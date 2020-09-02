
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
    "CARDNO",
    "PRIM",
    "DATE",
    "TXNDATE",
    "AMOUNT",
    "DESC",
    "INSTEREST",
    "TXNCURRENCYAMOUNT",
    "TXNCURRENCY_CH",
    "TXNCURRENCY_EN"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Txn implements Serializable{

    @JsonProperty("CARDNO")
    private String cardno;
    @JsonProperty("PRIM")
    private String prim;
    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd", timezone = "UTC+8")
    @JsonProperty("DATE")
    private String date;
    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd", timezone = "UTC+8")
    @JsonProperty("TXNDATE")
    private String txndate;
    @JsonProperty("AMOUNT")
    private String amount;
    @JsonProperty("DESC")
    private String desc;
    @JsonProperty("INSTEREST")
    private String insterest;
    @JsonProperty("TXNCURRENCYAMOUNT")
    private String txncurrencyamount;
    @JsonProperty("TXNCURRENCY_CH")
    private String txncurrencych;
    @JsonProperty("TXNCURRENCY_EN")
    private String txncurrencyen;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Txn() {
    }

    /**
     * 
     * @param date
     * @param amount
     * @param insterest
     * @param txndate
     * @param desc
     */
    public Txn(String date, String txndate, String amount, String desc, String insterest) {
        super();
        this.date = date;
        this.txndate = txndate;
        this.amount = amount;
        this.desc = desc;
        this.insterest = insterest;
    }

    @JsonProperty("CARDNO")
    public String getCardno() {
        return cardno;
    }

    @JsonProperty("CARDNO")
    public void setCardno(String cardno) {
        this.cardno = cardno;
    }

    @JsonProperty("PRIM")
    public String getPrim() {
        return prim;
    }

    @JsonProperty("PRIM")
    public void setPrim(String prim) {
        this.prim = prim;
    }

    @JsonProperty("DATE")
    public String getDate() {
        return date;
    }

    @JsonProperty("DATE")
    public void setDate(String date) {
        this.date = date;
    }

    @JsonProperty("TXNDATE")
    public String getTxndate() {
        return txndate;
    }

    @JsonProperty("TXNDATE")
    public void setTxndate(String txndate) {
        this.txndate = txndate;
    }

    @JsonProperty("AMOUNT")
    public String getAmount() {
        return amount;
    }

    @JsonProperty("AMOUNT")
    public void setAmount(String amount) {
        this.amount = amount;
    }

    @JsonProperty("DESC")
    public String getDesc() {
        return desc;
    }

    @JsonProperty("DESC")
    public void setDesc(String desc) {
        this.desc = desc;
    }

    @JsonProperty("INSTEREST")
    public String getInsterest() {
        return insterest;
    }

    @JsonProperty("INSTEREST")
    public void setInsterest(String insterest) {
        this.insterest = insterest;
    }
    
    @JsonProperty("TXNCURRENCYAMOUNT")
    public String getTxncurrencyamount() {
        return txncurrencyamount;
    }

    @JsonProperty("TXNCURRENCYAMOUNT")
    public void setTxncurrencyamount(String txncurrencyamount) {
        this.txncurrencyamount = txncurrencyamount;
    }
    
    @JsonProperty("TXNCURRENCY_CH")
    public String getTxncurrencych() {
        return txncurrencych;
    }

    @JsonProperty("TXNCURRENCY_CH")
    public void setTxncurrencych(String txncurrencych) {
        this.txncurrencych = txncurrencych;
    }
    
    @JsonProperty("TXNCURRENCY_EN")
    public String getTxncurrencyen() {
        return txncurrencyen;
    }

    @JsonProperty("TXNCURRENCY_EN")
    public void setTxncurrencyen(String txncurrencyen) {
        this.txncurrencyen = txncurrencyen;
    }

}

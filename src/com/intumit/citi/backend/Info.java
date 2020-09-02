
package com.intumit.citi.backend;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "CARDNO",
    "CARTYPE",
    "LOGO",
    "CARDTYPE",
    "BLKCD",
    "PRIM",
    "STMTDAY",
    "CR_L",
    "CCL",
    "AVL_POINT",
    "AUTOPAY",
    "TOT_AMT_DUE",
    "END_BAL",
    "DUE_DT",
    "CURR_BAL",
    "AVAIL_CL",
    "AVL_CREDIT",
    "Txns",
    "RTE",
    "Pyms",
    "EMAIL",
    "ESTMT"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Info implements Serializable {

    @JsonProperty("CARDNO")
    private String cardno;
    @JsonProperty("CARTYPE")
    private String cartype;
    @JsonProperty("LOGO")
    private String logo;
    @JsonProperty("CARDTYPE")
    private String cardtype;
    @JsonProperty("BLKCD")
    private String blkcd;
    @JsonProperty("PRIM")
    private String prim;
    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd", timezone = "UTC+8")
    @JsonProperty("STMTDAY")
    private String stmtday;
    @JsonProperty("CR_L")
    private String crL;
    @JsonProperty("CCL")
    private String ccl;
    @JsonProperty("AVL_POINT")
    private String avlPoint;
    @JsonProperty("AUTOPAY")
    private String autopay;
    @JsonProperty("TOT_AMT_DUE")
    private String totAmtDue;
    @JsonProperty("END_BAL")
    private String endBal;
    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd", timezone = "UTC+8")
    @JsonProperty("DUE_DT")
    private String dueDt;
    @JsonProperty("CURR_BAL")
    private String currBal;
    @JsonProperty("AVAIL_CL")
    private String availCl;
    @JsonProperty("AVL_CREDIT")
    private String avlCredit;
    @JsonProperty("Txns")
    private List<Txn> txns = new ArrayList<Txn>();
    @JsonProperty("RTE")
    private Rte rte;
    @JsonProperty("Pyms")
    private List<Pym> pyms = new ArrayList<Pym>();
    @JsonProperty("EMAIL")
    private String email;
    @JsonProperty("ESTMT")
    private String estmt;
    
    /**
     * No args constructor for use in serialization
     * 
     */
    public Info() {
    }

    @JsonProperty("CARDNO")
    public String getCardno() {
        return cardno;
    }

    @JsonProperty("CARDNO")
    public void setCardno(String cardno) {
        this.cardno = cardno;
    }

    @JsonProperty("CARTYPE")
    public String getCartype() {
        return cartype;
    }

    @JsonProperty("CARTYPE")
    public void setCartype(String cartype) {
        this.cartype = cartype;
    }

    @JsonProperty("LOGO")
    public String getLogo() {
        return logo;
    }

    @JsonProperty("LOGO")
    public void setLogo(String logo) {
        this.logo = logo;
    }

    @JsonProperty("CARDTYPE")
    public String getCardtype() {
        if (null == cardtype) cardtype = "100";
        return cardtype;
    }

    @JsonProperty("CARDTYPE")
    public void setCardtype(String cardtype) {
        this.cardtype = cardtype;
    }

    @JsonProperty("BLKCD")
    public String getBlkcd() {
        return blkcd;
    }

    @JsonProperty("BLKCD")
    public void setBlkcd(String blkcd) {
        this.blkcd = blkcd;
    }

    @JsonProperty("PRIM")
    public String getPrim() {
        return prim;
    }

    @JsonProperty("PRIM")
    public void setPrim(String prim) {
        this.prim = prim;
    }

    @JsonProperty("STMTDAY")
    public String getStmtday() {
        return stmtday;
    }

    @JsonProperty("STMTDAY")
    public void setStmtday(String stmtday) {
        this.stmtday = stmtday;
    }

    @JsonProperty("CR_L")
    public String getCrL() {
        return crL;
    }

    @JsonProperty("CR_L")
    public void setCrL(String crL) {
        this.crL = crL;
    }

    @JsonProperty("CCL")
    public String getCcl() {
        return ccl;
    }

    @JsonProperty("CCL")
    public void setCcl(String ccl) {
        this.ccl = ccl;
    }

    @JsonProperty("AVL_POINT")
    public String getAvlPoint() {
        return avlPoint;
    }

    @JsonProperty("AVL_POINT")
    public void setAvlPoint(String avlPoint) {
        this.avlPoint = avlPoint;
    }

    @JsonProperty("AUTOPAY")
    public String getAutopay() {
        return autopay;
    }

    @JsonProperty("AUTOPAY")
    public void setAutopay(String autopay) {
        this.autopay = autopay;
    }

    @JsonProperty("TOT_AMT_DUE")
    public String getTotAmtDue() {
        return totAmtDue;
    }

    @JsonProperty("TOT_AMT_DUE")
    public void setTotAmtDue(String totAmtDue) {
        this.totAmtDue = totAmtDue;
    }

    @JsonProperty("END_BAL")
    public String getEndBal() {
        return endBal;
    }

    @JsonProperty("END_BAL")
    public void setEndBal(String endBal) {
        this.endBal = endBal;
    }

    @JsonProperty("DUE_DT")
    public String getDueDt() {
        return dueDt;
    }

    @JsonProperty("DUE_DT")
    public void setDueDt(String dueDt) {
        this.dueDt = dueDt;
    }

    @JsonProperty("CURR_BAL")
    public String getCurrBal() {
        if (null == currBal) currBal = "0";
        return currBal;
    }

    @JsonProperty("CURR_BAL")
    public void setCurrBal(String currBal) {
        this.currBal = currBal;
    }

    @JsonProperty("AVAIL_CL")
    public String getAvailCl() {
        if (null == availCl) availCl = "0";
        return availCl;
    }

    @JsonProperty("AVAIL_CL")
    public void setAvailCl(String availCl) {
        this.availCl = availCl;
    }

    @JsonProperty("AVL_CREDIT")
    public String getAvlCredit() {
        return avlCredit;
    }

    @JsonProperty("AVL_CREDIT")
    public void setAvlCredit(String avlCredit) {
        this.avlCredit = avlCredit;
    }

    @JsonProperty("Txns")
    public List<Txn> getTxns() {
        return txns;
    }

    @JsonProperty("Txns")
    public void setTxns(List<Txn> txns) {
        this.txns = txns;
    }

    @JsonProperty("RTE")
    public Rte getRte() {
        return rte;
    }

    @JsonProperty("RTE")
    public void setRte(Rte rte) {
        this.rte = rte;
    }

    @JsonProperty("Pyms")
    public List<Pym> getPyms() {
        return pyms;
    }

    @JsonProperty("Pyms")
    public void setPyms(List<Pym> pyms) {
        this.pyms = pyms;
    }

    @JsonProperty("EMAIL")
    public String getEmail() {
        return email;
    }

    @JsonProperty("EMAIL")
    public void setEmail(String email) {
        this.email = email;
    }

    @JsonProperty("ESTMT")
    public String getEstmt() {
        return estmt;
    }

    @JsonProperty("ESTMT")
    public void setEstmt(String estmt) {
        this.estmt = estmt;
    }

}

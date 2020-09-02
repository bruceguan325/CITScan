package com.intumit.solr.keywords;

import java.util.List;

public class WikiKeyword {
    public String keyword;
    public String lang;
    public int num;   //pages
    public int linkFrom;  //linkFrom
    public int score;
    public String status;
    public String creator;
    public List<String> keywords;
    public String getKeyword() {
        return keyword;
    }
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    public String getLang() {
        return lang;
    }
    public void setLang(String lang) {
        this.lang = lang;
    }
    public int getNum() {
        return num;
    }
    public void setNum(int num) {
        this.num = num;
    }
    public int getLinkFrom() {
        return linkFrom;
    }
    public void setLinkFrom(int linkFrom) {
        this.linkFrom = linkFrom;
    }
    public int getScore() {
        return score;
    }
    public void setScore(int score) {
        this.score = score;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getCreator() {
        return creator;
    }
    public void setCreator(String creator) {
        this.creator = creator;
    }
    public List<String> getKeywords() {
        return keywords;
    }
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }
   
}

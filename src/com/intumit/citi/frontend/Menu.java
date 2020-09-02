package com.intumit.citi.frontend;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.intumit.citi.Result;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "Result",
    "Messages",
    "Box"
})
public class Menu {

    @JsonProperty("Result")
    private Result result;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("Messages")
    private List<Message> messages = new ArrayList<Message>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("Box")
    private Box box;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Menu() {
    }

    /**
     * 
     * @param result
     * @param messages
     * @param box
     */
    public Menu(Result result, List<Message> messages, Box box) {
        super();
        this.result = result;
        this.messages = messages;
        this.box = box;
    }

    @JsonProperty("Result")
    public Result getResult() {
        return result;
    }

    @JsonProperty("Result")
    public void setResult(Result result) {
        this.result = result;
    }

    @JsonProperty("Messages")
    public List<Message> getMessages() {
        return messages;
    }

    @JsonProperty("Messages")
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    @JsonProperty("Box")
    public Box getBox() {
        return box;
    }

    @JsonProperty("Box")
    public void setBox(Box box) {
        this.box = box;
    }

    public void addMessage(Message item) {
        this.messages.add(item);
    }
    
    public void clearAction() {
        this.messages.clear();
    }
}
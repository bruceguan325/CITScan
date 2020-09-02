
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
    "Actions"
})
public class MessageButtons
    extends Message
{

    @JsonProperty("Text")
    private String text;
    @JsonProperty("Actions")
    private List<Action> actions = new ArrayList<Action>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public MessageButtons() {
    }

    /**
     * 
     * @param text
     * @param id
     * @param type
     * @param actions
     */
    public MessageButtons(String text, List<Action> actions, String id, Message.Type type) {
        super(id, type);
        this.text = text;
        this.actions = actions;
    }

    @JsonProperty("Text")
    public String getText() {
        return text;
    }

    @JsonProperty("Text")
    public void setText(String text) {
        this.text = text;
    }

    @JsonProperty("Actions")
    public List<Action> getActions() {
        return actions;
    }

    @JsonProperty("Actions")
    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public void addAction(Action item) {
        this.actions.add(item);
    }
    
    public void clearAction() {
        this.actions.clear();
    }
}

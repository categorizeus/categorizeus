package us.categorize.api;
import us.categorize.model.*;
import java.util.*;

public class TagSearchRequest{
    private List<Tag> tags;
    private Message lastKnownMessage;
    private boolean findAfter = true;
    private int maximumResults = 10;
    public TagSearchRequest(){
        
    }
    
    public int getMaximumResults(){
        return maximumResults;
    }
    
    public void setMaximumResults(int max){
        maximumResults = max;
    }
    
    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public Message getLastKnownMessage() {
        return lastKnownMessage;
    }

    public void setLastKnownMessage(Message lastKnownMessage) {
        this.lastKnownMessage = lastKnownMessage;
    }

    public boolean getFindAfter() {
        return findAfter;
    }

    public void setFindAfter(boolean findAfter) {
        this.findAfter = findAfter;
    }
}

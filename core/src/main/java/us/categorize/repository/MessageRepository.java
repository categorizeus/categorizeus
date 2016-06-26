package us.categorize.repository;

import java.util.List;

import us.categorize.model.Message;
import us.categorize.model.MessageThread;
import us.categorize.model.Tag;

public interface MessageRepository {
	Message getMessage(String id);
	boolean addMessage(Message message);//are messages immutable?
	boolean postMessage(String body);//#TODO figure this out, for the post stuff
	MessageThread getThread(String id);
	List<Message> findMessages(List<Tag> tags);
	List<Message> findThreads(List<Tag> tags);
	boolean tag(Message message, List<Tag> tags);
	
}
package chat.client;

public interface ChatGui {
	void notifyParticipantsChanged(String[] names);
	void notifySpoken(String speaker, String sentence);
	void notifyWisper(String speaker, String sentence);
	void dispose();
}
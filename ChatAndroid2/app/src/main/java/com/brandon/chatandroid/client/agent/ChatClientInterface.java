package com.brandon.chatandroid.client.agent;

/**
 * Interface implémentant la logique du client de Chat sur le terminal utilisateur.
 */

public interface ChatClientInterface {
	public void handleSpoken(String s);
	public void handleWisper(String s, String name);
	public String[] getParticipantLocalNames();
	public String[] getParticipantNames();
}
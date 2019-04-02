package chat.ontology;

import java.util.List;

import jade.content.Predicate;
import jade.core.AID;

/**
 * Prédicat du départ d'un agent
 */
public class Left implements Predicate {

	private List<AID> _who;

	public void setWho(List<AID> who) {
		_who = who;
	}

	public List<AID> getWho() {
		return _who;
	}

}
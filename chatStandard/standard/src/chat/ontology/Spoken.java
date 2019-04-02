package chat.ontology;

import jade.content.Predicate;

/**
 * Prédicat de l'échange de message
 */

public class Spoken implements Predicate {

	private String _what;

	public void setWhat(String what) {
		_what = what;
	}

	public String getWhat() {
		return _what;
	}

}
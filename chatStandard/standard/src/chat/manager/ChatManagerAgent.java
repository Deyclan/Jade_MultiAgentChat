package chat.manager;

import jade.core.Agent;
import jade.core.AID;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.BasicOntology;
import jade.content.abs.*;

import jade.proto.SubscriptionResponder;
import jade.proto.SubscriptionResponder.SubscriptionManager;
import jade.proto.SubscriptionResponder.Subscription;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.FailureException;

import jade.domain.introspection.IntrospectionOntology;
import jade.domain.introspection.Event;
import jade.domain.introspection.DeadAgent;
import jade.domain.introspection.AMSSubscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import chat.ontology.*;

/**
 * Cet agent permet de conserver la liste des agents connectés au chat
 * et informe de l'arrivé ou du départ d'agent au sein du chat.
 */
public class ChatManagerAgent extends Agent implements SubscriptionManager {

	private Map<AID, Subscription> participants = new HashMap<AID, Subscription>();
	private Codec codec = new SLCodec();
	private Ontology ontology = ChatOntology.getInstance();
	private AMSSubscriber aMSSubscriber;

	protected void setup() {
        // Enregistrement du langage et du codec pour accepter les inscriptions au
        // chat par les autres agents.
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		// On crée le modèle de message à recevoir pour l'inscription
		MessageTemplate sTemplate = MessageTemplate.and(
				MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE),
				MessageTemplate.and(
						MessageTemplate.MatchLanguage(codec.getName()),
						MessageTemplate.MatchOntology(ontology.getName())));

		// On affecte le comportement de réponse a une inscription
		addBehaviour(new SubscriptionResponder(this, sTemplate, this));

        // On s'enregistre auprès de l'AMS pour détecter quand un agent client est soudainement déconnecté
		aMSSubscriber = new AMSSubscriber() {
			protected void installHandlers(Map handlersTable) {
				handlersTable.put(IntrospectionOntology.DEADAGENT, new EventHandler() {
					public void handle(Event ev) {
						DeadAgent da = (DeadAgent)ev;
						AID id = da.getAgent();
                        // Si un agent est déconnecté, on notifie tous les autres agents.
						if (participants.containsKey(id)) {
							try {
								deregister((Subscription) participants.get(id));
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				});
			}
		};
		addBehaviour(aMSSubscriber);
	}

	protected void takeDown() {
		send(aMSSubscriber.getCancel());
	}



	public boolean register(Subscription s) throws RefuseException, NotUnderstoodException { 
		try {
			AID newId = s.getMessage().getSender();
			// Notifie les participants de l'arrivée d'un nouvel agent.
			if (!participants.isEmpty()) {
				// Message pour le nouvel arrivant
				ACLMessage notif1 = s.getMessage().createReply();
				notif1.setPerformative(ACLMessage.INFORM);

				// Message pour les "anciens" participants
				ACLMessage notif2 = (ACLMessage) notif1.clone();
				notif2.clearAllReceiver();
				Joined joined = new Joined();
				List<AID> who = new ArrayList<AID>(1);
				who.add(newId);
				joined.setWho(who);
				getContentManager().fillContent(notif2, joined);

				who.clear();
				Iterator<AID> it = participants.keySet().iterator();
				while (it.hasNext()) {
					AID oldId = it.next();
					
					// On notifie les anciens participants
					Subscription oldS = (Subscription) participants.get(oldId);
					oldS.notify(notif2);
					
					who.add(oldId);
				}

				// On notifie le nouveau participant
				getContentManager().fillContent(notif1, joined);
				s.notify(notif1);
			}
			
			// Ajoute le nouvel inscrit dans la liste des participants
			participants.put(newId, s);
			return false;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RefuseException("Subscription error");
		}		
	}

	public boolean deregister(Subscription s) throws FailureException {
		AID oldId = s.getMessage().getSender();
		// Désinscrit un participant
		if (participants.remove(oldId) != null) {
			// Notifie les autres participants du départ
			if (!participants.isEmpty()) {
				try {
					ACLMessage notif = s.getMessage().createReply();
					notif.setPerformative(ACLMessage.INFORM);
					notif.clearAllReceiver();
					AbsPredicate p = new AbsPredicate(ChatOntology.LEFT);
					AbsAggregate agg = new AbsAggregate(BasicOntology.SEQUENCE);
					agg.add((AbsTerm) BasicOntology.getInstance().fromObject(oldId));
					p.set(ChatOntology.LEFT_WHO, agg);
					getContentManager().fillContent(notif, p);

					Iterator it = participants.values().iterator();
					while (it.hasNext()) {
						Subscription s1 = (Subscription) it.next();
						s1.notify(notif);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}
}

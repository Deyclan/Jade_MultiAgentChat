package com.brandon.chatandroid.client.agent;

import android.content.Context;
import android.content.Intent;

import java.util.List;
import java.util.logging.Level;

import chat.ontology.ChatOntology;
import chat.ontology.Joined;
import chat.ontology.Left;
import jade.content.ContentManager;
import jade.content.Predicate;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.util.leap.Iterator;
import jade.util.leap.Set;
import jade.util.leap.SortedSetImpl;

public class ChatClientAgent extends Agent implements ChatClientInterface {
    private static final long serialVersionUID = 1594371294421614291L;

    private Logger logger = Logger.getJADELogger(this.getClass().getName());

    private static final String CHAT_ID = "__chat__";
    private static final String CHAT_MANAGER_NAME = "manager";

    private Set participants = new SortedSetImpl();
    private Codec codec = new SLCodec();
    private Ontology ontology = ChatOntology.getInstance();
    private ACLMessage spokenMsg;

    private Context context;

    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            if (args[0] instanceof Context) {
                context = (Context) args[0];
            }
        }

        // Répértorie le language et l'ontology
        ContentManager cm = getContentManager();
        cm.registerLanguage(codec);
        cm.registerOntology(ontology);
        cm.setValidationMode(false);

        // Ajoute les behaviours initiaux
        addBehaviour(new ParticipantsManager(this));
        addBehaviour(new ChatListener(this));
        addBehaviour(new WisperListener(this));

        // Initialise le message utiliser pour parler dans le chat
        spokenMsg = new ACLMessage(ACLMessage.INFORM);
        spokenMsg.setConversationId(CHAT_ID);

        // Active l'interface de l'agent
        registerO2AInterface(ChatClientInterface.class, this);

        // Navigation vers l'activité de chat
        Intent broadcast = new Intent();
        broadcast.setAction("SHOW_CHAT");
        logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
        context.sendBroadcast(broadcast);
    }

    protected void takeDown() {
    }

    // Notifie ue la liste des participants à changé
    private void notifyParticipantsChanged() {
        Intent broadcast = new Intent();
        broadcast.setAction("REFRESH_PARTICIPANTS");
        logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
        context.sendBroadcast(broadcast);
    }

    // Notifie que quelqu'un a envoyé un message dans le chat
    private void notifySpoken(String speaker, String sentence) {
        Intent broadcast = new Intent();
        broadcast.setAction("REFRESH_CHAT");
        broadcast.putExtra("sentence", speaker + ": " + sentence + "\n");
        logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
        context.sendBroadcast(broadcast);
    }

    // Notifie que quelqu'un à envoyé un message privé
    private void notifyWisper(String speaker, String sentence) {
        Intent broadcast = new Intent();
        broadcast.setAction("REFRESH_CHAT");
        broadcast.putExtra("sentence", "[WISPER]" + speaker + ": " + sentence + "\n");
        logger.log(Level.INFO, "Sending broadcast " + broadcast.getAction());
        context.sendBroadcast(broadcast);
    }

    /**
     * Cyclic behavior permettant de s'enregistrer comme participant au chat
     * et permet de garder la liste des partcicipants à jour en traitant les informations
     * reçues de l'agent ChatManager (sur le "serveur")
     */
    class ParticipantsManager extends CyclicBehaviour {
        private static final long serialVersionUID = -4845730529175649756L;
        private MessageTemplate template;

        ParticipantsManager(Agent a) {
            super(a);
        }

        public void onStart() {
            // Inscription comme participant au chat auprès du ChatManager (sur le "serveur")
            // Création du message de subscribe
            ACLMessage subscription = new ACLMessage(ACLMessage.SUBSCRIBE);
            subscription.setLanguage(codec.getName());
            subscription.setOntology(ontology.getName());
            String conversationId = "C-" + myAgent.getLocalName();
            subscription.setConversationId(conversationId);
            subscription.addReceiver(new AID(CHAT_MANAGER_NAME, AID.ISLOCALNAME)); // Destinataire = manager
            myAgent.send(subscription);

            // Initialise le modèle de message utilisé pour recevoir les notifications du ChatManager
            template = MessageTemplate.MatchConversationId(conversationId);
        }

        public void action() {
            // Recoit les messages du ChatManager notifiant des changements des participants
            // (départs et arrivées)
            ACLMessage msg = myAgent.receive(template);
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.INFORM) {
                    try {
                        Predicate p = (Predicate) myAgent.getContentManager().extractContent(msg);
                        if (p instanceof Joined) {
                            Joined joined = (Joined) p;
                            List<AID> aid = (List<AID>) joined.getWho();
                            for (AID a : aid) {
                                participants.add(a);
                            }
                            notifyParticipantsChanged();
                        }
                        if (p instanceof Left) {
                            Left left = (Left) p;
                            List<AID> aid = (List<AID>) left.getWho();
                            for (AID a : aid)
                                participants.remove(a);
                            notifyParticipantsChanged();
                        }
                    } catch (Exception e) {
                        Logger.println(e.toString());
                        e.printStackTrace();
                    }
                } else {
                    handleUnexpected(msg);
                }
            } else {
                block();
            }
        }
    }

    /**
     * Cyclic behavior permettant d'écouter et recevoir les messages envoyés
     * sur la conversation générale.
     */
    class ChatListener extends CyclicBehaviour {
        private static final long serialVersionUID = 741233963737842521L;

        // Initialisation du modèle de message à recevoir
        private MessageTemplate template = MessageTemplate.MatchConversationId(CHAT_ID);

        ChatListener(Agent a) {
            super(a);
        }

        public void action() {
            // Récéption du message
            ACLMessage msg = myAgent.receive(template);
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.INFORM) {
                    // Notifie que quelqu'un à envoyé un message sur le chat
                    notifySpoken(msg.getSender().getLocalName(), msg.getContent());
                } else {
                    handleUnexpected(msg);
                }
            } else {
                block();
            }
        }
    }

    /**
     * Cyclic Behaviour permettant d'écouter et de recevoir les messages privés qui sont envoyés
     * à l'agent possèdant ce comportement.
     */
    class WisperListener extends CyclicBehaviour {
        private static final long serialVersionUID = 741233963737842521L;
        private String conversationId;
        private MessageTemplate wisperTemplate;

        WisperListener(Agent a) {
            super(a);

            // Initialisation du modèle de messasge à recevoir
            this.conversationId = a.getLocalName();
            this.wisperTemplate = MessageTemplate.MatchConversationId(conversationId);
        }

        public void action() {
            // Récéption du message
            ACLMessage msg = myAgent.receive(wisperTemplate);
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.INFORM) {
                    // On notifie l'interface qu'un message privé à été reçu
                    notifyWisper(msg.getSender().getLocalName(), msg.getContent().substring(msg.getContent().split("/")[1].split(" ")[0].length() +2));
                } else {
                    handleUnexpected(msg);
                }
            } else {
                block();
            }
        }
    }

    /**
     * OneShotBehavior permettant d'envoyer un message à tous les agents
     * sur le chat principal.
     */
    private class ChatSpeaker extends OneShotBehaviour {
        private static final long serialVersionUID = -1426033904935339194L;
        private String sentence;

        private ChatSpeaker(Agent a, String s) {
            super(a);
            sentence = s;
        }

        public void action() {
            // Met a jour la liste des agents qui doivent recevoir le message
            spokenMsg.clearAllReceiver();
            Iterator it = participants.iterator();
            while (it.hasNext()) {
                spokenMsg.addReceiver((AID) it.next());
            }
            spokenMsg.setContent(sentence);
            // Notifie l'interface graphique qu'un message est envoyé sur le chat
            notifySpoken(myAgent.getLocalName(), sentence);
            // Envoie le message aux agents destinataires.
            send(spokenMsg);
        }
    }

    /**
     * OneShotBehavior permettant d'envoyer un message privé à un seul des agents
     * connectés sur le chat
     */
    private class WisperSpeaker extends OneShotBehaviour {
        private static final long serialVersionUID = -1536015404964822113L;
        private String sentence;
        private String convId;
        private AID receiver;

        private WisperSpeaker(Agent thisAgent, AID receiver, String sentence, String conversationId) {
            super(thisAgent);
            this.sentence = sentence;
            this.convId = conversationId;
            this.receiver = receiver;
        }

        public void action() {
            // Créaation du message, ajout du contenu et du destinataire
            ACLMessage wisper = new ACLMessage(ACLMessage.INFORM);
            wisper.setConversationId(convId);
            wisper.addReceiver(receiver);
            wisper.setContent(sentence);
            // On notifie l'interface graphique qu'un message privé à été envoyé
            notifyWisper(myAgent.getLocalName(), sentence.substring(sentence.split("/")[1].split(" ")[0].length()+2));
            // On envoie le message
            send(wisper);
        }
    }

    /**
     * Méhtode appelées par l'interface (Dans les activity de l'application)
     */

    /**
     * Envoi de message sur le chat général
     */
    public void handleSpoken(String s) {
        // Ajout d'un ChatSpeaker Behaviour pour envoyer le message s
        addBehaviour(new ChatSpeaker(this, s));
    }

    /**
     * Envoi d'un message privé à un des agents connectés.
     */
    public void handleWisper(String s, String name){
        // On parcours l'ensemble des agents connectés
        Iterator it = participants.iterator();
        while (it.hasNext()) {
            AID id = (AID) it.next();
            // Si on trouve l'agent spécifié par le message, on ajoute un WisperSpeaker Behaviour
            // pour lui envoyer le message privé
            if(id.getLocalName().equals(name)){
                addBehaviour(new WisperSpeaker(this,id, s, id.getLocalName()));
            }
        }
    }

    /**
     * Récupère la liste des pseudo des participants connectés au chat
     */
    public String[] getParticipantLocalNames() {
        String[] pp = new String[participants.size()];
        Iterator it = participants.iterator();
        int i = 0;
        while (it.hasNext()) {
            AID id = (AID) it.next();
            pp[i++] = id.getLocalName();
        }
        return pp;
    }

    /**
     * Récupère la liste des pseudo@adresseIp des participants connectés au chat
     */
    public String[] getParticipantNames() {
        String[] pp = new String[participants.size()];
        Iterator it = participants.iterator();
        int i = 0;
        while (it.hasNext()) {
            AID id = (AID) it.next();
            pp[i++] = id.getName();
        }
        return pp;
    }

    /**
     * Gestion des messages non pris en charge.
     */
    private void handleUnexpected(ACLMessage msg) {
        if (logger.isLoggable(Logger.WARNING)) {
            logger.log(Logger.WARNING, "Unexpected message received from "
                    + msg.getSender().getName());
            logger.log(Logger.WARNING, "Content is: " + msg.getContent());
        }
    }

}

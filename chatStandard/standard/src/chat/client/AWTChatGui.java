package chat.client;

import java.awt.*;
import java.awt.event.*;

import chat.client.agent.ChatClientAgent;

public class AWTChatGui extends Frame implements ChatGui {
    private ChatClientAgent myAgent;
    private TextField writeTf;
    private TextArea allTa;
    private ParticipantsFrame participantsFrame;

    public AWTChatGui(ChatClientAgent a) {
        myAgent = a;

        setTitle("Chat: " + myAgent.getLocalName());
        setSize(getProperSize(256, 320));
        Panel p = new Panel();
        p.setLayout(new BorderLayout());
        writeTf = new TextField();
        p.add(writeTf, BorderLayout.CENTER);
        Button b = new Button("Send");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String s = writeTf.getText();
                System.out.println("Action Performed. String : " + s);
                if (s != null && !s.equals("")) {
                    if(s.charAt(0) == "/".charAt(0)){
                        System.out.println("Sending wisper to "+ s.split("/")[1].split(" ")[0]);
                        myAgent.handleWisper(s, s.split("/")[1].split(" ")[0]);
                        writeTf.setText("");
                    }else {
                        myAgent.handleSpoken(s);
                        writeTf.setText("");
                    }
                }
            }
        });
        p.add(b, BorderLayout.EAST);
        add(p, BorderLayout.NORTH);

        allTa = new TextArea();
        allTa.setEditable(false);
        allTa.setBackground(Color.white);
        add(allTa, BorderLayout.CENTER);

        b = new Button("Participants");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!participantsFrame.isVisible()) {
                    participantsFrame.setVisible(true);
                }
            }
        });
        add(b, BorderLayout.SOUTH);

        participantsFrame = new ParticipantsFrame(this, myAgent.getLocalName());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        });

        show();
    }

    public void notifyParticipantsChanged(String[] names) {
        if (participantsFrame != null) {
            participantsFrame.refresh(names);
        }
    }

    public void notifySpoken(String speaker, String sentence) {
        allTa.append(speaker + ": " + sentence + "\n");
    }

    public void notifyWisper(String speaker, String sentence) {
        allTa.append("[WISPER]" + speaker + ": " + sentence + "\n");
    }


    Dimension getProperSize(int maxX, int maxY) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width < maxX ? screenSize.width : maxX);
        int y = (screenSize.height < maxY ? screenSize.height : maxY);
        return new Dimension(x, y);
    }

    public void dispose() {
        participantsFrame.dispose();
        super.dispose();
    }
}




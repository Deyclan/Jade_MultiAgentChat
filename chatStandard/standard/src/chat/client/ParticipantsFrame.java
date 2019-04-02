package chat.client;

import java.awt.*;
import java.awt.event.*;

class ParticipantsFrame extends Frame {
	private AWTChatGui parent;
	private TextArea participants;
	private String me;
	
	ParticipantsFrame(AWTChatGui parent, String me) {
		this.parent = parent;
		this.me = me;
		
		setTitle("Participants: ");
		setSize(parent.getSize());
		
		participants = new TextArea();
		participants.setEditable(false);
		participants.setBackground(Color.white);
		participants.setText(me+"\n");
		add(participants, BorderLayout.CENTER);
				
		Button b = new Button("Close");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			} 
		} );
		
		add(b, BorderLayout.SOUTH);
		addWindowListener(new	WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				setVisible(false);
			}
		} );
	}
	
	void refresh(String[] ss) {
		participants.setText(me+"\n");
		if (ss != null) {
			for (int i = 0; i < ss.length; ++i) {
				participants.append(ss[i]+"\n");
			}
		}
	}
	
}
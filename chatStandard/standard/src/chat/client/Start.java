package chat.client;

import jade.MicroBoot;
import jade.core.MicroRuntime;
import jade.core.Agent;
import jade.util.leap.Properties;

//#MIDP_EXCLUDE_BEGIN
import java.awt.*;
import java.awt.event.*;

public class Start extends MicroBoot {

    public static void main(String args[]) {
        MicroBoot.main(args);
        NickNameDlg dlg = new NickNameDlg("Chat");
    }

    private static class NickNameDlg extends Frame implements ActionListener {
        private TextField nameTf;
        private TextArea msgTa;

        NickNameDlg(String s) {
            super(s);

            setSize(getProperSize(256, 320));
            Panel p = new Panel();
            p.setLayout(new BorderLayout());
            nameTf = new TextField();
            p.add(nameTf, BorderLayout.CENTER);
            Button b = new Button("OK");
            b.addActionListener(this);
            p.add(b, BorderLayout.EAST);
            add(p, BorderLayout.NORTH);

            msgTa = new TextArea("Enter nickname\n");
            msgTa.setEditable(false);
            msgTa.setBackground(Color.white);
            add(msgTa, BorderLayout.CENTER);

            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    MicroRuntime.stopJADE();
                }
            });

            showCorrect();
        }

        public void actionPerformed(ActionEvent e) {
            String name = nameTf.getText();
            if (!checkName(name)) {
                msgTa.append("Invalid nickname\n");
            } else {
                try {
                    MicroRuntime.startAgent(name, "chat.client.agent.ChatClientAgent", null);
                    dispose();
                } catch (Exception ex) {
                    msgTa.append("Nickname already in use\n");
                }
            }
        }

        private void showCorrect() {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension frameSize = getSize();
            int centerX = (int) screenSize.width / 2;
            int centerY = (int) screenSize.height / 2;
            setLocation(centerX - frameSize.width / 2, centerY - frameSize.height / 2);
            show();
        }

        private Dimension getProperSize(int maxX, int maxY) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = (screenSize.width < maxX ? screenSize.width : maxX);
            int y = (screenSize.height < maxY ? screenSize.height : maxY);
            return new Dimension(x, y);
        }
    }

    private static boolean checkName(String name) {
        if (name == null || name.trim().equals("")) {
            return false;
        }
        return !name.contains(" ");
    }
}
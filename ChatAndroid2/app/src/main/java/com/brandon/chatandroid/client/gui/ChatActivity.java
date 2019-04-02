package com.brandon.chatandroid.client.gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.brandon.chatandroid.R;
import com.brandon.chatandroid.client.agent.ChatClientInterface;

import java.util.logging.Level;

import jade.core.MicroRuntime;
import jade.util.Logger;
import jade.wrapper.ControllerException;
import jade.wrapper.O2AException;
import jade.wrapper.StaleProxyException;

/**
 * Activité de l'interface de chat
 */

public class ChatActivity extends Activity {
    private Logger logger = Logger.getJADELogger(this.getClass().getName());

    static final int PARTICIPANTS_REQUEST = 0;
    private BroadCastListener broadCastListener;

    private String nickname;
    private ChatClientInterface chatClientInterface;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            nickname = extras.getString("nickname");
        }

        try {
            chatClientInterface = MicroRuntime.getAgent(nickname).getO2AInterface(ChatClientInterface.class);
        } catch (StaleProxyException e) {
            showAlertDialog(getString(R.string.msg_interface_exc), true);
        } catch (ControllerException e) {
            showAlertDialog(getString(R.string.msg_controller_exc), true);
        }

        broadCastListener = new BroadCastListener();

        IntentFilter refreshChatFilter = new IntentFilter();
        refreshChatFilter.addAction("REFRESH_CHAT");
        registerReceiver(broadCastListener, refreshChatFilter);

        IntentFilter clearChatFilter = new IntentFilter();
        clearChatFilter.addAction("CLEAR_CHAT");
        registerReceiver(broadCastListener, clearChatFilter);

        setContentView(R.layout.chat);

        Button button = (Button) findViewById(R.id.button_send);
        button.setOnClickListener(buttonSendListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(broadCastListener);

        logger.log(Level.INFO, "Destroy activity!");
    }

    // Action réalisé lors de l'envoi d'un message
    private OnClickListener buttonSendListener = new OnClickListener() {
        public void onClick(View v) {
            final EditText messageField = (EditText) findViewById(R.id.edit_message);
            String message = messageField.getText().toString();
            if (message != null && !message.equals("")) {
                try {
                    // On check si le message commence par /pseudoReceveur
                    // Si c'est le cas, on envoi un message privé au receveur s'il existe
                    // Sinon, on envoi un message sur le chat général
                    if (message.charAt(0) == "/".charAt(0)) {
                        chatClientInterface.handleWisper(message, message.split("/")[1].split(" ")[0]);
                        messageField.setText("");
                    } else {
                        chatClientInterface.handleSpoken(message);
                        messageField.setText("");
                    }
                } catch (O2AException e) {
                    showAlertDialog(e.getMessage(), false);
                }
            }

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_participants:
                Intent showParticipants = new Intent(ChatActivity.this,
                        ParticipantsActivity.class);
                showParticipants.putExtra("nickname", nickname);
                startActivityForResult(showParticipants, PARTICIPANTS_REQUEST);
                return true;
            case R.id.menu_clear:
			/*
			Intent broadcast = new Intent();
			broadcast.setAction("CLEAR_CHAT");
			logger.info("Sending broadcast " + broadcast.getAction());
			sendBroadcast(broadcast);
			*/
                final TextView chatField = (TextView) findViewById(R.id.chatTextView);
                chatField.setText("");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PARTICIPANTS_REQUEST) {
            if (resultCode == RESULT_OK) {

            }
        }
    }


    private class BroadCastListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            logger.log(Level.INFO, "Received intent " + action);
            if (action.equalsIgnoreCase("REFRESH_CHAT")) {
                final TextView chatField = (TextView) findViewById(R.id.chatTextView);
                chatField.append(intent.getExtras().getString("sentence"));
                scrollDown();
            }
            if (action.equalsIgnoreCase("CLEAR_CHAT")) {
                final TextView chatField = (TextView) findViewById(R.id.chatTextView);
                chatField.setText("");
            }
        }
    }

    private void scrollDown() {
        final ScrollView scroller = (ScrollView) findViewById(R.id.scroller);
        final TextView chatField = (TextView) findViewById(R.id.chatTextView);
        scroller.smoothScrollTo(0, chatField.getBottom());
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        final TextView chatField = (TextView) findViewById(R.id.chatTextView);
        savedInstanceState.putString("chatField", chatField.getText()
                .toString());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final TextView chatField = (TextView) findViewById(R.id.chatTextView);
        chatField.setText(savedInstanceState.getString("chatField"));
    }

    private void showAlertDialog(String message, final boolean fatal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                ChatActivity.this);
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog, int id) {
                                dialog.cancel();
                                if (fatal) finish();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
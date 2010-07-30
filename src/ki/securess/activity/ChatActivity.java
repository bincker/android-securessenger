package ki.securess.activity;

import java.util.ArrayList;

import ki.securess.Constants;
import ki.securess.R;
import ki.securess.service.ChatService;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChatActivity extends Activity
{
	ArrayAdapter<String> messagesAdapter;

	public static final int MSG_ERROR_MESSAGE = 1;

	public static final int MSG_CODE_AVAILABLE = MSG_ERROR_MESSAGE + 1;

	public static final int MSG_EXCHANGING_KEYS = MSG_CODE_AVAILABLE + 1;
	
	public static final int MSG_PUBKEY_ARRIVED = MSG_EXCHANGING_KEYS + 1;
	
	public static final int MSG_MESSAGE_ARRIVED = MSG_PUBKEY_ARRIVED + 1;
	
	public static final int MSG_FINISH = MSG_MESSAGE_ARRIVED + 1;
	
	private ChatService service;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);

		//
		// Messages of the chat
		//
		messagesAdapter = new ArrayAdapter<String>(this, R.layout.message);
		if (savedInstanceState != null)
		{
			ArrayList<String> messages = savedInstanceState.getStringArrayList("messages");
			for (String m : messages)
			{
				messagesAdapter.add(m);
			}
		}
		ListView textMessages = (ListView) findViewById(R.id_chat.textMessages);
		textMessages.setAdapter(messagesAdapter);
		
		//
		// Send button
		//
		Button btnSend = (Button) findViewById(R.id_chat.btnSend);
		btnSend.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				TextView t = (TextView) findViewById(R.id_chat.editText);
				sendMessage(t.getText().toString());
				t.setText("");
			}
		});
		if (savedInstanceState != null && savedInstanceState.getBoolean("btnSendEnabled"))
		{
			btnSend.setEnabled(true);
		}

		Bundle b = getIntent().getExtras();
		service = new ChatService(this, message_handler, b.getBoolean("hosting"), b.getString("code"));
		service.start();
	}
	
	protected void onSaveInstanceState(Bundle outState)
	{
		ArrayList<String> list = new ArrayList<String>();
		int ct = messagesAdapter.getCount();
		for (int i = 0; i < ct; i++)
		{
			list.add(messagesAdapter.getItem(ct));
		}
		outState.putStringArrayList("messages", list);
		
		Button btnSend = (Button) findViewById(R.id_chat.btnSend);
		outState.putBoolean("btnSendEnabled", btnSend.isEnabled());
		
		TextView txtMessages = (TextView) findViewById(R.id_chat.notificationText);
		outState.putString("notificationText", txtMessages.getText().toString());
	}
	
	protected void onDestroy()
	{
		service.destroy();
		super.onDestroy();
	};
	
	Handler message_handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case MSG_ERROR_MESSAGE:
				{
					Toast.makeText(ChatActivity.this, msg.arg1, Constants.TOAST_LENGTH).show();
					finish();
					break;
				}
				case MSG_CODE_AVAILABLE:
				{
					//
					// Waiting for connection, show the code for the session
					//
					if (msg.getData() == null || msg.getData().getString("code") == null)
					{
						Toast.makeText(ChatActivity.this, R.string.no_network, Constants.TOAST_LENGTH).show();
						finish();
						return;
					}
					TextView t = (TextView) findViewById(R.id_chat.notificationText);
					t.setText(getText(R.string.waiting_code) + msg.getData().getString("code"));
					t.setVisibility(View.VISIBLE);
					break;
				}
				case MSG_EXCHANGING_KEYS:
				{
					TextView t = (TextView) findViewById(R.id_chat.notificationText);
					t.setText(R.string.exchanging_keys);
					t.setVisibility(View.VISIBLE);
					break;
				}
				case MSG_PUBKEY_ARRIVED:
				{
					enableChat();
					break;
				}
				case MSG_MESSAGE_ARRIVED:
				{
					String message = msg.getData().getString("message");
					messageAppearInChat(false, message);
					break;
				}
				case MSG_FINISH:
				{
					finish();
					break;
				}
			}
		}
	};
	
	private void sendMessage(String message)
	{
		if (service.sendMessage(message))
		{
			messageAppearInChat(true, message);
		}
	}

	private void enableChat()
	{
		Button btnSend = (Button) findViewById(R.id_chat.btnSend);
		btnSend.setEnabled(true);
		
		TextView txtMessages = (TextView) findViewById(R.id_chat.notificationText);
		txtMessages.setText("");
		txtMessages.setVisibility(View.GONE);
	}

	private void messageAppearInChat(boolean me, String message)
	{
		messagesAdapter.add((me ? getText(R.string.chat_me) : getText(R.string.chat_you)) + " " + message);
	}
}

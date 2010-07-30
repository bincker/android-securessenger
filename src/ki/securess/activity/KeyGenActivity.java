package ki.securess.activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;

import ki.securess.Constants;
import ki.securess.CryptUtil;
import ki.securess.R;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class KeyGenActivity extends Activity
{
	public static final int RESULT_OK = 1;

	public static final int RESULT_ERROR = RESULT_OK + 1;

	public static final int RESULT_NOCHANGE = RESULT_ERROR + 1;

	private static final int KEY_SIZE = 4096;

	File fpriv = new File(Environment.getExternalStorageDirectory() + "/" + Constants.PRIVKEY_LOCATION);

	File fpub = new File(Environment.getExternalStorageDirectory() + "/" + Constants.PUBKEY_LOCATION);

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.keygen);

		setResult(RESULT_NOCHANGE);

		Button btnGenerate = (Button) findViewById(R.id_keygen.generate);
		btnGenerate.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				tryGeneration();
			}
		});

		if (fpriv.exists() && fpub.exists())
		{
			btnGenerate.setText(R.string.keygenactivity_regenerate);
			TextView txtInvalid = (TextView) findViewById(R.id_keygen.text_keys_invalid);
			txtInvalid.setVisibility(View.INVISIBLE);
		}
	}

	private static final int MSG_NEWKEY_OK = 1;
	private static final int MSG_NEWKEY_ERROR = MSG_NEWKEY_OK + 1;
	Handler message_handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case MSG_NEWKEY_OK:
				{
					setResult(RESULT_OK);
					Toast.makeText(KeyGenActivity.this, R.string.key_generated, Constants.TOAST_LENGTH).show();
					finish();
					break;
				}
				case MSG_NEWKEY_ERROR:
				{
					setResult(RESULT_ERROR);
					setResult(RESULT_ERROR);
					Toast.makeText(KeyGenActivity.this, R.string.error_on_generate_key, Constants.TOAST_LENGTH).show();
					finish();
					break;
				}
			}
		}
	};

	/**
	 * Tries to generate the key and to report to user/Activity
	 */
	private void tryGeneration()
	{
		TextView t = (TextView) findViewById(R.id_keygen.text_generating);
		t.setVisibility(View.VISIBLE);
		new Thread(newKeyProcess).start();
	}

	/**
	 * Overwrites existing keys, generating a new pair
	 * @throws NoSuchAlgorithmException 
	 * @throws IOException 
	 */
	Runnable newKeyProcess = new Runnable()
	{
		@Override
		public void run()
		{
			try
			{
				// Ensure to delete old ones if existing
				if (fpriv.exists())
				{
					fpriv.delete();
				}
				if (fpub.exists())
				{
					fpub.delete();
				}

				// Generation - takes up nearly 30secs on Nexus One for a 4k key
				KeyPair key = CryptUtil.genKey(KEY_SIZE);

				// Write down keys on disk
				ObjectOutputStream fospriv = new ObjectOutputStream(new FileOutputStream(fpriv));
				try
				{
					fospriv.writeObject(key.getPrivate());
				}
				finally
				{
					fospriv.close();
				}

				// Write down keys on disk
				ObjectOutputStream fospub = new ObjectOutputStream(new FileOutputStream(fpub));
				try
				{
					fospub.writeObject(key.getPublic());
				}
				finally
				{
					fospub.close();
				}
				
				message_handler.sendMessage(Message.obtain(message_handler, MSG_NEWKEY_OK));
			}
			catch (Exception e)
			{
				Log.e(this.getClass().getName(), e.getMessage(), e);
				message_handler.sendMessage(Message.obtain(message_handler, MSG_NEWKEY_ERROR));
			}
		}
	};
}

package ki.securess.activity;

import java.io.File;

import ki.securess.Constants;
import ki.securess.R;
import ki.securess.dialogs.CodeConnectDialog;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity
{
	public static final int KEYGENACTIVITY_GENERATE = 1;

	public static final int KEYGENACTIVITY_VIEW = KEYGENACTIVITY_GENERATE + 1;

	File fpriv = new File(Environment.getExternalStorageDirectory() + "/" + Constants.PRIVKEY_LOCATION);

	File fpub = new File(Environment.getExternalStorageDirectory() + "/" + Constants.PUBKEY_LOCATION);
	
	File appdir = new File(Environment.getExternalStorageDirectory() + "/" + Constants.APP_DIR);

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		appdir.mkdirs();

		final Button btnHost = (Button) findViewById(R.id_main.btn_host);
		final Button btnConnect = (Button) findViewById(R.id_main.btn_connect);
		btnHost.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent i = new Intent(MainActivity.this, ChatActivity.class);
				i.putExtra("hosting", true);
				startActivity(i);
			}
		});
		btnConnect.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showDialog(Constants.DLG_CONNECT_TO);
			}
		});

		loadKey();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	CodeConnectDialog.OKListener codeConnectOKListener = new CodeConnectDialog.OKListener()
	{
		@Override
		public void ok(String code)
		{
			try
			{
				Intent i = new Intent(MainActivity.this, ChatActivity.class);
				i.putExtra("code", code);
				i.putExtra("hosting", false);
				startActivity(i);
			}
			catch (Exception e)
			{
				Log.e(this.getClass().getName(), e.getMessage(), e);
			}
		}
	};

	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id)
		{
			case Constants.DLG_CONNECT_TO:
			{
				return new CodeConnectDialog(this, codeConnectOKListener);
			}
		}

		return null;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id_menu.key_generator:
			{
				startActivityForResult(new Intent(this, KeyGenActivity.class), KEYGENACTIVITY_VIEW);
				return true;
			}
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case KEYGENACTIVITY_GENERATE:
			{
				switch (resultCode)
				{
					case KeyGenActivity.RESULT_OK:
					{
						loadKey();
						break;
					}
					case KeyGenActivity.RESULT_ERROR:
					{
						loadKey();
						break;
					}
					case KeyGenActivity.RESULT_NOCHANGE:
					{
						// NO CHANGE?
						break;
					}
				}
				break;
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	public void loadKey()
	{
		if (!fpriv.exists() || !fpub.exists())
		{
			TextView txtnokey = (TextView) findViewById(R.id_main.no_key_available);
			txtnokey.setVisibility(View.VISIBLE);
			return;
		}

		TextView txtnokey = (TextView) findViewById(R.id_main.no_key_available);
		txtnokey.setVisibility(View.INVISIBLE);
	}
}
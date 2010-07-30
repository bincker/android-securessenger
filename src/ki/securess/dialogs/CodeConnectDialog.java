package ki.securess.dialogs;

import ki.securess.R;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class CodeConnectDialog extends Dialog
{
	public interface OKListener {void ok(String code);}
	
	private OKListener oklistener;
	
	public CodeConnectDialog(Context context, OKListener oklistener)
	{
		super(context);
		this.oklistener = oklistener;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.dialog_codeconnect);
		setTitle(R.string.enter_code);
		
		Button btn = (Button) findViewById(R.id.dialog_rf_button);
		btn.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				EditText txt = (EditText) findViewById(R.id.dialog_rf_edit);
				oklistener.ok(txt.getText().toString());
				CodeConnectDialog.this.dismiss();
			}
		});
	}
}

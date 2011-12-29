package ca.drglove.uvic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class UVicGrades extends Activity implements OnClickListener {
	
	private static final int POST_DATA=0;
	
	private Button mLogin;
	private EditText mUsername;
	private EditText mPassword;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        mLogin = (Button) findViewById(R.id.login);
        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        
        mLogin.setOnClickListener(this);
    }

	@Override
	public void onClick(View v) {
		Bundle extras = new Bundle();
		
		extras.putString(GradeHelper.KEY_USERNAME, mUsername.getText().toString());
		extras.putString(GradeHelper.KEY_PASSWORD, mPassword.getText().toString());
		
		Intent i = new Intent(this, GradeHelper.class);
		i.putExtras(extras);
		startActivityForResult(i, POST_DATA);
	}
}
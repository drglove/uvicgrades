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
	public static final String TAG = "UVICGRADES";
	
	private Button mLogin;
	private Button mFeedback;
	private EditText mUsername;
	private EditText mPassword;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        mLogin = (Button) findViewById(R.id.login);
        mFeedback = (Button) findViewById(R.id.feedback);
        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        
        mLogin.setOnClickListener(this);
        mFeedback.setOnClickListener(this);
    }

	@Override
	public void onClick(View v) {
		Bundle extras = new Bundle();
		
		if (v == mLogin) {
			// Login
			extras.putString(GradeHelper.KEY_USERNAME, mUsername.getText().toString());
			extras.putString(GradeHelper.KEY_PASSWORD, mPassword.getText().toString());
			
			final Intent i = new Intent(this, GradesActivity.class);
			i.putExtras(extras);
			startActivityForResult(i, POST_DATA);
		}
		else if (v == mFeedback) {
			// Send feedback to me
			extras.putStringArray(android.content.Intent.EXTRA_EMAIL, new String[]{ getString(R.string.mail_feedback_email) });
			extras.putString(android.content.Intent.EXTRA_SUBJECT, getString(R.string.mail_feedback_subject));
			extras.putString(android.content.Intent.EXTRA_TEXT, getString(R.string.mail_feedback_message));
			
			final Intent i = new Intent(android.content.Intent.ACTION_SEND);
			i.setType("text/html");
			i.putExtras(extras);
			startActivity(Intent.createChooser(i, getString(R.string.feedback)));
		}
	}
}
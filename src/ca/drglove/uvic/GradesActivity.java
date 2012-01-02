package ca.drglove.uvic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class GradesActivity extends Activity {
	
	private GradeHelper g;
	private String term;
	
	private Spinner termSpinner;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		
		// Get credentials from intent
		String username = extras.getString(GradeHelper.KEY_USERNAME);
		String password = extras.getString(GradeHelper.KEY_PASSWORD);
		term = null;
		
		g = new GradeHelper(username, password);
		try {
			List<NameValuePair> terms = g.executeTermRequest();
			
			if (terms == null) {
				Toast.makeText(this, R.string.login_invalid, Toast.LENGTH_LONG).show();
				g.close();
				finish();
				return;
			}
			
			setContentView(R.layout.grades);
			setSpinner(terms);
			
		} catch (IOException e) {
			Log.e(UVicGrades.TAG, "IOException: "+e);
			e.printStackTrace();
			g.close();
			finish();
			return;
		}
		
	}
	
	private void updateGrades(String term) throws IOException {
		List<NameValuePair> grades = null;
		if (term != null) {
			grades = g.getTermGrades(term);
			
			TableLayout tl = (TableLayout) findViewById(R.id.gradeTable);
			tl.removeAllViewsInLayout();
			
			for (NameValuePair grade : grades) {
				TableRow tr = new TableRow(this);
				TextView course = new TextView(this);
				TextView value = new TextView(this);
				
				course.setText(grade.getName());
				course.setTextSize(25);
				
				value.setText(grade.getValue());
				value.setTextSize(25);
				value.setGravity(Gravity.RIGHT);
				
				tr.addView(course);
				tr.addView(value);
				
				tl.addView(tr, new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			}
		}
	}
	
	
	private void setSpinner(final List<NameValuePair> l) {
		ArrayList<String> termNames = new ArrayList<String>(l.size());
		
		for (NameValuePair n : l)
			termNames.add(n.getValue());
		
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, termNames);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		termSpinner = (Spinner) findViewById(R.id.termSpinner);
		termSpinner.setAdapter(spinnerAdapter);
		
		termSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
				term = l.get(position).getName();
				try {
					updateGrades(term);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		
	}
}

package ca.drglove.uvic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class GradeHelper {
	
	private static final String login_site = "https://www.uvic.ca/cas/login?service=https://www.uvic.ca";
	private static final String grade_referer = "https://www.uvic.ca/BAN2P/bwskogrd.P_ViewTermGrde";
	private static final String grade_site = "https://www.uvic.ca/BAN2P/bwskogrd.P_ViewGrde";
	
	public static final String KEY_USERNAME = "username";
	public static final String KEY_PASSWORD = "password";
	private static final String KEY_TERM = "term_in";
	private static final String KEY_LT = "lt";
	private static final String KEY_EVENTID = "_eventId";
	private static final String KEY_SUBMIT = "submit";
	private static final String KEY_LOGINCOOKIE = "uvic_sso";

	private String username;
	private String password;
	private static final String eventid = "submit";
	private static final String lt = "e1s1";
	private static final String submit = "Sign in";

	private DefaultHttpClient client;
    private CookieStore cookieStore;
	
	public GradeHelper() {
		this.username = null;
		this.password = null;
		
		client = new DefaultHttpClient();
		cookieStore = new BasicCookieStore();
		
		// Set the storage for the cookies
		client.setCookieStore(cookieStore);
		
		// Allow circular redirects for grades page
		client.getParams().setParameter("http.protocol.allow-circular-redirects", true);
	}

	public GradeHelper(String username, String password) {
		this.username = username;
		this.password = password;
		
		client = new DefaultHttpClient();
		cookieStore = new BasicCookieStore();
		
		// Set the storage for the cookies
		client.setCookieStore(cookieStore);
		
		// Allow circular redirects for grades page
		client.getParams().setParameter("http.protocol.allow-circular-redirects", true);
	}

	// Execute request with current parameters
	public List<NameValuePair> executeTermRequest() throws IOException {
		// Login using the open HttpClient and the current context
		login();
	
		// Check if credentials succeeded
		if (!isValidLogin()) {
			return null;
		}
		
		// Get grades page
		List<NameValuePair> terms = getTerms();
	
		return terms;
	}
	
	// Getters and setters
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	// Get login response from server using provided credentials
	private HttpResponse login() throws IOException {
		// GET page to set cookies
		HttpGet get = new HttpGet(login_site);
		setLoginHeaders(get);
		logCookiesAndHeaders("GET (sent) LOGIN", cookieStore, get.getAllHeaders());
		HttpResponse response = client.execute(get);
		if (response != null)
			response.getEntity().consumeContent();
		logCookiesAndHeaders("GET (received) LOGIN", cookieStore, response.getAllHeaders());
		
		// POST request to login
		HttpPost post = new HttpPost(login_site);
		setLoginInput(post);
		setLoginHeaders(post);
		logCookiesAndHeaders("POST (sent) LOGIN", cookieStore, post.getAllHeaders());
		response = client.execute(post);
		logCookiesAndHeaders("POST (received) LOGIN", cookieStore, response.getAllHeaders());
		
		return response;
	}
	
	// Determine if a login was successful
	private boolean isValidLogin() {
		for (Cookie c : cookieStore.getCookies()) {
			// Define a particular cookie which is set when a user logs in
			if (c.getName().equalsIgnoreCase(KEY_LOGINCOOKIE)) {
				Log.i(UVicGrades.TAG,"Login cookie found");
				return true;
			}
		}
		Log.i(UVicGrades.TAG,"Login cookie not found");
		return false;
	}
	
	// Return response from the grade page
	private List<NameValuePair> getTerms() throws IOException {
		// Get all grade pages first
		HttpGet get = new HttpGet(grade_referer);
		HttpResponse response = client.execute(get);
		
		// Parse HTML for terms
		List<NameValuePair> p = parseTerms(EntityUtils.toString(response.getEntity()));
		if (response != null)
			response.getEntity().consumeContent();
		return p;
	}

	// Parse the HTML to search for available terms
	private List<NameValuePair> parseTerms(String htmlData) {
		// Search through response to find appropriate terms
		String term_expr = "<OPTION\\s+VALUE=\"(.*?)\".*:\\s+(.*)"; //Props to Vedran
		
		List<NameValuePair> terms = new ArrayList<NameValuePair>(6);
        Pattern pattern = Pattern.compile(term_expr, Pattern.UNIX_LINES);
        Matcher matcher = pattern.matcher(htmlData);
        while (matcher.find()) {
        	terms.add(new BasicNameValuePair(matcher.group(1), matcher.group(2)));
        }
        return terms;
	}
	
	// POST to the grade page and get the grades
	public List<NameValuePair> getTermGrades(String term) throws IOException {
		// POST term request
		HttpPost post = new HttpPost(grade_site);
        setGradeInputs(post, term);
		setGradeHeaders(post);
		HttpResponse response = client.execute(post);
		
		// Parse HTML for grades
		return parseGrades(EntityUtils.toString(response.getEntity()));
	}
	
	// Parse the HTML for the course and grades
	private List<NameValuePair> parseGrades(String htmlData) {
		// Search through for appropriate rows
		List<String> entries = new ArrayList<String>(5);
		String row_expr = "<TR>(.*?)</TR>";
		Pattern pattern = Pattern.compile(row_expr, Pattern.DOTALL | Pattern.UNIX_LINES);
        Matcher matcher = pattern.matcher(htmlData);
        while (matcher.find()) {
        	if (matcher.group().contains("CLASS=\"default\"")) // This is all we have to go off of right now when determining if a row is relevant
        		entries.add(matcher.group(1));
        }
		
        // Search each row and pick out course name and number, and grade
        // NTL April 9, 2014: Regular expression changed. Now we have an easier table to parse
        // Here's an example of what we're parsing
        // (I know regexp is bad for HTML but it's not a hassle here)
        
        /*
 
         <TD CLASS="default">PHYS 502A</TD>
		 <TD CLASS="default">Classical Electrodynamics</TD>
		 <TD CLASS="default">A+</TD>
		 <TD CLASS="default">9</TD>
		 <TD CLASS="default">  1.50</TD>
		 <TD CLASS="default">&nbsp;</TD>

         */
        
		List<NameValuePair> grades = new ArrayList<NameValuePair>(5);
		String noData = "<TD[^>]*>.*?</TD>\\s+";
		String data = "<TD[^>]*>(.*?)</TD>\\s+";
		String grade_expr = data + noData + data + noData + noData + noData;
        pattern = Pattern.compile(grade_expr, Pattern.DOTALL | Pattern.UNIX_LINES);
        for (String entry : entries) {
	        matcher = pattern.matcher(entry);
	        while (matcher.find()) {
	        	grades.add(new BasicNameValuePair(matcher.group(1), matcher.group(2)));
	        	Log.i(UVicGrades.TAG, grades.get(grades.size()-1).toString());
	        }
        }
        
        return grades;
	}
	
	private void setLoginInput(HttpPost post) throws UnsupportedEncodingException {
		// Add pairs of form inputs
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(5);
		addInput(inputs, KEY_USERNAME, username);
		addInput(inputs, KEY_PASSWORD, password);
		addInput(inputs, KEY_LT, lt);
		addInput(inputs, KEY_EVENTID, eventid);
		addInput(inputs, KEY_SUBMIT, submit);
		
		// Encode URL
		UrlEncodedFormEntity encodedInput = new UrlEncodedFormEntity(inputs, HTTP.UTF_8);
		
		// Set POST entry
		post.setEntity(encodedInput);
	}
	
	private void setLoginHeaders(HttpPost post) {
		// Set headers
		post.setHeader("User-Agent","Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.17) Gecko/20110428 Fedora/3.6.17-1.fc13 Firefox/3.6.17");
		post.setHeader("Content-Type", "application/x-www-form-urlencoded");
		post.setHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,/;q=0.8");
		post.setHeader("Accept-Language","en-us,en;q=0.5");
		post.setHeader("Accept-Encoding","gzip,deflate");
		post.setHeader("Accept-Charset","ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		post.setHeader("Keep-Alive","115");
		post.setHeader("Connection","keep-alive");
		post.setHeader("Referer",login_site);
	}
	
	private void setLoginHeaders(HttpGet get) {
		// Set headers
		get.setHeader("User-Agent","Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.17) Gecko/20110428 Fedora/3.6.17-1.fc13 Firefox/3.6.17");
		get.setHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,/;q=0.8");
		get.setHeader("Accept-Language","en-us,en;q=0.5");
		get.setHeader("Accept-Encoding","gzip,deflate");
		get.setHeader("Accept-Charset","ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		get.setHeader("Keep-Alive","115");
		get.setHeader("Connection","keep-alive");
	}

	private void setGradeInputs(HttpPost post, String term) throws UnsupportedEncodingException {
		// Add pairs of form inputs
		List<NameValuePair> inputs = new ArrayList<NameValuePair>(1);
		addInput(inputs, KEY_TERM, term);
		
		// Encode URL
		UrlEncodedFormEntity encodedInput = new UrlEncodedFormEntity(inputs, HTTP.UTF_8);
		
		// Set POST entry
		post.setEntity(encodedInput);
	}
	
	private void setGradeHeaders(HttpPost post) {
		// Set headers
		post.setHeader("User-Agent","Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.17) Gecko/20110428 Fedora/3.6.17-1.fc13 Firefox/3.6.17");
		post.setHeader("Content-Type","application/x-www-form-urlencoded");
		post.setHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,/;q=0.8");
		post.setHeader("Accept-Language","en-us,en;q=0.5");
		post.setHeader("Accept-Encoding","gzip,deflate");
		post.setHeader("Accept-Charset","ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		post.setHeader("Keep-Alive","115");
		post.setHeader("Connection","keep-alive");
		post.setHeader("Referer",grade_referer);
		post.setHeader("Cache-Control", "max-age=0");
	}
	
	private void logCookiesAndHeaders(String note, CookieStore cookieStore, Header[] headers) {
		Log.d(UVicGrades.TAG,note);
		Log.d(UVicGrades.TAG,"Cookies: "+ (cookieStore.getCookies().isEmpty() ? "None" : ""));
		for (Cookie c : cookieStore.getCookies()) {
			Log.d(UVicGrades.TAG,"- "+c.toString());
		}
		Log.d(UVicGrades.TAG,"Headers:"+ (headers.length == 0 ? "None" : ""));
		for (Header h : headers) {
			Log.d(UVicGrades.TAG,"- "+h.getName()+": "+h.getValue());
		}
	}
	
	private void addInput(List<NameValuePair> inputs, String name, String value) {
		inputs.add(new BasicNameValuePair(name, value));
	}
	
	public void close() {
		if (client != null)
			client.getConnectionManager().shutdown();
	}
}
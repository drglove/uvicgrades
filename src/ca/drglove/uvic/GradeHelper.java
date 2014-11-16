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
	
	private static final String login_site = "https://www.uvic.ca/cas/login?service=https%3a//www.uvic.ca/BAN2P/banuvic.gzcaslib.P_Service_Ticket%3ftarget=bwskogrd.P_ViewTermGrde";
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
		
		// TODO: Add status code checking
		
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
		//String test = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/transitional.dtd\">\r\n<HTML lang=\"en\">\r\n<HEAD>\r\n<meta content=\"IE=7;FF=3;OtherUA=4\" http-equiv=\"X-UA-Compatible\">\r\n<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\r\n<META HTTP-EQUIV=\"Pragma\" NAME=\"Cache-Control\" CONTENT=\"no-cache\">\r\n<META HTTP-EQUIV=\"Cache-Control\" NAME=\"Cache-Control\" CONTENT=\"no-cache\">\r\n<LINK REL=\"stylesheet\" HREF=\"/css/web_defaultapp.css\" TYPE=\"text/css\">\r\n<LINK REL=\"stylesheet\" HREF=\"/css/web_defaultprint.css\" TYPE=\"text/css\" media=\"print\">\r\n<TITLE>Term Grades</TITLE>\r\n<META HTTP-EQUIV=\"Content-Script-Type\" NAME=\"Default_Script_Language\" CONTENT=\"text/javascript\">\r\n<SCRIPT LANGUAGE=\"JavaScript\" TYPE=\"text/javascript\">\r\n<!-- Hide JavaScript from older browsers \r\nvar submitcount=0;\r\nfunction checkSubmit() {\r\nif (submitcount == 0)\r\n   {\r\n   submitcount++;\r\n   return true;\r\n   }\r\nelse\r\n   {\r\nalert(\"Your changes have already been submitted.\");\r\n   return false;\r\n   }\r\n}\r\n//  End script hiding -->\r\n</SCRIPT>\r\n<SCRIPT LANGUAGE=\"JavaScript\" TYPE=\"text/javascript\">\r\n<!-- Hide JavaScript from older browsers \r\n//  Function to open a window\r\nfunction windowOpen(window_url) {\r\n   helpWin = window.open(window_url,'','toolbar=yes,status=no,scrollbars=yes,menubar=yes,resizable=yes,directories=no,location=no,width=350,height=400');\r\n   if (document.images) { \r\n       if (helpWin) helpWin.focus()\r\n   }\r\n}\r\n//  End script hiding -->\r\n</SCRIPT>\r\n<script src=\"/js/uvic.pack.js\" language=\"javascript1.4\" type=\"text/javascript\" ></script>\r\n</HEAD>\r\n<BODY>\r\n<DIV class=\"headerwrapperdiv\">\r\n<DIV class=\"pageheaderdiv1\">\r\n<A HREF=\"#main_content\" onMouseover=\"window.status='Go to Main Content'; return true\" onMouseout=\"window.status=''; return true\" OnFocus=\"window.status='Go to Main Content'; return true\" onBlur=\"window.status=''; return true\" class=\"skiplinks\">Go to Main Content</A>\r\n<H1>UVic</H1></DIV><DIV class=\"headerlinksdiv\">\r\n<SPAN class=\"pageheaderlinks2\">\r\n<MAP NAME=\"Module_Navigation_Links_H\" title=\"Module Navigation Links\">\r\n<p>\r\n<A HREF=\"#skip_Module_Navigation_Links_H\" onMouseover=\"window.status='Skip Module Navigation Links'; return true\" onMouseout=\"window.status=''; return true\" onFocus=\"window.status='Skip Module Navigation Links'; return true\" onBlur=\"window.status=''; return true\"  class=\"skiplinks\">Skip Module Navigation Links</A>\r\n<TABLE  CLASS=\"plaintable\" SUMMARY=\"This is main table for displaying Tab Items.\"\r\n                          WIDTH=\"100%\" cellSpacing=0 cellPadding=0 border=0>\r\n<TR>\r\n<TD CLASS=\"pldefault\">\r\n<TABLE  CLASS=\"plaintable\" SUMMARY=\"This table displays Tab Items.\"\r\n                 cellSpacing=0 cellPadding=0 border=0>\r\n<TR>\r\n<td class=\"taboff\" height=22>\r\n<A HREF=\"/BAN2P/twbkwbis.P_GenMenu?name=bmenu.P_GenMnu\" onMouseover=\"window.status='Personal'; return true\" onMouseout=\"window.status=''; return true\" onFocus=\"window.status='Personal'; return true\" onBlur=\"window.status=''; return true\" >Personal</A>\r\n</TD>\r\n<TD class=\"bgtaboff\" height=22 vAlign=\"top\" align=\"right\">\r\n<IMG SRC=\"/wtlgifs/web_tab_corner_right.gif\" ALT=\"Tab Corner Right\" CLASS=\"headerImg\" TITLE=\"Tab Corner Right\"  NAME=\"web_tab_corner_right\" HSPACE=0 VSPACE=0 BORDER=0 HEIGHT=20 WIDTH=8>\r\n</TD>\r\n<td class=\"tabon\"  height=22>\r\n<A HREF=\"/BAN2P/twbkwbis.P_GenMenu?name=bmenu.P_StuMainMnu\" onMouseover=\"window.status='Student Info'; return true\" onMouseout=\"window.status=''; return true\" onFocus=\"window.status='Student Info'; return true\" onBlur=\"window.status=''; return true\" >Student Info</A>\r\n</TD>\r\n<TD class=\"bgtabon\"  height=22 vAlign=\"top\" align=\"right\">\r\n<IMG SRC=\"/wtlgifs/web_tab_corner_right.gif\" ALT=\"Tab Corner Right\" CLASS=\"headerImg\" TITLE=\"Tab Corner Right\"  NAME=\"web_tab_corner_right\" HSPACE=0 VSPACE=0 BORDER=0 HEIGHT=20 WIDTH=8>\r\n</TD>\r\n<td class=\"taboff\" height=22>\r\n<A HREF=\"/BAN2P/twbkwbis.P_GenMenu?name=bmenu.P_BsacMnu\" onMouseover=\"window.status='Student Awards and Financial Aid'; return true\" onMouseout=\"window.status=''; return true\" onFocus=\"window.status='Student Awards and Financial Aid'; return true\" onBlur=\"window.status=''; return true\" >Student Awards and Financial Aid</A>\r\n</TD>\r\n<TD class=\"bgtaboff\" height=22 vAlign=\"top\" align=\"right\">\r\n<IMG SRC=\"/wtlgifs/web_tab_corner_right.gif\" ALT=\"Tab Corner Right\" CLASS=\"headerImg\" TITLE=\"Tab Corner Right\"  NAME=\"web_tab_corner_right\" HSPACE=0 VSPACE=0 BORDER=0 HEIGHT=20 WIDTH=8>\r\n</TD>\r\n</TR>\r\n</TABLE>\r\n</TD>\r\n</TR>\r\n<TR>\r\n<TD class=\"bgtabon\" width=\"100%\" colSpan=2><IMG SRC=\"/wtlgifs/web_transparent.gif\" ALT=\"Transparent Image\" CLASS=\"headerImg\" TITLE=\"Transparent Image\"  NAME=\"web_transparent\" HSPACE=0 VSPACE=0 BORDER=0 HEIGHT=3 WIDTH=10></TD></TR></TABLE>\r\n</MAP>\r\n</SPAN>\r\n<a name=\"skip_Module_Navigation_Links_H\"></a>\r\n</DIV>\r\n<TABLE  CLASS=\"plaintable\" SUMMARY=\"This table displays Menu Items and Banner Search textbox.\"\r\n         WIDTH=\"100%\">\r\n<TR>\r\n<TD CLASS=\"pldefault\">\r\n<DIV class=\"headerlinksdiv2\">\r\n<FORM ACTION=\"/BAN2P/twbksrch.P_ShowResults\" METHOD=\"POST\">\r\nSearch\r\n<SPAN class=\"fieldlabeltextinvisible\"><LABEL for=keyword_in_id><SPAN class=\"fieldlabeltext\">Search</SPAN></LABEL></SPAN>\r\n<INPUT TYPE=\"text\" NAME=\"KEYWRD_IN\" SIZE=\"20\" MAXLENGTH=\"65\" ID=\"keyword_in_id\">\r\n<INPUT TYPE=\"submit\" VALUE=\"Go\">\r\n</FORM>\r\n</div>\r\n</TD>\r\n<TD CLASS=\"pldefault\"><p class=\"rightaligntext\">\r\n<SPAN class=\"pageheaderlinks\">\r\n<A HREF=\"/BAN2P/twbksite.P_DispSiteMap?menu_name_in=bmenu.P_MainMnu&amp;depth_in=2&amp;columns_in=3\" accesskey=\"2\" class=\"submenulinktext2\">SITE MAP</A>\r\n|\r\n<A HREF=\"/BAN2P/twbkfrmt.P_DispHelp?pagename_in=bwskogrd.P_ViewGrde\" accesskey=\"H\" onClick=\"popup = window.open('/BAN2P/twbkfrmt.P_DispHelp?pagename_in=bwskogrd.P_ViewGrde', 'PopupPage','height=500,width=450,scrollbars=yes,resizable=yes'); return false\" target=\"_blank\" onMouseOver=\"window.status='';  return true\" onMouseOut=\"window.status=''; return true\"onFocus=\"window.status='';  return true\" onBlur=\"window.status=''; return true\"  class=\"submenulinktext2\">HELP</A>\r\n|\r\n<A HREF=\"twbkwbis.P_Logout\" accesskey=\"3\" class=\"submenulinktext2\">EXIT</A>\r\n</span>\r\n</TD>\r\n</TR>\r\n</TABLE>\r\n</DIV>\r\n<DIV class=\"pagetitlediv\">\r\n<TABLE  CLASS=\"plaintable\" SUMMARY=\"This table displays title and static header displays.\"\r\n   WIDTH=\"100%\">\r\n<TR>\r\n<TD CLASS=\"pldefault\">\r\n<H2>Term Grades</H2>\r\n</TD>\r\n<TD CLASS=\"pldefault\">\r\n&nbsp;\r\n</TD>\r\n<TD CLASS=\"pldefault\"><p class=\"rightaligntext\">\r\n<DIV class=\"staticheaders\">\r\nV00XXXXXX Joe B. Nobody<br>\r\nSummer Session: May - Aug 2014<br>\r\nAug 09, 2014 11:57 am<br>\r\n</div>\r\n</TD>\r\n</TR>\r\n<TR>\r\n<TD class=\"bg3\" width=\"100%\" colSpan=3><IMG SRC=\"/wtlgifs/web_transparent.gif\" ALT=\"Transparent Image\" CLASS=\"headerImg\" TITLE=\"Transparent Image\"  NAME=\"web_transparent\" HSPACE=0 VSPACE=0 BORDER=0 HEIGHT=3 WIDTH=10></TD>\r\n</TR>\r\n</TABLE>\r\n<a name=\"main_content\"></a>\r\n</DIV>\r\n<DIV class=\"pagebodydiv\">\r\n<!--  ** END OF twbkwbis.P_OpenDoc **  -->\r\n<script language=\"javascript1.4\"type=\"text/javascript\">\r\n$().ready(function(){\r\n//identify and format the table for the table sorter\r\n$(\".pagebodydiv:first\").attr(\"id\",\"P_ViewGrde\");\r\n$(\"#P_ViewGrde .tablesorter\").tablesorter();\r\n});\r\n</script>\r\n<DIV class=\"infotextdiv\"><TABLE  CLASS=\"infotexttable\" SUMMARY=\"This layout table contains information that may be helpful in understanding the content and functionality of this page.  It could be a brief set of instructions, a description of error messages, or other special information.\"><TR><TD CLASS=\"indefault\"><SPAN class=\"infotext\"> <br></br>\r\n   <br></br>\r\n   <p><img src=\"/wtlgifs/web_redflag.gif\" align=\"left\" alt=\"\"/>\r\n   <strong>IMPORTANT:</strong> Check your ADMINISTRATIVE TRANSCRIPT for academic standing that is assigned at the end of each session when grades are all in (Apr/May for Winter or Jul/Aug for Summer).</p>\r\n   <br></br>\r\n   <p>Review your ADMINISTRATIVE TRANSCRIPT regularly as it is your responsibility to access information that is updated (such as grade changes or degree programs).</p></SPAN></TD></TR></TABLE><P></DIV>\r\n<DIV style=\"font-size:15px\">\r\n&nbsp;&nbsp;\r\n<A HREF=\"banuvic.pkg_transcript_web.StuViewTrans\">View your Administrative Transcript</a>\r\n</DIV>\r\n<BR>\r\n<table id=\"grades-table\" class=\"datadisplaytable tablesorter\" summary=\"This table displays the final grade for a class as well as the subject, course, course title, grade point, awarded units and grade note.\">\r\n<caption class=\"captiontext\">Undergraduate Course work</caption>\r\n<thead>\r\n<TR>\r\n<th class=\"ddheader\" scope=\"col\">Course</th>\r\n<th class=\"ddheader\" scope=\"col\">Title</th>\r\n<th class=\"ddheader\" scope=\"col\">Grade</th>\r\n<th class=\"ddheader\" scope=\"col\">Grade<br>point</th>\r\n<th class=\"ddheader\" scope=\"col\">Awarded<br>units</th>\r\n<th class=\"ddheader\" scope=\"col\">Note</th>\r\n</TR>\r\n</thead><tbody>\r\n<TR>\r\n<TD CLASS=\"default\">TEST 101</TD>\r\n<TD CLASS=\"default\">Introduction to Testing</TD>\r\n<TD CLASS=\"default\">&nbsp;&nbsp;78%&nbsp;&nbsp;&nbsp;&nbsp;B+</TD>\r\n<TD CLASS=\"default\">6</TD>\r\n<TD CLASS=\"default\">  1.50</TD>\r\n<TD CLASS=\"default\">&nbsp;</TD>\r\n</TR>\r\n</tbody>\r\n</TABLE>\r\n\r\n<!--  ** START OF twbkwbis.P_CloseDoc **  -->\r\n<TABLE  CLASS=\"plaintable\" SUMMARY=\"This is table displays line separator at end of the page.\"\r\n                                             WIDTH=\"100%\" cellSpacing=0 cellPadding=0 border=0><TR><TD class=\"bgtabon\" width=\"100%\" colSpan=2><IMG SRC=\"/wtlgifs/web_transparent.gif\" ALT=\"Transparent Image\" CLASS=\"headerImg\" TITLE=\"Transparent Image\"  NAME=\"web_transparent\" HSPACE=0 VSPACE=0 BORDER=0 HEIGHT=3 WIDTH=10></TD></TR></TABLE>\r\n<A HREF=\"#top\" onMouseover=\"window.status='Skip to top of page'; return true\" onMouseout=\"window.status=''; return true\" OnFocus=\"window.status='Skip to top of page'; return true\" onBlur=\"window.status=''; return true\" class=\"skiplinks\">Skip to top of page</A>\r\n<A HREF=\"/BAN2P/bwskogrd.P_ViewTermGrde\" onMouseOver=\"window.status='Select another Term';  return true\" onMouseOut=\"window.status='';  return true\"onFocus=\"window.status='Select another Term';  return true\" onBlur=\"window.status='';  return true\">Select another Term</A>\r\n<p>&nbsp;\r\n</DIV>\r\n<DIV class=\"footerbeforediv\">\r\n\r\n</DIV>\r\n<DIV class=\"footerafterdiv\">\r\n\r\n</DIV>\r\n<DIV class=\"globalafterdiv\">\r\n\r\n</DIV>\r\n<DIV class=\"globalfooterdiv\">\r\n\r\n</DIV>\r\n<DIV class=\"pagefooterdiv\">\r\n<SPAN class=\"releasetext\">Release: 8.4</SPAN>\r\n</DIV>\r\n<DIV class=\"poweredbydiv\">\r\n</DIV>\r\n<DIV class=\"div1\"></DIV>\r\n<DIV class=\"div2\"></DIV>\r\n<DIV class=\"div3\"></DIV>\r\n<DIV class=\"div4\"></DIV>\r\n<DIV class=\"div5\"></DIV>\r\n<DIV class=\"div6\"></DIV>\r\n</BODY>\r\n</HTML>";
		//return parseGrades(test);
		
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
		 <TD CLASS="default">78% A+</TD>             <------ Percentage is OPTIONAL
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
	        	String course = matcher.group(1);
	        	String grade = matcher.group(2);
	        	
	        	// NTL August 9, 2014: With percentage grading, &nbsp shows up in the grade multiple times
	        	// We want to strip any leading/trailing &nbsp with nothing, and replace any other
	        	// consecutive appearances with a single space
	        	grade = fixGrade(grade);
	        	
	        	grades.add(new BasicNameValuePair(course, grade));
	        	Log.i(UVicGrades.TAG, grades.get(grades.size()-1).toString());
	        }
        }
        
        return grades;
	}
	
	// Fix up the grade to be presentable
	private String fixGrade(String grade) {
		// First, remove any non-breaking spaces
		String result = grade.replaceAll("&nbsp;", " ");
		
		// Second, trim the result
		result = result.trim();
		
		// Finally, replace consecutive whitespace with a single space
		return result.replaceAll("\\s+", " ");
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
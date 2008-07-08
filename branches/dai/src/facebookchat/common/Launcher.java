package facebookchat.common;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

import javax.swing.JOptionPane;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.json.JSONException;
import org.json.JSONObject;

import facebookchat.ui.chat.Chatroom;
import facebookchat.ui.main.Cheyenne;
import facebookchat.ui.main.LoginDialog;

public class Launcher {
	public static MultiThreadedHttpConnectionManager connectionManager;
	public static HttpClient httpClient;
	public static HttpState initialState;
	
	public static String loginPageUrl = "http://www.facebook.com/login.php";
	public static String homePageUrl = "http://www.facebook.com/home.php?";
	
	public static String uid = null;
	public static String channel = "15";
	public static String post_form_id = null;
	public static long seq = -1;

	public static HashSet<String> msgIDCollection;
	private static Map<String, Chatroom> chatroomCache;
	
	//public static Queue<Object> requestQ;
	
	
	public static Chatroom getChatroomAnyway(String uid){
		uid = uid.trim();
		System.out.println("%%%%%%>"+uid+"<%%%%%%");
		if(chatroomCache.containsKey(uid)){
			System.out.println("%%%%%%contains key:>"+uid+"<%%%%%%");
			return chatroomCache.get(uid);
		} else {
			System.out.println("%%%%%%new chatroom:>"+uid+"<%%%%%%");
			Chatroom chatroom = new Chatroom(uid);
			chatroomCache.put(uid, chatroom);
			System.out.println("registing chatroom...");
			return chatroom;
		}
	}
	public static boolean isChatroomExist(String uid){
		uid = uid.trim();
		if(chatroomCache.containsKey(uid)){
			return true;
		} 
		return false;
	}
	/*public static boolean registerChatroom(String uid, Chatroom room){
		if(uid != null && room != null
				&& !chatroomCache.containsKey(uid)){
			chatroomCache.put(uid, room);
			return true;
		}
		return false;
	}*/
	
	public static void main(String[] args){
		System.setProperty("sun.java2d.noddraw", "true");// 为半透明做准备
		System.setProperty("net.jxta.logging.Logging", "INFO");
		System.setProperty("net.jxta.level", "INFO");
		System.setProperty("java.util.logging.config.file",
				"logging.properties");
		
		Launcher laucher = new Launcher();
		laucher.go();
	}
	
	public Launcher(){
		connectionManager = new MultiThreadedHttpConnectionManager();
		httpClient = new HttpClient(connectionManager);
		msgIDCollection = new HashSet<String>();
		msgIDCollection.clear();
		
		chatroomCache = new Hashtable<String, Chatroom>();
		chatroomCache.clear();
		
		//requestQ = new LinkedList<Object>();
	}
	public void go(){
		LoginDialog login = new LoginDialog();
		String action = (String)login.showDialog();
		if(action.equals(LoginDialog.CANCELCMD))
			System.exit(0);

		String email = login.getUsername();
		String pass = new String(login.getPassword());
		System.out.println(email + ":" + pass);
		
		Long loginErrorCode = doLogin(email, pass);
		if(loginErrorCode.equals(ErrorCode.Error_Global_NoError)){
			if(doParseHomePage().equals(ErrorCode.Error_Global_NoError)){
				getBuddyList();
				//TODO get
				Thread msgRequester = new Thread(new Runnable(){
					public void run() {
						System.out.println("Keep requesting...");
						keepRequesting();
					}
				});
				msgRequester.start();
				//TODO post
				//Init GUI
				System.out.println("Init GUI...");
				Cheyenne fbc = new Cheyenne();
				fbc.setVisible(true);
			}
		} else if(loginErrorCode.equals(ErrorCode.kError_Async_NotLoggedIn)){
			//TODO handle the error derived from this login
			JOptionPane.showMessageDialog(
					null,"Not logged in, please check your input!",
					"Not Logged In",
					JOptionPane.ERROR_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(
					null,"Not logged in, please check your internet connection!",
					"Not Logged In",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private Long doLogin(String email, String pass) {
		System.out.println("Target URL: " + loginPageUrl);

        HttpClientParams clientParams = new HttpClientParams();

        // 隐藏自己请求相关的信息
        clientParams.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9) Gecko/2008052906 Firefox/3.0");

        httpClient.getHttpConnectionManager().getParams().setSoTimeout(30 * 1000);
        clientParams.setHttpElementCharset("utf-8");

        httpClient.setParams(clientParams);
        httpClient.getParams().setParameter(HttpClientParams.HTTP_CONTENT_CHARSET, "utf-8");
        //clientParams.setVersion(HttpVersion.HTTP_1_1);
        
		initialState = new HttpState();

		httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(
				30000);
		httpClient.setState(initialState);

		// RFC 2101 cookie management spec is used per default
		// to parse, validate, format & match cookies
		httpClient.getParams().setCookiePolicy(CookiePolicy.RFC_2109);

		GetMethod loginGet = new GetMethod(loginPageUrl);

		try{
			int statusCode = httpClient.executeMethod(loginGet);

			String httpResponseBody = loginGet.getResponseBodyAsString();
			System.out.println("=========httpResponseBody begin===========");
			//System.out.println(httpResponseBody);
			System.out.println("+++++++++httpResponseBody end+++++++++");

			System.out.println("Response status code: " + statusCode);

			Cookie[] initCookies = httpClient.getState().getCookies();

			System.out.println("Initial cookies: ");
			for (int i = 0; i < initCookies.length; i++) {
				System.out.println(" - " + initCookies[i].toExternalForm());
			}
		} catch (HttpException httpe) {
			System.err.print("HttpException");
			System.err.println(httpe.getMessage());
			httpe.printStackTrace();
			return ErrorCode.kError_Login_GenericError;
		} catch (IOException ioe) {
			System.err.print("IOException");
			System.err.println(ioe.getMessage());
			ioe.printStackTrace();
			return ErrorCode.kError_Global_ValidationError;
		}

		loginGet.releaseConnection();
		
		PostMethod loginPost = new PostMethod(loginPageUrl);

		NameValuePair[] postData = new NameValuePair[3];
		postData[0] = new NameValuePair("email", email);
		postData[1] = new NameValuePair("pass", pass);
		postData[2] = new NameValuePair("login", "");

		loginPost.addParameters(postData);
		
		try {
			httpClient.executeMethod(loginPost);

			Cookie[] cookies = httpClient.getState().getCookies();

			System.out.println("Present cookies: ");
			for (int i = 0; i < cookies.length; i++) {
				System.out.println(" - " + cookies[i].toExternalForm());
			}
			for (int i = 0; i < cookies.length; i++) {
				initialState.addCookie(cookies[i]);
			}
			httpClient.setState(initialState);
			
			String redirectLocation;
	        Header locationHeader = loginPost.getResponseHeader("location");
	        if (locationHeader != null) {
	        	//it should be "http://www.facebook.com/home.php?"
	            redirectLocation = locationHeader.getValue();
	            System.out.println("Redirect Location: " + redirectLocation);
	            
	            //homePageUrl = redirectLocation;
	            
	        } else {
	            // The response is invalid and did not provide the new location for
	            // the resource.  Report an error or possibly handle the response
	            // like a 404 Not Found error.
	        	
	        	//TODO login error!
	        	System.out.println("Login error!");
	        	return ErrorCode.kError_Async_NotLoggedIn;
	        }
	        
			String postMethodResponsStr = loginPost.getResponseBodyAsString();

			System.out
					.println("=========Login: postMethodResponsStr begin===========");
			System.out.println(postMethodResponsStr);
			System.out.println("+++++++++Login: postMethodResponsStr end+++++++++");

		} catch (HttpException httpe) {
			System.err.print("HttpException");
			System.err.println(httpe.getMessage());
			httpe.printStackTrace();
			return ErrorCode.kError_Login_GenericError;
		} catch (IOException ioe) {
			System.err.print("IOException");
			System.err.println(ioe.getMessage());
			ioe.printStackTrace();
			return ErrorCode.kError_Global_ValidationError;
		}

		loginPost.releaseConnection();
		return ErrorCode.Error_Global_NoError;
	}
	
	private Long doParseHomePage(){

		GetMethod homeGet = new GetMethod(homePageUrl);

		try {
			httpClient.executeMethod(homeGet);
			
			String getMethodResponseBody = homeGet.getResponseBodyAsString();
			System.out.println("=========HomePage: getMethodResponseBody begin=========");
			//System.out.println(getMethodResponseBody);
			System.out.println("+++++++++HomePage: getMethodResponseBody end+++++++++");

			Cookie[] finalCookies = httpClient.getState().getCookies();

			System.out.println("Final cookies: ");
			for (int i = 0; i < finalCookies.length; i++) {
				System.out.println(" - " + finalCookies[i].toExternalForm());
			}
			for (int i = 0; i < finalCookies.length; i++) {
				initialState.addCookie(finalCookies[i]);
			}
			httpClient.setState(initialState);
			
			if(getMethodResponseBody == null){
				System.out.println("Can't get the home page! Exit.");
				return ErrorCode.Error_Async_UnexpectedNullResponse;
			}
			
			//<a href="http://www.facebook.com/profile.php?id=xxxxxxxxx" class="profile_nav_link">
			String uidPrefix = "<a href=\"http://www.facebook.com/profile.php?id=";
			String uidPostfix = "\" class=\"profile_nav_link\">";
			//getMethodResponseBody.lastIndexOf(str, fromIndex)
			int uidPostFixPos = getMethodResponseBody.indexOf(uidPostfix);
			if(uidPostFixPos >= 0){
				int uidBeginPos = getMethodResponseBody.lastIndexOf(uidPrefix, uidPostFixPos) + uidPrefix.length();
				if(uidBeginPos < uidPrefix.length()){
					System.out.println("Can't get the user's id! Exit.");
					return ErrorCode.Error_System_UIDNotFound;
				}
				uid = getMethodResponseBody.substring(uidBeginPos, uidPostFixPos);
				System.out.println("UID: " + uid);
			}else{
				System.out.println("Can't get the user's id! Exit.");
				return ErrorCode.Error_System_UIDNotFound;
			}
			

			String channelPrefix = " \"channel";
			int channelBeginPos = getMethodResponseBody.indexOf(channelPrefix)
					+ channelPrefix.length();
			if (channelBeginPos < channelPrefix.length()){
				System.out.println("Error: Can't find channel!");
				return ErrorCode.Error_System_ChannelNotFound;
			}
			else {
				channel = getMethodResponseBody.substring(channelBeginPos,
						channelBeginPos + 2);
				System.out.println("Channel: " + channel);
			}

			// <input type="hidden" id="post_form_id" name="post_form_id"
			// value="3414c0f2db19233221ad8c2374398ed6" />
			String postFormIDPrefix = "<input type=\"hidden\" id=\"post_form_id\" name=\"post_form_id\" value=\"";
			int formIdBeginPos = getMethodResponseBody.indexOf(postFormIDPrefix)
					+ postFormIDPrefix.length();
			if (formIdBeginPos < postFormIDPrefix.length()){
				System.out.println("Error: Can't find post form ID!");
				return ErrorCode.Error_System_PostFornIDNotFound;
			}
			else {
				post_form_id = getMethodResponseBody.substring(formIdBeginPos,
						formIdBeginPos + 32);
				System.out.println("post_form_id: " + post_form_id);
			}
		} catch (HttpException httpe) {
			System.out.println(httpe.getMessage());
			return ErrorCode.Error_Async_HttpConnectionFailed;
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
			return ErrorCode.Error_Async_HttpConnectionFailed;
		}
		
		homeGet.releaseConnection();

		return ErrorCode.Error_Global_NoError;
	}
	
	public static void PostMessage(String uid, String msg) {
		if(uid.equals(Launcher.uid))
			return;
		
		System.out.println("====== PostMessage begin======");

		System.out.println("to:"+uid);
		System.out.println("msg:"+msg);
		
		String url = "http://www.facebook.com/ajax/chat/send.php";
		PostMethod postMethod = new PostMethod(url);

		// 填入各个表单域的值
		NameValuePair[] data = {
				new NameValuePair("msg_text", msg),
				new NameValuePair("msg_id", new Random().nextInt(999999999)
						+ ""),
				new NameValuePair("client_time", new Date().getTime() + ""),
				new NameValuePair("to", uid),
				new NameValuePair("post_form_id", post_form_id) };
		// 将表单的值放入postMethod中
		postMethod.setRequestBody(data);
		System.out.println("executeMethod ing...");
		try {
			// 执行postMethod
			int statusCode;

			statusCode = httpClient.executeMethod(postMethod);

			String responseStr = postMethod.getResponseBodyAsString();
			//TODO process the respons string
			//if statusCode == 200: no error;(responsStr contains "errorDescription":"No error.")
			//else retry?
			System.out.println("Message posted(" + statusCode+"):" + responseStr);

			//for (;;);{"t":"continue"}
			//for (;;);{"t":"refresh"}
			//for (;;);{"t":"refresh", "seq":0}
			//for (;;);{"error":0,"errorSummary":"","errorDescription":"No error.","payload":[],"bootload":[{"name":"js\/common.js.pkg.php","type":"js","src":"http:\/\/static.ak.fbcdn.net\/rsrc.php\/pkg\/60\/106715\/js\/common.js.pkg.php"}]}
			//for (;;);{"error":1356003,"errorSummary":"Send destination not online","errorDescription":"This person is no longer online.","payload":null,"bootload":[{"name":"js\/common.js.pkg.php","type":"js","src":"http:\/\/static.ak.fbcdn.net\/rsrc.php\/pkg\/60\/106715\/js\/common.js.pkg.php"}]}
			System.out.println("+++++++++ PostMessage end +++++++++");
			// testHttpClient("http://www.facebook.com/home.php?");

			ResponseParser.messagePostingResultParser(uid, msg, responseStr);
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private Long keepRequesting(){
		PostMessage("1386786477", "Hello~I am using a facebook chat client!");
		
		seq = getSeq();
		
		//go seq
		while(true){
			//PostMessage("1190346972", "SEQ:"+seq);
			long currentSeq = getSeq();
			System.out.print("My seq:" + seq + " | Current seq:" + currentSeq + '\n');
			if(seq > currentSeq)
				seq = currentSeq;
			
			while(seq <= currentSeq){
				//TODO get the old message between oldseq and seq
				GetMethod get = new GetMethod(getMessageUrl(seq));				
				try {
					httpClient.executeMethod(get);
					
					String msgResponseBody = get.getResponseBodyAsString();
					
					System.out.println("=========msgResponseBody begin=========");
					System.out.println(msgResponseBody);
					System.out.println("+++++++++msgResponseBody end+++++++++");
					
					try {
						ResponseParser.messageRequestResultParser(msgResponseBody);
					} catch (JSONException e) {
						e.printStackTrace();
					}
					seq++;
				} catch (HttpException httpe) {
					System.out.println(httpe.getMessage());
				} catch (IOException ioe) {
					System.out.println(ioe.getMessage());
				}
				
				get.releaseConnection();
			}
		}
	}
	
	private long getSeq() {
		long tempSeq = -1;
		while (tempSeq == -1) {
			GetMethod get = new GetMethod(getMessageUrl(-1));
			
			try {
				httpClient.executeMethod(get);
				
				String msgResponseBody = get.getResponseBodyAsString();

				System.out.println("=========GetSeq: msgResponseBody begin===========");
				System.out.println(msgResponseBody);
				System.out.println("+++++++++GetSeq: msgResponseBody end+++++++++");
				//for (;;);{"t":"refresh", "seq":0}
				tempSeq = parseSeq(msgResponseBody);
				System.out.println("getSeq(): SEQ: " + tempSeq);
				
				if(tempSeq >= 0){
					return tempSeq;
				}
				try {
					System.out.println("retrying to fetch the seq code...");
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (HttpException httpe) {
				System.out.println(httpe.getMessage());
			} catch (IOException ioe) {
				System.out.println(ioe.getMessage());
			} catch (JSONException e) {
				e.printStackTrace();
				//org.json.JSONException: JSONObject["seq"] not found.
				try {
					Thread.sleep(5 * 1000);
				} catch (InterruptedException ie) {
					e.printStackTrace();
				}
			}
			get.releaseConnection();
		}
		return tempSeq;
	}
	
	private long parseSeq(String msgResponseBody) throws JSONException{
		if(msgResponseBody == null)
			return -1;
		String prefix = "for (;;);";
		if(msgResponseBody.startsWith(prefix))
			msgResponseBody = msgResponseBody.substring(prefix.length());
		
		//JSONObject body =(JSONObject) JSONValue.parse(msgResponseBody);
		JSONObject body = new JSONObject(msgResponseBody);
		if(body != null)
			return (Long)body.getLong("seq");
		else
			return -1;
	}
	
	private String getMessageUrl(long seq) {
		//http://0.channel06.facebook.com/x/0/false/p_MYID=-1
		String url = "http://0.channel" + channel + ".facebook.com/x/0/false/p_" + uid + "=" + seq;
		System.out.println("request:" + url);
		return url;
	}
	/**
	 * fetch user's info<br>
	 * fetch buddy list<br>
	 * store them in the BuddyList object
	 */
	public static void getBuddyList(){
		System.out.println("====== getBuddyList begin======");

		String url = "http://www.facebook.com/ajax/presence/update.php";
		PostMethod postMethod = new PostMethod(url);

		// 填入各个表单域的值
		NameValuePair[] data = {
				new NameValuePair("buddy_list", "1"),
				new NameValuePair("notifications", "1"),
				new NameValuePair("force_render", "true"),
				//new NameValuePair("popped_out", "false"),
				new NameValuePair("post_form_id", post_form_id),
				new NameValuePair("user", uid)};
		// 将表单的值放入postMethod中
		postMethod.setRequestBody(data);
		System.out.println("executeMethod ing...");
		try {
			// 执行postMethod
			int statusCode;

			statusCode = httpClient.executeMethod(postMethod);

			String responseStr = postMethod.getResponseBodyAsString();
			//TODO process the respons string
			//if statusCode == 200: no error;(responsStr contains "errorDescription":"No error.")
			//else retry?
			System.out.println("Got buddy list(" + statusCode+"):\n" + responseStr);
			
			//for (;;);{"error":0,"errorSummary":"","errorDescription":"No error.","payload":{"buddy_list":{"listChanged":true,"availableCount":1,"nowAvailableList":{"UID1":{"i":false}},"wasAvailableIDs":[],"userInfos":{"UID1":{"name":"Buddy 1","firstName":"Buddy","thumbSrc":"http:\/\/static.ak.fbcdn.net\/pics\/q_default.gif","status":null,"statusTime":0,"statusTimeRel":""},"UID2":{"name":"Buddi 2","firstName":"Buddi","thumbSrc":"http:\/\/static.ak.fbcdn.net\/pics\/q_default.gif","status":null,"statusTime":0,"statusTimeRel":""}},"forcedRender":true},"time":1209560380000}}  
			//for (;;);{"error":0,"errorSummary":"","errorDescription":"No error.","payload":{"time":1214626375000,"buddy_list":{"listChanged":true,"availableCount":1,"nowAvailableList":{},"wasAvailableIDs":[],"userInfos":{"1386786477":{"name":"\u5341\u4e00","firstName":"\u4e00","thumbSrc":"http:\/\/static.ak.fbcdn.net\/pics\/q_silhouette.gif","status":null,"statusTime":0,"statusTimeRel":""}},"forcedRender":null,"flMode":false,"flData":{}},"notifications":{"countNew":0,"count":1,"app_names":{"2356318349":"\u670b\u53cb"},"latest_notif":1214502420,"latest_read_notif":1214502420,"markup":"<div id=\"presence_no_notifications\" style=\"display:none\" class=\"no_notifications\">\u65e0\u65b0\u901a\u77e5\u3002<\/div><div class=\"notification clearfix notif_2356318349\" onmouseover=\"CSS.addClass(this, 'hover');\" onmouseout=\"CSS.removeClass(this, 'hover');\"><div class=\"icon\"><img src=\"http:\/\/static.ak.fbcdn.net\/images\/icons\/friend.gif?0:41046\" alt=\"\" \/><\/div><div class=\"notif_del\" onclick=\"return presenceNotifications.showHideDialog(this, 2356318349)\"><\/div><div class=\"body\"><a href=\"http:\/\/www.facebook.com\/profile.php?id=1190346972\"   >David Willer<\/a>\u63a5\u53d7\u4e86\u60a8\u7684\u670b\u53cb\u8bf7\u6c42\u3002 <span class=\"time\">\u661f\u671f\u56db<\/span><\/div><\/div>","inboxCount":"0"}},"bootload":[{"name":"js\/common.js.pkg.php","type":"js","src":"http:\/\/static.ak.fbcdn.net\/rsrc.php\/pkg\/60\/106715\/js\/common.js.pkg.php"}]}
			System.out.println("+++++++++ getBuddyList end +++++++++");
			// testHttpClient("http://www.facebook.com/home.php?");
			ResponseParser.buddylistParser(responseStr);

		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void getNotifications(){
		System.out.println("====== getNotifications begin======");

		String url = "http://www.facebook.com/ajax/presence/update.php";
		PostMethod postMethod = new PostMethod(url);

		// 填入各个表单域的值
		NameValuePair[] data = {
				new NameValuePair("notifications", 1 + ""),
				new NameValuePair("post_form_id", post_form_id),
				new NameValuePair("user", uid)};
		// 将表单的值放入postMethod中
		postMethod.setRequestBody(data);
	}
}
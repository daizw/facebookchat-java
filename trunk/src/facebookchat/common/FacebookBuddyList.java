package facebookchat.common;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class FacebookBuddyList {
	
	public static transient FacebookUser me;
	public static transient Map<String, FacebookUser> buddies = new Hashtable<String, FacebookUser>();

	static {
		buddies.clear();
	}
	
	public static Boolean listChanged;
	public static Number availableCount;
	
	public static void updateBuddyList(JSONObject buddyList) throws JSONException {
		//JSONObject buddyList = (JSONObject) payload.get("buddy_list");
		listChanged = (Boolean)buddyList.get("listChanged");
		availableCount = (Number)buddyList.get("availableCount");
		
		System.out.println("listChanged: " + (Boolean)buddyList.get("listChanged"));
		System.out.println("availableCount: " + (Number)buddyList.get("availableCount"));
		
		JSONObject nowAvailableList = (JSONObject) buddyList.get("nowAvailableList");
		JSONObject userInfos = (JSONObject) buddyList.get("userInfos");
		
		if(nowAvailableList == null)
			return;
		
		JSONObject user = (JSONObject) userInfos.get(Launcher.uid);
		me = new FacebookUser(Launcher.uid, user);
		
		//tag all the buddies as offline
		Iterator<String> oldIt = buddies.keySet().iterator();
		while(oldIt.hasNext()){
			String key = oldIt.next();
			System.out.println("userID: " + key);
			buddies.get(key).onlineStatus = OnlineStatus.OFFLINE;
		}
		
		Iterator<String> it = nowAvailableList.keys();
		while(it.hasNext()){
			String key = it.next();
			System.out.println("userID: " + key);
			user = (JSONObject) userInfos.get(key);
			FacebookUser fu = new FacebookUser(key, user);
			buddies.put(key, fu);
			Launcher.getChatroomAnyway(key).setRoomName(fu.name);
			printUserInfo(fu);
		}
	}
	private static void printUserInfo(FacebookUser user){
		System.out.println("name:\t" + user.name);
		System.out.println("firstName:\t" + user.firstName);
		System.out.println("thumbSrc:\t" + user.thumbSrc);
		System.out.println("status:\t" + user.status);
		System.out.println("statusTime:\t" + user.statusTime);
		System.out.println("statusTimeRel:\t" + user.statusTimeRel);
		System.out.println("OnlineStatus:\t" + user.onlineStatus);
	}
}
/*
 * Copyright (c) 2013-2015 by appPlant UG. All rights reserved.
 *
 * @APPPLANT_LICENSE_HEADER_START@
 *
 * This file contains Original Code and/or Modifications of Original Code
 * as defined in and that are subject to the Apache License
 * Version 2.0 (the 'License'). You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at
 * http://opensource.org/licenses/Apache-2.0/ and read it before using this
 * file.
 *
 * The Original Code and all software distributed under the License are
 * distributed on an 'AS IS' basis, WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, AND APPLE HEREBY DISCLAIMS ALL SUCH WARRANTIES,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT.
 * Please see the License for the specific language governing rights and
 * limitations under the License.
 *
 * @APPPLANT_LICENSE_HEADER_END@
 */

package com.qtz.game.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Central way to access all or single local notifications set by specific
 * state like triggered or scheduled. Offers shortcut ways to schedule,
 * cancel or clear local notifications.
 */
public class Manager {

    // Context passed through constructor and used for notification builder.
    private Context context;

    /**
     * Constructor
     *
     * @param context
     *      Application context
     */
    private Manager(Context context){
        this.context = context;
    }

    /**
     * Static method to retrieve class instance.
     *
     * @param context
     *      Application context
     */
    public static Manager getInstance(Context context) {
        return new Manager(context);
    }

    /**
     * Schedule local notification specified by JSON object.
     *
     * @param options
     *      JSON object with set of options
     */
    public NotificationTmp schedule (JSONObject options) {
        return schedule(new Options(context).parse(options));
    }

    /**
     * Schedule local notification specified by options object.
     *
     * @param options
     *      Set of notification options
     */
    public NotificationTmp schedule (Options options) {
        android.util.Log.i("cocos2dnot", "manager schedule start");
        NotificationTmp notification = new Builder(options)
                .setTriggerReceiver(TriggerReceiver.class)
                .build();

        notification.schedule();

        android.util.Log.i("cocos2dnot", "manager schedule end");

        return notification;
    }

    /**
     * Clear local notification specified by ID.
     *
     * @param id
     *      The notification ID
     * @param updates
     *      JSON object with notification options
     */
    public NotificationTmp update (int id, JSONObject updates) {
        NotificationTmp notification = get(id);

        if (notification == null)
            return null;

        notification.cancel();

        JSONObject options = mergeJSONObjects(
                notification.getOptions().getDict(), updates);

        try {
            options.putOpt("updatedAt", new Date().getTime());
        } catch (JSONException ignore) {}

        return schedule(options);
    }

    /**
     * Clear local notification specified by ID.
     *
     * @param id
     *      The notification ID
     */
    public NotificationTmp clear (int id) {
        NotificationTmp notification = get(id);

        if (notification != null) {
            notification.clear();
        }

        return notification;
    }

    /**
     * Clear local notification specified by ID.
     *
     * @param id
     *      The notification ID
     */
    public NotificationTmp cancel (int id) {
        NotificationTmp notification = get(id);

        if (notification != null) {
            notification.cancel();
        }

        return notification;
    }

    /**
     * Clear all local notifications.
     */
    public void clearAll () {
        List<NotificationTmp> notifications = getAll();

        for (NotificationTmp notification : notifications) {
            notification.clear();
        }

        getNotMgr().cancelAll();
    }

    /**
     * Cancel all local notifications.
     */
    public void cancelAll () {
        List<NotificationTmp> notifications = getAll();

        for (NotificationTmp notification : notifications) {
            notification.cancel();
        }

        getNotMgr().cancelAll();
    }

    /**
     * All local notifications IDs.
     */
    public List<Integer> getIds() {
        Set<String> keys = getPrefs().getAll().keySet();
        ArrayList<Integer> ids = new ArrayList<Integer>();

        for (String key : keys) {
            ids.add(Integer.parseInt(key));
        }

        return ids;
    }

    /**
     * All local notification IDs for given type.
     *
     * @param type
     *      The notification life cycle type
     */
    public List<Integer> getIdsByType(NotificationTmp.Type type) {
        List<NotificationTmp> notifications = getAll();
        ArrayList<Integer> ids = new ArrayList<Integer>();

        for (NotificationTmp notification : notifications) {
            if (notification.getType() == type) {
                ids.add(notification.getId());
            }
        }

        return ids;
    }

    /**
     * List of local notifications with matching ID.
     *
     * @param ids
     *      Set of notification IDs
     */
    public List<NotificationTmp> getByIds(List<Integer> ids) {
        ArrayList<NotificationTmp> notifications = new ArrayList<NotificationTmp>();

        for (int id : ids) {
            NotificationTmp notification = get(id);

            if (notification != null) {
                notifications.add(notification);
            }
        }

        return notifications;
    }

    /**
     * List of all local notification.
     */
    public List<NotificationTmp> getAll() {
        return getByIds(getIds());
    }

    /**
     * List of local notifications from given type.
     *
     * @param type
     *      The notification life cycle type
     */
    public List<NotificationTmp> getByType(NotificationTmp.Type type) {
        List<NotificationTmp> notifications = getAll();
        ArrayList<NotificationTmp> list = new ArrayList<NotificationTmp>();

        if (type == NotificationTmp.Type.ALL)
            return notifications;

        for (NotificationTmp notification : notifications) {
            if (notification.getType() == type) {
                list.add(notification);
            }
        }

        return list;
    }

    /**
     * List of local notifications with matching ID from given type.
     *
     * @param type
     *      The notification life cycle type
     * @param ids
     *      Set of notification IDs
     */
    @SuppressWarnings("UnusedDeclaration")
    public List<NotificationTmp> getBy(NotificationTmp.Type type, List<Integer> ids) {
        ArrayList<NotificationTmp> notifications = new ArrayList<NotificationTmp>();

        for (int id : ids) {
            NotificationTmp notification = get(id);

            if (notification != null && notification.isScheduled()) {
                notifications.add(notification);
            }
        }

        return notifications;
    }

    /**
     * If a notification with an ID exists.
     *
     * @param id
     *      NotificationTmp ID
     */
    public boolean exist (int id) {
        return get(id) != null;
    }

    /**
     * If a notification with an ID and type exists.
     *
     * @param id
     *      NotificationTmp ID
     * @param type
     *      NotificationTmp type
     */
    public boolean exist (int id, NotificationTmp.Type type) {
        NotificationTmp notification = get(id);

        return notification != null && notification.getType() == type;
    }

    /**
     * List of properties from all local notifications.
     */
    public List<JSONObject> getOptions() {
        return getOptionsById(getIds());
    }

    /**
     * List of properties from local notifications with matching ID.
     *
     * @param ids
     *      Set of notification IDs
     */
    public List<JSONObject> getOptionsById(List<Integer> ids) {
        ArrayList<JSONObject> options = new ArrayList<JSONObject>();

        for (int id : ids) {
            NotificationTmp notification = get(id);

            if (notification != null) {
                options.add(notification.getOptions().getDict());
            }
        }

        return options;
    }

    /**
     * List of properties from all local notifications from given type.
     *
     * @param type
     *      The notification life cycle type
     */
    public List<JSONObject> getOptionsByType(NotificationTmp.Type type) {
        ArrayList<JSONObject> options = new ArrayList<JSONObject>();
        List<NotificationTmp> notifications = getByType(type);

        for (NotificationTmp notification : notifications) {
            options.add(notification.getOptions().getDict());
        }

        return options;
    }

    /**
     * List of properties from local notifications with matching ID from
     * given type.
     *
     * @param type
     *      The notification life cycle type
     * @param ids
     *      Set of notification IDs
     */
    public List<JSONObject> getOptionsBy(NotificationTmp.Type type,
                                         List<Integer> ids) {

        if (type == NotificationTmp.Type.ALL)
            return getOptionsById(ids);

        ArrayList<JSONObject> options = new ArrayList<JSONObject>();
        List<NotificationTmp> notifications = getByIds(ids);

        for (NotificationTmp notification : notifications) {
            if (notification.getType() == type) {
                options.add(notification.getOptions().getDict());
            }
        }

        return options;
    }

    /**
     * Get existent local notification.
     *
     * @param id
     *      NotificationTmp ID
     */
    public NotificationTmp get(int id) {
        Map<String, ?> alarms = getPrefs().getAll();
        String notId          = Integer.toString(id);
        JSONObject options;

        if (!alarms.containsKey(notId))
            return null;


        try {
            String json = alarms.get(notId).toString();
            options = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        Builder builder = new Builder(context, options);

        return builder.build();
    }

    /**
     * Merge two JSON objects.
     *
     * @param obj1
     *      JSON object
     * @param obj2
     *      JSON object with new options
     */
    private JSONObject mergeJSONObjects (JSONObject obj1, JSONObject obj2) {
        Iterator it = obj2.keys();

        while (it.hasNext()) {
            try {
                String key = (String)it.next();

                obj1.put(key, obj2.opt(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return obj1;
    }

    /**
     * Shared private preferences for the application.
     */
    private SharedPreferences getPrefs () {
        return context.getSharedPreferences(NotificationTmp.PREF_KEY, Context.MODE_PRIVATE);
    }

    /**
     * NotificationTmp manager for the application.
     */
    private NotificationManager getNotMgr () {
        return (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static void cleanAllInfo(Context _context) {
        	//clear ids
        	SharedPreferences prefs = _context.getSharedPreferences(NotificationTmp.PUSH_IDS_FILE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
    			AlarmManager aManager = (AlarmManager) _context.getSystemService(Context.ALARM_SERVICE);
    	        Set<String> keys = prefs.getAll().keySet();

    	        for (String key : keys) {
    	        	android.util.Log.e("cocos2dnot", "------ clear key : " +key);  
    	        	Intent intentTo = new Intent(_context, TriggerReceiver.class);
    				intentTo.setAction(key);
    				PendingIntent stopPI = PendingIntent.getBroadcast(  
    						_context, 0, intentTo,  PendingIntent.FLAG_UPDATE_CURRENT);
    				aManager.cancel(stopPI);  
    	        }
    	        editor.clear();
    	        editor.commit();
                android.util.Log.e("cocos2dnot", "------ cancel alarm:" + _context.getPackageName());  
                
                //clear info
            	SharedPreferences prefs2 = _context.getSharedPreferences(NotificationTmp.PUSH_INFO_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor2 = prefs2.edit();  
                editor2.clear();
                editor2.commit();
	}
}

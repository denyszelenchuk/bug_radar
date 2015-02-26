/** Copyright © 2015 Denys Zelenchuk.
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.**/

package com.error.hunter;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ListenService extends Service {
		
		protected ActivityManager mActivityManager;
		private String versionName = "";
		private int versionCode = 0;
		ArrayList<String> list = new ArrayList<String>();
		private final IBinder mBinder = new LocalBinder();
		Set<String> set = new HashSet<String>();
		SharedPreferences settings;
		StringBuffer strBuf = new StringBuffer();
		String packageName;
		String badProcess = "";

	    public class LocalBinder extends Binder {
	    	ListenService getService() {
	            return ListenService.this;
	        }
	    }
	    
	    @Override
	    public int onStartCommand(Intent intent, int flags, int startId) {
	    	
	    	settings = getSharedPreferences("eCatcher", 0);
			set = settings.getStringSet("ARRAY", (new HashSet<String>()));
	        return START_STICKY;
	    }
	    
	    @Override
	    public void onCreate() {
	    	
	    	final Context context = getApplicationContext();
			mActivityManager = (ActivityManager)this.getSystemService(Context.ACTIVITY_SERVICE);

			new Thread(new Runnable(){
			    public void run() {
				    while(true){
				    	try {
							Thread.sleep(750);
							
					        List<ActivityManager.ProcessErrorStateInfo> errList;        
					        errList = mActivityManager.getProcessesInErrorState();
					        
					        if (errList != null){
					        	
						        Iterator<ActivityManager.ProcessErrorStateInfo> iter = errList.iterator();
						        while (iter.hasNext()) {
							        ActivityManager.ProcessErrorStateInfo entry = iter.next();
							        
							        for(String packageName: set){
								        if (entry.processName.equals(packageName)) {
								        	generateBugReport(entry);
								        	createNotification(context);
				
								            strBuf.delete(0, strBuf.length());
								            errList.clear();
								            Thread.sleep(20000);
								        }
							        }
						        }
					        }
						} catch (InterruptedException e) {
							e.printStackTrace();
						} 
				    }
			    }
			}).start();
	    }

		 private void generateBugReport(ActivityManager.ProcessErrorStateInfo entry) {
	            
	            String condition;
	            badProcess = entry.processName;
	    		String build = Build.DISPLAY;
	    		String fingerprint = Build.FINGERPRINT;
	    		String serial = Build.SERIAL;
	    		String product = Build.PRODUCT;
	    		String model = Build.MODEL;
	
	    		File path = Environment.getDataDirectory();
	    		StatFs stat = new StatFs(path.getPath());
	    		@SuppressWarnings("deprecation")
				long blockSize = stat.getBlockSize();
	    		@SuppressWarnings("deprecation")
				long availableBlocks = stat.getAvailableBlocks();
	    		String memory = Formatter.formatFileSize(this, availableBlocks * blockSize).toString();
	    		
	    		MemoryInfo mi = new MemoryInfo();
	    		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    		activityManager.getMemoryInfo(mi);
	    		long availableMegs = mi.availMem / 1048576L;

	    		strBuf.append("Problem detected in: ").append(badProcess);
	    		if(entry.tag != null){
	    			strBuf.append(" (").append(entry.tag).append(")");
	    		}
	    		strBuf.append("\nDevice product: " + product);
	    		strBuf.append("\nDevice model: " + model);
	    		strBuf.append("\nDevice build: " + build);
	    		strBuf.append("\nDevice fingerprint: " + fingerprint);
	    		strBuf.append("\nDevice SN: " + serial);
	    		strBuf.append("\nDevice available RAM (MB): " + availableMegs);
	    		strBuf.append("\nDevice free phisical memory: " + memory +  "\n");
	    		strBuf.append(getNetworkInfo());
	    		strBuf.append("\n");
	    		
	    		ActivityManager actvityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
	    		List<ActivityManager.RunningServiceInfo> procInfos = actvityManager.getRunningServices(1000);
	    		Iterator<RunningServiceInfo> taskIter = procInfos.iterator();
	    		RunningServiceInfo info;
	    		while(taskIter.hasNext()){
	    			info = taskIter.next();
	    			if(info.process.equals(badProcess)){
	    				strBuf.append("\nService " + info.service + " crash count: " + info.crashCount + " active since: " + info.activeSince + " process: " + info.process);
	    			}
	    		}
	    		strBuf.append("\n");
	    		//android.os.Debug.MemoryInfo[] miPid = activityManager.getProcessMemoryInfo(new int[]{entry.pid}); 
	    		//String memoryProc = miPid[0].toString();
	    		//strBuf.append("\nRAM used by process (Process: " + entry.processName + " PID: " + entry.pid +"): " + memoryProc + "MB\n");
	
	            switch (entry.condition) {
	
	            case ActivityManager.ProcessErrorStateInfo.CRASHED:
	                condition = "CRASHED";
	                getDeviceProcessInfo(badProcess, entry.stackTrace);
	                writeLogsToFile(condition, badProcess);
	                break;
	            case ActivityManager.ProcessErrorStateInfo.NOT_RESPONDING:
	                condition = "ANR";
	                getDeviceProcessInfo(badProcess, "");
	                writeLogsToFile(condition, badProcess);
	                break;
	            default:
	                condition = "<unknown>";
	                getDeviceProcessInfo(badProcess, entry.stackTrace);
	                writeLogsToFile(condition, badProcess);
	                break;
	            }
		 }
		 
		 @SuppressLint("SimpleDateFormat")
		private void writeLogsToFile(String errorType, String process){
			 try {
            	 File root = new File(Environment.getExternalStorageDirectory(), this.getString(R.string.app_name));
     	        
				 if (!root.exists()) {
					 root.mkdirs();
				 }
				 
				 SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmssZ");
				 String date = sdf.format(new Date(System.currentTimeMillis()));
				 date = date.substring(0, 12);
				 
				 File errorFile = null;
				 if (errorType.equals("ANR")){
					 strBuf.append(getCPUUsage());
					 errorFile = new File(root, "ANR_" + process + "_" + date + ".txt");
				 } else if (errorType.equals("CRASHED")){
					 errorFile = new File(root, "CRASH_" + process + "_" + date + ".txt");
				 } else if (errorType.equals("<unknown>")){
					 errorFile = new File(root, "unknown_" + process + "_" + date + ".txt");
				 } 
				 FileWriter writer = new FileWriter(errorFile);
				 writer.append(strBuf.toString());
				 writer.flush();
				 writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		 }		
		 
		 private void getDeviceProcessInfo(String processName, String stackTrace){
			try {
			    PackageInfo manager=getPackageManager().getPackageInfo(processName, 0);
			    
			    versionName = manager.versionName;
			    versionCode = manager.versionCode;
			    
			} catch (NameNotFoundException e) {
			    //Handle exception
			}
			
			strBuf.append("\nProcess name: " + processName);
			strBuf.append("\nVersion name: " + versionName);
			strBuf.append("\nVersion code: " + versionCode + "\n");
			strBuf.append(stackTrace);
		 }
			 
		private String getCPUUsage(){
			String stats = "";
			Process p;
			try {
				p = Runtime.getRuntime().exec("top -m 20 -d 1 -n 1");
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line = reader.readLine();
				while (line != null){
					stats = stats + line.toString() + "\n";
					line = reader.readLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return stats;
		}
		
		private StringBuffer getNetworkInfo(){
			StringBuffer info = new StringBuffer();
			StringBuffer backUpInfo = new StringBuffer();
			
		    final ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
			
		    final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		    final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

		    if (wifi.isConnected()){
		    	info.append("\nWiFi network state: CONNECTED");
		    } else{
		    	info.append("\nWiFi network state: NOT CONNECTED");
		    }
		    backUpInfo = new StringBuffer(info);
		    try{
			    if (!mobile.isConnected()) {
				    switch (mobile.getSubtype()){
			    	case TelephonyManager.NETWORK_TYPE_GPRS:
			    		info.append("\nMobile network state: CONNECTED (GPRS)");
			    		break;
			    	case TelephonyManager.NETWORK_TYPE_EDGE:
			    		info.append("\nMobile network state: CONNECTED (EDGE)");
			    		break;
			    	case TelephonyManager.NETWORK_TYPE_CDMA:
			    		info.append("\nMobile network state: CONNECTED (CDMA)");
			    		break;
			    	case TelephonyManager.NETWORK_TYPE_HSPA:
			    		info.append("\nMobile network state: CONNECTED (HSPA)");
			    		break;
			    	case TelephonyManager.NETWORK_TYPE_HSPAP:
			    		info.append("\nMobile network state: CONNECTED (HSPA+)");
			    		break;
			    	case TelephonyManager.NETWORK_TYPE_LTE:
			    		info.append("\nMobile network state: CONNECTED (LTE)");
			    		break;
			    	case TelephonyManager.NETWORK_TYPE_UMTS:
			    		info.append("\nMobile network state: CONNECTED (UMTS)");
			    		break;
			    }
			    } else{
			    	info.append("\nMobile network state: NOT CONNECTED");
			    }
		    } catch (Exception e){
		    	e.printStackTrace();
		    	return backUpInfo;
		    }
			return info;
		}
	  
		@SuppressLint("SimpleDateFormat")
		public static String formatData(long data) {
		    Date d = new Date(data);
		    DateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    String str = format2.format(d);
	    	return str;
		}
			 
	private void createNotification(Context context){
		NotificationCompat.Builder builder =  
	            new NotificationCompat.Builder(context)  
	            .setSmallIcon(R.drawable.red_android)  
	            .setContentTitle(this.getString(R.string.app_name))  
	            .setContentText(this.getString(R.string.catch_error) + " " + badProcess);  
		
	    Random randomGenerator = new Random();
	    int randomInt = randomGenerator.nextInt(1000);
		
	    Intent emailIntent = new Intent(ListenService.this, SendEmailActivity.class);
	    emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    emailIntent.putExtra("REPORT", strBuf.toString());
	    emailIntent.putExtra("NOTIFICATION_ID", randomInt);

	    PendingIntent contentIntent = PendingIntent.getActivity(context, 0, emailIntent,   
	            PendingIntent.FLAG_UPDATE_CURRENT);  
	    builder.setContentIntent(contentIntent);  
	
	    // Add as notification  
	    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);  
	    manager.notify(randomInt, builder.build()); 
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
	
		return mBinder;
	}
}

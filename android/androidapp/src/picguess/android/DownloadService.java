package picguess.android;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DownloadService extends Service
{
	Context mycontext;
	List<Challenge> stored_challenges = Collections.synchronizedList(new ArrayList<Challenge>());
	List<Challenge> played_challenges = Collections.synchronizedList(new ArrayList<Challenge>());
	Challenge current_challenge = null;
	String play_log = "";
	int len_play_log = 0;
	int score = -1;
	int n_photos_shown = 0;
	int n_photos_correctly_answered = 0;
	String stored_username = "";
	String stored_password = "";

	HttpClient httpclient_picguess = new DefaultHttpClient();
	HttpClient httpclient_photosite = new DefaultHttpClient();
	Thread downloadThread = new DownloadThread();
	int download_status = 1; //1=working, 0=problem
	String download_error_message = "";
	boolean stopDownloadThread = false;
	boolean sync_in_progress = false;
	int sync_nfailures = 0;
	int sync_status = 1; //1=working, 0=problem
	String sync_error_message = "";
	
	public class DownloadThread extends Thread
	{
		public void run()
		{
			//Log.v("MyMessages", "DownloadThread started");
			int nfailures = 0;
		
			//##################we should delete all the files###########################
			//remove old files if any
	        String[] FL = mycontext.fileList();
    		for(int i=0;i<FL.length;i++)
    			mycontext.deleteFile(FL[i]);
			
    		int didwork = 0;
    		HttpGet httpget_challenge = new HttpGet(getString(R.string.url_base)+getString(R.string.url_challenge));
    		
			while(!stopDownloadThread)
			{
		        if (stored_challenges.size()<5)
		        {
		        	didwork = 1;
		            try 
		            {
		                HttpResponse response = httpclient_picguess.execute(httpget_challenge);
		                HttpEntity entity = response.getEntity();
		                InputStream inputStream = entity.getContent();
		                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		                String line="", result="";
		                while ((line = reader.readLine()) != null) {
		                	result += line + '\n';
		                }
		                //Log.v("MyMessage", "server response="+result+'\n');
		                JSONObject json = new JSONObject(result);
		                int success = json.getInt("success");
		                if (success==0)
		                {
		                	download_error_message = json.getString("message");
		                	break;
		                }
			            JSONArray challenges = json.getJSONArray("challenges");
			            for(int i=0; i<challenges.length(); i++)
			            {
			            	JSONObject challenge = challenges.getJSONObject(i);
			            	Challenge C = new Challenge();
			            	C.photo_id = challenge.getInt("photo_id");
			            	C.photo_owner = challenge.getString("photo_owner");
			            	C.photo_site = challenge.getString("photo_site");
			            	C.photo_url = challenge.getString("photo_url");
			            	JSONArray options = challenge.getJSONArray("options");
			            	for(int ii=0;ii<4;ii++)
			            		C.options[ii] = options.getString(ii);
			            	C.correct_option = challenge.getInt("correct_option");
			            	C.answered_correctly = -1;
			            	C.choice_selected = -1;
			            	C.already_played = 0;
			            	C.filename = challenge.getString("photo_id")+".dat";

			            	HttpGet httpget = new HttpGet(C.photo_url);
			            	HttpResponse response1 = httpclient_photosite.execute(httpget);
			            	if (response1.getStatusLine().getStatusCode()!=HttpStatus.SC_OK)
			            		continue;
			            	HttpEntity entity1 = response1.getEntity();
			            	BufferedHttpEntity bufentity = new BufferedHttpEntity(entity1);
			            	InputStream inStream = bufentity.getContent();
			            	FileOutputStream fos = mycontext.openFileOutput(C.filename, MODE_PRIVATE);
			            	byte buf[]=new byte[1024];
			            	int len;
			            	while((len=inStream.read(buf))>0)
			            		fos.write(buf,0,len);
			            	fos.close();            
			            	inStream.close();
			            	entity1.consumeContent();
			            	stored_challenges.add(C);
			            }
		                
		            } catch (MalformedURLException e) {
		            	nfailures += 1;
		                e.printStackTrace();
		                if(nfailures>=3)
		                {
		                	download_error_message = "Unable to contact the server";
		                	break;
		                }
		            } catch (IOException e) {
		            	nfailures += 1;
		                e.printStackTrace();
		                if(nfailures>=3)
		                {
		                	download_error_message = "Unable to contact the server";
		                	break;
		                }
		            } catch (JSONException e) {
		            	nfailures += 1;
						e.printStackTrace();
						if(nfailures>=3)
						{
		                	download_error_message = "Unable to contact the server";
		                	break;
						}
					}
		        }

				if(played_challenges.size()>5 || len_play_log>5)
				{
					didwork = 1;
					sync_score();
				}
				
				if (didwork==1)
				{
					didwork = 0;
			        try {
						sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			download_status = 0;
			//Log.v("MyMessage", "DownloadThread ends: " + stopDownloadThread + ", " + nfailures);
		}
	}
	
	
    public int sync_score()
    {
    	//Log.v("MyMessages", "sync_score() started");
    	if(sync_in_progress)
    	{
        	//Log.v("MyMessages", "sync_score() already in progress");
    		return 1;
    	}
    	sync_in_progress = true;
    	if(sync_nfailures>=3)
    	{
    		//Log.v("MyMessages", "sync_score() nfailures>=3");
    		sync_status = 0;
    		sync_in_progress = false;
    		return 0;
    	}
    	
    	//Log.v("MyMessages", "sync_score before: update_view="+update_view+"play_log="+state.play_log+", size(played_challenges)="+state.played_challenges.size());
    	while(!played_challenges.isEmpty())
    	{
    		Challenge C = played_challenges.remove(0);
    		play_log += C.photo_id + ":" + C.answered_correctly + ";";
    		len_play_log += 1;
    	}
    	//Log.v("MyMessages", "sync_score after : update_view="+update_view+"play_log="+state.play_log+", size(played_challenges)="+state.played_challenges.size());
    	    	
        HttpPost httppost = new HttpPost(getString(R.string.url_base)+getString(R.string.url_update));  
        try 
        {  
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair("username", stored_username));
            nameValuePairs.add(new BasicNameValuePair("password", stored_password));
            nameValuePairs.add(new BasicNameValuePair("corrections",play_log));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
            HttpResponse response = httpclient_picguess.execute(httppost);
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line="", result="";
            while ((line = reader.readLine()) != null) {
            	result += line + '\n';
            }
            //Log.v("MyMessage", "sync_score() server response="+result+'\n');
            JSONObject json = new JSONObject(result);
            int success = json.getInt("updated");
            if (success==1)
            {
            	score = json.getInt("score");
        		play_log = "";
        		len_play_log = 0;
            }
            else
            {
            	sync_nfailures += 1;
            	sync_error_message = json.getString("message");
            	if(sync_error_message.equals(""))
            		sync_error_message = "Failed to update score on the server";
            }
        } 
        catch (ClientProtocolException e) {  
            e.printStackTrace();
        	sync_nfailures += 1;
    		sync_in_progress = false;
            return 0;
        } catch (IOException e) {  
            e.printStackTrace();
        	sync_nfailures += 1;
    		sync_in_progress = false;
            return 0;
        } catch (JSONException e) {
			e.printStackTrace();
        	sync_nfailures += 1;
    		sync_in_progress = false;
			return 0;
		}  
        
		sync_in_progress = false;
        return 1;
    }


	
	
	public void onCreate() 
	{
		//Log.v("MyMessages", "DownloadService : onCreate()");
	      super.onCreate();
	      mycontext = this.getBaseContext();
	      downloadThread.start();	      
	} 
	
	public void onDestroy()
	{
		super.onDestroy(); 
		stopDownloadThread = true;
	}
	
	public IBinder onBind(Intent intent) 
	{
		//Log.v("MyMessages", "DownloadService : onBind()");
		return myBinder;
	}
	
	public boolean  onUnbind  (Intent intent)
	{
		return false;
	}
	
	
	private final DownloadInterface.Stub myBinder = new DownloadInterface.Stub() {

		public void setCredentials(String username, String password)
		{
			stored_username = username;
			stored_password = password;
			sync_nfailures = 0;
			sync_status = 1;
			score = -1;
		}
		
		public void getCredentials(String[] credentials)
		{
			credentials[0] = stored_username;
			credentials[1] = stored_password;
		}
		
		public Challenge getNewChallenge()
		{
			if(current_challenge!=null)
			{
				mycontext.deleteFile(current_challenge.filename);
			}
			if(stored_challenges.isEmpty())
				return null;
			current_challenge = stored_challenges.remove(0);
			return current_challenge;
		}
		
		public Challenge getCurrentChallenge()
		{
			return current_challenge;
		}
		
		public int reportAnswer(Challenge C)
		{
			played_challenges.add(C);
			n_photos_shown += 1;
			if(C.answered_correctly==1)
				n_photos_correctly_answered += 1;
			return 1;
		}
		
		public int getScore(int sync_now, String[] session_score)
		{
			session_score[0] = n_photos_correctly_answered + "/" + n_photos_shown;
			if(sync_now==1)
				sync_score();
			return score;
		}
		
		//0 = everything fine
		//1 = download challenge error
		//2 = sync error
		public int getStatus(String[] status)
		{
			if(download_status==0)
			{
				status[0] = download_error_message;
				return 1;
			}
			if(sync_status==0)
			{
				status[0] = sync_error_message;
				return 2;
			}
			status[0] = "";
			return 0;
		}
	};
	
}
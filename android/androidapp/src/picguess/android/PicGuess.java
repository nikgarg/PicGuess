package picguess.android;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


public class PicGuess extends Activity implements OnClickListener{
	
	private Context mycontext = null;

	
	public static class State implements Parcelable{

		int score = 0;
		int is_logged_in = 0;
		String stored_username = "";
		String stored_password = "";
		
		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel dest, int flags) {
	         dest.writeInt(score);
	         dest.writeInt(is_logged_in);
	         dest.writeString(stored_username);
	         dest.writeString(stored_password);
		}
		
	    public static final Parcelable.Creator<State> CREATOR
            = new Parcelable.Creator<State>() {
            public State createFromParcel(Parcel in) {
            return new State(in);
            }

            public State[] newArray(int size) {
                return new State[size];
            }
        };
 
        private State(Parcel in) {
            score = in.readInt();
            is_logged_in = in.readInt();
            stored_username = in.readString();
            stored_password = in.readString();
        }

        public State()
        {
        }
	}
	
	Button[] choice_button = new Button[4];
	Button submit_button;
	ImageButton login_button;
	ImageButton exit_button;
	ImageButton score_sync_button;
	Button broken_image_button;
	TextView score_overall_view, score_session_view;
	TextView copyright_view;
	TextView username_view;
	State state = new State();
	Challenge current_challenge = null;
	
	RemoteServiceConnection conn = null;
	DownloadInterface downloadService = null;
	private ProgressDialog progressDialog_service = null;
	Bundle savedState;
	boolean service_bound = false;
	
	int error_alert_shown = 0;
	AlertDialog current_alert = null;
	
	class RemoteServiceConnection implements ServiceConnection 
	{
		public void onServiceConnected(ComponentName className, IBinder boundService ) {
			//Log.v("MyMessages", "onServiceConnected()" );
			if(downloadService==null)
			{
				downloadService = DownloadInterface.Stub.asInterface(boundService);
				initialize_view();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			downloadService = null;
	        //Log.v("MyMessages", "onServiceDisconnected" );
	    }
	}; 
		
	public void onSaveInstanceState(Bundle savedInstanceState) {
		//Log.v("MyMessages", "Saving State");
		savedInstanceState.putParcelable("state", state);
		super.onSaveInstanceState(savedInstanceState);
	}

	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savedState = savedInstanceState;
        //Log.v("MyMessages", "Activity onCreate()");
        setContentView(R.layout.main);        
        mycontext = this.getBaseContext();
        
        
        choice_button[0] = (Button)findViewById(R.id.choice1);
        choice_button[1] = (Button)findViewById(R.id.choice2);
        choice_button[2] = (Button)findViewById(R.id.choice3);
        choice_button[3] = (Button)findViewById(R.id.choice4);
    	submit_button = (Button)findViewById(R.id.submit);
    	login_button = (ImageButton)findViewById(R.id.login);
    	broken_image_button = (Button)findViewById(R.id.broken_image);
    	exit_button = (ImageButton)findViewById(R.id.exit);
    	score_sync_button = (ImageButton)findViewById(R.id.score_sync);
    	score_overall_view = (TextView)findViewById(R.id.score_overall);
    	score_session_view = (TextView)findViewById(R.id.score_session);
    	username_view = (TextView)findViewById(R.id.username);
    	copyright_view = (TextView)findViewById(R.id.copyright);
    	
        for (int i=0;i<4;i++)
        	choice_button[i].setOnClickListener(this);
        submit_button.setOnClickListener(this);
        broken_image_button.setOnClickListener(this);
        login_button.setOnClickListener(buttonListener);
        exit_button.setOnClickListener(buttonListener);       
        score_sync_button.setOnClickListener(buttonListener);

        progressDialog_service = ProgressDialog.show(this, "Please Wait...", "Loading challenge", true,false);
        
		if(downloadService==null)
		{
			startService(new Intent(mycontext, picguess.android.DownloadService.class));
			if(conn==null)
				conn = new RemoteServiceConnection(); 
			bindService(new Intent(mycontext, picguess.android.DownloadService.class), conn, Context.BIND_AUTO_CREATE);
			service_bound = true;
		}
                
    }
	
	private int initialize_view()
	{
		//Log.v("MyMessages", "initialze_view()");
        if (savedState!=null)
        	state = savedState.getParcelable("state");

    	try
    	{
    		String[] credentials = {"", ""};
    		downloadService.getCredentials(credentials);
    		if(!credentials[0].equals("") && !credentials[1].equals(""))
    		{
    			//Log.v("MyMessages", "initialze_view() setting credentials from downloadservice");
    			state.stored_username = credentials[0];
    			state.stored_password = credentials[1];
    			state.is_logged_in = 1;
    			String[] session_score = {"0/0"}; 
				int score = downloadService.getScore(0, session_score);
				//Log.v("MyMessages", "initialze_view() downloadservice returned score:" + score);
				if(score!=-1)
					state.score = score;
				else if(state.score==0)
				{
        			SharedPreferences settings = mycontext.getSharedPreferences("auth", 0);
        			state.score = settings.getInt("score", 0);
					//Log.v("MyMessages", "initialze_view() savedstate.score=0, shared pref.score:" + state.score);
				}
    		}
    		else if(state.stored_username.equals("") || state.stored_password.equals(""))
    		{
    			SharedPreferences settings = mycontext.getSharedPreferences("auth", 0);
    			state.stored_username = settings.getString("username", "");
    			state.stored_password = settings.getString("password", "");
    			state.is_logged_in = settings.getInt("is_logged_in", 0);
    			downloadService.setCredentials(state.stored_username, state.stored_password);
    			if(state.score==0)
    				state.score = settings.getInt("score", 0);
    			
    		}
    	} catch (RemoteException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}

        int loaded = 0;
        current_challenge = null;
		try {
			current_challenge = downloadService.getCurrentChallenge();
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        if(current_challenge!=null)	
        	loaded = loadChallenge(current_challenge);
        if(loaded==0)
        	loaded = loadNewChallenge();

        set_buttons(current_challenge);
        set_userinfo(0);
        
        progressDialog_service.dismiss();
        return loaded;
	}
    
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }
    
    public boolean onPrepareOptionsMenu(Menu menu)
    {
    	MenuItem menu_login = menu.findItem(R.id.menu_login);
    	MenuItem menu_logout = menu.findItem(R.id.menu_logout);
    	MenuItem menu_signup = menu.findItem(R.id.menu_signup);
    	MenuItem menu_settings = menu.findItem(R.id.menu_settings);
    	if (state.is_logged_in==1)
    	{
    		menu_settings.setEnabled(true);
    		menu_settings.setVisible(true);
    		menu_login.setEnabled(false);
    		menu_login.setVisible(false);
    		menu_signup.setEnabled(false);
    		menu_signup.setVisible(false);
    		menu_logout.setEnabled(true);
    		menu_logout.setVisible(true);
    	}
    	else
    	{
    		menu_settings.setEnabled(false);
    		menu_settings.setVisible(false);
    		menu_login.setEnabled(true);
    		menu_login.setVisible(true);
    		menu_signup.setEnabled(true);
    		menu_signup.setVisible(true);
    		menu_logout.setEnabled(false);
    		menu_logout.setVisible(false);
    	}
    	return true;
    }
    
    
    protected void onDestroy()
    {
    	if(conn!=null && service_bound)
    		unbindService(conn);    	
    	
    	super.onDestroy();
    }
     
    protected void onStop()
    {
    	//Log.v("MyMessages", "onStop()");
    	
    	if (current_alert!=null)
    	{
    		current_alert.dismiss();
    		//Log.v("MeMessages", "OnStop(): dismissed alert");
    	}
    	
    	if(progressDialog_service!=null)
    		progressDialog_service.dismiss();

    	super.onStop();
    	    	
    	SharedPreferences settings = mycontext.getSharedPreferences("auth", 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString("username", state.stored_username);
    	editor.putString("password", state.stored_password);
    	editor.putInt("is_logged_in", state.is_logged_in);
    	editor.putInt("score", state.score);
    	editor.commit();
     }
    
    public int set_message(String msg)
    {
    	copyright_view.setText(msg);
    	return 1;
    }
    
    public int show_alert(String msg)
    {
    	if(current_alert!=null)
    		current_alert.dismiss();
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(msg);
    	builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
           }
        });
    	AlertDialog alert = builder.create();
    	current_alert = alert;
    	alert.show();
    	return 1;
    }
    
    public int set_userinfo(int sync_score_now)
    {    
    	String[] session_score = {"0/0"};
    	int score = -1;
    	try
    	{
    		score = downloadService.getScore(sync_score_now, session_score);
    		//Log.v("MyMessages", "download service returned score:" + score);
    	}
    	catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	if (state.is_logged_in==1)
    	{
			try {
				if(score!=-1)
					state.score = score;
				String[] status_message = {""};
				int status_code = downloadService.getStatus(status_message);
	    		if(status_code==2 && error_alert_shown==0)
	 		   	{
	 		   		if(status_message[0].equals(""))
	 		   			status_message[0] = "Failed to update the score on the server.";
	 		   		show_alert(status_message[0]);
	 		   		error_alert_shown += 1;
	 		   	}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		username_view.setText(state.stored_username);	
			login_button.setImageResource(R.drawable.logout);
    	    score_overall_view.setText("Overall Score: " + state.score);
    	    score_session_view.setText("This Session: " + session_score[0]);
    	    if(sync_score_now==0)
    	    	score_sync_button.setVisibility(0);
    	    else
    	    	score_sync_button.setVisibility(4);
    	}
    	else
    	{
    		username_view.setText("");
    		login_button.setImageResource(R.drawable.login);
    		score_overall_view.setText("Overall Score: Requires Login");
    		score_session_view.setText("This Session: " + session_score[0]);
    		score_sync_button.setVisibility(4);
    	}
    	return 1;
    }
    
    public int set_buttons(Challenge challenge)
    {
    	int i;
    	
        for (i=0;i<4;i++)
        {
        	choice_button[i].setEnabled(false);
        	choice_button[i].setBackgroundColor(0x00000000);
        	choice_button[i].setText("");
        }
        submit_button.setEnabled(false);
		submit_button.setTextColor(0xffffffff);
		submit_button.setText("");
		broken_image_button.setEnabled(false);
		broken_image_button.setVisibility(4);
		
    	if(challenge!=null)
    	{
    		for(i=0;i<4;i++)
    			choice_button[i].setText(challenge.options[i]);

    		if (challenge.already_played==1)
    		{
    			choice_button[challenge.choice_selected].setBackgroundColor(0xaaff0000);
    			choice_button[challenge.correct_option].setBackgroundColor(0xaa00ff00);
    			submit_button.setText("Next");
    			submit_button.setTextColor(0xff228b22);
    			submit_button.setEnabled(true);
    		}
    		else
    		{
    			for(i=0;i<4;i++)
    				choice_button[i].setEnabled(true);
    			submit_button.setText("Go");
    			if (challenge.choice_selected!=-1)
    			{
    				choice_button[challenge.choice_selected].setBackgroundColor(0xaa0000ff);
    				submit_button.setTextColor(0xff228b22);
    				submit_button.setEnabled(true);
    			}
    			broken_image_button.setEnabled(true);
    			broken_image_button.setVisibility(0);
    		}
    	}
        
    	return 1;
    }
    
    
    public void onClick(View v)
    {
    	switch(v.getId())
    	{
    		case R.id.choice1:
    			current_challenge.choice_selected = 0;
    			break;
    			
    		case R.id.choice2:
    			current_challenge.choice_selected = 1;
    			break;
    			
    		case R.id.choice3:
    			current_challenge.choice_selected = 2;
    			break;
    			
    		case R.id.choice4:
    			current_challenge.choice_selected = 3;
    			break;
    			
    		case R.id.submit:
    			if (current_challenge.already_played==0)
    			{
    				current_challenge.already_played = 1;
    				if (current_challenge.correct_option==current_challenge.choice_selected)
    					current_challenge.answered_correctly = 1;
    				else
    					current_challenge.answered_correctly = 0;
    				try {
    					downloadService.reportAnswer(current_challenge);
    				} catch (RemoteException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    				set_userinfo(0);
    			}
    			else
    			{
    				loadNewChallenge();
    			}
    			break;
    			
    		case R.id.broken_image:
            	AlertDialog.Builder builder = new AlertDialog.Builder(this);
            	builder.setMessage("Are you sure?");
            	builder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
            	{
     	           public void onClick(DialogInterface dialog, int id) 
     	           {
     	        	   report404(current_challenge.photo_id);
     	        	   loadNewChallenge();
     	        	   set_buttons(current_challenge);
     	           }
            	});
     	        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
     	  	           public void onClick(DialogInterface dialog, int id) {
     	 	                dialog.cancel();
     	 	           }
     	 	        });
     	        AlertDialog alert = builder.create();
     	        alert.show();
    			break;	
    	}
    	
    	set_buttons(current_challenge);
    }
    
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        switch (item.getItemId()) 
        {
        case R.id.menu_login:
    		Intent intent = new Intent(mycontext, picguess.android.Authentication.class);
    		startActivityForResult(intent, 1);  
    		return true;
        case R.id.menu_logout:
        	state.score = 0;
			state.is_logged_in = 0;
			state.stored_username = "";
			state.stored_password = "";
			try {
				downloadService.setCredentials(state.stored_username, state.stored_password);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		set_userinfo(0);
    		return true;
        case R.id.menu_signup:
    		Intent intent2 = new Intent(mycontext, picguess.android.Signup.class);
    		startActivityForResult(intent2, 2);  
    		return true;
        case R.id.menu_about:
        	show_alert(getString(R.string.about));
        	return true;
        case R.id.menu_feedback:
        	Intent intent3 = new Intent(mycontext, picguess.android.Feedback.class);
        	intent3.putExtra("is_logged_in", state.is_logged_in);
        	intent3.putExtra("username", state.stored_username);
        	intent3.putExtra("password", state.stored_password);
    		startActivityForResult(intent3, 3);  
        	return true;
        case R.id.menu_rankings:
    		Intent intent4 = new Intent(mycontext, picguess.android.Statistics.class);
           	intent4.putExtra("is_logged_in", state.is_logged_in);
        	intent4.putExtra("username", state.stored_username);
        	intent4.putExtra("password", state.stored_password);
    		startActivityForResult(intent4, 4);  
    		return true;
        case R.id.menu_settings:
    		Intent intent5 = new Intent(mycontext, picguess.android.Settings.class);
           	intent5.putExtra("is_logged_in", state.is_logged_in);
        	intent5.putExtra("username", state.stored_username);
        	intent5.putExtra("password", state.stored_password);
    		startActivityForResult(intent5, 5);  
        	return true;
        case R.id.menu_exit:
        	stopService(new Intent(mycontext, picguess.android.DownloadService.class));
        	finish();
        	return true;
        }
        return false;
    }

    
    public int loadChallenge(Challenge challenge)
    {
		try {
			//Log.v("MyMessage", "reading file: "+challenge.filename);
			InputStream fis = mycontext.openFileInput(challenge.filename);
	        Bitmap bitmap = BitmapFactory.decodeStream(fis);
	        fis.close();
            ImageView imView = (ImageView)findViewById(R.id.challenge_image);
            imView.setImageBitmap(bitmap);
            
          	set_message("Photo by '" + challenge.photo_owner + "' on " + challenge.photo_site);
        	//Log.v("MyMessage", "challenge loaded!");
        	
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return 0;
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
        return 1;
    }
    
    public int loadNewChallenge()
    {   
    	//Log.v("MyMessage", "load new challenge called");
    	current_challenge = null;

    	long wait = 0;
    	long max_wait = 10000;
    	int loaded = 0;
    	while (current_challenge==null && loaded==0 && wait<max_wait)
    	{
    		try {
				current_challenge = downloadService.getNewChallenge();
				if(current_challenge!=null)
					loaded = loadChallenge(current_challenge);
			} catch (RemoteException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return 0;
			}
    		//Log.v("MyMessage", "waiting for the download thread");
    		try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return 0;
			}
    		wait += 1000;
    	}
    	
    	if(loaded==0)
    	{
    		current_challenge = null;
    		
    		String[] status_message = {""};
    		int status_code = 0;
    		try {
    			status_code = downloadService.getStatus(status_message);
    		} catch (RemoteException e) {
    			// TODO Auto-generated catch block
 			   	e.printStackTrace();
 		   	}
    		if(status_code==1)
 		   	{
    			if(status_message[0].equals(""))
    				status_message[0] = "Unable to contact the server";
 		   	}
    		else
    			status_message[0] = "Unable to contact the server";
			set_message("Error!");
			if(error_alert_shown==0)
			{
				show_alert(status_message[0]);
				error_alert_shown += 1;
			}
    		ImageView imView = (ImageView)findViewById(R.id.challenge_image);
 		   	imView.setImageResource(R.drawable.picguess);
 		   	submit_button.setVisibility(4);
 		   	
 		   	return 0;
    	}

        return 1;
    }
    
    
    public int report404(int photo_id)
    {
        HttpClient httpclient = new DefaultHttpClient();  
        HttpPost httppost = new HttpPost(getString(R.string.url_base)+getString(R.string.url_report404));  
        try 
        {  
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair("username", state.stored_username));  
            nameValuePairs.add(new BasicNameValuePair("password", state.stored_password));
            nameValuePairs.add(new BasicNameValuePair("photo_id", ""+photo_id));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
            HttpResponse response = httpclient.execute(httppost);
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
            String msg = json.getString("message");
            if (msg.equals(""))
            {
            	if (success==1)
            		msg = "Thanks for your report!";
            	else
            		msg = "Sorry, failed to submit your report.";
            }
        	show_alert(msg);
        } 
        catch (ClientProtocolException e) {  
            e.printStackTrace();
            return 0;
        } catch (IOException e) {  
            e.printStackTrace();
            return 0;
        } catch (JSONException e) {
			e.printStackTrace();
			return 0;
		}  
        
        return 1;
    	
    }
    
    private OnClickListener buttonListener = new OnClickListener() {
        public void onClick(View v)
        {
        	if (v.getId()==R.id.login)
        	{
        		if (state.is_logged_in==1)
        		{
        			state.score = 0;
        			state.is_logged_in = 0;
        			state.stored_username = "";
        			state.stored_password = "";
        			try {
						downloadService.setCredentials(state.stored_username, state.stored_password);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
            		set_userinfo(0);
        		}
        		else
        		{
            		Intent intent = new Intent(mycontext, picguess.android.Authentication.class);
            		startActivityForResult(intent, 1);        			
        		}
        	}
        	else if(v.getId()==R.id.score_sync)
        	{
        		if (state.is_logged_in==1)
        		{
        			set_userinfo(1);
        		}
        	}
        	else if (v.getId()==R.id.exit)
        	{
        		stopService(new Intent(mycontext, picguess.android.DownloadService.class));
        		finish();
        	}
        }
    };
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	if (requestCode==1 || requestCode==2) //authentication or signup
    	{
    		if (resultCode==RESULT_OK)
    		{
    			state.stored_username = data.getStringExtra("username");
    			state.stored_password = data.getStringExtra("password");
    			state.score = data.getIntExtra("score", 0);
    			state.is_logged_in = 1;
    			try {
					downloadService.setCredentials(state.stored_username, state.stored_password);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			set_userinfo(0);
    		}
    	}
    	else if(requestCode==3) //feedback
    	{
       		if (resultCode==RESULT_OK)
    		{
       			show_alert("Thanks for your feedback.");
    		}
    	}
    	else if(requestCode==4) //Statistics
    	{
    		
    	}
       	else if(requestCode==5) //Settings
    	{
    		if (resultCode==RESULT_OK)
    		{
    			state.stored_username = data.getStringExtra("username");
    			state.stored_password = data.getStringExtra("password");
    			try {
					downloadService.setCredentials(state.stored_username, state.stored_password);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			if(state.stored_username.equals(""))
    			{
    				state.is_logged_in = 0;
    				state.score = 0;
    				set_userinfo(0);
    				show_alert("Account Deleted!");
    			}
    			else
    			{
    				set_userinfo(0);
    				show_alert("Password Changed Successfully");
    			}
    		}	
    	}
    }
}

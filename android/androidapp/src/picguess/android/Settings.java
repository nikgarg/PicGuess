package picguess.android;

import java.io.BufferedReader;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
//import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Settings extends Activity implements OnClickListener
{
    String username;
    String password;
    
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        
        Button settings_submit = (Button)findViewById(R.id.settings_submit);
        settings_submit.setOnClickListener(this);
        Button settings_back = (Button)findViewById(R.id.settings_back);
        settings_back.setOnClickListener(this);
        Button settings_delete_account = (Button)findViewById(R.id.settings_delete_account);
        settings_delete_account.setOnClickListener(this);
        
        Bundle extras = getIntent().getExtras();
        username = extras.getString("username");
        password = extras.getString("password");
        TextView settings_title = (TextView)findViewById(R.id.settings_title);
        settings_title.setText("Account settings for "+username);
    }

	public void onClick(View v) 
	{
		final TextView settings_message = (TextView)findViewById(R.id.settings_message);
		if (v.getId()==R.id.settings_submit)
		{
            EditText settings_password1 = (EditText)findViewById(R.id.settings_password1);
            String p1 = settings_password1.getText().toString();
            EditText settings_password2 = (EditText)findViewById(R.id.settings_password2);
            String p2 = settings_password2.getText().toString();

            if(p1.equals("") || p2.equals(""))
            {
            	settings_message.setText("Please enter the password");
            	return; 	
            }
            else if (!p1.equals(p2))
            {
                settings_message.setText("The two passwords do not match! Please try again");
                return;
            }
            
			boolean settings_success = false;
			String failure_reason = "";
            HttpClient httpclient = new DefaultHttpClient();  
            HttpPost httppost = new HttpPost(getString(R.string.url_base)+getString(R.string.url_change_password));  
            try 
            {  
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("username", username));  
                nameValuePairs.add(new BasicNameValuePair("password", password));
                nameValuePairs.add(new BasicNameValuePair("newpassword", p1));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();
                InputStream inputStream = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line="", result="";
                while ((line = reader.readLine()) != null) {
                	result += line + '\n';
                }
                //Log.v("MyMessage", "server resposne="+result+'\n');
                JSONObject json = new JSONObject(result);
                int success = json.getInt("success");
                if (success==1)
                {
                    settings_message.setText("");
                	Intent info = new Intent();
                	info.putExtra("username", username);
                	info.putExtra("password", p1);
                	setResult(RESULT_OK, info);
                	settings_success = true;
                	finish();
                }
                else
                {
                    failure_reason = json.getString("message");
                	if (failure_reason.equals(""))
                		failure_reason = "Sorry, could not update your password.";
            		settings_message.setText(failure_reason);
                }
            } 
            catch (ClientProtocolException e) {  
                e.printStackTrace();
            } catch (IOException e) {  
            	settings_message.setText("Unable to contact the server.");
                e.printStackTrace();  
            } catch (JSONException e) {
				e.printStackTrace();
			}  
		}
		else if (v.getId()==R.id.settings_delete_account)
		{
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setMessage("Are you sure?");
        	builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
 	           public void onClick(DialogInterface dialog, int id) {
 	  			boolean settings_success = false;
 				String failure_reason = "";
 	            HttpClient httpclient = new DefaultHttpClient();  
 	            HttpPost httppost = new HttpPost(getString(R.string.url_base)+getString(R.string.url_delete_account));  
 	            try 
 	            {  
 	                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
 	                nameValuePairs.add(new BasicNameValuePair("username", username));  
 	                nameValuePairs.add(new BasicNameValuePair("password", password));
 	                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
 	                HttpResponse response = httpclient.execute(httppost);
 	                HttpEntity entity = response.getEntity();
 	                InputStream inputStream = entity.getContent();
 	                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
 	                String line="", result="";
 	                while ((line = reader.readLine()) != null) {
 	                	result += line + '\n';
 	                }
 	                //Log.v("MyMessage", "server resposne="+result+'\n');
 	                JSONObject json = new JSONObject(result);
 	                int success = json.getInt("success");
 	                if (success==1)
 	                {
 	                    settings_message.setText("");
 	                	Intent info = new Intent();
 	                	info.putExtra("username", "");
 	                	info.putExtra("password", "");
 	                	setResult(RESULT_OK, info);
 	                	settings_success = true;
 	                	finish();
 	                }
 	                else
 	                {
 	                    failure_reason = json.getString("message");
 	                	if (failure_reason.equals(""))
 	                		failure_reason = "Sorry, could not delete your account.";
 	            		settings_message.setText(failure_reason);
 	                }
 	            } 
 	            catch (ClientProtocolException e) {  
 	                e.printStackTrace();
 	            } catch (IOException e) {  
 	            	settings_message.setText("Unable to contact the server.");
 	                e.printStackTrace();  
 	            } catch (JSONException e) {
 					e.printStackTrace();
 				}  
	                
	           }
	        });
        	builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
  	           public void onClick(DialogInterface dialog, int id) {
 	                dialog.cancel();
 	           }
 	        });
        	AlertDialog alert = builder.create();
        	alert.show();
		}
		else if (v.getId()==R.id.settings_back)
		{
		    setResult(RESULT_CANCELED);
		    finish();
		}
	}
}

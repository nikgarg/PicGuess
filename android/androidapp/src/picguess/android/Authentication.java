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
import android.content.Intent;
import android.os.Bundle;
//import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Authentication extends Activity implements OnClickListener
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        Button login_submit = (Button)findViewById(R.id.login_submit);
        login_submit.setOnClickListener(this);
        Button login_cancel = (Button)findViewById(R.id.login_cancel);
        login_cancel.setOnClickListener(this);
        Button login_signup = (Button)findViewById(R.id.login_signup);
        login_signup.setOnClickListener(this);
    }

	public void onClick(View v) 
	{
		if (v.getId()==R.id.login_submit)
		{
            TextView login_message = (TextView)findViewById(R.id.login_message);
            EditText login_username = (EditText)findViewById(R.id.login_username);
            String u = login_username.getText().toString();
            EditText login_password = (EditText)findViewById(R.id.login_password);
            String p = login_password.getText().toString();
            if (u.equals(""))
            {
            	login_message.setText("Please enter the username");
            	return;
            }
            else if(p.equals(""))
            {
            	login_message.setText("Please enter the password");
            	return; 	
            }
            
            login_message.setText("Signing In...");
			boolean login_success = false;
            HttpClient httpclient = new DefaultHttpClient();  
            HttpPost httppost = new HttpPost(getString(R.string.url_base)+getString(R.string.url_login));  
            try 
            {  
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("username", u));  
                nameValuePairs.add(new BasicNameValuePair("password", p));  
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
                    login_message.setText("");
                	Intent info = new Intent();
                	info.putExtra("username", u);
                	info.putExtra("password", p);
                	info.putExtra("score", json.getInt("score"));
                	setResult(RESULT_OK, info);
                	login_success = true;
                	finish();
                }
                else
                {
                    login_message.setText("Login Failed");                	
                }
            } 
            catch (ClientProtocolException e) {  
                e.printStackTrace();
            } catch (IOException e) {  
                e.printStackTrace(); 
                login_message.setText("Unable to contact the server");
            } catch (JSONException e) {
				e.printStackTrace();
			}  
            /*
            if (!login_success)
            {
                login_message.setText("Login Failed");
            }
            */
		}
		else if (v.getId()==R.id.login_cancel)
		{
		    setResult(RESULT_CANCELED);
		    finish();
		}
		else if (v.getId()==R.id.login_signup)
		{
    		Intent intent = new Intent(this.getBaseContext(), picguess.android.Signup.class);
    		startActivityForResult(intent, 2);        			
		}
	}
	
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	if (resultCode==RESULT_OK)
    	{
    		/*
        	Intent info = new Intent();
    		String u = data.getStringExtra("username");
    		String p = data.getStringExtra("password");    		
    		int score = data.getIntExtra("score", 0);
        	info.putExtra("username", u);
        	info.putExtra("password", p);
        	info.putExtra("score", score);
        	setResult(RESULT_OK, info);
        	*/
    		setResult(RESULT_OK, data);
        	finish();

    	}
    }
}

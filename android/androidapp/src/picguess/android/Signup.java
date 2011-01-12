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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Signup extends Activity implements OnClickListener
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);
        
        Button signup_submit = (Button)findViewById(R.id.signup_submit);
        signup_submit.setOnClickListener(this);
        Button signup_cancel = (Button)findViewById(R.id.signup_cancel);
        signup_cancel.setOnClickListener(this);
    }

	public void onClick(View v) 
	{
		if (v.getId()==R.id.signup_submit)
		{
            TextView signup_message = (TextView)findViewById(R.id.signup_message);
			EditText signup_username = (EditText)findViewById(R.id.signup_username);
            String u = signup_username.getText().toString();
            EditText signup_password1 = (EditText)findViewById(R.id.signup_password1);
            String p1 = signup_password1.getText().toString();
            EditText signup_password2 = (EditText)findViewById(R.id.signup_password2);
            String p2 = signup_password2.getText().toString();

            if (u.equals(""))
            {
            	signup_message.setText("Please enter the username");
            	return;
            }
            else if(p1.equals("") || p2.equals(""))
            {
            	signup_message.setText("Please enter the password");
            	return; 	
            }
            else if (!p1.equals(p2))
            {
                signup_message.setText("The two passwords do not match! Please try again");
                return;
            }
            
            signup_message.setText("Signing Up...");
			boolean signup_success = false;
			String failure_reason = "";
            HttpClient httpclient = new DefaultHttpClient();  
            HttpPost httppost = new HttpPost(getString(R.string.url_base)+getString(R.string.url_register));  
            try 
            {  
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("username", u));  
                nameValuePairs.add(new BasicNameValuePair("password", p1));  
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
                    signup_message.setText("");
                	Intent info = new Intent();
                	info.putExtra("username", u);
                	info.putExtra("password", p1);
                	info.putExtra("score", json.getInt("score"));
                	setResult(RESULT_OK, info);
                	signup_success = true;
                	finish();
                }
                else
                {
                    failure_reason = json.getString("message");
                	if (failure_reason.equals(""))
                		failure_reason = "Sign Up Failed.";
            		signup_message.setText(failure_reason);
                }
            } 
            catch (ClientProtocolException e) {  
                e.printStackTrace();
            } catch (IOException e) {  
            	signup_message.setText("Unable to contact the server.");
                e.printStackTrace();  
            } catch (JSONException e) {
				e.printStackTrace();
			}  
            /*
            if (!signup_success)
            {
            	if (!failure_reason.equals(""))
            		signup_message.setText(failure_reason);
            	else
                    signup_message.setText("Sign Up Failed");
            }
            */
		}
		else if (v.getId()==R.id.signup_cancel)
		{
		    setResult(RESULT_CANCELED);
		    finish();
		}
	}
}

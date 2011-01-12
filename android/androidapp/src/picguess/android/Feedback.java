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

public class Feedback extends Activity implements OnClickListener
{
	String username = "";
	String password = "";
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback);
        
        Button feedback_submit = (Button)findViewById(R.id.feedback_submit);
        feedback_submit.setOnClickListener(this);
        Button feedback_cancel = (Button)findViewById(R.id.feedback_cancel);
        feedback_cancel.setOnClickListener(this);
        
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
        	int is_logged_in = extras.getInt("is_logged_in");
        	if(is_logged_in==1)
        	{
        		username = extras.getString("username");
        		password = extras.getString("password");
        	}
        }
    }

	public void onClick(View v) 
	{
		if (v.getId()==R.id.feedback_submit)
		{
            TextView feedback_message = (TextView)findViewById(R.id.feedback_message);
			EditText feedback_content = (EditText)findViewById(R.id.feedback_content);
			EditText feedback_email = (EditText)findViewById(R.id.feedback_email);
			String comments = feedback_content.getText().toString();
			String email = feedback_email.getText().toString();
            if (comments.equals(""))
            {
            	feedback_message.setText("Please enter your comments");
            	return;
            }
            
			boolean feedback_success = false;
			String failure_reason = "";
            HttpClient httpclient = new DefaultHttpClient();  
            HttpPost httppost = new HttpPost(getString(R.string.url_base)+getString(R.string.url_feedback));  
            try 
            {  
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        		nameValuePairs.add(new BasicNameValuePair("username", username));
        		nameValuePairs.add(new BasicNameValuePair("password", password));  
                nameValuePairs.add(new BasicNameValuePair("comments", comments));
                nameValuePairs.add(new BasicNameValuePair("email", email));
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
                if (success==1)
                {
                    feedback_message.setText("");
                	setResult(RESULT_OK);
                	feedback_success = true;
                	finish();
                }
                else
                {
                    failure_reason = json.getString("message");
                	if (failure_reason.equals(""))
                		failure_reason = "Sorry, could not submit your feedback";
            		feedback_message.setText(failure_reason);
                }
            } 
            catch (ClientProtocolException e) {  
                e.printStackTrace();
            } catch (IOException e) {  
            	feedback_message.setText("Unable to contact the server.");
                e.printStackTrace();  
            } catch (JSONException e) {
				e.printStackTrace();
			}  
		}
		else if (v.getId()==R.id.feedback_cancel)
		{
		    setResult(RESULT_CANCELED);
		    finish();
		}
	}
}

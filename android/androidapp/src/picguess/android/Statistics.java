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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
//import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class Statistics extends Activity implements OnClickListener
{
	String username = "";
	String password = "";
	int is_logged_in = 0;
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statistics);
        
        TextView statistics_title = (TextView)findViewById(R.id.statistics_title);
        statistics_title.setText("Rankings");
        
        Button statistics_close = (Button)findViewById(R.id.statistics_close);
        statistics_close.setOnClickListener(this);
        
        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
        	is_logged_in = extras.getInt("is_logged_in");
        	if(is_logged_in==1)
        	{
        		username = extras.getString("username");
        		password = extras.getString("password");
        	}
        }
        
        showRankings();
    }

    
    public int showRankings()
    {
    	JSONArray rankings = null;
    	int user_score = 0;
    	int user_rank = 0;
    	String message = "";
        HttpClient httpclient = new DefaultHttpClient();  
        HttpPost httppost = new HttpPost(getString(R.string.url_base)+getString(R.string.url_rankings));  
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
            message = json.getString("message");
            if (success==0)
            {
            	if (message.equals(""))
            		message = "Unable to get Rankings from the server";
               	AlertDialog.Builder builder = new AlertDialog.Builder(this);
            	builder.setMessage(message);
            	builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                   }
                });
            	AlertDialog alert = builder.create();
            	alert.show();
            	return 0;
            }
            rankings = json.getJSONArray("rankings");
            user_score = json.getInt("user_score");
            user_rank = json.getInt("user_rank");
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
        
        Context mycontext = this.getBaseContext();
        TableLayout rankings_table = new TableLayout(mycontext);
        rankings_table.setStretchAllColumns(true);
        TableRow row = new TableRow(mycontext);
        TextView col1 = new TextView(mycontext);
        col1.setText("Rank");
        row.addView(col1);
        TextView col2 = new TextView(mycontext);
        col2.setText("Username");
        row.addView(col2);
        TextView col3 = new TextView(mycontext);
        col3.setText("Score");
        row.addView(col3);
        rankings_table.addView(row);

    	JSONObject stats;
    	try
    	{
    		for(int i=0;i<rankings.length();i++)
    		{
    			stats = rankings.getJSONObject(i);
	        	row = new TableRow(mycontext);
	        	if(i%2==0)
	        		row.setBackgroundColor(0xffcdc9c9);
	        	else
	        		row.setBackgroundColor(0xffcdc0b0);
	        	col1 = new TextView(mycontext);
	        	col1.setText(""+stats.getInt("rank"));
	        	col1.setTextColor(0xff000000);
	        	col2 = new TextView(mycontext);
	        	col2.setText(stats.getString("username"));
	        	col2.setTextColor(0xff000000);
	        	col3 = new TextView(mycontext);
	        	col3.setText(""+stats.getInt("score"));
	        	col3.setTextColor(0xff000000);
	        	row.addView(col1);
	        	row.addView(col2);
	        	row.addView(col3);
	        	rankings_table.addView(row);
    		}
    		row = new TableRow(mycontext);
    		col1 = new TextView(mycontext);
    		col1.setText("Your Ranking");
    		row.addView(col1);
    		rankings_table.addView(row);
    		row = new TableRow(mycontext);
    		row.setBackgroundColor(0xffb0c4de);
    		if (is_logged_in==1)
    		{
    			col1 = new TextView(mycontext);
    			col1.setText(""+user_rank);
    			col1.setTextColor(0xff000000);
    			col2 = new TextView(mycontext);
    			col2.setText(username);
    			col2.setTextColor(0xff000000);
    			col3 = new TextView(mycontext);
    			col3.setText(""+user_score);
    			col3.setTextColor(0xff000000);
	        	row.addView(col1);
	        	row.addView(col2);
	        	row.addView(col3);
    		}
    		else
    		{
    			col1 = new TextView(mycontext);
    			col1.setText("Requires Login");
    			col1.setTextColor(0xff000000);
    			row.addView(col1);
    		}
    		rankings_table.addView(row);
    		
		} 
    	catch (JSONException e) {
			e.printStackTrace();
			return 0;
		}
    	
        RelativeLayout statistics_content = (RelativeLayout)findViewById(R.id.statistics_content);
        statistics_content.addView(rankings_table);
        
        TextView statistics_description = (TextView)findViewById(R.id.statistics_description);
        statistics_description.setText(message);

        return 1;
    }
    
	public void onClick(View v) 
	{
		if (v.getId()==R.id.statistics_close)
		{
		    setResult(RESULT_CANCELED);
		    finish();
		}
	}
}

package picguess.android;

import android.os.Parcel;
import android.os.Parcelable;

public final class Challenge implements Parcelable
{
	int photo_id;
	String photo_owner;
	String photo_site;
	String photo_url;
	String[] options = new String[4];
	int correct_option;
	int answered_correctly;
	int choice_selected;
	int already_played;
	String filename;

    public static final Parcelable.Creator<Challenge> CREATOR = new Parcelable.Creator<Challenge>() {
        public Challenge createFromParcel(Parcel in) {
            return new Challenge(in);
        }

        public Challenge[] newArray(int size) {
            return new Challenge[size];
        }
    };
    
    public Challenge()
    {	
    }
    
    public Challenge(Parcel in)
    {
    	readFromParcel(in);
    }
	
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) 
	{
		out.writeInt(photo_id);
		out.writeString(photo_owner);
		out.writeString(photo_site);
		out.writeString(photo_url);
		for(int i=0;i<4;i++)
			out.writeString(options[i]);
		out.writeInt(correct_option);
		out.writeInt(answered_correctly);
		out.writeInt(choice_selected);
		out.writeInt(already_played);
		out.writeString(filename);
	}
	
	public void readFromParcel(Parcel in) 
	{
        photo_id = in.readInt();
        photo_owner = in.readString();
        photo_site = in.readString();
        photo_url = in.readString();
        for(int i=0;i<4;i++)
        	options[i] = in.readString();
        correct_option = in.readInt();
        answered_correctly = in.readInt();
        choice_selected = in.readInt();
        already_played = in.readInt();
        filename = in.readString();
    }
}
package com.dave.viewd;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.dave.viewd.R;

/**
 * Created by davidcasey on 2/27/15.
 */
@ParseClassName("Post")
public class Post extends ParseObject {

    public ParseGeoPoint gp() {
        return getParseGeoPoint("Location");
    }

    public ParseFile pf() {
        return getParseFile("Image");
    }

    public static ParseQuery<Post> getQuery() {
        return ParseQuery.getQuery(Post.class);
    }

    public void setRating(float f) {
        int q = (int) f;
        put("Rating",f);
    }

    public int getRating() {
        return getInt("Rating");
    }

    public void setNumberOfVotes(int num) {
        put("NumOfVotes", num);
    }

    public int getNumberOfVotes() {
        return getInt("NumOfVotes");
    }


    public int getTheNumberOfTimesReported() {return getInt("reported");}

}

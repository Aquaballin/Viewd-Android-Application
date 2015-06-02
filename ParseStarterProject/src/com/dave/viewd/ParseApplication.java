package com.dave.viewd;
/******************************************************************************
 * @author David Casey
 ******************************************************************************/

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseCrashReporting;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.dave.viewd.R;

public class ParseApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    // Initialize Crash Reporting.
    ParseCrashReporting.enable(this);
    // Enable Local Datastore.
    Parse.enableLocalDatastore(this);
    ParseObject.registerSubclass(Post.class);
    // Add your initialization code here
    Parse.initialize(this, "MgKecPMy0z7ceQnUBWltpc5Lfv2xyL1LOOCyZ7T2", "nyaxNPz2kkHKFXTWvmJSegZeRGEdey44wjzjvorb");
    ParseUser.enableAutomaticUser();
    ParseUser.getCurrentUser().saveInBackground();
    ParseACL defaultACL = new ParseACL();
    defaultACL.setPublicReadAccess(true);
    defaultACL.setReadAccess(ParseUser.getCurrentUser(),true);
    defaultACL.setPublicWriteAccess(true);
    // Optionally enable public read access.
    ParseACL.setDefaultACL(defaultACL, true);
  }
}

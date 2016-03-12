package com.labsgn.githubnotifier.utlis;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by rhony on 01/03/16.
 */
public class Logger {

    public static void log_i(String className, String message) {
        Log.i("TAG", " class : "+className+" \nMessage : "+message);
    }

    public static void log_e(String className, String message) {
        Log.e("TAG", " class : "+className+" \nError : "+message);
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, " "+message , Toast.LENGTH_SHORT).show();
    }


}

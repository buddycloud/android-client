package com.buddycloud.android.buddydroid;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

public class LogCollector {

    public static void giveFeedback(final Context ctx, final String additionalText) {
        final PackageManager packageManager = ctx.getPackageManager();
        final Intent intent = new Intent("com.xtralogic.logcollector.intent.action.SEND_LOG");
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        final boolean isInstalled = list.size() > 0;

        if (isInstalled){
               intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               intent.putExtra("com.xtralogic.logcollector.intent.extra.SEND_INTENT_ACTION", Intent.ACTION_SENDTO);
               intent.putExtra("com.xtralogic.logcollector.intent.extra.DATA", Uri.parse("mailto:androiddebug@buddycloud.com"));
               intent.putExtra("com.xtralogic.logcollector.intent.extra.ADDITIONAL_INFO", "additional infor goes here..\n");
               intent.putExtra(Intent.EXTRA_SUBJECT, "feedback Buddydroid");

               intent.putExtra("com.xtralogic.logcollector.intent.extra.FORMAT", "time");

               //The log can be filtered to contain data relevant only to your app
               /*String[] filterSpecs = new String[3];
               filterSpecs[0] = "AndroidRuntime:E";
               filterSpecs[1] = TAG + ":V";
               filterSpecs[2] = "*:S";
               intent.putExtra(EXTRA_FILTER_SPECS, filterSpecs);*/

               ctx.startActivity(intent);
        } else {
            new AlertDialog.Builder(ctx)
            .setTitle(ctx.getString(R.string.app_name))
            .setIcon(android.R.drawable.ic_dialog_info)
            .setMessage("Install the free and open source Log Collector application to collect the device log and send it to the developer.")
            .setPositiveButton("Install", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int whichButton){
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:com.xtralogic.android.logcollector"));
                    marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(marketIntent); 
                }
            })
            .setNegativeButton("Just Mail", new Dialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:android-debug@buddycloud.com"));
                    intent.putExtra(Intent.EXTRA_SUBJECT, "feedback "+ctx.getString(R.string.app_name));
                    intent.putExtra(Intent.EXTRA_TEXT, additionalText);
                    ctx.startActivity(intent); 
                }}
            )
            .show();
        }
    }
    
}

package com.buddycloud.android.buddydroid;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.buddycloud.android.buddydroid.provider.BuddycloudProvider;
import com.buddycloud.android.buddydroid.provider.RosterHelper;

public class BCNotifications {

    private static long unread = 0;
    private static long replies = 0;
    private static NotificationManager notificationManager = null;

    private static long lastSound = 0;

    private final static int NOTIFICATION = 1;

    public static synchronized void updateNotification(
        BuddycloudProvider provider
    ) {
        Log.d(BuddycloudProvider.TAG, "update notification");

        if (notificationManager == null) {
            notificationManager = (NotificationManager) provider.getContext()
                            .getSystemService(Context.NOTIFICATION_SERVICE);
        }

        long[] counts = RosterHelper.getUnreadCounts(provider);

        Notification notification = new Notification();

        notification.defaults &= ~Notification.DEFAULT_SOUND;

        long time = System.currentTimeMillis();

        if (counts[0] > replies) {
            if (time - lastSound > 2000) {
                notification.sound = Uri.parse(
                        "android.resource://com.buddycloud.android.buddydroid/"
                        + R.raw.dr
                );
                lastSound = time;
            }
            notification.tickerText = "New reply!";
        } else
        if (counts[1] > unread) {
            if (time - lastSound > 2000) {
                notification.sound = Uri.parse(
                        "android.resource://com.buddycloud.android.buddydroid/"
                        + R.raw.cp
                );
                lastSound = time;
            }
            notification.tickerText = "New channel post!";
        } else
        if (counts[0] == replies && counts[1] == unread) {
            return;
        } else {
            // decrease, cancel notifications

            replies = counts[0];
            unread = counts[1];

            Log.d(BuddycloudProvider.TAG, "clear notification");
            notificationManager.cancel(NOTIFICATION);
            return;
        }

        replies = counts[0];
        unread = counts[1];

        if (replies == 0 && unread == 0) {
            // clear notification
            Log.d(BuddycloudProvider.TAG, "clear notification");
            notificationManager.cancel(NOTIFICATION);
            return;
        }

        if (unread < 100 && unread > 1) {
            notification.number = (int)unread;
        } else {
            notification.number = 0;
        }

        Intent roster = new Intent(provider.getContext(), MainActivity.class);
        PendingIntent intent = PendingIntent.getActivity(
                provider.getContext(),
                0,
                roster,
                Intent.FLAG_ACTIVITY_NEW_TASK
        );

        String text = "";
        if (replies > 0) {
            if (replies == 1) {
                text = "1 reply";
            } else {
                text = replies + " replies";
            }
            if (unread > replies) {
                long delta = unread - replies;
                if (delta == 1) {
                    text += ", 1 message.";
                } else {
                    text += ", " + delta + " new messages.";
                }
            }
        } else
        if (unread == 1) {
            text = "1 message.";
        } else {
            text = unread + " messages.";
        }

        notification.icon = R.drawable.icon_statusbar;

        notification.setLatestEventInfo(
            provider.getContext(),
            notification.tickerText,
            text,
            intent
        );

        notification.flags = Notification.FLAG_AUTO_CANCEL;

        if (time - lastSound > 2000) {
            notificationManager.cancel(NOTIFICATION);
        }

        notification.when = System.currentTimeMillis() + 500;

        notificationManager.notify(NOTIFICATION, notification);
    }

}

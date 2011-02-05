package com.buddycloud.util;

public class HumanTime {

    /**
     * A (propably bad) implementation for human readable relative times.
     * @param ms
     * @return
     */
    public final static String humanReadableString(long ms) {

        if (ms < 0) {
            return "somewhere in the future";
        }

        long seconds = ms / 1000;

        if (seconds < 60) {
            return "seconds ago";
        }

        long minutes = seconds / 60;

        if (minutes < 60) {
            if (minutes < 2) {
                return "a minute ago";
            }
            if (minutes > 5) {
                minutes = (minutes / 5) * 5;
            }
            if (minutes > 30) {
                minutes = (minutes / 15) * 15;
            }
            if (minutes == 30) {
                return "half an hour ago";
            }
            return minutes + " minutes ago";
        }

        long hours = minutes / 60;

        if (hours < 24) {
            if (hours < 2) {
                return "an hour ago";
            }
            if (hours == 12) {
                return "half a day ago";
            }
            return hours + " hours ago";
        }

        long days = hours / 24;

        if (days == 1) {
            return "yesterday";
        }

        if (days == 1) {
            return "yesterday";
        }

        if (days < 7) {
            return days + " days ago";
        }

        if (days < 31) {

            long weeks = days / 7;

            if (weeks < 2) {
                return "a week ago";
            }

            return weeks + " weeks ago";
        }

        if (days > 365) {
            long years = days / 365;
            if (years < 2) {
                return "a year ago";
            }
            return "years ago";
        }

        long months = days / 31;

        if (months < 2) {
            return "a month ago";
        }

        if (months == 6) {
            return "half a year ago";
        }

        return months + " months ago";
    }

}

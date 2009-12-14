package com.buddycloud.android.buddydroid;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TaskQueueThread extends Thread {

    private ArrayBlockingQueue<Runnable> taskQueue
                = new ArrayBlockingQueue<Runnable>(5);

    public void run() {
        while (taskQueue != null) {
            try {
                Runnable runnable = taskQueue.poll(60, TimeUnit.SECONDS);
                if (runnable != null) {
                    runnable.run();
                }
            } catch (Exception e) {
            }
        }
    }

    public void stopQueue() {
        taskQueue = null;
    }

    public boolean add(Runnable run) {
        try {
            return taskQueue.offer(run, 10, TimeUnit.SECONDS);
        } catch (Exception e) {}
        return false;
    }
}

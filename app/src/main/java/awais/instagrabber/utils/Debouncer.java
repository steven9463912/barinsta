package awais.instagrabber.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// TODO replace with kotlinx-coroutines debounce
public class Debouncer<T> {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ConcurrentHashMap<T, TimerTask> delayedMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<T, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();
    private final Callback<T> callback;
    private final int interval;

    public Debouncer(final Callback<T> c, final int interval) {
        callback = c;
        this.interval = interval;
    }

    public void call(final T key) {
        final TimerTask task = new TimerTask(key);

        TimerTask prev;
        do {
            prev = this.delayedMap.putIfAbsent(key, task);
            if (prev == null) {
                ScheduledFuture<?> future = this.scheduler.schedule(task, this.interval, TimeUnit.MILLISECONDS);
                this.futureMap.put(key, future);
            }
        } while (prev != null && !prev.extend()); // Exit only if new task was added to map, or existing task was extended successfully
    }

    public void terminate() {
        this.scheduler.shutdownNow();
    }

    public void cancel(T key) {
        this.delayedMap.remove(key);
        ScheduledFuture<?> future = this.futureMap.get(key);
        if (future != null) {
            future.cancel(true);
        }
    }

    // The task that wakes up when the wait time elapses
    private class TimerTask implements Runnable {
        private final T key;
        private long dueTime;
        private final Object lock = new Object();

        public TimerTask(final T key) {
            this.key = key;
            this.extend();
        }

        public boolean extend() {
            synchronized (this.lock) {
                if (this.dueTime < 0) // Task has been shutdown
                    return false;
                this.dueTime = System.currentTimeMillis() + Debouncer.this.interval;
                return true;
            }
        }

        public void run() {
            synchronized (this.lock) {
                final long remaining = this.dueTime - System.currentTimeMillis();
                if (remaining > 0) { // Re-schedule task
                    Debouncer.this.scheduler.schedule(this, remaining, TimeUnit.MILLISECONDS);
                } else { // Mark as terminated and invoke callback
                    this.dueTime = -1;
                    try {
                        Debouncer.this.callback.call(this.key);
                    } catch (final Exception e) {
                        Debouncer.this.callback.onError(e);
                    } finally {
                        Debouncer.this.delayedMap.remove(this.key);
                    }
                }
            }
        }
    }

    public interface Callback<T> {
        void call(T key);

        void onError(Throwable t);
    }
}

package my.netty.rpc.parallel;

public abstract class AbstractDaemonThread implements Runnable {

    protected final Thread thread;
    private static final long JOIN_TIME = 90 * 1000L;
    protected volatile boolean hasNotified = false;
    protected volatile boolean stoped = false;

    public AbstractDaemonThread() {
        thread = new Thread(this, getDaemonThreadName());
    }

    public abstract String getDaemonThreadName();

    public void start() {
        thread.start();
    }

    public void shutdown() {
        shutdown(false);
    }

    public void stop() {
        stop(false);
    }

    public void makeStop() {
        stoped = true;
    }

    public void stop(final boolean interrupt) {
        stoped = true;
        synchronized (this) {
            if(!hasNotified) {
                hasNotified = true;
                notify();
            }
        }

        if(interrupt) {
            thread.interrupt();
        }
    }

    public void shutdown(final boolean interrupt) {
        stoped = true;
        synchronized (this) {
            if(!hasNotified) {
                hasNotified = true;
                notify();
            }
        }

        try {
            if(interrupt) {
                thread.interrupt();
            }

            if(!thread.isDaemon()) {
                thread.join(getJoinTime());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void wakeup() {
        synchronized (this) {
            if(!hasNotified) {
                hasNotified = true;
                notify();
            }
        }
    }

    protected void waitForRunning(long interval) {
        synchronized (this) {
            if(hasNotified) {
                hasNotified = false;
                onWaitEnd();
                return ;
            }

            try {
                wait(interval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                hasNotified = false;
                onWaitEnd();
            }
        }
    }

    protected void onWaitEnd() {
    }

    public boolean isStoped() {
        return stoped;
    }

    public long getJoinTime() {
        return JOIN_TIME;
    }
}

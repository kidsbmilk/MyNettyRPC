package my.netty.rpc.parallel;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class SemaphoreWrapper {

    protected final AtomicBoolean released = new AtomicBoolean(false);
    protected Semaphore semaphore;

    public SemaphoreWrapper() {
        this(1);
    }

    public SemaphoreWrapper(int permits) {
        this(new Semaphore(permits));
    }

    public SemaphoreWrapper(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    public void release() {
        if(semaphore != null) {
            if(released.compareAndSet(false, true)) {
                semaphore.release();
            }
        }
    }

    public void acquire() {
        if(semaphore != null) {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    public boolean isReleased() {
        return released.get();
    }
}

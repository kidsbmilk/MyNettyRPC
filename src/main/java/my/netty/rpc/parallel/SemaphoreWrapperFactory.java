package my.netty.rpc.parallel;

public class SemaphoreWrapperFactory extends SemaphoreWrapper {

    private static final SemaphoreWrapperFactory INSTANCE = new SemaphoreWrapperFactory();

    public static SemaphoreWrapperFactory getInstance() {
        return INSTANCE;
    }

    private SemaphoreWrapperFactory() {
        super();
    }

    @Override
    public void acquire() {
        if(this.semaphore != null) {
            try {
                while(true) {
                    boolean result = released.get();
                    if(released.compareAndSet(result, true)) {
                        semaphore.acquire();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void release() {
        if(getSemaphore() != null) {
            while(true) {
                boolean result = released.get();
                if(released.compareAndSet(result, false)) {
                    semaphore.release();
                    break;
                }
            }
        }
    }
}

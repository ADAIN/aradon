package net.ion.nradon.netty;

public abstract class CatchingRunnable implements Runnable {
    private final Thread.UncaughtExceptionHandler exceptionHandler;

    public CatchingRunnable(Thread.UncaughtExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public void run() {
        try {
            go();
        } catch (Throwable t) {
            exceptionHandler.uncaughtException(Thread.currentThread(), t);
        }
    }

    protected abstract void go() throws Throwable;
}
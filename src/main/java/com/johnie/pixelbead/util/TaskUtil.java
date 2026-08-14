package com.johnie.pixelbead.util;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;

/**
 * Factory helpers that remove the anonymous {@link Task} boilerplate.
 *
 * @author johnie
 * @version 3.0.0
 * @since 2026/08/11
 */
public final class TaskUtil {

    private TaskUtil() {
    }

    /**
     * Creates a background task without a result (export, I/O...).
     */
    public static Task<Void> run(ThrowableRunnable action) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                action.run();
                return null;
            }
        };
    }

    /**
     * Creates a background task that produces a result.
     */
    public static <T> Task<T> call(Callable<T> action) {
        return new Task<>() {
            @Override
            protected T call() throws Exception {
                return action.call();
            }
        };
    }

    @FunctionalInterface
    public interface ThrowableRunnable {
        void run() throws Exception;
    }
}

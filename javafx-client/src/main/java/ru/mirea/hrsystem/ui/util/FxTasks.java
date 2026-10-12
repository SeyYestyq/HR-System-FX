package ru.mirea.hrsystem.ui.util;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Фоновая задача: {@code call()} идёт в daemon-потоке,
 * колбэки успеха и ошибки возвращаются в UI-поток.
 */
public final class FxTasks {
    private FxTasks() {
    }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onFailure.accept(task.getException()));
        Thread thread = new Thread(task, "fx-background");
        thread.setDaemon(true);
        thread.start();
    }
}

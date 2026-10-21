package ru.mirea.hrsystem.ui.util;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class FxTasks {

    private FxTasks() {}

    public static <T> void run(Callable<T> callable, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return callable.call();
            }
        };

        task.setOnSucceeded(event -> {
            if (onSuccess != null) {
                onSuccess.accept(task.getValue());
            }
        });

        task.setOnFailed(event -> {
            if (onFailure != null) {
                onFailure.accept(task.getException());
            }
        });

        Thread thread = new Thread(task, "hr-task-thread-" + System.nanoTime());
        thread.setDaemon(true);
        thread.start();
    }

    public static void run(Runnable runnable, Runnable onSuccess, Consumer<Throwable> onFailure) {
        run(() -> {
            runnable.run();
            return null;
        }, ignored -> {
            if (onSuccess != null) {
                onSuccess.run();
            }
        }, onFailure);
    }
}

package cn.navclub.nes4j.app.concurrent;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class TaskService<T> extends Service<T> {
    private final Task<T> task;

    public TaskService(Task<T> task) {
        this.task = task;
    }

    @Override
    protected Task<T> createTask() {
        return task;
    }

    public static <K> TaskService<K> execute(Task<K> task) {
        var service = new TaskService<>(task);
        service.start();
        return service;
    }
}

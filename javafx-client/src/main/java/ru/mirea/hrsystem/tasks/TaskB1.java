package ru.mirea.hrsystem.tasks;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TaskB1 {
    public static void main(String[] args) {
        Set<String> names = new LinkedHashSet<>(List.of("Эдик", "Максим", "Эдик"));

        for (String name : names) {
            System.out.println(name);
        }
    }
}

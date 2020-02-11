package ru.cft.focus.util.sort;

import java.util.Comparator;
import java.util.List;

// Сортировщик для определенного типа
@FunctionalInterface
public interface Sorter<T> {
    void sort(List<T> list, Comparator<T> comparator);
}

package ru.cft.focus.util.sort;

/*
 * Предполагается, что здесь будет несколько типов сортировок,
 * из которых будет выбираться наиболее подходящая для входных данных.
 * Для частично упорядоченных последовательностей подойдет обычная сортировка вставками.
 * Для последовательностей, об упорядоченности которых нет информации,
 * можно использовать quick sort.
 * */
public final class Sorters {
    private Sorters() {
    }

    public static <T> Sorter<T> insertionSorter() {
        return (list, comparator) -> {
            for (int i = 1; i < list.size(); i++) {
                if (comparator.compare(list.get(i - 1), list.get(i)) > 0) {
                    T replaced = list.remove(i);
                    int insertionIndex = i - 1;
                    while (insertionIndex >= 0 && comparator.compare(list.get(insertionIndex), replaced) > 0)
                        insertionIndex--;
                    list.add(insertionIndex + 1, replaced);
                }
            }
        };
    }
}

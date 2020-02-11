package ru.cft.focus.util.formatter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

// Преобразователь данных из символьного потока к определенному типу и обратно
public interface Formatter<T> {
    T read(BufferedReader reader) throws IOException;

    void write(BufferedWriter writer, T data) throws IOException;
}

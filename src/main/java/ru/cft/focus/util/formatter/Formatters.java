package ru.cft.focus.util.formatter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;

public final class Formatters {
    private Formatters() {
    }

    public static Formatter<String> stringFormatter() {
        return new Formatter<String>() {
            @Override
            public String read(BufferedReader reader) throws IOException {
                String line = reader.readLine();
                if (line != null)
                    return line.trim().isEmpty() ? null : line;
                else
                    throw new EOFException();
            }

            @Override
            public void write(BufferedWriter writer, String data) throws IOException {
                writer.write(data);
                writer.newLine();
            }
        };
    }

    public static Formatter<Long> longFormatter(int radix) {
        return new Formatter<Long>() {
            @Override
            public Long read(BufferedReader reader) throws IOException {
                String line = reader.readLine();
                if (line != null)
                    return Long.parseLong(line, radix);
                else
                    throw new EOFException();
            }

            @Override
            public void write(BufferedWriter writer, Long data) throws IOException {
                writer.write(Long.toString(data, radix));
                writer.newLine();
            }
        };
    }
}

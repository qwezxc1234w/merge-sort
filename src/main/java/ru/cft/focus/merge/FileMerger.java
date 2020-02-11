package ru.cft.focus.merge;

import ru.cft.focus.util.formatter.Formatter;
import ru.cft.focus.util.sort.Sorter;

import java.io.*;
import java.util.*;

/*
* Сортировка простым слиянием
* */
public class FileMerger<T> {
    private static final File FIRST_TEMPORARY_FILE = new File("A.txt");
    private static final File SECOND_TEMPORARY_FILE = new File("B.txt");

    private static final int BASIC_PART_SIZE = 256; // начальный размер серии
    private static final int IO_BUFFER_SIZE = 8192;
    private long currentPartSize;
    private long dataTotalSize;
    private String charset;
    private Sorter<T> sorter;
    private Comparator<T> comparator;
    private Formatter<T> formatter;
    private String outputFilename;
    private List<String> inputFilenames;

    private FileMerger(Builder<T> builder) {
        this.comparator = builder.comparator;
        this.sorter = builder.sorter;
        this.formatter = builder.formatter;
        this.charset = builder.charset;
        this.outputFilename = builder.outputFilename;
        this.inputFilenames = builder.inputFilenames;
        currentPartSize = BASIC_PART_SIZE;
        dataTotalSize = 0;
        FIRST_TEMPORARY_FILE.deleteOnExit();
        SECOND_TEMPORARY_FILE.deleteOnExit();
    }

    public static <K> Builder<K> getBuilder() {
        return new Builder<>();
    }

    /*
    * Сливаем данные в выходной в виде упорядоченных серий,
    * после чего выполняем split-merge, увеличивая размер серии
    * */
    public void merge() throws IOException {
        collect();
        while (currentPartSize < dataTotalSize) {
            splitFile();
            mergeFiles();
            currentPartSize *= 2;
        }
    }

    // слияние файлов A и B в результирующий
    private void mergeFiles() throws IOException {
        try (BufferedReader firstTempFileReader = buildBufferReader(FIRST_TEMPORARY_FILE);
             BufferedReader secondTempFileReader = buildBufferReader(SECOND_TEMPORARY_FILE);
             BufferedWriter writer = buildBufferWriter(new File(outputFilename))
        ) {
            Queue<T> firstFileBuffer = new LinkedList<>();
            Queue<T> secondFileBuffer = new LinkedList<>();
            Queue<T> mergeFileBuffer = new LinkedList<>();

            while (fillCollectionByReader(firstTempFileReader, firstFileBuffer) > 0
                    | fillCollectionByReader(secondTempFileReader, secondFileBuffer) > 0
            ) {
                // контроль конца сливаемых серий
                long firstFileReadDataCounter = BASIC_PART_SIZE;
                long secondFileReadDataCounter = BASIC_PART_SIZE;

                // слияние серий
                while (!firstFileBuffer.isEmpty() || !secondFileBuffer.isEmpty()) {
                    if (mergeFileBuffer.size() < BASIC_PART_SIZE) {
                        if (!firstFileBuffer.isEmpty() && !secondFileBuffer.isEmpty()) {
                            if (comparator.compare(firstFileBuffer.peek(), secondFileBuffer.peek()) < 0)
                                mergeFileBuffer.add(firstFileBuffer.remove());
                            else
                                mergeFileBuffer.add(secondFileBuffer.remove());
                        } else if (!firstFileBuffer.isEmpty())
                            mergeFileBuffer.add(firstFileBuffer.remove());
                        else
                            mergeFileBuffer.add(secondFileBuffer.remove());
                    } else
                        flushCollectionToWriter(writer, mergeFileBuffer);

                    // подкачка данных, если есть
                    if (firstFileBuffer.isEmpty() && firstFileReadDataCounter < currentPartSize) {
                        fillCollectionByReader(firstTempFileReader, firstFileBuffer);
                        firstFileReadDataCounter += BASIC_PART_SIZE;
                    }

                    if (secondFileBuffer.isEmpty() && secondFileReadDataCounter < currentPartSize) {
                        fillCollectionByReader(secondTempFileReader, secondFileBuffer);
                        secondFileReadDataCounter += BASIC_PART_SIZE;
                    }
                }
                if (!mergeFileBuffer.isEmpty())
                    flushCollectionToWriter(writer, mergeFileBuffer);
            }
        }
    }

    /*
    * Упаковка данных из входных в файлов в выходной
    * Данные разбиваются на серии фиксированной длины, предварительно сортируются,
    * На данном этапе отсеиваются невалидные значения
    * */
    private void collect() throws IOException {
        try (BufferedWriter writer = buildBufferWriter(new File(outputFilename))) {
            List<T> list = new ArrayList<>();

            for (String inputFilename : inputFilenames) {
                try (BufferedReader reader = buildBufferReader(new File(inputFilename))) {
                    while (fillCollectionByReader(reader, list) > 0) {
                        if (list.size() >= BASIC_PART_SIZE) {
                            sorter.sort(list, comparator);
                            dataTotalSize += BASIC_PART_SIZE;
                            flushCollectionToWriter(writer, list);
                        }
                    }
                } catch (FileNotFoundException exception) {
                    System.err.println("Couldn't to open/find an input file " + inputFilename);
                }
            }
            if (!list.isEmpty()) {
                sorter.sort(list, comparator);
                dataTotalSize += list.size();
                flushCollectionToWriter(writer, list);
            }
        }
    }

    /*
    * Распределение серий из выходного файла по файлам А и B
    * */
    private void splitFile() throws IOException {
        try (BufferedReader reader = buildBufferReader(new File(outputFilename));
             BufferedWriter writerA = buildBufferWriter(FIRST_TEMPORARY_FILE);
             BufferedWriter writerB = buildBufferWriter(SECOND_TEMPORARY_FILE)
        ) {
            long availableData = currentPartSize;
            BufferedWriter currentWriter = writerA;
            List<T> list = new ArrayList<>();

            while (fillCollectionByReader(reader, list) > 0) {
                flushCollectionToWriter(currentWriter, list);
                availableData -= BASIC_PART_SIZE;
                if (availableData <= 0) {
                    currentWriter = currentWriter == writerA ? writerB : writerA;
                    availableData = currentPartSize;
                }
            }
        }
    }

    private T getDataFromReader(BufferedReader reader) throws IOException {
        T data = null;
        try {
            while (data == null) {
                try {
                    data = formatter.read(reader);
                } catch (NumberFormatException exception) {
                    // ошибка при парсинге целого числа
                }
            }
        } catch (EOFException exception) {
            // достигли конца файла
        }
        return data;
    }

    private void flushCollectionToWriter(BufferedWriter writer, Collection<T> collection) throws IOException {
        for (T data : collection)
            formatter.write(writer, data);
        writer.flush();
        collection.clear();
    }

    // возвращает количество прочитанных данных
    private int fillCollectionByReader(BufferedReader reader, Collection<T> collection) throws IOException {
        T data;
        int readCounter = 0;
        while (collection.size() < BASIC_PART_SIZE && (data = getDataFromReader(reader)) != null) {
            collection.add(data);
            readCounter++;
        }
        return readCounter;
    }

    private BufferedReader buildBufferReader(File file) throws FileNotFoundException, UnsupportedEncodingException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), charset),
                IO_BUFFER_SIZE);
    }

    private BufferedWriter buildBufferWriter(File file) throws FileNotFoundException, UnsupportedEncodingException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset),
                IO_BUFFER_SIZE);
    }

    public static class Builder<U> {
        private String charset;
        private Sorter<U> sorter;
        private Comparator<U> comparator;
        private Formatter<U> formatter;
        private String outputFilename;
        private List<String> inputFilenames;
        private boolean isBuilt = false;

        private Builder() {
        }

        public Builder<U> setComparator(Comparator<U> comparator) {
            requireNonBuilt();
            this.comparator = comparator;
            return this;
        }

        public Builder<U> setSorter(Sorter<U> sorter) {
            requireNonBuilt();
            this.sorter = sorter;
            return this;
        }

        public Builder<U> setFormatter(Formatter<U> formatter) {
            requireNonBuilt();
            this.formatter = formatter;
            return this;
        }

        public Builder<U> setCharset(String charset) {
            requireNonBuilt();
            this.charset = charset;
            return this;
        }

        public Builder<U> setOutputFilename(String outputFilename) {
            requireNonBuilt();
            this.outputFilename = outputFilename;
            return this;
        }

        public Builder<U> setInputFilenames(List<String> inputFilenames) {
            requireNonBuilt();
            this.inputFilenames = inputFilenames;
            return this;
        }

        public FileMerger<U> build() {
            requireNonBuilt();
            requireInitializeFields();
            isBuilt = true;
            return new FileMerger<>(this);
        }

        private void requireNonBuilt() {
            if (isBuilt)
                throw new IllegalStateException("FileMerger was already built");
        }

        private void requireInitializeFields() {
            if (comparator == null
                    || sorter == null
                    || formatter == null
                    || charset == null
                    || outputFilename == null
                    || inputFilenames == null)
                throw new IllegalStateException("Not all fields are initialized");
        }
    }
}

package ru.cft.focus;

import ru.cft.focus.merge.FileMerger;
import ru.cft.focus.util.formatter.Formatter;
import ru.cft.focus.util.formatter.Formatters;
import ru.cft.focus.util.parse.LaunchParametersParser;
import ru.cft.focus.util.sort.Sorter;
import ru.cft.focus.util.sort.Sorters;

import java.io.IOException;
import java.text.ParseException;
import java.util.Comparator;

public class Launcher {
    public static void main(String[] args) {
        try {
            LaunchParametersParser parser = new LaunchParametersParser();
            parser.parse(args);
            if (parser.getDataType() == String.class) {
                getTypedFileMerger(parser,
                        Sorters.insertionSorter(),
                        Formatters.stringFormatter()
                ).merge();
            } else {
                getTypedFileMerger(parser,
                        Sorters.insertionSorter(),
                        Formatters.longFormatter(parser.getRadix())
                ).merge();
            }
        } catch (ParseException | IOException exception) {
            System.err.println(exception.getMessage());
        } catch (NumberFormatException exception) {
            System.err.println("Invalid radix");
        } catch (NullPointerException exception) {
            exception.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> FileMerger<T> getTypedFileMerger(
            LaunchParametersParser parser,
            Sorter<T> sorter,
            Formatter<T> formatter
    ) {
        FileMerger.Builder<T> builder = FileMerger.getBuilder();
        return builder.setComparator((Comparator<T>) parser.getComparator())
                .setSorter(sorter)
                .setFormatter(formatter)
                .setCharset(parser.getCharset())
                .setOutputFilename(parser.getOutputFilename())
                .setInputFilenames(parser.getInputFilenames())
                .build();
    }
}

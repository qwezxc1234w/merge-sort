package ru.cft.focus.util.parse;

import javafx.util.Pair;

import java.text.ParseException;
import java.util.*;
import java.util.function.Consumer;

/*
 * Разбор параметров запуска.
 * Любые нераспознанные параметры считаются именами файлов
 * */
public class LaunchParametersParser {
    private String charset = "UTF-8";
    private Integer radix = 10;
    private Comparator<?> comparator = Comparator.naturalOrder();
    private Class<?> dataType;
    private String outputFilename;
    private List<String> inputFilenames;
    private Map<String, Pair<Consumer<Object>, Object>> unaryParamMapper;
    private Map<String, Consumer<String>> binaryParamMapper;

    public LaunchParametersParser() {
        inputFilenames = new ArrayList<>();

        unaryParamMapper = new TreeMap<>();
        unaryParamMapper.put("-a", new Pair<>(o -> comparator = (Comparator<?>) o, Comparator.naturalOrder()));
        unaryParamMapper.put("-d", new Pair<>(o -> comparator = (Comparator<?>) o, Comparator.reverseOrder()));
        unaryParamMapper.put("-s", new Pair<>(o -> dataType = (Class<?>) o, String.class));
        unaryParamMapper.put("-i", new Pair<>(o -> dataType = (Class<?>) o, Long.class));

        binaryParamMapper = new TreeMap<>();
        binaryParamMapper.put("-cs", str -> charset = str);
        binaryParamMapper.put("-ns", str -> {
            radix = Integer.parseInt(str);
            if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
                throw new NumberFormatException();
        });
    }

    public void parse(String[] args) throws ParseException, NumberFormatException {
        if (args.length > 2) {
            for (int i = 0; i < args.length; i++) {
                if (unaryParamMapper.containsKey(args[i])) {
                    Pair<Consumer<Object>, Object> pair = unaryParamMapper.get(args[i]);
                    pair.getKey().accept(pair.getValue());
                } else if (binaryParamMapper.containsKey(args[i])) {
                    binaryParamMapper.get(args[i]).accept(args[++i]);
                } else if (outputFilename == null) {
                    outputFilename = args[i];
                } else
                    inputFilenames.add(args[i]);
            }
            if (inputFilenames.isEmpty() || outputFilename == null || dataType == null)
                throw new ParseException("No key arguments specified", 0);
        } else
            throw new ParseException("Too few arguments", 0);
    }

    public Class<?> getDataType() {
        return dataType;
    }

    public Comparator<?> getComparator() {
        return comparator;
    }

    public Integer getRadix() {
        return radix;
    }

    public List<String> getInputFilenames() {
        return inputFilenames;
    }

    public String getCharset() {
        return charset;
    }

    public String getOutputFilename() {
        return outputFilename;
    }
}

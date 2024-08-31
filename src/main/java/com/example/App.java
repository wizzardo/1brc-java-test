package com.example;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;

public class App {

    final String filePath;

    public App(String filePath) {
        this.filePath = filePath;
    }

    private static final DecimalFormat df = new DecimalFormat("0.0");

    static class MeasurementInfo {
        //        long sum;
        String name;
        double sum;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        long count;

        public MeasurementInfo(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name + "=" + df.format(min) + "/" + df.format(sum / count) + "/" + df.format(max);
        }
    }

    public void execute() throws IOException {
        FileInputStream in = new FileInputStream(filePath);
        Map<String, MeasurementInfo> map = new HashMap<>();
        try {
            byte[] buf = new byte[1024 * 64];
            int r;
            ByteArrayBuilder name = new ByteArrayBuilder();
            StringBuilder value = new StringBuilder();
            ParsingState parsingState = ParsingState.NAME;
            while ((r = in.read(buf)) != -1) {
                int offset = 0;
                parsingState = process(buf, offset, r, name, value, map, parsingState);
            }

            List<Map.Entry<String, MeasurementInfo>> entries = new ArrayList<>(map.entrySet());
            entries.sort(Map.Entry.comparingByKey());
//            for (Map.Entry<String, MeasurementInfo> entry : entries) {
//                System.out.println(entry);
//            }
            System.out.print("{");
            for (int i = 0; i < entries.size(); i++) {
                if (i > 0) {
                    System.out.print(", ");
                }
                Map.Entry<String, MeasurementInfo> entry = entries.get(i);
                System.out.print(entry.getValue());
            }
            System.out.print("}");

        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }

    public enum ParsingState {
        NAME,
        VALUE
    }

    static class ByteArrayBuilder {
        byte[] buf = new byte[128];
        int pos;

        public void append(byte b) {
            buf[pos++] = b;
        }

        @Override
        public String toString() {
            return new String(buf, 0, pos, StandardCharsets.UTF_8);
        }

        public void setLength(int len) {
            pos = len;
        }

        public int length() {
            return pos;
        }
    }

    public ParsingState process(byte[] buf, int offset, int length, ByteArrayBuilder nameSb, StringBuilder valueSb, Map<String, MeasurementInfo> map, ParsingState state) {
        int limit = offset + length;

        for (int i = offset; i < limit; i++) {
            byte c = buf[i];
            if (state == ParsingState.NAME) {
                if (c == ';') {
                    state = ParsingState.VALUE;
                } else {
                    if (c == '#' && nameSb.length() == 0) {

                        state = ParsingState.VALUE;
                    } else {
                        nameSb.append(c);
                    }
                }
            } else {
                if (c == '\n') {
                    if (nameSb.length() == 0) {
                        state = ParsingState.NAME;
                        valueSb.setLength(0);
                        continue;
                    }

                    String name = nameSb.toString();
                    nameSb.setLength(0);


//                    int l = valueSb.length();
//                    long value = 0;
//                    for (int j = 0; j < l; j++) {
//                        char cc = valueSb.charAt(j);
//                        if (cc != '.') {
//                            value = value * 10 + (cc - '0');
//                        }
//                    }
                    double value = Double.parseDouble(valueSb.toString());
                    valueSb.setLength(0);

                    MeasurementInfo measurementInfo = map.computeIfAbsent(name, MeasurementInfo::new);
                    measurementInfo.sum += value;
                    measurementInfo.max = Math.max(measurementInfo.max, value);
                    measurementInfo.min = Math.min(measurementInfo.min, value);
                    measurementInfo.count++;

                    state = ParsingState.NAME;
                } else {
                    valueSb.append((char) c);
                }
            }
        }
        return state;
    }

    public static void main(String[] args) throws IOException {
        String filePath = "weather_stations.csv";
        if (args.length != 0) {
            filePath = args[0];
        }
        new App(filePath).execute();
    }
}

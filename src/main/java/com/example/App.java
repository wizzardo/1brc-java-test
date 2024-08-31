package com.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class App {

    final String filePath;

    public App(String filePath) {
        this.filePath = filePath;
    }

    public static class MeasurementInfo {
        String name;
        long count;
        long sum;
        long min = 1000;
        long max = -1000;

        public MeasurementInfo(String name) {
            this.name = name;
        }

        public String toString() {
            return name + "=" + (min / 10.0) + "/" + roundDouble((sum / 10.0) / count, 10) + "/" + (max / 10.0);
        }
    }

    static double roundDouble(double d, double scale) {
        return Math.round(d * scale) / scale;
    }

    public void execute() throws IOException {
        FileInputStream in = new FileInputStream(filePath);
        Map<String, MeasurementInfo> map = new HashMap<>();
        try {
            byte[] buf = new byte[1024 * 64];
            int r;
            ByteArrayBuilder name = new ByteArrayBuilder();
            ByteArrayBuilder value = new ByteArrayBuilder();
            ParsingState parsingState = ParsingState.NAME;
            while ((r = in.read(buf)) != -1) {
                int offset = 0;
                parsingState = process(buf, offset, r, name, value, map, parsingState);
            }

            List<Map.Entry<String, MeasurementInfo>> entries = new ArrayList<>(map.entrySet());
            entries.sort(Map.Entry.comparingByKey());
            System.out.print("{");
            for (int i = 0; i < entries.size(); i++) {
                if (i > 0) {
                    System.out.print(", ");
                }
                Map.Entry<String, MeasurementInfo> entry = entries.get(i);
                System.out.print(entry.getValue().toString());
            }
            System.out.println("}");

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

    public static class ByteArrayBuilder {
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

        public byte byteAt(int pos) {
            return buf[pos];
        }
    }

    public ParsingState process(byte[] buf, int offset, int length, ByteArrayBuilder nameSb, ByteArrayBuilder valueSb, Map<String, MeasurementInfo> map, ParsingState state) {
        int limit = offset + length;

        for (int i = offset; i < limit; i++) {
            byte c = buf[i];
            if (state == ParsingState.NAME) {
                if (c == ';') {
                    state = ParsingState.VALUE;
                } else {
                    nameSb.append(c);
                }
            } else {
                if (c == '\n') {
                    String name = nameSb.toString();
                    nameSb.setLength(0);

//                    long value = parseLong(valueSb);
                    long value = (long) (Double.parseDouble(valueSb.toString()) * 10);
                    valueSb.setLength(0);

                    MeasurementInfo measurementInfo = map.computeIfAbsent(name, MeasurementInfo::new);
                    measurementInfo.sum += value;
                    measurementInfo.max = Math.max(measurementInfo.max, value);
                    measurementInfo.min = Math.min(measurementInfo.min, value);
                    measurementInfo.count++;

                    state = ParsingState.NAME;
                } else {
                    valueSb.append(c);
                }
            }
        }
        return state;
    }

    private static long parseLong(ByteArrayBuilder valueSb) {
        long value = 0;
        int l = valueSb.length();
        boolean minus = false;
        {
            byte c = valueSb.byteAt(0);
            if (c != '-') {
                value = (c - '0');
            } else {
                minus = true;
            }
        }
        for (int j = 1; j < l; j++) {
            byte c = valueSb.byteAt(j);
            if (c != '.') {
                value = value * 10 + (c - '0');
            }
        }
        if (minus) {
            value = -value;
        }
        return value;
    }

    public static void main(String[] args) throws IOException {
        String filePath = "weather_stations.csv";
        if (args.length != 0) {
            filePath = args[0];
        }
        new App(filePath).execute();
    }
}

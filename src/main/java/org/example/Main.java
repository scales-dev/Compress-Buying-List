package org.example;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

public class Main {
  private static final Path CSV_PATH = Path.of("path");

  public static void main(String[] args) {
    ArrayList<Map<String, String>> csv = CsvUtil.readCsv(CSV_PATH, true);

    CsvUtil.writeCsv(CSV_PATH.getParent().resolve("OUTPUT.csv"), csv);
  }
}
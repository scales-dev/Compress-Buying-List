package org.example;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

public class Main {
  private static final Path CSV_PATH = Path.of("/home/jack-scales/Downloads/input.csv");

  public static void main(String[] args) {
    ArrayList<Map<String, String>> csv = CsvUtil.readCsv(CSV_PATH, false);

    for (var map : csv) {
      String productName = map.get("0");
      String variantName = map.get("1").replaceAll("[\\[\\]\"]", "");

      String quantityString = map.get("2");
      int quantity = quantityString.isEmpty() ? 1 : Integer.parseInt(quantityString);

      map.get("0");
    }

    CsvUtil.writeCsv(CSV_PATH.getParent().resolve("OUTPUT.csv"), csv);
  }
}
package org.example;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CsvUtil {
  public static void writeCsv(Path file, ArrayList<Map<String, String>> csv) {
    LinkedHashSet<String> headers = new LinkedHashSet<>();
    csv.forEach(product -> headers.addAll(product.keySet()));

    try{
      BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
      CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);

      csvPrinter.printRecord(headers);

      for (Map<String, String> item : csv) {
        csvPrinter.printRecord(headers.stream().map(item::get).toList());
      }

      writer.flush();
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static ArrayList<Map<String, String>> readCsv(Path file) {
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(
              // removes weird characters like zero width spaces which are somehow getting into my headers
              BOMInputStream.builder().setPath(file).get(),
              StandardCharsets.UTF_8
      ));

      CSVParser csvParser = CSVParser.parse(
              reader,
              CSVFormat.DEFAULT.builder()
                      .setHeader()
                      .setSkipHeaderRecord(true)
                      .get()
      );

      ArrayList<Map<String, String>> csv = new ArrayList<>();

      for (CSVRecord record : csvParser.getRecords()) {
        csv.add(record.toMap());
      }

      csvParser.close();

      return csv;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
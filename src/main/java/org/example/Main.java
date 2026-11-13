package org.example;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.xdrop.fuzzywuzzy.FuzzySearch;

public class Main {
  private static final int MIN_FUZZY_SCORE = 90;

  private static final String[] IGNORED_WORDS = {"pre-order", "fresh", "fruit"};
  private static final String IGNORED_WORDS_REGEX = "(?i)" + String.join("|", IGNORED_WORDS);

  // looks for numbers not immediately followed by a weight, if theres a weight anywhere, this regex
  // isnt called anyway.
  // none of the numbers should have a decimal, its just to stop it matching weights.
  private static final Pattern QUANTITY_REGEX =
      Pattern.compile("([0-9.]++)(?!\\s*(kg|g)\\b)", Pattern.CASE_INSENSITIVE);

  // looks for numbers followed by an optional space and a weight
  private static final Pattern KILOGRAM_REGEX =
      Pattern.compile("([0-9.]++)\\s*kg\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern GRAM_REGEX =
      Pattern.compile("([0-9.]++)\\s*g\\b", Pattern.CASE_INSENSITIVE);

  public static void main(String[] args) {
    final Path CSV_PATH = Path.of(args[0]);

    ArrayList<Map<String, String>> inputCSV = CsvUtil.readCsv(CSV_PATH, false);
    ArrayList<Map<String, String>> outputCSV = new ArrayList<>();

    while (!inputCSV.isEmpty()) {
      Product product = new Product();
      handleRow(inputCSV, inputCSV.getFirst(), product);

      // i cried when i found out hashmaps dont preserve order.
      Map<String, String> output = new LinkedHashMap<>();
      output.put("Source", String.join("\n", product.source));
      output.put("Quantity", String.valueOf(product.quantity));
      output.put("Weight (g)", String.valueOf(product.weight));
      outputCSV.add(output);
    }

    outputCSV.sort(Comparator.comparing(a -> a.get("Source")));

    CsvUtil.writeCsv(CSV_PATH.getParent().resolve("OUTPUT.csv"), outputCSV);
  }

  private static void handleRow(
      ArrayList<Map<String, String>> inputCSV, Map<String, String> row, Product product) {
    if (!inputCSV.remove(row)) return;

    String quantityString = row.get("2");
    int quantity = quantityString.isEmpty() ? 1 : Integer.parseInt(quantityString);

    String matchProductName = getMatchProductName(row);
    List<Map<String, String>> matching =
        inputCSV.stream()
            .filter(otherRow ->
                FuzzySearch.weightedRatio(getMatchProductName(otherRow), matchProductName)
                    > MIN_FUZZY_SCORE)
            .toList();

    // handle all matching products
    for (Map<String, String> matchingRow : matching) {
      handleRow(inputCSV, matchingRow, product);
    }

    String name = getProductName(row);
    int weight = getVariantWeight(name);
    product.weight += quantity * weight;

    int variantQuantity = getVariantQuantity(name);
    // if the product has 1 x 250g or 2.5kg x 1, we ignore those numbers,
    // and if there is neither quantity or weight default to 1
    product.quantity += weight > 0 ? 0 : variantQuantity > 0 ? quantity * variantQuantity : 1;

    product.source.add(name + " (" + quantity + ")");
  }

  private static class Product {
    public int quantity;
    public int weight;
    public ArrayList<String> source;

    public Product() {
      this.source = new ArrayList<>();
    }
  }

  private static int getVariantQuantity(String sourceName) {
    Matcher matcher = QUANTITY_REGEX.matcher(sourceName);

    // im sure just returning the first number is always fine
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
  }

  private static int getVariantWeight(String sourceName) {
    Matcher kgMatcher = KILOGRAM_REGEX.matcher(sourceName);
    // since most things are sold per gram it makes sense to convert kg -> g
    int kg = kgMatcher.find() ? (int) (Double.parseDouble(kgMatcher.group(1)) * 1000) : 0;

    Matcher gMatcher = GRAM_REGEX.matcher(sourceName);
    int g = gMatcher.find() ? Integer.parseInt(gMatcher.group(1)) : 0;

    // theres commonly stuff like 3 x 500g, so it makes sense to multiply the found weight by other
    // numbers
    Matcher quantity = QUANTITY_REGEX.matcher(sourceName);
    int quantityMultiplier = quantity.find() ? Integer.parseInt(quantity.group(1)) : 1;

    return (kg + g) * quantityMultiplier;
  }

  private static String getMatchProductName(Map<String, String> row) {
    return getProductName(row)
        .replaceAll(IGNORED_WORDS_REGEX, "")
        .replaceAll(KILOGRAM_REGEX.pattern(), "")
        .replaceAll(GRAM_REGEX.pattern(), "")
        .replaceAll(QUANTITY_REGEX.pattern(), "");
  }

  private static String getVariant(Map<String, String> row) {
    return row.get("1").replaceAll("[\\[\\]\"]", "");
  }

  private static String getProductName(Map<String, String> row) {
    return row.get("0") + " [" + getVariant(row) + "]";
  }
}
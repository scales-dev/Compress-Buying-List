package org.example;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import scales.dev.SpreadsheetUtil;

public class Main {
  private static final int MIN_FUZZY_SCORE = 90;

  private static final String[] IGNORED_WORDS = {"pre-order", "fresh", "fruit", "citrus"};
  private static final String IGNORED_WORDS_REGEX = "(?i)" + String.join("|", IGNORED_WORDS);

  // looks for numbers not immediately followed by a weight.
  // if there is a weight anywhere, this regex isnt called anyway.
  // none of the numbers should have a decimal, its just to stop it matching weights.
  private static final Pattern QUANTITY_REGEX =
          Pattern.compile("(\\d++)(?!\\.\\d)(?!\\s*(?:kg|g)\\b)", Pattern.CASE_INSENSITIVE);

  // looks for numbers followed by an optional space and a weight
  private static final Pattern KILOGRAM_REGEX =
          Pattern.compile("(\\d++(?:\\.\\d++)?)\\s*kg\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern GRAM_REGEX =
          Pattern.compile("(\\d++(?:\\.\\d++)?)\\s*g\\b", Pattern.CASE_INSENSITIVE);

  // Source - https://stackoverflow.com/a/6802502
  // (this is how i learnt Map.ofEntries exists and allows more than 10 values!!!
  // the amazon products generally have very different names :pensive:
  private static final Map<String, String> MANUAL_MAP = Map.ofEntries(
          Map.entry("Premium French Sand Carrots, Fresh Garden Vegetables, 1.5kg Box (1.5KG Box of French Sand Carrots)", "Carrot - sand 'carotte de sables' [1.5kg]"),

          Map.entry("Frozen Jackfruit, Exotic Tropical Fruit (500g)","Jackfruit flesh frozen - sweet and juicy [500g]"),

          Map.entry("Pepper PadrÃ³n - 30 Vegetables","Peppers - padron [30]"),
          Map.entry("Fresh Padron Peppers, Mild to Hot, perfect for adding a kick of heat to your mealtimes! (500g)","Peppers - padron [500g]"),

          Map.entry("Fresh Prickly Pear Fruit - 8 Whole Fruits (2 Packs of 4 Units)","Cactus fig – 'prickly pear' [8]"),

          Map.entry("Fresh Purple Sweet Potato Vegetables Approx. 500g - Imported Weekly from Thailand","Potato – purple, sweet [500g]"),
          Map.entry("Purple potatoes 1kg - Vitelotte variety - Fresh from France","Potato - purple, 'violetta', 'valfi' & 'vitelotte' [1kg]"),

          Map.entry("Fresh seeded key limes x 100. Grown in Egypt - Sourced by My Exotic Fruit","Citrus - key limes, seeded [100]"),
          Map.entry("Fresh seeded key limes x 20. Grown in Egypt - Sourced by My Exotic Fruit", "Citrus - key limes, seeded [20]"),

          Map.entry("Bitter Kola Fresh Garcinia Kola Approx 400g","Bitter kola [400g]"),
          Map.entry("Bitter Kola Nut (Cola Nuts) Fresh Garcinia Kola Approx 400g ...","Bitter kola [400g]"),

          Map.entry("Soursop 'Soursop' 'Graviola' 'Guanabana' - Fresh Fruit from Sri Lanka & Saint Lucia | My Exotic Fruit (1KG Fresh Soursop)","Soursop [1kg]"),

          Map.entry("FRESH PASSION FRUIT | 250G", "Passionfruit - purple [250g]")
  );

  public static void main(String[] args) {
    final Path CSV_PATH = Path.of(args[0]);

    ArrayList<Map<String, String>> inputCSV = SpreadsheetUtil.read(CSV_PATH, false);
    ArrayList<Map<String, String>> outputCSV = new ArrayList<>();

    while (!inputCSV.isEmpty()) {
      ProductType product = new ProductType();

      if (!handleRow(inputCSV, inputCSV.getFirst(), product)) continue;

      // i cried when i found out hashmaps dont preserve order.
      Map<String, String> output = new LinkedHashMap<>();
      output.put("Source", String.join("\n", product.source.stream().sorted().toList()));
      output.put("Quantity", String.valueOf(product.quantity));
      output.put("Weight (g)", String.valueOf(product.weight));
      outputCSV.add(output);
    }

    outputCSV.sort(Comparator.comparing(a -> a.get("Source")));

    Path outputPath = CSV_PATH.getParent().resolve("output.csv");
    SpreadsheetUtil.write(outputPath, outputCSV);
    System.out.println("Output compressed list to: " + outputPath);
  }

  private static boolean handleRow(
          ArrayList<Map<String, String>> inputCSV, Map<String, String> row, ProductType product) {
    if (!inputCSV.remove(row)) return false;

    // we often have empty entries, idk why.
    String nameSquare = row.get("0");
    if (nameSquare == null || nameSquare.isBlank()) return false;

    // manual mapped products have their variant embedded, so the 2nd row will be quantity
    String quantityRow = MANUAL_MAP.get(nameSquare) != null ? "1" : "2";
    int quantity = tryParseInt(row.get(quantityRow));

    String matchProductName = getProductNameForMatching(row);
    List<Map<String, String>> matching =
            inputCSV.stream()
                    .filter(otherRow ->
                            FuzzySearch.weightedRatio(getProductNameForMatching(otherRow), matchProductName)
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
    return true;
  }

  /// instead of throwing after the try fails, we return 1
  private static int tryParseInt(String s) {
    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      return 1;
    }
  }

  private static class ProductType {
    public int quantity;
    public int weight;
    public ArrayList<String> source;

    public ProductType() {
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

    // theres commonly stuff like 3 x 500g,
    // so it makes sense to multiply the found weight by other numbers
    Matcher quantity = QUANTITY_REGEX.matcher(sourceName);
    int quantityMultiplier = quantity.find() ? Integer.parseInt(quantity.group(1)) : 1;

    return (kg + g) * quantityMultiplier;
  }

  private static String getProductNameForMatching(Map<String, String> row) {
    return getProductName(row)
            .replaceAll(IGNORED_WORDS_REGEX, "")
            .replaceAll(KILOGRAM_REGEX.pattern(), "")
            .replaceAll(GRAM_REGEX.pattern(), "")

            .replaceAll("[^a-zA-Z ]", " ")
            .replaceAll("\\s+", " ")
            .trim();
  }

  private static String getVariant(Map<String, String> row) {
    return row.get("1").replaceAll("[\\[\\]\"]", "");
  }

  private static String getProductName(Map<String, String> row) {
    String name = row.get("0");
    String manualMap = MANUAL_MAP.get(name);

    return manualMap != null
            ? manualMap
            : name + " [" + getVariant(row) + "]";
  }
}
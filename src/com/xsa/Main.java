package com.xsa;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class Main {

  // default options
  static boolean Debug = false, Learn = false, Validation = false;
  static final String DefaultPath = "./poker_tables/";
  static int FilesLimit = 50, FilesSkip = 240;

  // recognize options
  static final String outputDebugFolder = "./output/";
  static final String defaultImageType = "png";
  static final int cardWith = 63, fullImageTopOffset = 64, minDiffer = 150;

  // statistics
  static int Valid, RecognizeError, AllItems = 0;
  static long start;
  static long end;

  // model
  static Map<String, String> CardNames = new HashMap<>();
  static Map<Integer, EnumCardColors> CardsCharsMap =
      new HashMap<>() {
        {
          put(-14474458, EnumCardColors.Black);
          put(-15724526, EnumCardColors.Black); // dark
          put(-3323575, EnumCardColors.Red);
          put(-10477022, EnumCardColors.Red); // dark
          put(-1, EnumCardColors.White);
          put(-8882056, EnumCardColors.White); // dark
          put(-14013910, EnumCardColors.empty);
          put(-14474461, EnumCardColors.empty);
          put(-678365, EnumCardColors.yellow); // TODO after tests: dark?
        }
      };
  static Map<EnumCardColors, Point> CheckPixelCoordinate =
      new HashMap<>() {
        {
          put(EnumCardColors.Black, new Point(33, 60)); // для сравнения по крестям
          put(EnumCardColors.Red, new Point(42, 54)); // для сравнения по сердцам
        }
      };

  public static void main(String[] args) throws IOException {
    // TODO PARAMS
    // если параметр обязательный, и отсутвует то используется значение по умолчанию
    // ./poker_tables -f 10 -s 200 -d false -l false -v true
    String path = null;
    if (args.length == 0) {
      path = DefaultPath;
      System.out.printf("Using default path: %s\r\n", path);
    }
    if (args.length > 0) {
      path = args[0];
      System.out.printf("Using path: %s\r\n", path);
      try {
        // TODO
        FilesLimit = Integer.parseInt(args[1]);
        FilesSkip = Integer.parseInt(args[2]);
        Debug = Boolean.parseBoolean(args[3]);
        Learn = Boolean.parseBoolean(args[4]);
        Validation = Boolean.parseBoolean(args[5]);
      } catch (Exception ignored) {
      }
    }
    try (Stream<Path> paths = Files.walk(Paths.get(path))) {
      paths
          .filter(Files::isRegularFile)
          .filter(object -> object.toString().endsWith(defaultImageType))
          .skip(FilesSkip)
          .limit(FilesLimit)
          .forEach(
              pokerTableScreenshot -> {
                try {
                  start = System.currentTimeMillis();

                  cardShapesLoadHashMapFromCsv();
                  System.out.printf(
                      "%s - %s\r\n ",
                      pokerTableScreenshot.getFileName(),
                      recognizedStringForFullImage(pokerTableScreenshot).replace("--", ""));

                  end = System.currentTimeMillis();

                  if (Debug) {
                    System.out.println(end - start);
                  }

                  if (Learn) {
                    cardShapesSaveHashMapToCsv();
                  }

                } catch (IOException e) {
                  e.printStackTrace();
                }
              });
    }
    if (Validation) {
      System.out.printf(
          "Statistics: \r\nAllFiles: %d, Valid: %d, RecognizeErrors: %d. ",
          AllItems, Valid, RecognizeError);
    }
  }

  private static BufferedImage convertImageToBW(BufferedImage image) {
    BufferedImage cardNameIgmBwImg =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
    Graphics2D graphics = cardNameIgmBwImg.createGraphics();
    graphics.drawImage(image, 0, 0, null);
    return cardNameIgmBwImg;
  }

  private static EnumCardColorMode getCardColorMode(BufferedImage image) {
    EnumCardColorMode cardColorMode = EnumCardColorMode.Normal;
    int cardColorModePixel = image.getRGB(45, 30);
    int cardColorMixedMarker = -8882056;
    if (cardColorModePixel == cardColorMixedMarker) {
      cardColorMode = EnumCardColorMode.Darker;
    }
    return cardColorMode;
  }

  private static BufferedImage convertShapeToLightMode(
      BufferedImage image, EnumCardColorMode cardColorMode) {
    int cardColorModePixel = image.getRGB(45, 30);
    if (cardColorMode == EnumCardColorMode.Darker) {
      for (int y = 0; y < image.getHeight(); y++) {
        for (int x = 0; x < image.getWidth(); x++) {
          int pixelColor = image.getRGB(x, y);
          boolean isDarkColor = (pixelColor == cardColorModePixel);
          if (isDarkColor) {
            image.setRGB(x, y, -1);
          } else {
            image.setRGB(x, y, pixelColor);
          }
        }
      }
    }
    return image;
  }

  private static String getShapeForCardImage(BufferedImage image, int number, Path screenShotFile)
      throws IOException {
    String card;
    EnumCardColors cardColor = colorForPoint(image);

    if (!cardColor.equals(EnumCardColors.Red) && !cardColor.equals(EnumCardColors.Black)) {
      return "-";
    }

    EnumCardColorMode colorMode = getCardColorMode(image);
    BufferedImage whiteImage = convertShapeToLightMode(image, colorMode);
    BufferedImage cardNameBW = convertImageToBW(whiteImage.getSubimage(2, 5, 35, 25));

    String imageHash = getBinaryStringForPixels(cardNameBW);

    for (int i = 0; i < imageHash.length(); i++) { //
      System.out.printf("%c", imageHash.toCharArray()[i]);
      if (i % 35 == 0) {
        System.out.println("\r\n");
      }
    }

    int min = minDiffer;
    String findSymbol = "?";
    for (Map.Entry<String, String> entry : CardNames.entrySet()) {
      int differs = compareShapeFunction(imageHash, entry.getValue());
      if (differs < min) {
        min = differs;
        findSymbol = entry.getKey();
      }
      if (Debug) {
        System.out.printf("Symbol differ > %d\r\n", minDiffer);
      }
    }
    card = findSymbol;

    if (Learn) {
      // open file
      openFile(
          saveDebugImageToPath(cardNameBW, screenShotFile, DebugImagesTypes.CardNameBW, number));

      // valid or not
      System.out.printf("Please validate image. This is %s?\r\n", findSymbol);
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      String answer = br.readLine();
      if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("")) {
        card = findSymbol.trim().toUpperCase();
      } else {
        System.out.println("Please write a valid card name for this hash.");
        br = new BufferedReader(new InputStreamReader(System.in));
        answer = br.readLine();
        CardNames.put(answer.trim().toUpperCase(), imageHash);
        card = answer.trim().toUpperCase();
      }
    }
    return card;
  }

  private static int compareShapeFunction(String targetStr, String sourceStr) {
    int m = targetStr.length(), n = sourceStr.length();
    int[][] delta = new int[m + 1][n + 1];
    for (int i = 1; i <= m; i++) delta[i][0] = i;
    for (int j = 1; j <= n; j++) delta[0][j] = j;
    for (int j = 1; j <= n; j++)
      for (int i = 1; i <= m; i++) {
        if (targetStr.charAt(i - 1) == sourceStr.charAt(j - 1)) delta[i][j] = delta[i - 1][j - 1];
        else
          delta[i][j] =
              Math.min(delta[i - 1][j] + 1, Math.min(delta[i][j - 1] + 1, delta[i - 1][j - 1] + 1));
      }
    return delta[m][n];
  }

  private static String getBinaryStringForPixels(BufferedImage symbol) {
    short whiteBg = -1;
    StringBuilder binaryString = new StringBuilder();
    for (short y = 1; y < symbol.getHeight(); y++)
      for (short x = 1; x < symbol.getWidth(); x++) {
        int rgb = symbol.getRGB(x, y);
        binaryString.append(rgb == whiteBg ? " " : "*");
      }
    return binaryString.toString();
  }

  private static BufferedImage[] getCardVectorFromFullImage(BufferedImage full, Path object)
      throws IOException {
    // считываем цент экрана
    BufferedImage crop = full.getSubimage(120, 521, full.getWidth() - 220, 89);
    BufferedImage[] cardVector = new BufferedImage[5];
    int indexOfCard = 0;
    for (int fileIndex = 0; fileIndex < full.getWidth() - 220; fileIndex++) {
      int color = crop.getRGB(fileIndex, crop.getHeight() / 2);
      EnumCardColors cardColor = CardsCharsMap.get(color);
      if (cardColor == EnumCardColors.White || cardColor == EnumCardColors.yellow) {
        cardVector[indexOfCard] = crop.getSubimage(fileIndex, 0, cardWith, crop.getHeight());
        fileIndex += cardWith;
        indexOfCard++;
      }
    }
    if (Debug) {
      openFile(saveDebugImageToPath(crop, object, DebugImagesTypes.Center));
    }
    return cardVector;
  }

  private static String recognizedStringForFullImage(Path screenshotFilePath) throws IOException {
    StringBuilder result = new StringBuilder();

    // считываем полную картинку
    BufferedImage img = ImageIO.read(screenshotFilePath.toFile());
    int verticalOffset = fullImageTopOffset;

    BufferedImage full =
        img.getSubimage(0, verticalOffset, img.getWidth(), img.getHeight() - verticalOffset);
    saveDebugImageToPath(full, screenshotFilePath, DebugImagesTypes.Full);

    BufferedImage[] cardVector = getCardVectorFromFullImage(full, screenshotFilePath);

    for (int index = 0;
        index < Arrays.stream(cardVector).filter(Objects::nonNull).toArray().length;
        index++) {
      EnumCardSuit cardSuit = cardSuitForCardImage(cardVector[index]);
      String card = getShapeForCardImage(cardVector[index], index, screenshotFilePath);
      result.append(card);
      result.append(cardSuit.toString().substring(0, 1).toLowerCase());
    }

    result.append("\r");

    if (Validation) {
      openFile(screenshotFilePath.toFile());
      System.out.printf("File: %s, Result: %s\r\n", screenshotFilePath.getFileName(), result);
      System.out.print("Validate recognition:");
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      String answer = br.readLine();
      if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("")) {
        Valid++;
      } else {
        RecognizeError++;
      }
      AllItems++;
    }

    return result.toString();
  }

  private static EnumCardColors colorForPoint(BufferedImage image) {
    Point firstLayer = new Point(41, 69);
    int rgb_int = image.getRGB(firstLayer.x, firstLayer.y);
    return CardsCharsMap.get(rgb_int);
  }

  private static void openFile(File file) {
    try {
      Desktop desktop = null;
      if (Desktop.isDesktopSupported()) {
        desktop = Desktop.getDesktop();
      }
      if (desktop != null) {
        desktop.open(file);
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  private static EnumCardSuit cardSuitForCardImage(BufferedImage image) {
    EnumCardSuit mast = null;
    Point secondLayerPoint;

    EnumCardColors colorForPoint = colorForPoint(image);
    EnumCardColors secondLayerColor;
    switch (colorForPoint) {
      case Black:
        secondLayerPoint = CheckPixelCoordinate.get(EnumCardColors.Black);
        secondLayerColor = CardsCharsMap.get(image.getRGB(secondLayerPoint.x, secondLayerPoint.y));
        if (secondLayerColor == EnumCardColors.Black) {
          mast = EnumCardSuit.Spades;
        } else {
          mast = EnumCardSuit.Clubs;
        }
        break;
      case Red:
        secondLayerPoint = CheckPixelCoordinate.get(EnumCardColors.Red);
        secondLayerColor = CardsCharsMap.get(image.getRGB(secondLayerPoint.x, secondLayerPoint.y));
        if (secondLayerColor == EnumCardColors.Red) {
          mast = EnumCardSuit.Diamonds;
        } else {
          mast = EnumCardSuit.Hearts;
        }
        break;
      case empty:
      case yellow:
      case White:
      default:
        break;
    }

    return mast;
  }

  private static void cardShapesLoadHashMapFromCsv() throws IOException {
    final BufferedReader br = new BufferedReader(new FileReader("card_shapes.csv"));
    while (br.ready()) {
      cardShapeLoadHashMapFromString(new CardName(br.readLine()));
    }
    if (Debug) {
      System.out.printf("Hashes in model: %d\r\n", (long) CardNames.entrySet().size());
    }
  }

  private static void cardShapeLoadHashMapFromString(CardName cardName) {
    if (!CardNames.containsKey(cardName.getName())) {
      CardNames.put(cardName.getName(), cardName.getHash());
    }
  }

  private static void cardShapesSaveHashMapToCsv() {
    String eol = System.getProperty("line.separator");
    try (Writer writer = new FileWriter("card_shapes.csv")) {
      writer.write("");
      for (Map.Entry<String, String> entry : CardNames.entrySet()) {
        writer.append(entry.getKey()).append(';').append(entry.getValue()).append(eol);
      }
    } catch (IOException ex) {
      ex.printStackTrace(System.err);
    }
  }

  private static class CardName {
    String name;
    String hash;

    public CardName(String line) {
      this.name = line.split(";")[0];
      this.hash = line.split(";")[1];
    }

    public String getName() {
      return this.name;
    }

    public String getHash() {
      return this.hash;
    }
  }

  private static class Point {
    int x;
    int y;

    public Point(int x, int y) {
      this.x = x;
      this.y = y;
    }
  }

  private enum EnumCardColors {
    Black,
    Red,
    White,
    empty,
    yellow
  }

  private enum EnumCardSuit {
    Diamonds, // ♦️ Diamonds
    Hearts, // ♥️ Hearts
    Spades, // ♠️ Spades
    Clubs, // ♣️ Clubs
  }

  private enum EnumCardColorMode {
    Darker, // ▓
    Normal // ⬜️
  }

  private static File saveDebugImageToPath(
      BufferedImage image, Path screenShotFile, DebugImagesTypes fileType) throws IOException {
    if (Debug) {
      File file = null;

      switch (fileType) {
        case Full:
          file =
              new File(String.format("%sfull_%s", outputDebugFolder, screenShotFile.getFileName()));
          break;
        case Center:
          file =
              new File(String.format("%scrop_%s", outputDebugFolder, screenShotFile.getFileName()));

        default:
          break;
      }

      ImageIO.write(image, defaultImageType, file);

      return file;
    }
    return null;
  }

  private static File saveDebugImageToPath(
      BufferedImage image, Path screenShotFile, DebugImagesTypes fileType, int number)
      throws IOException {
    if (Debug) {
      File file = null;

      switch (fileType) {
        case CardNameBW:
          file =
              new File(
                  String.format(
                      "%sCrop_%s_%d_name_BW.%s",
                      outputDebugFolder, screenShotFile.getFileName(), number, defaultImageType));
          ImageIO.write(image, defaultImageType, file);
          break;
        case CardImage:

        default:
          break;
      }

      ImageIO.write(image, defaultImageType, file);

      return file;
    }
    return null;
  }

  private enum DebugImagesTypes {
    Full,
    Center,
    CardImage,
    CardNameBW
  }
}

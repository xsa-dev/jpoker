package com.xsa;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class Main {
    // general
    static String result;
    // recognize options
    static final String DefaultPath = "./poker_tables/", outputDebugFolder = "./debug_output/", defaultImageType = "png", validatedResultsFile = "validated_results.csv", cardShapesFile = "card_shapes.csv";
    static final int cardWith = 63, fullImageTopOffset = 64, minDiffer = 101;
    // default options
    static boolean Debug = false, Learn = false, Validation = false;
    static int FilesLimit = 500, FilesSkip = 0;
    // statistics
    static int Valid, RecognizeError, AllItems = 0;
    static long start, end;
    // model
    static HashMap<String, String> CardShapes =
            new HashMap<>();
    static HashMap<Integer, EnumCardColors> CardsCharsMap =
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
    static HashMap<EnumCardColors, Point> CheckPixelCoordinate =
            new HashMap<>() {
                {
                    put(EnumCardColors.Black, new Point(33, 60)); // для сравнения по крестям
                    put(EnumCardColors.Red, new Point(42, 54)); // для сравнения по сердцам
                }
            };
    static HashMap<String, String> ValidatedResults =
            new HashMap<>();
    static HashMap<String, String> Errors =
            new HashMap<>();

    public static void main(String[] args) throws IOException {
        // some debug optimization dumb
        BufferedImage dumb =
                new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D graphics = dumb.createGraphics();
        graphics.drawImage(dumb, 0, 0, null);


        String path = null;
        if (args.length == 0) {
            path = DefaultPath;
        }
        if (args.length > 0) {
            path = args[0];
            try {
                FilesLimit = Integer.parseInt(args[1]);
                FilesSkip = Integer.parseInt(args[2]);
                Debug = Boolean.parseBoolean(args[3]);
                Learn = Boolean.parseBoolean(args[4]);
                Validation = Boolean.parseBoolean(args[5]);
            } catch (Exception ignored) {
                System.out.println("Error while loaded params.\r\nHelp:\r\nArgs example: /path/to/full/imgs/ IntCountOfImgs IntOffsetImgs BooleanDebug BooleanLearn BooleanValidate");
            }
        }
        if (Debug) {
            Files.createDirectories(Paths.get(outputDebugFolder));
            System.out.println("Help:\r\nArgs example: /path/to/full/imgs/ IntCountOfImgs IntOffsetImgs BooleanDebug BooleanLearn BooleanValidate");
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
                                    LoadCsvToHashMap(ValidatedResults, validatedResultsFile, DataType.Results);
                                    LoadCsvToHashMap(CardShapes, cardShapesFile, DataType.Cards);
                                    result = recognizedStringForFullImage(pokerTableScreenshot).replace("--", "");
                                    long time = end - start;
                                    if (Debug) {
                                        System.out.printf("Time: %d: %s - %s\r\n", time, pokerTableScreenshot.getFileName(), result);
                                    } else {
                                        System.out.printf("%s - %s\r\n", pokerTableScreenshot.getFileName(), result);
                                    }
                                    if (Learn) {
                                        SaveCsvFromHashMap(CardShapes, cardShapesFile, DataType.Results);
                                    }
                                    if (Validation) {
                                        SaveCsvFromHashMap(ValidatedResults, validatedResultsFile, DataType.Results);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
            if (Validation) {
                System.out.printf(
                        "Statistics: \r\nAllFiles: %d, Valid: %d, RecognizeErrors: %d.\r\n",
                        AllItems, Valid, RecognizeError);
                for (Map.Entry<String, String> error : Errors.entrySet()) {
                    System.out.println("-###-###-");
                    System.out.println("Error for: " + error.getValue());
                    printImage(error.getKey());
                }
            }
        }
        if (Debug || Validation) {
            System.out.printf("Validation map count: %d", ValidatedResults.entrySet().size());
            for (Map.Entry<String, String> resultd : ValidatedResults.entrySet()) {
                System.out.printf("%s - %s\r\n", resultd.getKey(), resultd.getValue());
            }
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
        String imageBinaryString = getBinaryStringForPixels(cardNameBW);
//        System.out.println("Last Chance: Eight Point Algo");
        String findSymbol = "?";
        int differs = -1;
        int min = 100;

        for (Map.Entry<String, String> entry : CardShapes.entrySet()) {
            differs = compareShapeFunction(imageBinaryString, entry.getValue());
            if (differs < min) {
                min = differs;
                findSymbol = entry.getKey();
//                System.out.println(findSymbol + ", difference: " + differs);
            }
        }
        if (Validation) {
            System.out.printf("Differs: %d\r\n", min);
            if (min > minDiffer) {
                System.out.println("Warning! Differ больше минимального!");
            }
        }
        card = findSymbol;
        if (Learn || findSymbol.equals("?")) {
            if (ValidatedResults.containsValue(screenShotFile.getFileName())) {
                char[] validVector = ValidatedResults.get(screenShotFile.getFileName()).toString().toCharArray();
                if (Character.compare(validVector[number], findSymbol.toCharArray()[0]) == 0) {
                    System.out.println("Automatic validation.");
                } else {
                    System.out.printf("Please validate image. This is %s?\r\n", findSymbol);
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    String answer = br.readLine();

                    if (answer.equalsIgnoreCase("y") || answer.equals("")) {
                        card = findSymbol.trim().toUpperCase();
                    } else {
                        RecognizeError++;
                        System.out.println("Please write a valid card name for this hash.");
                        br = new BufferedReader(new InputStreamReader(System.in));
                        answer = br.readLine();
                        CardShapes.put(answer.trim().toUpperCase(), imageBinaryString);
                        Errors.put(imageBinaryString, answer.trim().toUpperCase());
                        card = answer.trim().toUpperCase();
                    }
                }
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
        short whiteColor = -1;
        StringBuilder binaryString = new StringBuilder();
        for (short y = 1; y < symbol.getHeight(); y++) {
            for (short x = 1; x < symbol.getWidth(); x++) {
                int rgb = symbol.getRGB(x, y);
                binaryString.append(rgb == whiteColor ? "@" : "*");
                if (Validation) {
                    System.out.printf("%s", rgb == whiteColor ? "@" : "*");
                }
            }
            if (Validation) {
                System.out.println("");
            }
        }
        return binaryString.toString();
    }

    private static BufferedImage[] getCardVectorFromFullImage(BufferedImage full, Path object)
            throws IOException {
        // считываем цент экрана
        BufferedImage crop = full.getSubimage(120, 521, full.getWidth() - 220, 89); // TODO to final const
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
        return cardVector;
    }

    private static String recognizedStringForFullImage(Path screenshotFilePath) throws IOException {
        StringBuilder result = new StringBuilder();
        start = System.currentTimeMillis();
        // считываем полную картинку
        BufferedImage img = ImageIO.read(screenshotFilePath.toFile());
        int verticalOffset = fullImageTopOffset;
        BufferedImage full =
                img.getSubimage(0, verticalOffset, img.getWidth(), img.getHeight() - verticalOffset);
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
        end = System.currentTimeMillis();
        if (Validation) {
            System.out.printf("File: %s, Result: %s\r\n", screenshotFilePath.getFileName(), result);
            String fileName = screenshotFilePath.getFileName().toString();
            String r_result = result.toString().replace("--", "").replace("\r", "");
            if (ValidatedResults.get(fileName) != null) {
                if (ValidatedResults.get(fileName).equals(r_result)) {
                    System.out.printf("Automatic validation. Result -> EqualsOk: [%s] for file: %s\r\n", result.toString().replace("\r", ""), screenshotFilePath.getFileName());
                    Valid++;
                } else {
                    System.out.print("Validate recognition:");
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    String answer = br.readLine();
                    if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("")) {
                        ValidatedResults.put(screenshotFilePath.getFileName().toString(), result.toString());
                        Valid++;
                    } else {
                        RecognizeError++;
                    }
                }
            } else {
                System.out.print("Validate recognition:");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String answer = br.readLine();
                if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("")) {
                    ValidatedResults.put(screenshotFilePath.getFileName().toString(), result.toString());
                    Valid++;
                } else {
                    RecognizeError++;
                }
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

    private static EnumCardSuit cardSuitForCardImage(BufferedImage image) {
        EnumCardSuit suit = null;
        Point secondLayerPoint;

        EnumCardColors colorForPoint = colorForPoint(image);
        EnumCardColors secondLayerColor;
        switch (colorForPoint) {
            case Black:
                secondLayerPoint = CheckPixelCoordinate.get(EnumCardColors.Black);
                secondLayerColor = CardsCharsMap.get(image.getRGB(secondLayerPoint.x, secondLayerPoint.y));
                if (secondLayerColor == EnumCardColors.Black) {
                    suit = EnumCardSuit.Spades;
                } else {
                    suit = EnumCardSuit.Clubs;
                }
                break;
            case Red:
                secondLayerPoint = CheckPixelCoordinate.get(EnumCardColors.Red);
                secondLayerColor = CardsCharsMap.get(image.getRGB(secondLayerPoint.x, secondLayerPoint.y));
                if (secondLayerColor == EnumCardColors.Red) {
                    suit = EnumCardSuit.Diamonds;
                } else {
                    suit = EnumCardSuit.Hearts;
                }
                break;
            case empty:
            case yellow:
            case White:
            default:
                break;
        }

        return suit;
    }

    private static void LoadCsvToHashMap(HashMap hashMap, String fileName, DataType dataType) throws IOException {
        final BufferedReader br = new BufferedReader(new FileReader(fileName));
        while (br.ready()) {
            if (DataType.Cards.equals(dataType)) {
                LoadHashMapFromString(dataType, new CardName(br.readLine()));
            }
            if (DataType.Results.equals(dataType)) {
                LoadHashMapFromString(dataType, new Result(br.readLine()));
            }
        }
        if (Validation) {
            long lenght = 0;
            if (DataType.Cards.equals(dataType)) {
                lenght = (long) CardShapes.entrySet().size();
            }
            if (DataType.Results.equals(dataType)) {
                lenght = (long) ValidatedResults.entrySet().size();
            }
            System.out.printf("Validated results in model: %d\r\n", lenght);
        }
    }

    private static void SaveCsvFromHashMap(HashMap<String, String> hashMap, String fileName, Enum dataType) throws IOException {
        String eol = System.getProperty("line.separator");
        try (Writer writer = new FileWriter(fileName)) {
            writer.write("");
            for (HashMap.Entry<String, String> entry : hashMap.entrySet()) {
                writer.append(entry.getKey()).append(';').append(entry.getValue()).append(eol);
            }
        }
    }

    private static void LoadHashMapFromString(DataType type, Object obj) {
        if (obj instanceof Result) {
            ValidatedResults.put(((Result) obj).getFileName(), ((Result) obj).getRecognizedValue());
        }
        if (obj instanceof CardName) {
            CardShapes.put(((CardName) obj).getName(), ((CardName) obj).getHash());
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

    private enum DebugImagesTypes {
        Full,
        Center,
        CardImage,
        CardNameBW
    }

    private enum DataType {
        Cards,
        Results
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

    private static class Result {
        String fileName;
        String recognizedValue;

        public Result(String line) {
            this.fileName = line.split(";")[0];
            this.recognizedValue = line.split(";")[1];
        }

        public String getFileName() {
            return this.fileName;
        }

        public String getRecognizedValue() {
            return this.recognizedValue;
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

    static void printImage(String string) {
        System.out.println("is see:");
        char[] chars = string.replace("\n", "").toCharArray();
        for (int i = 1; i <= chars.length; i++) {
            System.out.printf("%c", chars[i - 1]);
            if (i % 34 == 0) {
                System.out.println("");
            }
        }
        System.out.println("This code:");
        System.out.println(string.replace("\n", ""));
    }
}

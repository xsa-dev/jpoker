package com.java.xsa;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.io.FileWriter;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class Main {
    // recognize
    private static final String DefaultPath = "../imgs/", defaultImageType = "png", validatedResultsFile = "model/validated_results.csv", cardShapesFile = "model/card_shapes.csv";
    private static final int cardWith = 63, fullImageTopOffset = 64, minDiffer = 99, cropLeftOffset = 120, cropTopOffset = 521, cropWidthOffset = 220, cropHeightOffset = 89;
    // strings
    private static String result, eol = System.getProperty("line.separator");
    // options
    private static boolean Debug = false, Learn = false, Validation = false;
    private static int FilesLimit = 9999, FilesSkip = 0;
    // statistics
    private static int Valid, RecognizeError, AllItems = 0;
    private static long start, end;
    // model
    private static HashMap<String, String> CardShapes =
            new HashMap<>();
    private static HashMap<Integer, EnumCardColors> CardsCharsMap =
            new HashMap<Integer, EnumCardColors>() {
                {
                    put(-14474458, EnumCardColors.Black);
                    put(-15724526, EnumCardColors.Black); // dark
                    put(-3323575, EnumCardColors.Red);
                    put(-10477022, EnumCardColors.Red); // dark
                    put(-1, EnumCardColors.White);
                    put(-8882056, EnumCardColors.White); // dark
                    put(-14013910, EnumCardColors.empty);
                    put(-14474461, EnumCardColors.empty);
                    put(-678365, EnumCardColors.yellow); //
                }
            };
    private static HashMap<EnumCardColors, Point> CheckPixelCoordinate =
            new HashMap<EnumCardColors, Point>() {
                {
                    put(EnumCardColors.Black, new Point(33, 60)); // clubs
                    put(EnumCardColors.Red, new Point(42, 54)); // hearts
                }
            };
    private static HashMap<String, String> ValidatedResults =
            new HashMap<>();
    private static HashMap<String, String> Errors =
            new HashMap<>();

    public static void main(String[] args) {
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
            }
        }
        if (Debug) {
            try {
                loadCsvToModel(validatedResultsFile, EnumDataType.Results);
            } catch (IOException e) {
                System.out.printf("Ошибка загрузки результатов. Проверьте доступность CSV-модели.\r\n%s", e.getMessage());
            }
            System.out.println("Help: Args example: /path/to/full/imgs/ IntCountOfImgs IntOffsetImgs BooleanDebug BooleanLearn BooleanValidate");
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
                                    loadCsvToModel(cardShapesFile, EnumDataType.Cards);
                                } catch (IOException e) {
                                    System.out.printf("Ошибка загрузки карт. Проверьте доступность CSV-модели.\r\n%s", e.getMessage());
                                }
                                try {
                                    result = recognizeFullImage(pokerTableScreenshot).replace("--", "");
                                } catch (IOException e) {
                                    System.out.printf("Ошибка при распознавании. Проверьте модель и параметры.\r\n%s", e.getMessage());
                                }
                                long time = end - start;
                                if (Debug) {
                                    System.out.printf("Time: %d: %s - %s", time, pokerTableScreenshot.getFileName(), result);
                                } else {
                                    System.out.printf("%s - %s", pokerTableScreenshot.getFileName(), result);
                                }
                                if (Learn) {
                                    try {
                                        saveModelToCsv(CardShapes, cardShapesFile);
                                    } catch (IOException e) {
                                        System.out.printf("Не удалось сохранить бинарные значения карты.\r\n%s", e.getMessage());
                                    }
                                }
                                if (Validation) {
                                    try {
                                        saveModelToCsv(ValidatedResults, validatedResultsFile);
                                    } catch (IOException e) {
                                        System.out.printf("Не удалось сохранить провалидированные результаты.\r\n%s", e.getMessage());
                                    }
                                }
                            });
            if (Validation) {
                System.out.printf(
                        "Statistics:\rAllFiles: %d, Valid: %d, RecognizeErrors: %d.\r",
                        AllItems, Valid, RecognizeError);
                for (Map.Entry<String, String> error : Errors.entrySet()) {
                    System.out.println("Error for: " + error.getValue());
                    printBinaryImage(error.getKey());
                }
            }
        }
        catch (IOException e) {
            System.out.printf("Ошибка загрузки файлов. Проверьте доступность папки с картинками.\r\n%s", e.getMessage());
        }
    }

    private static String recognizeFullImage(Path screenshotFilePath)
            throws IOException {
        StringBuilder result = new StringBuilder();
        start = System.currentTimeMillis();
        BufferedImage img = ImageIO.read(screenshotFilePath.toFile());
        int verticalOffset = fullImageTopOffset;
        BufferedImage full =
                img.getSubimage(0, verticalOffset, img.getWidth(), img.getHeight() - verticalOffset);
        BufferedImage[] cardVector = getCardImagesVector(full);
        for (int index = 0;
             index < Arrays.stream(cardVector).filter(Objects::nonNull).toArray().length;
             index++) {
            EnumCardSuit cardSuit = getCardSuit(cardVector[index]);
            String card = getCardValue(cardVector[index]);
            result.append(card);
            result.append(cardSuit.toString().substring(0, 1).toLowerCase());
        }
        result.append(eol);
        end = System.currentTimeMillis();
        if (Validation) {
            System.out.printf("File: %s, Result: %s\r", screenshotFilePath.getFileName(), result);
            String fileName = screenshotFilePath.getFileName().toString();
            String r_result = result.toString().replace("--", "").replace(eol, "");
            if (ValidatedResults.get(fileName) != null) {
                if (ValidatedResults.get(fileName).equals(r_result)) {
                    System.out.printf("Automatic validation:: Result -> EqualsOk [%s] << file: %s%s", result.toString().replace(eol, ""), screenshotFilePath.getFileName(), eol);
                    Valid++;
                } else {
                    setValidatedResults(screenshotFilePath);
                }
            } else {
                setValidatedResults(screenshotFilePath);
            }
            AllItems++;
        }
        return result.toString();
    }

    private static BufferedImage[] getCardImagesVector(BufferedImage image) {
        BufferedImage crop = image.getSubimage(cropLeftOffset, cropTopOffset, image.getWidth() - cropWidthOffset, cropHeightOffset);
        BufferedImage[] cardVector = new BufferedImage[5];
        int indexOfCard = 0;
        for (int fileIndex = 0; fileIndex < image.getWidth() - cropWidthOffset; fileIndex++) {
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

    private static EnumCardColors getCardColor(BufferedImage image) {
        Point firstLayer = new Point(41, 69);
        int rgb_int = image.getRGB(firstLayer.x, firstLayer.y);
        return CardsCharsMap.get(rgb_int);
    }

    private static EnumCardSuit getCardSuit(BufferedImage image) {
        EnumCardSuit suit = null;
        Point secondLayerPoint;
        EnumCardColors colorForPoint = getCardColor(image);
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
            default:
                break;
        }

        return suit;
    }

    private static EnumCardMode getColorMode(BufferedImage image) {
        EnumCardMode cardColorMode = EnumCardMode.Normal;
        int cardColorModePixel = image.getRGB(45, 30);
        int cardColorMixedMarker = -8882056;
        if (cardColorModePixel == cardColorMixedMarker) {
            cardColorMode = EnumCardMode.Darker;
        }
        return cardColorMode;
    }

    private static BufferedImage convertImgToLight(
            BufferedImage image, EnumCardMode cardColorMode) {
        int cardColorModePixel = image.getRGB(45, 30);
        if (cardColorMode == EnumCardMode.Darker) {
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

    private static String getCardValue(BufferedImage image)
            throws IOException {
        String card;
        EnumCardColors cardColor = getCardColor(image);
        if (!cardColor.equals(EnumCardColors.Red) && !cardColor.equals(EnumCardColors.Black)) {
            return "-";
        }
        EnumCardMode colorMode = getColorMode(image);
        BufferedImage whiteImage = convertImgToLight(image, colorMode);
        BufferedImage cardValueBW = convertImageToBW(whiteImage.getSubimage(2, 5, 35, 25));
        String imageBinaryString = convertToBinaryString(cardValueBW);
        String findSymbol = "?";
        int differs = -1;
        int min = 100;
        for (Map.Entry<String, String> entry : CardShapes.entrySet()) {
            differs = compareValue(imageBinaryString, entry.getValue());
            if (differs < min) {
                min = differs;
                findSymbol = entry.getKey();
            }
        }
        if (Validation) {
            System.out.printf("Differs: %d%s", min, eol);
            if (min > minDiffer) {
                System.out.println("Warning! Differ больше минимального!");
            }
        }
        card = findSymbol;
        if (Learn) {
            System.out.printf("Please validate image. This is %s?%s", findSymbol, eol);
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
        return card;
    }

    private static BufferedImage convertImageToBW(BufferedImage image) {
        BufferedImage cardValueIgmBw =
                new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D graphics = cardValueIgmBw.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        return cardValueIgmBw;
    }

    private static String convertToBinaryString(BufferedImage image) {
        short whiteColor = -1;
        StringBuilder binaryString = new StringBuilder();
        for (short y = 1; y < image.getHeight(); y++) {
            for (short x = 1; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                binaryString.append(rgb == whiteColor ? "@" : "*");
                if (Learn || Validation) {
                    System.out.printf("%s", rgb == whiteColor ? "@" : "*");
                }
            }
            if (Learn || Validation) {
                System.out.println("");
            }
        }
        return binaryString.toString();
    }

    private static int compareValue(String targetStr, String sourceStr) {
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

    private static void printBinaryImage(String string) {
        System.out.println("is see:");
        char[] chars = string.replace(eol, "").toCharArray();
        for (int i = 1; i <= chars.length; i++) {
            System.out.printf("%c", chars[i - 1]);
            if (i % 34 == 0) {
                System.out.println("");
            }
        }
        System.out.printf("This code: %s%s", string, eol);
    }

    private static void setValidatedResults(Path screenshotFilePath) throws IOException {
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

    private static void loadCsvToModel(String fileName, EnumDataType enumDataType)
            throws IOException {
        final BufferedReader br = new BufferedReader(new FileReader(fileName));
        while (br.ready()) {
            if (EnumDataType.Cards.equals(enumDataType)) {
                loadCsvToMap(new CardValue(br.readLine()));
            }
            if (EnumDataType.Results.equals(enumDataType)) {
                loadCsvToMap(new Result(br.readLine()));
            }
        }
        if (Validation) {
            long lenght = 0;
            if (EnumDataType.Cards.equals(enumDataType)) {
                lenght = (long) CardShapes.entrySet().size();
            }
            if (EnumDataType.Results.equals(enumDataType)) {
                lenght = (long) ValidatedResults.entrySet().size();
            }
            System.out.printf("Validated %s in model: %d%s", enumDataType, lenght, eol);
        }
    }

    private static void saveModelToCsv(HashMap<String, String> hashMap, String fileName)
            throws IOException {
        try (Writer writer = new FileWriter(fileName)) {
            writer.write("");
            for (HashMap.Entry<String, String> entry : hashMap.entrySet()) {
                writer.append(entry.getKey()).append(';').append(entry.getValue()).append(eol);
            }
        }
    }

    private static void loadCsvToMap(Object obj) {
        if (obj instanceof Result) {
            ValidatedResults.put(((Result) obj).getFileName(), ((Result) obj).getRecognizedValue());
        }
        if (obj instanceof CardValue) {
            CardShapes.put(((CardValue) obj).getName(), ((CardValue) obj).getHash());
        }
    }

    private static class CardValue {
        String name;
        String hash;

        public CardValue(String line) {
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

    private enum EnumCardMode {
        Darker, // ▓
        Normal // ⬜️
    }

    private enum EnumDataType {
        Cards,
        Results

    }
}
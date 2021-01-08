package com.xsa;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Main {

    // options
    static boolean Debug = false;
    static boolean Learn = false;
    static String DefaultPath = "/Users/xsa-osx/Downloads/java_test_task/imgs/onlyfive";
    static int FilesLimit = 50;

    // statistics
    static boolean Validation = false;
    static int Valid = 0;
    static int RecognizeError = 0;
    static int AllItems = 0;

    // model
    static Map<String, String> CardNames = new HashMap<>();
    static Map<Integer, EnumCardColors> CardCollors = new HashMap<>() {{
        put(-14474458, EnumCardColors.Black);
        put(-15724526, EnumCardColors.Black); //  (dark)
        put(-3323575, EnumCardColors.Red);
        put(-10477022, EnumCardColors.Red); //  (dark)
        put(-1, EnumCardColors.White);
        put(-8882056, EnumCardColors.White); // (dark)
        put(-14013910, EnumCardColors.empty);
        put(-14474461, EnumCardColors.empty);
        put(-678365, EnumCardColors.yellow); // (dark?)
    }};
    static Map<EnumCardColors, Point> CheckPixelCoordinate = new HashMap<>() {{
        put(EnumCardColors.Black, new Point(33, 60)); // для сравнения по крестям
        put(EnumCardColors.Red, new Point(42, 54)); // для сравнения по сердцам
    }};

    public static void main(String[] args) throws IOException, FileNotFoundException {
        String path = null;
        if (args.length == 0) {
            path = DefaultPath;
            System.out.printf("Using default path: %s\r\n", path);
        }
        if (args.length > 0) {
            if (args[0].equals(null)) {
                throw new FileNotFoundException("Path not passed!");
            } else {
                path = args[0];
                System.out.printf("Using path: %s\r\n", path);
                try {
                    // TODO
                    FilesLimit = Integer.parseInt(args[1]);
                    Debug = Boolean.parseBoolean(args[2]);
                    Learn = Boolean.parseBoolean(args[3]);
                    Validation = Boolean.parseBoolean(args[4]);
                } catch (Exception exception) {
                }
            }
        }

        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            paths
                    .filter(Files::isRegularFile).skip(240).filter(object -> object.toString().endsWith(".png")).limit(FilesLimit).forEach(
                    pokerTableScreenshot -> {
                        try {
                            cardNamesLoadHashMap();
                            System.out.println(pokerTableScreenshot.getFileName() + " - " + getRecognizedStringForFullImage(pokerTableScreenshot).replace("--", ""));
                            if (Learn) {
                                cardSaveHashMapToCsv(CardNames);
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            );
        }

        if (Validation) {
            System.out.printf("Statistics: \r\nAllFiles: %d, Valid: %d, RecognizeErrors: %d. ", AllItems, Valid, RecognizeError);
        }
    }

    private static BufferedImage convertImageToBW(BufferedImage image) {
        BufferedImage cardNameIgmBwImg = new BufferedImage(
                image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D graphics = cardNameIgmBwImg.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        return cardNameIgmBwImg;
    }

    private static EnumCardColorMode getCardColorMode(BufferedImage image) {
        // card color mode
        EnumCardColorMode cardColorMode = EnumCardColorMode.Normal;

        int cardColorModePixel = image.getRGB(45, 30);
        int cardColorMixedMarker = -8882056;
        if (cardColorModePixel == cardColorMixedMarker) {
            cardColorMode = EnumCardColorMode.Darked;
        }

        return cardColorMode;
    }

    private static BufferedImage convertToLitedMode(BufferedImage image, EnumCardColorMode cardColorMode) {
        // convert to black and white
        int cardColorModePixel = image.getRGB(45, 30);

        if (cardColorMode == EnumCardColorMode.Darked) {
            // убираем попиксельно цвет
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

    private static String getCardnameForCardImage(BufferedImage image, int number, Path object) throws IOException {
        String card = null;
        EnumCardColors cardCollor = getColorForPoint(image);

        if (!cardCollor.equals(EnumCardColors.Red) && !cardCollor.equals(EnumCardColors.Black)) {
            return "-";
        }

        EnumCardColorMode colorMode = getCardColorMode(image);
        image = convertToLitedMode(image, colorMode);

        BufferedImage cardName = image.getSubimage(2, 3, 40, 30);
        BufferedImage cardNameBW = convertImageToBW(cardName);
        File cardNameIgmBW = new File(String.format(".//output//сrop_%s_%d_name_BW.png", object.getFileName(), number));
        ImageIO.write(cardNameBW, "png", cardNameIgmBW);
        String imageHash = getBinaryStringForPixels(cardNameBW);
        int min = 150;
        String findSymbol = "?";
        for (Map.Entry<String, String> entry : CardNames.entrySet()) {
            int differs = StringCompareFunction(imageHash.toString(), entry.getValue());
            if (differs < min) {
                min = differs;
                findSymbol = entry.getKey();
            }
            System.out.println("Symbol differ > 150;");
            // TODO learn here in else?
        }
        card = findSymbol;

        if (Learn) {
            // open file
            try {
                Desktop desktop = null;
                if (Desktop.isDesktopSupported()) {
                    desktop = Desktop.getDesktop();
                }
                desktop.open(cardNameIgmBW);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            // valid or not
            System.out.printf("Plese validate image. This is %s?\r\n", findSymbol);
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String answer = br.readLine();
            if (answer.toLowerCase().equals("y") || answer.toLowerCase().equals("")) {
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

    public static int StringCompareFunction(String targetStr, String sourceStr) {
        int m = targetStr.length(), n = sourceStr.length();
        int[][] delta = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++)
            delta[i][0] = i;
        for (int j = 1; j <= n; j++)
            delta[0][j] = j;
        for (int j = 1; j <= n; j++)
            for (int i = 1; i <= m; i++) {
                if (targetStr.charAt(i - 1) == sourceStr.charAt(j - 1))
                    delta[i][j] = delta[i - 1][j - 1];
                else
                    delta[i][j] = Math.min(delta[i - 1][j] + 1,
                            Math.min(delta[i][j - 1] + 1, delta[i - 1][j - 1] + 1));
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

    private static BufferedImage[] getCardVectorFromFullImage(BufferedImage full, Path object) throws IOException {
        // считываем цент экрана
        BufferedImage crop = full.getSubimage(120, 521, full.getWidth() - 220, 89);
        BufferedImage[] cardVector = new BufferedImage[5];

        // new variant for searching
        int serchForLeftX;
        int indexOfCard = 0;
        int _width = 63;
        for (int i = 0; i < full.getWidth() - 220; i++) {
            int color = crop.getRGB(i, crop.getHeight() / 2);
            EnumCardColors cardCollor = CardCollors.get(color);

            if (cardCollor == EnumCardColors.White || cardCollor == EnumCardColors.yellow) {
                // finded card
                // YELLOW HERE TOO!
                cardVector[indexOfCard] = crop.getSubimage(i, 0, _width, crop.getHeight());
                i += _width;
                indexOfCard++;
            }
        }

        File cropFile = new File(String.format(".//output//crop_%s", object.getFileName()));
        ImageIO.write(crop, "png", cropFile);

        // TODO ???
        if (Debug) {
            try {
                Desktop desktop = null;
                if (Desktop.isDesktopSupported()) {
                    desktop = Desktop.getDesktop();
                }
                desktop.open(cropFile);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        return cardVector;
    }

    private static String getRecognizedStringForFullImage(Path object) throws IOException {
        StringBuilder result = new StringBuilder();

        // считываем полную картинку
        BufferedImage img = ImageIO.read(object.toFile());

        int verticalOffset = 64; // это для тестов ставим 0, на нормальных данных нужно ставить 64

        BufferedImage full = img.getSubimage(0, verticalOffset, img.getWidth(), img.getHeight() - verticalOffset);
        File fullFile = new File(String.format(".//output//full_%s", object.getFileName()));

        BufferedImage[] cardVector = getCardVectorFromFullImage(full, object);

        for (int index = 0; index < Arrays.stream(cardVector).filter(o -> o != null).toArray().length; index++) {
            // get card cardSuit
            EnumCardMastes cardSuit = getCardSuitForCardImage(cardVector[index]);
            String card = getCardnameForCardImage(cardVector[index], index, object);
            result.append(card);
            result.append(cardSuit.toString().substring(0, 1).toLowerCase());
        }

        result.append("\r");

        if (Debug) {
            System.out.printf("File: %s, Result: %s\r\n", object.getFileName(), result);
        }

        if (Validation) {
            /*
                static boolean Validation = false;
                static int Valid = 0;
                static int RecognizeError = 0;
                static int AllItems = 0;
             */

            System.out.println("Validate recognition:");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String answer = br.readLine();
            if (answer.toLowerCase().equals("y") || answer.toLowerCase().equals("")) {
                Valid++;
            } else {
                RecognizeError++;
            }
            AllItems++;
        }

        return result.toString();
    }

    private static EnumCardColors getColorForPoint(BufferedImage image) {
        Point firstLayer = new Point(41, 69);
        int rgb_int = image.getRGB(firstLayer.x, firstLayer.y);
        EnumCardColors cardCollor = CardCollors.get(rgb_int);
        return cardCollor;
    }

    private static EnumCardMastes getCardSuitForCardImage(BufferedImage image) {
        EnumCardMastes mast = null;
        Point secondLayerPoint;

        EnumCardColors cardCollor = getColorForPoint(image);
        EnumCardColors secondLayerColor;
        switch (cardCollor) {
            case Black:
                secondLayerPoint = CheckPixelCoordinate.get(EnumCardColors.Black);
                secondLayerColor = CardCollors.get(image.getRGB(secondLayerPoint.x, secondLayerPoint.y));
                if (secondLayerColor == EnumCardColors.Black) {
                    mast = EnumCardMastes.Spades;
                } else {
                    mast = EnumCardMastes.Clubs;
                }
                break;
            case Red:
                secondLayerPoint = CheckPixelCoordinate.get(EnumCardColors.Red);
                secondLayerColor = CardCollors.get(image.getRGB(secondLayerPoint.x, secondLayerPoint.y));
                if (secondLayerColor == EnumCardColors.Red) {
                    mast = EnumCardMastes.Diamonds;
                } else {
                    mast = EnumCardMastes.Hearts;
                }
                break;
            case empty:
                mast = null;
                break;
            default:
                mast = null;
        }

        // TODO Исклюение Масть не распознанна!
        //if (mast == null) throw new LearNotFoundException();
        return mast;
    }

    private static void cardNamesLoadHashMap() throws IOException {
        final BufferedReader br = new BufferedReader(new FileReader("card_names.csv"));
        while (br.ready()) {
            cardHashLoadToMap(new CardName(br.readLine()));
        }
        if (Debug) {
            System.out.printf("Hashes in model: %d\r\n", CardNames.entrySet().stream().count());
        }
    }

    private static void cardHashLoadToMap(CardName cardName) {
        if (!CardNames.containsKey(cardName.getName())) {
            CardNames.put(cardName.getName(), cardName.getHash());
        }
    }

    private static void cardSaveHashMapToCsv(Map<String, String> map) {
        String eol = System.getProperty("line.separator");
        try (Writer writer = new FileWriter("card_names.csv")) {
            writer.write("");
            for (Map.Entry<String, String> entry : CardNames.entrySet()) {
                writer
                        .append(entry.getKey())
                        .append(';')
                        .append(entry.getValue())
                        .append(eol);
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

    private enum EnumCardMastes {
        Diamonds, // ♦️ Diamonds
        Hearts, // ♥️ Hearts
        Spades, // ♠️ Spades
        Clubs, // ♣️ Clubs
    }

    private enum EnumCardColorMode {
        Darked, // ▓
        Normal  // ⬜️
    }
}

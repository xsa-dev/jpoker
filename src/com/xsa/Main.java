package com.xsa;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Main {

    static final int filesLimit = 50;
    static final boolean debug = true;
    static final boolean to_csv = true;
    static Map<String, String> cardNamesMap = new HashMap<>();
    static Map<Integer, enumCardColors> CardCollors = new HashMap<>() {{
        put(-14474458, enumCardColors.Black);
        put(-15724526, enumCardColors.Black); //  (dark)
        put(-3323575, enumCardColors.Red);
        put(-10477022, enumCardColors.Red); //  (dark)
        put(-1, enumCardColors.White);
        put(-8882056, enumCardColors.White); // (dark)
        put(-14013910, enumCardColors.empty);
        put(-14474461, enumCardColors.empty);
    }};

    public static void main(String[] args) throws IOException, FileNotFoundException {
        String path = null;
        if (args.length == 0) {
            path = "/Users/xsa-osx/Downloads/java_test_task/imgs/onlyfive";
            System.out.printf("Using default path: %s\r\n", path);
        }
        if (args.length > 0) {
            if (args[0].equals(null)) {
                throw new FileNotFoundException("Path not passed!");
            } else {
                path = args[0];
                System.out.printf("Using path: %s\r\n", path);
                filesLimit = args[1];
            }
        }


        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            paths
                    .filter(Files::isRegularFile).filter(object -> object.toString().endsWith(".png")).limit(filesLimit).forEach(
                    object -> {
                        try {
                            loadCardNamesHashes();
                            System.out.println(object.getFileName() + " - " + recognize(object).replace("--", ""));
                            saveHashMap(cardNamesMap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            );
        }
    }

    private static void loadCardNamesHashes() throws IOException {
        final BufferedReader br = new BufferedReader(new FileReader("card_names.csv"));
        while (br.ready()) {
            add(new CardName(br.readLine()));
        }
        if (debug) {
            System.out.printf("Hashes in model: %d\r\n", cardNamesMap.entrySet().stream().count());
        }
    }

    private static void add(CardName cardName) {
        if (!cardNamesMap.containsKey(cardName.getHash())) {
            cardNamesMap.put(cardName.getHash(), cardName.getName());
        }
    }

    static class CardName {
        String name;
        String hash;

        public CardName(String line) {
            this.hash = line.split(";")[0];
            this.name = line.split(";")[1];
        }

        public String getName() {
            return this.name;
        }

        public String getHash() {
            return this.hash;
        }
    }

    private static void saveHashMap(Map<String, String> map) {
        String eol = System.getProperty("line.separator");
        try (Writer writer = new FileWriter("card_names.csv")) {
            writer.write("");
            for (Map.Entry<String, String> entry : cardNamesMap.entrySet()) {
                writer.append(entry.getKey())
                        .append(';')
                        .append(entry.getValue())
                        .append(eol);
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }

    private static String recognize(Path object) throws IOException {
        // подготавливаем резутат
        StringBuilder result = new StringBuilder();

        // считываем полную картинку
        BufferedImage img = ImageIO.read(object.toFile());
        int verticalOffset = 64;
        BufferedImage full = img.getSubimage(0, verticalOffset, img.getWidth(), img.getHeight() - verticalOffset);
        if (debug) {
            File fullFile = new File(String.format(".//output//full_%s", object.getFileName()));
            ImageIO.write(full, "png", fullFile);
        }

        // наполняем внутренний вектор изображений карт
        BufferedImage crop = full.getSubimage(140, 521, 356, 89);
        if (debug) {
            File cropFile = new File(String.format(".//output//%s", object.getFileName()));
            ImageIO.write(crop, "png", cropFile);
        }
        int offset = 3; // проскок
        int width = 65; // ширина карты (только белое, тень карты)
        int scip = 8 - 1; // ширина черного заполнения между карт без теней
        BufferedImage[] cardVector = new BufferedImage[5];
        for (int i = 0; i < 5; i++) {
            cardVector[i] = crop.getSubimage(offset, 0, width - 2, crop.getHeight());
            offset += width + scip;
        }

        for (int number = 0; number < cardVector.length; number++) {
            File name = new File(String.format(".//output//сrop_%s_%d.png", object.getFileName(), number));
            if (debug) {
                ImageIO.write(cardVector[number], "png", name);
            }

            // TODO вынести во внешний метод
            // check for color maste
            //region getMastForCardItem
            Point firstLayer = new Point(41, 69);
            int rgb_int = cardVector[number].getRGB(firstLayer.x, firstLayer.y);
            enumCardColors cardCollor = CardCollors.get(rgb_int);
            Map<enumCardColors, Point> CheckPixelCoordinate = new HashMap<>();
            CheckPixelCoordinate.put(enumCardColors.Black, new Point(33, 60)); // для сравнения по крестям
            CheckPixelCoordinate.put(enumCardColors.Red, new Point(42, 54)); // для сравнения по сердцам

            boolean ifWhiteCollorInPallet = whiteCollorInPallet(cardVector, number, cardCollor);
            Point secondLayerPoint;
            enumCardMastes mast = null;
            enumCardColors secondLayerColor;
            switch (cardCollor) {
                case Black:
                    secondLayerPoint = CheckPixelCoordinate.get(enumCardColors.Black);
                    secondLayerColor = CardCollors.get(cardVector[number].getRGB(secondLayerPoint.x, secondLayerPoint.y));
                    if (secondLayerColor == enumCardColors.Black) {
                        mast = enumCardMastes.Spades;
                    } else {
                        mast = enumCardMastes.Clubs;
                    }
                    break;
                case Red:
                    secondLayerPoint = CheckPixelCoordinate.get(enumCardColors.Red);
                    secondLayerColor = CardCollors.get(cardVector[number].getRGB(secondLayerPoint.x, secondLayerPoint.y));
                    if (secondLayerColor == enumCardColors.Red) {
                        mast = enumCardMastes.Diamonds;
                    } else {
                        mast = enumCardMastes.Hearts;
                    }
                    break;
                case empty:
                    mast = null;
                    break;
                default:
                    System.out.println("???");
            }
            //endregion

            // TODO вынести в разные методы
            // check from hashmap or differ
            //region getCardnameForCardItem
            String card = "-";
            if (ifWhiteCollorInPallet) {
                // выбираем изображение в примерной области значения карты как вариант поиск изменённого цвета пикселя до отличного цвета от белого или тёмно-белого
                int cardNameOffsetX = 0;
                BufferedImage cardName = cardVector[number].getSubimage(5, 5, 40, 25);

                if (debug) {
                    File cardNameIgm = new File(String.format(".//output//сrop_%s_%d_name.png", object.getFileName(), number));
                    ImageIO.write(cardName, "png", cardNameIgm);
                }

                Point leftToRightBottomToTop = new Point(0, 0);
                boolean finded_left = false;
                int minX = 100;
                int maxX = 0;
                int minY = 100;
                int maxY = 0;

                // еще может быть желтый
                // тут заполняем переменные :
                // самый верхний цветной пиксель
                // самый нижний цветной пиксель
                // самый левый цветной пиксель
                // самый правый цветной пиксель

                // курсор идёт сверху вниз, слева на право
                for (int y = 0; y < cardName.getHeight(); y++) {
                    for (int x = 0; x < cardName.getWidth(); x++) {
                        int pixelColor = cardName.getRGB(x, y);
                        enumCardColors colorOfCard = CardCollors.get(pixelColor);
                        if ((colorOfCard == enumCardColors.Black) || (colorOfCard == enumCardColors.Red)) {
                            if (minX > x) {
                                minX = x;
                            } else if (maxX < x) {
                                maxX = x;
                            }
                            if (minY > y) {
                                minY = y;
                            } else if (maxY < y) {
                                maxY = y;
                            }
                        }
                    }
                }

                // TODO ПОСМОТРИ КАРТИНКИ ОТ КРАЙНЕЙ ЛЕВОЙ ТОЧКИ ПАРУ ПИКСЕЛЕВ ЛЕВОВВЕРХ ?И ПАРУ ПИКСЕЛЕЙ ОТ КРАЙНЕЙ ПРАВОЙ ВНИЗ

                if (false) {
                    System.out.printf("MinX: %d, MaxX: %d, MinY: %d, MaxY: %d\r\n", minX, maxX, minY, maxY);
                }

                BufferedImage cardNameSubimage = cardName.getSubimage(minX, minY, maxX - minX, maxY - minY);
                if (false) {
                    File cardNameIgm = new File(String.format(".//output//сrop_%s_%d_nameRRR.png", object.getFileName(), number));
                    ImageIO.write(cardNameSubimage, "png", cardNameIgm);
                }


                if (debug) {
                    Color color = new Color(0, 255, 0);
                    cardName.setRGB(maxX, maxY, color.getRGB());
                    cardName.setRGB(minX, minY, color.getRGB());
                    File cardNameIgm = new File(String.format(".//output//сrop_%s_%d_name.png", object.getFileName(), number));
                    ImageIO.write(cardName, "png", cardNameIgm);
                }

                // card color mode
                enumCardColorMode cardColorMode = enumCardColorMode.Normal;
                int cardColorModePixel = cardVector[number].getRGB(45, 30);
                int cardColorMixedMarker = -8882056;
                if (cardColorModePixel == cardColorMixedMarker) {
                    cardColorMode = enumCardColorMode.Darked;
                }
                // convert to black and white
                if (cardColorMode == enumCardColorMode.Darked) {
                    // убираем попиксельно цвет
                    for (int y = 0; y < cardName.getHeight(); y++) {
                        for (int x = 0; x < cardName.getWidth(); x++) {
                            int pixelColor = cardName.getRGB(x, y);
                            boolean isDarkColor = (pixelColor == cardColorModePixel);
                            if (isDarkColor) {
                                cardName.setRGB(x, y, -1);
                            } else {
                                cardName.setRGB(x, y, pixelColor);
                            }
                        }
                    }
                }

                File cardNameIgmBW = new File(String.format(".//output//сrop_%s_%d_name_BW.png", object.getFileName(), number));
                BufferedImage cardNameIgmBwImg = new BufferedImage(
                        cardNameSubimage.getWidth(), cardNameSubimage.getHeight(),
                        BufferedImage.TYPE_BYTE_BINARY);
                Graphics2D graphics = cardNameIgmBwImg.createGraphics();
                graphics.drawImage(cardNameSubimage, 0, 0, null);

                if (debug) {
                    ImageIO.write(cardNameIgmBwImg, "png", cardNameIgmBW);
                }

                // сохранение в мапу значений изображения
                String imageHash = encodeToString(cardNameIgmBwImg, "png");
                if (cardNamesMap.get(imageHash) == null) {
                    // здесь начинаем обучать модель
                    card = "?";
                    try {
                        Desktop desktop = null;
                        if (Desktop.isDesktopSupported()) {
                            desktop = Desktop.getDesktop();
                        }
                        desktop.open(cardNameIgmBW);
                        System.out.println("Plese validate image:");
                        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                        String answer = br.readLine();
                        cardNamesMap.put(imageHash, answer.trim().toUpperCase());
                        card = answer.trim().toUpperCase();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                    if (debug) {
                        System.out.printf("%s - %s\r\n", cardNameIgmBW, imageHash);
                    }
                } else {
                    if (debug) {
                        if (debug) {
                            System.out.printf("%s - %s\r\n", cardNameIgmBW, cardNamesMap.get(imageHash));
                        }
                    }
                    card = cardNamesMap.get(imageHash);
                    File rCard = new File(String.format(".//output//_%s//%d%s-%s-%s.png", card, number, cardColorMode, card, imageHash.replace("/", "")));
                    if (true) {
                        ImageIO.write(cardNameIgmBwImg, "png", rCard);
                    }
                }
            }
            //endregion

            result.append(card);
            String mast_string = mast == null ? "-" : mast.toString();
            result.append(mast_string.substring(0, 1).toLowerCase());
        }
        result.append("\r");
        if (debug) {
            System.out.printf("File: %s, Result: %s", object.getFileName(), result);
            System.out.println("@@@@@@@@@@@@@@@@@");
        }
        return result.toString();
    }

    private static boolean whiteCollorInPallet(BufferedImage[] imgs, int number, enumCardColors cardCollor) {
        boolean isColorInPallet = cardCollor.equals(enumCardColors.Black) || cardCollor.equals(enumCardColors.Red);
        if (isColorInPallet) {
            int prepWhite = imgs[number].getRGB(45, 30);
            if (CardCollors.get(prepWhite) == null) {
                System.out.printf("White: %d\r\n", prepWhite);
            }
        }
        return isColorInPallet;
    }

    public static String encodeToString(BufferedImage image, String type) {
        String imageString = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            ImageIO.write(image, type, bos);
            byte[] imageBytes = bos.toByteArray();

            Base64.Encoder encoder = Base64.getEncoder();
            imageString = encoder.encodeToString(imageBytes);

            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageString;
    }

    public static class Point {
        int x;
        int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public enum enumCardColors {
        Black,
        Red,
        White,
        empty
    }

    public enum enumCardMastes {
        Diamonds, // ♦️ Diamonds
        Hearts, // ♥️ Hearts
        Spades, // ♠️ Spades
        Clubs, // ♣️ Clubs
    }

    public enum enumCardColorMode {
        Darked,
        Normal
    }
}

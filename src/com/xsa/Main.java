package com.xsa;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {

    // options
    static boolean Debug = false;
    static String DefaultPath = "/Users/xsa-osx/Downloads/java_test_task/imgs/onlyfive";
    static int FilesLimit = 50;

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
                FilesLimit = Integer.parseInt(args[1]);
            }
        }

        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            paths
                    .filter(Files::isRegularFile).filter(object -> object.toString().endsWith(".png")).limit(FilesLimit).forEach(
                    object -> {
                        try {
                            cardNamesLoadHashMap();
                            System.out.println(object.getFileName() + " - " + getRecognizedStringForFullImage(object).replace("--", ""));
                            cardSaveHashMapToCsv(CardNames);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            );
        }
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
        if (!CardNames.containsKey(cardName.getHash())) {
            CardNames.put(cardName.getHash(), cardName.getName());
        }
    }

    private static void cardSaveHashMapToCsv(Map<String, String> map) {
        String eol = System.getProperty("line.separator");
        try (Writer writer = new FileWriter("card_names.csv")) {
            writer.write("");
            for (Map.Entry<String, String> entry : CardNames.entrySet()) {
                writer.append(entry.getKey())
                        .append(';')
                        .append(entry.getValue())
                        .append(eol);
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }

    private static String getCardnameForCardImageEnchanced(BufferedImage image, int number, Path object) throws IOException {
        // вообще получается что нужно
        // первым слоем получать все картинки начиная с первого:
        // - тени белого,
        // - тени затемнённого белого,
        // - тени желтого,
        // - тени затемнённого желтого
        // выбираем изображение в примерной области значения карты как вариант поиск изменённого цвета пикселя до отличного цвета от белого или тёмно-белого

        // еще может быть желтый
        // тут заполняем переменные :
        // самый верхний цветной пиксель
        // самый нижний цветной пиксель
        // самый левый цветной пиксель
        // самый правый цветной пиксель

        // TODO ПОСМОТРИ КАРТИНКИ ОТ КРАЙНЕЙ ЛЕВОЙ ТОЧКИ ПАРУ ПИКСЕЛЕВ ЛЕВОВВЕРХ ?И ПАРУ ПИКСЕЛЕЙ ОТ КРАЙНЕЙ ПРАВОЙ ВНИЗ

        if (Debug) {
            // Color color = new Color(0, 255, 0);
            // cardName.setRGB(maxX, maxY, color.getRGB());
            // cardName.setRGB(minX, minY, color.getRGB());
            // File cardNameIgm = new File(String.format(".//output//сrop_%s_%d_name.png", object.getFileName(), number));
            // ImageIO.write(cardName, "png", cardNameIgm);
        }

        return "z";
    }

    private static String getCardnameForCardImage(BufferedImage image, int number, Path object) throws IOException {
        String card = null;
        EnumCardColors cardCollor = getColorForPoint(image);
        if (!cardCollor.equals(EnumCardColors.Red) && !cardCollor.equals(EnumCardColors.Black)) {
            return "-";
        }

        int cardNameOffsetX = 5;
        BufferedImage cardName = image.getSubimage(cardNameOffsetX, 5, 40, 25);

        if (Debug) {
            File cardNameIgm = new File(String.format(".//output//сrop_%s_%d_name.png", object.getFileName(), number));
            ImageIO.write(cardName, "png", cardNameIgm);
        }

        Point leftToRightBottomToTop = new Point(0, 0);
        boolean finded_left = false;
        int minX = 100;
        int maxX = 0;
        int minY = 100;
        int maxY = 0;

        // курсор идёт сверху вниз, слева на право
        for (int y = 0; y < cardName.getHeight(); y++) {
            for (int x = 0; x < cardName.getWidth(); x++) {
                int pixelColor = cardName.getRGB(x, y);
                EnumCardColors colorOfCard = CardCollors.get(pixelColor);
                if ((colorOfCard == EnumCardColors.Black) || (colorOfCard == EnumCardColors.Red)) {
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

        if (false) {
            System.out.printf("MinX: %d, MaxX: %d, MinY: %d, MaxY: %d\r\n", minX, maxX, minY, maxY);
        }

        BufferedImage cardNameSubimage = cardName.getSubimage(minX, minY, maxX - minX, maxY - minY);
        if (false) {
            File cardNameIgm = new File(String.format(".//output//сrop_%s_%d_nameRRR.png", object.getFileName(), number));
            ImageIO.write(cardNameSubimage, "png", cardNameIgm);
        }

        // card color mode
        EnumCardColorMode cardColorMode = EnumCardColorMode.Normal;
        int cardColorModePixel = image.getRGB(45, 30);
        int cardColorMixedMarker = -8882056;
        if (cardColorModePixel == cardColorMixedMarker) {
            cardColorMode = EnumCardColorMode.Darked;
        }

        // convert to black and white
        if (cardColorMode == EnumCardColorMode.Darked) {
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

        if (Debug) {
            ImageIO.write(cardNameIgmBwImg, "png", cardNameIgmBW);
        }

        // сохранение в мапу значений изображения
        String imageHash = encodeImageToString(cardNameIgmBwImg, "png");
        if (CardNames.get(imageHash) == null) {
            // здесь начинаем обучать модель
            card = "?";
            try {
                Desktop desktop = null;
                if (Desktop.isDesktopSupported()) {
                    desktop = Desktop.getDesktop();
                }
                desktop.open(cardNameIgmBW);
                System.out.printf("%s - %s\r\n", cardNameIgmBW, CardNames.get(imageHash));
                System.out.println("Plese validate image:");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                String answer = br.readLine();
                CardNames.put(imageHash, answer.trim().toUpperCase());
                card = answer.trim().toUpperCase();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            if (Debug) {
                System.out.printf("%s - %s\r\n", cardNameIgmBW, imageHash);
            }
        } else {
            if (Debug) {
                if (Debug) {
                    System.out.printf("%s - %s\r\n", cardNameIgmBW, CardNames.get(imageHash));
                }
            }
            card = CardNames.get(imageHash);
            File rCard = new File(String.format(".//output//_%s//%d%s-%s-%s.png", card, number, cardColorMode, card, imageHash.replace("/", "")));
            if (Debug) {
                ImageIO.write(cardNameIgmBwImg, "png", rCard);
            }
        }

        return card;
    }

    private static String getRecognizedStringForFullImage(Path object) throws IOException {
        // подготавливаем резутат
        StringBuilder result = new StringBuilder();

        // считываем полную картинку
        BufferedImage img = ImageIO.read(object.toFile());
        int verticalOffset = 64;
        BufferedImage full = img.getSubimage(0, verticalOffset, img.getWidth(), img.getHeight() - verticalOffset);
        File fullFile = new File(String.format(".//output//full_%s", object.getFileName()));
        if (Debug) {
            ImageIO.write(full, "png", fullFile);
        }

        // наполняем внутренний вектор изображений карт
        BufferedImage crop = full.getSubimage(140, 521, 356, 89);
        if (Debug) {
            File cropFile = new File(String.format(".//output//crop_%s", object.getFileName()));
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

        for (int index = 0; index < cardVector.length; index++) {
            File name = new File(String.format(".//output//сrop_%s_%d.png", object.getFileName(), index));
            if (Debug) {
                ImageIO.write(cardVector[index], "png", name);
            }

            // check for color maste
            EnumCardMastes mast = getMastForCardImage(cardVector[index]);

            String card = getCardnameForCardImage(cardVector[index], index, object);

            //endregion
            result.append(card);
            String mast_string = mast == null ? "-" : mast.toString();

            String patternString = "[A-z0-9]{8,14}";
            Pattern pattern = Pattern.compile(patternString);
            if (Pattern.matches(patternString, result.toString())) {
                System.out.println("%$%$%$%$%$%$%$%$%$%$%$%$");
                Files.copy(fullFile.toPath(), new File(String.format(".//output//hard//%s", object.getFileName())).toPath());
            }

            result.append(mast_string.substring(0, 1).toLowerCase());
        }

        result.append("\r");
        if (Debug) {
            System.out.printf("File: %s, Result: %s", object.getFileName(), result);
            System.out.println("@@@@@@@@@@@@@@@@@");
        }
        return result.toString();
    }

    private static String encodeImageToString(BufferedImage image, String type) {
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

    private static EnumCardColors getColorForPoint(BufferedImage image) {
        Point firstLayer = new Point(41, 69);
        int rgb_int = image.getRGB(firstLayer.x, firstLayer.y);
        EnumCardColors cardCollor = CardCollors.get(rgb_int);
        return cardCollor;
    }

    private static EnumCardMastes getMastForCardImage(BufferedImage image) {
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

    private static class CardName {
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
        empty
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

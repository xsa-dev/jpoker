package com.xsa;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws IOException {
        String path = "/Users/xsa-osx/Downloads/java_test_task/imgs/onlyfive";

        try (PrintWriter writer = new PrintWriter(new File(".//output//test.csv"))) {
            writer.write("");

            try (Stream<Path> paths = Files.walk(Paths.get(path))) {
                paths
                        .filter(Files::isRegularFile).filter(object -> object.toString().endsWith(".png")).forEach(
                        object -> {
                            try {
                                StringBuilder sb = new StringBuilder();
                                String result = recognize(object); // << TODO
                                writer.append(object.getFileName() + ";" + result);
//                                System.out.println("done");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                );
            } catch (FileNotFoundException e) {
                System.out.println(e.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static String recognize(Path object) throws IOException {
        /*
        Для решения задачи рекомендуется использовать следующие функции, встроенные в Java:
        - BufferedImage img = ImageIO.read(f); - зачитка картинки из файла
        - ImageIO.write(img, "png", f); - запись картинки в файл
        - img.getWidth(); img.getHeight(); - рамеры картинки
        - BufferedImage img1 = img.getSubimage(x, y, w, h); - взятие области в картинке
        - img.getRGB(x, y); - взятие цвета точки по координате
        - Color c = new Color(img.getRGB(x, y)); c.getRed(); c.getGreen(); c.getBlue(); c.equals(c1) - работа с цветом точки

        // MY
        // Y - width
        // X - height

        // для каждой картинки нужно
        // определить размер внутреннего экрана и
        // смещать его на каждой итерации на ширину внутреннего экрана

        */
        BufferedImage img = ImageIO.read(object.toFile());
        // экран не строго по середине
        // нужно добавить оффсеты для выравнивания
        int verticalOffset = 64;
        BufferedImage full = img.getSubimage(0, verticalOffset, img.getWidth(), img.getHeight() - verticalOffset);
        File fullFile = new File(String.format(".//output//full_%s", object.getFileName()));
        ImageIO.write(full, "png", fullFile);

        // размеры внутреннего экрана
        int innerScreenW = 300;
        int innerScreenH = 0;

        BufferedImage crop = full.getSubimage(133 + 7, 495 + 26, 374 - 18, 99 - 10);
        // крайний левый угол должен строго делать всё ровно
        // test item: сrop_20180821_102328.773_0x1FE201D8.png
        File cropFile = new File(String.format(".//output//сrop_%s", object.getFileName()));
        ImageIO.write(crop, "png", cropFile);

        int offset = 3; // проскок
        int width = 65; // ширина карты (только белое, тень карты)
        int scip = 8 - 1; // ширина черного заполнения между карт без теней

        BufferedImage[] imgs = new BufferedImage[5];

        for (int i = 0; i < 5; i++) {
            imgs[i] = crop.getSubimage(0 + offset, 0, width - 2, crop.getHeight());
            offset += width + scip;
        }

        StringBuilder result = new StringBuilder();

        for (int j = 0; j < imgs.length; j++) {
            File name = new File(String.format(".//output//сrop_%s_%d.png", object.getFileName(), j));

            ImageIO.write(imgs[j], "png", name);
            // TODO: card maste?
            // масти: ️ ️
            // ♦️ Diamonds
            // ♥️ Hearts
            // ♠️ Spades
            // ♣️ Clubs
            Map<Integer, enumCardColors> CardCollors = new HashMap<>();

            CardCollors.put(-14474458, enumCardColors.Black);
            CardCollors.put(-15724526, enumCardColors.Black); //  (dark)
            CardCollors.put(-3323575, enumCardColors.Red);
            CardCollors.put(-10477022, enumCardColors.Red); //  (dark)
            CardCollors.put(-1, enumCardColors.White);
            CardCollors.put(-8882056, enumCardColors.White); // (dark)
            CardCollors.put(-14013910, enumCardColors.empty);
            CardCollors.put(-14474461, enumCardColors.empty);

            Point firstLayer = new Point(41, 69);

            // check for color maste
            enumCardColors cardCollor = CardCollors.get(imgs[j].getRGB(firstLayer.x, firstLayer.y));
            Map<enumCardColors, Point> CheckPixelCoordinate = new HashMap<>();
            CheckPixelCoordinate.put(enumCardColors.Black, new Point(33, 60)); // для сравнения по крестям
            CheckPixelCoordinate.put(enumCardColors.Red, new Point(42, 54)); // для сравнения по сердцам

            // UTIL
            if (cardCollor.equals(enumCardColors.Black) || cardCollor.equals(enumCardColors.Red)) {
                int white = imgs[j].getRGB(45, 30);
                if (CardCollors.get(white) == null) {
                    System.out.printf("White: %d\r\n", white);
                }
            }

            Point secondLayerPoint = null;
            enumCardMastes mast = null;
            enumCardColors secondLayerColor = null;

            switch (cardCollor) {
                case Black:
                    secondLayerPoint = CheckPixelCoordinate.get(enumCardColors.Black);
                    secondLayerColor = CardCollors.get(imgs[j].getRGB(secondLayerPoint.x, secondLayerPoint.y));
                    if (secondLayerColor == enumCardColors.Black) {
                        mast = enumCardMastes.Spades;
                    } else {
                        mast = enumCardMastes.Clubs;
                    }
                    break;
                case Red:
                    secondLayerPoint = CheckPixelCoordinate.get(enumCardColors.Red);
                    secondLayerColor = CardCollors.get(imgs[j].getRGB(secondLayerPoint.x, secondLayerPoint.y));
                    if (secondLayerColor == enumCardColors.Red) {
                        mast = enumCardMastes.Diamonds;
                    } else {
                        mast = enumCardMastes.Hearts;
                    }
                    break;
                case empty:
                    mast = null;
                    secondLayerColor = enumCardColors.empty;
                    break;
                default:
                    System.out.println("???");
            }
            // TODO: card name?
            // числа: 2 : 10
            // карты: A Ja Q K Jo
            String card = "-";


            result.append(card);

            String mast_string = mast == null ? "-" : mast.toString();
            result.append(mast_string.toLowerCase());


        }

        result.append("\r\n");
        System.out.printf("File: %s, Result: %s", object.getFileName(), result);

        String targetOfCardSet = "4hQd7s";
        System.out.println("@@@@@@@@@@@@@@@@@");
        return result.toString();
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
}

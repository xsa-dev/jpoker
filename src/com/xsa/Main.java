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


    public static void main(String[] args) {
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
                                sb.append(String.format("%s;%s", object.getFileName(), result));
                                writer.append(sb);
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
        // для каждой картинки нужно
        // определить размер внутреннего экрана и
        // смещать его на каждой итерации на ширину внутреннего экрана
        */
        BufferedImage img = ImageIO.read(object.toFile());
        // экран не строго по середине
        // нужно добавить оффсеты для выравнивания
        int verticalOffset = 64;
        BufferedImage full = img.getSubimage(0, verticalOffset, img.getWidth(), img.getHeight() - verticalOffset);
        if (false) {
            File fullFile = new File(String.format(".//output//full_%s", object.getFileName()));
            ImageIO.write(full, "png", fullFile);
        }
        // размеры внутреннего экрана
        BufferedImage crop = full.getSubimage(133 + 7, 495 + 26, 374 - 18, 99 - 10);
        // крайний левый угол должен строго делать всё ровно
        // test item: сrop_20180821_102328.773_0x1FE201D8.png
        if (true) {
            File cropFile = new File(String.format(".//output//сrop_%s", object.getFileName()));
            ImageIO.write(crop, "png", cropFile);
        }
        int offset = 3; // проскок
        int width = 65; // ширина карты (только белое, тень карты)
        int scip = 8 - 1; // ширина черного заполнения между карт без теней
        BufferedImage[] imgs = new BufferedImage[5];
        for (int i = 0; i < 5; i++) {
            imgs[i] = crop.getSubimage(offset, 0, width - 2, crop.getHeight());
            offset += width + scip;
        }
        StringBuilder result = new StringBuilder();
        for (int j = 0; j < imgs.length; j++) {
            File name = new File(String.format(".//output//сrop_%s_%d.png", object.getFileName(), j));
            if (false) {
                ImageIO.write(imgs[j], "png", name);
            }
            // TODO: card maste?
            // масти: ️ ️
            // ♦️ Diamonds
            // ♥️ Hearts
            // ♠️ Spades
            // ♣️ Clubs
            Map<Integer, enumCardColors> CardCollors = new HashMap<>() {{
                put(-14474458, enumCardColors.Black);
                put(-15724526, enumCardColors.Black); //  (dark)
                put(-3323575, enumCardColors.Red);
                put(-10477022, enumCardColors.Red); //  (dark)
                put(-1, enumCardColors.White);
                put(-8882056, enumCardColors.White); // (dark)
                put(-14013910, enumCardColors.empty);
                put(-14474461, enumCardColors.empty);
            }};

            Point firstLayer = new Point(41, 69);

            // check for color maste
            int rgb_int = imgs[j].getRGB(firstLayer.x, firstLayer.y);
            enumCardColors cardCollor = CardCollors.get(rgb_int);
            Map<enumCardColors, Point> CheckPixelCoordinate = new HashMap<>();
            CheckPixelCoordinate.put(enumCardColors.Black, new Point(33, 60)); // для сравнения по крестям
            CheckPixelCoordinate.put(enumCardColors.Red, new Point(42, 54)); // для сравнения по сердцам

            // UTIL
            boolean b = cardCollor.equals(enumCardColors.Black) || cardCollor.equals(enumCardColors.Red);
            if (b) {
                int white = imgs[j].getRGB(45, 30);
                if (CardCollors.get(white) == null) {
                    System.out.printf("White: %d\r\n", white);
                }
            }

            Point secondLayerPoint;
            enumCardMastes mast = null;
            enumCardColors secondLayerColor;

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
                    break;
                default:
                    System.out.println("???");
            }
            // TODO: card name?
            // числа: 2 : 10
            // карты: A Ja Q K Jo
            String card = "-";

            if (b) {
                // выбираем изображение в примерной области значения карты как вариант поиск изменённого цвета пикселя до отличного цвета от белого или тёмно-белого
                int cardNameOffsetX = 0;

                // если тёмный режим карты
                CardCollors.put(-15724526, enumCardColors.Black); //  (dark)
                CardCollors.put(-3323575, enumCardColors.Red);

                // немного подравниваем, в следующей итерации можно убрать
                if (j == 3) {
                    cardNameOffsetX = -2;
                }

                BufferedImage cardName = imgs[j].getSubimage(5 + cardNameOffsetX, 5, 40, 25);
                File cardNameIgm = new File(String.format(".//output//сrop_%s_%d_name.png", object.getFileName(), j));
                if (false) {
                    ImageIO.write(cardName, "png", cardNameIgm);
                }

                // card color mode
                enumCardColorMode cardColorMode = enumCardColorMode.Normal;

                int cardColorModePixel = imgs[j].getRGB(45, 30);
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

                File cardNameIgmBW = new File(String.format(".//output//сrop_%s_%d_name_BW.png", object.getFileName(), j));
                BufferedImage cardNameIgmBwImg = new BufferedImage(
                        cardName.getWidth(), cardName.getHeight(),
                        BufferedImage.TYPE_BYTE_BINARY);

                Graphics2D graphics = cardNameIgmBwImg.createGraphics();
                graphics.drawImage(cardName, 0, 0, null);

                // сохранение в мапу значений изображения
                Map<String, String> cardNamesMap = new HashMap<>() {{
                    // A
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAS0lEQVR4XmP4DwIMUPIwEnkQTDYikRII8p9NPYKssweSf6CkPJD88R9BfmDgh5MP/7cDycf/jwPJwyBrGI7/fwwmHwLJdjAJcsh/AHmBaia69NKKAAAAAElFTkSuQmCC", "A");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAATElEQVR4XmP4DwIMKGQ7EskOJplB5D9lMGkIJgtB5J8KBPnjRz2I/AMiP0LIf/ZA8gEDhJSHk8f/84PJfiDZDyWPA0l+MPn//+3//wHZ8mlbx/FOKQAAAABJRU5ErkJggg==", "A");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUklEQVR4XmP4DwIMKGQ/mGRHkP8gpDKYTAaRfwoR5I8PYPIHiPz4ox5E/gGRn8HkAwZ7OHmAQR5IHv8PIvv/84NJoGUM/GBS/v/x/yA33P//HwDrPGjsRjG/6gAAAABJRU5ErkJggg==", "A");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAU0lEQVR4XmP4DwIMULIdTDaDSWYwyYgg/wmCSQMQ+ccCTFbUA8kfELLGHkh++AMm/4FJBnkg+QBK8gPJA2DyMMgahnYoeRxI9v8/DCT5weR/EAAAQEdkt2fwXAsAAAAASUVORK5CYII=", "A");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUUlEQVR4XmP4DwIMUPIwmGwGk41gkhGJFACR/yTApEU9kPwDIWvsgeQPCFkHIj/8kweRDBCSH0g+gJLsQPLw/3YweRxItoOshJL8/x/+h7oEACgMZHOKGnDNAAAAAElFTkSuQmCC", "A");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAS0lEQVR4XmP4DwIMUPIwEtmIRILkGf5zgMh/EvUI0sYeSP6BkHUg8kedPIisB5EfGPiRSHYg+QBMPv5/HEgeBlkDJdv/P4STYJcAAAxMZKVyeHD7AAAAAElFTkSuQmCC", "A");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUElEQVR4XmP4DwIMULIdTLKDSWYQ+Q+ZVASRfwzBZAGI/AEhf9QDyQ8Q8g+IfPjHHkg+YICQ8kDyAJTkB5Lt/yFkP5DkB1kJJeX/H/4PdQkAfaJkS+xayVcAAAAASUVORK5CYII=", "A");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAVUlEQVR4XmP4DwIMUPI4mDwMJpvBZCOYZASTAiDynwUSWVMPJP9AyDp7IPnjH4SUB5IfICQDiHzAwA8kH//vB5PtQPI4yDIgCbSMof//YzD58D/UJQChHGqRRKVFjwAAAABJRU5ErkJggg==", "A");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAWklEQVR4XmP4DwIMUPI4mGwHk81gkhlMMoLIf4Zg0gBMWoDIPxVgsqYeSP74AyHtgeTHfxBSHkh+YACRD8Dk4//8QPL4/34wCbSGoR9kJZA8DCT5/z/+D3UJAJKbaaW59mYOAAAAAElFTkSuQmCC", "A");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAVElEQVR4XmP4DwIMKORxMNkOJpvBJDOYFASR/wzBpAWYrACRfyBkTT2Q/PEHTP6zB5IfweQHBnkg+QBK8gPJw//7geRxKAm0jKEfSh4GkkD9//8DAP6cagyV6VsTAAAAAElFTkSuQmCC", "A");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAVElEQVR4XmP4DwIMUPIhmDwIJg8gkQ1gsqMeRPKASRl7IPkPQsrJg0g7EPnHnh9E1oPIHwwQkh1IfoCSzEDy4f/DYBJoGcNjMHn4/0cw+QFkMggAADtbZYfqj8zQAAAAAElFTkSuQmCC", "A");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAASklEQVR4XmP4DwIMKORhJLIRiZwAJiUQ5D+bejj5p8YeRNZBSHkg+eM/mGQAkR8Y+OHk4//tYPI4kDwMsobh+P/HcHL//7///wMAnTFpSmVt9uwAAAAASUVORK5CYII=", "A");

                    // K
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUElEQVR4XmP4DwIMEPLH/4dg8jCYbAeT/GBSHkT+sweRf+pB5A+w+o9g8iGYPAgmQSYx/JAAq7EBq68Bk3VgE6CmQUyGkBC7wPbWf/gPdQkAslpuPRDmByEAAAAASUVORK5CYII=", "K");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUElEQVR4XmP4DwIMEPLh/2YwyQ4i//GDyD/yIPKHPZisB5EfwCofgsnDYLIdTDKDyAeMYFIRTBaAyQ9gXRAT/oDJf2AzIeZD7ILaCzQO4hIAr+Jrc9PJGYIAAAAASUVORK5CYII=", "K");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUElEQVR4XmP4DwIMEPLH/4dg8jCYbAeT/CDynzyI/GMPIn/Ug8gPYPUPweRhMHkQTIJMYvghASYtwCprwLogJvyHkGAzoeZD7IKQINvBLgEA1wFvJ9ihYk0AAAAASUVORK5CYII=", "K");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUElEQVR4XmP4DwIMEPLD/4NgshlMsoPIf/wg8o88iPxhDyI/1INJsPqHYPIwmGwEk4xgUgBMWoBVVoB1QUz4Byb/g82EmA+16zCYBBoHcQkABpVsevXK8HIAAAAASUVORK5CYII=", "K");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUklEQVR4XmP4DwIMEPLH/8Ngsh1M9oNJeRD5zx5E/qkHkT/A5Aew+odg8jCYbAaTjGBSAExagFXWgMk/YBP+gU37zw8mIbZASJC9H/4DjYO4BADUsW8ZSgCTwAAAAABJRU5ErkJggg==", "K");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAU0lEQVR4XmP4DwIMEPJH/QMweRBMNoNJdhBZxw8ia+RBZIU9iCyoB5EJYF2OYNIBTIJMYvjBAZaVAKu0AeuqA5P1YHMgZkLNPwwmH4JJkO1glwAAe5Rj1XUUtAQAAAAASUVORK5CYII=", "K");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAU0lEQVR4XmP4DwIMEPLH/4dg8jCYbAeT/CDynzyI/GMPIn/Ug8gPYPUPweRBMNkIJkEmMfwQAKuxAJM1YF01YBP+gU2DmAk1vxlMHgSTD/5DXQIAgoJthxThM7YAAAAASUVORK5CYII=", "K");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAATklEQVR4XmP4DwIMEPLH/8dg8jiYbAeT8iDyH5j8Yw8if9SDyA9g9Q/B5GEw2QgmGcGkAJi0AKusAev6ByHB5vznRzIfYtdhMAk0DuISANJ1bxlCvkmPAAAAAElFTkSuQmCC", "K");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAT0lEQVR4XmP4DwIMEPKP/Qcw+RBMHgaT7WCSH0TayYNIG3sQaVEPIg3ApAJYL8gMIAkWYQCr4QGrlwOTEBPs2cFkM5L5ELsegMkP9VCXAABpoF+bWDoL3gAAAABJRU5ErkJggg==", "K");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAATElEQVR4XmP4DwIMEPJP/Qcw+RBMHgaT7WCSH0TWyYPIGnsQWVEPIgvAugrBZAKYZACL84BJGbBKO7AuiAlQ0yAkxHyIXRB7QSTYJQA1pGZ8KHn9BAAAAABJRU5ErkJggg==", "K");

                    // Q
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAWUlEQVR4XkXNuw2AMAwE0BMZwCN4FI+WiIK5sgmMkJLCisMlIK54xckfBIPXJrQmWkBTzYEuzQKuTs01cFuneRlL+T1nc8zJXblVqF72XXsu0wLlx402YcMMvqRXPQ0FnewAAAAASUVORK5CYII=", "Q");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAWElEQVR4Xj3NsRHAMAgDQHKXi8dgFEbDk2QWj5IRKF34wMiJo+ILFRIFQp/GsBVYj9RLg2zpkK5BXYek8eq/Fs7b3d/LE2oVLLRcGPzoWk6DSPB4QWM0yARAul+S4wfnawAAAABJRU5ErkJggg==", "Q");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAVUlEQVR4Xn3OsQ2AQAgFUExMaBzBeKMwGgzmMIxw5RWEk2/OVooXmv+BJoaW3qAxpK1MtjKal0O6ljrKPkP+vF9PmIcgRciK6dcWjD2JBBd36JeuTx6KgV1ROigWTQAAAABJRU5ErkJggg==", "Q");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAXUlEQVR4Xm3OsQ2AMAwEwJdACh0LIBjFoznsRZFRwgYpU0QxfkiJi5Ml2y/DWBjmg8bgdkxuC6dbJVMtaihaqX02Mdw/XsLpRvvyXkGZEKkkJq/sO+A7hpmmXccnD725XX29TgjsAAAAAElFTkSuQmCC", "Q");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAWUlEQVR4XmP4DwIMUPKDPIhsYAaRDIxA8h/7ASD5R/5DPZC0/wEkf9T/sYeRH/7/kQeR/zDIeWCyD6ySBUw2gEj7AyBz5B/Ug02uB9nCIA+yEUw+4LeHugQAETlbcLk9NOIAAAAASUVORK5CYII=", "Q");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAXElEQVR4Xk3OsRGAMAgFULwUlhkhozAaVq5lNnAEM0JKilyQjymkeA3/PpBhaKkFtgSPDe7NnVnFLYONBk8os0Bz9dN+yhPekTkhV0ZDg7lLNDOukG+tJqj5Wp+8ADJbzx93XlMAAAAASUVORK5CYII=", "Q");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAW0lEQVR4XmXMzQmAMAwF4AcKXgQXEFfJaOlkUjdxhPTWQ2nsi97M4SPk58FZ+DSheaGYhn27hk2MalVHffUmDguL959nbHfh5Uo1xW9Wpt2RzN6BsfU0Uzs4YT1afGAg75tWwQAAAABJRU5ErkJggg==", "Q");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAXUlEQVR4Xk3OMQ6AIBAEwE0stMPnWJjct+yIP8OfQPgAJQXhvCXEeMVkq92D8jANQuHMhtWsbqHymEWjGbWYSYun9TP/THrRftC2m7FjNAzlHs3MDfBc3Gg4dX7yAn2oYYc7G+hOAAAAAElFTkSuQmCC", "Q");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAVUlEQVR4Xl3OQQrAMAgEwIVeC36gb2meZvrTPCG95RDcuhAKrYdBZBVBFZatyGoSWxp2pbO0dHiXHE50zvT+Wv4eSsbu2oL0+l6bpj6gOasybCfXJw/zx2N6UnHdOQAAAABJRU5ErkJggg==", "Q");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAWUlEQVR4XkXNsQ3AIAwEwFcYwCN4FI+Gq8zFJskIlCkQhodEcXHFy34jOHitQkuiDppKDnSpFmjaqDUNPNZp3sZWfq+VnGvzUF451du+ttlMHcqP3JnfmXAGpSRWvVpchwwAAAAASUVORK5CYII=", "Q");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAWElEQVR4Xl3NsQ2AMAwEwJdokbwAs+DRnMEoYJOMELoUVowfAQVfXPV+Ixg8VqVFKKZ0yJG61rRbo9Et0MLT81X/brcLm2M2XoFa+dZcdu4Dyo/sRF0pcwHrB2K6onRbIQAAAABJRU5ErkJggg==", "Q");

                    // J
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAALElEQVR4XmP4DwIMUPJHPU3J32Dy4wcQ+eDBfyD5AWQzww9GEPmHHUT+BwEAIyBvoB3+eeAAAAAASUVORK5CYII=", "J");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAKElEQVR4XmP4DwIMUPJHPR3Izx/+A8kHD8AkI4j8wAwi/7CDyP8gAAAyyHARIOE7nwAAAABJRU5ErkJggg==", "J");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAKklEQVR4XmP4DwIMUPIHmPxAK/I7mHwMJg8eBJEPGEHkB2YQ+YcfrBIEAOHXd8NKoemZAAAAAElFTkSuQmCC", "J");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAK0lEQVR4XmP4DwIMUPJPPW3JHyDy84//QPLBBxD5gRFE/gCT/9hB5H8QAABORnCB2n4GMAAAAABJRU5ErkJggg==", "J");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAJklEQVR4XmP4DwIMUPIHPcjPH0Dkwwcg8gMjiPzBDCL/8YPVgAAAGPB4tFYDkFsAAAAASUVORK5CYII=", "J");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAKElEQVR4XmP4DwIMUPIDjcmHIPI5mDx4EEQeYAaRD9hB5A9+sBoQAADXZXd7yzup6gAAAABJRU5ErkJggg==", "J");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAKElEQVR4XmP4DwIMUPIHjckPIPI7mHz4EEQ+YASRH5hB5B9+sBoQAAAWSniXd+Vi4AAAAABJRU5ErkJggg==", "J");

                    // 10
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAT0lEQVR4XmP4DwIMH/+xA8nGP4xAkvEDQz2QfFAAJA0f/rAHyh7+Iw8i/4HI42Cy/T8/ESRCF4Q8+Adk2gOwmWBbPv5gANn+hxlIQlwCJgGSq13UxYjlNQAAAABJRU5ErkJggg==", "10");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAATElEQVR4XmP4DwIMEPJHfSOQPFDHUP+foaGGwR5IVtjI/2eYUVDHD5Qt+A8iE/+3o5HJ/49jJTFVQkyAmAY2+QfYlh9gG3/UM8BcAgCqklo7weFRqgAAAABJRU5ErkJggg==", "10");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAATElEQVR4XmP4DwIMEPLjf3Yg2fiHEUgyfmAAkz/qgeIP/9gDycP/QOTxf/Ig8j+CbMdLQlWCdUFMgJj2AGwy2JaPP0A2fvwHsh3sEgCMU2HDoxFM+QAAAABJRU5ErkJggg==", "10");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUUlEQVR4XmP4DwIMEPLH/2YgeeAfkMvA8IOhHkh+qLEHin/4Jw8kH/4HkY//84PJfiB5GEwex0FCZCEqIbogJkBMg5gMtuXHH5CNP/4zw1wCAMMuZZT0lGalAAAAAElFTkSuQmCC", "10");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUElEQVR4XmP4DwIMEPLjf2Yg2fgHyGVg/MFQDyQ//LD/z/Dz4R8Q+fifPJA8/B9EHoeS/GCyHy+J0AUx4eE/kGkfKkAk2JafYBs/gG0HuwQAIJNjwaQ/hdgAAAAASUVORK5CYII=", "10");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAASklEQVR4XmP4DwIMEPLhP2YgyfiDAUR+YKgHkg8K7P8zFD78AyQ/Hv4jDyL/gch2CPmfnwgSoQtCQkwDm/wRbMtHsI0fwbaDXQIA245cmQwemesAAAAASUVORK5CYII=", "10");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAU0lEQVR4XmP4DwIMEPLDf0Yg2fAHyGVg+MFgDyQ/VADJHQ/+yP9n+PHwHz+I/A8iD0PJfjDZjoOEyIJUHgSTEBMe/AOZBjb5B9iWH2AbIbaDXQIAlRliHv/Bue8AAAAASUVORK5CYII=", "10");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAV0lEQVR4XmP4DwIMH/4zA8mGPwxAkuEHQz2Q/GBg/5/B4MEfeaDsw38g8jCE/M8PJ4+DyXYMEiKOrPIgWO9DsGkPKoAmf/gAsuXDD5CNH/4xAkmIS8AkAIRWXZlADXI/AAAAAElFTkSuQmCC", "10");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAVUlEQVR4XmP4DwIMP/4zA8kDfxiAJMMPhnog+cHA/j+DxIc/8kDZh//A5H9+IHkYhWzHQfbD1RwEkxC9D8DmfKgBkT8YgOb/+AOy68M/RqC9EJeASQCkYWDEedHtNwAAAABJRU5ErkJggg==", "10");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAWElEQVR4XmP4DwIM//+fB5IH/gAZDAw/GOqB5AcGeyD5gEEeSB6oAJIFB//w/2f40PgPRDb/YweR/4kgwSohug6ATQCb9gFs8gewLR/ANn4A2/7hP/P//wC1flS4TnZffAAAAABJRU5ErkJggg==", "10");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUUlEQVR4XmP4DwIMEPLDP2YgyfCHAUT+YKgHkh8q7P8zGDz4Iw+UffgPRB7+xw8i/yPI42CyHYmEiCCrgZAQEyCmgU3+8AFkywewjRDbwS4BACyvXcEKmXs2AAAAAElFTkSuQmCC", "10");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUUlEQVR4XmP4DwIMP+obgeSBOob6/wwNNQz2QLJCQv4/w4yCOn6gbMF/EJn4vx2NTIaSx9FITJUQEwrqQWSFjTyIZACRYLt+1DMAbYe4BEwCAK1MWXL/gWFmAAAAAElFTkSuQmCC", "10");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAATklEQVR4XmP4DwIMEPJPfSOQfFDHUP+foaGGwR5IVtjIA8UL/kNIfiBZ+L8fSCb+bweSyf+PEyQhKhPBugqgJMg0iMlgW/6AbYTYDnYJAJFCXc53Et7XAAAAAElFTkSuQmCC", "10");

                    // 9
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUUlEQVR4Xk3OMQqAQAxE0VksLD2CR/FigldeuxQhcX9c0BQPAsMkSkbThs4m04XnMeyxD+/8u30GdkcTesNcSWblp9WTTqdZNb9X6mIs9QPzAFp3Y2nk8HLAAAAAAElFTkSuQmCC", "9");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAVUlEQVR4Xk3Ouw2AQAwDUFNRMgKbwGKIE2IAVqW8IsQXh49I8QorigNq8LhJRwkNc1inUXJIj/D8eSeVvVylQZsO5dzT3HnVtculLXk/W0z9YPd90gARJmW9pxE1bwAAAABJRU5ErkJggg==", "9");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUElEQVR4XmP4DwIMULIBRP5jsAeSfxjkgeQPGzBZzw8kP/zvRyLbEWQ9O0iNHZhkAJF/GEDi/xnBJFj9//8gEyDk339gMyvAJNSuerB6uEsAE/1iGk53tLEAAAAASUVORK5CYII=", "9");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAU0lEQVR4Xl3OsQ3AMAgEQCwPkBEyStZKYSmzegOnIxIC829X+eI6/pFAZFugyZOqXOlo1M/0Ddjj+DmcNqgCrUCvMHgVbFh+hk692by3sOuVPyATpWphRKYurmAAAAAASUVORK5CYII=", "9");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAT0lEQVR4XmP4DwIMUJIRRP5jqAeSPxjsQaSNPJD88A9M/ueHkx+R2FCyBkT+YACRf8Dkf0YwCZaFkmBz/oLJHxUg8z9AbAHZz/APbDvYJQB5gmCP6Kxg/QAAAABJRU5ErkJggg==", "9");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUklEQVR4XmP4DwIMULIRRP5jqAeSfxjsgeSPOnkQ+R9EfvzPDyb7kUiQyAcw+aMOTDKAyH+MINn/zWASLPsfbAKE/PsPbOYfsPlQu8C2M8NdAgC4C2TUpmZJEAAAAABJRU5ErkJggg==", "9");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUklEQVR4XmP4DwIMULKhHkQy2APJfwzyQPKPHD+Q/GHfDiLrIeRxJDaEbAaplGcGkQzsYL3uYNNAKv//B6mBkSDT/oHJPzZg8yG2gG0EuwLiEgB2YmCydqlEFQAAAABJRU5ErkJggg==", "9");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAT0lEQVR4XmP4DwIM/4+DyH8MIPIHgz2Q/MAgDycf1PCDyH/IJDuCrAGTDCDyA5j8ASb/gMn/bCD1/8G6fv4BkR9+IJkMtaserAvsBmYgCQDchVn04TG50gAAAABJRU5ErkJggg==", "9");

                    // 8
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAVElEQVR4XmP4DwIMULIRRP5jqAeSfxjsgeSPGjD5Tx5I/vyPICEiP/6BZP9YgNT/A5kB1AViQ3R9AKv5CFb/8T8/EgkSgcj+ANsC0fWfEWTCfxAAAKlwZAJ+Nml2AAAAAElFTkSuQmCC", "8");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAASklEQVR4XmP4DwIMUJIZRP4B8Rh+MNSDyD/2QPLjP+wkRPaPBUjlP7AuqN4aiBp5EPkfRD5GIiHiH/6AyB8MYBPAdv1nBOn9DwIARfhlnk3vu+EAAAAASUVORK5CYII=", "8");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAASUlEQVR4XmP4DwIMULIBRP5jsAeSfxjkQaQcP5D8UQ8h++HkH7DIH3sQ+Q+s8j9DPZz9hwdJ1/92dLIeTNqBTWBAMqEBZALYJQDUm15jXboGWAAAAABJRU5ErkJggg==", "8");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAASElEQVR4XmP4DwIMUJIRRP5hqAeSPxjsQWSNPJD88A87+aMGpOYPWOU/kBkwvRIINR//8wPJh0jkBwgJNvkHA4iEmAB2BcQlALHkYJb/KGxuAAAAAElFTkSuQmCC", "8");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAASklEQVR4XmP4DwIMULIBRP5jqAeSfxjsgeQPG3kQWQcm//ODSQgbIQ5RiaJLAiT+oR6k/gNY18f//WA2hASL14FNY0CYAHYFxCUAdtZgEea3RmsAAAAASUVORK5CYII=", "8");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAR0lEQVR4XmP4DwIMULIRRP5jqAeSfxjsgeSPOnkQ+R87+acOpOafDEg92Ayo3h82CDUf//ejkR/+8yNMZgCR/8B2QWz/DwIAg6BkA7x4BBIAAAAASUVORK5CYII=", "8");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAASklEQVR4XmP4DwIMUJIRRP5hqAeSPxjsgeSHCjD5Tx5IfkQiP/wDi/8BkT/A6v+AzICyPxggdD38j0FCTKgBk2BbILr+gW3/DwIAGCViHhAvh9QAAAAASUVORK5CYII=", "8");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAATElEQVR4XmP4DwIMUPIAmGSwB5L/GOSB5B97MFnPDyb70Umw7D8ZkPr/DPVwvX/sQOI/wGp+/G8Hk8cRJETcHmwm2JZ/YF3/G+AuAQCuZGHLUCqeAQAAAABJRU5ErkJggg==", "8");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAATUlEQVR4XmP4DwIMEPIfO4j8wQgiP4DEGD78qAeSD/+AyMf/7MEkQuQjmPxRAFL5B6zrB4ouiHoQeRiJhIhAZD8wgE0A6/rHDCL/gwAAVf9pW62luTUAAAAASUVORK5CYII=", "8");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAR0lEQVR4XmP4DwIMEPIfM4j8AeIxfGCoB5IPfoDIh3/ssZIfwLIfwOr/MCL0PkhA6Dr8D0LKI0iw+IMfYBJsC9QEsO3/QQAAVMJmOGs/2IIAAAAASUVORK5CYII=", "8");

                    // 7
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAASklEQVR4Xl3NoRXAIBCD4duAERiF0ZCVHQrBHH2doA7B4yD/uUZ8Lom5YuGw/NMdV1jkDKscdMMPH3yxYccLb0yYpYaPRWpYjycbUVxtI97E2CkAAAAASUVORK5CYII=", "7");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAPUlEQVR4XmP4DwIMEPIDgzwa+f8fmPxjjyB/1CPID2C9EPIhEnkYiWxHIvkRJMjg/wx/IKQ9iPwBJv+DAAAy2WxVkfbuTAAAAABJRU5ErkJggg==", "7");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAASElEQVR4XmP4DwIMEPIDgzwa+f8fhLQHkX8gZD2I/AEhwXo/gMmPYPIxEnkcTLaDyX4wyQ8m5UEkyGAgaQ8iQQYDyXqwLAgAAE9nbRfwalxNAAAAAElFTkSuQmCC", "7");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAOUlEQVR4XmP4DwIMEPIDgzwa+f8PhLQHkT/qEeQHsC4I+RCJPIxEtiOR/AjynzyCBBkMI0EGQ10CAE0obJWwkNBpAAAAAElFTkSuQmCC", "7");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAPklEQVR4XmP4DwIMEPIDAz8a+f+fPIL8Y48gf9QjyA9gEyDkQyTyMBLZDibZwSQ/iPwHIeVB5B8IaQ+WBQEA4ShritCcl0kAAAAASUVORK5CYII=", "7");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAP0lEQVR4XmP4DwIMEPIDAztW8v8/fgT5Rx5E/oCQ9iDyA4SsB5EPwOQBsJkQshGJZEaQ/9jBJD+I/AMh5eEkAEh5ZeeLMYbGAAAAAElFTkSuQmCC", "7");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAARUlEQVR4XmP4DwIMEPIHAz8a+f8/hJQHkf8gpD2I/AMh60HkDwgJNucDmPwIJh+Cycdg8jCYPA4m28EkP4IEGQwj/4MAAO90bHcWKoU/AAAAAElFTkSuQmCC", "7");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAATElEQVR4XmP4DwIMEPIDgz0SKQ8S/wdi//8DIetB5A8ICdb1AUx+BJMPweRjMHkYTB4Hk+1gkh9MyoPIfxDSHkSCDAaS9SASZDDUJQCorG3jfkUPuAAAAABJRU5ErkJggg==", "7");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAR0lEQVR4XmP4DwIMEPIBAz8GKQ+S/QEh7UHkBzD5oB5BHgCbACEbwWQzmGQGk+wg8h+E5AeRfyCkPIj8ASHtQSTIYCBZDyQBc1xmaWbeer4AAAAASUVORK5CYII=", "7");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAARklEQVR4XmP4DwIMEPIDAz9W8v8feQT5A0x+sAeT9SDyAYQEm3MATB4Ek41gshlMMoNJdhD5D0Lyg8g/EFIeRP6AkPZAEgCmembqwkdMLQAAAABJRU5ErkJggg==", "7");

                    // 6
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUklEQVR4XmP4DwIMUPJAPYhkkAeS/xj4geQfPhD7j/1+IPmjHqQGSoLVf3gAJhvsQSRY1wcZkK4P9WDyfzsa+QNCgmX/2IFJqF0gE/43gN0AAgCEI2PfOzUyxQAAAABJRU5ErkJggg==", "6");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAVElEQVR4XmP4DwIMULIBRP5jsAeSfxjkgeQPGRD7Rz2I/ABWg0x+PAgiHzLUA8kHYF0PbEC6HvwDk//5QbJg8iOY/AAhwbI/asAk1C6QCWBXQFwCANEuZAHiByqdAAAAAElFTkSuQmCC", "6");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAU0lEQVR4XmP4DwIMUPIAmGSwB5L/GOSB5B87EPtHPUj8B1jNBzD5EUJ+BpMN9SBxsK4PNSBdH/6ByI//+TFIkPgPCFkHIv9A7QKZ8L8RZBrI4P8AzDdnKQtbO7MAAAAASUVORK5CYII=", "6");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUUlEQVR4Xk3NsRGAQAhEUTqwA23E8UrjOrAkLelCAkf0rwQSvAB2FkvGyu5oy+strxmjHehkhpLlKa1JkmOd0GV+7j/ZhK6xqb9+0ZCdNorzAUYDYs9JGC4VAAAAAElFTkSuQmCC", "6");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAVUlEQVR4XmP4DwIMUPIAmGSwB5L/GOSB5B97EPtP/X4g+QOsBkJ+AJMfP4PJhnqQCFjXBzuQrg//+cFkP0gWifwBJUGyP+rA5kPtApnwvxFkGsjg/wD9OmiOXYGx7AAAAABJRU5ErkJggg==", "6");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUElEQVR4XmP4DwIMUPIBmGyQB5L/GMCkPIj8Yw8S/1EPJsEqoeRnEPmhwR5EgtV/sOMHkfVg8n8/WCUGCZb9A1YJseU/A8iE/w0g00AG/wcAk+FmUhh2FokAAAAASUVORK5CYII=", "6");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAU0lEQVR4XmP4DwIMUPIAmGSwB5L/GOSB5B87EPtHPUj8B1jNBzD5EUJ+BpMN9SBxsK4PNSBdH/6ByI//+TFIkPgPCAlW+QdqF8iE/40g00AG/wcAy/9nJxwd7Q8AAAAASUVORK5CYII=", "6");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAVklEQVR4XmP4DwIMUPKBPYhs4AeRDCDyHz+I/CN/H0Tag9RAyB/1YDKhHkQ6yINIsPofcuwg0r4drAZCHkciQSJ/wLJ/5EEq/4F1/WcAmfD/AMg0sEsAnehgVcTdNF8AAAAASUVORK5CYII=", "6");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAU0lEQVR4XmP4DwIM/x+DSQZ7IPmHQR5I/mDgB5Mg9gc7MFkPkn1QD1L54DuYZKgHkyBZGAnS9aAGTP5jB5H/kUiwyAew7Ack8/9A7QWZ9p8RaDIA22hZ5tGoVFsAAAAASUVORK5CYII=", "6");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAT0lEQVR4Xm3MoRGAQAxE0XVISrhOSFEUwHVCi8FFMAnZVSeIeCY7H8VDPXJam9jbdzHGoHa3blz6KcG9g1/X0rHRQ17/hr6x9FOFVK1mlz9wWFegmGFjQwAAAABJRU5ErkJggg==", "6");

                    // 5
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAPklEQVR4XmP4DwIMEPIPgzw6WQ8WB5M/MElGMMlQDybtQSotQLr+/wOT//mxkj/Ash9swCREF9gEsCsgLgEAPHpivh05Ky8AAAAASUVORK5CYII=", "5");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAPUlEQVR4XmP4DwIMEPIPgz06CRb/gYt8DiZBuoFkPUj9H5Cu///kwWZiJ3+CZT/UgFR+gNoFMuE/I5gEAQBayWlG/3/m/wAAAABJRU5ErkJggg==", "5");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAQElEQVR4XmP4DwIMEPIHgz06WQ8Wh8hiks0g8gNIN5CsB4kUgHT9/wMm/8ljJT+CZR9UgEioLrAJ/xhBJMjg/wDVB2gJUlKu4gAAAABJRU5ErkJggg==", "5");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAQ0lEQVR4XmP4DwIMEPIHgz06WQ8Wh5AQNcgkI0RXPVz9HwN5kGl/wOQ/MPkfiQSL/ASTHypA6j9A7QKZ8A9sGsjg/wAL4WUX4OLGgwAAAABJRU5ErkJggg==", "5");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAP0lEQVR4XmP4DwIMEPIPgz06WQ8WRyYhKiHkc4iuerj6fzXyINP+gcn/2MmfYNkPYJU/oHaBTPjPCDINZPB/AEykZewY6ASPAAAAAElFTkSuQmCC", "5");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAPklEQVR4XmP4DwIMEPIHgz06CRHHRZ6H6IKQ9UDyzw+Qrv9/wOQ/CCmPRIJEfoPJD2CVH8C6ICb8ZwaTIAAAcxNqUDjVJtAAAAAASUVORK5CYII=", "5");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAQklEQVR4XmP4DwIMEPIPAz920h4sCyZ/IJMM9mBSHkxC1LMDyX91IPL/f3TyD5j8YQciP4DVQ0iICf/Apv1vAJoMAC63Ws4T0Lj4AAAAAElFTkSuQmCC", "5");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAQElEQVR4XmP4DwIMEPIDQz06CRb/iItkB+tiROj9kQAi//+wB5F/0MnvYPIxWPbAB5DKB1C7QCb8YQaRIIP/AwBVnWmVepddlAAAAABJRU5ErkJggg==", "5");

                    // 4
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAP0lEQVR4XmP4DwIMUPKfPIj8AyZ/IJEfwOQDMHkITLaBST4Q+U8OQf6xA5E/6kDkB7BpDxiYQWwkEmoLLhIEANRlYKopgMEvAAAAAElFTkSuQmCC", "4");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAQ0lEQVR4XmP4DwIMUPKfPYj8AyZ/gMkPYPIBmDwAJg+ByTYwyQci/8mByD92IPJHHYL8ADbtAQM7nPwAJqG24CJBAACKGmMoNGn7AAAAAABJRU5ErkJggg==", "4");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAQklEQVR4XmP4DwIMUPKfPYj8AyZ/gMkPSOQjMHkMTPaBST4Q+U8OTNqByD91IPIH2JyPYPIBAzuQ/IBEQm3BRYIAAMiPY/1Rz11AAAAAAElFTkSuQmCC", "4");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAP0lEQVR4XmP4DwIMUPKfPYL8AyZ/gMkPYPIRmDwEJo+ByT4wKQci/9mByD91IPIHmPwANucDAzMaiWILJgkCAO9/ZEMA2zFbAAAAAElFTkSuQmCC", "4");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAARUlEQVR4XmP4DwIMUPKHPIj8gEQ+AJMHwGQDmGQAkf8gJAeI/CMBIn+AyQ8WIPJBBYg8wMCMRj4Ak/9/gM3BRf6x//8fAIsqW8n3EkCeAAAAAElFTkSuQmCC", "4");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAPklEQVR4XmP4DwIMUPKPPYj8gUR+AJMPwOQBMNkCJnkQ5D8ZEPnHBkT+qAGRH8DmPASTDxjY0UioLbhIEAAAValirdAUNBEAAAAASUVORK5CYII=", "4");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAQ0lEQVR4XmP4DwIMUPKfPYj8AyZ/gMkPYPIBmDwEJo+ByT4wKQci/9mByD91IPIHhASb8xFMPmBgRyOhtmCSEHtBAADPxWQV+FrKXgAAAABJRU5ErkJggg==", "4");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAPUlEQVR4XmP4DwIMUPKPPIj8ASY/gMkHYPIAEtkAJhlA5D8WEPmHB0T+kAGRH2zAZA2IfMDAjJWE2oKPBAD1kVz2lOroxQAAAABJRU5ErkJggg==", "4");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAQUlEQVR4XmP4DwIMUPKPPIj8gUR+AJMPwOQBMNkAJhnAJAuI/McDIv+AyR8yIPKDDYh8wMCMRn4Ak1BbcJH/gCQA+FRc3xkXZAUAAAAASUVORK5CYII=", "4");

                    // 3
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAASElEQVR4XmP4DwIMEPIHgzwa+f+PPZisB5E/wCo/gsnHYPI4mGwHkw0QEqIeTP4Dm/AfnfwJFv9QA1LzgwFE/mEAm98INgEEAOM7aKp6AjTyAAAAAElFTkSuQmCC", "3");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAT0lEQVR4XmP4DwIMEPIHgz0SKQ8S/wNi//9RDyI/gFU+BJOPweRxMNkOJhsgJFglRNc/sAkQ8j+C/fMfSPZDDYj8wQBS/wdkP8N/RjAJAgAVvmlsOW1e0QAAAABJRU5ErkJggg==", "3");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAS0lEQVR4XmP4DwIMEPIDgz0a+f8HmPxQDybBKh+CycNgsh1MsoNJkBlAEqISrOsPmPwnDyd/g8mPYPEHFSDyA1j9D7Def4xgE0AAAH3mZ2rSNMRqAAAAAElFTkSuQmCC", "3");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAATUlEQVR4XmP4DwIMEPIHgzwa+f8PmPxhDyI/1IPIB2D1B8HkYTDZDCYbwLINYJU/wLr+gcn/CPIvmPwBFv9gA2YzgNT/YQDrBbkC4hIAK0Rl+4c6XvEAAAAASUVORK5CYII=", "3");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAS0lEQVR4XmP4DwIMEPIDgzwa+f+HPZisB5EfwCofgsnDYLIdTLKDSZAZQBKiHkz+A5uARP4Gkx/B5IMKkJoPYPU/GEDm/2MEmwACACp+Zd5oDHIEAAAAAElFTkSuQmCC", "3");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAARUlEQVR4XmP4DwIMEPIDgz0a+f9HPYL8AFb5EEweBpP9YJIfTDKCSQaIerDeP2DyHzr5EUw+AKv5AFb/B2Q/w39mMAkCAMZtaLjmQOCIAAAAAElFTkSuQmCC", "3");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAT0lEQVR4XmP4DwIMEPIBgzxW8v8HexD5oB5EHgCrbwSTzCDyHyOYZADJ/mMAq4To+gEm/4DI32DyI5h8ABY/ADUfpP4DWO8PkCsY/oHMBABFJl8SM4u8LwAAAABJRU5ErkJggg==", "3");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAATElEQVR4Xm3MsRHAIAxDUWWCjJBRWCx3OTbLKIzgkoIE0KfFxWssSd2nZejc2uOyJeFjX1oZ3Z7yFcllo/vbDyvGbQuZwEq3ieVjrg0CL11jT0+YogAAAABJRU5ErkJggg==", "3");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAS0lEQVR4XmP4DwIMEPIDgzwa+f+HPZisB5EfwCofgsnDYLIdTLKDSZAZQBKiHkz+A5uARP4Gkx/B5IMKkJoPYPV/GEDm/2MEmwACACrCZeJB3HKKAAAAAElFTkSuQmCC", "3");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAATklEQVR4XmP4DwIMEPIBgzxW8v8HexD5oB5EHgCrbwSTzCDyHyOYZADJ/mMAq4To+gEm/4DI3/9A5Ecw+wFY/ADUfJD6D2C9P0CugJgJAEWHXxUMCtorAAAAAElFTkSuQmCC", "3");

                    // 2
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUUlEQVR4XmXMsQ2AMAxEUTMBI2QNOkZLwWRskhFSBsnKxd+iQOKKV9nfxOx1Q7caDjvDfqRewmei/jo3GmnnVy1rd3rh3LNckMNoVmxG4avYAjucZhbNZ5hCAAAAAElFTkSuQmCC", "2");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUElEQVR4XmP4DwIMUJIRRP4B8Rh+MNQDyQ8V9iDyD4j8CSb//wOTEDYy+QOk/v8HsDkPwWQzmGQHkf/4wSbLg022B5Ef6kHkAwZ5NPI/CAAAAk9nty4oEA8AAAAASUVORK5CYII=", "2");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUklEQVR4Xk3MwQnAIBQD0EAH6Eiu1ZvHDuYidQN7E/oxNcGD//DghxBQh+UhQx868rRdafqG/IbkrnOGmuy2eafaYm85Ti8nL2epIvFAC7uK+QNkFmoA9rgiRAAAAABJRU5ErkJggg==", "2");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAU0lEQVR4XmXMsQ2AMAxEUSMKxmCUrEUHo2UDVmAEl0GKcvibBgkXr7DuzsSZTuyGzfbQrYSXrbihd7xTvbaPTl6Vrg7hhGPGviDBWCtpLtef+X8A6kxezx3WXiIAAAAASUVORK5CYII=", "2");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAT0lEQVR4Xk3MsQ3AIBADQG+QEVgEKaOhKJNmA0oK8uZtUeDiipffoILtIwMtnbjTUW2U9Kfkqe8MdTjt0C+71z772svLRaqYm012aOGUygKjz2c2Zi4vuAAAAABJRU5ErkJggg==", "2");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUklEQVR4Xk3MsQ2AMAxEUW/ACBkliyGloGAwJmGDlC5CnPyTC654kq3TWRBLL5xcNqxt/azbPvGbhU7KJx3S6Ydr55WPvOWh5YJesTdprP0NsgBplmkSFX7DlwAAAABJRU5ErkJggg==", "2");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUUlEQVR4XmXMsRGAMAxDUXeUGYE16LIZOY7J2IQRUrrwxfE3UKHiVZLEibweOKSFJjXUbQ37jjbQ/9ojfVe2fufblZ7pks8FtWJvqbD9LDTJBDpAZgBdH3zPAAAAAElFTkSuQmCC", "2");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUklEQVR4XmXNsRGAMAxDUXMUjMEoWYsORssGrMAIKcOdL8LfKXHxKkk2caYb3bDbGTYr4WM7Hqnjm2ra0zYlr0pXFztacKzoGxKPnYI1l//mxw/dbF6XShQi3QAAAABJRU5ErkJggg==", "2");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUklEQVR4XnXMsQ2AMBBDUSMGYISsQcdikWA0RklHS5kiirlPGhqueMVZtszJF3Zh0xZWpfAerq95Ie3oNuTv+rHQ9bnjwZon7DNSip2ERSz8+AB4Rl3GE5eSUwAAAABJRU5ErkJggg==", "2");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAT0lEQVR4XmXMsQ2AMBBDUW+QBZAySjYDJCbLJoxAmSKKk3+iQMLF0xU+y0SvZ6iy7Mq4YdsTt9F/Bx33sPHr58A71mp44UhIcW2WUCx8NZnPkmTzlTmfigAAAABJRU5ErkJggg==", "2");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUklEQVR4XnXMsQ2AMAxEUSMGyAhZg47FIsFojJKONmUKy0c+aWi44hVn+UzEdGMYuu3DbnnYpttrSVwDNXV69Y+VX10HnqxpwVjRE7aM1Vj48QF5Al3IEnD39wAAAABJRU5ErkJggg==", "2");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAU0lEQVR4XnXMsQ2AQAwDQFN9+SOwCBKbAaMxCiNEovkiiolDQ0OKU2TJBnXgXWJPA3Pq6Okobal/a8opGa/K6R9NXV6rPLXGQ8sxSW9ydGnQwo8PDnVdCVE6zT4AAAAASUVORK5CYII=", "2");
                }};

                String imageHash = encodeToString(cardNameIgmBwImg, "png");
                if (cardNamesMap.get(imageHash) == null) {
                    cardNamesMap.put(imageHash, "?");
                    if (false) {
                        ImageIO.write(cardNameIgmBwImg, "png", cardNameIgmBW);
                    }
                    System.out.printf("%s - %s\r\n", cardNameIgmBW, imageHash);
                    System.out.println("Hash added?");
                    card = "?";
                    // open file
                    // input for learning
                    // seriliaze map to file
                } else {
                    if (false) {
                        if (cardNameIgmBW.toString().contains("сrop_20180821_084134.865_0x240C023E.png_2_name_BW")) {
                            ImageIO.write(cardNameIgmBwImg, "png", cardNameIgmBW);
                            System.out.println("Warning!!!: hash: " + imageHash);
                        }
                        System.out.printf("%s - %s\r\n", cardNameIgmBW, cardNamesMap.get(imageHash));
                    }
                    card = cardNamesMap.get(imageHash);
                }
            }

            result.append(card);
            String mast_string = mast == null ? "-" : mast.toString();
            result.append(mast_string.substring(0, 1).toLowerCase().replace("--", ""));
        }

        result.append("\r\n");
        System.out.printf("File: %s, Result: %s", object.getFileName(), result);

        System.out.println("@@@@@@@@@@@@@@@@@");
        return result.toString();
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

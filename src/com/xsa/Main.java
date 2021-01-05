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

        if (false) {
            File fullFile = new File(String.format(".//output//full_%s", object.getFileName()));
            ImageIO.write(full, "png", fullFile);
        }

        // размеры внутреннего экрана
        int innerScreenW = 300;
        int innerScreenH = 0;

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
            imgs[i] = crop.getSubimage(0 + offset, 0, width - 2, crop.getHeight());
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
            int rgb_int = imgs[j].getRGB(firstLayer.x, firstLayer.y);
            enumCardColors cardCollor = CardCollors.get(rgb_int);
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

            if (cardCollor.equals(enumCardColors.Black) || cardCollor.equals(enumCardColors.Red)) {
                // выбираем изображение в примерной области значения карты как вариант поиск изменённого цвета пикселя до отличного цвета от белого или тёмно-белого
                int cardNameOffsetX = 0;
                int cardNameOffsetY = 0;

                // если тёмный режим карты
                CardCollors.put(-15724526, enumCardColors.Black); //  (dark)
                CardCollors.put(-3323575, enumCardColors.Red);

                if (j == 0) {
                    cardNameOffsetX = 0;
                    cardNameOffsetY = 0;
                }

                if (j == 1) {
                    cardNameOffsetX = 0;
                    cardNameOffsetY = 0;

                }

                if (j == 2) {
                    cardNameOffsetX = 0;
                    cardNameOffsetY = 0;
                }

                if (j == 3) {
                    cardNameOffsetX = -2;
                    cardNameOffsetY = -5;
                }

                if (j == 4) {
                    cardNameOffsetX = 0;
                    cardNameOffsetY = 0;
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
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAS0lEQVR4XmP4DwIMUPIwEnkQTDYikRII8p9NPYKssweSf6CkPJD88R9BfmDgh5MP/7cDycf/jwPJwyBrGI7/fwwmHwLJdjAJcsh/AHmBaia69NKKAAAAAElFTkSuQmCC", "A");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAATElEQVR4XmP4DwIMKGQ7EskOJplB5D9lMGkIJgtB5J8KBPnjRz2I/AMiP0LIf/ZA8gEDhJSHk8f/84PJfiDZDyWPA0l+MPn//+3//wHZ8mlbx/FOKQAAAABJRU5ErkJggg==", "A");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUElEQVR4XmP4DwIMEPLH/4dg8jCYbAeT/GBSHkT+sweRf+pB5A+w+o9g8iGYPAgmQSYx/JAAq7EBq68Bk3VgE6CmQUyGkBC7wPbWf/gPdQkAslpuPRDmByEAAAAASUVORK5CYII=", "K");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUElEQVR4XmP4DwIMEPLh/2YwyQ4i//GDyD/yIPKHPZisB5EfwCofgsnDYLIdTDKDyAeMYFIRTBaAyQ9gXRAT/oDJf2AzIeZD7ILaCzQO4hIAr+Jrc9PJGYIAAAAASUVORK5CYII=", "K");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAWUlEQVR4XkXNuw2AMAwE0BMZwCN4FI+WiIK5sgmMkJLCisMlIK54xckfBIPXJrQmWkBTzYEuzQKuTs01cFuneRlL+T1nc8zJXblVqF72XXsu0wLlx402YcMMvqRXPQ0FnewAAAAASUVORK5CYII=", "Q");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAWElEQVR4Xj3NsRHAMAgDQHKXi8dgFEbDk2QWj5IRKF34wMiJo+ILFRIFQp/GsBVYj9RLg2zpkK5BXYek8eq/Fs7b3d/LE2oVLLRcGPzoWk6DSPB4QWM0yARAul+S4wfnawAAAABJRU5ErkJggg==", "Q");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAVUlEQVR4Xn3OsQ2AQAgFUExMaBzBeKMwGgzmMIxw5RWEk2/OVooXmv+BJoaW3qAxpK1MtjKal0O6ljrKPkP+vF9PmIcgRciK6dcWjD2JBBd36JeuTx6KgV1ROigWTQAAAABJRU5ErkJggg==", "Q");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAALElEQVR4XmP4DwIMUPJHPU3J32Dy4wcQ+eDBfyD5AWQzww9GEPmHHUT+BwEAIyBvoB3+eeAAAAAASUVORK5CYII=", "J");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAT0lEQVR4XmP4DwIMH/+xA8nGP4xAkvEDQz2QfFAAJA0f/rAHyh7+Iw8i/4HI42Cy/T8/ESRCF4Q8+Adk2gOwmWBbPv5gANn+hxlIQlwCJgGSq13UxYjlNQAAAABJRU5ErkJggg==", "10");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAATElEQVR4XmP4DwIMEPJHfSOQPFDHUP+foaGGwR5IVtjI/2eYUVDHD5Qt+A8iE/+3o5HJ/49jJTFVQkyAmAY2+QfYlh9gG3/UM8BcAgCqklo7weFRqgAAAABJRU5ErkJggg==", "10");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAATElEQVR4XmP4DwIMEPLjf3Yg2fiHEUgyfmAAkz/qgeIP/9gDycP/QOTxf/Ig8j+CbMdLQlWCdUFMgJj2AGwy2JaPP0A2fvwHsh3sEgCMU2HDoxFM+QAAAABJRU5ErkJggg==", "10");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUUlEQVR4Xk3OMQqAQAxE0VksLD2CR/FigldeuxQhcX9c0BQPAsMkSkbThs4m04XnMeyxD+/8u30GdkcTesNcSWblp9WTTqdZNb9X6mIs9QPzAFp3Y2nk8HLAAAAAAElFTkSuQmCC", "9");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAVElEQVR4XmP4DwIMULIRRP5jqAeSfxjsgeSPGjD5Tx5I/vyPICEiP/6BZP9YgNT/A5kB1AViQ3R9AKv5CFb/8T8/EgkSgcj+ANsC0fWfEWTCfxAAAKlwZAJ+Nml2AAAAAElFTkSuQmCC", "8");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAASklEQVR4XmP4DwIMUJIZRP4B8Rh+MNSDyD/2QPLjP+wkRPaPBUjlP7AuqN4aiBp5EPkfRD5GIiHiH/6AyB8MYBPAdv1nBOn9DwIARfhlnk3vu+EAAAAASUVORK5CYII=", "8");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAASUlEQVR4XmP4DwIMULIBRP5jsAeSfxjkQaQcP5D8UQ8h++HkH7DIH3sQ+Q+s8j9DPZz9hwdJ1/92dLIeTNqBTWBAMqEBZALYJQDUm15jXboGWAAAAABJRU5ErkJggg==", "8");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAASElEQVR4XmP4DwIMUJIRRP5hqAeSPxjsQWSNPJD88A87+aMGpOYPWOU/kBkwvRIINR//8wPJh0jkBwgJNvkHA4iEmAB2BcQlALHkYJb/KGxuAAAAAElFTkSuQmCC", "8");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAASklEQVR4Xl3NoRXAIBCD4duAERiF0ZCVHQrBHH2doA7B4yD/uUZ8Lom5YuGw/NMdV1jkDKscdMMPH3yxYccLb0yYpYaPRWpYjycbUVxtI97E2CkAAAAASUVORK5CYII=", "7");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAPUlEQVR4XmP4DwIMEPIDgzwa+f8fmPxjjyB/1CPID2C9EPIhEnkYiWxHIvkRJMjg/wx/IKQ9iPwBJv+DAAAy2WxVkfbuTAAAAABJRU5ErkJggg==", "7");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUklEQVR4XmP4DwIMUPJAPYhkkAeS/xj4geQfPhD7j/1+IPmjHqQGSoLVf3gAJhvsQSRY1wcZkK4P9WDyfzsa+QNCgmX/2IFJqF0gE/43gN0AAgCEI2PfOzUyxQAAAABJRU5ErkJggg==", "6");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAVElEQVR4XmP4DwIMULIBRP5jsAeSfxjkgeQPGRD7Rz2I/ABWg0x+PAgiHzLUA8kHYF0PbEC6HvwDk//5QbJg8iOY/AAhwbI/asAk1C6QCWBXQFwCANEuZAHiByqdAAAAAElFTkSuQmCC", "6");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAPklEQVR4XmP4DwIMEPIPgzw6WQ8WB5M/MElGMMlQDybtQSotQLr+/wOT//mxkj/Ash9swCREF9gEsCsgLgEAPHpivh05Ky8AAAAASUVORK5CYII=", "5");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAPUlEQVR4XmP4DwIMEPIPgz06CRb/gYt8DiZBuoFkPUj9H5Cu///kwWZiJ3+CZT/UgFR+gNoFMuE/I5gEAQBayWlG/3/m/wAAAABJRU5ErkJggg==", "5");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAQElEQVR4XmP4DwIMEPIHgz06WQ8Wh8hiks0g8gNIN5CsB4kUgHT9/wMm/8ljJT+CZR9UgEioLrAJ/xhBJMjg/wDVB2gJUlKu4gAAAABJRU5ErkJggg==", "5");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAT0lEQVR4XmP4DwIMEPIHgz0SKQ8S/wNi//9RDyI/gFU+BJOPweRxMNkOJhsgJFglRNc/sAkQ8j+C/fMfSPZDDYj8wQBS/wdkP8N/RjAJAgAVvmlsOW1e0QAAAABJRU5ErkJggg==", "3");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUUlEQVR4XmXMsQ2AMAxEUTMBI2QNOkZLwWRskhFSBsnKxd+iQOKKV9nfxOx1Q7caDjvDfqRewmei/jo3GmnnVy1rd3rh3LNckMNoVmxG4avYAjucZhbNZ5hCAAAAAElFTkSuQmCC", "2");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUElEQVR4XmP4DwIMUJIRRP4B8Rh+MNQDyQ8V9iDyD4j8CSb//wOTEDYy+QOk/v8HsDkPwWQzmGQHkf/4wSbLg022B5Ef6kHkAwZ5NPI/CAAAAk9nty4oEA8AAAAASUVORK5CYII=", "2");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUklEQVR4Xk3MwQnAIBQD0EAH6Eiu1ZvHDuYidQN7E/oxNcGD//DghxBQh+UhQx868rRdafqG/IbkrnOGmuy2eafaYm85Ti8nL2epIvFAC7uK+QNkFmoA9rgiRAAAAABJRU5ErkJggg==", "2");
                    put("iVBORw0KGgoAAAANSUhEUgAAACgAAAAZAQAAAABro1HIAAAAUElEQVR4XmP4DwIMEPLD/4NgshlMsoPIf/wg8o88iPxhDyI/1INJsPqHYPIwmGwEk4xgUgBMWoBVVoB1QUz4Byb/g82EmA+16zCYBBoHcQkABpVsevXK8HIAAAAASUVORK5CYII=", "K");
                }};


                String imageHash = encodeToString(cardNameIgmBwImg, "png");
                if (cardNamesMap.get(imageHash) == null) {
                    ImageIO.write(cardNameIgmBwImg, "png", cardNameIgmBW);
                    System.out.printf("%s - %s\r\n", cardNameIgmBW, imageHash);
                    System.out.println("Hash added?");
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

            // выбор значения из мапы для распечатывания результата


            result.append(card);
            String mast_string = mast == null ? "-" : mast.toString();
            result.append(mast_string.substring(0, 1).toLowerCase());
        }

        result.append("\r\n");
        System.out.printf("File: %s, Result: %s", object.getFileName(), result);

        String targetOfCardSet = "4hQd7s";
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

    public static BufferedImage decodeToImage(String imageString) {
        BufferedImage image = null;
        byte[] imageByte;
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            imageByte = decoder.decode(imageString);
            ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
            image = ImageIO.read(bis);
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return image;
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

    public enum enumCardNames {
        J,
        A,
        K,
        Q,
        W,
        TEN,
        NINE,
        EIGHT,
        SEVEN,
        SIX,
        FIVE,
        FOUR,
        THREE,
        TWO
    }
}

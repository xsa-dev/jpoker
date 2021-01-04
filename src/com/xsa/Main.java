package com.xsa;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
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
                                writer.append(result);
                                System.out.println("done");
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
        BufferedImage full = img.getSubimage(0, verticalOffset, img.getWidth(),  img.getHeight() - verticalOffset);
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
        int scip = 8 -1; // ширина черного заполнения между карт без теней

        for (int i = 0; i < 5; i++) {
            // размер мини внутреннего экрана
            if (offset > crop.getWidth()) {
                offset = 0;
            }
            ImageIO.write(
                    crop.getSubimage(0 + offset, 0, width - 2, crop.getHeight()), "png",
                    new File(String.format(".//output//%d_сrop_%s", i, object.getFileName())));

            offset += width + scip;

        }

        // TODO: card name?
        // TODO: card maste?

        // числа: 2 : 10
        // карты: A Ja Q K Jo
        // масти: ️ ️
        // ♦️ Diamonds
        // ♥️ Hearts
        // ♠️ Spades
        // ♣️ Clubs
        String targetOfCardSet = "4hQd7s";
        targetOfCardSet = String.format("Width: %d, Height: %d, File: %s \r\n", img.getWidth(), img.getHeight(), object.toString());
        return targetOfCardSet;
    }
}

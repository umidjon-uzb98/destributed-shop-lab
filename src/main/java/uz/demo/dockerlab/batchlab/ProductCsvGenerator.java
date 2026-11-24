package uz.demo.dockerlab.batchlab;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProductCsvGenerator {

    public static void main(String[] args) throws Exception {

        Path directory = Path.of("batch-data");

        Files.createDirectories(directory);

        Path file = directory.resolve("products.csv");

        try (BufferedWriter writer = Files.newBufferedWriter(
                file,
                StandardCharsets.UTF_8
        )) {

            writer.write("id,name,price");
            writer.newLine();

            for (int i = 1; i <= 100_000; i++) {

                String price = (i == 7500)
                        ? "-10.00"
                        : (100 + i % 900) + ".99";

                writer.write(
                        i + ", product-" + i + " ," + price
                );

                writer.newLine();
            }
        }

        System.out.println(
                "CSV generated: " + file.toAbsolutePath()
        );
    }
}
package filesWork;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Распаковка zip
 * Используется java.nio.file
 */
public class UnZip {

    /**
     * Распаковка zip-архива
     *
     * @param zipFileName имя zip-архива
     */
    public void unZip(String zipFileName) throws IOException {
        // Получаем содержимое ZIP архива
        File zipFile = new File(zipFileName);
        ZipInputStream zis = null;

        //пробуем открыть в кодировке UTF-8, если не получается, то пробуем в cp1251
        try {
            zis = new ZipInputStream(new FileInputStream(zipFile), Charset.forName("UTF-8"));
            ZipEntry ze = zis.getNextEntry();
            String nextFileName;
            while (ze != null) {
                nextFileName = String.format("%s%s%s", zipFileName.replace(".zip", ""), File.pathSeparator, ze.getName());
                // Записываем только файлы из корня архива
                if (!ze.isDirectory() && !nextFileName.contains("/")) {
                    Files.copy(zis, Paths.get(nextFileName));
                }
                ze = zis.getNextEntry();
            }
        } catch (IllegalArgumentException e) {
            zis = new ZipInputStream(new FileInputStream(zipFile), Charset.forName("cp1251"));
            ZipEntry ze = zis.getNextEntry();
            String nextFileName;
            while (ze != null) {
                nextFileName = String.format("%s%s%s", zipFileName.replace(".zip", ""), File.pathSeparator, ze.getName());
                // Записываем только файлы из корня архива
                if (!ze.isDirectory() && !nextFileName.contains("/")) {
                    Files.copy(zis, Paths.get(nextFileName).normalize());
                }
                ze = zis.getNextEntry();
            }
        } finally {
            if (zis != null) {
                zis.closeEntry();
                zis.close();
            }
        }
    }

}


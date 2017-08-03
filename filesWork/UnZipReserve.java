package filesWork;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Распаковка zip
 * Используется java.io
 */
public class UnZipReserve {

    private static final int BUFFER_SIZE = 1024;

    /**
     * Распаковка zip-архива
     *
     * @param zipFileName имя zip-архива
     */
    public void unZip(String zipFileName) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];

        // Получаем содержимое ZIP архива
        File zipFile = new File(zipFileName);
        final ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry ze = zis.getNextEntry();
        String nextFileName;
        while (ze != null) {
            nextFileName = ze.getName();
            File nextFile = new File(zipFileName.replace(".zip", "") + File.pathSeparator + nextFileName);
            // Записываем только файлы из корня архива
            if (!ze.isDirectory() && !nextFileName.contains("/")) {
                try (FileOutputStream fos = new FileOutputStream(nextFile)) {
                    int length;
                    while ((length = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                }
            }
            ze = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

}


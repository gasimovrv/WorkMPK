package UI;

import java.text.SimpleDateFormat;

/**
 * Форматы времени
 */
public interface Time {
    SimpleDateFormat TIME_FILE_CONTENT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    SimpleDateFormat TIME_FILE_NAME = new SimpleDateFormat("yyyy_MM_dd_HH-mm-ss");
    SimpleDateFormat TIME_HTML = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    SimpleDateFormat TIME_HTML_00_00 = new SimpleDateFormat("dd.MM.yyyy");
    SimpleDateFormat TIME_CONTENT_TO_DAY = new SimpleDateFormat("yyyy.MM.dd");
}

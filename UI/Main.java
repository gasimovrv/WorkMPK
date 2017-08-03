package UI;

import BLL.BLLException;
import BLL.BusinessLogic;
import entities.Event;
import entities.FolderEvent;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;


/**
 * Стартовый класс. Запись данных в файл
 * <p>
 * работа с логами программы mipko personal monitor
 * в данном приложении имеется возможность
 * обрабатывать события снимков экрана (первый аргумент -s)
 * обрабатывать события активность программ (первый аргумент -a)
 * обрабатывать события активность компьютера (первый аргумент -c)
 * обрабатывать события активность программ + активность компьютера (первый аргумент -ac)
 */
public class Main implements Time {

    private static ArrayList<Event> events = null;
    private static ArrayList<Event> events2 = null;
    private static ArrayList<Event> filterCompEvents = null;
    private static ArrayList<FolderEvent> folderEvents = null;

    public static void main(String[] args) {
        String inputPath;
        String outputPath;
        String mode;
        if (args.length == 3) {
            mode = args[0];
            inputPath = args[1];
            outputPath = args[2];
            if (!mode.equals("-s") && !mode.equals("-a") && !mode.equals("-c") && !mode.equals("-ac")) {
                System.out.println();
                System.out.println("Тип программы указан неверно, выберите значение из вариантов:");
                System.out.println("\t'-s' - обработка событий ежеминутных снимков экрана;");
                System.out.println("\t'-a' - обработка событий активность программ");
                System.out.println("\t'-c' - обработка событий активность компьютера");
                System.out.println("\t'-ac' - обработка событий активность программ + активность компьютера");
                return;
            }
        } else {
            System.out.println();
            System.out.println("Требуется указать параметры запуска:");
            System.out.println("- тип программы:");
            System.out.println("\t'-s' - обработка событий снимков экрана;");
            System.out.println("\t'-a' - обработка событий активность программ");
            System.out.println("\t'-c' - обработка событий активность компьютера");
            System.out.println("\t'-ac' - обработка событий активность программ + активность компьютера");
            System.out.println("- путь к папке с архивами программы слежки");
            System.out.println("- путь для выходных файлов");
            return;
        }

        BusinessLogic bll;
        File systemOut = new File(String.format("%s%s%s%s", outputPath, "\\all_events_", TIME_FILE_NAME.format(new Date()), ".txt"));
        File systemOut2 = new File(String.format("%s%s%s%s", outputPath, "\\all_events_2_", TIME_FILE_NAME.format(new Date()), ".txt"));
        File out = new File(String.format("%s%s%s%s", outputPath, "\\GameTime_", TIME_FILE_NAME.format(new Date()), ".txt"));


        try {
            bll = new BusinessLogic(inputPath);

            System.out.print("Режим поиска: по разделу ");
            switch (mode) {
                case "-s":
                    System.out.println("\"Снимки экрана\"");
                    break;
                case "-a":
                    System.out.println("\"Активность программ\"");
                    break;
                case "-c":
                    System.out.println("\"Активность компьютера\"");
                    break;
                case "-ac":
                    System.out.println("\"Активность программ + активность компьютера\"");
                    break;
            }
        } catch (BLLException bllEx) {
            System.out.println();
            System.out.println(bllEx);
            return;
        }

        System.out.println();
        try {
            System.out.println("Распаковка...");
            boolean zipResult = bll.zipOperation();
            if (zipResult) {
                System.out.println("Распаковка выполнена");
            } else
                System.out.println(String.format("Zip-архивов в папке [%s] не найдено", inputPath));
        } catch (BLLException bllEx) {
            System.out.println();
            System.out.println(bllEx);
            return;
        }


        System.out.println();
        try {
            System.out.println("Обработка файлов...");
            switch (mode) {
                case "-s":
                    events = bll.getScreenshotEvents(systemOut);
                    try {
                        //задаем тип сортировки
                        Event.setTypeSort('w');
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                    Collections.sort(events);
                    bll.clearInput();
                    break;

                case "-a":
                    events = bll.getActiveEvents(systemOut);
                    try {
                        //задаем тип сортировки
                        Event.setTypeSort('w');
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                    Collections.sort(events);
                    bll.clearInput();
                    break;

                case "-c":
                    events = bll.getActiveComputer(systemOut);
                    bll.clearInput();
                    break;

                case "-ac":
                    events = bll.getActiveEvents(systemOut);
                    try {
                        //задаем тип сортировки
                        Event.setTypeSort('w');
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                    Collections.sort(events);
                    events2 = bll.getActiveComputer(systemOut2);
                    bll.clearInput();
                    break;
            }
            System.out.println("Файлы обработаны");

        } catch (BLLException bllEx) {
            System.out.println();
            System.out.println(bllEx);
            return;
        }


        System.out.println();
        try {
            System.out.println("Фильтрация...");
            switch (mode) {
                case "-s":
                case "-a":
                    folderEvents = bll.getFolders(events);
                    Collections.sort(folderEvents);
                    break;
                case "-c":
                    filterCompEvents = bll.getActiveCompToDay(events);
                    break;
                case "-ac":
                    folderEvents = bll.getFolders(events);
                    Collections.sort(folderEvents);
                    filterCompEvents = bll.getActiveCompToDay(events2);
                    break;
            }
            System.out.println("Фильтрация выполнена");
        } catch (BLLException bllEx) {
            System.out.println();
            System.out.println(bllEx);
        }


        if (!out.exists()) {
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), Charset.forName("UTF-8"))))) {

                switch (mode) {
                    case "-s":
                        if (events == null || events.size() <= 0) {
                            writer.println("Событий \"снимки экрана\" не обнаружено");
                            break;
                        }
                    case "-a":
                        if (events == null || events.size() <= 0) {
                            writer.println("Событий \"Активность программ\" не обнаружено");
                            break;
                        }
                        progActiveWriter(writer, events);
                        break;
                    case "-c":
                        if (events == null || events.size() <= 0) {
                            writer.println("Событий \"Активность компьютера\" не обнаружено");
                            break;
                        }
                        compActiveWriter(writer, events);
                        break;
                    case "-ac":
                        if ((events == null || events.size() <= 0) && (events2 != null && events2.size() > 0)) {
                            writer.println("Событий \"Активность программ\" не обнаружено");
                            writer.println();
                            writer.println();
                            compActiveWriter(writer, events2);
                            break;
                        } else if ((events != null && events.size() > 0) && (events2 == null || events2.size() <= 0)) {
                            progActiveWriter(writer, events);
                            writer.println();
                            writer.println();
                            writer.println("Событий \"Активность компьютера\" не обнаружено");
                            break;
                        } else if ((events == null || events.size() <= 0) && (events2 == null || events2.size() <= 0)) {
                            writer.println("Событий \"Активность программ\" не обнаружено");
                            writer.println("Событий \"Активность компьютера\" не обнаружено");
                            break;
                        } else {
                            progActiveWriter(writer, events);
                            writer.println();
                            writer.println();
                            compActiveWriter(writer, events2);
                        }

                }

                writer.println();
                writer.println("Дата самого раннего события: ");
                if (bll.getStartTime() == null) writer.println("none");
                else writer.println(TIME_FILE_CONTENT.format(bll.getStartTime()));
                writer.println("Дата самого позднего события: ");
                if (bll.getEndTime() == null) writer.println("none");
                else writer.println(TIME_FILE_CONTENT.format(bll.getEndTime()));

                System.out.println();
                System.out.println("Выполнено!");
                if (outputPath.endsWith("Desktop"))
                    System.out.println("Результаты в файле на рабочем столе");
                else
                    System.out.printf("Результаты в папке: %s%s", outputPath, System.lineSeparator());

            } catch (IOException e) {
                System.out.printf("Ошибка записи в файл: %s", e);
            }
        }
    }

    private static void progActiveWriter(PrintWriter writer, ArrayList<Event> events) {
        writer.println("-----------------------Запуски программ:-----------------------");
        for (Event event : events) {
            long min = event.getWorkTime() / 1000 / 60;
            long sec = (event.getWorkTime() / 1000) - min * 60;
            writer.printf("%d мин %d сек - %s", min, sec, event.toString());
            writer.println();
        }

        writer.println();
        writer.println("Фильтр по папкам:");
        if (folderEvents != null) {
            for (FolderEvent folderEvent : folderEvents) {
                long min = folderEvent.getWorkTime() / 1000 / 60;
                long sec = (folderEvent.getWorkTime() / 1000) - min * 60;
                writer.printf("%d мин %d сек - %s", min, sec, folderEvent.toString());
                writer.println();
            }
        }
    }

    private static void compActiveWriter(PrintWriter writer, ArrayList<Event> events) {
        writer.println("-----------------------Активность компьютера:-----------------------");
        for (Event event : events) {
            long min = event.getWorkTime() / 1000 / 60;
            long sec = (event.getWorkTime() / 1000) - min * 60;
            String date1, date2;
            date1 = event.getDate1() != null ? TIME_FILE_CONTENT.format(event.getDate1()) : "none";
            date2 = event.getDate2() != null ? TIME_FILE_CONTENT.format(event.getDate2()) : "none";

            writer.printf("%d мин %d сек - вкл.: %s, выкл.: %s", min, sec, date1, date2);
            writer.println();
        }
        writer.println();
        writer.println("Фильтр по дням:");
        if (filterCompEvents != null) {
            for (Event event : filterCompEvents) {
                long min = event.getWorkTime() / 1000 / 60;
                long sec = (event.getWorkTime() / 1000) - min * 60;
                String date = event.getDate1() != null ? TIME_CONTENT_TO_DAY.format(event.getDate1()) : "none";
                writer.printf("%d мин %d сек - %s - кол-во событий: %d", min, sec, date, event.getEventsFromDay());
                writer.println();
            }
        }
    }

}

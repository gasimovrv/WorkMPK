package filesWork;

import BLL.BLLException;
import UI.Time;
import entities.Event;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.io.IOException;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Парсинг html документа
 */
public class HTMLtoEvents implements Time {
    private Document htmlDoc;


    /**
     * Все события снимков экрана
     *
     * @param htmlFile html-файл (созданный программой mipko)
     * @return список событий снимков экрана
     */
    public ArrayList<Event> getScreenshotEventsFromFile(File htmlFile) throws IOException {
        ArrayList<Event> result = new ArrayList<>();

        htmlDoc = Jsoup.parse(htmlFile, "UTF-8");

        //Список всех тегов с именем TABLE
        Elements tables = htmlDoc.body().getElementsByTag("TABLE");

        String eventName = null;
        Date eventDate = null;
        for (Element el : tables) {

            //обрабатываем только элементы, содержащие строку "Снимки экрана:"
            if (el.select(".RedData").text().contains("Снимки экрана:")) {
                eventName = el.select(".BlueDate").text().replace("Программа: ", "");

                //поиск тэгов с датой
                try {
                    String time = el.select(".BlackDate").text().replace("Дата: ", "").replace(" Тип события:", "");
                    eventDate = TIME_HTML.parse(time);
                } catch (ParseException e) {
                    eventDate = null;
                }
            }

            //создаем событие и добавляем в результирующий список
            if (eventName != null) {
                //разделяем имя и путь приложения
                String[] nameAndPath = eventName.split(" - ");
                Event event = new Event(nameAndPath[0], nameAndPath[1]);
                //задаем дату, если она есть
                if (eventDate != null) {
                    event.setDate1(eventDate);
                }
                //добавляем готовое собыите в результирующий список
                result.add(event);
            }
        }

        return result;
    }


    /**
     * Все события "активность программ"
     *
     * @param htmlFile html-файл (созданный программой mipko)
     * @return список событий "активность программ"
     */
    public ArrayList<Event> getActiveEventsFromFile(File htmlFile) throws IOException, BLLException {
        ArrayList<Event> result = new ArrayList<>();

        htmlDoc = Jsoup.parse(htmlFile, "UTF-8");

        //Список всех тегов с именем TABLE
        Elements tables = htmlDoc.body().getElementsByTag("TABLE");

        Event event;
        String eventName;
        Date eventDate;
        boolean activity;
        for (Element el : tables) {

            //обрабатываем только элементы, содержащие строку "Активность программ:"
            if (el.select(".RedData").text().contains("Активность программ:")) {
                eventName = el.select(".BlueDate").text().replace("Программа: ", "");

                //поиск тэгов с состоянием
                if (el.select(".BlackBold").text().contains("Запуск")) {
                    activity = true;
                } else if (el.select(".BlackBold").text().contains("Выход")) {
                    activity = false;
                } else
                    throw new BLLException(String.format("Не найдено состояние (запуск/выход): %s", eventName));

                //поиск тэгов с датой
                try {
                    String time = el.select(".BlackDate").text().replace("Дата: ", "");
                    eventDate = TIME_HTML.parse(time);
                } catch (ParseException e) {
                    eventDate = null;
                }

                //создаем событие и добавляем в результирующий список
                if (eventName != null) {
                    //разделяем имя и путь приложения
                    String[] nameAndPath = eventName.split(" - ");
                    event = new Event(nameAndPath[0], nameAndPath[1]);
                    //задаем дату
                    if (eventDate != null) {
                        event.setDate1(eventDate);
                    } else
                        throw new BLLException(String.format("Не найдена дата: %s", event.getName1()));
                    //изменяем тип (запуск или выход)
                    event.setStarting(activity);
                    //добавляем готовое собыите в результирующий список
                    result.add(event);
                }
            }


        }

        return result;
    }


    /**
     * Все события "активность компьютера"
     *
     * @param htmlFile html-файл (созданный программой mipko)
     * @return список событий "активность компьютера"
     */
    public ArrayList<Event> getActiveComputerFromFile(File htmlFile) throws IOException, BLLException {
        ArrayList<Event> result = new ArrayList<>();

        htmlDoc = Jsoup.parse(htmlFile, "UTF-8");

        //Список всех тегов с именем TABLE
        Elements tables = htmlDoc.body().getElementsByTag("TABLE");

        Event event;
        Date eventDate;
        Date lastTimeReport = null;

        for (Element el : tables) {

            //находим дату окончания отчета
            String lastTimeReportStr = el.select(".head2").text();
            if(lastTimeReportStr.contains("от") && lastTimeReportStr.contains("до")){
                try {
                    String s = lastTimeReportStr.replaceAll("(.)+от(.)+до", "").replaceAll("^\\s+", "").replaceAll("\\s+\\S+$", "");
                    //System.out.println(s);
                    try {
                        lastTimeReport = TIME_HTML.parse(s);
                    } catch (ParseException e) {
                        lastTimeReport = TIME_HTML_00_00.parse(String.format("%s 0:00:00",s));
                    }
                } catch (ParseException e) {
                    throw new BLLException(String.format("Ошибка при поиске даты отчета: %s", e));
                }
            }

        }


        for (Element el : tables) {

            //обрабатываем только элементы, содержащие строку "Активность компьютера:"
            if (el.select(".RedData").text().contains("Активность компьютера:")) {

                //поиск тэгов с датой
                try {
                    String time = el.select(".BlackDate").text().replace("Дата: ", "");
                    eventDate = TIME_HTML.parse(time);
                } catch (ParseException e) {
                    throw new BLLException("Не найдена дата события Активность компьютера");
                }

                //поиск тэгов с состоянием
                if (el.select(".BlackBold").text().contains("Запуск") || el.select(".BlackBold").text().contains("Активир")) {
                    //создаем событие
                    event = new Event("Активность компьютера", "none");
                    //задаем дату
                    event.setDate1(eventDate);
                    event.setDate2(lastTimeReport);
                    //изменяем тип (запуск/выключение)
                    event.setStarting(true);
                    //добавляем готовое собыите в результирующий список
                    result.add(event);
                } else if (el.select(".BlackBold").text().contains("Конец") || el.select(".BlackBold").text().contains("Выключ") || el.select(".BlackBold").text().contains("Заблок") || el.select(".BlackBold").text().contains("Останов")) {
                    //создаем событие
                    event = new Event("Активность компьютера", "none");
                    //задаем дату
                    event.setDate1(eventDate);
                    //изменяем тип (запуск/выключение)
                    event.setStarting(false);
                    //добавляем готовое собыите в результирующий список
                    result.add(event);
                }

            }
        }

        return result;
    }


}

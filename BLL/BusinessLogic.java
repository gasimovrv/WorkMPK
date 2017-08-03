package BLL;

import UI.Time;
import entities.Event;
import entities.FolderEvent;
import filesWork.HTMLtoEvents;
import filesWork.UnZip;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;


/**
 * Логика работы
 */
public class BusinessLogic implements Time {

    private File resultFile;//файл, в котором объединяем все отчеты
    private File directory;//рабочая директория(input)
    private Date startTime = null;
    private Date endTime = null;


    public BusinessLogic(String directory) throws BLLException {
        this.directory = new File(directory);
        try {
            if (!this.directory.exists()) {
                throw new NullPointerException(String.format("Директория [%s] не существует", this.directory.getPath()));
            }
        } catch (NullPointerException e) {
            throw new BLLException(String.format("%s", e.getMessage()));
        }
    }


    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }


    /**
     * распаковка и удаление архивов из директории
     */
    public boolean zipOperation() throws BLLException {
        int zipCount = 0;
        int htmlCount = 0;
        UnZip app = new UnZip();
        try {
            //получаем список архивов
            File[] files = directory.listFiles();

            //проверяем чтобы папка не была пуста
            if (files == null || files.length == 0) {
                throw new BLLException(String.format("Папка [%s] пуста", directory.getPath()));
            }

            //проверяем чтобы в папке были только архивы или только html-файлы
            for (File file : files) {
                if (file.getName().endsWith(".zip")) {
                    if (htmlCount > 0)
                        throw new BLLException(String.format("Структура папки [%s] неверная, папка должна содержать только zip-архивы или только html-файлы", directory.getPath()));
                    zipCount++;
                } else if (file.getName().endsWith(".htm") || file.getName().endsWith(".html")) {
                    if (zipCount > 0)
                        throw new BLLException(String.format("Структура папки [%s] неверная, папка должна содержать только zip-архивы или только html-файлы", directory.getPath()));
                    htmlCount++;
                } else
                    throw new BLLException(String.format("Структура папки [%s] неверная, папка должна содержать только zip-архивы или только html-файлы", directory.getPath()));
            }

            //выходим из метода если в папке только html-файлы
            if (zipCount == 0) {
                return false;
            }

            //распаковываем и удаляем архивы
            for (File file : files) {
                app.unZip(file.getAbsolutePath());
                file.delete();
            }
            return true;

        } catch (Exception e) {
            throw new BLLException(String.format("Ошибка чтения zip-архива: %s", e));
        }
    }


    /**
     * события - ежеминутные скриншоты
     * чтение и удаление html-файлов из директории
     * метод объединяет повторяющиеся события с увеличением их рабочего времени
     *
     * @return общий список событий из всех файлов
     */
    public ArrayList<Event> getScreenshotEvents(File outFile) throws BLLException {
        ArrayList<Event> result = new ArrayList<>();
        HTMLtoEvents hte = new HTMLtoEvents();

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), Charset.forName("UTF-8"))))) {
            //получаем список файлов
            File[] files = directory.listFiles();
            //парсим html-файлы
            for (File file : files) {
                //получаем все события из файла
                ArrayList<Event> eventsFromFile = hte.getScreenshotEventsFromFile(file);

                for (Event event : eventsFromFile) {
                    writer.printf("%s\t-\t%s%s", TIME_FILE_CONTENT.format(event.getDate1()), event.toString(), System.lineSeparator());
                    //находим самую раннюю и самую позднюю даты событий
                    if (endTime == null && startTime == null) {
                        startTime = endTime = event.getDate1();
                    } else if (event.getDate1().before(startTime)) {
                        startTime = event.getDate1();
                    } else if (event.getDate1().after(endTime)) {
                        endTime = event.getDate1();
                    }

                    if (result.contains(event)) {
                        //если событие уже есть в списке, то добавляем к нему 1 минуту
                        result.get(result.indexOf(event)).addWorkTime(60000);
                    } else {
                        //если события еще не было, то добавляем к нему 1 минуту и добавляем его в список
                        event.addWorkTime(60000);
                        result.add(event);
                    }
                }
                file.delete();
            }
        } catch (Exception e) {
            throw new BLLException(String.format("Ошибка при парсинге html-файла: %s", e));
        }
        return result;
    }


    /**
     * события - активность программ
     * чтение и удаление html-файлов из директории
     * метод расчитывает время активности определенных событий и объединяет их
     *
     * @return общий список событий из всех файлов
     */
    public ArrayList<Event> getActiveEvents(File outFile) throws BLLException {
        ArrayList<Event> resultList = new ArrayList<>();
        HTMLtoEvents hte = new HTMLtoEvents();

        try (PrintWriter wToOutFile = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), Charset.forName("UTF-8"))))) {

            //записываем все html-файлы в resultFile
            writeResultFile();

            //получаем все события из файла, сортируем список
            ArrayList<Event> eventsFromFile = hte.getActiveEventsFromFile(resultFile);
            Collections.sort(eventsFromFile);

            //копируем исходный файл в выходную папку
            Path inPath = Paths.get(resultFile.getAbsolutePath()).normalize();
            Path outPath = Paths.get(outFile.getParent() + "\\result.html").normalize();
            //если файл result.html уже есть в выходной папке, то заменяем его
            Files.copy(inPath, outPath, StandardCopyOption.REPLACE_EXISTING);

            //фильтруем события и составляем итоговый список, а также записываем системный файл со всеми неотфильтрованными событиями
            for (Event event : eventsFromFile) {
                wToOutFile.printf("%s\t-\t%s\t-\t%s%s", TIME_FILE_CONTENT.format(event.getDate1()), (event.isStarting() ? "Запуск" : "Выход"), event.toString(), System.lineSeparator());

                //ищем текущее событие в общем списке
                Event eventFromResult = null;
                if (resultList.indexOf(event) >= 0)
                    eventFromResult = resultList.get(resultList.indexOf(event));

                //находим самую раннюю и самую позднюю даты событий
                if (endTime == null && startTime == null) {
                    startTime = endTime = event.getDate1();
                } else if (event.getDate1().before(startTime)) {
                    startTime = event.getDate1();
                } else if (event.getDate1().after(endTime)) {
                    endTime = event.getDate1();
                }


                //если событие уже есть в списке (c типом "Запуск") и добавляемое имеет тип "Выход"
                if (eventFromResult != null && eventFromResult.isStarting() && !event.isStarting()) {
                    if (event.getDate1().after(eventFromResult.getDate1()) || event.getDate1().equals(eventFromResult.getDate1())) {
                        eventFromResult.addWorkTime(event.getDate1().getTime() - eventFromResult.getDate1().getTime());
                        eventFromResult.setDate1(event.getDate1());
                        eventFromResult.setStarting(false);
                    } else
                        throw new BLLException("Неверный порядок следования событий: найдено событие \"Выход\" раньше существующего");
                }
                //если событие уже есть в списке (c типом "Выход") и добавляемое имеет тип "Запуск"
                if (eventFromResult != null && !eventFromResult.isStarting() && event.isStarting()) {
                    if (event.getDate1().after(eventFromResult.getDate1()) || event.getDate1().equals(eventFromResult.getDate1())) {
                        eventFromResult.setDate1(event.getDate1());
                        eventFromResult.setStarting(true);
                    } else {
                        throw new BLLException("Неверный порядок следования событий: найдено событие \"Запуск\" раньше существующего");
                    }
                }
                //если событие уже есть в списке (c типом "Выход") и добавляемое имеет тип "Выход"
                if (eventFromResult != null && !eventFromResult.isStarting() && !event.isStarting()) {
                    if (event.getDate1().after(eventFromResult.getDate1())) {
                        eventFromResult.addWorkTime(event.getDate1().getTime() - eventFromResult.getDate1().getTime());
                        eventFromResult.setDate1(event.getDate1());
                    }
                }
                //если события еще не было, то добавляем его в список
                if (eventFromResult == null && event.isStarting()) {
                    resultList.add(event);
                }
            }

        } catch (Exception e) {
            throw new BLLException(String.format("Ошибка при парсинге html-файла: %s", e));
        }
        return resultList;
    }


    /**
     * события - активность компьютера
     * чтение и удаление html-файлов из директории
     * метод расчитывает общее время активности компьютера
     *
     * @return общий список событий из всех файлов
     */
    public ArrayList<Event> getActiveComputer(File outFile) throws BLLException {
        ArrayList<Event> resultList = new ArrayList<>();
        HTMLtoEvents hte = new HTMLtoEvents();

        try (PrintWriter wToOutFile = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), Charset.forName("UTF-8"))))) {

            //записываем все html-файлы в resultFile
            writeResultFile();

            //получаем все события из файла, сортируем список
            ArrayList<Event> eventsFromFile = hte.getActiveComputerFromFile(resultFile);
            Collections.sort(eventsFromFile);

            //копируем исходный файл в выходную папку
            Path inPath = Paths.get(resultFile.getAbsolutePath()).normalize();
            Path outPath = Paths.get(outFile.getParent() + "\\result.html").normalize();
            //если файл result.html уже есть в выходной папке, то заменяем его
            Files.copy(inPath, outPath, StandardCopyOption.REPLACE_EXISTING);

            Event eventFromResult;
            //фильтруем события и составляем итоговый список, а также записываем системный файл со всеми неотфильтрованными событиями
            wToOutFile.println("Активность компьютера:");
            for (Event event : eventsFromFile) {
                wToOutFile.printf("%s - %s", TIME_FILE_CONTENT.format(event.getDate1()), (event.isStarting() ? "Запуск" : "Выключение"));
                if (event.getDate2() != null)
                    wToOutFile.printf(" (резервная дата выкл. - %s)%s", TIME_FILE_CONTENT.format(event.getDate2()), System.lineSeparator());
                else
                    wToOutFile.print(System.lineSeparator());

                //находим самую раннюю и самую позднюю даты событий
                if (endTime == null && startTime == null) {
                    startTime = endTime = event.getDate1();
                } else if (event.getDate1().before(startTime)) {
                    startTime = event.getDate1();
                } else if (event.getDate1().after(endTime)) {
                    endTime = event.getDate1();
                }

                eventFromResult = null;
                //ищем текущее событие в общем списке
                if (resultList.lastIndexOf(event) >= 0)
                    eventFromResult = resultList.get(resultList.lastIndexOf(event));

                //если событие уже есть в списке (c типом "Запуск") и добавляемое имеет тип "Выключение"
                if (eventFromResult != null && eventFromResult.isStarting() && !event.isStarting()) {
                    eventFromResult.setDate2(event.getDate1());
                    eventFromResult.addWorkTime(eventFromResult.getDate2().getTime() - eventFromResult.getDate1().getTime());
                    eventFromResult.setStarting(false);
                }
                //если событие имеет тип "Запуск", то добавляем его в список
                if (event.isStarting()) {
//--------------------------------добавляем событие только если оно первое или существующее последнее имеет тип "Выключение"--------------------------------------------------------------новое условие учитывает также время простоя (подряд 2 события запуска)
                    if (eventFromResult == null || !eventFromResult.isStarting()) {
                        resultList.add(event);
                    }
                }
            }

            //если последнее событие оказалось запуском, то добавляем ему время до конца отчета
            if (resultList.size() > 0) {
                eventFromResult = resultList.get(resultList.size() - 1);
                if (eventFromResult.isStarting()) {
                    eventFromResult.addWorkTime(eventFromResult.getDate2().getTime() - eventFromResult.getDate1().getTime());
                }
            }

        } catch (Exception e) {
            throw new BLLException(String.format("Ошибка при парсинге html-файла: %s", e));
        }
        return resultList;
    }

    /**
     * Получить события сгруппированные по папкам
     *
     * @param events список событий
     * @return список папок
     */
    public ArrayList<FolderEvent> getFolders(ArrayList<Event> events) throws BLLException {
        ArrayList<FolderEvent> folderEvents = new ArrayList<>();
        ArrayList<FolderEvent> result = new ArrayList<>();

        StringBuilder folderName = new StringBuilder();

        try {
            for (Event temp : events) {
                String[] path = temp.getName2().split("\\\\");
                for (int i = 0; i < path.length; i++) {
                    if (i <= 1) {
                        folderName.append(path[i]).append("\\");
                    } else break;
                }
                folderEvents.add(new FolderEvent(folderName.toString(), temp.getWorkTime()));
                folderName.delete(0, folderName.length());
            }

            for (FolderEvent folderEvent : folderEvents) {
                if (result.contains(folderEvent)) {
                    //если событие уже было, то добавляем к нему время найденного события
                    result.get(result.indexOf(folderEvent)).appendTime(folderEvent.getWorkTime());
                } else {
                    result.add(folderEvent);
                }
            }
        } catch (Exception e) {
            throw new BLLException(String.format("Ошибка при группировке по папкам: %s", e));
        }
        return result;
    }


    /**
     * Получить события активностей компьютера сгруппированные по дням
     *
     * @param events список событий
     * @return сгруппированный список событий
     */
    public ArrayList<Event> getActiveCompToDay(ArrayList<Event> events) throws BLLException {
        ArrayList<Event> result = new ArrayList<>();
        GregorianCalendar calendarResult = new GregorianCalendar();
        GregorianCalendar calendarEvent = new GregorianCalendar();

        try {
            for (Event event : events) {
                //получаем день для фильтрации
                calendarEvent.setTime(event.getDate1());

                Event eventFromResult = null;
                if (result.lastIndexOf(event) >= 0) {
                    //ищем текущее событие в общем списке
                    eventFromResult = result.get(result.lastIndexOf(event));
                    calendarResult.setTime(eventFromResult.getDate1());
                }

                //если событие в текущий день уже было, то добавляем к нему время найденного события
                if (eventFromResult != null && calendarResult.get(Calendar.DAY_OF_YEAR) == calendarEvent.get(Calendar.DAY_OF_YEAR)) {
                    eventFromResult.addWorkTime(event.getWorkTime());
                    eventFromResult.addEventsFromDay();
                } else {
                    result.add(event.clone());
                }

            }
        } catch (Exception e) {
            throw new BLLException(String.format("Ошибка при группировке по дням: %s", e));
        }
        return result;
    }


    private void writeResultFile() throws BLLException {
        if (resultFile == null) {
            try {
                //получаем список файлов
                File[] files = directory.listFiles();

                if (files == null)
                    throw new BLLException("Файлов для обработки не найдено");
                Arrays.sort(files);

                //записываем все html-файлы в resultFile
                resultFile = new File(directory + "\\result.html");
                FileOutputStream fos = new FileOutputStream(new File(directory + "\\result.html"), true);
                for (File f : files) {
                    fos.write(Files.readAllBytes(f.toPath()));
                }
                fos.close();
            } catch (Exception e) {
                throw new BLLException(String.format("Ошибка при парсинге html-файла: %s", e));
            }
        }
    }

    public void clearInput() throws BLLException{
        try {
            //получаем список файлов
            File[] files = directory.listFiles();
            for (File f : files) {
                Files.delete(f.toPath());
            }
        } catch (Exception e) {
            throw new BLLException(String.format("Ошибка при очистке входной папки: %s", e));
        }
    }
}

package entities;

import java.io.IOException;
import java.util.Date;

/**
 * Описание запусков приложений (событий)
 */
public class Event implements Comparable<Event> {
    private static char typeSort = 'd';

    private String name1;
    private String name2;
    private long workTime;
    private Date date1;
    private Date date2;
    private boolean starting = false;
    private int eventsFromDay = 1;

    public Event(String name1, String name2) {
        this.name1 = name1;
        this.name2 = name2;
    }

    /**
     * задать тип сортировки
     * @param type 'd' - по дате, 'w' - по количеству отработанного времени
     */
    public static void setTypeSort(final char type) throws IOException {
        if(type!='d' && type !='w')
            throw new IOException("Ошибка сортировки: аргументы функции setTypeSort() необходимо выбрать из: 'd' или 'w'");
        typeSort = type;
    }

    public int getEventsFromDay() {
        return eventsFromDay;
    }

    public void addEventsFromDay() {
        eventsFromDay++;
    }

    public long getWorkTime() {
        return workTime;
    }

    public void setWorkTime(long workTime) {
        this.workTime = workTime;
    }

    public void addWorkTime(long workTime) {
        this.workTime += workTime;
    }

    public Date getDate1() {
        return date1;
    }

    public void setDate1(Date date1) {
        this.date1 = date1;
    }

    public Date getDate2() {
        return date2;
    }

    public void setDate2(Date date2) {
        this.date2 = date2;
    }

    public String getName1() {
        return name1;
    }

    public void setName1(String name1) {
        this.name1 = name1;
    }

    public String getName2() {
        return name2;
    }

    public void setName2(String name2) {
        this.name2 = name2;
    }

    public boolean isStarting() {
        return starting;
    }

    public void setStarting(boolean starting) {
        this.starting = starting;
    }

    @Override
    public String toString() {
        return String.format("%s [%s];", name1, name2);
    }

    @Override
    public boolean equals(Object obj) {
        return (name1.equals(((Event) obj).getName1()) && name2.equals(((Event) obj).getName2()));
    }

    @Override
    public int compareTo(Event o) {
        switch (typeSort) {
            case 'd':
                if (this.date1.after(o.getDate1()))
                    return 1;
                else if (this.date1.before(o.getDate1()))
                    return -1;
            case 'w':
                if (this.workTime > o.getWorkTime())
                    return -1;
                else if (this.workTime < o.getWorkTime())
                    return 1;
        }
        return 0;
    }

    @Override
    public Event clone() throws CloneNotSupportedException {
        Event e = new Event(name1, name2);
        e.setStarting(starting);
        e.setDate1(date1);
        e.setDate2(date2);
        e.setWorkTime(workTime);
        return e;
    }
}

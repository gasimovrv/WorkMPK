package entities;


/**
 * События, запущенные из одной папки
 */
public class FolderEvent implements Comparable<FolderEvent> {
    private String folder;
    private long workTime;

    public FolderEvent(String folder, long workTime) {
        this.folder = folder;
        this.workTime = workTime;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public long getWorkTime() {
        return workTime;
    }

    public void appendTime(long time) {
        workTime += time;
    }

    @Override
    public String toString() {
        return String.format("%s;", folder);
    }

    @Override
    public boolean equals(Object obj) {
        return folder.equals(((FolderEvent) obj).getFolder());
    }

    @Override
    public int compareTo(FolderEvent o) {
        if (this.workTime > o.getWorkTime())
            return -1;
        else if (this.workTime < o.getWorkTime())
            return 1;
        else
            return 0;
    }
}

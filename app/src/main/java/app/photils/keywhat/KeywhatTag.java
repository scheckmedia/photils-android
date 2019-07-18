package app.photils.keywhat;

public class KeywhatTag {
    private int tid;
    private String name;
    private boolean isSelected;

    public KeywhatTag(int tid, String name, boolean isSelected) {
        this.tid = tid;
        this.name = name;
        this.isSelected = isSelected;
    }

    public int getTid() {
        return tid;
    }

    public String getName() {
        return name;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}

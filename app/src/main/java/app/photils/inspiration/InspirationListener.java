package app.photils.inspiration;

public interface InspirationListener {
    void onGazeProgress(int progress);
    void onGazeSelected();
    void onGazeUnselect();
}

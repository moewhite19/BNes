package cn.whiteg.bnes.utils;

public class FpsMonitor {
    long nextUpdateTime = System.currentTimeMillis();
    float fps;
    int count;

    public void draw() {
        long now = System.currentTimeMillis();
        count++;
        if (now > nextUpdateTime){
            nextUpdateTime = now + 1000;
            fps = (fps + count) / 2;
            count = 0;
        }
    }

    public float getFps() {
        return fps;
    }

    public String getFpsText() {
        return String.format("%.1f",getFps());
    }
}

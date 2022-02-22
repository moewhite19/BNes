package cn.whiteg.bnes.utils;

public class PerformanceRecorder {
    private String name;
    private long millis;
    private long nano;

    public PerformanceRecorder() {
        this(PerformanceRecorder.class.getSimpleName());
    }

    PerformanceRecorder(String name) {
        reset(name);
    }

    public void reset(String name) {
        this.name = name.concat(":");
        millis = System.currentTimeMillis();
        nano = System.nanoTime();
    }

    public void reset() {
        millis = System.currentTimeMillis();
        nano = System.nanoTime();
    }

    public String out() {
        var m = System.currentTimeMillis() - millis;
        var n = System.nanoTime() - nano;
        return m + "~" + n;
    }

    public PerformanceRecorder print() {
        System.out.println(name.concat(out()));
        return this;
    }
}

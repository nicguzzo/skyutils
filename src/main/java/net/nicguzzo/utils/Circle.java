package net.nicguzzo.utils;

public class Circle {
    public int cx;
    public int cz;
    public int rad;// rad^2
    private int rad2;
    public Circle(int cx, int cz, int rad) {
        this.cx = cx;
        this.cz = cz;
        this.rad = rad;
        this.rad2=rad*rad;
    }

    public boolean inside(int x, int z) {
        x = x - cx;
        z = z - cz;
        return (x * x + z * z) <= rad2;
    }
}
package net.nicguzzo.utils;

class Parabola {
    double a;
    int cx;
    int cy;
    int cz;

    Parabola(int _cx, int _cy, int _cz, double _a) {
        a = _a;
        cx = _cx;
        cy = _cy;
        cz = _cz;
    }

    public boolean inside(int x, int y, int z) {
        x = x - cx;
        y = y - cy;
        z = z - cz;
        return (x * x + z * z) - (a * y) <= 0;
    }
}
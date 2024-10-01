package org.urbcomp.startdb.compress.elf.utils;

public class Couple<L, R> {
    private L left;
    private R right;

    public Couple(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getL() { return left; }
    public R getR() { return right; }
}

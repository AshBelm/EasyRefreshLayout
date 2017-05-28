package com.mcmo.easyrefreshlayout.library;

/**
 * Created by ZhangWei on 2017/5/24.
 */

public enum  SpringDock {
    BEFORE(0),AFTER(1);

    private int value;

    SpringDock(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
    public static SpringDock create(int value){
        switch (value){
            case 0:
                return BEFORE;
            case 1:
                return AFTER;
            default:
                return BEFORE;
        }
    }
}

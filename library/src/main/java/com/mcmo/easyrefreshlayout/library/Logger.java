package com.mcmo.easyrefreshlayout.library;

import android.util.Log;


/**
 * Created by ZhangWei on 2017/5/22.
 */

public class Logger {
    private static final String TAG = "Logger";
    public static final boolean IS_PRINT_LOG = true;

    public static String getLogString(String msg){
        StackTraceElement stackTraceElement[]=Thread.currentThread().getStackTrace();
        int index = 0;
        //获取代码所运行的位置
        for (StackTraceElement e : stackTraceElement) {
            String name = e.getClassName();
            if (!name.equals(Logger.class.getName())) {
                index++;
            } else {
                break;
            }
        }
        index+=2;
        String fullClassName = stackTraceElement[index].getClassName();
        String className = fullClassName.substring(fullClassName.lastIndexOf(".")+1);
        if(className.contains("$"))
            className = className.substring(0,className.lastIndexOf("$"));
        String methodName = stackTraceElement[index].getMethodName();
        String lineNumber = String.valueOf(stackTraceElement[index].getLineNumber());
        return "("+className+".java:"+lineNumber+")   "+msg;
    }
    public static void e(String tag,String msg){
        if(IS_PRINT_LOG){
            Log.e(tag, getLogString(msg));
        }
    }
    public static void d(String tag,String msg){
        if(IS_PRINT_LOG){
            Log.d(tag, getLogString(msg));
        }
    }
    public static void i(String tag,String msg){
        if(IS_PRINT_LOG){
            Log.i(tag, getLogString(msg));
        }
    }
    public static void w(String tag,String msg){
        if(IS_PRINT_LOG){
            Log.w(tag, getLogString(msg));
        }
    }
    public static void v(String tag,String msg){
        if(IS_PRINT_LOG){
            Log.v(tag, getLogString(msg));
        }
    }
}

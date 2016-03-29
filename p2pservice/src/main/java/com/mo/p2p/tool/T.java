package com.mo.p2p.tool;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by MomxMo on 2015/12/16.
 * <p/>
 * Toast提示工具类
 */
public class T {
    private static Toast TOAST = null;
    /**
     * 在屏幕上输出 Toast
     *
     * @param context 上下文
     * @param resStr  字符串数据
     */
    public static void showToast(Context context, CharSequence resStr) {
        showToast(context, resStr, Toast.LENGTH_SHORT);
    }

    public static void showToast(Context context, CharSequence resStr,int duration) {
        if (TOAST != null) {
            TOAST.cancel();
        }
        TOAST = Toast.makeText(context, resStr, duration);
        TOAST.show();
    }

    /**
     * 在屏幕上输出 Toast
     *
     * @param context 上下文
     * @param resId   字符串数据的 Id
     */
    public static void showToast(Context context, int resId) {
        showToast(context, resId, Toast.LENGTH_SHORT);
    }

    public static void showToast(Context context, int resId, int duration) {
        showToast(context, context.getResources().getText(resId), duration);
    }
}
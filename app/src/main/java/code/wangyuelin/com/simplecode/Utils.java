package code.wangyuelin.com.simplecode;

import android.graphics.Paint;

/**
 * Created by wangyuelin on 2017/8/29.
 */

public class Utils {
    /**
     * 据文字的中心处获取绘制文字时的y坐标（即baseline坐标）
     * @param paint
     * @param center 文字的垂直中心坐标
     * @return
     */
    public static float getTextY(Paint paint, float center){
        Paint.FontMetrics fm = paint.getFontMetrics();
        return center + (fm.bottom - fm.top)/2 - fm.bottom;

    }
}

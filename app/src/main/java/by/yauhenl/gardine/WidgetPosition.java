package by.yauhenl.gardine;

import android.util.Log;
import android.view.Gravity;

public enum WidgetPosition {
    TOP_RIGHT(Gravity.TOP | Gravity.RIGHT, 1),
    TOP_LEFT(Gravity.TOP | Gravity.LEFT, -1),
    BOTTOM_RIGHT(Gravity.BOTTOM | Gravity.RIGHT, 1),
    BOTTOM_LEFT(Gravity.BOTTOM | Gravity.LEFT, -1);

    public final int gravity;
    public final int swipeDirection;

    WidgetPosition(int gravity, int swipeDirection) {
        this.gravity = gravity;
        this.swipeDirection = swipeDirection;
    }

    public static WidgetPosition parse(String name) {
        try {
            return WidgetPosition.valueOf(name);
        } catch (IllegalArgumentException e) {
            Log.w(LoggingUtils.PREFS,
                    "Not supported widget position preference value: " + name + ". Fallback to: " + TOP_RIGHT);
            return TOP_RIGHT;
        }
    }
}

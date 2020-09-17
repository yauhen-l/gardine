package by.yauhenl.gardine;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayDeque;
import java.util.ArrayList;

import static android.widget.AdapterView.INVALID_POSITION;

public class GardineWidgetService extends AccessibilityService {

    // TODO: move to preferences
    private static final int MAX_ITEMS = 6;
    private static final int SHOW_X_THRESHOLD = 30;
    private static final int SCROLL_Y_THRESHOLD = 45;
    private static final int VIBRATE_DURATION = 20;
    private static final int MAX_Y_DIFF = MAX_ITEMS * SCROLL_Y_THRESHOLD - SCROLL_Y_THRESHOLD / 2;
    private static final int MIN_Y_DIFF = 0;

    private WindowManager windowManager;
    private View widget;
    private DiscardingStack<App> recentActivities;
    private String currentAppPackage;
    private ArrayAdapter<App> recentAppsAdapter;
    private Vibrator vibrator;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    public GardineWidgetService() {
        this.recentActivities = new DiscardingStack<>(MAX_ITEMS);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        setServiceInfo(config);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        Log.d(LoggingUtils.EVENT_TAG, "Window in foreground: " + event.getPackageName());
        if (event.getPackageName() == null || event.getClassName() == null) {
            return;
        }

        ComponentName componentName = new ComponentName(
                event.getPackageName().toString(),
                event.getClassName().toString()
        );

        ActivityInfo activityInfo;
        PackageManager pm = this.getPackageManager();
        try {
            activityInfo = pm.getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(LoggingUtils.RECENT_APPS_TAG, "Ignore window of component: " + componentName);
            return;
        }

        this.currentAppPackage = event.getPackageName().toString();

        Intent startIntent = pm.getLaunchIntentForPackage(activityInfo.packageName);
        if (startIntent == null) {
            Log.d(LoggingUtils.RECENT_APPS_TAG, "Skipping package " + activityInfo.packageName + " due to absence of launch intent");
            return;
        }

        String label = pm.getApplicationLabel(activityInfo.applicationInfo).toString();
        App a = new App(label, activityInfo.packageName, startIntent);
        this.recentActivities.add(a);

        Log.i(LoggingUtils.RECENT_APPS_TAG, "Added app to the stack: " + a.toLogString());
    }


    @Override
    public void onInterrupt() {

    }

    private void actualize_widget_visibility(SharedPreferences sharedPreferences) {
        boolean isWidgetVisible = sharedPreferences.getBoolean(getString(R.string.pref_widget_enabled_key), true);
        if (isWidgetVisible) {
            widget.setVisibility(View.VISIBLE);
        } else {
            widget.setVisibility(View.GONE);
        }
    }

    private void actualize_widget_background(SharedPreferences prefs, View view) {
        final int defaultColor = ContextCompat.getColor(getApplicationContext(), R.color.hidden);
        int backgroundColor = prefs.getInt(getString(R.string.pref_widget_background_color_key), defaultColor);
        view.setBackgroundColor(backgroundColor);
    }


    @Override
    public void onCreate() {
        super.onCreate();

        this.vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        widget = LayoutInflater.from(this).inflate(R.layout.widget, null);

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.RIGHT | Gravity.TOP;
        params.x = 0;
        params.y = 0;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(widget, params);


        final View collapsedView = widget.findViewById(R.id.collapse_view);
        final View expandedView = widget.findViewById(R.id.gardine);
        final View rootContainer = widget.findViewById(R.id.root_container);

        widget.getViewTreeObserver().addOnGlobalLayoutListener(new AntipodeViewsLayoutListener(collapsedView, expandedView));

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        actualize_widget_visibility(sharedPreferences);
        actualize_widget_background(sharedPreferences, collapsedView);

        this.preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences changedPreferences, String key) {
                if (getString(R.string.pref_widget_enabled_key).equals(key)) {
                    actualize_widget_visibility(changedPreferences);
                    return;
                }
                if (getString(R.string.pref_widget_background_color_key).equals(key)) {
                    actualize_widget_background(changedPreferences, collapsedView);
                    return;
                }
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(this.preferenceChangeListener);

        this.recentAppsAdapter = new ArrayAdapter<>(
                this, R.layout.item, R.id.item,
                new ArrayList<App>(MAX_ITEMS));

        final ListView tasksList = (ListView) widget.findViewById(R.id.tasks_list);
        tasksList.setAdapter(this.recentAppsAdapter);

        rootContainer.setOnTouchListener(new View.OnTouchListener() {
            private float initialTouchX;
            private float initialShowY;
            private float initialShowX;

            private boolean isHidden() {
                return collapsedView.getVisibility() == View.VISIBLE;
            }

            private void hide() {
                expandedView.setVisibility(View.GONE);
                //collapsedView.setVisibility(View.VISIBLE);
            }

            private void show() {
                collapsedView.setVisibility(View.GONE);
                //expandedView.setVisibility(View.VISIBLE);
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(LoggingUtils.COORD_TAG, "DOWN at " + event.getRawX() + ", " + event.getRawY());
                        initialTouchX = event.getRawX();
                        return true;
                    case MotionEvent.ACTION_UP:
                        Log.d(LoggingUtils.COORD_TAG, "UP at " + event.getRawX() + ", " + event.getRawY());


                        if (!isHidden()) {
                            int checkedPos = tasksList.getCheckedItemPosition();
                            if (checkedPos != INVALID_POSITION) {
                                App selectedApp = (App) tasksList.getItemAtPosition(checkedPos);
                                if (selectedApp != null) {
                                    GardineWidgetService.this.startActivity(selectedApp.startIntent);
                                }
                            }
                            hide();
                        }

                        return true;
                    case MotionEvent.ACTION_MOVE:

                        if (isHidden()) {
                            if (initialTouchX - event.getRawX() > SHOW_X_THRESHOLD) {
                                GardineWidgetService.this.actualize_recent_apps();

                                this.initialShowX = event.getRawX();
                                this.initialShowY = event.getRawY();

                                show();
                            }
                        } else {
                            if (event.getRawX() - this.initialShowX > SHOW_X_THRESHOLD / 2) {
                                hide();
                                return true;
                            }

                            int yd = (int) (event.getRawY() - this.initialShowY);
                            int oyd = yd;
                            yd = Math.min(yd, MAX_Y_DIFF);
                            yd = Math.max(yd, MIN_Y_DIFF);
                            int correction = oyd - yd;
                            this.initialShowY += correction;

                            int itemsNumber = recentAppsAdapter.getCount();

                            if (itemsNumber > 0) {
                                int selectedItem = (yd / SCROLL_Y_THRESHOLD);
                                if (selectedItem < 0) {
                                    selectedItem = 0;
                                } else if (selectedItem >= itemsNumber) {
                                    selectedItem = itemsNumber - 1;
                                }

                                int prevItem = tasksList.getCheckedItemPosition();
                                if (prevItem != selectedItem) {
                                    Log.d(LoggingUtils.COORD_TAG, "Item changed at Ydiff=" + yd + ", prevItem=" + prevItem + ", curItem=" + selectedItem);
                                    vibrate_on_scroll();
                                    tasksList.setItemChecked(selectedItem, true);
                                }
                            }
                        }


                        return true;
                }
                return false;
            }
        });
    }

    private void vibrate_on_scroll() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        if (prefs.getBoolean(getString(R.string.pref_vibrate_on_scroll_key), true) && this.vibrator != null) {
            this.vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (widget != null) windowManager.removeView(widget);
    }

    private void actualize_recent_apps() {
        this.recentAppsAdapter.clear();

        ArrayDeque<App> recentApps = this.recentActivities.getAll();
        if (this.currentAppPackage != null) {
            boolean removed = recentApps.removeFirstOccurrence(new App(null, this.currentAppPackage, null));
            Log.d(LoggingUtils.RECENT_APPS_TAG, "Current app " + this.currentAppPackage + " has been removed: " + removed);
        }
        this.recentAppsAdapter.addAll(recentApps);
        this.recentAppsAdapter.notifyDataSetChanged();
    }
}

package by.yauhenl.gardine;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayDeque;
import java.util.ArrayList;

import static android.widget.AdapterView.INVALID_POSITION;

public class GardineWidgetService extends AccessibilityService {

    public static final String LOG_TAG_COORD = "coord";
    public static final String LOG_TAG_RECENT_APPS = "recent_apps";
    public static final String LOG_TAG_EVENT = "event";

    // TODO: move to preferences
    private static final int MAX_ITEMS = 6;
    private static final int SHOW_X_THRESHOLD = 30;
    private static final int SCROLL_Y_THRESHOLD = 45;
    private static final int VIBRATE_DURATION = 20;
    private static final int MAX_Y_DIFF = MAX_ITEMS * SCROLL_Y_THRESHOLD - SCROLL_Y_THRESHOLD/2;
    private static final int MIN_Y_DIFF = 0;

    private WindowManager windowManager;
    private View gardine;
    private DiscardingStack<App> recentActivities;
    private String currentAppPackage;
    private ArrayAdapter<App> recentAppsAdapter;
    private Vibrator vibrator;

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
        Log.d(LOG_TAG_EVENT, "Window in foreground: " + event.getPackageName());
        if (event.getPackageName() == null || event.getClassName() == null) {
            return;
        }
        this.currentAppPackage = event.getPackageName().toString();

        ComponentName componentName = new ComponentName(
                event.getPackageName().toString(),
                event.getClassName().toString()
        );

        ActivityInfo activityInfo;
        PackageManager pm = this.getPackageManager();
        try {
            activityInfo = pm.getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(LOG_TAG_RECENT_APPS, "Ignore window of component: " + componentName);
            return;
        }
        Intent startIntent = pm.getLaunchIntentForPackage(activityInfo.packageName);
        if (startIntent == null) {
            Log.d(LOG_TAG_RECENT_APPS, "Skipping package " + activityInfo.packageName + " due to absence of launch intent");
            return;
        }

        String label = pm.getApplicationLabel(activityInfo.applicationInfo).toString();
        App a = new App(label, activityInfo.packageName, startIntent);
        this.recentActivities.add(a);

        Log.i(LOG_TAG_RECENT_APPS, "Added app to the stack: " + a.toLogString());
    }


    @Override
    public void onInterrupt() {

    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        gardine = LayoutInflater.from(this).inflate(R.layout.gardine, null);

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
        windowManager.addView(gardine, params);

        final View collapsedView = gardine.findViewById(R.id.collapse_view);
        final View expandedView = gardine.findViewById(R.id.gardine);
        final View rootContainer = gardine.findViewById(R.id.root_container);

        this.recentAppsAdapter = new ArrayAdapter<>(
                this, R.layout.item, R.id.item,
                new ArrayList<App>(MAX_ITEMS));

        final ListView tasksList = (ListView) gardine.findViewById(R.id.tasks_list);
        tasksList.setAdapter(this.recentAppsAdapter);

        ImageView closeButtonCollapsed = (ImageView) gardine.findViewById(R.id.destroy_btn);
        closeButtonCollapsed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSelf();
            }
        });

        rootContainer.setOnTouchListener(new View.OnTouchListener() {
            private float initialTouchX;
            private float initialShowY;
            private float initialShowX;

            private boolean isHidden() {
                return collapsedView.getVisibility() == View.VISIBLE;
            }

            private void hide() {
                expandedView.setVisibility(View.GONE);
                collapsedView.setVisibility(View.VISIBLE);
            }

            private void show() {
                collapsedView.setVisibility(View.GONE);
                expandedView.setVisibility(View.VISIBLE);
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(LOG_TAG_COORD, "DOWN at " + event.getRawX() + ", " + event.getRawY());
                        initialTouchX = event.getRawX();
                        return true;
                    case MotionEvent.ACTION_UP:
                        Log.d(LOG_TAG_COORD, "UP at " + event.getRawX() + ", " + event.getRawY());


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
                                GardineWidgetService.this.actualizeRecentApps();

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
                                if(selectedItem < 0) {
                                    selectedItem = 0;
                                } else if (selectedItem >= itemsNumber) {
                                    selectedItem = itemsNumber - 1;
                                }

                                int prevItem = tasksList.getCheckedItemPosition();
                                if (prevItem != selectedItem) {
                                    Log.d(LOG_TAG_COORD, "Vibrate at Ydiff=" + yd + ", prevItem=" + prevItem + ", curItem=" + selectedItem);
                                    vibrator.vibrate(VIBRATE_DURATION);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (gardine != null) windowManager.removeView(gardine);
    }

    private void actualizeRecentApps() {
        this.recentAppsAdapter.clear();

        ArrayDeque<App> recentApps = this.recentActivities.getAll();
        if(this.currentAppPackage != null) {
            boolean removed = recentApps.removeFirstOccurrence(new App(null, this.currentAppPackage, null));
            Log.d(LOG_TAG_RECENT_APPS, "Current app " + this.currentAppPackage + " has been removed: " + removed);
        }
        this.recentAppsAdapter.addAll(recentApps);
        this.recentAppsAdapter.notifyDataSetChanged();
    }
}

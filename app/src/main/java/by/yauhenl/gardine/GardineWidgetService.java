package by.yauhenl.gardine;

import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GardineWidgetService extends Service {

    public static final String LOG_TAG_COORD = "coord";
    public static final String LOG_TAG_RECENT_APPS = "recent_apps";

    private WindowManager windowManager;
    private View gardine;
    private ArrayList<App> recentApps;
    private ArrayAdapter<App> recentAppsAdapter;

    public GardineWidgetService() {
        this.recentApps = new ArrayList<>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        gardine = LayoutInflater.from(this).inflate(R.layout.gardine, null);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
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
                this.recentApps);

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

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d("gardine", "DOWN at " + event.getRawX() + ", " + event.getRawY());
                        initialTouchX = event.getRawX();
                        return true;
                    case MotionEvent.ACTION_UP:
                        Log.d("gardine", "UP at " + event.getRawX() + ", " + event.getRawY());

                        collapsedView.setVisibility(View.VISIBLE);
                        expandedView.setVisibility(View.GONE);

                        App selectedApp = (App)tasksList.getItemAtPosition(tasksList.getCheckedItemPosition());
                        if(selectedApp != null) {
                            GardineWidgetService.this.startActivity(selectedApp.startIntent);
                        }

                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int Xdiff = (int) (event.getRawX() - initialTouchX);

                        if (Math.abs(Xdiff) > 30) {
                            if (isViewCollapsed()) {
                                GardineWidgetService.this.actualizeRecentApps();

                                this.initialShowY = event.getRawY();

                                collapsedView.setVisibility(View.GONE);
                                expandedView.setVisibility(View.VISIBLE);

                            }
                        }
                        if(!isViewCollapsed()) {
                            int Ydiff = (int) (event.getRawY() - this.initialShowY);
                            int itemsNumber = recentAppsAdapter.getCount();

                            if (itemsNumber > 0) {
                                int selectedItem = Math.abs((Ydiff / 30) % itemsNumber);
                                int prevItem = tasksList.getSelectedItemPosition();
                                if (prevItem != selectedItem) {
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
        this.recentApps.clear();

        PackageManager pm = this.getPackageManager();

        UsageStatsManager usm = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageStats> applist = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 100 * 1000, time);
        for (UsageStats us : applist) {
            Log.d(LOG_TAG_RECENT_APPS, us.getPackageName() + ": inForeground= " + us.getTotalTimeInForeground() + ", lastTimeUsed= " + us.getLastTimeUsed());
            if (us.getTotalTimeInForeground() == 0 ||
                    us.getLastTimeUsed() == 0) {
                continue;
            }
            Intent startIntent = pm.getLaunchIntentForPackage(us.getPackageName());
            if (startIntent == null) {
                Log.d(LOG_TAG_RECENT_APPS, "Skipping package " + us.getPackageName() + " due to absence of launch intent");
                continue;
            }
            ApplicationInfo info = null;
            String label = null;
            try {
                info = pm.getApplicationInfo(us.getPackageName(), PackageManager.GET_META_DATA);
                label = pm.getApplicationLabel(info).toString();
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(LOG_TAG_RECENT_APPS, "Bad package: " + us.getPackageName(), e);
                continue;
            }
            this.recentApps.add(new App(label, us.getPackageName(), us.getLastTimeUsed(), startIntent));
        }
        Log.i(LOG_TAG_RECENT_APPS, "Sorting " + this.recentApps.size() + " recent apps");

        Collections.sort(this.recentApps);
        this.recentAppsAdapter.notifyDataSetChanged();
    }
}

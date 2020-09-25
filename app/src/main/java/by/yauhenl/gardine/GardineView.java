package by.yauhenl.gardine;

import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collection;

import static android.content.Context.WINDOW_SERVICE;

public class GardineView {

    private final View widget, collapsedView, expandedView;
    private final ListView tasksList;
    private final WindowManager windowManager;
    private final AppArrayAdapter recentAppsAdapter;
    private final DisplayMetrics displatMetrics;

    public GardineView(GardineWidgetService ctx) {
        this.displatMetrics = ctx.getResources().getDisplayMetrics();
        this.widget = LayoutInflater.from(ctx).inflate(R.layout.widget, null);

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

        this.windowManager = (WindowManager) ctx.getSystemService(WINDOW_SERVICE);
        windowManager.addView(widget, params);

        collapsedView = widget.findViewById(R.id.collapse_view);
        expandedView = widget.findViewById(R.id.gardine);
        final View rootContainer = widget.findViewById(R.id.root_container);

        //widget.getViewTreeObserver().addOnGlobalLayoutListener(new AntipodeViewsLayoutListener(collapsedView, expandedView));

        this.recentAppsAdapter = new AppArrayAdapter(
                ctx, R.layout.item, R.id.app_title, R.id.app_icon,
                new ArrayList<App>(ctx.getMaxItems()));

        this.tasksList = widget.findViewById(R.id.tasks_list);
        tasksList.setAdapter(this.recentAppsAdapter);

        rootContainer.setOnTouchListener(new WidgetTouchListener(ctx, this));
    }

    public void setUseIcons(boolean useIcons) {
        this.recentAppsAdapter.setUseIcon(useIcons);
    }

    public void destroy() {
        if (widget != null) windowManager.removeView(widget);
    }

    public void setApps(Collection<App> apps) {
        this.recentAppsAdapter.clear();
        this.recentAppsAdapter.addAll(apps);
        this.recentAppsAdapter.notifyDataSetChanged();
    }

    public void setEnabled(boolean enabled) {
        this.widget.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    public void setCollapsedBackground(int color) {
        this.collapsedView.setBackgroundColor(color);
    }

    public void setCollapsedHeight(int heightDp) {
        ViewGroup.LayoutParams params = this.collapsedView.getLayoutParams();
        params.height = dpToPx(heightDp);
        this.collapsedView.setLayoutParams(params);
    }

    public void setCollapsedWidth(int widthDp) {
        ViewGroup.LayoutParams params = this.collapsedView.getLayoutParams();
        params.width = dpToPx(widthDp);
        this.collapsedView.setLayoutParams(params);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displatMetrics);
    }

    public void setExpandedBackground(int color) {
        this.expandedView.setBackgroundColor(color);
    }

    public int getItemsCount() {
        return this.recentAppsAdapter.getCount();
    }

    public boolean isHidden() {
        return collapsedView.getVisibility() == View.VISIBLE;
    }

    public void hide() {
        this.expandedView.setVisibility(View.GONE);
        this.collapsedView.setVisibility(View.VISIBLE);
    }

    public void show() {
        this.collapsedView.setVisibility(View.GONE);
        this.expandedView.setVisibility(View.VISIBLE);
    }

    public ListView getTasksList() {
        return this.tasksList;
    }

}

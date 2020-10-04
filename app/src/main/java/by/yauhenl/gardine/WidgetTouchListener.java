package by.yauhenl.gardine;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import static android.widget.AdapterView.INVALID_POSITION;

class WidgetTouchListener implements View.OnTouchListener {
    private static final int SHOW_X_THRESHOLD = 30;
    private static final int SCROLL_Y_THRESHOLD = 45;
    private final int MAX_Y_DIFF;
    private static final int MIN_Y_DIFF = 0;

    private GardineWidgetService gardineWidgetService;
    private final GardineView view;
    private float initialTouchX;
    private float initialShowY;
    private float initialShowX;
    private State state;
    private int swipeDirection = 1;
    private boolean pointToSelect = false;
    private int[] widgetLocation = new int[2];

    public WidgetTouchListener(GardineWidgetService gardineWidgetService, GardineView view) {
        this.gardineWidgetService = gardineWidgetService;
        this.view = view;
        this.MAX_Y_DIFF = gardineWidgetService.getMaxItems() * SCROLL_Y_THRESHOLD - SCROLL_Y_THRESHOLD / 2;
        this.state = State.WAITING;
    }

    public void setSwipeDirection(WidgetPosition pos) {
        this.swipeDirection = pos.swipeDirection;
    }

    public void setPointToSelect(boolean pointToSelect) {
        this.pointToSelect = pointToSelect;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        ListView listView = this.view.getAppsListView();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(LoggingUtils.COORD_TAG, "DOWN at " + event.getRawX() + ", " + event.getRawY());
                this.initialTouchX = event.getRawX();
                this.state = State.INITIATED;
                return true;
            case MotionEvent.ACTION_UP:
                Log.d(LoggingUtils.COORD_TAG, "UP at " + event.getRawX() + ", " + event.getRawY());


                if (!this.view.isHidden()) {
                    int checkedPos = listView.getCheckedItemPosition();
                    if (checkedPos != INVALID_POSITION) {
                        App selectedApp = (App) listView.getItemAtPosition(checkedPos);
                        if (selectedApp != null) {
                            try {
                                this.gardineWidgetService.startActivity(selectedApp.startIntent);
                            } catch(android.content.ActivityNotFoundException e) {
                                this.gardineWidgetService.removeApp(selectedApp);
                            }
                        }
                    }
                    this.view.hide();
                }
                this.state = State.WAITING;

                return true;
            case MotionEvent.ACTION_MOVE:
                if (this.view.isHidden()) {
                    if ((initialTouchX - event.getRawX()) * this.swipeDirection > SHOW_X_THRESHOLD && state == State.INITIATED) {
                        gardineWidgetService.actualizeRecentApps();

                        Log.d(LoggingUtils.COORD_TAG, "SHOW at " + event.getRawX() + ", " + event.getRawY());

                        this.initialShowX = event.getRawX();
                        this.initialShowY = event.getRawY();

                        this.view.show();
                        this.state = State.OPENED;
                    }
                } else {
                    if ((event.getRawX() - this.initialShowX) * this.swipeDirection > SHOW_X_THRESHOLD && state == State.OPENED) {
                        Log.d(LoggingUtils.COORD_TAG, "HIDE at " + event.getRawX() + ", " + event.getRawY());

                        this.view.hide();
                        this.state = State.CLOSED;
                        return true;
                    }

                    int yd = (int) (event.getRawY() - this.initialShowY);
                    int oyd = yd;
                    yd = Math.min(yd, MAX_Y_DIFF);
                    yd = Math.max(yd, MIN_Y_DIFF);
                    int correction = oyd - yd;
                    this.initialShowY += correction;

                    int prevItem = listView.getCheckedItemPosition();
                    int selectedItem = prevItem;
                    if (pointToSelect) {
                        listView.getLocationOnScreen(this.widgetLocation);
                        float localX = event.getRawX() - this.widgetLocation[0];
                        float localY = event.getRawY() - this.widgetLocation[1];
                        selectedItem = listView.pointToPosition((int) localX, (int) localY);
                        if (selectedItem == INVALID_POSITION) {
                            return true;
                        }
                    } else {
                        int itemsNumber = this.view.getItemsCount();

                        if (itemsNumber > 0) {
                            selectedItem = (yd / SCROLL_Y_THRESHOLD);
                            if (selectedItem < 0) {
                                selectedItem = 0;
                            } else if (selectedItem >= itemsNumber) {
                                selectedItem = itemsNumber - 1;
                            }
                        }
                    }
                    if (prevItem != selectedItem) {
                        Log.d(LoggingUtils.COORD_TAG, "Item changed at Ydiff=" + yd + ", prevItem=" + prevItem + ", curItem=" + selectedItem);
                        gardineWidgetService.applyPreferenceVibrateOnScroll();
                        listView.setItemChecked(selectedItem, true);
                    }
                }

                return true;
        }
        return false;
    }

    private enum State {
        WAITING,
        INITIATED,
        OPENED,
        CLOSED
    }
}

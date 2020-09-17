package by.yauhenl.gardine;

import android.view.View;
import android.view.ViewTreeObserver;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class AntipodeViewsLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {

    private View a, b;
    private int av, bv;

    public AntipodeViewsLayoutListener(View a, View b) {
        this.a = a;
        this.b = b;
        this.setVisibility();
    }

    @Override
    public void onGlobalLayout() {
        boolean aVisibilityChanged = a.getVisibility() != av;
        boolean bVisibilityChanged = b.getVisibility() != bv;

        this.setVisibility();

        if (aVisibilityChanged) {
            this.inverse(a, b);
        }

        if (bVisibilityChanged) {
            this.inverse(b, a);
        }

        // At least one view has to be visible
        if(av == GONE && bv == GONE) {
            this.a.setVisibility(VISIBLE);
        }
    }

    private void setVisibility() {
        this.av = this.a.getVisibility();
        this.bv = this.b.getVisibility();
    }

    private void inverse(View v1, View v2) {
        switch (v1.getVisibility()) {
            case GONE:
                v2.setVisibility(VISIBLE);
                break;
            case VISIBLE:
                v2.setVisibility(GONE);
                break;
            case INVISIBLE:
                break;
        }
    }
}

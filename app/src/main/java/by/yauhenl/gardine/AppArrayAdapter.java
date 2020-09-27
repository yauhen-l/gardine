package by.yauhenl.gardine;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppArrayAdapter extends ArrayAdapter<App> {

    private final int itemResourceId, textViewResourceId, imageViewResourceId;
    private final LayoutInflater layoutInflater;
    private final AtomicBoolean useIcon;
    private final AtomicBoolean viewPreferenceChanged;
    private int layoutGravity = Gravity.START;

    public AppArrayAdapter(@NonNull Context context, @LayoutRes int itemResourceId,
                           @IdRes int textViewResourceId, @IdRes int imageViewResourceId, @NonNull List<App> objects) {
        super(context, itemResourceId, textViewResourceId, objects);
        this.itemResourceId = itemResourceId;
        this.imageViewResourceId = imageViewResourceId;
        this.textViewResourceId = textViewResourceId;
        this.layoutInflater = LayoutInflater.from(context);
        this.useIcon = new AtomicBoolean(false);
        this.viewPreferenceChanged = new AtomicBoolean(false);
    }

    public void setUseIcon(boolean useIcon) {
        boolean oldValue = this.useIcon.getAndSet(useIcon);
        this.viewPreferenceChanged.set(oldValue != useIcon);
    }

    public void setGravity(WidgetPosition pos) {
        this.viewPreferenceChanged.set(true);
        if (pos.swipeDirection > 0) {
            this.layoutGravity = Gravity.START;
        } else {
            this.layoutGravity = Gravity.END;
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (this.viewPreferenceChanged.getAndSet(false)) {
            convertView = null;
        }

        View itemView;
        ImageView imageView;
        TextView textView;
        if (useIcon.get()) {
            itemView = convertView != null ? convertView :
                    layoutInflater.inflate(this.itemResourceId, parent, false);
            imageView = itemView.findViewById(this.imageViewResourceId);

            App app = this.getItem(position);

            imageView.setImageDrawable(app.icon);
            imageView.setVisibility(View.VISIBLE);

            textView = itemView.findViewById(this.textViewResourceId);
            textView.setVisibility(View.GONE);
        } else {
            itemView = super.getView(position, convertView, parent);
            imageView = itemView.findViewById(this.imageViewResourceId);
            imageView.setVisibility(View.GONE);
            textView = itemView.findViewById(this.textViewResourceId);
            textView.setVisibility(View.VISIBLE);
        }

//        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) itemView.getLayoutParams();
//        params.gravity = this.layoutGravity;
//        itemView.setLayoutParams(params);
        //itemView.setLayoutDirection(this.layoutGravity == Gravity.START ? View.LAYOUT_DIRECTION_LTR : View.LAYOUT_DIRECTION_RTL);

        return itemView;
    }
}

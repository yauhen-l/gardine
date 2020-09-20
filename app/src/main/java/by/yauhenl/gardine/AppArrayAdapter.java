package by.yauhenl.gardine;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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

    public AppArrayAdapter(@NonNull Context context, @LayoutRes int resource,
                           @IdRes int textViewResourceId, @IdRes int imageViewResourceId, @NonNull List<App> objects) {
        super(context, resource, textViewResourceId, objects);
        this.itemResourceId = resource;
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

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (this.viewPreferenceChanged.getAndSet(false)) {
            convertView = null;
        }

        if (useIcon.get()) {
            View itemView = convertView != null ? convertView :
                    layoutInflater.inflate(this.itemResourceId, parent, false);
            ImageView imageView = itemView.findViewById(this.imageViewResourceId);

            App app = this.getItem(position);

            imageView.setImageDrawable(app.icon);
            imageView.setVisibility(View.VISIBLE);

            TextView textView = itemView.findViewById(this.textViewResourceId);
            textView.setVisibility(View.GONE);

            return itemView;
        }

        View itemView = super.getView(position, convertView, parent);
        itemView.findViewById(this.imageViewResourceId).setVisibility(View.GONE);
        itemView.findViewById(this.textViewResourceId).setVisibility(View.VISIBLE);
        return itemView;
    }
}

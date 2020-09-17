package by.yauhenl.gardine;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_OVERLAY = 102;
    private static final int PERMISSION_REQUEST_ACCESSIBILITY = 103;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        PreferenceManager.setDefaultValues(this.getApplicationContext(), R.xml.root_preferences, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            initializeWidget();
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, PERMISSION_REQUEST_OVERLAY);
        }

        SettingsFragment settingsFragment = new SettingsFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, settingsFragment)
                .commitNow();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        initializeWidget();
//    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    private void requestAccessibilityService() {
        if (isAccessibilityServiceEnabled(this.getApplicationContext())) {
            return;
        }
        startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), PERMISSION_REQUEST_ACCESSIBILITY);
    }


    public static boolean isAccessibilityServiceEnabled(Context context) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        Log.d(LoggingUtils.ACCESSIBILITY_TAG, "Expect service is enabled for: package=" + context.getPackageName() + ", class=" + GardineWidgetService.class.getName());

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo i = enabledService.getResolveInfo().serviceInfo;
            Log.d(LoggingUtils.ACCESSIBILITY_TAG, "Service is enabled for: package=" + i.packageName + " , class=" + i.name);
            if (i.packageName.equals(context.getPackageName()) &&
                    i.name.equals(GardineWidgetService.class.getName()))
                return true;
        }

        return false;
    }

    private void initializeWidget() {
        Log.i(LoggingUtils.WIDGET, "Starting widget service");
        startService(new Intent(MainActivity.this, GardineWidgetService.class));
        this.requestAccessibilityService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PERMISSION_REQUEST_OVERLAY:
                Log.i(LoggingUtils.WIDGET, "Got result for overlay request: " + resultCode);
                initializeWidget();
                break;
            case PERMISSION_REQUEST_ACCESSIBILITY:
                Log.i(LoggingUtils.ACCESSIBILITY_TAG, "Got result for accessibility request: " + resultCode);
            default:
                //ignore
        }
    }
}

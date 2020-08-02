package by.yauhenl.gardine;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int APP_PERMISSION_REQUEST = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if(!isAccessibilityServiceEnabled(this.getApplicationContext())) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, APP_PERMISSION_REQUEST);
        } else {
            initializeView();
        }

    }

    public static boolean isAccessibilityServiceEnabled(Context context) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        Log.d("accessibility", "Expect service is enabled for: package=" + context.getPackageName() + ", class=" + GardineWidgetService.class.getName());

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo i = enabledService.getResolveInfo().serviceInfo;
            Log.d("accessibility", "Service is enabled for: package=" + i.packageName + " , class=" + i.name);
            if (i.packageName.equals(context.getPackageName()) &&
                    i.name.equals(GardineWidgetService.class.getName()))
                return true;
        }

        return false;
    }

    private void initializeView() {

        Button mButton= (Button) findViewById(R.id.createBtn);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(new Intent(MainActivity.this, GardineWidgetService.class));
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case APP_PERMISSION_REQUEST:
                if(resultCode == RESULT_OK) {
                    initializeView();
                } else {
                    Toast.makeText(this, "Draw over other app permission not enable.", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                //ignore
        }
    }
}

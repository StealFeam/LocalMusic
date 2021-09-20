package pub.devrel.easypermissions.helper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

/**
 * Permissions helper for {@link Activity}.
 */
class ActivityPermissionHelper extends BaseFrameworkPermissionsHelper<Activity> {

    public ActivityPermissionHelper(Activity host) {
        super(host);
    }

    @Override
    @SuppressLint("NewApi")
    public FragmentManager getFragmentManager() {
        return getHost().getFragmentManager();
    }

    @Override
    public void directRequestPermissions(int requestCode, @NonNull String... perms) {
        ActivityCompat.requestPermissions(getHost(), perms, requestCode);
    }

    @Override
    public boolean shouldShowRequestPermissionRationale(@NonNull String perm) {
        return ActivityCompat.shouldShowRequestPermissionRationale(getHost(), perm);
    }

    @Override
    public Context getContext() {
        return getHost();
    }
}

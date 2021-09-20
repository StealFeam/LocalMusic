package pub.devrel.easypermissions;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppSettingsDialogHolderActivity extends AppCompatActivity implements DialogInterface.OnClickListener {
    private AlertDialog mDialog;

    public static Intent createShowDialogIntent(Context context, AppSettingsDialog dialog) {
        return new Intent(context, AppSettingsDialogHolderActivity.class)
                .putExtra(AppSettingsDialog.EXTRA_APP_SETTINGS, dialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSettingsDialog dialog = getIntent().getParcelableExtra(AppSettingsDialog.EXTRA_APP_SETTINGS);
        dialog.setContext(this);
        dialog.setActivityOrFragment(this);
        dialog.setNegativeListener(this);
        mDialog = dialog.showDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        setResult(-1);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode, data);
        finish();
    }
}

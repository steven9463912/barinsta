package awais.instagrabber.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.documentfile.provider.DocumentFile;

import java.util.Random;

import awais.instagrabber.utils.TextUtils;

public class DeleteImageIntentService extends IntentService {
    private static final String TAG = "DeleteImageIntent";
    private static final int DELETE_IMAGE_SERVICE_REQUEST_CODE = 9010;
    private static final Random random = new Random();

    public static final String EXTRA_IMAGE_PATH = "extra_image_path";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
    public static final String DELETE_IMAGE_SERVICE = "delete_image_service";

    public DeleteImageIntentService() {
        super(DeleteImageIntentService.DELETE_IMAGE_SERVICE);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.startService(new Intent(this, DeleteImageIntentService.class));
    }

    @Override
    protected void onHandleIntent(@Nullable final Intent intent) {
        if (intent != null && Intent.ACTION_DELETE.equals(intent.getAction()) && intent.hasExtra(DeleteImageIntentService.EXTRA_IMAGE_PATH)) {
            String path = intent.getStringExtra(DeleteImageIntentService.EXTRA_IMAGE_PATH);
            if (TextUtils.isEmpty(path)) return;
            // final File file = new File(path);
            Uri parse = Uri.parse(path);
            if (parse == null) return;
            DocumentFile file = DocumentFile.fromSingleUri(this.getApplicationContext(), parse);
            final boolean deleted;
            if (file.exists()) {
                deleted = file.delete();
                if (!deleted) {
                    Log.w(DeleteImageIntentService.TAG, "onHandleIntent: file not deleted!");
                }
            } else {
                deleted = true;
            }
            if (deleted) {
                int notificationId = intent.getIntExtra(DeleteImageIntentService.EXTRA_NOTIFICATION_ID, -1);
                NotificationManagerCompat.from(this).cancel(notificationId);
            }
        }
    }

    @NonNull
    public static PendingIntent pendingIntent(@NonNull Context context,
                                              @NonNull DocumentFile imagePath,
                                              int notificationId) {
        Intent intent = new Intent(context, DeleteImageIntentService.class);
        intent.setAction(Intent.ACTION_DELETE);
        intent.putExtra(DeleteImageIntentService.EXTRA_IMAGE_PATH, imagePath.getUri().toString());
        intent.putExtra(DeleteImageIntentService.EXTRA_NOTIFICATION_ID, notificationId);
        return PendingIntent.getService(context, DeleteImageIntentService.random.nextInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}

package awais.instagrabber.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileDescriptor;

public final class MediaUtils {
    private static final String TAG = MediaUtils.class.getSimpleName();

    public static void getVideoInfo(@NonNull ContentResolver contentResolver,
                                    @NonNull Uri uri,
                                    @NonNull OnInfoLoadListener<VideoInfo> listener) {
        MediaUtils.getInfo(contentResolver, uri, listener, true);
    }

    public static void getVoiceInfo(@NonNull ContentResolver contentResolver,
                                    @NonNull Uri uri,
                                    @NonNull OnInfoLoadListener<VideoInfo> listener) {
        MediaUtils.getInfo(contentResolver, uri, listener, false);
    }

    private static void getInfo(@NonNull ContentResolver contentResolver,
                                @NonNull Uri uri,
                                @NonNull OnInfoLoadListener<VideoInfo> listener,
                                @NonNull Boolean isVideo) {
        AppExecutors.INSTANCE.getTasksThread().submit(() -> {
            try (final ParcelFileDescriptor parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")) {
                if (parcelFileDescriptor == null) {
                    listener.onLoad(null);
                    return;
                }
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(fileDescriptor);
                String duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (TextUtils.isEmpty(duration)) duration = "0";
                if (isVideo) {
                    String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    if (TextUtils.isEmpty(width)) width = "1";
                    String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                    if (TextUtils.isEmpty(height)) height = "1";
                    Cursor cursor = contentResolver.query(uri, new String[]{MediaStore.MediaColumns.SIZE}, null, null, null);
                    cursor.moveToFirst();
                    long fileSize = cursor.getLong(0);
                    cursor.close();
                    listener.onLoad(new VideoInfo(
                            Long.parseLong(duration),
                            Integer.parseInt(width),
                            Integer.parseInt(height),
                            fileSize
                    ));
                    return;
                }
                listener.onLoad(new VideoInfo(
                        Long.parseLong(duration),
                        0,
                        0,
                        0
                ));
            } catch (final Exception e) {
                Log.e(MediaUtils.TAG, "getInfo: ", e);
                listener.onFailure(e);
            }
        });
    }

    public static class VideoInfo {
        public long duration;
        public int width;
        public int height;
        public long size;

        public VideoInfo(long duration, int width, int height, long size) {
            this.duration = duration;
            this.width = width;
            this.height = height;
            this.size = size;
        }

    }

    public interface OnInfoLoadListener<T> {
        void onLoad(@Nullable T info);

        void onFailure(Throwable t);
    }
}

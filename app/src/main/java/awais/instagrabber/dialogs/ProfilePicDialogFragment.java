package awais.instagrabber.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;

// import java.io.File;

import awais.instagrabber.R;
import awais.instagrabber.customviews.drawee.AnimatedZoomableController;
import awais.instagrabber.customviews.drawee.DoubleTapGestureListener;
import awais.instagrabber.databinding.DialogProfilepicBinding;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.webservices.UserRepository;
import kotlinx.coroutines.Dispatchers;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class ProfilePicDialogFragment extends DialogFragment {
    private static final String TAG = "ProfilePicDlgFragment";

    private long id;
    private String name;
    private String fallbackUrl;

    private boolean isLoggedIn;
    private DialogProfilepicBinding binding;
    private String url;

    public static ProfilePicDialogFragment getInstance(long id, String name, String fallbackUrl) {
        Bundle args = new Bundle();
        args.putLong("id", id);
        args.putString("name", name);
        args.putString("fallbackUrl", fallbackUrl);
        ProfilePicDialogFragment fragment = new ProfilePicDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public ProfilePicDialogFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        this.binding = DialogProfilepicBinding.inflate(inflater, container, false);
        String cookie = settingsHelper.getString(Constants.COOKIE);
        this.isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) > 0;
        return this.binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = this.getDialog();
        if (dialog == null) return;
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        final int width = ViewGroup.LayoutParams.MATCH_PARENT;
        final int height = ViewGroup.LayoutParams.MATCH_PARENT;
        window.setLayout(width, height);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.init();
        this.fetchAvatar();
    }

    private void init() {
        Bundle arguments = this.getArguments();
        if (arguments == null) {
            this.dismiss();
            return;
        }
        this.id = arguments.getLong("id");
        this.name = arguments.getString("name");
        this.fallbackUrl = arguments.getString("fallbackUrl");
        this.binding.download.setOnClickListener(v -> {
            Context context = this.getContext();
            if (context == null) return;
            // if (ContextCompat.checkSelfPermission(context, DownloadUtils.PERMS[0]) == PackageManager.PERMISSION_GRANTED) {
            this.downloadProfilePicture();
            // return;
            // }
            // requestPermissions(DownloadUtils.PERMS, 8020);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 8020 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            this.downloadProfilePicture();
        }
    }

    private void fetchAvatar() {
        if (this.isLoggedIn) {
            UserRepository repository = UserRepository.Companion.getInstance();
            repository.getUserInfo(this.id, CoroutineUtilsKt.getContinuation((user, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                if (throwable != null) {
                    Context context = this.getContext();
                    if (context == null) {
                        this.dismiss();
                        return;
                    }
                    Toast.makeText(context, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    this.dismiss();
                    return;
                }
                if (user != null) {
                    String url = user.getHDProfilePicUrl();
                    if (TextUtils.isEmpty(url)) {
                        Context context = this.getContext();
                        if (context == null) return;
                        Toast.makeText(context, R.string.no_profile_pic_found, Toast.LENGTH_LONG).show();
                        return;
                    }
                    this.setupPhoto(url);
                }
            }), Dispatchers.getIO()));
        } else this.setupPhoto(this.fallbackUrl);
    }

    private void setupPhoto(String result) {
        if (TextUtils.isEmpty(result)) this.url = this.fallbackUrl;
        else this.url = result;
        DraweeController controller = Fresco
                .newDraweeControllerBuilder()
                .setUri(this.url)
                .setOldController(this.binding.imageViewer.getController())
                .setControllerListener(new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFailure(String id, Throwable throwable) {
                        super.onFailure(id, throwable);
                        ProfilePicDialogFragment.this.binding.download.setVisibility(View.GONE);
                        ProfilePicDialogFragment.this.binding.progressView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFinalImageSet(String id,
                                                ImageInfo imageInfo,
                                                Animatable animatable) {
                        super.onFinalImageSet(id, imageInfo, animatable);
                        ProfilePicDialogFragment.this.binding.download.setVisibility(View.VISIBLE);
                        ProfilePicDialogFragment.this.binding.progressView.setVisibility(View.GONE);
                    }
                })
                .build();
        this.binding.imageViewer.setController(controller);
        AnimatedZoomableController zoomableController = (AnimatedZoomableController) this.binding.imageViewer.getZoomableController();
        zoomableController.setMaxScaleFactor(3f);
        zoomableController.setGestureZoomEnabled(true);
        zoomableController.setEnabled(true);
        this.binding.imageViewer.setZoomingEnabled(true);
        DoubleTapGestureListener tapListener = new DoubleTapGestureListener(this.binding.imageViewer);
        this.binding.imageViewer.setTapListener(tapListener);
    }

    private void downloadProfilePicture() {
        if (this.url == null) return;
        // final File dir = new File(Environment.getExternalStorageDirectory(), "Download");
        Context context = this.getContext();
        if (context == null) return;
        // if (dir.exists() || dir.mkdirs()) {
        //
        // }
        String fileName = this.name + '_' + System.currentTimeMillis() + ".jpg";
        // final File saveFile = new File(dir, fileName);
        DocumentFile downloadDir = DownloadUtils.getDownloadDir();
        DocumentFile saveFile = downloadDir.createFile(Utils.mimeTypeMap.getMimeTypeFromExtension("jpg"), fileName);
        DownloadUtils.download(context, this.url, saveFile);
        // return;
        // Toast.makeText(context, R.string.downloader_error_creating_folder, Toast.LENGTH_SHORT).show();
    }
}

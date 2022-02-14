package awais.instagrabber.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.NotificationsAdapter;
import awais.instagrabber.databinding.FragmentNotificationsViewerBinding;
import awais.instagrabber.models.enums.NotificationType;
import awais.instagrabber.repositories.requests.StoryViewerOptions;
import awais.instagrabber.repositories.responses.notification.Notification;
import awais.instagrabber.repositories.responses.notification.NotificationArgs;
import awais.instagrabber.repositories.responses.notification.NotificationImage;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.NotificationViewModel;
import awais.instagrabber.webservices.FriendshipRepository;
import awais.instagrabber.webservices.MediaRepository;
import awais.instagrabber.webservices.NewsService;
import awais.instagrabber.webservices.ServiceCallback;
import kotlinx.coroutines.Dispatchers;

public final class NotificationsViewerFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "NotificationsViewer";

    private AppCompatActivity fragmentActivity;
    private FragmentNotificationsViewerBinding binding;
    private SwipeRefreshLayout root;
    private boolean shouldRefresh = true;
    private NotificationViewModel notificationViewModel;
    private FriendshipRepository friendshipRepository;
    private MediaRepository mediaRepository;
    private NewsService newsService;
    private String csrfToken, deviceUuid;
    private String type;
    private long targetId;
    private Context context;
    private long userId;

    private final ServiceCallback<List<Notification>> cb = new ServiceCallback<List<Notification>>() {
        @Override
        public void onSuccess(final List<Notification> notificationModels) {
            binding.swipeRefreshLayout.setRefreshing(false);
            notificationViewModel.getList().postValue(notificationModels);
        }

        @Override
        public void onFailure(final Throwable t) {
            try {
                binding.swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {}
        }
    };

    private final NotificationsAdapter.OnNotificationClickListener clickListener = new NotificationsAdapter.OnNotificationClickListener() {
        @Override
        public void onProfileClick(final String username) {
            openProfile(username);
        }

        @Override
        public void onPreviewClick(final Notification model) {
            final NotificationImage notificationImage = model.getArgs().getMedia().get(0);
            final long mediaId = Long.parseLong(notificationImage.getId().split("_")[0]);
            if (model.getType() == NotificationType.RESPONDED_STORY) {
                final StoryViewerOptions options = StoryViewerOptions.forStory(
                        mediaId,
                        model.getArgs().getUsername()
                );
                try {
                    final NavDirections action = NotificationsViewerFragmentDirections.actionToStory(options);
                    NavHostFragment.findNavController(NotificationsViewerFragment.this).navigate(action);
                } catch (Exception e) {
                    Log.e(TAG, "onPreviewClick: ", e);
                }
            } else {
                final AlertDialog alertDialog = new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setView(R.layout.dialog_opening_post)
                        .create();
                alertDialog.show();
                mediaRepository.fetch(
                        mediaId,
                        CoroutineUtilsKt.getContinuation((media, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                            if (throwable != null) {
                                alertDialog.dismiss();
                                Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            try {
                                final NavDirections action = NotificationsViewerFragmentDirections.actionToPost(media, 0);
                                NavHostFragment.findNavController(NotificationsViewerFragment.this).navigate(action);
                            } catch (Exception e) {
                                Log.e(TAG, "onSuccess: ", e);
                            } finally {
                                alertDialog.dismiss();
                            }
                        }), Dispatchers.getIO())
                );
            }
        }

        @Override
        public void onNotificationClick(final Notification model) {
            if (model == null) return;
            final NotificationArgs args = model.getArgs();
            final String username = args.getUsername();
            if (model.getType() == NotificationType.FOLLOW || model.getType() == NotificationType.AYML) {
                openProfile(username);
            } else {
                final SpannableString title = new SpannableString(username + (TextUtils.isEmpty(args.getText()) ? "" : (":\n" + args.getText())));
                title.setSpan(new RelativeSizeSpan(1.23f), 0, username.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                String[] commentDialogList;
                if (model.getType() == NotificationType.RESPONDED_STORY) {
                    commentDialogList = new String[]{
                            getString(R.string.open_profile),
                            getString(R.string.view_story)
                    };
                } else if (args.getMedia() != null) {
                    commentDialogList = new String[]{
                            getString(R.string.open_profile),
                            getString(R.string.view_post)
                    };
                } else if (model.getType() == NotificationType.REQUEST) {
                    commentDialogList = new String[]{
                            getString(R.string.open_profile),
                            getString(R.string.request_approve),
                            getString(R.string.request_reject)
                    };
                } else commentDialogList = null; // shouldn't happen
                final Context context = getContext();
                if (context == null) return;
                final DialogInterface.OnClickListener profileDialogListener = (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openProfile(username);
                            break;
                        case 1:
                            if (model.getType() == NotificationType.REQUEST) {
                                friendshipRepository.approve(
                                        csrfToken,
                                        userId,
                                        deviceUuid,
                                        args.getUserId(),
                                        CoroutineUtilsKt.getContinuation(
                                                (response, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                                    if (throwable != null) {
                                                        Log.e(TAG, "approve: onFailure: ", throwable);
                                                        return;
                                                    }
                                                    onRefresh();
                                                }),
                                                Dispatchers.getIO()
                                        )
                                );
                                return;
                            }
                            clickListener.onPreviewClick(model);
                            break;
                        case 2:
                            friendshipRepository.ignore(
                                    csrfToken,
                                    userId,
                                    deviceUuid,
                                    args.getUserId(),
                                    CoroutineUtilsKt.getContinuation((response, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                        if (throwable != null) {
                                            Log.e(TAG, "approve: onFailure: ", throwable);
                                            return;
                                        }
                                        onRefresh();
                                    }), Dispatchers.getIO())
                            );
                            break;
                    }
                };
                new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setItems(commentDialogList, profileDialogListener)
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        }
    };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (AppCompatActivity) requireActivity();
        context = getContext();
        if (context == null) return;
        NotificationManagerCompat.from(context.getApplicationContext()).cancel(Constants.ACTIVITY_NOTIFICATION_ID);
        final String cookie = Utils.settingsHelper.getString(Constants.COOKIE);
        if (TextUtils.isEmpty(cookie)) {
            Toast.makeText(context, R.string.activity_notloggedin, Toast.LENGTH_SHORT).show();
        }
        userId = CookieUtils.getUserIdFromCookie(cookie);
        deviceUuid = Utils.settingsHelper.getString(Constants.DEVICE_UUID);
        csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        friendshipRepository = FriendshipRepository.Companion.getInstance();
        mediaRepository = MediaRepository.Companion.getInstance();
        newsService = NewsService.getInstance();
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentNotificationsViewerBinding.inflate(getLayoutInflater());
        root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        if (!shouldRefresh) return;
        init();
        shouldRefresh = false;
    }

    private void init() {
        final NotificationsViewerFragmentArgs fragmentArgs = NotificationsViewerFragmentArgs.fromBundle(getArguments());
        type = fragmentArgs.getType();
        targetId = fragmentArgs.getTargetId();
        final Context context = getContext();
        CookieUtils.setupCookies(Utils.settingsHelper.getString(Constants.COOKIE));
        binding.swipeRefreshLayout.setOnRefreshListener(this);
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        final NotificationsAdapter adapter = new NotificationsAdapter(clickListener);
        binding.rvComments.setLayoutManager(new LinearLayoutManager(context));
        binding.rvComments.setAdapter(adapter);
        notificationViewModel.getList().observe(getViewLifecycleOwner(), adapter::submitList);
        onRefresh();
    }

    @Override
    public void onRefresh() {
        binding.swipeRefreshLayout.setRefreshing(true);
        final ActionBar actionBar = fragmentActivity.getSupportActionBar();
        switch (type) {
            case "notif":
                if (actionBar != null) actionBar.setTitle(R.string.action_notif);
                newsService.fetchAppInbox(true, cb);
                break;
            case "ayml":
                if (actionBar != null) actionBar.setTitle(R.string.action_ayml);
                newsService.fetchSuggestions(csrfToken, deviceUuid, cb);
                break;
            case "chaining":
                if (actionBar != null) actionBar.setTitle(R.string.action_ayml);
                newsService.fetchChaining(targetId, cb);
                break;
        }
    }

    private void openProfile(final String username) {
        try {
            final NavDirections action = NotificationsViewerFragmentDirections.actionToProfile().setUsername(username);
            NavHostFragment.findNavController(this).navigate(action);
        } catch (Exception e) {
            Log.e(TAG, "openProfile: ", e);
        }
    }
}
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
        public void onSuccess(List<Notification> notificationModels) {
            NotificationsViewerFragment.this.binding.swipeRefreshLayout.setRefreshing(false);
            NotificationsViewerFragment.this.notificationViewModel.getList().postValue(notificationModels);
        }

        @Override
        public void onFailure(Throwable t) {
            try {
                NotificationsViewerFragment.this.binding.swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(NotificationsViewerFragment.this.getContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (final Throwable ignored) {}
        }
    };

    private final NotificationsAdapter.OnNotificationClickListener clickListener = new NotificationsAdapter.OnNotificationClickListener() {
        @Override
        public void onProfileClick(String username) {
            NotificationsViewerFragment.this.openProfile(username);
        }

        @Override
        public void onPreviewClick(Notification model) {
            NotificationImage notificationImage = model.getArgs().getMedia().get(0);
            long mediaId = Long.parseLong(notificationImage.getId().split("_")[0]);
            if (model.getType() == NotificationType.RESPONDED_STORY) {
                StoryViewerOptions options = StoryViewerOptions.forStory(
                        mediaId,
                        model.getArgs().getUsername()
                );
                try {
                    NavDirections action = NotificationsViewerFragmentDirections.actionToStory(options);
                    NavHostFragment.findNavController(NotificationsViewerFragment.this).navigate(action);
                } catch (final Exception e) {
                    Log.e(NotificationsViewerFragment.TAG, "onPreviewClick: ", e);
                }
            } else {
                AlertDialog alertDialog = new AlertDialog.Builder(NotificationsViewerFragment.this.context)
                        .setCancelable(false)
                        .setView(R.layout.dialog_opening_post)
                        .create();
                alertDialog.show();
                NotificationsViewerFragment.this.mediaRepository.fetch(
                        mediaId,
                        CoroutineUtilsKt.getContinuation((media, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                            if (throwable != null) {
                                alertDialog.dismiss();
                                Toast.makeText(NotificationsViewerFragment.this.context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            try {
                                NavDirections action = NotificationsViewerFragmentDirections.actionToPost(media, 0);
                                NavHostFragment.findNavController(NotificationsViewerFragment.this).navigate(action);
                            } catch (final Exception e) {
                                Log.e(NotificationsViewerFragment.TAG, "onSuccess: ", e);
                            } finally {
                                alertDialog.dismiss();
                            }
                        }), Dispatchers.getIO())
                );
            }
        }

        @Override
        public void onNotificationClick(Notification model) {
            if (model == null) return;
            NotificationArgs args = model.getArgs();
            String username = args.getUsername();
            if (model.getType() == NotificationType.FOLLOW || model.getType() == NotificationType.AYML) {
                NotificationsViewerFragment.this.openProfile(username);
            } else {
                SpannableString title = new SpannableString(username + (TextUtils.isEmpty(args.getText()) ? "" : (":\n" + args.getText())));
                title.setSpan(new RelativeSizeSpan(1.23f), 0, username.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                final String[] commentDialogList;
                if (model.getType() == NotificationType.RESPONDED_STORY) {
                    commentDialogList = new String[]{
                            NotificationsViewerFragment.this.getString(R.string.open_profile),
                            NotificationsViewerFragment.this.getString(R.string.view_story)
                    };
                } else if (args.getMedia() != null) {
                    commentDialogList = new String[]{
                            NotificationsViewerFragment.this.getString(R.string.open_profile),
                            NotificationsViewerFragment.this.getString(R.string.view_post)
                    };
                } else if (model.getType() == NotificationType.REQUEST) {
                    commentDialogList = new String[]{
                            NotificationsViewerFragment.this.getString(R.string.open_profile),
                            NotificationsViewerFragment.this.getString(R.string.request_approve),
                            NotificationsViewerFragment.this.getString(R.string.request_reject)
                    };
                } else commentDialogList = null; // shouldn't happen
                Context context = NotificationsViewerFragment.this.getContext();
                if (context == null) return;
                DialogInterface.OnClickListener profileDialogListener = (dialog, which) -> {
                    switch (which) {
                        case 0:
                            NotificationsViewerFragment.this.openProfile(username);
                            break;
                        case 1:
                            if (model.getType() == NotificationType.REQUEST) {
                                NotificationsViewerFragment.this.friendshipRepository.approve(
                                        NotificationsViewerFragment.this.csrfToken,
                                        NotificationsViewerFragment.this.userId,
                                        NotificationsViewerFragment.this.deviceUuid,
                                        args.getUserId(),
                                        CoroutineUtilsKt.getContinuation(
                                                (response, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                                    if (throwable != null) {
                                                        Log.e(NotificationsViewerFragment.TAG, "approve: onFailure: ", throwable);
                                                        return;
                                                    }
                                                    NotificationsViewerFragment.this.onRefresh();
                                                }),
                                                Dispatchers.getIO()
                                        )
                                );
                                return;
                            }
                            NotificationsViewerFragment.this.clickListener.onPreviewClick(model);
                            break;
                        case 2:
                            NotificationsViewerFragment.this.friendshipRepository.ignore(
                                    NotificationsViewerFragment.this.csrfToken,
                                    NotificationsViewerFragment.this.userId,
                                    NotificationsViewerFragment.this.deviceUuid,
                                    args.getUserId(),
                                    CoroutineUtilsKt.getContinuation((response, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                        if (throwable != null) {
                                            Log.e(NotificationsViewerFragment.TAG, "approve: onFailure: ", throwable);
                                            return;
                                        }
                                        NotificationsViewerFragment.this.onRefresh();
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.fragmentActivity = (AppCompatActivity) this.requireActivity();
        this.context = this.getContext();
        if (this.context == null) return;
        NotificationManagerCompat.from(this.context.getApplicationContext()).cancel(Constants.ACTIVITY_NOTIFICATION_ID);
        String cookie = Utils.settingsHelper.getString(Constants.COOKIE);
        if (TextUtils.isEmpty(cookie)) {
            Toast.makeText(this.context, R.string.activity_notloggedin, Toast.LENGTH_SHORT).show();
        }
        this.userId = CookieUtils.getUserIdFromCookie(cookie);
        this.deviceUuid = Utils.settingsHelper.getString(Constants.DEVICE_UUID);
        this.csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        this.friendshipRepository = FriendshipRepository.Companion.getInstance();
        this.mediaRepository = MediaRepository.Companion.getInstance();
        this.newsService = NewsService.getInstance();
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (this.root != null) {
            this.shouldRefresh = false;
            return this.root;
        }
        this.binding = FragmentNotificationsViewerBinding.inflate(this.getLayoutInflater());
        this.root = this.binding.getRoot();
        return this.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (!this.shouldRefresh) return;
        this.init();
        this.shouldRefresh = false;
    }

    private void init() {
        NotificationsViewerFragmentArgs fragmentArgs = NotificationsViewerFragmentArgs.fromBundle(this.getArguments());
        this.type = fragmentArgs.getType();
        this.targetId = fragmentArgs.getTargetId();
        Context context = this.getContext();
        CookieUtils.setupCookies(Utils.settingsHelper.getString(Constants.COOKIE));
        this.binding.swipeRefreshLayout.setOnRefreshListener(this);
        this.notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        NotificationsAdapter adapter = new NotificationsAdapter(this.clickListener);
        this.binding.rvComments.setLayoutManager(new LinearLayoutManager(context));
        this.binding.rvComments.setAdapter(adapter);
        this.notificationViewModel.getList().observe(this.getViewLifecycleOwner(), adapter::submitList);
        this.onRefresh();
    }

    @Override
    public void onRefresh() {
        this.binding.swipeRefreshLayout.setRefreshing(true);
        ActionBar actionBar = this.fragmentActivity.getSupportActionBar();
        switch (this.type) {
            case "notif":
                if (actionBar != null) actionBar.setTitle(R.string.action_notif);
                this.newsService.fetchAppInbox(true, this.cb);
                break;
            case "ayml":
                if (actionBar != null) actionBar.setTitle(R.string.action_ayml);
                this.newsService.fetchSuggestions(this.csrfToken, this.deviceUuid, this.cb);
                break;
            case "chaining":
                if (actionBar != null) actionBar.setTitle(R.string.action_ayml);
                this.newsService.fetchChaining(this.targetId, this.cb);
                break;
        }
    }

    private void openProfile(String username) {
        try {
            NavDirections action = NotificationsViewerFragmentDirections.actionToProfile().setUsername(username);
            NavHostFragment.findNavController(this).navigate(action);
        } catch (final Exception e) {
            Log.e(NotificationsViewerFragment.TAG, "openProfile: ", e);
        }
    }
}
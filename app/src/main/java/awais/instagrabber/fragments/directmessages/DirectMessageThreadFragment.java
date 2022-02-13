package awais.instagrabber.fragments.directmessages;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsAnimationControlListenerCompat;
import androidx.core.view.WindowInsetsAnimationControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.transition.TransitionManager;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.internal.ToolbarUtils;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import awais.instagrabber.R;
import awais.instagrabber.activities.CameraActivity;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.DirectItemsAdapter;
import awais.instagrabber.adapters.DirectItemsAdapter.DirectItemCallback;
import awais.instagrabber.adapters.DirectItemsAdapter.DirectItemLongClickListener;
import awais.instagrabber.adapters.DirectReactionsAdapter;
import awais.instagrabber.adapters.viewholder.directmessages.DirectItemViewHolder;
import awais.instagrabber.animations.CubicBezierInterpolator;
import awais.instagrabber.customviews.InsetsAnimationLinearLayout;
import awais.instagrabber.customviews.KeyNotifyingEmojiEditText;
import awais.instagrabber.customviews.RecordView;
import awais.instagrabber.customviews.Tooltip;
import awais.instagrabber.customviews.emoji.Emoji;
import awais.instagrabber.customviews.emoji.EmojiBottomSheetDialog;
import awais.instagrabber.customviews.emoji.EmojiPicker;
import awais.instagrabber.customviews.helpers.ControlFocusInsetsAnimationCallback;
import awais.instagrabber.customviews.helpers.EmojiPickerInsetsAnimationCallback;
import awais.instagrabber.customviews.helpers.HeaderItemDecoration;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoaderAtEdge;
import awais.instagrabber.customviews.helpers.SimpleImeAnimationController;
import awais.instagrabber.customviews.helpers.SwipeAndRestoreItemTouchHelperCallback;
import awais.instagrabber.customviews.helpers.TextWatcherAdapter;
import awais.instagrabber.customviews.helpers.TranslateDeferringInsetsAnimationCallback;
import awais.instagrabber.databinding.FragmentDirectMessagesThreadBinding;
import awais.instagrabber.dialogs.DirectItemReactionDialogFragment;
import awais.instagrabber.dialogs.GifPickerBottomDialogFragment;
import awais.instagrabber.fragments.UserSearchMode;
import awais.instagrabber.fragments.settings.PreferenceKeys;
import awais.instagrabber.models.Resource;
import awais.instagrabber.models.enums.DirectItemType;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.requests.StoryViewerOptions;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectItemEmojiReaction;
import awais.instagrabber.repositories.responses.directmessages.DirectItemLink;
import awais.instagrabber.repositories.responses.directmessages.DirectItemReactions;
import awais.instagrabber.repositories.responses.directmessages.DirectItemReelShare;
import awais.instagrabber.repositories.responses.directmessages.DirectItemStoryShare;
import awais.instagrabber.repositories.responses.directmessages.DirectItemVisualMedia;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.DMUtils;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.PermissionUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.AppStateViewModel;
import awais.instagrabber.viewmodels.DirectThreadViewModel;
import awais.instagrabber.viewmodels.factories.DirectThreadViewModelFactory;

public class DirectMessageThreadFragment extends Fragment implements DirectReactionsAdapter.OnReactionClickListener,
        EmojiPicker.OnEmojiClickListener {
    private static final String TAG = DirectMessageThreadFragment.class.getSimpleName();
    private static final int AUDIO_RECORD_PERM_REQUEST_CODE = 1000;
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int FILE_PICKER_REQUEST_CODE = 500;
    private static final String TRANSLATION_Y = "translationY";

    private DirectItemsAdapter itemsAdapter;
    private MainActivity fragmentActivity;
    private DirectThreadViewModel viewModel;
    private InsetsAnimationLinearLayout root;
    private boolean shouldRefresh = true;
    private List<DirectItemsAdapter.DirectItemOrHeader> itemOrHeaders;
    private FragmentDirectMessagesThreadBinding binding;
    private Tooltip tooltip;
    private float initialSendX;
    private ActionBar actionBar;
    private AppStateViewModel appStateViewModel;
    private Runnable prevTitleRunnable;
    private AnimatorSet animatorSet;
    private boolean isRecording;
    private DirectItemReactionDialogFragment reactionDialogFragment;
    private DirectItem itemToForward;
    private MutableLiveData<Object> backStackSavedStateResultLiveData;
    private int prevLength;
    private BadgeDrawable pendingRequestCountBadgeDrawable;
    private boolean isPendingRequestCountBadgeAttached = false;
    private ItemTouchHelper itemTouchHelper;
    private LiveData<Boolean> pendingLiveData;
    private LiveData<DirectThread> threadLiveData;
    private LiveData<Integer> inputModeLiveData;
    private LiveData<String> threadTitleLiveData;
    private LiveData<Resource<Object>> fetchingLiveData;
    private LiveData<List<DirectItem>> itemsLiveData;
    private LiveData<DirectItem> replyToItemLiveData;
    private LiveData<Integer> pendingRequestsCountLiveData;
    private LiveData<List<User>> usersLiveData;
    private boolean autoMarkAsSeen = false;
    private MenuItem markAsSeenMenuItem;
    private DirectItem addReactionItem;
    private TranslateDeferringInsetsAnimationCallback inputHolderAnimationCallback;
    private TranslateDeferringInsetsAnimationCallback chatsAnimationCallback;
    private EmojiPickerInsetsAnimationCallback emojiPickerAnimationCallback;
    private boolean hasKbOpenedOnce;
    private boolean wasToggled;
    private SwipeAndRestoreItemTouchHelperCallback touchHelperCallback;

    private final AppExecutors appExecutors = AppExecutors.INSTANCE;
    private final Animatable2Compat.AnimationCallback micToSendAnimationCallback = new Animatable2Compat.AnimationCallback() {
        @Override
        public void onAnimationEnd(final Drawable drawable) {
            AnimatedVectorDrawableCompat.unregisterAnimationCallback(drawable, this);
            setSendToMicIcon();
        }
    };
    private final Animatable2Compat.AnimationCallback sendToMicAnimationCallback = new Animatable2Compat.AnimationCallback() {
        @Override
        public void onAnimationEnd(final Drawable drawable) {
            AnimatedVectorDrawableCompat.unregisterAnimationCallback(drawable, this);
            setMicToSendIcon();
        }
    };
    private final DirectItemCallback directItemCallback = new DirectItemCallback() {
        @Override
        public void onHashtagClick(final String hashtag) {
            try {
                final NavDirections action = DirectMessageThreadFragmentDirections.actionToHashtag(hashtag);
                NavHostFragment.findNavController(DirectMessageThreadFragment.this).navigate(action);
            } catch (Exception e) {
                Log.e(TAG, "onHashtagClick: ", e);
            }
        }

        @Override
        public void onMentionClick(final String mention) {
            navigateToUser(mention);
        }

        @Override
        public void onLocationClick(final long locationId) {
            try {
                final NavDirections action = DirectMessageThreadFragmentDirections.actionToLocation(locationId);
                NavHostFragment.findNavController(DirectMessageThreadFragment.this).navigate(action);
            } catch (Exception e) {
                Log.e(TAG, "onLocationClick: ", e);
            }
        }

        @Override
        public void onURLClick(final String url) {
            final Context context = getContext();
            if (context == null) return;
            Utils.openURL(context, url);
        }

        @Override
        public void onEmailClick(final String email) {
            final Context context = getContext();
            if (context == null) return;
            Utils.openEmailAddress(context, email);
        }

        @Override
        public void onMediaClick(final Media media, final int index) {
            if (media.isReelMedia()) {
                try {
                    final String pk = media.getPk();
                    if (pk == null) return;
                    final long mediaId = Long.parseLong(pk);
                    final User user = media.getUser();
                    if (user == null) return;
                    final String username = user.getUsername();
                    final NavDirections action = DirectMessageThreadFragmentDirections.actionToStory(StoryViewerOptions.forStory(mediaId, username));
                    NavHostFragment.findNavController(DirectMessageThreadFragment.this).navigate(action);
                } catch (Exception e) {
                    Log.e(TAG, "onMediaClick (story): ", e);
                }
                return;
            }
            try {
                final NavDirections actionToPost = DirectMessageThreadFragmentDirections.actionToPost(media, index);
                NavHostFragment.findNavController(DirectMessageThreadFragment.this).navigate(actionToPost);
            } catch (Exception e) {
                Log.e(TAG, "openPostDialog: ", e);
            }
        }

        @Override
        public void onStoryClick(final DirectItemStoryShare storyShare) {
            try {
                final String pk = storyShare.getReelId();
                if (pk == null) return;
                final long mediaId = Long.parseLong(pk);
                final Media media = storyShare.getMedia();
                if (media == null) return;
                final User user = media.getUser();
                if (user == null) return;
                final String username = user.getUsername();
                final NavDirections action = DirectMessageThreadFragmentDirections.actionToStory(StoryViewerOptions.forUser(mediaId, username));
                NavHostFragment.findNavController(DirectMessageThreadFragment.this).navigate(action);
            } catch (Exception e) {
                Log.e(TAG, "onStoryClick: ", e);
            }
        }

        @Override
        public void onReaction(final DirectItem item, final Emoji emoji) {
            if (item == null || emoji == null) return;
            final LiveData<Resource<Object>> resourceLiveData = viewModel.sendReaction(item, emoji);
            resourceLiveData.observe(getViewLifecycleOwner(), directItemResource -> handleSentMessage(resourceLiveData));
        }

        @Override
        public void onReactionClick(final DirectItem item, final int position) {
            showReactionsDialog(item);
        }

        @Override
        public void onOptionSelect(final DirectItem item, final int itemId, final Function<DirectItem, Void> cb) {
            if (itemId == R.id.unsend) {
                handleSentMessage(viewModel.unsend(item));
                return;
            }
            if (itemId == R.id.forward) {
                itemToForward = item;
                final NavDirections actionGlobalUserSearch = DirectMessageThreadFragmentDirections
                        .actionToUserSearch()
                        .setTitle(getString(R.string.forward))
                        .setActionLabel(getString(R.string.send))
                        .setShowGroups(true)
                        .setMultiple(true)
                        .setSearchMode(UserSearchMode.RAVEN);
                NavHostFragment.findNavController(DirectMessageThreadFragment.this).navigate(actionGlobalUserSearch);
            }
            if (itemId == R.id.download) {
                downloadItem(item);
                return;
            }
            // otherwise call callback if present
            if (cb != null) {
                cb.apply(item);
            }
        }

        @Override
        public void onAddReactionListener(final DirectItem item) {
            if (item == null) return;
            addReactionItem = item;
            final EmojiBottomSheetDialog emojiBottomSheetDialog = EmojiBottomSheetDialog.newInstance();
            emojiBottomSheetDialog.show(getChildFragmentManager(), EmojiBottomSheetDialog.TAG);
        }
    };
    private final DirectItemLongClickListener directItemLongClickListener = position -> {
        // viewModel.setSelectedPosition(position);
    };
    private final Observer<Object> backStackSavedStateObserver = result -> {
        if (result == null) return;
        if (result instanceof Uri) {
            final Uri uri = (Uri) result;
            handleSentMessage(viewModel.sendUri(uri));
        } else if ((result instanceof RankedRecipient)) {
            // Log.d(TAG, "result: " + result);
            if (itemToForward != null) {
                viewModel.forward((RankedRecipient) result, itemToForward);
            }
        } else if ((result instanceof Set)) {
            try {
                // Log.d(TAG, "result: " + result);
                if (itemToForward != null) {
                    //noinspection unchecked
                    viewModel.forward((Set<RankedRecipient>) result, itemToForward);
                }
            } catch (Exception e) {
                Log.e(TAG, "forward result: ", e);
            }
        }
        // clear result
        backStackSavedStateResultLiveData.postValue(null);
    };
    private final MutableLiveData<Integer> inputLength = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> emojiPickerVisible = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> kbVisible = new MutableLiveData<>(false);
    private final OnBackPressedCallback onEmojiPickerBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            emojiPickerVisible.postValue(false);
        }
    };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentActivity = (MainActivity) requireActivity();
        appStateViewModel = new ViewModelProvider(fragmentActivity).get(AppStateViewModel.class);
        autoMarkAsSeen = Utils.settingsHelper.getBoolean(PreferenceKeys.DM_MARK_AS_SEEN);
        final Bundle arguments = getArguments();
        if (arguments == null) return;
        final DirectMessageThreadFragmentArgs fragmentArgs = DirectMessageThreadFragmentArgs.fromBundle(arguments);
        final Resource<User> currentUserResource = appStateViewModel.getCurrentUser();
        if (currentUserResource == null) return;
        final User currentUser = currentUserResource.data;
        if (currentUser == null) return;
        final DirectThreadViewModelFactory viewModelFactory = new DirectThreadViewModelFactory(
                fragmentActivity.getApplication(),
                fragmentArgs.getThreadId(),
                fragmentArgs.getPending(),
                currentUser
        );
        viewModel = new ViewModelProvider(this, viewModelFactory).get(DirectThreadViewModel.class);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        if (root != null) {
            shouldRefresh = false;
            return root;
        }
        binding = FragmentDirectMessagesThreadBinding.inflate(inflater, container, false);
        binding.send.setRecordView(binding.recordView);
        root = binding.getRoot();
        final Context context = getContext();
        if (context == null) {
            return root;
        }
        tooltip = new Tooltip(context, root, getResources().getColor(R.color.grey_400), getResources().getColor(R.color.black));
        // todo check has camera and remove view
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        // WindowCompat.setDecorFitsSystemWindows(fragmentActivity.getWindow(), false);
        if (!shouldRefresh) return;
        init();
        binding.send.post(() -> initialSendX = binding.send.getX());
        shouldRefresh = false;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.dm_thread_menu, menu);
        markAsSeenMenuItem = menu.findItem(R.id.mark_as_seen);
        if (markAsSeenMenuItem != null) {
            if (autoMarkAsSeen) {
                markAsSeenMenuItem.setVisible(false);
            } else {
                markAsSeenMenuItem.setEnabled(false);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.info) {
            final Boolean pending = viewModel.isPending().getValue();
            final NavDirections directions = DirectMessageThreadFragmentDirections
                    .actionToSettings(viewModel.getThreadId(), null)
                    .setPending(pending != null && pending);
            NavHostFragment.findNavController(this).navigate(directions);
            return true;
        }
        if (itemId == R.id.mark_as_seen) {
            handleMarkAsSeen(item);
            return true;
        }
        if (itemId == R.id.refresh && viewModel != null) {
            viewModel.refreshChats();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleMarkAsSeen(@NonNull final MenuItem item) {
        final LiveData<Resource<Object>> resourceLiveData = viewModel.markAsSeen();
        resourceLiveData.observe(getViewLifecycleOwner(), new Observer<Resource<Object>>() {
            @Override
            public void onChanged(final Resource<Object> resource) {
                try {
                    if (resource == null) return;
                    final Context context = getContext();
                    if (context == null) return;
                    switch (resource.status) {
                        case SUCCESS:
                            Toast.makeText(context, R.string.marked_as_seen, Toast.LENGTH_SHORT).show();
                        case LOADING:
                            item.setEnabled(false);
                            break;
                        case ERROR:
                            item.setEnabled(true);
                            if (resource.message != null) {
                                Snackbar.make(context, binding.getRoot(), resource.message, Snackbar.LENGTH_LONG).show();
                                return;
                            }
                            if (resource.resId != 0) {
                                Snackbar.make(binding.getRoot(), resource.resId, Snackbar.LENGTH_LONG).show();
                                return;
                            }
                            break;
                    }
                } finally {
                    resourceLiveData.removeObserver(this);
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data == null || data.getData() == null) {
                Log.w(TAG, "data is null!");
                return;
            }
            final Context context = getContext();
            if (context == null) {
                Log.w(TAG, "conetxt is null!");
                return;
            }
            final Uri uri = data.getData();
            final String mimeType = Utils.getMimeType(uri, context.getContentResolver());
            if (mimeType != null && mimeType.startsWith("image")) {
                navigateToImageEditFragment(uri);
                return;
            }
            handleSentMessage(viewModel.sendUri(uri));
        }
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data == null || data.getData() == null) {
                Log.w(TAG, "data is null!");
                return;
            }
            final Uri uri = data.getData();
            navigateToImageEditFragment(uri);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        final Context context = getContext();
        if (context == null) return;
        if (requestCode == AUDIO_RECORD_PERM_REQUEST_CODE) {
            if (PermissionUtils.hasAudioRecordPerms(context)) {
                Toast.makeText(context, "You can send voice messages now!", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(context, "Require RECORD_AUDIO permission", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPause() {
        if (isRecording) {
            binding.recordView.cancelRecording(binding.send);
        }
        emojiPickerVisible.postValue(false);
        kbVisible.postValue(false);
        binding.inputHolder.setTranslationY(0);
        binding.chats.setTranslationY(0);
        binding.emojiPicker.setTranslationY(0);
        removeObservers();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (initialSendX != 0) {
            binding.send.setX(initialSendX);
        }
        binding.send.stopScale();
        final OnBackPressedDispatcher onBackPressedDispatcher = fragmentActivity.getOnBackPressedDispatcher();
        onBackPressedDispatcher.addCallback(onEmojiPickerBackPressedCallback);
        setupBackStackResultObserver();
        setObservers();
        // attachPendingRequestsBadge(viewModel.getPendingRequestsCount().getValue());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cleanup();
    }

    @Override
    public void onDestroy() {
        viewModel.deleteThreadIfRequired();
        super.onDestroy();
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void cleanup() {
        if (prevTitleRunnable != null) {
            appExecutors.getMainThread().cancel(prevTitleRunnable);
        }
        for (int childCount = binding.chats.getChildCount(), i = 0; i < childCount; ++i) {
            final RecyclerView.ViewHolder holder = binding.chats.getChildViewHolder(binding.chats.getChildAt(i));
            if (holder == null) continue;
            if (holder instanceof DirectItemViewHolder) {
                ((DirectItemViewHolder) holder).cleanup();
            }
        }
        isPendingRequestCountBadgeAttached = false;
        if (pendingRequestCountBadgeDrawable != null) {
            @SuppressLint("RestrictedApi") final ActionMenuItemView menuItemView = ToolbarUtils
                    .getActionMenuItemView(fragmentActivity.getToolbar(), R.id.info);
            if (menuItemView != null) {
                BadgeUtils.detachBadgeDrawable(pendingRequestCountBadgeDrawable, fragmentActivity.getToolbar(), R.id.info);
            }
            pendingRequestCountBadgeDrawable = null;
        }
    }

    private void init() {
        final Context context = getContext();
        if (context == null) return;
        if (getArguments() == null) return;
        actionBar = fragmentActivity.getSupportActionBar();
        setupList();
    }

    private void setupList() {
        final Context context = getContext();
        if (context == null) return;
        binding.chats.setItemViewCacheSize(20);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setReverseLayout(true);
        // layoutManager.setStackFromEnd(false);
        // binding.messageList.addItemDecoration(new VerticalSpaceItemDecoration(3));
        final RecyclerView.ItemAnimator animator = binding.chats.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            final SimpleItemAnimator itemAnimator = (SimpleItemAnimator) animator;
            itemAnimator.setSupportsChangeAnimations(false);
        }
        binding.chats.setLayoutManager(layoutManager);
        binding.chats.addOnScrollListener(new RecyclerLazyLoaderAtEdge(layoutManager, true, page -> viewModel.fetchChats()));
        final HeaderItemDecoration headerItemDecoration = new HeaderItemDecoration(binding.chats, itemPosition -> {
            if (itemOrHeaders == null || itemOrHeaders.isEmpty()) return false;
            try {
                final DirectItemsAdapter.DirectItemOrHeader itemOrHeader = itemOrHeaders.get(itemPosition);
                return itemOrHeader.isHeader();
            } catch (IndexOutOfBoundsException e) {
                return false;
            }
        });
        binding.chats.addItemDecoration(headerItemDecoration);
    }

    private void setObservers() {
        if (viewModel == null) return;
        threadLiveData = viewModel.getThread();
        // if (threadLiveData == null) {
        //     final NavController navController = NavHostFragment.findNavController(this);
        //     navController.navigateUp();
        //     return;
        // }
        pendingLiveData = viewModel.isPending();
        pendingLiveData.observe(getViewLifecycleOwner(), isPending -> {
            if (isPending == null) {
                hideInput();
                return;
            }
            if (isPending) {
                showPendingOptions();
                return;
            }
            hidePendingOptions();
            final Integer inputMode = viewModel.getInputMode().getValue();
            if (inputMode != null && inputMode == 1) return;
            showInput();
        });
        inputModeLiveData = viewModel.getInputMode();
        inputModeLiveData.observe(getViewLifecycleOwner(), inputMode -> {
            final Boolean isPending = viewModel.isPending().getValue();
            if (isPending != null && isPending || inputMode == null) return;
            setupInput(inputMode);
            if (inputMode == 0) {
                setupTouchHelper();
                return;
            }
            if (inputMode == 1) {
                hideInput();
            }
        });
        threadTitleLiveData = viewModel.getThreadTitle();
        threadTitleLiveData.observe(getViewLifecycleOwner(), this::setTitle);
        fetchingLiveData = viewModel.isFetching();
        fetchingLiveData.observe(getViewLifecycleOwner(), fetchingResource -> {
            if (fetchingResource == null) return;
            switch (fetchingResource.status) {
                case SUCCESS:
                case ERROR:
                    setTitle(viewModel.getThreadTitle().getValue());
                    if (fetchingResource.message != null) {
                        Snackbar.make(binding.getRoot(), fetchingResource.message, Snackbar.LENGTH_LONG).show();
                    }
                    if (fetchingResource.resId != 0) {
                        Snackbar.make(binding.getRoot(), fetchingResource.resId, Snackbar.LENGTH_LONG).show();
                    }
                    break;
                case LOADING:
                    setTitle(getString(R.string.dms_thread_updating));
                    break;
            }
        });
        // final ItemsAdapterDataMerger itemsAdapterDataMerger = new ItemsAdapterDataMerger(appStateViewModel.getCurrentUser(), viewModel.getThread());
        // itemsAdapterDataMerger.observe(getViewLifecycleOwner(), userThreadPair -> {
        //     viewModel.setCurrentUser(userThreadPair.first);
        //     setupItemsAdapter(userThreadPair.first, userThreadPair.second);
        // });
        threadLiveData.observe(getViewLifecycleOwner(), this::setupItemsAdapter);
        itemsLiveData = viewModel.getItems();
        itemsLiveData.observe(getViewLifecycleOwner(), this::submitItemsToAdapter);
        replyToItemLiveData = viewModel.getReplyToItem();
        replyToItemLiveData.observe(getViewLifecycleOwner(), item -> {
            if (item == null) {
                if (binding.input.length() == 0) {
                    showExtraInputOption(true);
                }
                binding.getRoot().post(() -> {
                    TransitionManager.beginDelayedTransition(binding.getRoot());
                    binding.replyBg.setVisibility(View.GONE);
                    binding.replyInfo.setVisibility(View.GONE);
                    binding.replyPreviewImage.setVisibility(View.GONE);
                    binding.replyCancel.setVisibility(View.GONE);
                    binding.replyPreviewText.setVisibility(View.GONE);
                });
                return;
            }
            showExtraInputOption(false);
            binding.getRoot().postDelayed(() -> {
                binding.replyBg.setVisibility(View.VISIBLE);
                binding.replyInfo.setVisibility(View.VISIBLE);
                binding.replyPreviewImage.setVisibility(View.VISIBLE);
                binding.replyCancel.setVisibility(View.VISIBLE);
                binding.replyPreviewText.setVisibility(View.VISIBLE);
                if (item.getUserId() == viewModel.getViewerId()) {
                    binding.replyInfo.setText(R.string.replying_to_yourself);
                } else {
                    final User user = viewModel.getUser(item.getUserId());
                    if (user != null) {
                        binding.replyInfo.setText(getString(R.string.replying_to_user, user.getFullName()));
                    } else {
                        binding.replyInfo.setVisibility(View.GONE);
                    }
                }
                final String previewText = getDirectItemPreviewText(item);
                binding.replyPreviewText.setText(TextUtils.isEmpty(previewText) ? getString(R.string.message) : previewText);
                final String previewImageUrl = getDirectItemPreviewImageUrl(item);
                if (TextUtils.isEmpty(previewImageUrl)) {
                    binding.replyPreviewImage.setVisibility(View.GONE);
                } else {
                    binding.replyPreviewImage.setImageURI(previewImageUrl);
                }
                binding.replyCancel.setOnClickListener(v -> viewModel.setReplyToItem(null));
            }, 200);
        });
        inputLength.observe(getViewLifecycleOwner(), length -> {
            if (length == null) return;
            final boolean hasReplyToItem = viewModel.getReplyToItem().getValue() != null;
            if (hasReplyToItem) {
                prevLength = length;
                return;
            }
            if ((prevLength == 0 && length != 0) || (prevLength != 0 && length == 0)) {
                showExtraInputOption(length == 0);
            }
            prevLength = length;
        });
        pendingRequestsCountLiveData = viewModel.getPendingRequestsCount();
        pendingRequestsCountLiveData.observe(getViewLifecycleOwner(), this::attachPendingRequestsBadge);
        usersLiveData = viewModel.getUsers();
        usersLiveData.observe(getViewLifecycleOwner(), users -> {
            if (users == null || users.isEmpty()) return;
            final User user = users.get(0);
            binding.acceptPendingRequestQuestion.setText(getString(R.string.accept_request_from_user, user.getUsername(), user.getFullName()));
        });
    }

    private void setupTouchHelper() {
        final Context context = getContext();
        if (context == null) return;
        touchHelperCallback = new SwipeAndRestoreItemTouchHelperCallback(
                context,
                (adapterPosition, viewHolder) -> {
                    if (itemsAdapter == null) return;
                    final DirectItemsAdapter.DirectItemOrHeader directItemOrHeader = this.itemsAdapter.getList().get(adapterPosition);
                    if (directItemOrHeader.isHeader()) return;
                    this.viewModel.setReplyToItem(directItemOrHeader.item);
                }
        );
        this.itemTouchHelper = new ItemTouchHelper(this.touchHelperCallback);
        this.itemTouchHelper.attachToRecyclerView(this.binding.chats);
    }

    private void removeObservers() {
        this.pendingLiveData.removeObservers(this.getViewLifecycleOwner());
        this.inputModeLiveData.removeObservers(this.getViewLifecycleOwner());
        this.threadTitleLiveData.removeObservers(this.getViewLifecycleOwner());
        this.fetchingLiveData.removeObservers(this.getViewLifecycleOwner());
        this.threadLiveData.removeObservers(this.getViewLifecycleOwner());
        this.itemsLiveData.removeObservers(this.getViewLifecycleOwner());
        this.replyToItemLiveData.removeObservers(this.getViewLifecycleOwner());
        this.inputLength.removeObservers(this.getViewLifecycleOwner());
        this.pendingRequestsCountLiveData.removeObservers(this.getViewLifecycleOwner());
        this.usersLiveData.removeObservers(this.getViewLifecycleOwner());

    }

    private void hidePendingOptions() {
        this.binding.acceptPendingRequestQuestion.setVisibility(View.GONE);
        this.binding.decline.setVisibility(View.GONE);
        this.binding.accept.setVisibility(View.GONE);
    }

    private void showPendingOptions() {
        this.binding.acceptPendingRequestQuestion.setVisibility(View.VISIBLE);
        this.binding.decline.setVisibility(View.VISIBLE);
        this.binding.accept.setVisibility(View.VISIBLE);
        this.binding.accept.setOnClickListener(v -> {
            LiveData<Resource<Object>> resourceLiveData = this.viewModel.acceptRequest();
            this.handlePendingChangeResource(resourceLiveData, false);
        });
        this.binding.decline.setOnClickListener(v -> {
            LiveData<Resource<Object>> resourceLiveData = this.viewModel.declineRequest();
            this.handlePendingChangeResource(resourceLiveData, true);
        });
    }

    private void handlePendingChangeResource(LiveData<Resource<Object>> resourceLiveData, boolean isDecline) {
        resourceLiveData.observe(this.getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            Resource.Status status = resource.status;
            switch (status) {
                case SUCCESS:
                    resourceLiveData.removeObservers(this.getViewLifecycleOwner());
                    if (isDecline) {
                        this.removeObservers();
                        this.viewModel.removeThread();
                        NavController navController = NavHostFragment.findNavController(this);
                        navController.navigateUp();
                        return;
                    }
                    this.removeObservers();
                    this.viewModel.moveFromPending();
                    this.setObservers();
                    break;
                case LOADING:
                    break;
                case ERROR:
                    if (resource.message != null) {
                        Snackbar.make(this.binding.getRoot(), resource.message, BaseTransientBottomBar.LENGTH_LONG).show();
                    }
                    if (resource.resId != 0) {
                        Snackbar.make(this.binding.getRoot(), resource.resId, BaseTransientBottomBar.LENGTH_LONG).show();
                    }
                    resourceLiveData.removeObservers(this.getViewLifecycleOwner());
                    break;
            }
        });
    }

    private void hideInput() {
        this.binding.emojiToggle.setVisibility(View.GONE);
        this.binding.gif.setVisibility(View.GONE);
        this.binding.camera.setVisibility(View.GONE);
        this.binding.gallery.setVisibility(View.GONE);
        this.binding.input.setVisibility(View.GONE);
        this.binding.inputBg.setVisibility(View.GONE);
        this.binding.recordView.setVisibility(View.GONE);
        this.binding.send.setVisibility(View.GONE);
        if (this.itemTouchHelper != null) {
            this.itemTouchHelper.attachToRecyclerView(null);
        }
    }

    private void showInput() {
        this.binding.emojiToggle.setVisibility(View.VISIBLE);
        this.binding.gif.setVisibility(View.VISIBLE);
        this.binding.camera.setVisibility(View.VISIBLE);
        this.binding.gallery.setVisibility(View.VISIBLE);
        this.binding.input.setVisibility(View.VISIBLE);
        this.binding.inputBg.setVisibility(View.VISIBLE);
        this.binding.recordView.setVisibility(View.VISIBLE);
        this.binding.send.setVisibility(View.VISIBLE);
        if (this.itemTouchHelper != null) {
            this.itemTouchHelper.attachToRecyclerView(this.binding.chats);
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void attachPendingRequestsBadge(@Nullable Integer count) {
        if (this.pendingRequestCountBadgeDrawable == null) {
            Context context = this.getContext();
            if (context == null) return;
            this.pendingRequestCountBadgeDrawable = BadgeDrawable.create(context);
        }
        if (count == null || count == 0) {
            @SuppressLint("RestrictedApi") ActionMenuItemView menuItemView = ToolbarUtils
                    .getActionMenuItemView(this.fragmentActivity.getToolbar(), R.id.info);
            if (menuItemView != null) {
                BadgeUtils.detachBadgeDrawable(this.pendingRequestCountBadgeDrawable, this.fragmentActivity.getToolbar(), R.id.info);
            }
            this.isPendingRequestCountBadgeAttached = false;
            this.pendingRequestCountBadgeDrawable.setNumber(0);
            return;
        }
        if (this.pendingRequestCountBadgeDrawable.getNumber() == count) return;
        this.pendingRequestCountBadgeDrawable.setNumber(count);
        if (!this.isPendingRequestCountBadgeAttached) {
            BadgeUtils.attachBadgeDrawable(this.pendingRequestCountBadgeDrawable, this.fragmentActivity.getToolbar(), R.id.info);
            this.isPendingRequestCountBadgeAttached = true;
        }
    }

    private void showExtraInputOption(boolean show) {
        if (show) {
            if (!this.binding.send.isListenForRecord()) {
                this.binding.send.setListenForRecord(true);
                this.startIconAnimation();
            }
            this.binding.gif.setVisibility(View.VISIBLE);
            this.binding.camera.setVisibility(View.VISIBLE);
            this.binding.gallery.setVisibility(View.VISIBLE);
            return;
        }
        if (this.binding.send.isListenForRecord()) {
            this.binding.send.setListenForRecord(false);
            this.startIconAnimation();
        }
        this.binding.gif.setVisibility(View.GONE);
        this.binding.camera.setVisibility(View.GONE);
        this.binding.gallery.setVisibility(View.GONE);
    }

    private String getDirectItemPreviewText(@NonNull DirectItem item) {
        DirectItemType itemType = item.getItemType();
        if (itemType == null) return "";
        switch (itemType) {
            case TEXT:
                return item.getText();
            case LINK:
                DirectItemLink link = item.getLink();
                if (link == null) return "";
                return link.getText();
            case MEDIA: {
                Media media = item.getMedia();
                if (media == null) return "";
                return this.getMediaPreviewTextString(media);
            }
            case RAVEN_MEDIA: {
                DirectItemVisualMedia visualMedia = item.getVisualMedia();
                if (visualMedia == null) return "";
                Media media = visualMedia.getMedia();
                if (media == null) return "";
                return this.getMediaPreviewTextString(media);
            }
            case VOICE_MEDIA:
                return this.getString(R.string.voice_message);
            case MEDIA_SHARE:
                return this.getString(R.string.post);
            case REEL_SHARE:
                DirectItemReelShare reelShare = item.getReelShare();
                if (reelShare == null) return "";
                return reelShare.getText();
        }
        return "";
    }

    @NonNull
    private String getMediaPreviewTextString(@NonNull Media media) {
        MediaItemType mediaType = media.getType();
        if (mediaType == null) return "";
        switch (mediaType) {
            case MEDIA_TYPE_IMAGE:
                return this.getString(R.string.photo);
            case MEDIA_TYPE_VIDEO:
                return this.getString(R.string.video);
            default:
                return "";
        }
    }

    @Nullable
    private String getDirectItemPreviewImageUrl(@NonNull DirectItem item) {
        DirectItemType itemType = item.getItemType();
        if (itemType == null) return null;
        switch (itemType) {
            case TEXT:
            case LINK:
            case VOICE_MEDIA:
            case REEL_SHARE:
                return null;
            case MEDIA: {
                Media media = item.getMedia();
                return ResponseBodyUtils.getThumbUrl(media);
            }
            case RAVEN_MEDIA: {
                DirectItemVisualMedia visualMedia = item.getVisualMedia();
                if (visualMedia == null) return null;
                Media media = visualMedia.getMedia();
                return ResponseBodyUtils.getThumbUrl(media);
            }
            case MEDIA_SHARE: {
                Media media = item.getMediaShare();
                return ResponseBodyUtils.getThumbUrl(media);
            }
        }
        return null;
    }

    private void setupBackStackResultObserver() {
        NavController navController = NavHostFragment.findNavController(this);
        NavBackStackEntry backStackEntry = navController.getCurrentBackStackEntry();
        if (backStackEntry != null) {
            this.backStackSavedStateResultLiveData = backStackEntry.getSavedStateHandle().getLiveData("result");
            this.backStackSavedStateResultLiveData.observe(this.getViewLifecycleOwner(), this.backStackSavedStateObserver);
        }
    }

    private void submitItemsToAdapter(List<DirectItem> items) {
        this.binding.chats.post(() -> {
            if (this.autoMarkAsSeen) {
                this.viewModel.markAsSeen();
                return;
            }
            DirectThread thread = this.threadLiveData.getValue();
            if (thread == null) return;
            if (this.markAsSeenMenuItem != null) {
                this.markAsSeenMenuItem.setEnabled(!DMUtils.isRead(thread));
            }
        });
        if (this.itemsAdapter == null) return;
        this.itemsAdapter.submitList(items, () -> {
            this.itemOrHeaders = this.itemsAdapter.getList();
            this.binding.chats.post(() -> {
                RecyclerView.LayoutManager layoutManager = this.binding.chats.getLayoutManager();
                if (layoutManager instanceof LinearLayoutManager) {
                    int position = ((LinearLayoutManager) layoutManager).findLastCompletelyVisibleItemPosition();
                    if (position < 0) return;
                    if (position == this.itemsAdapter.getItemCount() - 1) {
                        this.viewModel.fetchChats();
                    }
                }
            });
        });
    }

    private void setupItemsAdapter(DirectThread thread) {
        if (thread == null) return;
        if (this.itemsAdapter != null) {
            if (this.itemsAdapter.getThread() == thread) return;
            this.itemsAdapter.setThread(thread);
            return;
        }
        Resource<User> currentUserResource = this.appStateViewModel.getCurrentUser();
        if (currentUserResource == null) return;
        User currentUser = currentUserResource.data;
        if (currentUser == null) return;
        this.itemsAdapter = new DirectItemsAdapter(currentUser, thread, this.directItemCallback, this.directItemLongClickListener);
        this.itemsAdapter.setHasStableIds(true);
        this.itemsAdapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        this.binding.chats.setAdapter(this.itemsAdapter);
        this.registerDataObserver();
        List<DirectItem> items = this.viewModel.getItems().getValue();
        if (items != null && this.itemsAdapter.getItems() != items) {
            this.submitItemsToAdapter(items);
        }
    }

    private void registerDataObserver() {
        this.itemsAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                LinearLayoutManager layoutManager = (LinearLayoutManager) DirectMessageThreadFragment.this.binding.chats.getLayoutManager();
                if (layoutManager == null) return;
                final int firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition();
                if ((firstVisiblePosition == -1 || firstVisiblePosition == 0) && (positionStart == 0)) {
                    DirectMessageThreadFragment.this.binding.chats.scrollToPosition(0);
                }
            }
        });
    }

    private void setupInput(@Nullable Integer inputMode) {
        if (inputMode != null && inputMode == 1) return;
        Context context = this.getContext();
        if (context == null) return;
        this.tooltip.setText(R.string.dms_thread_audio_hint);
        this.setMicToSendIcon();
        this.binding.recordView.setMinMillis(1000);
        this.binding.recordView.setOnRecordListener(new RecordView.OnRecordListener() {
            @Override
            public void onStart() {
                DirectMessageThreadFragment.this.isRecording = true;
                DirectMessageThreadFragment.this.binding.input.setHint(null);
                DirectMessageThreadFragment.this.binding.gif.setVisibility(View.GONE);
                DirectMessageThreadFragment.this.binding.camera.setVisibility(View.GONE);
                DirectMessageThreadFragment.this.binding.gallery.setVisibility(View.GONE);
                if (PermissionUtils.hasAudioRecordPerms(context)) {
                    DirectMessageThreadFragment.this.viewModel.startRecording();
                    return;
                }
                PermissionUtils.requestAudioRecordPerms(DirectMessageThreadFragment.this, DirectMessageThreadFragment.AUDIO_RECORD_PERM_REQUEST_CODE);
            }

            @Override
            public void onCancel() {
                Log.d(DirectMessageThreadFragment.TAG, "onCancel");
                // binding.input.setHint("Message");
                DirectMessageThreadFragment.this.viewModel.stopRecording(true);
                DirectMessageThreadFragment.this.isRecording = false;
            }

            @Override
            public void onFinish(long recordTime) {
                Log.d(DirectMessageThreadFragment.TAG, "onFinish");
                DirectMessageThreadFragment.this.binding.input.setHint("Message");
                DirectMessageThreadFragment.this.binding.gif.setVisibility(View.VISIBLE);
                DirectMessageThreadFragment.this.binding.camera.setVisibility(View.VISIBLE);
                DirectMessageThreadFragment.this.binding.gallery.setVisibility(View.VISIBLE);
                DirectMessageThreadFragment.this.viewModel.stopRecording(false);
                DirectMessageThreadFragment.this.isRecording = false;
            }

            @Override
            public void onLessThanMin() {
                Log.d(DirectMessageThreadFragment.TAG, "onLessThanMin");
                DirectMessageThreadFragment.this.binding.input.setHint("Message");
                if (PermissionUtils.hasAudioRecordPerms(context)) {
                    DirectMessageThreadFragment.this.tooltip.show(DirectMessageThreadFragment.this.binding.send);
                }
                DirectMessageThreadFragment.this.binding.gif.setVisibility(View.VISIBLE);
                DirectMessageThreadFragment.this.binding.camera.setVisibility(View.VISIBLE);
                DirectMessageThreadFragment.this.binding.gallery.setVisibility(View.VISIBLE);
                DirectMessageThreadFragment.this.viewModel.stopRecording(true);
                DirectMessageThreadFragment.this.isRecording = false;
            }
        });
        this.binding.recordView.setOnBasketAnimationEndListener(() -> {
            this.binding.input.setHint(R.string.dms_thread_message_hint);
            this.binding.gif.setVisibility(View.VISIBLE);
            this.binding.camera.setVisibility(View.VISIBLE);
            this.binding.gallery.setVisibility(View.VISIBLE);
        });
        this.binding.input.addTextChangedListener(new TextWatcherAdapter() {
            // int prevLength = 0;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                DirectMessageThreadFragment.this.inputLength.postValue(length);
            }
        });
        this.binding.send.setOnRecordClickListener(v -> {
            Editable text = this.binding.input.getText();
            if (TextUtils.isEmpty(text)) return;
            LiveData<Resource<Object>> resourceLiveData = this.viewModel.sendText(text.toString());
            resourceLiveData.observe(this.getViewLifecycleOwner(), resource -> this.handleSentMessage(resourceLiveData));
            this.binding.input.setText("");
            this.viewModel.setReplyToItem(null);
        });
        this.binding.send.setOnRecordLongClickListener(v -> {
            Log.d(DirectMessageThreadFragment.TAG, "setOnRecordLongClickListener");
            return true;
        });
        this.binding.input.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) return;
            Boolean emojiPickerVisibleValue = this.emojiPickerVisible.getValue();
            if (emojiPickerVisibleValue == null || !emojiPickerVisibleValue) return;
            this.inputHolderAnimationCallback.setShouldTranslate(false);
            this.chatsAnimationCallback.setShouldTranslate(false);
            this.emojiPickerAnimationCallback.setShouldTranslate(false);
        });
        this.setupInsetsCallback();
        this.setupEmojiPicker();
        this.binding.gallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "image/*",
                    "video/mp4"
            });
            this.startActivityForResult(intent, DirectMessageThreadFragment.FILE_PICKER_REQUEST_CODE);
        });
        this.binding.gif.setOnClickListener(v -> {
            GifPickerBottomDialogFragment gifPicker = GifPickerBottomDialogFragment.newInstance();
            gifPicker.setOnSelectListener(giphyGif -> {
                gifPicker.dismiss();
                if (giphyGif == null) return;
                this.handleSentMessage(this.viewModel.sendAnimatedMedia(giphyGif));
            });
            gifPicker.show(this.getChildFragmentManager(), "GifPicker");
        });
        this.binding.camera.setOnClickListener(v -> {
            Intent intent = new Intent(context, CameraActivity.class);
            this.startActivityForResult(intent, DirectMessageThreadFragment.CAMERA_REQUEST_CODE);
        });
    }

    private void setupInsetsCallback() {
        this.inputHolderAnimationCallback = new TranslateDeferringInsetsAnimationCallback(
                this.binding.inputHolder,
                WindowInsetsCompat.Type.systemBars(),
                WindowInsetsCompat.Type.ime(),
                WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE
        );
        ViewCompat.setWindowInsetsAnimationCallback(this.binding.inputHolder, this.inputHolderAnimationCallback);
        this.chatsAnimationCallback = new TranslateDeferringInsetsAnimationCallback(
                this.binding.chats,
                WindowInsetsCompat.Type.systemBars(),
                WindowInsetsCompat.Type.ime()
        );
        ViewCompat.setWindowInsetsAnimationCallback(this.binding.chats, this.chatsAnimationCallback);
        this.emojiPickerAnimationCallback = new EmojiPickerInsetsAnimationCallback(
                this.binding.emojiPicker,
                WindowInsetsCompat.Type.systemBars(),
                WindowInsetsCompat.Type.ime()
        );
        this.emojiPickerAnimationCallback.setKbVisibilityListener(this::onKbVisibilityChange);
        ViewCompat.setWindowInsetsAnimationCallback(this.binding.emojiPicker, this.emojiPickerAnimationCallback);
        ViewCompat.setWindowInsetsAnimationCallback(
                this.binding.input,
                new ControlFocusInsetsAnimationCallback(this.binding.input)
        );
        SimpleImeAnimationController imeAnimController = this.root.getImeAnimController();
        if (imeAnimController != null) {
            imeAnimController.setAnimationControlListener(new WindowInsetsAnimationControlListenerCompat() {
                @Override
                public void onReady(@NonNull WindowInsetsAnimationControllerCompat controller, int types) {}

                @Override
                public void onFinished(@NonNull WindowInsetsAnimationControllerCompat controller) {
                    this.checkKbVisibility();
                }

                @Override
                public void onCancelled(@Nullable WindowInsetsAnimationControllerCompat controller) {
                    this.checkKbVisibility();
                }

                private void checkKbVisibility() {
                    WindowInsetsCompat rootWindowInsets = ViewCompat.getRootWindowInsets(DirectMessageThreadFragment.this.binding.getRoot());
                    boolean visible = rootWindowInsets != null && rootWindowInsets.isVisible(WindowInsetsCompat.Type.ime());
                    DirectMessageThreadFragment.this.onKbVisibilityChange(visible);
                }
            });
        }
    }

    private void onKbVisibilityChange(boolean kbVisible) {
        this.kbVisible.postValue(kbVisible);
        if (this.wasToggled) {
            this.emojiPickerVisible.postValue(!kbVisible);
            this.wasToggled = false;
            return;
        }
        Boolean emojiPickerVisibleValue = this.emojiPickerVisible.getValue();
        if (kbVisible && emojiPickerVisibleValue != null && emojiPickerVisibleValue) {
            this.emojiPickerVisible.postValue(false);
            return;
        }
        if (!kbVisible) {
            this.emojiPickerVisible.postValue(false);
        }
    }

    private void startIconAnimation() {
        Drawable icon = this.binding.send.getIcon();
        if (icon instanceof Animatable) {
            Animatable animatable = (Animatable) icon;
            if (animatable.isRunning()) {
                animatable.stop();
            }
            animatable.start();
        }
    }

    private void navigateToImageEditFragment(String path) {
        this.navigateToImageEditFragment(Uri.fromFile(new File(path)));
    }

    private void navigateToImageEditFragment(Uri uri) {
        try {
            NavDirections navDirections = DirectMessageThreadFragmentDirections.actionToImageEdit(uri);
            NavHostFragment.findNavController(this).navigate(navDirections);
        } catch (final Exception e) {
            Log.e(DirectMessageThreadFragment.TAG, "navigateToImageEditFragment: ", e);
        }
    }

    private void handleSentMessage(LiveData<Resource<Object>> resourceLiveData) {
        Resource<Object> resource = resourceLiveData.getValue();
        if (resource == null) return;
        Resource.Status status = resource.status;
        switch (status) {
            case SUCCESS:
                resourceLiveData.removeObservers(this.getViewLifecycleOwner());
                break;
            case LOADING:
                break;
            case ERROR:
                if (resource.message != null) {
                    Snackbar.make(this.binding.getRoot(), resource.message, BaseTransientBottomBar.LENGTH_LONG).show();
                }
                if (resource.resId != 0) {
                    Snackbar.make(this.binding.getRoot(), resource.resId, BaseTransientBottomBar.LENGTH_LONG).show();
                }
                resourceLiveData.removeObservers(this.getViewLifecycleOwner());
                break;
        }
    }

    private void setupEmojiPicker() {
        this.root.post(() -> this.binding.emojiPicker.init(
                this.root,
                (view, emoji) -> {
                    KeyNotifyingEmojiEditText input = this.binding.input;
                    int start = input.getSelectionStart();
                    int end = input.getSelectionEnd();
                    if (start < 0) {
                        input.append(emoji.getUnicode());
                        return;
                    }
                    input.getText().replace(
                            Math.min(start, end),
                            Math.max(start, end),
                            emoji.getUnicode(),
                            0,
                            emoji.getUnicode().length()
                    );
                },
                () -> this.binding.input.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        ));
        this.binding.emojiToggle.setOnClickListener(v -> {
            Boolean isEmojiPickerVisible = this.emojiPickerVisible.getValue();
            if (isEmojiPickerVisible == null) isEmojiPickerVisible = false;
            Boolean isKbVisible = this.kbVisible.getValue();
            if (isKbVisible == null) isKbVisible = false;
            this.wasToggled = isEmojiPickerVisible || isKbVisible;

            if (isEmojiPickerVisible) {
                if (this.hasKbOpenedOnce && this.binding.emojiPicker.getTranslationY() != 0) {
                    this.inputHolderAnimationCallback.setShouldTranslate(false);
                    this.chatsAnimationCallback.setShouldTranslate(false);
                    this.emojiPickerAnimationCallback.setShouldTranslate(false);
                }
                // trigger ime.
                // Since the kb visibility listener will toggle the emojiPickerVisible live data, we do not explicitly toggle it here
                this.showKeyboard();
                return;
            }

            if (isKbVisible) {
                // hide the keyboard, but don't translate the views
                // Since the kb visibility listener will toggle the emojiPickerVisible live data, we do not explicitly toggle it here
                this.inputHolderAnimationCallback.setShouldTranslate(false);
                this.chatsAnimationCallback.setShouldTranslate(false);
                this.emojiPickerAnimationCallback.setShouldTranslate(false);
                this.hideKeyboard();
            }
            this.emojiPickerVisible.postValue(true);
        });
        LiveData<Pair<Boolean, Boolean>> emojiKbVisibilityLD = Utils.zipLiveData(this.emojiPickerVisible, this.kbVisible);
        emojiKbVisibilityLD.observe(this.getViewLifecycleOwner(), pair -> {
            Boolean isEmojiPickerVisible = pair.first;
            Boolean isKbVisible = pair.second;
            if (isEmojiPickerVisible == null) isEmojiPickerVisible = false;
            if (isKbVisible == null) isKbVisible = false;
            this.root.setScrollImeOffScreenWhenVisible(!isEmojiPickerVisible);
            this.root.setScrollImeOnScreenWhenNotVisible(!isEmojiPickerVisible);
            this.onEmojiPickerBackPressedCallback.setEnabled(isEmojiPickerVisible && !isKbVisible);
            if (isEmojiPickerVisible && !isKbVisible) {
                this.animatePan(this.binding.emojiPicker.getMeasuredHeight(), unused -> {
                    this.binding.emojiPicker.setAlpha(1);
                    this.binding.emojiToggle.setIconResource(R.drawable.ic_keyboard_24);
                    return null;
                }, null);
                return;
            }
            if (!isEmojiPickerVisible && !isKbVisible) {
                this.animatePan(0, null, unused -> {
                    this.binding.emojiPicker.setAlpha(0);
                    this.binding.emojiToggle.setIconResource(R.drawable.ic_face_24);
                    return null;
                });
                return;
            }
            // isKbVisible will always be true going forward
            this.hasKbOpenedOnce = true;
            if (!isEmojiPickerVisible) {
                this.binding.emojiToggle.setIconResource(R.drawable.ic_face_24);
                this.binding.emojiPicker.setAlpha(0);
                return;
            }
            this.binding.emojiPicker.setAlpha(1);
        });
    }

    public void showKeyboard() {
        Context context = this.getContext();
        if (context == null) return;
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        if (!this.binding.input.isFocused()) {
            this.binding.input.requestFocus();
        }
        boolean shown = imm.showSoftInput(this.binding.input, InputMethodManager.SHOW_IMPLICIT);
        if (!shown) {
            Log.e(DirectMessageThreadFragment.TAG, "showKeyboard: System did not display the keyboard");
        }
    }

    public void hideKeyboard() {
        Context context = this.getContext();
        if (context == null) return;
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        imm.hideSoftInputFromWindow(this.binding.input.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
    }

    private void setSendToMicIcon() {
        Context context = this.getContext();
        if (context == null) return;
        Drawable sendToMicDrawable = Utils.getAnimatableDrawable(context, R.drawable.avd_send_to_mic_anim);
        if (sendToMicDrawable instanceof Animatable) {
            AnimatedVectorDrawableCompat.registerAnimationCallback(sendToMicDrawable, this.sendToMicAnimationCallback);
        }
        this.binding.send.setIcon(sendToMicDrawable);
    }

    private void setMicToSendIcon() {
        Context context = this.getContext();
        if (context == null) return;
        Drawable micToSendDrawable = Utils.getAnimatableDrawable(context, R.drawable.avd_mic_to_send_anim);
        if (micToSendDrawable instanceof Animatable) {
            AnimatedVectorDrawableCompat.registerAnimationCallback(micToSendDrawable, this.micToSendAnimationCallback);
        }
        this.binding.send.setIcon(micToSendDrawable);
    }

    private void setTitle(String title) {
        if (this.actionBar == null) return;
        if (this.prevTitleRunnable != null) {
            this.appExecutors.getMainThread().cancel(this.prevTitleRunnable);
        }
        this.prevTitleRunnable = () -> this.actionBar.setTitle(title);
        // set title delayed to avoid title blink if fetch is fast
        this.appExecutors.getMainThread().execute(this.prevTitleRunnable, 1000);
    }

    private void downloadItem(DirectItem item) {
        Context context = this.getContext();
        if (context == null) return;
        DirectItemType itemType = item.getItemType();
        if (itemType == null) return;
        //noinspection SwitchStatementWithTooFewBranches
        switch (itemType) {
            case VOICE_MEDIA:
                this.downloadItem(context, item.getVoiceMedia() == null ? null : item.getVoiceMedia().getMedia());
                break;
            default:
                break;
        }
    }

    // currently ONLY for voice
    private void downloadItem(@NonNull Context context, Media media) {
        if (media == null) {
            Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
            return;
        }
        DownloadUtils.download(context, media);
        Toast.makeText(context, R.string.downloader_downloading_media, Toast.LENGTH_SHORT).show();
    }

    // Sets the translationY of views to height with animation
    private void animatePan(int height,
                            @Nullable Function<Void, Void> onAnimationStart,
                            @Nullable Function<Void, Void> onAnimationEnd) {
        if (this.animatorSet != null && this.animatorSet.isStarted()) {
            this.animatorSet.cancel();
        }
        ImmutableList.Builder<Animator> builder = ImmutableList.builder();
        builder.add(
                ObjectAnimator.ofFloat(this.binding.chats, DirectMessageThreadFragment.TRANSLATION_Y, -height),
                ObjectAnimator.ofFloat(this.binding.inputHolder, DirectMessageThreadFragment.TRANSLATION_Y, -height),
                ObjectAnimator.ofFloat(this.binding.emojiPicker, DirectMessageThreadFragment.TRANSLATION_Y, -height)
        );
        // if (headerItemDecoration != null && headerItemDecoration.getCurrentHeader() != null) {
        //     builder.add(ObjectAnimator.ofFloat(headerItemDecoration.getCurrentHeader(), TRANSLATION_Y, height));
        // }
        this.animatorSet = new AnimatorSet();
        this.animatorSet.playTogether(builder.build());
        this.animatorSet.setDuration(200);
        this.animatorSet.setInterpolator(CubicBezierInterpolator.EASE_IN);
        this.animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (onAnimationStart != null) {
                    onAnimationStart.apply(null);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                DirectMessageThreadFragment.this.animatorSet = null;
                if (onAnimationEnd != null) {
                    onAnimationEnd.apply(null);
                }
            }
        });
        this.animatorSet.start();
    }

    private void showReactionsDialog(DirectItem item) {
        LiveData<List<User>> users = this.viewModel.getUsers();
        LiveData<List<User>> leftUsers = this.viewModel.getLeftUsers();
        ArrayList<User> allUsers = new ArrayList<>();
        allUsers.add(this.viewModel.getCurrentUser());
        if (users.getValue() != null) {
            allUsers.addAll(users.getValue());
        }
        if (leftUsers.getValue() != null) {
            allUsers.addAll(leftUsers.getValue());
        }
        String itemId = item.getItemId();
        if (itemId == null) return;
        DirectItemReactions reactions = item.getReactions();
        if (reactions == null) return;
        this.reactionDialogFragment = DirectItemReactionDialogFragment.newInstance(
                this.viewModel.getViewerId(),
                allUsers,
                itemId,
                reactions
        );
        this.reactionDialogFragment.show(this.getChildFragmentManager(), "reactions_dialog");
    }

    @Override
    public void onReactionClick(String itemId, DirectItemEmojiReaction reaction) {
        if (this.reactionDialogFragment != null) {
            this.reactionDialogFragment.dismiss();
        }
        if (itemId == null || reaction == null) return;
        if (reaction.getSenderId() == this.viewModel.getViewerId()) {
            LiveData<Resource<Object>> resourceLiveData = this.viewModel.sendDeleteReaction(itemId);
            resourceLiveData.observe(this.getViewLifecycleOwner(), directItemResource -> this.handleSentMessage(resourceLiveData));
            return;
        }
        // navigate to user
        User user = this.viewModel.getUser(reaction.getSenderId());
        if (user == null) return;
        this.navigateToUser(user.getUsername());
    }

    private void navigateToUser(@NonNull String username) {
        try {
            NavDirections direction = DirectMessageThreadFragmentDirections.actionToProfile().setUsername(username);
            NavHostFragment.findNavController(this).navigate(direction);
        } catch (final Exception e) {
            Log.e(DirectMessageThreadFragment.TAG, "navigateToUser: ", e);
        }
    }

    @Override
    public void onClick(View view, Emoji emoji) {
        if (this.addReactionItem == null || emoji == null) return;
        LiveData<Resource<Object>> resourceLiveData = this.viewModel.sendReaction(this.addReactionItem, emoji);
        resourceLiveData.observe(this.getViewLifecycleOwner(), directItemResource -> this.handleSentMessage(resourceLiveData));
    }
}

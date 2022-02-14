package awais.instagrabber.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.ArrowPositionRules;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.BalloonAnimation;
import com.skydoves.balloon.BalloonHighlightAnimation;
import com.skydoves.balloon.BalloonSizeSpec;
import com.skydoves.balloon.overlay.BalloonOverlayAnimation;
import com.skydoves.balloon.overlay.BalloonOverlayCircle;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.SliderCallbackAdapter;
import awais.instagrabber.adapters.SliderItemsAdapter;
import awais.instagrabber.adapters.viewholder.SliderVideoViewHolder;
import awais.instagrabber.customviews.VerticalImageSpan;
import awais.instagrabber.customviews.VideoPlayerCallbackAdapter;
import awais.instagrabber.customviews.VideoPlayerViewHelper;
import awais.instagrabber.customviews.drawee.AnimatedZoomableController;
import awais.instagrabber.customviews.drawee.DoubleTapGestureListener;
import awais.instagrabber.customviews.drawee.ZoomableController;
import awais.instagrabber.customviews.drawee.ZoomableDraweeView;
import awais.instagrabber.databinding.DialogPostViewBinding;
import awais.instagrabber.databinding.LayoutPostViewBottomBinding;
import awais.instagrabber.databinding.LayoutVideoPlayerWithThumbnailBinding;
import awais.instagrabber.dialogs.EditTextDialogFragment;
import awais.instagrabber.fragments.settings.PreferenceKeys;
import awais.instagrabber.models.Resource;
import awais.instagrabber.models.enums.MediaItemType;
import awais.instagrabber.repositories.responses.Caption;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.MediaCandidate;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.NullSafePair;
import awais.instagrabber.utils.NumberUtils;
import awais.instagrabber.utils.ResponseBodyUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.PostViewV2ViewModel;

import static awais.instagrabber.fragments.settings.PreferenceKeys.PREF_SHOWN_COUNT_TOOLTIP;

public class PostViewV2Fragment extends Fragment implements EditTextDialogFragment.EditTextDialogFragmentCallback {
    private static final String TAG = "PostViewV2Fragment";
    // private static final int DETAILS_HIDE_DELAY_MILLIS = 2000;
    public static final String ARG_MEDIA = "media";
    public static final String ARG_SLIDER_POSITION = "position";

    private DialogPostViewBinding binding;
    private Context context;
    private boolean detailsVisible = true;
//    private boolean video;
    private VideoPlayerViewHelper videoPlayerViewHelper;
    private SliderItemsAdapter sliderItemsAdapter;
    private int sliderPosition = -1;
    private PostViewV2ViewModel viewModel;
    private PopupMenu optionsPopup;
    private EditTextDialogFragment editTextDialogFragment;
    private boolean wasDeleted;
    private MutableLiveData<Object> backStackSavedStateCollectionLiveData;
    private MutableLiveData<Object> backStackSavedStateResultLiveData;
    private OnDeleteListener onDeleteListener;
    @Nullable
    private ViewPager2 sliderParent;
    private LayoutPostViewBottomBinding bottom;
    private View postView;
    private int originalHeight;
    private boolean isInFullScreenMode;
    private StyledPlayerView playerView;
    private int playerViewOriginalHeight;
    private Drawable originalRootBackground;
    private ColorStateList originalLikeColorStateList;
    private ColorStateList originalSaveColorStateList;
    private WindowInsetsControllerCompat controller;

    private final Observer<Object> backStackSavedStateObserver = result -> {
        if (result == null) return;
        if (result instanceof String) {
            String collection = (String) result;
            this.handleSaveUnsaveResourceLiveData(this.viewModel.toggleSave(collection, this.viewModel.getMedia().getHasViewerSaved()));
        } else if ((result instanceof RankedRecipient)) {
            // Log.d(TAG, "result: " + result);
            Context context = this.getContext();
            if (context != null) {
                Toast.makeText(context, R.string.sending, Toast.LENGTH_SHORT).show();
            }
            this.viewModel.shareDm((RankedRecipient) result, this.sliderPosition);
        } else if ((result instanceof Set)) {
            try {
                // Log.d(TAG, "result: " + result);
                Context context = this.getContext();
                if (context != null) {
                    Toast.makeText(context, R.string.sending, Toast.LENGTH_SHORT).show();
                }
                //noinspection unchecked
                this.viewModel.shareDm((Set<RankedRecipient>) result, this.sliderPosition);
            } catch (Exception e) {
                Log.e(PostViewV2Fragment.TAG, "share: ", e);
            }
        }
        // clear result
        this.backStackSavedStateCollectionLiveData.postValue(null);
        this.backStackSavedStateResultLiveData.postValue(null);
    };

    public void setOnDeleteListener(OnDeleteListener onDeleteListener) {
        if (onDeleteListener == null) return;
        this.onDeleteListener = onDeleteListener;
    }

    public interface OnDeleteListener {
        void onDelete();
    }

    // default constructor for fragment manager
    public PostViewV2Fragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.viewModel = new ViewModelProvider(this).get(PostViewV2ViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        this.binding = DialogPostViewBinding.inflate(inflater, container, false);
        this.bottom = LayoutPostViewBottomBinding.bind(this.binding.getRoot());
        MainActivity activity = (MainActivity) this.getActivity();
        if (activity == null) return null;
        this.controller = new WindowInsetsControllerCompat(activity.getWindow(), activity.getRootView());
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // postponeEnterTransition();
        this.init();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onPause() {
        super.onPause();
        // wasPaused = true;
        if (Utils.settingsHelper.getBoolean(PreferenceKeys.PLAY_IN_BACKGROUND)) return;
        Media media = this.viewModel.getMedia();
        if (media.getType() == null) return;
        switch (media.getType()) {
            case MEDIA_TYPE_VIDEO:
                if (this.videoPlayerViewHelper != null) {
                    this.videoPlayerViewHelper.pause();
                }
                return;
            case MEDIA_TYPE_SLIDER:
                if (this.sliderItemsAdapter != null) {
                    this.pauseSliderPlayer();
                }
            default:
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        NavController navController = NavHostFragment.findNavController(this);
        NavBackStackEntry backStackEntry = navController.getCurrentBackStackEntry();
        if (backStackEntry != null) {
            this.backStackSavedStateCollectionLiveData = backStackEntry.getSavedStateHandle().getLiveData("collection");
            this.backStackSavedStateCollectionLiveData.observe(this.getViewLifecycleOwner(), this.backStackSavedStateObserver);
            this.backStackSavedStateResultLiveData = backStackEntry.getSavedStateHandle().getLiveData("result");
            this.backStackSavedStateResultLiveData.observe(this.getViewLifecycleOwner(), this.backStackSavedStateObserver);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.showSystemUI();
        Media media = this.viewModel.getMedia();
        if (media.getType() == null) return;
        switch (media.getType()) {
            case MEDIA_TYPE_VIDEO:
                if (this.videoPlayerViewHelper != null) {
                    this.videoPlayerViewHelper.releasePlayer();
                }
                return;
            case MEDIA_TYPE_SLIDER:
                if (this.sliderItemsAdapter != null) {
                    this.releaseAllSliderPlayers();
                }
            default:
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            Media media = this.viewModel.getMedia();
            if (media.getType() == MediaItemType.MEDIA_TYPE_SLIDER) {
                outState.putInt(PostViewV2Fragment.ARG_SLIDER_POSITION, this.sliderPosition);
            }
        }
        catch (final Exception _) {}
    }

    @Override
    public void onPrimaryNavigationFragmentChanged(boolean isPrimaryNavigationFragment) {
        if (!isPrimaryNavigationFragment) {
            Media media = this.viewModel.getMedia();
            switch (media.getType()) {
                case MEDIA_TYPE_VIDEO:
                    if (this.videoPlayerViewHelper != null) {
                        this.videoPlayerViewHelper.pause();
                    }
                    return;
                case MEDIA_TYPE_SLIDER:
                    if (this.sliderItemsAdapter != null) {
                        this.pauseSliderPlayer();
                    }
                default:
            }
        }
    }

    private void init() {
        Bundle arguments = this.getArguments();
        if (arguments == null) {
            // dismiss();
            return;
        }
        Serializable feedModelSerializable = arguments.getSerializable(PostViewV2Fragment.ARG_MEDIA);
        if (feedModelSerializable == null) {
            Log.e(PostViewV2Fragment.TAG, "onCreate: feedModelSerializable is null");
            // dismiss();
            return;
        }
        if (!(feedModelSerializable instanceof Media)) {
            // dismiss();
            return;
        }
        Media media = (Media) feedModelSerializable;
        if (media.getType() == MediaItemType.MEDIA_TYPE_SLIDER && this.sliderPosition == -1) {
            this.sliderPosition = arguments.getInt(PostViewV2Fragment.ARG_SLIDER_POSITION, 0);
        }
        this.viewModel.setMedia(media);
        // if (!wasPaused && (sharedProfilePicElement != null || sharedMainPostElement != null)) {
        //     binding.getRoot().getBackground().mutate().setAlpha(0);
        // }
        // setProfilePicSharedElement();
        // setupCaptionBottomSheet();
        this.setupCommonActions();
        this.setObservers();
    }

    private void setObservers() {
        this.viewModel.getUser().observe(this.getViewLifecycleOwner(), user -> {
            if (user == null) {
                this.binding.profilePic.setVisibility(View.GONE);
                this.binding.title.setVisibility(View.GONE);
                this.binding.subtitle.setVisibility(View.GONE);
                return;
            }
            this.binding.profilePic.setVisibility(View.VISIBLE);
            this.binding.title.setVisibility(View.VISIBLE);
            this.binding.subtitle.setVisibility(View.VISIBLE);
            this.binding.getRoot().post(() -> this.setupProfilePic(user));
            this.binding.getRoot().post(() -> this.setupTitles(user));
        });
        this.viewModel.getCaption().observe(this.getViewLifecycleOwner(), caption -> this.binding.getRoot().post(() -> this.setupCaption(caption)));
        this.viewModel.getLocation().observe(this.getViewLifecycleOwner(), location -> this.binding.getRoot().post(() -> this.setupLocation(location)));
        this.viewModel.getDate().observe(this.getViewLifecycleOwner(), date -> this.binding.getRoot().post(() -> {
            if (date == null) {
                this.bottom.date.setVisibility(View.GONE);
                return;
            }
            this.bottom.date.setVisibility(View.VISIBLE);
            this.bottom.date.setText(date);
        }));
        this.viewModel.getLikeCount().observe(this.getViewLifecycleOwner(), count -> {
            this.bottom.likesCount.setNumber(this.getSafeCount(count));
            this.binding.getRoot().postDelayed(() -> this.bottom.likesCount.setAnimateChanges(true), 1000);
            if (count > 1000 && !Utils.settingsHelper.getBoolean(PREF_SHOWN_COUNT_TOOLTIP)) {
                this.binding.getRoot().postDelayed(this::showCountTooltip, 1000);
            }
        });
        if (!this.viewModel.getMedia().getCommentsDisabled()) {
            this.viewModel.getCommentCount().observe(this.getViewLifecycleOwner(), count -> {
                this.bottom.commentsCount.setNumber(this.getSafeCount(count));
                this.binding.getRoot().postDelayed(() -> this.bottom.commentsCount.setAnimateChanges(true), 1000);
            });
        }
        this.viewModel.getViewCount().observe(this.getViewLifecycleOwner(), count -> {
            if (count == null) {
                this.bottom.viewsCount.setVisibility(View.GONE);
                return;
            }
            this.bottom.viewsCount.setVisibility(View.VISIBLE);
            long safeCount = this.getSafeCount(count);
            String viewString = this.getResources().getQuantityString(R.plurals.views_count, (int) safeCount, safeCount);
            this.bottom.viewsCount.setText(viewString);
        });
        this.viewModel.getType().observe(this.getViewLifecycleOwner(), this::setupPostTypeLayout);
        this.viewModel.getLiked().observe(this.getViewLifecycleOwner(), this::setLikedResources);
        this.viewModel.getSaved().observe(this.getViewLifecycleOwner(), this::setSavedResources);
        this.viewModel.getOptions().observe(this.getViewLifecycleOwner(), options -> this.binding.getRoot().post(() -> {
            this.setupOptions(options != null && !options.isEmpty());
            this.createOptionsPopupMenu();
        }));
    }

    private void showCountTooltip() {
        Context context = this.getContext();
        if (context == null) return;
        Rect rect = new Rect();
        this.bottom.likesCount.getGlobalVisibleRect(rect);
        Balloon balloon = new Balloon.Builder(context)
                .setArrowSize(8)
                .setArrowOrientation(ArrowOrientation.TOP)
                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                .setArrowPosition(0.5f)
                .setWidth(BalloonSizeSpec.WRAP)
                .setHeight(BalloonSizeSpec.WRAP)
                .setPadding(4)
                .setTextSize(16)
                .setAlpha(0.9f)
                .setBalloonAnimation(BalloonAnimation.ELASTIC)
                .setBalloonHighlightAnimation(BalloonHighlightAnimation.HEARTBEAT, 0)
                .setIsVisibleOverlay(true)
                .setOverlayColorResource(R.color.black_a50)
                .setOverlayShape(new BalloonOverlayCircle((float) Math.max(
                        this.bottom.likesCount.getMeasuredWidth(),
                        this.bottom.likesCount.getMeasuredHeight()
                ) / 2f))
                .setBalloonOverlayAnimation(BalloonOverlayAnimation.FADE)
                .setLifecycleOwner(this.getViewLifecycleOwner())
                .setTextResource(R.string.click_to_show_full)
                .setDismissWhenTouchOutside(false)
                .setDismissWhenOverlayClicked(false)
                .build();
        balloon.showAlignBottom(this.bottom.likesCount);
        Utils.settingsHelper.putBoolean(PREF_SHOWN_COUNT_TOOLTIP, true);
        balloon.setOnBalloonOutsideTouchListener((view, motionEvent) -> {
            if (rect.contains((int) motionEvent.getRawX(), (int) motionEvent.getRawY())) {
                this.bottom.likesCount.setShowAbbreviation(false);
            }
            balloon.dismiss();
        });
    }

    @NonNull
    private Long getSafeCount(Long count) {
        Long safeCount = count;
        if (count == null) {
            safeCount = 0L;
        }
        return safeCount;
    }

    private void setupCommonActions() {
        this.setupLike();
        this.setupSave();
        this.setupDownload();
        this.setupComment();
        this.setupShare();
    }

    private void setupComment() {
        if (!this.viewModel.hasPk() || this.viewModel.getMedia().getCommentsDisabled()) {
            this.bottom.comment.setVisibility(View.GONE);
            // bottom.commentsCount.setVisibility(View.GONE);
            return;
        }
        this.bottom.comment.setVisibility(View.VISIBLE);
        this.bottom.comment.setOnClickListener(v -> {
            Media media = this.viewModel.getMedia();
            User user = media.getUser();
            if (user == null) return;
            NavController navController = this.getNavController();
            if (navController == null) return;
            try {
                NavDirections action = PostViewV2FragmentDirections.actionToComments(media.getCode(), media.getPk(), user.getPk());
                navController.navigate(action);
            } catch (final Exception e) {
                Log.e(PostViewV2Fragment.TAG, "setupComment: ", e);
            }
        });
        TooltipCompat.setTooltipText(this.bottom.comment, this.getString(R.string.comment));
    }

    private void setupDownload() {
        this.bottom.download.setOnClickListener(v -> DownloadUtils.showDownloadDialog(this.context, this.viewModel.getMedia(), this.sliderPosition, this.bottom.download));
        TooltipCompat.setTooltipText(this.bottom.download, this.getString(R.string.action_download));
    }

    private void setupLike() {
        this.originalLikeColorStateList = this.bottom.like.getIconTint();
        boolean likableMedia = this.viewModel.hasPk() /*&& viewModel.getMedia().isCommentLikesEnabled()*/;
        if (!likableMedia) {
            this.bottom.like.setVisibility(View.GONE);
            // bottom.likesCount.setVisibility(View.GONE);
            return;
        }
        if (!this.viewModel.isLoggedIn()) {
            this.bottom.like.setVisibility(View.GONE);
            return;
        }
        this.bottom.like.setOnClickListener(v -> {
            v.setEnabled(false);
            this.handleLikeUnlikeResourceLiveData(this.viewModel.toggleLike());
        });
        this.bottom.like.setOnLongClickListener(v -> {
            NavController navController = this.getNavController();
            if (navController != null && this.viewModel.isLoggedIn()) {
                try {
                    NavDirections action = PostViewV2FragmentDirections.actionToLikes(this.viewModel.getMedia().getPk(), false);
                    navController.navigate(action);
                } catch (final Exception e) {
                    Log.e(PostViewV2Fragment.TAG, "setupLike: ", e);
                }
                return true;
            }
            return true;
        });
    }

    private void handleLikeUnlikeResourceLiveData(@NonNull LiveData<Resource<Object>> resource) {
        resource.observe(this.getViewLifecycleOwner(), value -> {
            switch (value.status) {
                case SUCCESS:
                    this.bottom.like.setEnabled(true);
                    break;
                case ERROR:
                    this.bottom.like.setEnabled(true);
                    this.unsuccessfulLike();
                    break;
                case LOADING:
                    this.bottom.like.setEnabled(false);
                    break;
            }
        });

    }

    private void unsuccessfulLike() {
        int errorTextResId;
        Media media = this.viewModel.getMedia();
        if (!media.getHasLiked()) {
            Log.e(PostViewV2Fragment.TAG, "like unsuccessful!");
            errorTextResId = R.string.like_unsuccessful;
        } else {
            Log.e(PostViewV2Fragment.TAG, "unlike unsuccessful!");
            errorTextResId = R.string.unlike_unsuccessful;
        }
        Snackbar snackbar = Snackbar.make(this.binding.getRoot(), errorTextResId, BaseTransientBottomBar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.ok, null);
        snackbar.show();
    }

    private void setLikedResources(boolean liked) {
        int iconResource;
        ColorStateList tintColorStateList;
        Context context = this.getContext();
        if (context == null) return;
        Resources resources = context.getResources();
        if (resources == null) return;
        if (liked) {
            iconResource = R.drawable.ic_like;
            tintColorStateList = ColorStateList.valueOf(resources.getColor(R.color.red_600));
        } else {
            iconResource = R.drawable.ic_not_liked;
            tintColorStateList = this.originalLikeColorStateList != null ? this.originalLikeColorStateList
                                                                    : ColorStateList.valueOf(resources.getColor(R.color.white));
        }
        this.bottom.like.setIconResource(iconResource);
        this.bottom.like.setIconTint(tintColorStateList);
    }

    private void setupSave() {
        this.originalSaveColorStateList = this.bottom.save.getIconTint();
        if (!this.viewModel.isLoggedIn() || !this.viewModel.hasPk() || !this.viewModel.getMedia().getCanViewerSave()) {
            this.bottom.save.setVisibility(View.GONE);
            return;
        }
        this.bottom.save.setOnClickListener(v -> {
            this.bottom.save.setEnabled(false);
            this.handleSaveUnsaveResourceLiveData(this.viewModel.toggleSave());
        });
        this.bottom.save.setOnLongClickListener(v -> {
            try {
                NavDirections action = PostViewV2FragmentDirections.actionToSavedCollections().setIsSaving(true);
                NavHostFragment.findNavController(this).navigate(action);
                return true;
            } catch (final Exception e) {
                Log.e(PostViewV2Fragment.TAG, "setupSave: ", e);
            }
            return false;
        });
    }

    private void handleSaveUnsaveResourceLiveData(@NonNull LiveData<Resource<Object>> resource) {
        resource.observe(this.getViewLifecycleOwner(), value -> {
            if (value == null) return;
            switch (value.status) {
                case SUCCESS:
                    this.bottom.save.setEnabled(true);
                    break;
                case ERROR:
                    this.bottom.save.setEnabled(true);
                    this.unsuccessfulSave();
                    break;
                case LOADING:
                    this.bottom.save.setEnabled(false);
                    break;
            }
        });
    }

    private void unsuccessfulSave() {
        int errorTextResId;
        Media media = this.viewModel.getMedia();
        if (!media.getHasViewerSaved()) {
            Log.e(PostViewV2Fragment.TAG, "save unsuccessful!");
            errorTextResId = R.string.save_unsuccessful;
        } else {
            Log.e(PostViewV2Fragment.TAG, "save remove unsuccessful!");
            errorTextResId = R.string.save_remove_unsuccessful;
        }
        Snackbar snackbar = Snackbar.make(this.binding.getRoot(), errorTextResId, BaseTransientBottomBar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.ok, null);
        snackbar.show();
    }

    private void setSavedResources(boolean saved) {
        int iconResource;
        ColorStateList tintColorStateList;
        Context context = this.getContext();
        if (context == null) return;
        Resources resources = context.getResources();
        if (resources == null) return;
        if (saved) {
            iconResource = R.drawable.ic_bookmark;
            tintColorStateList = ColorStateList.valueOf(resources.getColor(R.color.blue_700));
        } else {
            iconResource = R.drawable.ic_round_bookmark_border_24;
            tintColorStateList = this.originalSaveColorStateList != null ? this.originalSaveColorStateList
                                                                    : ColorStateList.valueOf(resources.getColor(R.color.white));
        }
        this.bottom.save.setIconResource(iconResource);
        this.bottom.save.setIconTint(tintColorStateList);
    }

    private void setupProfilePic(User user) {
        if (user == null) {
            this.binding.profilePic.setImageURI((String) null);
            return;
        }
        String uri = user.getProfilePicUrl();
        DraweeController controller = Fresco
                .newDraweeControllerBuilder()
                .setUri(uri)
                .build();
        this.binding.profilePic.setController(controller);
        this.binding.profilePic.setOnClickListener(v -> this.navigateToProfile("@" + user.getUsername()));
    }

    private void setupTitles(User user) {
        if (user == null) {
            this.binding.title.setVisibility(View.GONE);
            this.binding.subtitle.setVisibility(View.GONE);
            return;
        }
        String fullName = user.getFullName();
        if (TextUtils.isEmpty(fullName)) {
            this.binding.subtitle.setVisibility(View.GONE);
        } else {
            this.binding.subtitle.setVisibility(View.VISIBLE);
            this.binding.subtitle.setText(fullName);
        }
        this.setUsername(user);
        this.binding.title.setOnClickListener(v -> this.navigateToProfile("@" + user.getUsername()));
        this.binding.subtitle.setOnClickListener(v -> this.navigateToProfile("@" + user.getUsername()));
    }

    private void setUsername(User user) {
        SpannableStringBuilder sb = new SpannableStringBuilder(user.getUsername());
        int drawableSize = Utils.convertDpToPx(24);
        if (user.isVerified()) {
            Context context = this.getContext();
            if (context == null) return;
            Drawable verifiedDrawable = AppCompatResources.getDrawable(context, R.drawable.verified);
            VerticalImageSpan verifiedSpan = null;
            if (verifiedDrawable != null) {
                Drawable drawable = verifiedDrawable.mutate();
                drawable.setBounds(0, 0, drawableSize, drawableSize);
                verifiedSpan = new VerticalImageSpan(drawable);
            }
            try {
                if (verifiedSpan != null) {
                    sb.append("  ");
                    sb.setSpan(verifiedSpan, sb.length() - 1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } catch (final Exception e) {
                Log.e(PostViewV2Fragment.TAG, "setUsername: ", e);
            }
        }
        this.binding.title.setText(sb);
    }

    private void setupCaption(Caption caption) {
        if (caption == null || TextUtils.isEmpty(caption.getText())) {
            this.bottom.caption.setVisibility(View.GONE);
            this.bottom.translate.setVisibility(View.GONE);
            return;
        }
        String postCaption = caption.getText();
        this.bottom.caption.addOnHashtagListener(autoLinkItem -> {
            try {
                String originalText = autoLinkItem.getOriginalText().trim();
                NavDirections action = PostViewV2FragmentDirections.actionToHashtag(originalText);
                NavHostFragment.findNavController(this).navigate(action);
            } catch (final Exception e) {
                Log.e(PostViewV2Fragment.TAG, "setupCaption: ", e);
            }
        });
        this.bottom.caption.addOnMentionClickListener(autoLinkItem -> {
            String originalText = autoLinkItem.getOriginalText().trim();
            this.navigateToProfile(originalText);
        });
        this.bottom.caption.addOnEmailClickListener(autoLinkItem -> Utils.openEmailAddress(this.getContext(), autoLinkItem.getOriginalText().trim()));
        this.bottom.caption.addOnURLClickListener(autoLinkItem -> Utils.openURL(this.getContext(), autoLinkItem.getOriginalText().trim()));
        this.bottom.caption.setOnLongClickListener(v -> {
            Context context = this.getContext();
            if (context == null) return false;
            Utils.copyText(context, postCaption);
            return true;
        });
        this.bottom.caption.setText(postCaption);
        this.bottom.translate.setOnClickListener(v -> this.handleTranslateCaptionResource(this.viewModel.translateCaption()));
    }

    private void handleTranslateCaptionResource(@NonNull LiveData<Resource<String>> data) {
        data.observe(this.getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            switch (resource.status) {
                case SUCCESS:
                    this.bottom.translate.setVisibility(View.GONE);
                    this.bottom.caption.setText(resource.data);
                    break;
                case ERROR:
                    this.bottom.translate.setEnabled(true);
                    String message = resource.message;
                    if (TextUtils.isEmpty(message)) {
                        message = this.getString(R.string.downloader_unknown_error);
                    }
                    Snackbar snackbar = Snackbar.make(this.binding.getRoot(), message, BaseTransientBottomBar.LENGTH_INDEFINITE);
                    snackbar.setAction(R.string.ok, null);
                    snackbar.show();
                    break;
                case LOADING:
                    this.bottom.translate.setEnabled(false);
                    break;
            }
        });
    }

    private void setupLocation(Location location) {
        if (location == null || !this.detailsVisible) {
            this.binding.location.setVisibility(View.GONE);
            return;
        }
        String locationName = location.getName();
        if (TextUtils.isEmpty(locationName)) return;
        this.binding.location.setText(locationName);
        this.binding.location.setVisibility(View.VISIBLE);
        this.binding.location.setOnClickListener(v -> {
            try {
                NavController navController = this.getNavController();
                if (navController == null) return;
                NavDirections action = PostViewV2FragmentDirections.actionToLocation(location.getPk());
                navController.navigate(action);
            } catch (final Exception e) {
                Log.e(PostViewV2Fragment.TAG, "setupLocation: ", e);
            }
        });
    }

    private void setupShare() {
        if (!this.viewModel.hasPk()) {
            this.bottom.share.setVisibility(View.GONE);
            return;
        }
        this.bottom.share.setVisibility(View.VISIBLE);
        TooltipCompat.setTooltipText(this.bottom.share, this.getString(R.string.share));
        this.bottom.share.setOnClickListener(v -> {
            Media media = this.viewModel.getMedia();
            User profileModel = media.getUser();
            if (profileModel == null) return;
            if (this.viewModel.isLoggedIn()) {
                Context context = this.getContext();
                if (context == null) return;
                ContextThemeWrapper themeWrapper = new ContextThemeWrapper(context, R.style.popupMenuStyle);
                PopupMenu popupMenu = new PopupMenu(themeWrapper, this.bottom.share);
                Menu menu = popupMenu.getMenu();
                menu.add(0, R.id.share_dm, 0, R.string.share_via_dm);
                menu.add(0, R.id.share, 1, R.string.share_link);
                popupMenu.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.share_dm) {
                        if (profileModel.isPrivate()) Toast.makeText(context, R.string.share_private_post, Toast.LENGTH_SHORT).show();
                        PostViewV2FragmentDirections.ActionToUserSearch actionGlobalUserSearch = PostViewV2FragmentDirections
                                .actionToUserSearch()
                                .setTitle(this.getString(R.string.share))
                                .setActionLabel(this.getString(R.string.send))
                                .setShowGroups(true)
                                .setMultiple(true)
                                .setSearchMode(UserSearchMode.RAVEN);
                        NavController navController = NavHostFragment.findNavController(this);
                        try {
                            navController.navigate(actionGlobalUserSearch);
                        } catch (final Exception e) {
                            Log.e(PostViewV2Fragment.TAG, "setupShare: ", e);
                        }
                        return true;
                    } else if (itemId == R.id.share) {
                        this.shareLink(media, profileModel.isPrivate());
                        return true;
                    }
                    return false;
                });
                popupMenu.show();
                return;
            }
            this.shareLink(media, false);
        });
    }

    private void shareLink(@NonNull Media media, boolean isPrivate) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TITLE,
                this.getString(isPrivate ? R.string.share_private_post : R.string.share_public_post));
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "https://instagram.com/p/" + media.getCode());
        this.startActivity(Intent.createChooser(
                sharingIntent,
                isPrivate ? this.getString(R.string.share_private_post)
                          : this.getString(R.string.share_public_post)
        ));
    }

    private void setupPostTypeLayout(MediaItemType type) {
        if (type == null) return;
        switch (type) {
            case MEDIA_TYPE_IMAGE:
                this.setupPostImage();
                break;
            case MEDIA_TYPE_SLIDER:
                this.setupSlider();
                break;
            case MEDIA_TYPE_VIDEO:
                this.setupVideo();
                break;
        }
    }

    private void setupPostImage() {
        // binding.mediaCounter.setVisibility(View.GONE);
        Context context = this.getContext();
        if (context == null) return;
        Resources resources = context.getResources();
        if (resources == null) return;
        Media media = this.viewModel.getMedia();
        String imageUrl = ResponseBodyUtils.getImageUrl(media);
        if (TextUtils.isEmpty(imageUrl)) return;
        ZoomableDraweeView postImage = new ZoomableDraweeView(context);
        this.postView = postImage;
        NullSafePair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(media.getOriginalHeight(),
                                                                                            media.getOriginalWidth(),
                                                                                            (int) (Utils.displayMetrics.heightPixels * 0.8),
                                                                                            Utils.displayMetrics.widthPixels);
        this.originalHeight = widthHeight.second;
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                this.originalHeight);
        postImage.setLayoutParams(layoutParams);
        postImage.setHierarchy(new GenericDraweeHierarchyBuilder(resources)
                                       .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                                       .build());

        postImage.setController(Fresco.newDraweeControllerBuilder()
                                      .setLowResImageRequest(ImageRequest.fromUri(ResponseBodyUtils.getThumbUrl(media)))
                                      .setImageRequest(ImageRequestBuilder.newBuilderWithSource(Uri.parse(imageUrl))
                                                                          .setLocalThumbnailPreviewsEnabled(true)
                                                                          .build())
                                      .build());
        AnimatedZoomableController zoomableController = (AnimatedZoomableController) postImage.getZoomableController();
        zoomableController.setMaxScaleFactor(3f);
        zoomableController.setGestureZoomEnabled(true);
        zoomableController.setEnabled(true);
        postImage.setZoomingEnabled(true);
        DoubleTapGestureListener tapListener = new DoubleTapGestureListener(postImage) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (!PostViewV2Fragment.this.isInFullScreenMode) {
                    zoomableController.reset();
                    PostViewV2Fragment.this.hideSystemUI();
                } else {
                    PostViewV2Fragment.this.showSystemUI();
                    PostViewV2Fragment.this.binding.getRoot().postDelayed(zoomableController::reset, 500);
                }
                return super.onSingleTapConfirmed(e);
            }
        };
        postImage.setTapListener(tapListener);
        this.binding.postContainer.addView(this.postView);
    }

    private void setupSlider() {
        Media media = this.viewModel.getMedia();
        this.binding.mediaCounter.setVisibility(View.VISIBLE);
        Context context = this.getContext();
        if (context == null) return;
        this.sliderParent = new ViewPager2(context);
        List<Media> carouselMedia = media.getCarouselMedia();
        if (carouselMedia == null) return;
        NullSafePair<Integer, Integer> maxHW = carouselMedia
                .stream()
                .reduce(new NullSafePair<>(0, 0),
                        (prev, m) -> {
                            int height = m.getOriginalHeight() > prev.first ? m.getOriginalHeight() : prev.first;
                            int width = m.getOriginalWidth() > prev.second ? m.getOriginalWidth() : prev.second;
                            return new NullSafePair<>(height, width);
                        },
                        (p1, p2) -> {
                            int height = p1.first > p2.first ? p1.first : p2.first;
                            int width = p1.second > p2.second ? p1.second : p2.second;
                            return new NullSafePair<>(height, width);
                        });
        NullSafePair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(maxHW.first,
                                                                                            maxHW.second,
                                                                                            (int) (Utils.displayMetrics.heightPixels * 0.8),
                                                                                            Utils.displayMetrics.widthPixels);
        this.originalHeight = widthHeight.second;
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                this.originalHeight);
        this.sliderParent.setLayoutParams(layoutParams);
        this.postView = this.sliderParent;
        // binding.contentRoot.addView(sliderParent, 0);
        this.binding.postContainer.addView(this.postView);

        boolean hasVideo = media.getCarouselMedia()
                                      .stream()
                                      .anyMatch(postChild -> postChild.getType() == MediaItemType.MEDIA_TYPE_VIDEO);
        if (hasVideo) {
            View child = this.sliderParent.getChildAt(0);
            if (child instanceof RecyclerView) {
                ((RecyclerView) child).setItemViewCacheSize(media.getCarouselMedia().size());
                ((RecyclerView) child).addRecyclerListener(holder -> {
                    if (holder instanceof SliderVideoViewHolder) {
                        ((SliderVideoViewHolder) holder).releasePlayer();
                    }
                });
            }
        }
        this.sliderItemsAdapter = new SliderItemsAdapter(true, new SliderCallbackAdapter() {
            @Override
            public void onItemClicked(int position, Media media, View view) {
                if (media == null
                        || media.getType() != MediaItemType.MEDIA_TYPE_IMAGE
                        || !(view instanceof ZoomableDraweeView)) {
                    return;
                }
                ZoomableController zoomableController = ((ZoomableDraweeView) view).getZoomableController();
                if (!(zoomableController instanceof AnimatedZoomableController)) return;
                if (!PostViewV2Fragment.this.isInFullScreenMode) {
                    ((AnimatedZoomableController) zoomableController).reset();
                    PostViewV2Fragment.this.hideSystemUI();
                    return;
                }
                PostViewV2Fragment.this.showSystemUI();
                PostViewV2Fragment.this.binding.getRoot().postDelayed(((AnimatedZoomableController) zoomableController)::reset, 500);
            }

            @Override
            public void onPlayerPlay(int position) {
                FragmentActivity activity = PostViewV2Fragment.this.getActivity();
                if (activity == null) return;
                Utils.enabledKeepScreenOn(activity);
                // if (!detailsVisible || hasBeenToggled) return;
                // showPlayerControls();
            }

            @Override
            public void onPlayerPause(int position) {
                FragmentActivity activity = PostViewV2Fragment.this.getActivity();
                if (activity == null) return;
                Utils.disableKeepScreenOn(activity);
                // if (detailsVisible || hasBeenToggled) return;
                // toggleDetails();
            }

            @Override
            public void onPlayerRelease(int position) {
                FragmentActivity activity = PostViewV2Fragment.this.getActivity();
                if (activity == null) return;
                Utils.disableKeepScreenOn(activity);
            }

            @Override
            public void onFullScreenModeChanged(boolean isFullScreen, StyledPlayerView playerView) {
                PostViewV2Fragment.this.playerView = playerView;
                if (isFullScreen) {
                    PostViewV2Fragment.this.hideSystemUI();
                    return;
                }
                PostViewV2Fragment.this.showSystemUI();
            }

            @Override
            public boolean isInFullScreen() {
                return PostViewV2Fragment.this.isInFullScreenMode;
            }
        });
        this.sliderParent.setAdapter(this.sliderItemsAdapter);
        if (this.sliderPosition >= 0 && this.sliderPosition < media.getCarouselMedia().size()) {
            this.sliderParent.setCurrentItem(this.sliderPosition);
        }
        this.sliderParent.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            int prevPosition = -1;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (this.prevPosition != -1) {
                    View view = PostViewV2Fragment.this.sliderParent.getChildAt(0);
                    if (view instanceof RecyclerView) {
                        this.pausePlayerAtPosition(this.prevPosition, (RecyclerView) view);
                        this.pausePlayerAtPosition(position, (RecyclerView) view);
                    }
                }
                if (positionOffset == 0) {
                    this.prevPosition = position;
                }
            }

            @Override
            public void onPageSelected(int position) {
                int size = media.getCarouselMedia().size();
                if (position < 0 || position >= size) return;
                PostViewV2Fragment.this.sliderPosition = position;
                String text = (position + 1) + "/" + size;
                PostViewV2Fragment.this.binding.mediaCounter.setText(text);
                Media childMedia = media.getCarouselMedia().get(position);
//                video = false;
//                if (childMedia.getType() == MediaItemType.MEDIA_TYPE_VIDEO) {
//                    video = true;
//                    viewModel.setViewCount(childMedia.getViewCount());
//                    return;
//                }
//                viewModel.setViewCount(null);
            }

            private void pausePlayerAtPosition(int position, RecyclerView view) {
                RecyclerView.ViewHolder viewHolder = view.findViewHolderForAdapterPosition(position);
                if (viewHolder instanceof SliderVideoViewHolder) {
                    ((SliderVideoViewHolder) viewHolder).pause();
                }
            }
        });
        String text = "1/" + carouselMedia.size();
        this.binding.mediaCounter.setText(text);
        this.sliderItemsAdapter.submitList(media.getCarouselMedia());
        this.sliderParent.setCurrentItem(this.sliderPosition);
    }

    private void pauseSliderPlayer() {
        if (this.sliderParent == null) return;
        int currentItem = this.sliderParent.getCurrentItem();
        View view = this.sliderParent.getChildAt(0);
        if (!(view instanceof RecyclerView)) return;
        RecyclerView.ViewHolder viewHolder = ((RecyclerView) view).findViewHolderForAdapterPosition(currentItem);
        if (!(viewHolder instanceof SliderVideoViewHolder)) return;
        ((SliderVideoViewHolder) viewHolder).pause();
    }

    private void releaseAllSliderPlayers() {
        if (this.sliderParent == null) return;
        View view = this.sliderParent.getChildAt(0);
        if (!(view instanceof RecyclerView)) return;
        int itemCount = this.sliderItemsAdapter.getItemCount();
        for (int position = itemCount - 1; position >= 0; position--) {
            RecyclerView.ViewHolder viewHolder = ((RecyclerView) view).findViewHolderForAdapterPosition(position);
            if (!(viewHolder instanceof SliderVideoViewHolder)) continue;
            ((SliderVideoViewHolder) viewHolder).releasePlayer();
        }
    }

    private void setupVideo() {
//        video = true;
        Media media = this.viewModel.getMedia();
        this.binding.mediaCounter.setVisibility(View.GONE);
        Context context = this.getContext();
        if (context == null) return;
        LayoutVideoPlayerWithThumbnailBinding videoPost = LayoutVideoPlayerWithThumbnailBinding
                .inflate(LayoutInflater.from(context), this.binding.contentRoot, false);
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) videoPost.getRoot().getLayoutParams();
        NullSafePair<Integer, Integer> widthHeight = NumberUtils.calculateWidthHeight(media.getOriginalHeight(),
                                                                                            media.getOriginalWidth(),
                                                                                            (int) (Utils.displayMetrics.heightPixels * 0.8),
                                                                                            Utils.displayMetrics.widthPixels);
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        this.originalHeight = widthHeight.second;
        layoutParams.height = this.originalHeight;
        this.postView = videoPost.getRoot();
        this.binding.postContainer.addView(this.postView);

        // final GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
        //     @Override
        //     public boolean onSingleTapConfirmed(final MotionEvent e) {
        //         videoPost.playerView.performClick();
        //         return true;
        //     }
        // });
        // videoPost.playerView.setOnTouchListener((v, event) -> {
        //     gestureDetector.onTouchEvent(event);
        //     return true;
        // });
        float vol = Utils.settingsHelper.getBoolean(PreferenceKeys.MUTED_VIDEOS) ? 0f : 1f;
        VideoPlayerViewHelper.VideoPlayerCallback videoPlayerCallback = new VideoPlayerCallbackAdapter() {
            @Override
            public void onThumbnailLoaded() {
                PostViewV2Fragment.this.startPostponedEnterTransition();
            }

            @Override
            public void onPlayerViewLoaded() {
                // binding.playerControls.getRoot().setVisibility(View.VISIBLE);
                ViewGroup.LayoutParams layoutParams = videoPost.playerView.getLayoutParams();
                int requiredWidth = Utils.displayMetrics.widthPixels;
                int resultingHeight = NumberUtils
                        .getResultingHeight(requiredWidth, media.getOriginalHeight(), media.getOriginalWidth());
                layoutParams.width = requiredWidth;
                layoutParams.height = resultingHeight;
                videoPost.playerView.requestLayout();
            }

            @Override
            public void onPlay() {
                FragmentActivity activity = PostViewV2Fragment.this.getActivity();
                if (activity == null) return;
                Utils.enabledKeepScreenOn(activity);
                // if (detailsVisible) {
                //     new Handler().postDelayed(() -> toggleDetails(), DETAILS_HIDE_DELAY_MILLIS);
                // }
            }

            @Override
            public void onPause() {
                FragmentActivity activity = PostViewV2Fragment.this.getActivity();
                if (activity == null) return;
                Utils.disableKeepScreenOn(activity);
            }

            @Override
            public void onRelease() {
                FragmentActivity activity = PostViewV2Fragment.this.getActivity();
                if (activity == null) return;
                Utils.disableKeepScreenOn(activity);
            }

            @Override
            public void onFullScreenModeChanged(boolean isFullScreen, StyledPlayerView playerView) {
                PostViewV2Fragment.this.playerView = playerView;
                if (isFullScreen) {
                    PostViewV2Fragment.this.hideSystemUI();
                    return;
                }
                PostViewV2Fragment.this.showSystemUI();
            }
        };
        float aspectRatio = (float) media.getOriginalWidth() / media.getOriginalHeight();
        String videoUrl = null;
        List<MediaCandidate> videoVersions = media.getVideoVersions();
        if (videoVersions != null && !videoVersions.isEmpty()) {
            MediaCandidate videoVersion = videoVersions.get(0);
            if (videoVersion != null) {
                videoUrl = videoVersion.getUrl();
            }
        }
        if (videoUrl != null) {
            this.videoPlayerViewHelper = new VideoPlayerViewHelper(
                    this.binding.getRoot().getContext(),
                    videoPost,
                    videoUrl,
                    vol,
                    aspectRatio,
                    ResponseBodyUtils.getThumbUrl(media),
                    true,
                    videoPlayerCallback);
        }
    }

    private void setupOptions(Boolean show) {
        if (!show) {
            this.binding.options.setVisibility(View.GONE);
            return;
        }
        this.binding.options.setVisibility(View.VISIBLE);
        this.binding.options.setOnClickListener(v -> {
            if (this.optionsPopup == null) return;
            this.optionsPopup.show();
        });
    }

    private void createOptionsPopupMenu() {
        if (this.optionsPopup == null) {
            Context context = this.getContext();
            if (context == null) return;
            ContextThemeWrapper themeWrapper = new ContextThemeWrapper(context, R.style.popupMenuStyle);
            this.optionsPopup = new PopupMenu(themeWrapper, this.binding.options);
        } else {
            this.optionsPopup.getMenu().clear();
        }
        this.optionsPopup.getMenuInflater().inflate(R.menu.post_view_menu, this.optionsPopup.getMenu());
        // final Menu menu = optionsPopup.getMenu();
        // final int size = menu.size();
        // for (int i = 0; i < size; i++) {
        //     final MenuItem item = menu.getItem(i);
        //     if (item == null) continue;
        //     if (options.contains(item.getItemId())) continue;
        //     menu.removeItem(item.getItemId());
        // }
        this.optionsPopup.setOnMenuItemClickListener(item -> {
            final int itemId = item.getItemId();
            if (itemId == R.id.edit_caption) {
                this.showCaptionEditDialog();
                return true;
            }
            if (itemId == R.id.delete) {
                item.setEnabled(false);
                LiveData<Resource<Object>> resourceLiveData = this.viewModel.delete();
                this.handleDeleteResource(resourceLiveData, item);
            }
            return true;
        });
    }

    private void handleDeleteResource(LiveData<Resource<Object>> resourceLiveData, MenuItem item) {
        if (resourceLiveData == null) return;
        resourceLiveData.observe(this.getViewLifecycleOwner(), new Observer<Resource<Object>>() {
            @Override
            public void onChanged(Resource<Object> resource) {
                try {
                    switch (resource.status) {
                        case SUCCESS:
                            PostViewV2Fragment.this.wasDeleted = true;
                            if (PostViewV2Fragment.this.onDeleteListener != null) {
                                PostViewV2Fragment.this.onDeleteListener.onDelete();
                            }
                            break;
                        case ERROR:
                            if (item != null) {
                                item.setEnabled(true);
                            }
                            Snackbar snackbar = Snackbar.make(PostViewV2Fragment.this.binding.getRoot(),
                                                                    R.string.delete_unsuccessful,
                                    BaseTransientBottomBar.LENGTH_INDEFINITE);
                            snackbar.setAction(R.string.ok, null);
                            snackbar.show();
                            break;
                        case LOADING:
                            if (item != null) {
                                item.setEnabled(false);
                            }
                            break;
                    }
                } finally {
                    resourceLiveData.removeObserver(this);
                }
            }
        });
    }

    private void showCaptionEditDialog() {
        Caption caption = this.viewModel.getCaption().getValue();
        String captionText = caption != null ? caption.getText() : null;
        this.editTextDialogFragment = EditTextDialogFragment
                .newInstance(R.string.edit_caption, R.string.confirm, R.string.cancel, captionText);
        this.editTextDialogFragment.show(this.getChildFragmentManager(), "edit_caption");
    }

    @Override
    public void onPositiveButtonClicked(String caption) {
        this.handleEditCaptionResource(this.viewModel.updateCaption(caption));
        if (this.editTextDialogFragment == null) return;
        this.editTextDialogFragment.dismiss();
        this.editTextDialogFragment = null;
    }

    private void handleEditCaptionResource(LiveData<Resource<Object>> updateCaption) {
        if (updateCaption == null) return;
        updateCaption.observe(this.getViewLifecycleOwner(), resource -> {
            MenuItem item = this.optionsPopup.getMenu().findItem(R.id.edit_caption);
            switch (resource.status) {
                case SUCCESS:
                    if (item != null) {
                        item.setEnabled(true);
                    }
                    break;
                case ERROR:
                    if (item != null) {
                        item.setEnabled(true);
                    }
                    Snackbar snackbar = Snackbar.make(this.binding.getRoot(), R.string.edit_unsuccessful, BaseTransientBottomBar.LENGTH_INDEFINITE);
                    snackbar.setAction(R.string.ok, null);
                    snackbar.show();
                    break;
                case LOADING:
                    if (item != null) {
                        item.setEnabled(false);
                    }
                    break;
            }
        });
    }

    @Override
    public void onNegativeButtonClicked() {
        if (this.editTextDialogFragment == null) return;
        this.editTextDialogFragment.dismiss();
        this.editTextDialogFragment = null;
    }

    private void toggleDetails() {
        // final boolean hasBeenToggled = true;
        MainActivity activity = (MainActivity) this.getActivity();
        if (activity == null) return;
        Media media = this.viewModel.getMedia();
        this.binding.getRoot().post(() -> {
            TransitionManager.beginDelayedTransition(this.binding.getRoot());
            if (this.detailsVisible) {
                Context context = this.getContext();
                if (context == null) return;
                this.originalRootBackground = this.binding.getRoot().getBackground();
                Resources resources = context.getResources();
                if (resources == null) return;
                ColorDrawable colorDrawable = new ColorDrawable(resources.getColor(R.color.black));
                this.binding.getRoot().setBackground(colorDrawable);
                if (this.postView != null) {
                    // Make post match parent
                    int fullHeight = Utils.displayMetrics.heightPixels - Utils.getStatusBarHeight(context);
                    this.postView.getLayoutParams().height = fullHeight;
                    this.binding.postContainer.getLayoutParams().height = fullHeight;
                    if (this.playerView != null) {
                        this.playerViewOriginalHeight = this.playerView.getLayoutParams().height;
                        this.playerView.getLayoutParams().height = fullHeight;
                    }
                }
                BottomNavigationView bottomNavView = activity.getBottomNavView();
                bottomNavView.setVisibility(View.GONE);
                this.detailsVisible = false;
                if (media.getUser() != null) {
                    this.binding.profilePic.setVisibility(View.GONE);
                    this.binding.title.setVisibility(View.GONE);
                    this.binding.subtitle.setVisibility(View.GONE);
                }
                if (media.getLocation() != null) {
                    this.binding.location.setVisibility(View.GONE);
                }
                if (media.getCaption() != null && !TextUtils.isEmpty(media.getCaption().getText())) {
                    this.bottom.caption.setVisibility(View.GONE);
                    this.bottom.translate.setVisibility(View.GONE);
                }
                this.bottom.likesCount.setVisibility(View.GONE);
                this.bottom.commentsCount.setVisibility(View.GONE);
                this.bottom.date.setVisibility(View.GONE);
                this.bottom.comment.setVisibility(View.GONE);
                this.bottom.like.setVisibility(View.GONE);
                this.bottom.save.setVisibility(View.GONE);
                this.bottom.share.setVisibility(View.GONE);
                this.bottom.download.setVisibility(View.GONE);
                this.binding.mediaCounter.setVisibility(View.GONE);
                this.bottom.viewsCount.setVisibility(View.GONE);
                List<Integer> options = this.viewModel.getOptions().getValue();
                if (options != null && !options.isEmpty()) {
                    this.binding.options.setVisibility(View.GONE);
                }
                return;
            }
            if (this.originalRootBackground != null) {
                this.binding.getRoot().setBackground(this.originalRootBackground);
            }
            if (this.postView != null) {
                // Make post height back to original
                this.postView.getLayoutParams().height = this.originalHeight;
                this.binding.postContainer.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                if (this.playerView != null) {
                    this.playerView.getLayoutParams().height = this.playerViewOriginalHeight;
                    this.playerView = null;
                }
            }
            BottomNavigationView bottomNavView = activity.getBottomNavView();
            bottomNavView.setVisibility(View.VISIBLE);
            if (media.getUser() != null) {
                this.binding.profilePic.setVisibility(View.VISIBLE);
                this.binding.title.setVisibility(View.VISIBLE);
                this.binding.subtitle.setVisibility(View.VISIBLE);
                // binding.topBg.setVisibility(View.VISIBLE);
            }
            if (media.getLocation() != null) {
                this.binding.location.setVisibility(View.VISIBLE);
            }
            if (media.getCaption() != null && !TextUtils.isEmpty(media.getCaption().getText())) {
                this.bottom.caption.setVisibility(View.VISIBLE);
                this.bottom.translate.setVisibility(View.VISIBLE);
            }
            if (this.viewModel.hasPk()) {
                this.bottom.likesCount.setVisibility(View.VISIBLE);
                this.bottom.date.setVisibility(View.VISIBLE);
                // binding.captionParent.setVisibility(View.VISIBLE);
                // binding.captionToggle.setVisibility(View.VISIBLE);
                this.bottom.share.setVisibility(View.VISIBLE);
            }
            if (this.viewModel.hasPk() && !this.viewModel.getMedia().getCommentsDisabled()) {
                this.bottom.comment.setVisibility(View.VISIBLE);
                this.bottom.commentsCount.setVisibility(View.VISIBLE);
            }
            this.bottom.download.setVisibility(View.VISIBLE);
            List<Integer> options = this.viewModel.getOptions().getValue();
            if (options != null && !options.isEmpty()) {
                this.binding.options.setVisibility(View.VISIBLE);
            }
            if (this.viewModel.isLoggedIn() && this.viewModel.hasPk()) {
                this.bottom.like.setVisibility(View.VISIBLE);
                this.bottom.save.setVisibility(View.VISIBLE);
            }
            // if (video) {
            if (media.getType() == MediaItemType.MEDIA_TYPE_VIDEO) {
                // binding.playerControlsToggle.setVisibility(View.VISIBLE);
                this.bottom.viewsCount.setVisibility(View.VISIBLE);
            }
            // if (wasControlsVisible) {
            //     showPlayerControls();
            // }
            if (media.getType() == MediaItemType.MEDIA_TYPE_SLIDER) {
                this.binding.mediaCounter.setVisibility(View.VISIBLE);
            }
            this.detailsVisible = true;
        });
    }

    private void hideSystemUI() {
        if (this.detailsVisible) {
            this.toggleDetails();
        }
        MainActivity activity = (MainActivity) this.getActivity();
        if (activity == null) return;
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        CollapsingToolbarLayout appbarLayout = activity.getCollapsingToolbarView();
        appbarLayout.setVisibility(View.GONE);
        Toolbar toolbar = activity.getToolbar();
        toolbar.setVisibility(View.GONE);
        this.binding.getRoot().setPadding(this.binding.getRoot().getPaddingLeft(),
                this.binding.getRoot().getPaddingTop(),
                this.binding.getRoot().getPaddingRight(),
                                     0);
        this.controller.hide(WindowInsetsCompat.Type.systemBars());
        this.controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE);
        this.isInFullScreenMode = true;
    }

    private void showSystemUI() {
        if (!this.detailsVisible) {
            this.toggleDetails();
        }
        MainActivity activity = (MainActivity) this.getActivity();
        if (activity == null) return;
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
        }
        CollapsingToolbarLayout appbarLayout = activity.getCollapsingToolbarView();
        appbarLayout.setVisibility(View.VISIBLE);
        Toolbar toolbar = activity.getToolbar();
        toolbar.setVisibility(View.VISIBLE);
        Context context = this.getContext();
        if (context == null) return;
        this.binding.getRoot().setPadding(this.binding.getRoot().getPaddingLeft(),
                this.binding.getRoot().getPaddingTop(),
                this.binding.getRoot().getPaddingRight(),
                                     Utils.getActionBarHeight(context));
        this.controller.show(WindowInsetsCompat.Type.systemBars());
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        this.isInFullScreenMode = false;
    }

    private void navigateToProfile(String username) {
        NavController navController = this.getNavController();
        if (navController == null) return;
        NavDirections actionToProfile = PostViewV2FragmentDirections.actionToProfile().setUsername(username);
        navController.navigate(actionToProfile);
    }

    @Nullable
    private NavController getNavController() {
        NavController navController = null;
        try {
            navController = NavHostFragment.findNavController(this);
        } catch (final IllegalStateException e) {
            Log.e(PostViewV2Fragment.TAG, "navigateToProfile", e);
        }
        return navController;
    }

    public boolean wasDeleted() {
        return this.wasDeleted;
    }
}
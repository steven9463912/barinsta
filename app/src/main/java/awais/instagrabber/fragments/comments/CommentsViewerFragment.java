package awais.instagrabber.fragments.comments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.CommentsAdapter;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.FragmentCommentsBinding;
import awais.instagrabber.fragments.settings.PreferenceKeys;
import awais.instagrabber.models.Comment;
import awais.instagrabber.models.Resource;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.AppStateViewModel;
import awais.instagrabber.viewmodels.CommentsViewerViewModel;

public final class CommentsViewerFragment extends BottomSheetDialogFragment {
    private static final String TAG = CommentsViewerFragment.class.getSimpleName();

    private CommentsViewerViewModel viewModel;
    private CommentsAdapter commentsAdapter;
    private FragmentCommentsBinding binding;
    private ConstraintLayout root;
    private boolean shouldRefresh = true;
    private AppStateViewModel appStateViewModel;
    private boolean showingReplies;

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = this.getDialog();
        if (dialog == null) return;
        BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
        View bottomSheetInternal = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal == null) return;
        bottomSheetInternal.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        bottomSheetInternal.requestLayout();
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetInternal);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentActivity activity = this.getActivity();
        if (activity == null) return;
        this.viewModel = new ViewModelProvider(this).get(CommentsViewerViewModel.class);
        this.appStateViewModel = new ViewModelProvider(activity).get(AppStateViewModel.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new BottomSheetDialog(this.requireContext(), this.getTheme()) {
            @Override
            public void onBackPressed() {
                if (CommentsViewerFragment.this.showingReplies) {
                    CommentsViewerFragment.this.getChildFragmentManager().popBackStack();
                    CommentsViewerFragment.this.showingReplies = false;
                    return;
                }
                super.onBackPressed();
            }
        };
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (this.root != null) {
            this.shouldRefresh = false;
            return this.root;
        }
        this.binding = FragmentCommentsBinding.inflate(this.getLayoutInflater());
        this.binding.swipeRefreshLayout.setEnabled(false);
        this.binding.swipeRefreshLayout.setNestedScrollingEnabled(false);
        this.root = this.binding.getRoot();
        this.appStateViewModel.getCurrentUserLiveData().observe(this.getViewLifecycleOwner(), userResource -> {
            if (userResource == null || userResource.status == Resource.Status.LOADING) return;
            this.viewModel.setCurrentUser(userResource.data);
            if (userResource.data == null) {
                this.viewModel.fetchComments();
                return;
            }
            this.viewModel.getCurrentUserId().observe(this.getViewLifecycleOwner(), new Observer<Long>() {
                @Override
                public void onChanged(Long i) {
                    if (i != 0L) {
                        CommentsViewerFragment.this.viewModel.fetchComments();
                        CommentsViewerFragment.this.viewModel.getCurrentUserId().removeObserver(this);
                    }
                }
            });
        });
        if (this.getArguments() == null) return this.root;
        CommentsViewerFragmentArgs args = CommentsViewerFragmentArgs.fromBundle(this.getArguments());
        this.viewModel.setPostDetails(args.getShortCode(), args.getPostId(), args.getPostUserId());
        return this.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (!this.shouldRefresh) return;
        this.shouldRefresh = false;
        this.init();
    }

    private void init() {
        this.setupToolbar();
        this.setupList();
        this.setupObservers();
    }

    private void setupObservers() {
        this.viewModel.getCurrentUserId().observe(this.getViewLifecycleOwner(), currentUserId -> {
            long userId = 0;
            if (currentUserId != null) {
                userId = currentUserId;
            }
            this.setupAdapter(userId);
            if (userId == 0) return;
            Helper.setupCommentInput(this.binding.commentField, this.binding.commentText, false, text -> {
                LiveData<Resource<Object>> resourceLiveData = this.viewModel.comment(text, false);
                resourceLiveData.observe(this.getViewLifecycleOwner(), new Observer<Resource<Object>>() {
                    @Override
                    public void onChanged(Resource<Object> objectResource) {
                        if (objectResource == null) return;
                        Context context = CommentsViewerFragment.this.getContext();
                        if (context == null) return;
                        Helper.handleCommentResource(
                                context,
                                objectResource.status,
                                objectResource.message,
                                resourceLiveData,
                                this,
                                CommentsViewerFragment.this.binding.commentField,
                                CommentsViewerFragment.this.binding.commentText,
                                CommentsViewerFragment.this.binding.comments);
                    }
                });
                return null;
            });
        });
        this.viewModel.getRootList().observe(this.getViewLifecycleOwner(), listResource -> {
            if (listResource == null) return;
            switch (listResource.status) {
                case SUCCESS:
                    this.binding.swipeRefreshLayout.setRefreshing(false);
                    if (this.commentsAdapter != null) {
                        this.commentsAdapter.submitList(listResource.data);
                    }
                    break;
                case ERROR:
                    this.binding.swipeRefreshLayout.setRefreshing(false);
                    if (!TextUtils.isEmpty(listResource.message)) {
                        Snackbar.make(this.binding.getRoot(), listResource.message, BaseTransientBottomBar.LENGTH_LONG).show();
                    }
                    break;
                case LOADING:
                    this.binding.swipeRefreshLayout.setRefreshing(true);
                    break;
            }
        });
        this.viewModel.getRootCommentsCount().observe(this.getViewLifecycleOwner(), count -> {
            if (count == null || count == 0) {
                this.binding.toolbar.setTitle(R.string.title_comments);
                return;
            }
            String titleComments = this.getString(R.string.title_comments);
            String countString = String.valueOf(count);
            SpannableString titleWithCount = new SpannableString(String.format("%s   %s", titleComments, countString));
            titleWithCount.setSpan(new RelativeSizeSpan(0.8f),
                                   titleWithCount.length() - countString.length(),
                                   titleWithCount.length(),
                                   Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.binding.toolbar.setTitle(titleWithCount);
        });
    }

    private void setupToolbar() {
        this.binding.toolbar.setTitle(R.string.title_comments);
    }

    private void setupAdapter(long currentUserId) {
        Context context = this.getContext();
        if (context == null) return;
        this.commentsAdapter = new CommentsAdapter(currentUserId, false, Helper.getCommentCallback(
                context,
                this.getViewLifecycleOwner(),
                this.getNavController(),
                this.viewModel,
                (comment, focusInput) -> {
                    if (comment == null) return null;
                    boolean disableTransition = Utils.settingsHelper.getBoolean(PreferenceKeys.PREF_DISABLE_SCREEN_TRANSITIONS);
                    RepliesFragment repliesFragment = RepliesFragment.newInstance(comment, focusInput != null && focusInput);
                    FragmentTransaction transaction = this.getChildFragmentManager().beginTransaction();
                    if (!disableTransition) {
                        transaction.setCustomAnimations(R.anim.slide_left, R.anim.slide_right, 0, R.anim.slide_right);
                    }
                    transaction.add(R.id.replies_container_view, repliesFragment)
                               .addToBackStack(RepliesFragment.TAG)
                               .commit();
                    this.showingReplies = true;
                    return null;
                }));
        Resource<List<Comment>> listResource = this.viewModel.getRootList().getValue();
        this.binding.comments.setAdapter(this.commentsAdapter);
        this.commentsAdapter.submitList(listResource != null ? listResource.data : Collections.emptyList());
    }

    private void setupList() {
        Context context = this.getContext();
        if (context == null) return;
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        RecyclerLazyLoader lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> this.viewModel.fetchComments());
        Helper.setupList(context, this.binding.comments, layoutManager, lazyLoader);
    }

    @Nullable
    private NavController getNavController() {
        NavController navController = null;
        try {
            navController = NavHostFragment.findNavController(this);
        } catch (final IllegalStateException e) {
            Log.e(CommentsViewerFragment.TAG, "navigateToProfile", e);
        }
        return navController;
    }
}
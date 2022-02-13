package awais.instagrabber.fragments.comments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.adapters.CommentsAdapter;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.FragmentCommentsBinding;
import awais.instagrabber.models.Comment;
import awais.instagrabber.models.Resource;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.CommentsViewerViewModel;

public class RepliesFragment extends Fragment {
    public static final String TAG = RepliesFragment.class.getSimpleName();
    private static final String ARG_PARENT = "parent";
    private static final String ARG_FOCUS_INPUT = "focus";

    private FragmentCommentsBinding binding;
    private CommentsViewerViewModel viewModel;
    private CommentsAdapter commentsAdapter;

    @NonNull
    public static RepliesFragment newInstance(@NonNull Comment parent,
                                              boolean focusInput) {
        Bundle args = new Bundle();
        args.putSerializable(RepliesFragment.ARG_PARENT, parent);
        args.putBoolean(RepliesFragment.ARG_FOCUS_INPUT, focusInput);
        RepliesFragment fragment = new RepliesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fragment parentFragment = this.getParentFragment();
        if (parentFragment == null) return;
        this.viewModel = new ViewModelProvider(parentFragment).get(CommentsViewerViewModel.class);
        Bundle bundle = this.getArguments();
        if (bundle == null) return;
        Serializable serializable = bundle.getSerializable(RepliesFragment.ARG_PARENT);
        if (!(serializable instanceof Comment)) return;
        this.viewModel.showReplies((Comment) serializable);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = FragmentCommentsBinding.inflate(inflater, container, false);
        this.binding.swipeRefreshLayout.setEnabled(false);
        this.binding.swipeRefreshLayout.setNestedScrollingEnabled(false);
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        this.setupToolbar();
    }

    @Override
    public Animation onCreateAnimation(final int transit, final boolean enter, final int nextAnim) {
        if (!enter) {
            return super.onCreateAnimation(transit, false, nextAnim);
        }
        if (nextAnim == 0) {
            this.setupList();
            this.setupObservers();
            return super.onCreateAnimation(transit, true, 0);
        }
        Animation animation = AnimationUtils.loadAnimation(this.getContext(), nextAnim);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(final Animation animation) {}

            @Override
            public void onAnimationEnd(final Animation animation) {
                RepliesFragment.this.setupList();
                RepliesFragment.this.setupObservers();
            }

            @Override
            public void onAnimationRepeat(final Animation animation) {}
        });
        return animation;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.viewModel != null) {
            this.viewModel.clearReplies();
        }
    }

    private void setupObservers() {
        if (this.viewModel == null) return;
        this.viewModel.getCurrentUserId().observe(this.getViewLifecycleOwner(), currentUserId -> {
            long userId = 0;
            if (currentUserId != null) {
                userId = currentUserId;
            }
            this.setupAdapter(userId);
            if (userId == 0) return;
            Helper.setupCommentInput(this.binding.commentField, this.binding.commentText, true, text -> {
                LiveData<Resource<Object>> resourceLiveData = this.viewModel.comment(text, true);
                resourceLiveData.observe(this.getViewLifecycleOwner(), new Observer<Resource<Object>>() {
                    @Override
                    public void onChanged(Resource<Object> objectResource) {
                        if (objectResource == null) return;
                        Context context = RepliesFragment.this.getContext();
                        if (context == null) return;
                        Helper.handleCommentResource(context,
                                                     objectResource.status,
                                                     objectResource.message,
                                                     resourceLiveData,
                                                     this,
                                RepliesFragment.this.binding.commentField,
                                RepliesFragment.this.binding.commentText,
                                RepliesFragment.this.binding.comments);
                    }
                });
                return null;
            });
            Bundle bundle = this.getArguments();
            if (bundle == null) return;
            boolean focusInput = bundle.getBoolean(RepliesFragment.ARG_FOCUS_INPUT);
            if (focusInput && this.viewModel.getRepliesParent() != null) {
                this.viewModel.getRepliesParent().getUser();
                this.binding.commentText.setText(String.format("@%s ", this.viewModel.getRepliesParent().getUser().getUsername()));
                Utils.showKeyboard(this.binding.commentText);
            }
        });
        this.viewModel.getReplyList().observe(this.getViewLifecycleOwner(), listResource -> {
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
                    String message = listResource.message;
                    if (!TextUtils.isEmpty(message)) {
                        Snackbar.make(this.binding.getRoot(), message, BaseTransientBottomBar.LENGTH_LONG).show();
                    }
                    break;
                case LOADING:
                    this.binding.swipeRefreshLayout.setRefreshing(true);
                    break;
            }
        });
    }

    private void setupToolbar() {
        this.binding.toolbar.setTitle(R.string.title_replies);
        this.binding.toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24);
        this.binding.toolbar.setNavigationOnClickListener(v -> {
            FragmentManager fragmentManager = this.getParentFragmentManager();
            fragmentManager.popBackStack();
        });
    }

    private void setupAdapter(long currentUserId) {
        if (this.viewModel == null) return;
        Context context = this.getContext();
        if (context == null) return;
        this.commentsAdapter = new CommentsAdapter(
                currentUserId,
                true,
                Helper.getCommentCallback(
                        context,
                        this.getViewLifecycleOwner(),
                        this.getNavController(),
                        this.viewModel,
                        (comment, focusInput) -> {
                            this.viewModel.setReplyTo(comment);
                            this.binding.commentText.setText(String.format("@%s ", comment.getUser().getUsername()));
                            if (focusInput) Utils.showKeyboard(this.binding.commentText);
                            return null;
                        }
                )
        );
        this.binding.comments.setAdapter(this.commentsAdapter);
        Resource<List<Comment>> listResource = this.viewModel.getReplyList().getValue();
        this.commentsAdapter.submitList(listResource != null ? listResource.data : Collections.emptyList());
    }

    private void setupList() {
        Context context = this.getContext();
        if (context == null) return;
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        RecyclerLazyLoader lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
            if (this.viewModel != null) this.viewModel.fetchReplies();
        });
        Helper.setupList(context, this.binding.comments, layoutManager, lazyLoader);
    }

    @Nullable
    private NavController getNavController() {
        NavController navController = null;
        try {
            navController = NavHostFragment.findNavController(this);
        } catch (final IllegalStateException e) {
            Log.e(RepliesFragment.TAG, "navigateToProfile", e);
        }
        return navController;
    }
}

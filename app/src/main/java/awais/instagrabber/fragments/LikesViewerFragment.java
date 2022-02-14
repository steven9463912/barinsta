package awais.instagrabber.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

import awais.instagrabber.adapters.LikesAdapter;
import awais.instagrabber.customviews.helpers.RecyclerLazyLoader;
import awais.instagrabber.databinding.FragmentLikesBinding;
import awais.instagrabber.repositories.responses.GraphQLUserListFetchResponse;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.webservices.GraphQLRepository;
import awais.instagrabber.webservices.MediaRepository;
import awais.instagrabber.webservices.ServiceCallback;
import kotlinx.coroutines.Dispatchers;

import static awais.instagrabber.utils.Utils.settingsHelper;

public final class LikesViewerFragment extends BottomSheetDialogFragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = LikesViewerFragment.class.getSimpleName();

    private FragmentLikesBinding binding;
    private RecyclerLazyLoader lazyLoader;
    private MediaRepository mediaRepository;
    private GraphQLRepository graphQLRepository;
    private boolean isLoggedIn;
    private String postId, endCursor;
    private boolean isComment;

    private final ServiceCallback<List<User>> cb = new ServiceCallback<List<User>>() {
        @Override
        public void onSuccess(List<User> result) {
            LikesAdapter likesAdapter = new LikesAdapter(result, v -> {
                Object tag = v.getTag();
                if (tag instanceof User) {
                    final User model = (User) tag;
                    try {
                        NavDirections action = LikesViewerFragmentDirections.actionToProfile().setUsername(model.getUsername());
                        NavHostFragment.findNavController(LikesViewerFragment.this).navigate(action);
                    } catch (final Exception e) {
                        Log.e(LikesViewerFragment.TAG, "onSuccess: ", e);
                    }
                }
            });
            LikesViewerFragment.this.binding.rvLikes.setAdapter(likesAdapter);
            Context context = LikesViewerFragment.this.getContext();
            if (context == null) return;
            LikesViewerFragment.this.binding.rvLikes.setLayoutManager(new LinearLayoutManager(context));
            LikesViewerFragment.this.binding.rvLikes.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
            LikesViewerFragment.this.binding.swipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void onFailure(Throwable t) {
            Log.e(LikesViewerFragment.TAG, "Error", t);
            try {
                Context context = LikesViewerFragment.this.getContext();
                Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (final Exception ignored) {}
        }
    };

    private final ServiceCallback<GraphQLUserListFetchResponse> anonCb = new ServiceCallback<GraphQLUserListFetchResponse>() {
        @Override
        public void onSuccess(GraphQLUserListFetchResponse result) {
            LikesViewerFragment.this.endCursor = result.getNextMaxId();
            LikesAdapter likesAdapter = new LikesAdapter(result.getItems(), v -> {
                Object tag = v.getTag();
                if (tag instanceof User) {
                    final User model = (User) tag;
                    try {
                        NavDirections action = LikesViewerFragmentDirections.actionToProfile().setUsername(model.getUsername());
                        NavHostFragment.findNavController(LikesViewerFragment.this).navigate(action);
                    } catch (final Exception e) {
                        Log.e(LikesViewerFragment.TAG, "onSuccess: ", e);
                    }
                }
            });
            LikesViewerFragment.this.binding.rvLikes.setAdapter(likesAdapter);
            LikesViewerFragment.this.binding.swipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void onFailure(Throwable t) {
            Log.e(LikesViewerFragment.TAG, "Error", t);
            try {
                Context context = LikesViewerFragment.this.getContext();
                Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (final Exception ignored) {}
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String cookie = settingsHelper.getString(Constants.COOKIE);
        long userId = CookieUtils.getUserIdFromCookie(cookie);
        this.isLoggedIn = !TextUtils.isEmpty(cookie) && userId != 0;
        // final String deviceUuid = settingsHelper.getString(Constants.DEVICE_UUID);
        String csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        if (csrfToken == null) return;
        this.mediaRepository = this.isLoggedIn ? MediaRepository.Companion.getInstance() : null;
        this.graphQLRepository = this.isLoggedIn ? null : GraphQLRepository.Companion.getInstance();
        // setHasOptionsMenu(true);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = FragmentLikesBinding.inflate(this.getLayoutInflater());
        this.binding.swipeRefreshLayout.setEnabled(false);
        this.binding.swipeRefreshLayout.setNestedScrollingEnabled(false);
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        this.init();
    }

    @Override
    public void onRefresh() {
        if (this.isComment && !this.isLoggedIn) {
            this.lazyLoader.resetState();
            this.graphQLRepository.fetchCommentLikers(
                    this.postId,
                    null,
                    CoroutineUtilsKt.getContinuation((response, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                        if (throwable != null) {
                            this.anonCb.onFailure(throwable);
                            return;
                        }
                        this.anonCb.onSuccess(response);
                    }), Dispatchers.getIO())
            );
        } else {
            this.mediaRepository.fetchLikes(
                    this.postId,
                    this.isComment,
                    CoroutineUtilsKt.getContinuation((users, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                        if (throwable != null) {
                            this.cb.onFailure(throwable);
                            return;
                        }
                        //noinspection unchecked
                        this.cb.onSuccess((List<User>) users);
                    }), Dispatchers.getIO())
            );
        }
    }

    private void init() {
        if (this.getArguments() == null) return;
        LikesViewerFragmentArgs fragmentArgs = LikesViewerFragmentArgs.fromBundle(this.getArguments());
        this.postId = fragmentArgs.getPostId();
        this.isComment = fragmentArgs.getIsComment();
        this.binding.swipeRefreshLayout.setOnRefreshListener(this);
        this.binding.swipeRefreshLayout.setRefreshing(true);
        if (this.isComment && !this.isLoggedIn) {
            Context context = this.getContext();
            if (context == null) return;
            LinearLayoutManager layoutManager = new LinearLayoutManager(context);
            this.binding.rvLikes.setLayoutManager(layoutManager);
            this.binding.rvLikes.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL));
            this.lazyLoader = new RecyclerLazyLoader(layoutManager, (page, totalItemsCount) -> {
                if (!TextUtils.isEmpty(this.endCursor)) {
                    this.graphQLRepository.fetchCommentLikers(
                            this.postId,
                            this.endCursor,
                            CoroutineUtilsKt.getContinuation((response, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                                if (throwable != null) {
                                    this.anonCb.onFailure(throwable);
                                    return;
                                }
                                this.anonCb.onSuccess(response);
                            }), Dispatchers.getIO())
                    );
                }
                this.endCursor = null;
            });
            this.binding.rvLikes.addOnScrollListener(this.lazyLoader);
        }
        this.onRefresh();
    }
}
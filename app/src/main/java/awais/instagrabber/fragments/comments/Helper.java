package awais.instagrabber.fragments.comments;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.internal.CheckableImageButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.function.BiFunction;
import java.util.function.Function;

import awais.instagrabber.R;
import awais.instagrabber.adapters.CommentsAdapter;
import awais.instagrabber.customviews.helpers.TextWatcherAdapter;
import awais.instagrabber.models.Comment;
import awais.instagrabber.models.Resource;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.CommentsViewerViewModel;
import awais.instagrabber.webservices.ServiceCallback;

public final class Helper {
    private static final String TAG = Helper.class.getSimpleName();

    public static void setupList(@NonNull Context context,
                                 @NonNull RecyclerView list,
                                 @NonNull RecyclerView.LayoutManager layoutManager,
                                 @NonNull RecyclerView.OnScrollListener lazyLoader) {
        list.setLayoutManager(layoutManager);
        DividerItemDecoration itemDecoration = new DividerItemDecoration(context, LinearLayoutManager.VERTICAL);
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.pref_list_divider_material);
        if (drawable != null) {
            itemDecoration.setDrawable(drawable);
        }
        list.addItemDecoration(itemDecoration);
        list.addOnScrollListener(lazyLoader);
    }

    @NonNull
    public static CommentsAdapter.CommentCallback getCommentCallback(@NonNull final Context context,
                                                                     final LifecycleOwner lifecycleOwner,
                                                                     final NavController navController,
                                                                     @NonNull final CommentsViewerViewModel viewModel,
                                                                     final BiFunction<Comment, Boolean, Void> onRepliesClick) {
        return new CommentsAdapter.CommentCallback() {
            @Override
            public void onClick(Comment comment) {
                // onCommentClick(comment);
                if (onRepliesClick == null) return;
                onRepliesClick.apply(comment, false);
            }

            @Override
            public void onHashtagClick(String hashtag) {
                try {
                    if (navController == null) return;
                    navController.navigate(CommentsViewerFragmentDirections.actionToHashtag(hashtag));
                } catch (final Exception e) {
                    Log.e(Helper.TAG, "onHashtagClick: ", e);
                }
            }

            @Override
            public void onMentionClick(String mention) {
                Helper.openProfile(navController, mention);
            }

            @Override
            public void onURLClick(String url) {
                Utils.openURL(context, url);
            }

            @Override
            public void onEmailClick(String emailAddress) {
                Utils.openEmailAddress(context, emailAddress);
            }

            @Override
            public void onLikeClick(Comment comment, boolean liked, boolean isReply) {
                if (comment == null) return;
                LiveData<Resource<Object>> resourceLiveData = viewModel.likeComment(comment, liked, isReply);
                resourceLiveData.observe(lifecycleOwner, new Observer<Resource<Object>>() {
                    @Override
                    public void onChanged(Resource<Object> objectResource) {
                        if (objectResource == null) return;
                        switch (objectResource.status) {
                            case SUCCESS:
                                resourceLiveData.removeObserver(this);
                                break;
                            case LOADING:
                                break;
                            case ERROR:
                                if (objectResource.message != null) {
                                    Toast.makeText(context, objectResource.message, Toast.LENGTH_LONG).show();
                                }
                                resourceLiveData.removeObserver(this);
                        }
                    }
                });
            }

            @Override
            public void onRepliesClick(Comment comment) {
                // viewModel.showReplies(comment);
                if (onRepliesClick == null) return;
                onRepliesClick.apply(comment, true);
            }

            @Override
            public void onViewLikes(Comment comment) {
                try {
                    if (navController == null) return;
                    NavDirections actionToLikes = CommentsViewerFragmentDirections.actionToLikes(comment.getPk(), true);
                    navController.navigate(actionToLikes);
                } catch (final Exception e) {
                    Log.e(Helper.TAG, "onViewLikes: ", e);
                }
            }

            @Override
            public void onTranslate(Comment comment) {
                if (comment == null) return;
                viewModel.translate(comment, new ServiceCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        if (TextUtils.isEmpty(result)) {
                            Toast.makeText(context, R.string.downloader_unknown_error, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String username = comment.getUser().getUsername();
                        new MaterialAlertDialogBuilder(context)
                                .setTitle(username)
                                .setMessage(result)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e(Helper.TAG, "Error translating comment", t);
                        Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onDelete(Comment comment, boolean isReply) {
                if (comment == null) return;
                LiveData<Resource<Object>> resourceLiveData = viewModel.deleteComment(comment, isReply);
                resourceLiveData.observe(lifecycleOwner, new Observer<Resource<Object>>() {
                    @Override
                    public void onChanged(Resource<Object> objectResource) {
                        if (objectResource == null) return;
                        switch (objectResource.status) {
                            case SUCCESS:
                                resourceLiveData.removeObserver(this);
                                break;
                            case ERROR:
                                if (objectResource.message != null) {
                                    Toast.makeText(context, objectResource.message, Toast.LENGTH_LONG).show();
                                }
                                resourceLiveData.removeObserver(this);
                                break;
                            case LOADING:
                                break;
                        }
                    }
                });
            }
        };
    }

    private static void openProfile(NavController navController,
                                    @NonNull String username) {
        try {
            if (navController == null) return;
            NavDirections action = CommentsViewerFragmentDirections.actionToProfile().setUsername(username);
            navController.navigate(action);
        } catch (final Exception e) {
            Log.e(Helper.TAG, "openProfile: ", e);
        }
    }

    public static void setupCommentInput(@NonNull TextInputLayout commentField,
                                         @NonNull TextInputEditText commentText,
                                         boolean isReplyFragment,
                                         @NonNull Function<String, Void> commentFunction) {
        // commentField.setStartIconVisible(false);
        commentField.setVisibility(View.VISIBLE);
        commentField.setEndIconVisible(false);
        if (isReplyFragment) {
            commentField.setHint(R.string.reply_hint);
        }
        commentText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isEmpty = TextUtils.isEmpty(s);
                commentField.setStartIconVisible(!isEmpty);
                commentField.setEndIconVisible(!isEmpty);
                commentField.setCounterEnabled(s != null && s.length() > 2000); // show the counter when user approaches the limit
            }
        });
        // commentField.setStartIconOnClickListener(v -> {
        //     // commentsAdapter.clearSelection();
        //     commentText.setText("");
        // });
        commentField.setEndIconOnClickListener(v -> {
            Editable text = commentText.getText();
            if (TextUtils.isEmpty(text)) return;
            commentFunction.apply(text.toString().trim());
        });
    }

    public static void handleCommentResource(@NonNull Context context,
                                             @NonNull Resource.Status status,
                                             String message,
                                             @NonNull LiveData<Resource<Object>> resourceLiveData,
                                             @NonNull Observer<Resource<Object>> observer,
                                             @NonNull TextInputLayout commentField,
                                             @NonNull TextInputEditText commentText,
                                             @NonNull RecyclerView comments) {
        CheckableImageButton endIcon = null;
        try {
            endIcon = commentField.findViewById(com.google.android.material.R.id.text_input_end_icon);
        } catch (final Exception e) {
            Log.e(Helper.TAG, "setupObservers: ", e);
        }
        CheckableImageButton startIcon = null;
        try {
            startIcon = commentField.findViewById(com.google.android.material.R.id.text_input_start_icon);
        } catch (final Exception e) {
            Log.e(Helper.TAG, "setupObservers: ", e);
        }
        switch (status) {
            case SUCCESS:
                resourceLiveData.removeObserver(observer);
                comments.postDelayed(() -> comments.scrollToPosition(0), 500);
                if (startIcon != null) {
                    startIcon.setEnabled(true);
                }
                if (endIcon != null) {
                    endIcon.setEnabled(true);
                }
                commentText.setText("");
                break;
            case LOADING:
                commentText.setEnabled(false);
                if (startIcon != null) {
                    startIcon.setEnabled(false);
                }
                if (endIcon != null) {
                    endIcon.setEnabled(false);
                }
                break;
            case ERROR:
                if (message != null && context != null) {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                }
                if (startIcon != null) {
                    startIcon.setEnabled(true);
                }
                if (endIcon != null) {
                    endIcon.setEnabled(true);
                }
                resourceLiveData.removeObserver(observer);
        }
    }
}

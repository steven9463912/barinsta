package awais.instagrabber.viewmodels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.common.collect.ImmutableList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import awais.instagrabber.R;
import awais.instagrabber.models.Comment;
import awais.instagrabber.models.Resource;
import awais.instagrabber.repositories.responses.ChildCommentsFetchResponse;
import awais.instagrabber.repositories.responses.CommentsFetchResponse;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.webservices.CommentService;
import awais.instagrabber.webservices.GraphQLRepository;
import awais.instagrabber.webservices.ServiceCallback;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class CommentsViewerViewModel extends ViewModel {
    private static final String TAG = CommentsViewerViewModel.class.getSimpleName();

    private final MutableLiveData<Long> currentUserId = new MutableLiveData<>(0L);
    private final MutableLiveData<Resource<List<Comment>>> rootList = new MutableLiveData<>();
    private final MutableLiveData<Integer> rootCount = new MutableLiveData<>(0);
    private final MutableLiveData<Resource<List<Comment>>> replyList = new MutableLiveData<>();
    private final GraphQLRepository graphQLRepository;

    private String shortCode;
    private String postId;
    private String rootCursor;
    private boolean rootHasNext = true;
    private Comment repliesParent, replyTo;
    private String repliesCursor;
    private boolean repliesHasNext = true;
    private final CommentService commentService;
    private List<Comment> prevReplies;
    private String prevRepliesCursor;
    private boolean prevRepliesHasNext = true;

    private final ServiceCallback<CommentsFetchResponse> ccb = new ServiceCallback<CommentsFetchResponse>() {
        @Override
        public void onSuccess(CommentsFetchResponse result) {
            // Log.d(TAG, "onSuccess: " + result);
            if (result == null) {
                CommentsViewerViewModel.this.rootList.postValue(Resource.error(R.string.generic_null_response, CommentsViewerViewModel.this.getPrevList(CommentsViewerViewModel.this.rootList)));
                return;
            }
            List<Comment> comments = result.getComments();
            if (CommentsViewerViewModel.this.rootCursor == null) {
                CommentsViewerViewModel.this.rootCount.postValue(result.getCommentCount());
            }
            if (CommentsViewerViewModel.this.rootCursor != null) {
                comments = CommentsViewerViewModel.this.mergeList(CommentsViewerViewModel.this.rootList, comments);
            }
            CommentsViewerViewModel.this.rootCursor = result.getNextMinId();
            CommentsViewerViewModel.this.rootHasNext = !TextUtils.isEmpty(CommentsViewerViewModel.this.rootCursor);
            CommentsViewerViewModel.this.rootList.postValue(Resource.success(comments));
        }

        @Override
        public void onFailure(Throwable t) {
            Log.e(CommentsViewerViewModel.TAG, "onFailure: ", t);
            CommentsViewerViewModel.this.rootList.postValue(Resource.error(t.getMessage(), CommentsViewerViewModel.this.getPrevList(CommentsViewerViewModel.this.rootList)));
        }
    };
    private final ServiceCallback<ChildCommentsFetchResponse> rcb = new ServiceCallback<ChildCommentsFetchResponse>() {
        @Override
        public void onSuccess(ChildCommentsFetchResponse result) {
            // Log.d(TAG, "onSuccess: " + result);
            if (result == null) {
                CommentsViewerViewModel.this.rootList.postValue(Resource.error(R.string.generic_null_response, CommentsViewerViewModel.this.getPrevList(CommentsViewerViewModel.this.replyList)));
                return;
            }
            List<Comment> comments = result.getChildComments();
            // Replies
            if (CommentsViewerViewModel.this.repliesCursor == null) {
                // add parent to top of replies
                comments = ImmutableList.<Comment>builder()
                        .add(CommentsViewerViewModel.this.repliesParent)
                        .addAll(comments)
                        .build();
            }
            if (CommentsViewerViewModel.this.repliesCursor != null) {
                comments = CommentsViewerViewModel.this.mergeList(CommentsViewerViewModel.this.replyList, comments);
            }
            CommentsViewerViewModel.this.repliesCursor = result.getNextMaxChildCursor();
            CommentsViewerViewModel.this.repliesHasNext = result.getHasMoreTailChildComments();
            CommentsViewerViewModel.this.replyList.postValue(Resource.success(comments));
        }

        @Override
        public void onFailure(Throwable t) {
            Log.e(CommentsViewerViewModel.TAG, "onFailure: ", t);
            CommentsViewerViewModel.this.replyList.postValue(Resource.error(t.getMessage(), CommentsViewerViewModel.this.getPrevList(CommentsViewerViewModel.this.replyList)));
        }
    };

    public CommentsViewerViewModel() {
        this.graphQLRepository = GraphQLRepository.Companion.getInstance();
        String cookie = settingsHelper.getString(Constants.COOKIE);
        String deviceUuid = settingsHelper.getString(Constants.DEVICE_UUID);
        String csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        long userIdFromCookie = CookieUtils.getUserIdFromCookie(cookie);
        this.commentService = CommentService.getInstance(deviceUuid, csrfToken, userIdFromCookie);
    }

    public void setCurrentUser(User currentUser) {
        this.currentUserId.postValue(currentUser == null ? 0 : currentUser.getPk());
    }

    public void setPostDetails(String shortCode, String postId, long postUserId) {
        this.shortCode = shortCode;
        this.postId = postId;
    }

    public LiveData<Long> getCurrentUserId() {
        return this.currentUserId;
    }

    @Nullable
    public Comment getRepliesParent() {
        return this.repliesParent;
    }

    @Nullable
    public void setReplyTo(Comment replyTo) {
        this.replyTo = replyTo;
    }

    public LiveData<Resource<List<Comment>>> getRootList() {
        return this.rootList;
    }

    public LiveData<Resource<List<Comment>>> getReplyList() {
        return this.replyList;
    }

    public LiveData<Integer> getRootCommentsCount() {
        return this.rootCount;
    }

    public void fetchComments() {
        if (this.shortCode == null || this.postId == null) return;
        if (!this.rootHasNext) return;
        this.rootList.postValue(Resource.loading(this.getPrevList(this.rootList)));
        if (this.currentUserId.getValue() != 0L) {
            this.commentService.fetchComments(this.postId, this.rootCursor, this.ccb);
            return;
        }
        this.graphQLRepository.fetchComments(
                this.shortCode,
                true,
                this.rootCursor,
                this.enqueueRequest(true, this.shortCode, this.ccb)
        );
    }

    public void fetchReplies() {
        if (this.repliesParent == null) return;
        this.fetchReplies(this.repliesParent.getPk());
    }

    public void fetchReplies(@NonNull String commentId) {
        if (!this.repliesHasNext) return;
        List<Comment> list;
        if (this.repliesParent != null && !Objects.equals(this.repliesParent.getPk(), commentId)) {
            this.repliesCursor = null;
            this.repliesHasNext = false;
            list = Collections.emptyList();
        } else {
            list = this.getPrevList(this.replyList);
        }
        this.replyList.postValue(Resource.loading(list));
        if (this.currentUserId.getValue() != 0L) {
            this.commentService.fetchChildComments(this.postId, commentId, this.repliesCursor, this.rcb);
            return;
        }
        this.graphQLRepository.fetchComments(commentId, false, this.repliesCursor, this.enqueueRequest(false, commentId, this.rcb));
    }

    private Continuation<String> enqueueRequest(boolean root,
                                                String shortCodeOrCommentId,
                                                @SuppressWarnings("rawtypes") ServiceCallback callback) {
        return CoroutineUtilsKt.getContinuation((response, throwable) -> {
            if (throwable != null) {
                callback.onFailure(throwable);
                return;
            }
            if (response == null) {
                Log.e(CommentsViewerViewModel.TAG, "Error occurred while fetching gql comments of " + shortCodeOrCommentId);
                //noinspection unchecked
                callback.onSuccess(null);
                return;
            }
            try {
                JSONObject body = root ? new JSONObject(response).getJSONObject("data")
                                                                       .getJSONObject("shortcode_media")
                                                                       .getJSONObject("edge_media_to_parent_comment")
                                             : new JSONObject(response).getJSONObject("data")
                                                                       .getJSONObject("comment")
                                                                       .getJSONObject("edge_threaded_comments");
                int count = body.optInt("count");
                JSONObject pageInfo = body.getJSONObject("page_info");
                boolean hasNextPage = pageInfo.getBoolean("has_next_page");
                String endCursor = pageInfo.isNull("end_cursor") || !hasNextPage ? null : pageInfo.optString("end_cursor");
                JSONArray commentsJsonArray = body.getJSONArray("edges");
                ImmutableList.Builder<Comment> builder = ImmutableList.builder();
                for (int i = 0; i < commentsJsonArray.length(); i++) {
                    Comment commentModel = this.getComment(commentsJsonArray.getJSONObject(i).getJSONObject("node"), root);
                    builder.add(commentModel);
                }
                Object result = root ? new CommentsFetchResponse(count, endCursor, builder.build())
                                           : new ChildCommentsFetchResponse(count, endCursor, builder.build(), hasNextPage);
                //noinspection unchecked
                callback.onSuccess(result);
            } catch (final Exception e) {
                Log.e(CommentsViewerViewModel.TAG, "onResponse", e);
                callback.onFailure(e);
            }
        }, Dispatchers.getIO());
    }

    @NonNull
    private Comment getComment(@NonNull JSONObject commentJsonObject, boolean root) throws JSONException {
        JSONObject owner = commentJsonObject.getJSONObject("owner");
        User user = new User(
                owner.optLong(Constants.EXTRAS_ID, 0),
                owner.getString(Constants.EXTRAS_USERNAME),
                null,
                false,
                owner.getString("profile_pic_url"),
                owner.optBoolean("is_verified"));
        JSONObject likedBy = commentJsonObject.optJSONObject("edge_liked_by");
        String commentId = commentJsonObject.getString("id");
        JSONObject childCommentsJsonObject = commentJsonObject.optJSONObject("edge_threaded_comments");
        int replyCount = 0;
        if (childCommentsJsonObject != null) {
            replyCount = childCommentsJsonObject.optInt("count");
        }
        return new Comment(commentId,
                           commentJsonObject.getString("text"),
                           commentJsonObject.getLong("created_at"),
                           likedBy != null ? likedBy.optLong("count", 0) : 0,
                           commentJsonObject.getBoolean("viewer_has_liked"),
                           user,
                           replyCount);
    }

    @NonNull
    private List<Comment> getPrevList(@NonNull LiveData<Resource<List<Comment>>> list) {
        if (list.getValue() == null) return Collections.emptyList();
        Resource<List<Comment>> listResource = list.getValue();
        if (listResource.data == null) return Collections.emptyList();
        return listResource.data;
    }

    private List<Comment> mergeList(@NonNull LiveData<Resource<List<Comment>>> list,
                                    List<Comment> comments) {
        List<Comment> prevList = this.getPrevList(list);
        if (comments == null) {
            return prevList;
        }
        return ImmutableList.<Comment>builder()
                .addAll(prevList)
                .addAll(comments)
                .build();
    }

    public void showReplies(Comment comment) {
        if (comment == null) return;
        if (this.repliesParent == null || !Objects.equals(this.repliesParent.getPk(), comment.getPk())) {
            this.repliesParent = comment;
            this.replyTo = comment;
            this.prevReplies = null;
            this.prevRepliesCursor = null;
            this.prevRepliesHasNext = true;
            this.fetchReplies(comment.getPk());
            return;
        }
        if (this.prevReplies != null && !this.prevReplies.isEmpty()) {
            // user clicked same comment, show prev loaded replies
            this.repliesCursor = this.prevRepliesCursor;
            this.repliesHasNext = this.prevRepliesHasNext;
            this.replyList.postValue(Resource.success(this.prevReplies));
            return;
        }
        // prev list was null or empty, fetch
        this.prevRepliesCursor = null;
        this.prevRepliesHasNext = true;
        this.fetchReplies(comment.getPk());
    }

    public LiveData<Resource<Object>> likeComment(@NonNull Comment comment, boolean liked, boolean isReply) {
        MutableLiveData<Resource<Object>> data = new MutableLiveData<>(Resource.loading(null));
        ServiceCallback<Boolean> callback = new ServiceCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result == null || !result) {
                    data.postValue(Resource.error(R.string.downloader_unknown_error, null));
                    return;
                }
                data.postValue(Resource.success(new Object()));
                CommentsViewerViewModel.this.setLiked(isReply, comment, liked);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(CommentsViewerViewModel.TAG, "Error liking comment", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        };
        if (liked) {
            this.commentService.commentLike(comment.getPk(), callback);
        } else {
            this.commentService.commentUnlike(comment.getPk(), callback);
        }
        return data;
    }

    private void setLiked(boolean isReply,
                          @NonNull Comment comment,
                          boolean liked) {
        List<Comment> list = this.getPrevList(isReply ? this.replyList : this.rootList);
        if (list == null) return;
        List<Comment> copy = new ArrayList<>(list);
        final OptionalInt indexOpt = IntStream.range(0, copy.size())
                                        .filter(i -> copy.get(i) != null && Objects.equals(copy.get(i).getPk(), comment.getPk()))
                                        .findFirst();
        if (!indexOpt.isPresent()) return;
        try {
            Comment clone = (Comment) comment.clone();
            clone.setLiked(liked);
            copy.set(indexOpt.getAsInt(), clone);
            MutableLiveData<Resource<List<Comment>>> liveData = isReply ? this.replyList : this.rootList;
            liveData.postValue(Resource.success(copy));
        } catch (final Exception e) {
            Log.e(CommentsViewerViewModel.TAG, "setLiked: ", e);
        }
    }

    public LiveData<Resource<Object>> comment(@NonNull String text,
                                              boolean isReply) {
        MutableLiveData<Resource<Object>> data = new MutableLiveData<>(Resource.loading(null));
        String replyToId = null;
        if (isReply && this.replyTo != null) {
            replyToId = this.replyTo.getPk();
        }
        if (isReply && replyToId == null) {
            data.postValue(Resource.error(null, null));
            return data;
        }
        this.commentService.comment(this.postId, text, replyToId, new ServiceCallback<Comment>() {
            @Override
            public void onSuccess(Comment result) {
                if (result == null) {
                    data.postValue(Resource.error(R.string.downloader_unknown_error, null));
                    return;
                }
                CommentsViewerViewModel.this.addComment(result, isReply);
                data.postValue(Resource.success(new Object()));
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(CommentsViewerViewModel.TAG, "Error during comment", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
        return data;
    }

    private void addComment(@NonNull Comment comment, boolean isReply) {
        List<Comment> list = this.getPrevList(isReply ? this.replyList : this.rootList);
        ImmutableList.Builder<Comment> builder = ImmutableList.builder();
        if (isReply) {
            // replies are added to the bottom of the list to preserve chronological order
            builder.addAll(list)
                   .add(comment);
        } else {
            builder.add(comment)
                   .addAll(list);
        }
        MutableLiveData<Resource<List<Comment>>> liveData = isReply ? this.replyList : this.rootList;
        liveData.postValue(Resource.success(builder.build()));
    }

    public void translate(@NonNull Comment comment,
                          @NonNull ServiceCallback<String> callback) {
        this.commentService.translate(comment.getPk(), callback);
    }

    public LiveData<Resource<Object>> deleteComment(@NonNull Comment comment, boolean isReply) {
        MutableLiveData<Resource<Object>> data = new MutableLiveData<>(Resource.loading(null));
        this.commentService.deleteComment(this.postId, comment.getPk(), new ServiceCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result == null || !result) {
                    data.postValue(Resource.error(R.string.downloader_unknown_error, null));
                    return;
                }
                CommentsViewerViewModel.this.removeComment(comment, isReply);
                data.postValue(Resource.success(new Object()));
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(CommentsViewerViewModel.TAG, "Error deleting comment", t);
                data.postValue(Resource.error(t.getMessage(), null));
            }
        });
        return data;
    }

    private void removeComment(@NonNull Comment comment, boolean isReply) {
        List<Comment> list = this.getPrevList(isReply ? this.replyList : this.rootList);
        List<Comment> updated = list.stream()
                                          .filter(Objects::nonNull)
                                          .filter(c -> !Objects.equals(c.getPk(), comment.getPk()))
                                          .collect(Collectors.toList());
        MutableLiveData<Resource<List<Comment>>> liveData = isReply ? this.replyList : this.rootList;
        liveData.postValue(Resource.success(updated));
    }

    public void clearReplies() {
        this.prevRepliesCursor = this.repliesCursor;
        this.prevRepliesHasNext = this.repliesHasNext;
        this.repliesCursor = null;
        this.repliesHasNext = true;
        // cache prev reply list to save time and data if user clicks same comment again
        this.prevReplies = this.getPrevList(this.replyList);
        this.replyList.postValue(Resource.success(Collections.emptyList()));
    }
}

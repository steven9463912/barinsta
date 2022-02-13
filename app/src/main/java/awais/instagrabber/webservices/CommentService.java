package awais.instagrabber.webservices;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import awais.instagrabber.models.Comment;
import awais.instagrabber.repositories.CommentRepository;
import awais.instagrabber.repositories.responses.ChildCommentsFetchResponse;
import awais.instagrabber.repositories.responses.CommentsFetchResponse;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentService {
    private static final String TAG = "CommentService";

    private final CommentRepository repository;
    private final String deviceUuid, csrfToken;
    private final long userId;

    private static CommentService instance;

    private CommentService(String deviceUuid,
                           String csrfToken,
                           long userId) {
        this.deviceUuid = deviceUuid;
        this.csrfToken = csrfToken;
        this.userId = userId;
        this.repository = RetrofitFactory.INSTANCE
                .getRetrofit()
                .create(CommentRepository.class);
    }

    public String getCsrfToken() {
        return this.csrfToken;
    }

    public String getDeviceUuid() {
        return this.deviceUuid;
    }

    public long getUserId() {
        return this.userId;
    }

    public static CommentService getInstance(String deviceUuid, String csrfToken, long userId) {
        if (CommentService.instance == null
                || !Objects.equals(CommentService.instance.getCsrfToken(), csrfToken)
                || !Objects.equals(CommentService.instance.getDeviceUuid(), deviceUuid)
                || !Objects.equals(CommentService.instance.getUserId(), userId)) {
            CommentService.instance = new CommentService(deviceUuid, csrfToken, userId);
        }
        return CommentService.instance;
    }

    public void fetchComments(@NonNull String mediaId,
                              String minId,
                              @NonNull ServiceCallback<CommentsFetchResponse> callback) {
        Map<String, String> form = new HashMap<>();
        form.put("can_support_threading", "true");
        if (minId != null) form.put("min_id", minId);
        Call<CommentsFetchResponse> request = this.repository.fetchComments(mediaId, form);
        request.enqueue(new Callback<CommentsFetchResponse>() {
            @Override
            public void onResponse(@NonNull Call<CommentsFetchResponse> call, @NonNull Response<CommentsFetchResponse> response) {
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<CommentsFetchResponse> call, @NonNull Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    public void fetchChildComments(@NonNull String mediaId,
                                   @NonNull String commentId,
                                   String maxId,
                                   @NonNull ServiceCallback<ChildCommentsFetchResponse> callback) {
        Map<String, String> form = new HashMap<>();
        if (maxId != null) form.put("max_id", maxId);
        Call<ChildCommentsFetchResponse> request = this.repository.fetchChildComments(mediaId, commentId, form);
        request.enqueue(new Callback<ChildCommentsFetchResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChildCommentsFetchResponse> call, @NonNull Response<ChildCommentsFetchResponse> response) {
                ChildCommentsFetchResponse cfr = response.body();
                if (cfr == null) callback.onFailure(new Exception("response is empty"));
                callback.onSuccess(cfr);
            }

            @Override
            public void onFailure(@NonNull Call<ChildCommentsFetchResponse> call, @NonNull Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    public void comment(@NonNull String mediaId,
                        @NonNull String comment,
                        String replyToCommentId,
                        @NonNull ServiceCallback<Comment> callback) {
        final String module = "self_comments_v2";
        Map<String, Object> form = new HashMap<>();
        // form.put("user_breadcrumb", userBreadcrumb(comment.length()));
        form.put("idempotence_token", UUID.randomUUID().toString());
        form.put("_csrftoken", this.csrfToken);
        form.put("_uid", this.userId);
        form.put("_uuid", this.deviceUuid);
        form.put("comment_text", comment);
        form.put("containermodule", module);
        if (!TextUtils.isEmpty(replyToCommentId)) {
            form.put("replied_to_comment_id", replyToCommentId);
        }
        Map<String, String> signedForm = Utils.sign(form);
        Call<String> commentRequest = this.repository.comment(mediaId, signedForm);
        commentRequest.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                String body = response.body();
                if (body == null) {
                    Log.e(CommentService.TAG, "Error occurred while creating comment");
                    callback.onSuccess(null);
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject(body);
                    // final String status = jsonObject.optString("status");
                    JSONObject commentJsonObject = jsonObject.optJSONObject("comment");
                    Comment comment = null;
                    if (commentJsonObject != null) {
                        JSONObject userJsonObject = commentJsonObject.optJSONObject("user");
                        if (userJsonObject != null) {
                            Gson gson = new Gson();
                            User user = gson.fromJson(userJsonObject.toString(), User.class);
                            comment = new Comment(
                                    commentJsonObject.optString("pk"),
                                    commentJsonObject.optString("text"),
                                    commentJsonObject.optLong("created_at"),
                                    0L,
                                    false,
                                    user,
                                    0
                            );
                        }
                    }
                    callback.onSuccess(comment);
                } catch (final Exception e) {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    public void deleteComment(String mediaId,
                              String commentId,
                              @NonNull ServiceCallback<Boolean> callback) {
        this.deleteComments(mediaId, Collections.singletonList(commentId), callback);
    }

    public void deleteComments(String mediaId,
                               List<String> commentIds,
                               @NonNull ServiceCallback<Boolean> callback) {
        Map<String, Object> form = new HashMap<>();
        form.put("comment_ids_to_delete", android.text.TextUtils.join(",", commentIds));
        form.put("_csrftoken", this.csrfToken);
        form.put("_uid", this.userId);
        form.put("_uuid", this.deviceUuid);
        Map<String, String> signedForm = Utils.sign(form);
        Call<String> bulkDeleteRequest = this.repository.commentsBulkDelete(mediaId, signedForm);
        bulkDeleteRequest.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                String body = response.body();
                if (body == null) {
                    Log.e(CommentService.TAG, "Error occurred while deleting comments");
                    callback.onSuccess(false);
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject(body);
                    String status = jsonObject.optString("status");
                    callback.onSuccess(status.equals("ok"));
                } catch (final JSONException e) {
                    // Log.e(TAG, "Error parsing body", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                // Log.e(TAG, "Error deleting comments", t);
                callback.onFailure(t);
            }
        });
    }

    public void commentLike(@NonNull String commentId,
                            @NonNull ServiceCallback<Boolean> callback) {
        Map<String, Object> form = new HashMap<>();
        form.put("_csrftoken", this.csrfToken);
        // form.put("_uid", userId);
        // form.put("_uuid", deviceUuid);
        Map<String, String> signedForm = Utils.sign(form);
        Call<String> commentLikeRequest = this.repository.commentLike(commentId, signedForm);
        commentLikeRequest.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                String body = response.body();
                if (body == null) {
                    Log.e(CommentService.TAG, "Error occurred while liking comment");
                    callback.onSuccess(false);
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject(body);
                    String status = jsonObject.optString("status");
                    callback.onSuccess(status.equals("ok"));
                } catch (final JSONException e) {
                    // Log.e(TAG, "Error parsing body", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.e(CommentService.TAG, "Error liking comment", t);
                callback.onFailure(t);
            }
        });
    }

    public void commentUnlike(String commentId,
                              @NonNull ServiceCallback<Boolean> callback) {
        Map<String, Object> form = new HashMap<>();
        form.put("_csrftoken", this.csrfToken);
        // form.put("_uid", userId);
        // form.put("_uuid", deviceUuid);
        Map<String, String> signedForm = Utils.sign(form);
        Call<String> commentUnlikeRequest = this.repository.commentUnlike(commentId, signedForm);
        commentUnlikeRequest.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                String body = response.body();
                if (body == null) {
                    Log.e(CommentService.TAG, "Error occurred while unliking comment");
                    callback.onSuccess(false);
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject(body);
                    String status = jsonObject.optString("status");
                    callback.onSuccess(status.equals("ok"));
                } catch (final JSONException e) {
                    // Log.e(TAG, "Error parsing body", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.e(CommentService.TAG, "Error unliking comment", t);
                callback.onFailure(t);
            }
        });
    }

    public void translate(String id,
                          @NonNull ServiceCallback<String> callback) {
        Map<String, String> form = new HashMap<>();
        form.put("id", String.valueOf(id));
        form.put("type", "2");
        Call<String> request = this.repository.translate(form);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                String body = response.body();
                if (body == null) {
                    Log.e(CommentService.TAG, "Error occurred while translating");
                    callback.onSuccess(null);
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject(body);
                    String translation = jsonObject.optString("translation");
                    callback.onSuccess(translation);
                } catch (final JSONException e) {
                    // Log.e(TAG, "Error parsing body", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.e(CommentService.TAG, "Error translating", t);
                callback.onFailure(t);
            }
        });
    }
}
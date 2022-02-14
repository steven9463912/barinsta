package awais.instagrabber.webservices;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import awais.instagrabber.repositories.CollectionRepository;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.utils.Utils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CollectionService {
    private static final String TAG = "CollectionService";

    private final CollectionRepository repository;
    private final String deviceUuid, csrfToken;
    private final long userId;

    private static CollectionService instance;

    private CollectionService(String deviceUuid,
                              String csrfToken,
                              long userId) {
        this.deviceUuid = deviceUuid;
        this.csrfToken = csrfToken;
        this.userId = userId;
        this.repository = RetrofitFactory.INSTANCE
                                    .getRetrofit()
                                    .create(CollectionRepository.class);
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

    public static CollectionService getInstance(String deviceUuid, String csrfToken, long userId) {
        if (CollectionService.instance == null
                || !Objects.equals(CollectionService.instance.getCsrfToken(), csrfToken)
                || !Objects.equals(CollectionService.instance.getDeviceUuid(), deviceUuid)
                || !Objects.equals(CollectionService.instance.getUserId(), userId)) {
            CollectionService.instance = new CollectionService(deviceUuid, csrfToken, userId);
        }
        return CollectionService.instance;
    }

    public void addPostsToCollection(String collectionId,
                                     List<Media> posts,
                                     ServiceCallback<String> callback) {
        Map<String, Object> form = new HashMap<>(2);
        form.put("module_name", "feed_saved_add_to_collection");
        List<String> ids;
        ids = posts.stream()
                   .map(Media::getPk)
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
        form.put("added_media_ids", "[" + TextUtils.join(",", ids) + "]");
        this.changeCollection(collectionId, "edit", form, callback);
    }

    public void editCollectionName(String collectionId,
                                   String name,
                                   ServiceCallback<String> callback) {
        Map<String, Object> form = new HashMap<>(1);
        form.put("name", name);
        this.changeCollection(collectionId, "edit", form, callback);
    }

    public void deleteCollection(String collectionId,
                                 ServiceCallback<String> callback) {
        this.changeCollection(collectionId, "delete", null, callback);
    }

    public void changeCollection(String collectionId,
                                 String action,
                                 Map<String, Object> options,
                                 ServiceCallback<String> callback) {
        Map<String, Object> form = new HashMap<>();
        form.put("_csrftoken", this.csrfToken);
        form.put("_uuid", this.deviceUuid);
        form.put("_uid", this.userId);
        if (options != null) form.putAll(options);
        Map<String, String> signedForm = Utils.sign(form);
        Call<String> request = this.repository.changeCollection(collectionId, action, signedForm);
        request.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (callback == null) return;
                String collectionsListResponse = response.body();
                if (collectionsListResponse == null) {
                    callback.onSuccess(null);
                    return;
                }
                callback.onSuccess(collectionsListResponse);
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                if (callback != null) {
                    callback.onFailure(t);
                }
            }
        });
    }
}

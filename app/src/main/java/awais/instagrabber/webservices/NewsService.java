package awais.instagrabber.webservices;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import awais.instagrabber.repositories.NewsRepository;
import awais.instagrabber.repositories.responses.AymlResponse;
import awais.instagrabber.repositories.responses.AymlUser;
import awais.instagrabber.repositories.responses.NewsInboxResponse;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.UserSearchResponse;
import awais.instagrabber.repositories.responses.notification.Notification;
import awais.instagrabber.repositories.responses.notification.NotificationArgs;
import awais.instagrabber.repositories.responses.notification.NotificationCounts;
import awais.instagrabber.utils.Constants;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsService {
    private static final String TAG = "NewsService";

    private final NewsRepository repository;

    private static NewsService instance;

    private NewsService() {
        this.repository = RetrofitFactory.INSTANCE
                                    .getRetrofit()
                                    .create(NewsRepository.class);
    }

    public static NewsService getInstance() {
        if (NewsService.instance == null) {
            NewsService.instance = new NewsService();
        }
        return NewsService.instance;
    }

    public void fetchAppInbox(boolean markAsSeen,
                              ServiceCallback<List<Notification>> callback) {
        Call<NewsInboxResponse> request = this.repository.appInbox(markAsSeen, Constants.X_IG_APP_ID);
        request.enqueue(new Callback<NewsInboxResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsInboxResponse> call, @NonNull Response<NewsInboxResponse> response) {
                NewsInboxResponse body = response.body();
                if (body == null) {
                    callback.onSuccess(null);
                    return;
                }
                List<Notification> result = new ArrayList<Notification>();
                List<Notification> newStories = body.getNewStories();
                if (newStories != null) result.addAll(newStories);
                List<Notification> oldStories = body.getOldStories();
                if (oldStories != null) result.addAll(oldStories);
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(@NonNull Call<NewsInboxResponse> call, @NonNull Throwable t) {
                callback.onFailure(t);
                // Log.e(TAG, "onFailure: ", t);
            }
        });
    }

    public void fetchActivityCounts(ServiceCallback<NotificationCounts> callback) {
        Call<NewsInboxResponse> request = this.repository.appInbox(false, null);
        request.enqueue(new Callback<NewsInboxResponse>() {
            @Override
            public void onResponse(@NonNull Call<NewsInboxResponse> call, @NonNull Response<NewsInboxResponse> response) {
                NewsInboxResponse body = response.body();
                if (body == null) {
                    callback.onSuccess(null);
                    return;
                }
                callback.onSuccess(body.getCounts());
            }

            @Override
            public void onFailure(@NonNull Call<NewsInboxResponse> call, @NonNull Throwable t) {
                callback.onFailure(t);
                // Log.e(TAG, "onFailure: ", t);
            }
        });
    }

    public void fetchSuggestions(String csrfToken,
                                 String deviceUuid,
                                 ServiceCallback<List<Notification>> callback) {
        Map<String, String> form = new HashMap<>();
        form.put("_uuid", UUID.randomUUID().toString());
        form.put("_csrftoken", csrfToken);
        form.put("phone_id", UUID.randomUUID().toString());
        form.put("device_id", UUID.randomUUID().toString());
        form.put("module", "discover_people");
        form.put("paginate", "false");
        Call<AymlResponse> request = this.repository.getAyml(form);
        request.enqueue(new Callback<AymlResponse>() {
            @Override
            public void onResponse(@NonNull Call<AymlResponse> call, @NonNull Response<AymlResponse> response) {
                AymlResponse body = response.body();
                if (body == null) {
                    callback.onSuccess(null);
                    return;
                }
                List<AymlUser> aymlUsers = new ArrayList<AymlUser>();
                List<AymlUser> newSuggestions = body.getNewSuggestedUsers().getSuggestions();
                if (newSuggestions != null) {
                    aymlUsers.addAll(newSuggestions);
                }
                List<AymlUser> oldSuggestions = body.getSuggestedUsers().getSuggestions();
                if (oldSuggestions != null) {
                    aymlUsers.addAll(oldSuggestions);
                }

                List<Notification> newsItems = aymlUsers
                        .stream()
                        .map(i -> {
                            User u = i.getUser();
                            return new Notification(
                                    new NotificationArgs(
                                            i.getSocialContext(),
                                            i.getAlgorithm(),
                                            u.getPk(),
                                            u.getProfilePicUrl(),
                                            null,
                                            0L,
                                            u.getUsername(),
                                            u.getFullName(),
                                            u.isVerified()
                                    ),
                                    9999,
                                    String.valueOf(u.getPk()) // placeholder
                            );
                        })
                        .collect(Collectors.toList());
                callback.onSuccess(newsItems);
            }

            @Override
            public void onFailure(@NonNull Call<AymlResponse> call, @NonNull Throwable t) {
                callback.onFailure(t);
                // Log.e(TAG, "onFailure: ", t);
            }
        });
    }

    public void fetchChaining(long targetId, ServiceCallback<List<Notification>> callback) {
        Call<UserSearchResponse> request = this.repository.getChaining(targetId);
        request.enqueue(new Callback<UserSearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserSearchResponse> call, @NonNull Response<UserSearchResponse> response) {
                UserSearchResponse body = response.body();
                if (body == null) {
                    callback.onSuccess(null);
                    return;
                }

                List<Notification> newsItems = body
                        .getUsers()
                        .stream()
                        .map(u -> {
                            return new Notification(
                                    new NotificationArgs(
                                            u.getSocialContext(),
                                            null,
                                            u.getPk(),
                                            u.getProfilePicUrl(),
                                            null,
                                            0L,
                                            u.getUsername(),
                                            u.getFullName(),
                                            u.isVerified()
                                    ),
                                    9999,
                                    String.valueOf(u.getPk()) // placeholder
                            );
                        })
                        .collect(Collectors.toList());
                callback.onSuccess(newsItems);
            }

            @Override
            public void onFailure(@NonNull Call<UserSearchResponse> call, @NonNull Throwable t) {
                callback.onFailure(t);
                // Log.e(TAG, "onFailure: ", t);
            }
        });
    }
}

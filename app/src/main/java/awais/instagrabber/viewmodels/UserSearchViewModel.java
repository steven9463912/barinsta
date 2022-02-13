package awais.instagrabber.viewmodels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import awais.instagrabber.R;
import awais.instagrabber.fragments.UserSearchMode;
import awais.instagrabber.models.Resource;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.repositories.responses.directmessages.RankedRecipient;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.Debouncer;
import awais.instagrabber.utils.RankedRecipientsCache;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.webservices.DirectMessagesRepository;
import awais.instagrabber.webservices.UserRepository;
import kotlinx.coroutines.Dispatchers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class UserSearchViewModel extends ViewModel {
    private static final String TAG = UserSearchViewModel.class.getSimpleName();
    public static final String DEBOUNCE_KEY = "search";

    private String prevQuery;
    private String currentQuery;
    private Call<?> searchRequest;
    private long[] hideUserIds;
    private String[] hideThreadIds;
    private UserSearchMode searchMode;
    private boolean showGroups;
    private boolean waitingForCache;
    private boolean showCachedResults;

    private final MutableLiveData<Resource<List<RankedRecipient>>> recipients = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showAction = new MutableLiveData<>(false);
    private final Debouncer<String> searchDebouncer;
    private final Set<RankedRecipient> selectedRecipients = new HashSet<>();
    private final UserRepository userRepository;
    private final DirectMessagesRepository directMessagesRepository;
    private final RankedRecipientsCache rankedRecipientsCache;

    public UserSearchViewModel() {
        String cookie = settingsHelper.getString(Constants.COOKIE);
        String csrfToken = CookieUtils.getCsrfTokenFromCookie(cookie);
        long viewerId = CookieUtils.getUserIdFromCookie(cookie);
        String deviceUuid = settingsHelper.getString(Constants.DEVICE_UUID);
        if (TextUtils.isEmpty(csrfToken) || viewerId <= 0 || TextUtils.isEmpty(deviceUuid)) {
            throw new IllegalArgumentException("User is not logged in!");
        }
        this.userRepository = UserRepository.Companion.getInstance();
        this.directMessagesRepository = DirectMessagesRepository.Companion.getInstance();
        this.rankedRecipientsCache = RankedRecipientsCache.INSTANCE;
        if ((this.rankedRecipientsCache.isFailed() || this.rankedRecipientsCache.isExpired()) && !this.rankedRecipientsCache.isUpdateInitiated()) {
            this.updateRankedRecipientCache();
        }
        Debouncer.Callback<String> searchCallback = new Debouncer.Callback<String>() {
            @Override
            public void call(String key) {
                if (UserSearchViewModel.this.currentQuery != null && UserSearchViewModel.this.currentQuery.equalsIgnoreCase(UserSearchViewModel.this.prevQuery)) return;
                UserSearchViewModel.this.sendSearchRequest();
                UserSearchViewModel.this.prevQuery = UserSearchViewModel.this.currentQuery;
            }

            @Override
            public void onError(Throwable t) {
                Log.e(UserSearchViewModel.TAG, "onError: ", t);
            }
        };
        this.searchDebouncer = new Debouncer<>(searchCallback, 1000);
    }

    private void updateRankedRecipientCache() {
        this.rankedRecipientsCache.setUpdateInitiated(true);
        this.directMessagesRepository.rankedRecipients(
                null,
                null,
                null,
                CoroutineUtilsKt.getContinuation((response, throwable) -> {
                    if (throwable != null) {
                        Log.e(UserSearchViewModel.TAG, "updateRankedRecipientCache: ", throwable);
                        this.rankedRecipientsCache.setUpdateInitiated(false);
                        this.rankedRecipientsCache.setFailed(true);
                        this.continueSearchIfRequired();
                        return;
                    }
                    this.rankedRecipientsCache.setResponse(response);
                    this.rankedRecipientsCache.setUpdateInitiated(false);
                    this.continueSearchIfRequired();
                }, Dispatchers.getIO())
        );
    }

    private void continueSearchIfRequired() {
        if (!this.waitingForCache) {
            if (this.showCachedResults) {
                this.recipients.postValue(Resource.success(this.getCachedRecipients()));
            }
            return;
        }
        this.waitingForCache = false;
        this.sendSearchRequest();
    }

    public LiveData<Resource<List<RankedRecipient>>> getRecipients() {
        return this.recipients;
    }

    public void search(@Nullable String query) {
        this.currentQuery = query;
        if (TextUtils.isEmpty(query)) {
            this.cancelSearch();
            if (this.showCachedResults) {
                this.recipients.postValue(Resource.success(this.getCachedRecipients()));
            }
            return;
        }
        this.recipients.postValue(Resource.loading(this.getCachedRecipients()));
        this.searchDebouncer.call(UserSearchViewModel.DEBOUNCE_KEY);
    }

    private void sendSearchRequest() {
        if (!this.rankedRecipientsCache.isFailed()) { // to avoid infinite loop in case of any network issues
            if (this.rankedRecipientsCache.isUpdateInitiated()) {
                // wait for cache first
                this.waitingForCache = true;
                return;
            }
            if (this.rankedRecipientsCache.isExpired()) {
                // update cache first
                this.updateRankedRecipientCache();
                this.waitingForCache = true;
                return;
            }
        }
        switch (this.searchMode) {
            case RAVEN:
            case RESHARE:
                this.rankedRecipientSearch();
                break;
            case USER_SEARCH:
            default:
                this.defaultUserSearch();
                break;
        }
    }

    private void defaultUserSearch() {
        this.userRepository.search(this.currentQuery, CoroutineUtilsKt.getContinuation((userSearchResponse, throwable) -> {
            if (throwable != null) {
                Log.e(UserSearchViewModel.TAG, "onFailure: ", throwable);
                this.recipients.postValue(Resource.error(throwable.getMessage(), this.getCachedRecipients()));
                this.searchRequest = null;
                return;
            }
            if (userSearchResponse == null) {
                this.recipients.postValue(Resource.error(R.string.generic_null_response, this.getCachedRecipients()));
                this.searchRequest = null;
                return;
            }
            List<RankedRecipient> list = userSearchResponse
                    .getUsers()
                    .stream()
                    .map(RankedRecipient::of)
                    .collect(Collectors.toList());
            this.recipients.postValue(Resource.success(this.mergeResponseWithCache(list)));
            this.searchRequest = null;
        }));
    }

    private void rankedRecipientSearch() {
        this.directMessagesRepository.rankedRecipients(
                this.searchMode.getMode(),
                this.showGroups,
                this.currentQuery,
                CoroutineUtilsKt.getContinuation((response, throwable) -> {
                    if (throwable != null) {
                        Log.e(UserSearchViewModel.TAG, "rankedRecipientSearch: ", throwable);
                        this.recipients.postValue(Resource.error(throwable.getMessage(), this.getCachedRecipients()));
                        return;
                    }
                    List<RankedRecipient> list = response.getRankedRecipients();
                    if (list != null) {
                        this.recipients.postValue(Resource.success(this.mergeResponseWithCache(list)));
                    }
                }, Dispatchers.getIO())
        );
    }

    private List<RankedRecipient> mergeResponseWithCache(@NonNull List<RankedRecipient> list) {
        Iterator<RankedRecipient> iterator = list.stream()
                                                       .filter(Objects::nonNull)
                                                       .filter(this::filterValidRecipients)
                                                       .filter(this::filterOutGroups)
                                                       .filter(this::filterIdsToHide)
                                                       .iterator();
        return ImmutableList.<RankedRecipient>builder()
                .addAll(this.getCachedRecipients()) // add cached results first
                .addAll(iterator)
                .build();
    }

    @NonNull
    private List<RankedRecipient> getCachedRecipients() {
        List<RankedRecipient> rankedRecipients = this.rankedRecipientsCache.getRankedRecipients();
        List<RankedRecipient> list = rankedRecipients != null ? rankedRecipients : Collections.emptyList();
        return list.stream()
                   .filter(Objects::nonNull)
                   .filter(this::filterValidRecipients)
                   .filter(this::filterOutGroups)
                   .filter(this::filterQuery)
                   .filter(this::filterIdsToHide)
                   .collect(Collectors.toList());
    }

    private void handleErrorResponse(Response<?> response, final boolean updateResource) {
        ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            if (updateResource) {
                this.recipients.postValue(Resource.error(R.string.generic_failed_request, this.getCachedRecipients()));
            }
            return;
        }
        String errorString;
        try {
            errorString = errorBody.string();
            Log.e(UserSearchViewModel.TAG, "handleErrorResponse: " + errorString);
        } catch (final IOException e) {
            Log.e(UserSearchViewModel.TAG, "handleErrorResponse: ", e);
            errorString = e.getMessage();
        }
        if (updateResource) {
            this.recipients.postValue(Resource.error(errorString, this.getCachedRecipients()));
        }
    }

    public void cleanup() {
        this.searchDebouncer.terminate();
    }

    public void setSelectedRecipient(RankedRecipient recipient, boolean selected) {
        if (selected) {
            this.selectedRecipients.add(recipient);
        } else {
            this.selectedRecipients.remove(recipient);
        }
        this.showAction.postValue(!this.selectedRecipients.isEmpty());
    }

    public Set<RankedRecipient> getSelectedRecipients() {
        return this.selectedRecipients;
    }

    public void clearResults() {
        this.recipients.postValue(Resource.success(Collections.emptyList()));
        this.prevQuery = "";
    }

    public void cancelSearch() {
        this.searchDebouncer.cancel(UserSearchViewModel.DEBOUNCE_KEY);
        if (this.searchRequest != null) {
            this.searchRequest.cancel();
            this.searchRequest = null;
        }
    }

    public LiveData<Boolean> showAction() {
        return this.showAction;
    }

    public void setSearchMode(UserSearchMode searchMode) {
        this.searchMode = searchMode;
    }

    public void setShowGroups(boolean showGroups) {
        this.showGroups = showGroups;
    }

    public void setHideUserIds(long[] hideUserIds) {
        if (hideUserIds != null) {
            long[] copy = Arrays.copyOf(hideUserIds, hideUserIds.length);
            Arrays.sort(copy);
            this.hideUserIds = copy;
            return;
        }
        this.hideUserIds = null;
    }

    public void setHideThreadIds(String[] hideThreadIds) {
        if (hideThreadIds != null) {
            String[] copy = Arrays.copyOf(hideThreadIds, hideThreadIds.length);
            Arrays.sort(copy);
            this.hideThreadIds = copy;
            return;
        }
        this.hideThreadIds = null;
    }

    private boolean filterOutGroups(@NonNull final RankedRecipient recipient) {
        // if showGroups is false, remove groups from the list
        if (this.showGroups || recipient.getThread() == null) {
            return true;
        }
        return !recipient.getThread().isGroup();
    }

    private boolean filterValidRecipients(@NonNull final RankedRecipient recipient) {
        // check if both user and thread are null
        return recipient.getUser() != null || recipient.getThread() != null;
    }

    private boolean filterIdsToHide(@NonNull final RankedRecipient recipient) {
        if (this.hideThreadIds != null && recipient.getThread() != null) {
            return Arrays.binarySearch(this.hideThreadIds, recipient.getThread().getThreadId()) < 0;
        }
        if (this.hideUserIds != null) {
            long pk = -1;
            if (recipient.getUser() != null) {
                pk = recipient.getUser().getPk();
            } else if (recipient.getThread() != null && !recipient.getThread().isGroup()) {
                User user = recipient.getThread().getUsers().get(0);
                pk = user.getPk();
            }
            return Arrays.binarySearch(this.hideUserIds, pk) < 0;
        }
        return true;
    }

    private boolean filterQuery(@NonNull final RankedRecipient recipient) {
        if (TextUtils.isEmpty(this.currentQuery)) {
            return true;
        }
        if (recipient.getThread() != null) {
            return recipient.getThread().getThreadTitle().toLowerCase().contains(this.currentQuery.toLowerCase());
        }
        return recipient.getUser().getUsername().toLowerCase().contains(this.currentQuery.toLowerCase())
                || recipient.getUser().getFullName().toLowerCase().contains(this.currentQuery.toLowerCase());
    }

    public void showCachedResults() {
        showCachedResults = true;
        if (this.rankedRecipientsCache.isUpdateInitiated()) return;
        this.recipients.postValue(Resource.success(this.getCachedRecipients()));
    }
}

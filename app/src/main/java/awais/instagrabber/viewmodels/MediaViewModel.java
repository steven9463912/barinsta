package awais.instagrabber.viewmodels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.fragments.settings.PreferenceKeys;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.utils.KeywordsFilterUtilsKt;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class MediaViewModel extends ViewModel {
    private static final String TAG = MediaViewModel.class.getSimpleName();

    private boolean refresh = true;

    private final PostFetcher postFetcher;
    private final MutableLiveData<List<Media>> list = new MutableLiveData<>();

    public MediaViewModel(@NonNull PostFetcher.PostFetchService postFetchService) {
        FetchListener<List<Media>> fetchListener = new FetchListener<List<Media>>() {
            @Override
            public void onResult(List<Media> result) {
                if (MediaViewModel.this.refresh) {
                    MediaViewModel.this.list.postValue(MediaViewModel.this.filterResult(result, true));
                    MediaViewModel.this.refresh = false;
                    return;
                }
                MediaViewModel.this.list.postValue(MediaViewModel.this.filterResult(result, false));
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(MediaViewModel.TAG, "onFailure: ", t);
            }
        };
        this.postFetcher = new PostFetcher(postFetchService, fetchListener);
    }

    @NonNull
    private List<Media> filterResult(List<Media> result, boolean isRefresh) {
        List<Media> models = this.list.getValue();
        List<Media> modelsCopy = models == null || isRefresh ? new ArrayList<>() : new ArrayList<>(models);
        if (settingsHelper.getBoolean(PreferenceKeys.TOGGLE_KEYWORD_FILTER)) {
            List<String> keywords = new ArrayList<>(settingsHelper.getStringSet(PreferenceKeys.KEYWORD_FILTERS));
            List<Media> filter = KeywordsFilterUtilsKt.filter(keywords, result);
            if (filter != null) {
                modelsCopy.addAll(filter);
            }
            return modelsCopy;
        }
        modelsCopy.addAll(result);
        return modelsCopy;
    }

    public LiveData<List<Media>> getList() {
        return this.list;
    }

    public boolean hasMore() {
        return this.postFetcher.hasMore();
    }

    public void fetch() {
        this.postFetcher.fetch();
    }

    public void reset() {
        this.postFetcher.reset();
    }

    public boolean isFetching() {
        return this.postFetcher.isFetching();
    }

    public void refresh() {
        this.refresh = true;
        this.reset();
        this.fetch();
    }

    public static class ViewModelFactory implements ViewModelProvider.Factory {

        @NonNull
        private final PostFetcher.PostFetchService postFetchService;

        public ViewModelFactory(@NonNull PostFetcher.PostFetchService postFetchService) {
            this.postFetchService = postFetchService;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            //noinspection unchecked
            return (T) new MediaViewModel(this.postFetchService);
        }
    }
}
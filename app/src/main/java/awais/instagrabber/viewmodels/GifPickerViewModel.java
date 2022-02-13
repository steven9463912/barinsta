package awais.instagrabber.viewmodels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import awais.instagrabber.R;
import awais.instagrabber.models.Resource;
import awais.instagrabber.repositories.responses.AnimatedMediaFixedHeight;
import awais.instagrabber.repositories.responses.giphy.GiphyGif;
import awais.instagrabber.repositories.responses.giphy.GiphyGifImages;
import awais.instagrabber.repositories.responses.giphy.GiphyGifResponse;
import awais.instagrabber.repositories.responses.giphy.GiphyGifResults;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.webservices.GifService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GifPickerViewModel extends ViewModel {
    private static final String TAG = GifPickerViewModel.class.getSimpleName();

    private final MutableLiveData<Resource<List<GiphyGif>>> images = new MutableLiveData<>(Resource.success(Collections.emptyList()));
    private final GifService gifService;

    private Call<GiphyGifResponse> searchRequest;

    public GifPickerViewModel() {
        this.gifService = GifService.getInstance();
        this.search(null);
    }

    public LiveData<Resource<List<GiphyGif>>> getImages() {
        return this.images;
    }

    public void search(String query) {
        Resource<List<GiphyGif>> currentValue = this.images.getValue();
        if (currentValue != null && currentValue.status == Resource.Status.LOADING) {
            this.cancelSearchRequest();
        }
        this.images.postValue(Resource.loading(this.getCurrentImages()));
        this.searchRequest = this.gifService.searchGiphyGifs(query, query != null);
        this.searchRequest.enqueue(new Callback<GiphyGifResponse>() {
            @Override
            public void onResponse(@NonNull Call<GiphyGifResponse> call,
                                   @NonNull Response<GiphyGifResponse> response) {
                if (response.isSuccessful()) {
                    GifPickerViewModel.this.parseResponse(response);
                    return;
                }
                if (response.errorBody() != null) {
                    try {
                        String string = response.errorBody().string();
                        String msg = String.format(Locale.US,
                                                         "onResponse: url: %s, responseCode: %d, errorBody: %s",
                                call.request().url(),
                                                         response.code(),
                                                         string);
                        GifPickerViewModel.this.images.postValue(Resource.error(msg, GifPickerViewModel.this.getCurrentImages()));
                        Log.e(GifPickerViewModel.TAG, msg);
                    } catch (final IOException e) {
                        GifPickerViewModel.this.images.postValue(Resource.error(e.getMessage(), GifPickerViewModel.this.getCurrentImages()));
                        Log.e(GifPickerViewModel.TAG, "onResponse: ", e);
                    }
                }
                GifPickerViewModel.this.images.postValue(Resource.error(R.string.generic_failed_request, GifPickerViewModel.this.getCurrentImages()));
            }

            @Override
            public void onFailure(@NonNull Call<GiphyGifResponse> call,
                                  @NonNull Throwable t) {
                GifPickerViewModel.this.images.postValue(Resource.error(t.getMessage(), GifPickerViewModel.this.getCurrentImages()));
                Log.e(GifPickerViewModel.TAG, "enqueueRequest: onFailure: ", t);
            }
        });
    }

    private void parseResponse(Response<GiphyGifResponse> response) {
        GiphyGifResponse giphyGifResponse = response.body();
        if (giphyGifResponse == null) {
            this.images.postValue(Resource.error(R.string.generic_null_response, this.getCurrentImages()));
            return;
        }
        GiphyGifResults results = giphyGifResponse.getResults();
        this.images.postValue(Resource.success(
                ImmutableList.<GiphyGif>builder()
                        .addAll(results.getGiphy() == null ? Collections.emptyList() : this.filterInvalid(results.getGiphy()))
                        .addAll(results.getGiphyGifs() == null ? Collections.emptyList() : this.filterInvalid(results.getGiphyGifs()))
                        .build()
        ));
    }

    private List<GiphyGif> filterInvalid(@NonNull List<GiphyGif> giphyGifs) {
        return giphyGifs.stream()
                        .filter(Objects::nonNull)
                        .filter(giphyGif -> {
                            GiphyGifImages images = giphyGif.getImages();
                            if (images == null) return false;
                            AnimatedMediaFixedHeight fixedHeight = images.getFixedHeight();
                            if (fixedHeight == null) return false;
                            return !TextUtils.isEmpty(fixedHeight.getWebp());
                        })
                        .collect(Collectors.toList());
    }

    // @NonNull
    // private List<GiphyGifImage> getGiphyGifImages(@NonNull final List<GiphyGif> giphy) {
    //     return giphy.stream()
    //                 .map(giphyGif -> {
    //                     final GiphyGifImages images = giphyGif.getImages();
    //                     if (images == null) return null;
    //                     return images.getOriginal();
    //                 })
    //                 .filter(Objects::nonNull)
    //                 .collect(Collectors.toList());
    // }

    private List<GiphyGif> getCurrentImages() {
        Resource<List<GiphyGif>> value = this.images.getValue();
        return value == null ? Collections.emptyList() : value.data;
    }

    public void cancelSearchRequest() {
        if (this.searchRequest == null) return;
        this.searchRequest.cancel();
    }
}

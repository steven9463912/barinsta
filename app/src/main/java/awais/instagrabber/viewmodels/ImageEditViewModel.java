package awais.instagrabber.viewmodels;

import android.app.Application;
import android.graphics.RectF;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import awais.instagrabber.fragments.imageedit.filters.FiltersHelper;
import awais.instagrabber.fragments.imageedit.filters.filters.Filter;
import awais.instagrabber.fragments.imageedit.filters.properties.Property;
import awais.instagrabber.models.SavedImageEditState;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.DownloadUtils;
import awais.instagrabber.utils.SerializablePair;
import awais.instagrabber.utils.Utils;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup;

public class ImageEditViewModel extends AndroidViewModel {
    private static final String CROP = "crop";
    private static final String RESULT = "result";
    private static final String FILE_FORMAT = "yyyyMMddHHmmssSSS";
    private static final String MIME_TYPE = Utils.mimeTypeMap.getMimeTypeFromExtension("jpg");
    private static final DateTimeFormatter SIMPLE_DATE_FORMAT = DateTimeFormatter.ofPattern(ImageEditViewModel.FILE_FORMAT, Locale.US);

    private Uri originalUri;
    private SavedImageEditState savedImageEditState;

    private final String sessionId;
    private final Uri destinationUri;
    private final Uri cropDestinationUri;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Uri> resultUri = new MutableLiveData<>(null);
    private final MutableLiveData<Tab> currentTab = new MutableLiveData<>(Tab.RESULT);
    private final MutableLiveData<Boolean> isCropped = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isTuned = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isFiltered = new MutableLiveData<>(false);
    private final DocumentFile outputDir;
    private List<Filter<? extends GPUImageFilter>> tuningFilters;
    private Filter<? extends GPUImageFilter> appliedFilter;
    private final DocumentFile destinationFile;

    public ImageEditViewModel(Application application) {
        super(application);
        this.sessionId = LocalDateTime.now().format(ImageEditViewModel.SIMPLE_DATE_FORMAT);
        this.outputDir = DownloadUtils.getImageEditDir(this.sessionId, application);
        this.destinationFile = this.outputDir.createFile(ImageEditViewModel.MIME_TYPE, ImageEditViewModel.RESULT + ".jpg");
        this.destinationUri = this.destinationFile.getUri();
        this.cropDestinationUri = this.outputDir.createFile(ImageEditViewModel.MIME_TYPE, ImageEditViewModel.CROP + ".jpg").getUri();
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public Uri getOriginalUri() {
        return this.originalUri;
    }

    public void setOriginalUri(Uri originalUri) {
        if (originalUri == null) return;
        this.originalUri = originalUri;
        this.savedImageEditState = new SavedImageEditState(this.sessionId, originalUri.toString());
        if (this.resultUri.getValue() == null) {
            this.resultUri.postValue(originalUri);
        }
    }

    public Uri getDestinationUri() {
        return this.destinationUri;
    }

    public Uri getCropDestinationUri() {
        return this.cropDestinationUri;
    }

    public LiveData<Boolean> isLoading() {
        return this.loading;
    }

    public LiveData<Uri> getResultUri() {
        return this.resultUri;
    }

    public LiveData<Boolean> isCropped() {
        return this.isCropped;
    }

    public LiveData<Boolean> isTuned() {
        return this.isTuned;
    }

    public LiveData<Boolean> isFiltered() {
        return this.isFiltered;
    }

    public void setResultUri(Uri uri) {
        if (uri == null) return;
        this.resultUri.postValue(uri);
    }

    public LiveData<Tab> getCurrentTab() {
        return this.currentTab;
    }

    public void setCurrentTab(Tab tab) {
        if (tab == null) return;
        currentTab.postValue(tab);
    }

    public SavedImageEditState getSavedImageEditState() {
        return this.savedImageEditState;
    }

    public void setCropResult(float[] imageMatrixValues, RectF cropRect) {
        this.savedImageEditState.setCropImageMatrixValues(imageMatrixValues);
        this.savedImageEditState.setCropRect(cropRect);
        this.isCropped.postValue(true);
        this.applyFilters();
    }

    private void applyFilters() {
        GPUImage gpuImage = new GPUImage(this.getApplication());
        if ((this.tuningFilters != null && !this.tuningFilters.isEmpty()) || this.appliedFilter != null) {
            AppExecutors.INSTANCE.getTasksThread().submit(() -> {
                List<GPUImageFilter> list = new ArrayList<>();
                if (this.tuningFilters != null) {
                    for (final Filter<? extends GPUImageFilter> tuningFilter : this.tuningFilters) {
                        list.add(tuningFilter.getInstance());
                    }
                }
                if (this.appliedFilter != null) {
                    list.add(this.appliedFilter.getInstance());
                }
                gpuImage.setFilter(new GPUImageFilterGroup(list));
                Uri uri = this.cropDestinationUri != null ? this.cropDestinationUri : this.originalUri;
                gpuImage.setImage(uri);
                gpuImage.saveToPictures(new File(this.destinationUri.toString()), false, uri1 -> this.setResultUri(this.destinationUri));
            });
            return;
        }
        this.setResultUri(this.cropDestinationUri);
    }

    public void cancel() {
        this.delete(this.outputDir);
    }

    private void delete(@NonNull DocumentFile file) {
        if (file.isDirectory()) {
            DocumentFile[] files = file.listFiles();
            if (files != null) {
                for (final DocumentFile f : files) {
                    this.delete(f);
                }
            }
        }
        file.delete();
    }

    public void setAppliedFilters(List<Filter<?>> tuningFilters, Filter<?> filter) {
        this.tuningFilters = tuningFilters;
        appliedFilter = filter;
        if (this.savedImageEditState != null) {
            HashMap<FiltersHelper.FilterType, Map<Integer, Object>> tuningFiltersMap = new HashMap<>();
            for (final Filter<?> tuningFilter : tuningFilters) {
                final SerializablePair<FiltersHelper.FilterType, Map<Integer, Object>> filterValuesMap = getFilterValuesMap(tuningFilter);
                tuningFiltersMap.put(filterValuesMap.first, filterValuesMap.second);
            }
            savedImageEditState.setAppliedTuningFilters(tuningFiltersMap);
            savedImageEditState.setAppliedFilter(getFilterValuesMap(filter));
        }
        isTuned.postValue(!tuningFilters.isEmpty());
        isFiltered.postValue(filter != null);
        setResultUri(destinationUri);
    }

    private SerializablePair<FiltersHelper.FilterType, Map<Integer, Object>> getFilterValuesMap(final Filter<?> filter) {
        if (filter == null) return null;
        final FiltersHelper.FilterType type = filter.getType();
        Map<Integer, Property<?>> properties = filter.getProperties();
        Map<Integer, Object> propertyValueMap = new HashMap<>();
        if (properties != null) {
            Set<Map.Entry<Integer, Property<?>>> entries = properties.entrySet();
            for (Map.Entry<Integer, Property<?>> entry : entries) {
                Integer propId = entry.getKey();
                Property<?> property = entry.getValue();
                Object value = property.getValue();
                propertyValueMap.put(propId, value);
            }
        }
        return new SerializablePair<>(type, propertyValueMap);
    }

    // public File getDestinationFile() {
    //     return destinationFile;
    // }

    public enum Tab {
        RESULT,
        CROP,
        TUNE,
        FILTERS
    }
}

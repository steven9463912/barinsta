package awais.instagrabber.fragments.imageedit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.Barrier;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.material.slider.Slider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import awais.instagrabber.adapters.FiltersAdapter;
import awais.instagrabber.databinding.FragmentFiltersBinding;
import awais.instagrabber.fragments.imageedit.filters.FiltersHelper;
import awais.instagrabber.fragments.imageedit.filters.filters.Filter;
import awais.instagrabber.fragments.imageedit.filters.filters.FilterFactory;
import awais.instagrabber.fragments.imageedit.filters.properties.FloatProperty;
import awais.instagrabber.fragments.imageedit.filters.properties.Property;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.BitmapUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.SerializablePair;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.FiltersFragmentViewModel;
import awais.instagrabber.viewmodels.ImageEditViewModel;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup;
import kotlinx.coroutines.Dispatchers;

public class FiltersFragment extends Fragment {
    private static final String TAG = FiltersFragment.class.getSimpleName();

    private static final String ARGS_SOURCE_URI = "source_uri";
    private static final String ARGS_DEST_URI = "dest_uri";
    private static final String ARGS_TUNING_FILTERS = "tuning_filters";
    private static final String ARGS_FILTER = "filter";
    private static final String ARGS_TAB = "tab";

    private final Map<FiltersHelper.FilterType, Filter<?>> tuningFilters = new HashMap<>();
    private final Map<Property<?>, Integer> propertySliderIdMap = new HashMap<>();

    private GPUImageFilterGroup filterGroup;
    private Filter<? extends GPUImageFilter> appliedFilter;
    private FragmentFiltersBinding binding;
    private AppExecutors appExecutors;
    private Uri sourceUri;
    private Uri destUri;
    private FiltersFragmentViewModel viewModel;
    private boolean isFilterGroupSet;
    private FilterCallback callback;
    private FiltersAdapter filtersAdapter;
    private HashMap<FiltersHelper.FilterType, Map<Integer, Object>> initialTuningFiltersValues;
    private SerializablePair<FiltersHelper.FilterType, Map<Integer, Object>> initialFilter;

    @NonNull
    public static FiltersFragment newInstance(@NonNull final Uri sourceUri,
                                              @NonNull final Uri destUri,
                                              @NonNull final ImageEditViewModel.Tab tab) {
        return newInstance(sourceUri, destUri, null, null, tab);
    }

    @NonNull
    public static FiltersFragment newInstance(@NonNull final Uri sourceUri,
                                              @NonNull final Uri destUri,
                                              final HashMap<FiltersHelper.FilterType, Map<Integer, Object>> appliedTuningFilters,
                                              final SerializablePair<FiltersHelper.FilterType, Map<Integer, Object>> appliedFilter,
                                              @NonNull final ImageEditViewModel.Tab tab) {
        final Bundle args = new Bundle();
        args.putParcelable(ARGS_SOURCE_URI, sourceUri);
        args.putParcelable(ARGS_DEST_URI, destUri);
        if (appliedTuningFilters != null) {
            args.putSerializable(ARGS_TUNING_FILTERS, appliedTuningFilters);
        }
        if (appliedFilter != null) {
            args.putSerializable(ARGS_FILTER, appliedFilter);
        }
        args.putString(ARGS_TAB, tab.name());
        final FiltersFragment fragment = new FiltersFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public FiltersFragment() {
        filterGroup = new GPUImageFilterGroup();
        filterGroup.addFilter(new GPUImageFilter());
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appExecutors = AppExecutors.INSTANCE;
        viewModel = new ViewModelProvider(this).get(FiltersFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        binding = FragmentFiltersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        init(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        final ImageEditViewModel.Tab tab = viewModel.getCurrentTab().getValue();
        if (tab != null) {
            outState.putString(ARGS_TAB, tab.name());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // binding.preview.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        // binding.preview.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (final GPUImageFilter filter : filterGroup.getFilters()) {
            filter.destroy();
        }
        filterGroup.getFilters().clear();
        filterGroup.destroy();
    }

    private void init(final Bundle savedInstanceState) {
        setupObservers();
        final Bundle arguments = getArguments();
        if (arguments == null) return;
        final Parcelable uriParcelable = arguments.getParcelable(ARGS_SOURCE_URI);
        if (!(uriParcelable instanceof Uri)) return;
        sourceUri = (Uri) uriParcelable;
        final Parcelable destUriParcelable = arguments.getParcelable(ARGS_DEST_URI);
        if (!(destUriParcelable instanceof Uri)) return;
        destUri = (Uri) destUriParcelable;
        final Serializable tuningFiltersSerializable = arguments.getSerializable(ARGS_TUNING_FILTERS);
        if (tuningFiltersSerializable instanceof HashMap) {
            try {
                //noinspection unchecked
                initialTuningFiltersValues = (HashMap<FiltersHelper.FilterType, Map<Integer, Object>>) tuningFiltersSerializable;
            } catch (Exception e) {
                Log.e(TAG, "init: ", e);
            }
        }
        final Serializable filterSerializable = arguments.getSerializable(ARGS_FILTER);
        if (filterSerializable instanceof SerializablePair) {
            try {
                //noinspection unchecked
                initialFilter = (SerializablePair<FiltersHelper.FilterType, Map<Integer, Object>>) filterSerializable;
            } catch (final Exception e) {
                Log.e(FiltersFragment.TAG, "init: ", e);
            }
        }
        Context context = this.getContext();
        if (context == null) return;
        this.binding.preview.setScaleType(GPUImage.ScaleType.CENTER_INSIDE);
        this.appExecutors.getTasksThread().execute(() -> {
            this.binding.preview.setImage(this.sourceUri);
            this.setPreviewBounds();
        });
        this.setCurrentTab(ImageEditViewModel.Tab.valueOf(savedInstanceState != null && savedInstanceState.containsKey(FiltersFragment.ARGS_TAB)
                                                     ? savedInstanceState.getString(FiltersFragment.ARGS_TAB)
                                                     : arguments.getString(FiltersFragment.ARGS_TAB)));
        this.binding.cancel.setOnClickListener(v -> {
            if (this.callback == null) return;
            this.callback.onCancel();
        });
        this.binding.reset.setOnClickListener(v -> {
            ImageEditViewModel.Tab tab = this.viewModel.getCurrentTab().getValue();
            if (tab == ImageEditViewModel.Tab.TUNE) {
                Collection<Filter<?>> filters = this.tuningFilters.values();
                for (Filter<?> filter : filters) {
                    if (filter == null) continue;
                    filter.reset();
                }
                this.resetSliders();
            }
            if (tab == ImageEditViewModel.Tab.FILTERS) {
                List<GPUImageFilter> groupFilters = this.filterGroup.getFilters();
                if (this.appliedFilter != null) {
                    groupFilters.remove(this.appliedFilter.getInstance());
                    this.appliedFilter = null;
                }
                if (this.filtersAdapter != null) {
                    this.filtersAdapter.setSelected(0);
                }
                this.binding.preview.post(() -> this.binding.preview.setFilter(this.filterGroup = new GPUImageFilterGroup(groupFilters)));
            }
        });
        this.binding.apply.setOnClickListener(v -> {
            if (this.callback == null) return;
            List<Filter<?>> appliedTunings = this.getAppliedTunings();
            this.appExecutors.getTasksThread().submit(() -> {
                Bitmap bitmap = this.binding.preview.getGPUImage().getBitmapWithFilterApplied();
                try {
                    BitmapUtils.convertToJpegAndSaveToUri(context, bitmap, this.destUri);
                    this.callback.onApply(this.destUri, appliedTunings, this.appliedFilter);
                } catch (final Exception e) {
                    Log.e(FiltersFragment.TAG, "init: ", e);
                }
            });
        });
    }

    @NonNull
    private List<Filter<?>> getAppliedTunings() {
        return this.tuningFilters
                .values()
                .stream()
                .filter(Objects::nonNull)
                .filter(filter -> {
                    Map<Integer, Property<?>> propertyMap = filter.getProperties();
                    if (propertyMap == null) return false;
                    Collection<Property<?>> properties = propertyMap.values();
                    return properties.stream()
                                     .noneMatch(property -> {
                                         Object value = property.getValue();
                                         if (value == null) {
                                             return false;
                                         }
                                         return value.equals(property.getDefaultValue());
                                     });
                })
                .collect(Collectors.toList());
    }

    private void resetSliders() {
        Set<Map.Entry<Property<?>, Integer>> entries = this.propertySliderIdMap.entrySet();
        for (Map.Entry<Property<?>, Integer> entry : entries) {
            Property<?> property = entry.getKey();
            Integer viewId = entry.getValue();
            Slider slider = this.binding.getRoot().findViewById(viewId);
            if (slider == null) continue;
            Object defaultValue = property.getDefaultValue();
            if (!(defaultValue instanceof Float)) continue;
            slider.setValue((float) defaultValue);
        }
    }

    private void setPreviewBounds() {
        InputStream inputStream = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Context context = this.getContext();
            if (context == null) return;
            inputStream = context.getContentResolver().openInputStream(this.sourceUri);
            BitmapFactory.decodeStream(inputStream, null, options);
            float ratio = (float) options.outWidth / options.outHeight;
            this.appExecutors.getMainThread().execute(() -> {
                ViewGroup.LayoutParams previewLayoutParams = this.binding.preview.getLayoutParams();
                if (options.outHeight > options.outWidth) {
                    previewLayoutParams.width = (int) (this.binding.preview.getHeight() * ratio);
                } else {
                    previewLayoutParams.height = (int) (this.binding.preview.getWidth() / ratio);
                }
                this.binding.preview.setRatio(ratio);
                this.binding.preview.requestLayout();
            });
        } catch (final FileNotFoundException e) {
            Log.e(FiltersFragment.TAG, "setPreviewBounds: ", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (final IOException ignored) {}
            }
        }
    }

    private void setupObservers() {
        this.viewModel.isLoading().observe(this.getViewLifecycleOwner(), loading -> {

        });
        this.viewModel.getCurrentTab().observe(this.getViewLifecycleOwner(), tab -> {
            switch (tab) {
                case TUNE:
                    this.setupTuning();
                    break;
                case FILTERS:
                    this.setupFilters();
                    break;
                default:
                    break;
            }
        });
    }

    private void setupTuning() {
        this.initTuningControls();
        this.binding.filters.setVisibility(View.GONE);
        this.binding.tuneControlsWrapper.setVisibility(View.VISIBLE);
    }

    private void initTuningControls() {
        Context context = this.getContext();
        if (context == null) return;
        ConstraintLayout controlsParent = new ConstraintLayout(context);
        controlsParent.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Barrier sliderBarrier = new Barrier(context);
        sliderBarrier.setId(View.generateViewId());
        sliderBarrier.setType(Barrier.START);
        controlsParent.addView(sliderBarrier);
        this.binding.tuneControlsWrapper.addView(controlsParent);
        int labelPadding = Utils.convertDpToPx(8);
        List<Filter<?>> tuneFilters = FiltersHelper.getTuneFilters();
        Slider previousSlider = null;
        // Need to iterate backwards
        for (int i = tuneFilters.size() - 1; i >= 0; i--) {
            Filter<?> tuneFilter = tuneFilters.get(i);
            if (tuneFilter.getProperties() == null || tuneFilter.getProperties().isEmpty() || tuneFilter.getProperties().size() > 1) continue;
            int propKey = tuneFilter.getProperties().keySet().iterator().next();
            Property<?> property = tuneFilter.getProperties().values().iterator().next();
            if (!(property instanceof FloatProperty)) continue;
            GPUImageFilter filterInstance = tuneFilter.getInstance();
            this.tuningFilters.put(tuneFilter.getType(), tuneFilter);
            this.filterGroup.addFilter(filterInstance);

            FloatProperty floatProperty = (FloatProperty) property;
            Slider slider = new Slider(context);
            int viewId = View.generateViewId();
            slider.setId(viewId);
            this.propertySliderIdMap.put(floatProperty, viewId);

            ConstraintLayout.LayoutParams sliderLayoutParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);

            sliderLayoutParams.startToEnd = sliderBarrier.getId();
            sliderLayoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            if (previousSlider == null) {
                sliderLayoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            } else {
                sliderLayoutParams.bottomToTop = previousSlider.getId();
                ConstraintLayout.LayoutParams prevSliderLayoutParams = (ConstraintLayout.LayoutParams) previousSlider.getLayoutParams();
                prevSliderLayoutParams.topToBottom = slider.getId();
            }
            if (i == 0) {
                sliderLayoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            }
            slider.setLayoutParams(sliderLayoutParams);
            slider.setValueFrom(floatProperty.getMinValue());
            slider.setValueTo(floatProperty.getMaxValue());
            float defaultValue = floatProperty.getDefaultValue();
            if (this.initialTuningFiltersValues != null && this.initialTuningFiltersValues.containsKey(tuneFilter.getType())) {
                Map<Integer, Object> valueMap = this.initialTuningFiltersValues.get(tuneFilter.getType());
                if (valueMap != null) {
                    Object value = valueMap.get(propKey);
                    if (value instanceof Float) {
                        defaultValue = (float) value;
                        tuneFilter.adjust(propKey, value);
                    }
                }
            }
            slider.setValue(defaultValue);
            slider.addOnChangeListener((slider1, value, fromUser) -> {
                Filter<?> filter = this.tuningFilters.get(tuneFilter.getType());
                if (filter != null) {
                    tuneFilter.adjust(propKey, value);
                }
                this.binding.preview.post(() -> this.binding.preview.requestRender());
            });

            AppCompatTextView label = new AppCompatTextView(context);
            label.setId(View.generateViewId());
            ConstraintLayout.LayoutParams labelLayoutParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                                                      ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
            labelLayoutParams.topToTop = slider.getId();
            labelLayoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            labelLayoutParams.endToStart = sliderBarrier.getId();
            labelLayoutParams.bottomToBottom = slider.getId();
            labelLayoutParams.horizontalBias = 1;
            label.setLayoutParams(labelLayoutParams);
            label.setGravity(Gravity.CENTER);
            label.setPadding(labelPadding, labelPadding, labelPadding, labelPadding);
            label.setText(tuneFilter.getLabel());

            controlsParent.addView(label);
            controlsParent.addView(slider);

            previousSlider = slider;
        }
        this.addInitialFilter();
        if (!this.isFilterGroupSet) {
            this.isFilterGroupSet = true;
            this.binding.preview.post(() -> this.binding.preview.setFilter(this.filterGroup));
        }
    }

    private void addInitialFilter() {
        if (this.initialFilter == null) return;
        Filter<?> instance = FilterFactory.getInstance(this.initialFilter.first);
        if (instance == null) return;
        this.addFilterToGroup(instance, this.initialFilter.second);
        this.appliedFilter = instance;
    }

    private void setupFilters() {
        Context context = this.getContext();
        if (context == null) return;
        this.addTuneFilters();
        this.binding.filters.setVisibility(View.VISIBLE);
        RecyclerView.ItemAnimator animator = this.binding.filters.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            SimpleItemAnimator itemAnimator = (SimpleItemAnimator) animator;
            itemAnimator.setSupportsChangeAnimations(false);
        }
        this.binding.tuneControlsWrapper.setVisibility(View.GONE);
        this.binding.filters.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
        FiltersAdapter.OnFilterClickListener onFilterClickListener = (position, filter) -> {
            if (this.appliedFilter != null && this.appliedFilter.equals(filter)) return;
            List<GPUImageFilter> filters = this.filterGroup.getFilters();
            if (this.appliedFilter != null) {
                // remove applied filter from current filter list
                filters.remove(this.appliedFilter.getInstance());
            }
            // add the new filter
            filters.add(filter.getInstance());
            this.filterGroup = new GPUImageFilterGroup(filters);
            this.binding.preview.post(() -> this.binding.preview.setFilter(this.filterGroup));
            this.filtersAdapter.setSelected(position);
            this.appliedFilter = filter;
        };
        BitmapUtils.getThumbnail(
                context,
                this.sourceUri,
                CoroutineUtilsKt.getContinuation((bitmapResult, throwable) -> this.appExecutors.getMainThread().execute(() -> {
                    if (throwable != null) {
                        Log.e(FiltersFragment.TAG, "setupFilters: ", throwable);
                        return;
                    }
                    if (bitmapResult == null || bitmapResult.getBitmap() == null) {
                        return;
                    }
                    this.filtersAdapter = new FiltersAdapter(
                            this.tuningFilters.values()
                                         .stream()
                                         .map(Filter::getInstance)
                                         .collect(Collectors.toList()),
                            this.sourceUri.toString(),
                            bitmapResult.getBitmap(),
                            onFilterClickListener
                    );
                    this.binding.filters.setAdapter(this.filtersAdapter);
                    this.filtersAdapter.submitList(FiltersHelper.getFilters(), () -> {
                        if (this.appliedFilter == null) return;
                        this.filtersAdapter.setSelectedFilter(this.appliedFilter.getInstance());
                    });
                }), Dispatchers.getIO())
        );
        this.addInitialFilter();
        this.binding.preview.setFilter(this.filterGroup);
    }

    private void addTuneFilters() {
        if (this.initialTuningFiltersValues == null) return;
        List<Filter<?>> tuneFilters = FiltersHelper.getTuneFilters();
        for (Filter<?> tuneFilter : tuneFilters) {
            if (!this.initialTuningFiltersValues.containsKey(tuneFilter.getType())) continue;
            this.addFilterToGroup(tuneFilter, this.initialTuningFiltersValues.get(tuneFilter.getType()));
        }
    }

    private void addFilterToGroup(@NonNull Filter<?> tuneFilter, Map<Integer, Object> valueMap) {
        GPUImageFilter filter = tuneFilter.getInstance();
        this.filterGroup.addFilter(filter);
        if (valueMap == null) return;
        Set<Map.Entry<Integer, Object>> entries = valueMap.entrySet();
        for (Map.Entry<Integer, Object> entry : entries) {
            tuneFilter.adjust(entry.getKey(), entry.getValue());
        }
    }

    public void setCurrentTab(ImageEditViewModel.Tab tab) {
        this.viewModel.setCurrentTab(tab);
    }

    public void setCallback(FilterCallback callback) {
        if (callback == null) return;
        this.callback = callback;
    }

    public interface FilterCallback {
        void onApply(Uri uri, List<Filter<?>> tuningFilters, Filter<?> filter);

        void onCancel();
    }
}

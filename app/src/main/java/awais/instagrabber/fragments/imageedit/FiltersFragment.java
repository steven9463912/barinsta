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
    public static FiltersFragment newInstance(@NonNull Uri sourceUri,
                                              @NonNull Uri destUri,
                                              @NonNull ImageEditViewModel.Tab tab) {
        return FiltersFragment.newInstance(sourceUri, destUri, null, null, tab);
    }

    @NonNull
    public static FiltersFragment newInstance(@NonNull Uri sourceUri,
                                              @NonNull Uri destUri,
                                              HashMap<FiltersHelper.FilterType, Map<Integer, Object>> appliedTuningFilters,
                                              SerializablePair<FiltersHelper.FilterType, Map<Integer, Object>> appliedFilter,
                                              @NonNull ImageEditViewModel.Tab tab) {
        Bundle args = new Bundle();
        args.putParcelable(FiltersFragment.ARGS_SOURCE_URI, sourceUri);
        args.putParcelable(FiltersFragment.ARGS_DEST_URI, destUri);
        if (appliedTuningFilters != null) {
            args.putSerializable(FiltersFragment.ARGS_TUNING_FILTERS, appliedTuningFilters);
        }
        if (appliedFilter != null) {
            args.putSerializable(FiltersFragment.ARGS_FILTER, appliedFilter);
        }
        args.putString(FiltersFragment.ARGS_TAB, tab.name());
        FiltersFragment fragment = new FiltersFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public FiltersFragment() {
        this.filterGroup = new GPUImageFilterGroup();
        this.filterGroup.addFilter(new GPUImageFilter());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.appExecutors = AppExecutors.INSTANCE;
        this.viewModel = new ViewModelProvider(this).get(FiltersFragmentViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = FragmentFiltersBinding.inflate(inflater, container, false);
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        this.init(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        ImageEditViewModel.Tab tab = this.viewModel.getCurrentTab().getValue();
        if (tab != null) {
            outState.putString(FiltersFragment.ARGS_TAB, tab.name());
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
        for (GPUImageFilter filter : this.filterGroup.getFilters()) {
            filter.destroy();
        }
        this.filterGroup.getFilters().clear();
        this.filterGroup.destroy();
    }

    private void init(Bundle savedInstanceState) {
        this.setupObservers();
        Bundle arguments = this.getArguments();
        if (arguments == null) return;
        Parcelable uriParcelable = arguments.getParcelable(FiltersFragment.ARGS_SOURCE_URI);
        if (!(uriParcelable instanceof Uri)) return;
        this.sourceUri = (Uri) uriParcelable;
        Parcelable destUriParcelable = arguments.getParcelable(FiltersFragment.ARGS_DEST_URI);
        if (!(destUriParcelable instanceof Uri)) return;
        this.destUri = (Uri) destUriParcelable;
        Serializable tuningFiltersSerializable = arguments.getSerializable(FiltersFragment.ARGS_TUNING_FILTERS);
        if (tuningFiltersSerializable instanceof HashMap) {
            try {
                //noinspection unchecked
                this.initialTuningFiltersValues = (HashMap<FiltersHelper.FilterType, Map<Integer, Object>>) tuningFiltersSerializable;
            } catch (final Exception e) {
                Log.e(FiltersFragment.TAG, "init: ", e);
            }
        }
        Serializable filterSerializable = arguments.getSerializable(FiltersFragment.ARGS_FILTER);
        if (filterSerializable instanceof SerializablePair) {
            try {
                //noinspection unchecked
                this.initialFilter = (SerializablePair<FiltersHelper.FilterType, Map<Integer, Object>>) filterSerializable;
            } catch (Exception e) {
                Log.e(TAG, "init: ", e);
            }
        }
        final Context context = getContext();
        if (context == null) return;
        binding.preview.setScaleType(GPUImage.ScaleType.CENTER_INSIDE);
        appExecutors.getTasksThread().execute(() -> {
            binding.preview.setImage(sourceUri);
            setPreviewBounds();
        });
        setCurrentTab(ImageEditViewModel.Tab.valueOf(savedInstanceState != null && savedInstanceState.containsKey(ARGS_TAB)
                                                     ? savedInstanceState.getString(ARGS_TAB)
                                                     : arguments.getString(ARGS_TAB)));
        binding.cancel.setOnClickListener(v -> {
            if (callback == null) return;
            callback.onCancel();
        });
        binding.reset.setOnClickListener(v -> {
            final ImageEditViewModel.Tab tab = viewModel.getCurrentTab().getValue();
            if (tab == ImageEditViewModel.Tab.TUNE) {
                final Collection<Filter<?>> filters = tuningFilters.values();
                for (final Filter<?> filter : filters) {
                    if (filter == null) continue;
                    filter.reset();
                }
                resetSliders();
            }
            if (tab == ImageEditViewModel.Tab.FILTERS) {
                final List<GPUImageFilter> groupFilters = filterGroup.getFilters();
                if (appliedFilter != null) {
                    groupFilters.remove(appliedFilter.getInstance());
                    appliedFilter = null;
                }
                if (filtersAdapter != null) {
                    filtersAdapter.setSelected(0);
                }
                binding.preview.post(() -> binding.preview.setFilter(filterGroup = new GPUImageFilterGroup(groupFilters)));
            }
        });
        binding.apply.setOnClickListener(v -> {
            if (callback == null) return;
            final List<Filter<?>> appliedTunings = getAppliedTunings();
            appExecutors.getTasksThread().submit(() -> {
                final Bitmap bitmap = binding.preview.getGPUImage().getBitmapWithFilterApplied();
                try {
                    BitmapUtils.convertToJpegAndSaveToUri(context, bitmap, destUri);
                    callback.onApply(destUri, appliedTunings, appliedFilter);
                } catch (Exception e) {
                    Log.e(TAG, "init: ", e);
                }
            });
        });
    }

    @NonNull
    private List<Filter<?>> getAppliedTunings() {
        return tuningFilters
                .values()
                .stream()
                .filter(Objects::nonNull)
                .filter(filter -> {
                    final Map<Integer, Property<?>> propertyMap = filter.getProperties();
                    if (propertyMap == null) return false;
                    final Collection<Property<?>> properties = propertyMap.values();
                    return properties.stream()
                                     .noneMatch(property -> {
                                         final Object value = property.getValue();
                                         if (value == null) {
                                             return false;
                                         }
                                         return value.equals(property.getDefaultValue());
                                     });
                })
                .collect(Collectors.toList());
    }

    private void resetSliders() {
        final Set<Map.Entry<Property<?>, Integer>> entries = propertySliderIdMap.entrySet();
        for (final Map.Entry<Property<?>, Integer> entry : entries) {
            final Property<?> property = entry.getKey();
            final Integer viewId = entry.getValue();
            final Slider slider = binding.getRoot().findViewById(viewId);
            if (slider == null) continue;
            final Object defaultValue = property.getDefaultValue();
            if (!(defaultValue instanceof Float)) continue;
            slider.setValue((float) defaultValue);
        }
    }

    private void setPreviewBounds() {
        InputStream inputStream = null;
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            final Context context = getContext();
            if (context == null) return;
            inputStream = context.getContentResolver().openInputStream(sourceUri);
            BitmapFactory.decodeStream(inputStream, null, options);
            final float ratio = (float) options.outWidth / options.outHeight;
            appExecutors.getMainThread().execute(() -> {
                final ViewGroup.LayoutParams previewLayoutParams = binding.preview.getLayoutParams();
                if (options.outHeight > options.outWidth) {
                    previewLayoutParams.width = (int) (binding.preview.getHeight() * ratio);
                } else {
                    previewLayoutParams.height = (int) (binding.preview.getWidth() / ratio);
                }
                binding.preview.setRatio(ratio);
                binding.preview.requestLayout();
            });
        } catch (FileNotFoundException e) {
            Log.e(TAG, "setPreviewBounds: ", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private void setupObservers() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {

        });
        viewModel.getCurrentTab().observe(getViewLifecycleOwner(), tab -> {
            switch (tab) {
                case TUNE:
                    setupTuning();
                    break;
                case FILTERS:
                    setupFilters();
                    break;
                default:
                    break;
            }
        });
    }

    private void setupTuning() {
        initTuningControls();
        binding.filters.setVisibility(View.GONE);
        binding.tuneControlsWrapper.setVisibility(View.VISIBLE);
    }

    private void initTuningControls() {
        final Context context = getContext();
        if (context == null) return;
        final ConstraintLayout controlsParent = new ConstraintLayout(context);
        controlsParent.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final Barrier sliderBarrier = new Barrier(context);
        sliderBarrier.setId(View.generateViewId());
        sliderBarrier.setType(Barrier.START);
        controlsParent.addView(sliderBarrier);
        binding.tuneControlsWrapper.addView(controlsParent);
        final int labelPadding = Utils.convertDpToPx(8);
        final List<Filter<?>> tuneFilters = FiltersHelper.getTuneFilters();
        Slider previousSlider = null;
        // Need to iterate backwards
        for (int i = tuneFilters.size() - 1; i >= 0; i--) {
            final Filter<?> tuneFilter = tuneFilters.get(i);
            if (tuneFilter.getProperties() == null || tuneFilter.getProperties().isEmpty() || tuneFilter.getProperties().size() > 1) continue;
            final int propKey = tuneFilter.getProperties().keySet().iterator().next();
            final Property<?> property = tuneFilter.getProperties().values().iterator().next();
            if (!(property instanceof FloatProperty)) continue;
            final GPUImageFilter filterInstance = tuneFilter.getInstance();
            tuningFilters.put(tuneFilter.getType(), tuneFilter);
            filterGroup.addFilter(filterInstance);

            final FloatProperty floatProperty = (FloatProperty) property;
            final Slider slider = new Slider(context);
            final int viewId = View.generateViewId();
            slider.setId(viewId);
            propertySliderIdMap.put(floatProperty, viewId);

            final ConstraintLayout.LayoutParams sliderLayoutParams = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);

            sliderLayoutParams.startToEnd = sliderBarrier.getId();
            sliderLayoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            if (previousSlider == null) {
                sliderLayoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            } else {
                sliderLayoutParams.bottomToTop = previousSlider.getId();
                final ConstraintLayout.LayoutParams prevSliderLayoutParams = (ConstraintLayout.LayoutParams) previousSlider.getLayoutParams();
                prevSliderLayoutParams.topToBottom = slider.getId();
            }
            if (i == 0) {
                sliderLayoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            }
            slider.setLayoutParams(sliderLayoutParams);
            slider.setValueFrom(floatProperty.getMinValue());
            slider.setValueTo(floatProperty.getMaxValue());
            float defaultValue = floatProperty.getDefaultValue();
            if (initialTuningFiltersValues != null && initialTuningFiltersValues.containsKey(tuneFilter.getType())) {
                final Map<Integer, Object> valueMap = initialTuningFiltersValues.get(tuneFilter.getType());
                if (valueMap != null) {
                    final Object value = valueMap.get(propKey);
                    if (value instanceof Float) {
                        defaultValue = (float) value;
                        tuneFilter.adjust(propKey, value);
                    }
                }
            }
            slider.setValue(defaultValue);
            slider.addOnChangeListener((slider1, value, fromUser) -> {
                final Filter<?> filter = tuningFilters.get(tuneFilter.getType());
                if (filter != null) {
                    tuneFilter.adjust(propKey, value);
                }
                binding.preview.post(() -> binding.preview.requestRender());
            });

            final AppCompatTextView label = new AppCompatTextView(context);
            label.setId(View.generateViewId());
            final ConstraintLayout.LayoutParams labelLayoutParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
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
        addInitialFilter();
        if (!isFilterGroupSet) {
            isFilterGroupSet = true;
            binding.preview.post(() -> binding.preview.setFilter(filterGroup));
        }
    }

    private void addInitialFilter() {
        if (initialFilter == null) return;
        final Filter<?> instance = FilterFactory.getInstance(initialFilter.first);
        if (instance == null) return;
        addFilterToGroup(instance, initialFilter.second);
        appliedFilter = instance;
    }

    private void setupFilters() {
        final Context context = getContext();
        if (context == null) return;
        addTuneFilters();
        binding.filters.setVisibility(View.VISIBLE);
        final RecyclerView.ItemAnimator animator = binding.filters.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            final SimpleItemAnimator itemAnimator = (SimpleItemAnimator) animator;
            itemAnimator.setSupportsChangeAnimations(false);
        }
        binding.tuneControlsWrapper.setVisibility(View.GONE);
        binding.filters.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));
        final FiltersAdapter.OnFilterClickListener onFilterClickListener = (position, filter) -> {
            if (appliedFilter != null && appliedFilter.equals(filter)) return;
            final List<GPUImageFilter> filters = filterGroup.getFilters();
            if (appliedFilter != null) {
                // remove applied filter from current filter list
                filters.remove(appliedFilter.getInstance());
            }
            // add the new filter
            filters.add(filter.getInstance());
            filterGroup = new GPUImageFilterGroup(filters);
            binding.preview.post(() -> binding.preview.setFilter(filterGroup));
            filtersAdapter.setSelected(position);
            appliedFilter = filter;
        };
        BitmapUtils.getThumbnail(
                context,
                sourceUri,
                CoroutineUtilsKt.getContinuation((bitmapResult, throwable) -> appExecutors.getMainThread().execute(() -> {
                    if (throwable != null) {
                        Log.e(TAG, "setupFilters: ", throwable);
                        return;
                    }
                    if (bitmapResult == null || bitmapResult.getBitmap() == null) {
                        return;
                    }
                    filtersAdapter = new FiltersAdapter(
                            tuningFilters.values()
                                         .stream()
                                         .map(Filter::getInstance)
                                         .collect(Collectors.toList()),
                            sourceUri.toString(),
                            bitmapResult.getBitmap(),
                            onFilterClickListener
                    );
                    binding.filters.setAdapter(filtersAdapter);
                    filtersAdapter.submitList(FiltersHelper.getFilters(), () -> {
                        if (appliedFilter == null) return;
                        filtersAdapter.setSelectedFilter(appliedFilter.getInstance());
                    });
                }), Dispatchers.getIO())
        );
        addInitialFilter();
        binding.preview.setFilter(filterGroup);
    }

    private void addTuneFilters() {
        if (initialTuningFiltersValues == null) return;
        final List<Filter<?>> tuneFilters = FiltersHelper.getTuneFilters();
        for (final Filter<?> tuneFilter : tuneFilters) {
            if (!initialTuningFiltersValues.containsKey(tuneFilter.getType())) continue;
            addFilterToGroup(tuneFilter, initialTuningFiltersValues.get(tuneFilter.getType()));
        }
    }

    private void addFilterToGroup(@NonNull final Filter<?> tuneFilter, final Map<Integer, Object> valueMap) {
        final GPUImageFilter filter = tuneFilter.getInstance();
        filterGroup.addFilter(filter);
        if (valueMap == null) return;
        final Set<Map.Entry<Integer, Object>> entries = valueMap.entrySet();
        for (final Map.Entry<Integer, Object> entry : entries) {
            tuneFilter.adjust(entry.getKey(), entry.getValue());
        }
    }

    public void setCurrentTab(final ImageEditViewModel.Tab tab) {
        viewModel.setCurrentTab(tab);
    }

    public void setCallback(final FilterCallback callback) {
        if (callback == null) return;
        this.callback = callback;
    }

    public interface FilterCallback {
        void onApply(Uri uri, List<Filter<?>> tuningFilters, Filter<?> filter);

        void onCancel();
    }
}

package awais.instagrabber.fragments.imageedit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;
import com.yalantis.ucrop.UCropFragment;
import com.yalantis.ucrop.UCropFragmentCallback;

import java.util.List;

import awais.instagrabber.R;
import awais.instagrabber.databinding.FragmentImageEditBinding;
import awais.instagrabber.fragments.imageedit.filters.filters.Filter;
import awais.instagrabber.models.SavedImageEditState;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.viewmodels.ImageEditViewModel;

public class ImageEditFragment extends Fragment {
    private static final String TAG = ImageEditFragment.class.getSimpleName();
    private static final String ARGS_URI = "uri";
    private static final String FILTERS_FRAGMENT_TAG = "Filters";

    private FragmentImageEditBinding binding;
    private ImageEditViewModel viewModel;
    private ImageEditViewModel.Tab previousTab;
    private FiltersFragment filtersFragment;

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            this.setEnabled(false);
            this.remove();
            if (ImageEditFragment.this.previousTab != ImageEditViewModel.Tab.CROP
                    && ImageEditFragment.this.previousTab != ImageEditViewModel.Tab.TUNE
                    && ImageEditFragment.this.previousTab != ImageEditViewModel.Tab.FILTERS) {
                return;
            }
            FragmentManager fragmentManager = ImageEditFragment.this.getChildFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.setReorderingAllowed(true)
                               .remove(ImageEditFragment.this.previousTab == ImageEditViewModel.Tab.CROP ? ImageEditFragment.this.uCropFragment : ImageEditFragment.this.filtersFragment)
                               .commit();
            ImageEditFragment.this.viewModel.setCurrentTab(ImageEditViewModel.Tab.RESULT);
        }
    };
    private FragmentActivity fragmentActivity;
    private UCropFragment uCropFragment;

    public static ImageEditFragment newInstance(Uri uri) {
        Bundle args = new Bundle();
        args.putParcelable(ImageEditFragment.ARGS_URI, uri);
        ImageEditFragment fragment = new ImageEditFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public ImageEditFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.fragmentActivity = this.getActivity();
        this.viewModel = new ViewModelProvider(this).get(ImageEditViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.binding = FragmentImageEditBinding.inflate(inflater, container, false);
        return this.binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        this.init();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void init() {
        this.setupObservers();
        Bundle arguments = this.getArguments();
        if (arguments == null) return;
        Parcelable parcelable = arguments.getParcelable(ImageEditFragment.ARGS_URI);
        Uri originalUri = null;
        if (parcelable instanceof Uri) {
            originalUri = (Uri) parcelable;
        }
        if (originalUri == null) return;
        this.viewModel.setOriginalUri(originalUri);
        this.viewModel.setCurrentTab(ImageEditViewModel.Tab.RESULT);
    }

    private void setupObservers() {
        this.viewModel.isLoading().observe(this.getViewLifecycleOwner(), loading -> {});
        this.viewModel.getCurrentTab().observe(this.getViewLifecycleOwner(), tab -> {
            if (tab == null) return;
            switch (tab) {
                case RESULT:
                    this.setupResult();
                    break;
                case CROP:
                    this.setupCropFragment();
                    break;
                case TUNE:
                case FILTERS:
                    this.setupFilterFragment();
                    break;
            }
            this.previousTab = tab;
        });
        this.viewModel.isCropped().observe(this.getViewLifecycleOwner(), isCropped -> this.binding.crop.setSelected(isCropped));
        this.viewModel.isTuned().observe(this.getViewLifecycleOwner(), isTuned -> this.binding.tune.setSelected(isTuned));
        this.viewModel.isFiltered().observe(this.getViewLifecycleOwner(), isFiltered -> this.binding.filters.setSelected(isFiltered));
        this.viewModel.getResultUri().observe(this.getViewLifecycleOwner(), uri -> {
            if (uri == null) {
                this.binding.preview.setController(null);
                return;
            }
            this.binding.preview.setController(Fresco.newDraweeControllerBuilder()
                                                .setImageRequest(ImageRequestBuilder.newBuilderWithSource(uri)
                                                                                    .disableDiskCache()
                                                                                    .disableMemoryCache()
                                                                                    .build())
                                                .build());
        });
    }

    private void setupResult() {
        this.binding.fragmentContainerView.setVisibility(View.GONE);
        this.binding.cropBottomControls.setVisibility(View.GONE);
        this.binding.preview.setVisibility(View.VISIBLE);
        this.binding.resultBottomControls.setVisibility(View.VISIBLE);
        this.binding.crop.setOnClickListener(v -> this.viewModel.setCurrentTab(ImageEditViewModel.Tab.CROP));
        this.binding.tune.setOnClickListener(v -> this.viewModel.setCurrentTab(ImageEditViewModel.Tab.TUNE));
        this.binding.filters.setOnClickListener(v -> this.viewModel.setCurrentTab(ImageEditViewModel.Tab.FILTERS));
        this.binding.cancel.setOnClickListener(v -> {
            this.viewModel.cancel();
            NavController navController = NavHostFragment.findNavController(this);
            this.setNavControllerResult(navController, null);
            navController.navigateUp();
        });
        this.binding.done.setOnClickListener(v -> {
            Context context = this.getContext();
            if (context == null) return;
            Uri resultUri = this.viewModel.getResultUri().getValue();
            if (resultUri == null) return;
            AppExecutors.INSTANCE.getMainThread().execute(() -> {
                NavController navController = NavHostFragment.findNavController(this);
                this.setNavControllerResult(navController, resultUri);
                navController.navigateUp();
            });
            // Utils.mediaScanFile(context, new File(resultUri.toString()), (path, uri) -> );
        });
    }

    private void setNavControllerResult(@NonNull NavController navController, Uri resultUri) {
        NavBackStackEntry navBackStackEntry = navController.getPreviousBackStackEntry();
        if (navBackStackEntry == null) return;
        SavedStateHandle savedStateHandle = navBackStackEntry.getSavedStateHandle();
        savedStateHandle.set("result", resultUri);
    }

    private void setupCropFragment() {
        Context context = this.getContext();
        if (context == null) return;
        this.binding.preview.setVisibility(View.GONE);
        this.binding.resultBottomControls.setVisibility(View.GONE);
        this.binding.fragmentContainerView.setVisibility(View.VISIBLE);
        this.binding.cropBottomControls.setVisibility(View.VISIBLE);
        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setFreeStyleCropEnabled(true);
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL);
        UCrop uCrop = UCrop.of(this.viewModel.getOriginalUri(), this.viewModel.getCropDestinationUri()).withOptions(options);
        SavedImageEditState savedState = this.viewModel.getSavedImageEditState();
        if (savedState != null && savedState.getCropImageMatrixValues() != null && savedState.getCropRect() != null) {
            uCrop.withSavedState(savedState.getCropImageMatrixValues(), savedState.getCropRect());
        }
        this.uCropFragment = uCrop.getFragment(uCrop.getIntent(context).getExtras());
        FragmentManager fragmentManager = this.getChildFragmentManager();
        this.uCropFragment.setCallback(new UCropFragmentCallback() {
            @Override
            public void loadingProgress(boolean showLoader) {
                Log.d(ImageEditFragment.TAG, "loadingProgress: " + showLoader);
            }

            @Override
            public void onCropFinish(UCropFragment.UCropResult result) {
                Log.d(ImageEditFragment.TAG, "onCropFinish: " + result.mResultCode);
                if (result.mResultCode == Activity.RESULT_OK) {
                    Intent resultData = result.mResultData;
                    Bundle extras = resultData.getExtras();
                    if (extras == null) return;
                    Object uri = extras.get(UCrop.EXTRA_OUTPUT_URI);
                    Object imageMatrixValues = extras.get(UCrop.EXTRA_IMAGE_MATRIX_VALUES);
                    Object cropRect = extras.get(UCrop.EXTRA_CROP_RECT);
                    if (uri instanceof Uri && imageMatrixValues instanceof float[] && cropRect instanceof RectF) {
                        Log.d(ImageEditFragment.TAG, "onCropFinish: result uri: " + uri);
                        ImageEditFragment.this.viewModel.setCropResult((float[]) imageMatrixValues, (RectF) cropRect);
                        ImageEditFragment.this.viewModel.setCurrentTab(ImageEditViewModel.Tab.RESULT);
                    }
                }
            }
        });
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setReorderingAllowed(true)
                           .replace(R.id.fragment_container_view, this.uCropFragment, UCropFragment.TAG)
                           .commit();
        if (!this.onBackPressedCallback.isEnabled()) {
            OnBackPressedDispatcher onBackPressedDispatcher = this.fragmentActivity.getOnBackPressedDispatcher();
            this.onBackPressedCallback.setEnabled(true);
            onBackPressedDispatcher.addCallback(this.getViewLifecycleOwner(), this.onBackPressedCallback);
        }
        this.binding.cropCancel.setOnClickListener(v -> this.onBackPressedCallback.handleOnBackPressed());
        this.binding.cropReset.setOnClickListener(v -> this.uCropFragment.reset());
        this.binding.cropDone.setOnClickListener(v -> this.uCropFragment.cropAndSaveImage());
    }

    private void setupFilterFragment() {
        this.binding.resultBottomControls.setVisibility(View.GONE);
        this.binding.preview.setVisibility(View.GONE);
        this.binding.cropBottomControls.setVisibility(View.GONE);
        this.binding.fragmentContainerView.setVisibility(View.VISIBLE);
        Boolean isCropped = this.viewModel.isCropped().getValue();
        Uri uri = isCropped != null && isCropped ? this.viewModel.getCropDestinationUri() : this.viewModel.getOriginalUri();
        ImageEditViewModel.Tab value = this.viewModel.getCurrentTab().getValue();
        SavedImageEditState savedImageEditState = this.viewModel.getSavedImageEditState();
        this.filtersFragment = FiltersFragment.newInstance(
                uri,
                this.viewModel.getDestinationUri(),
                savedImageEditState.getAppliedTuningFilters(),
                savedImageEditState.getAppliedFilter(),
                value == null ? ImageEditViewModel.Tab.TUNE : value
        );
        FragmentManager fragmentManager = this.getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setReorderingAllowed(true)
                           .replace(R.id.fragment_container_view, this.filtersFragment, ImageEditFragment.FILTERS_FRAGMENT_TAG)
                           .commit();
        if (!this.onBackPressedCallback.isEnabled()) {
            OnBackPressedDispatcher onBackPressedDispatcher = this.fragmentActivity.getOnBackPressedDispatcher();
            this.onBackPressedCallback.setEnabled(true);
            onBackPressedDispatcher.addCallback(this.getViewLifecycleOwner(), this.onBackPressedCallback);
        }
        this.filtersFragment.setCallback(new FiltersFragment.FilterCallback() {
            @Override
            public void onApply(Uri uri, List<Filter<?>> tuningFilters, Filter<?> filter) {
                ImageEditFragment.this.viewModel.setAppliedFilters(tuningFilters, filter);
                ImageEditFragment.this.viewModel.setCurrentTab(ImageEditViewModel.Tab.RESULT);
            }

            @Override
            public void onCancel() {
                ImageEditFragment.this.onBackPressedCallback.handleOnBackPressed();
            }
        });
    }
}

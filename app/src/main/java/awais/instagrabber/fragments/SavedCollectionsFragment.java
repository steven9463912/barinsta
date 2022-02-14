package awais.instagrabber.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.adapters.SavedCollectionsAdapter;
import awais.instagrabber.customviews.helpers.GridSpacingItemDecoration;
import awais.instagrabber.databinding.FragmentSavedCollectionsBinding;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.Utils;
import awais.instagrabber.viewmodels.SavedCollectionsViewModel;
import awais.instagrabber.webservices.ProfileRepository;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class SavedCollectionsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = SavedCollectionsFragment.class.getSimpleName();
    public static boolean pleaseRefresh;

    private MainActivity fragmentActivity;
    private CoordinatorLayout root;
    private FragmentSavedCollectionsBinding binding;
    private SavedCollectionsViewModel savedCollectionsViewModel;
    private boolean shouldRefresh = true;
    private boolean isSaving;
    private ProfileRepository profileRepository;
    private SavedCollectionsAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.fragmentActivity = (MainActivity) this.requireActivity();
        this.profileRepository = ProfileRepository.Companion.getInstance();
        this.savedCollectionsViewModel = new ViewModelProvider(this.fragmentActivity).get(SavedCollectionsViewModel.class);
        this.setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        if (this.root != null) {
            this.shouldRefresh = false;
            return this.root;
        }
        this.binding = FragmentSavedCollectionsBinding.inflate(inflater, container, false);
        this.root = this.binding.getRoot();
        return this.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        this.setupObservers();
        if (!this.shouldRefresh) return;
        this.binding.swipeRefreshLayout.setOnRefreshListener(this);
        this.init();
        this.shouldRefresh = false;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.saved_collection_menu, menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (SavedCollectionsFragment.pleaseRefresh) this.onRefresh();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.add) {
            Context context = this.getContext();
            EditText input = new EditText(context);
            new AlertDialog.Builder(context)
                    .setTitle(R.string.saved_create_collection)
                    .setView(input)
                    .setPositiveButton(R.string.confirm, (d, w) -> {
                        String cookie = settingsHelper.getString(Constants.COOKIE);
                        this.profileRepository.createCollection(
                                input.getText().toString(),
                                settingsHelper.getString(Constants.DEVICE_UUID),
                                CookieUtils.getUserIdFromCookie(cookie),
                                CookieUtils.getCsrfTokenFromCookie(cookie),
                                CoroutineUtilsKt.getContinuation((result, t) -> {
                                    if (t != null) {
                                        Log.e(SavedCollectionsFragment.TAG, "Error creating collection", t);
                                        Toast.makeText(context, t.getMessage(), Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    this.onRefresh();
                                })
                        );
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        }
        return false;
    }

    private void init() {
        this.setupTopics();
        this.fetchTopics(null);
        SavedCollectionsFragmentArgs fragmentArgs = SavedCollectionsFragmentArgs.fromBundle(this.getArguments());
        this.isSaving = fragmentArgs.getIsSaving();
    }

    @Override
    public void onRefresh() {
        this.fetchTopics(null);
    }

    public void setupTopics() {
        this.binding.topicsRecyclerView.addItemDecoration(new GridSpacingItemDecoration(Utils.convertDpToPx(2)));
        this.adapter = new SavedCollectionsAdapter((topicCluster, root, cover, title, titleColor, backgroundColor) -> {
            NavController navController = NavHostFragment.findNavController(this);
            if (this.isSaving) {
                this.setNavControllerResult(navController, topicCluster.getCollectionId());
                navController.navigateUp();
            } else {
                try {
                    FragmentNavigator.Extras.Builder builder = new FragmentNavigator.Extras.Builder()
                            .addSharedElement(cover, "collection-" + topicCluster.getCollectionId());
                    NavDirections action = SavedCollectionsFragmentDirections
                            .actionToCollectionPosts(topicCluster, titleColor, backgroundColor);
                    navController.navigate(action, builder.build());
                } catch (final Exception e) {
                    Log.e(SavedCollectionsFragment.TAG, "setupTopics: ", e);
                }
            }
        });
        this.binding.topicsRecyclerView.setAdapter(this.adapter);
    }

    private void setupObservers() {
        this.savedCollectionsViewModel.getList().observe(this.getViewLifecycleOwner(), list -> {
            if (this.adapter == null) return;
            this.adapter.submitList(list);
        });
    }

    private void fetchTopics(String maxId) {
        this.binding.swipeRefreshLayout.setRefreshing(true);
        this.profileRepository.fetchCollections(maxId, CoroutineUtilsKt.getContinuation((result, t) -> {
            if (t != null) {
                Log.e(SavedCollectionsFragment.TAG, "onFailure", t);
                this.binding.swipeRefreshLayout.setRefreshing(false);
                return;
            }
            if (result == null) return;
            this.savedCollectionsViewModel.getList().postValue(result.getItems());
            this.binding.swipeRefreshLayout.setRefreshing(false);
        }));
    }

    private void setNavControllerResult(@NonNull NavController navController, String result) {
        NavBackStackEntry navBackStackEntry = navController.getPreviousBackStackEntry();
        if (navBackStackEntry == null) return;
        SavedStateHandle savedStateHandle = navBackStackEntry.getSavedStateHandle();
        savedStateHandle.set("collection", result);
    }
}

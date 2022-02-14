package awais.instagrabber.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FiltersFragmentViewModel extends ViewModel {
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<ImageEditViewModel.Tab> currentTab = new MutableLiveData<>();

    public FiltersFragmentViewModel() {
    }

    public LiveData<Boolean> isLoading() {
        return this.loading;
    }

    public LiveData<ImageEditViewModel.Tab> getCurrentTab() {
        return this.currentTab;
    }

    public void setCurrentTab(ImageEditViewModel.Tab tab) {
        if (tab == null) return;
        this.currentTab.postValue(tab);
    }
}

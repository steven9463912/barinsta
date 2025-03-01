package awais.instagrabber.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import awais.instagrabber.repositories.responses.saved.SavedCollection;

public class SavedCollectionsViewModel extends ViewModel {
    private MutableLiveData<List<SavedCollection>> list;

    public MutableLiveData<List<SavedCollection>> getList() {
        if (this.list == null) {
            this.list = new MutableLiveData<>();
        }
        return this.list;
    }
}

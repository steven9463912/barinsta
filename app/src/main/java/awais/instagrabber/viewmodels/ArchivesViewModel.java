package awais.instagrabber.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import awais.instagrabber.repositories.responses.stories.Story;

public class ArchivesViewModel extends ViewModel {
    private MutableLiveData<List<Story>> list;

    public MutableLiveData<List<Story>> getList() {
        if (this.list == null) {
            this.list = new MutableLiveData<>();
        }
        return this.list;
    }
}
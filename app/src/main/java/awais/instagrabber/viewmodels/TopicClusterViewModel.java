package awais.instagrabber.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import awais.instagrabber.repositories.responses.discover.TopicCluster;

public class TopicClusterViewModel extends ViewModel {
    private MutableLiveData<List<TopicCluster>> list;

    public MutableLiveData<List<TopicCluster>> getList() {
        if (this.list == null) {
            this.list = new MutableLiveData<>();
        }
        return this.list;
    }
}

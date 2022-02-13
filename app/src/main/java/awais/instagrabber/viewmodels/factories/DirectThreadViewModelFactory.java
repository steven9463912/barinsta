package awais.instagrabber.viewmodels.factories;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.viewmodels.DirectThreadViewModel;

public class DirectThreadViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final String threadId;
    private final boolean pending;
    private final User currentUser;

    public DirectThreadViewModelFactory(@NonNull Application application,
                                        @NonNull String threadId,
                                        boolean pending,
                                        @NonNull User currentUser) {
        this.application = application;
        this.threadId = threadId;
        this.pending = pending;
        this.currentUser = currentUser;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        //noinspection unchecked
        return (T) new DirectThreadViewModel(this.application, this.threadId, this.pending, this.currentUser);
    }
}

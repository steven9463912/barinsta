package awais.instagrabber.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import awais.instagrabber.db.repositories.AccountRepository;
import awais.instagrabber.models.Resource;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.webservices.UserRepository;
import kotlinx.coroutines.Dispatchers;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class AppStateViewModel extends AndroidViewModel {
    private static final String TAG = AppStateViewModel.class.getSimpleName();

    private final String cookie;
    private final MutableLiveData<Resource<User>> currentUser = new MutableLiveData<>(Resource.loading(null));

    private AccountRepository accountRepository;

    private UserRepository userRepository;

    public AppStateViewModel(@NonNull Application application) {
        super(application);
        // Log.d(TAG, "AppStateViewModel: constructor");
        this.cookie = settingsHelper.getString(Constants.COOKIE);
        boolean isLoggedIn = !TextUtils.isEmpty(this.cookie) && CookieUtils.getUserIdFromCookie(this.cookie) != 0;
        if (!isLoggedIn) {
            this.currentUser.postValue(Resource.success(null));
            return;
        }
        this.userRepository = UserRepository.Companion.getInstance();
        this.accountRepository = AccountRepository.Companion.getInstance(application);
        this.fetchProfileDetails();
    }

    @Nullable
    public Resource<User> getCurrentUser() {
        return this.currentUser.getValue();
    }

    public LiveData<Resource<User>> getCurrentUserLiveData() {
        return this.currentUser;
    }

    public void fetchProfileDetails() {
        this.currentUser.postValue(Resource.loading(null));
        long uid = CookieUtils.getUserIdFromCookie(this.cookie);
        if (uid == 0L) {
            this.currentUser.postValue(Resource.success(null));
            return;
        }
        this.userRepository.getUserInfo(uid, CoroutineUtilsKt.getContinuation((user, throwable) -> {
            if (throwable != null) {
                Log.e(AppStateViewModel.TAG, "onFailure: ", throwable);
                Resource<User> userResource = this.currentUser.getValue();
                User backup = userResource != null && userResource.data != null ? userResource.data : new User(uid);
                this.currentUser.postValue(Resource.error(throwable.getMessage(), backup));
                return;
            }
            this.currentUser.postValue(Resource.success(user));
            if (this.accountRepository != null && user != null) {
                this.accountRepository.insertOrUpdateAccount(
                        user.getPk(),
                        user.getUsername(),
                        this.cookie,
                        user.getFullName() != null ? user.getFullName() : "",
                        user.getProfilePicUrl(),
                        CoroutineUtilsKt.getContinuation((account, throwable1) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                            if (throwable1 != null) {
                                Log.e(AppStateViewModel.TAG, "updateAccountInfo: ", throwable1);
                            }
                        }), Dispatchers.getIO())
                );
            }
        }, Dispatchers.getIO()));
    }
}

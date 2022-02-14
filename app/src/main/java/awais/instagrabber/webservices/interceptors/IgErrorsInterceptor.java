package awais.instagrabber.webservices.interceptors;

import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.dialogs.ConfirmDialogFragment;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.TextUtils;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class IgErrorsInterceptor implements Interceptor {
    private static final String TAG = IgErrorsInterceptor.class.getSimpleName();

    public IgErrorsInterceptor() { }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        if (response.isSuccessful()) {
            return response;
        }
        this.checkError(response);
        return response;
    }

    private void checkError(@NonNull Response response) {
        int errorCode = response.code();
        switch (errorCode) {
            case 429: // "429 Too Many Requests"
                // ('Throttled by Instagram because of too many API requests.');
                this.showErrorDialog(R.string.throttle_error);
                return;
            case 431: // "431 Request Header Fields Too Large"
                // show dialog?
                Log.e(IgErrorsInterceptor.TAG, "Network error: " + this.getMessage(errorCode, "The request start-line and/or headers are too large to process."));
                return;
            case 404:
                this.showErrorDialog(R.string.not_found);
                return;
            case 302: // redirect
                String location = response.header("location");
                if ("https://www.instagram.com/accounts/login/".equals(location)) {
                    // rate limited
                    String message = MainActivity.getInstance().getString(R.string.rate_limit);
                    Spanned spanned = Html.fromHtml(message);
                    this.showErrorDialog(spanned);
                }
                return;
        }
        ResponseBody body = response.body();
        if (body == null) return;
        try {
            String bodyString = body.string();
            Log.d(IgErrorsInterceptor.TAG, "checkError: " + bodyString);
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(bodyString);
            } catch (final JSONException e) {
                Log.e(IgErrorsInterceptor.TAG, "checkError: ", e);
            }
            String message;
            if (jsonObject != null) {
                message = jsonObject.optString("message");
            } else {
                message = bodyString;
            }
            if (!TextUtils.isEmpty(message)) {
                message = message.toLowerCase();
                switch (message) {
                    case "user_has_logged_out":
                        this.showErrorDialog(R.string.account_logged_out);
                        return;
                    case "login_required":
                        this.showErrorDialog(R.string.login_required);
                        return;
                    case "execution failure":
                        this.showSnackbar(message);
                        return;
                    case "not authorized to view user": // Do we handle this in profile view fragment?
                    case "challenge_required": // Since we make users login using browser, we should not be getting this error in api requests
                    default:
                        this.showSnackbar(message);
                        Log.e(IgErrorsInterceptor.TAG, "checkError: " + bodyString);
                        return;
                }
            }
            String errorType = jsonObject.optString("error_type");
            if (TextUtils.isEmpty(errorType)) return;
            if (errorType.equals("sentry_block")) {
                this.showErrorDialog("\"sentry_block\". Please contact developers.");
                return;
            }
            if (errorType.equals("inactive user")) {
                this.showErrorDialog(R.string.inactive_user);
            }
        } catch (final Exception e) {
            Log.e(IgErrorsInterceptor.TAG, "checkError: ", e);
        }
    }

    private void showSnackbar(String message) {
        MainActivity mainActivity = MainActivity.getInstance();
        if (mainActivity == null) return;
        // final View view = mainActivity.getRootView();
        // if (view == null) return;
        try {
            AppExecutors.INSTANCE
                    .getMainThread()
                    .execute(() -> Toast.makeText(mainActivity.getApplicationContext(), message, Toast.LENGTH_LONG).show());
        } catch (final Exception e) {
            Log.e(IgErrorsInterceptor.TAG, "showSnackbar: ", e);
        }
    }

    @NonNull
    private String getMessage(int errorCode, String message) {
        return String.format("code: %s, internalMessage: %s", errorCode, message);
    }

    private void showErrorDialog(@NonNull CharSequence message) {
        MainActivity mainActivity = MainActivity.getInstance();
        if (mainActivity == null) return;
        FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
        if (fragmentManager.isStateSaved()) return;
        ConfirmDialogFragment dialogFragment = ConfirmDialogFragment.newInstance(
                Constants.GLOBAL_NETWORK_ERROR_DIALOG_REQUEST_CODE,
                R.string.error,
                message,
                R.string.ok,
                0,
                0
        );
        dialogFragment.show(fragmentManager, "network_error_dialog");
    }

    private void showErrorDialog(@StringRes int messageResId) {
        this.showErrorDialog(MainActivity.getInstance().getString(messageResId));
    }

    public void destroy() {
        // mainActivity = null;
    }
}
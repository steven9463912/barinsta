package awais.instagrabber.webservices.interceptors;

import androidx.annotation.NonNull;

import java.io.IOException;

import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.LocaleUtils;
import awais.instagrabber.utils.TextUtils;
import awais.instagrabber.utils.Utils;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AddCookiesInterceptor implements Interceptor {
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Request.Builder builder = request.newBuilder();
        String cookie = Utils.settingsHelper.getString(Constants.COOKIE);
        boolean hasCookie = !TextUtils.isEmpty(cookie);
        if (hasCookie) {
            builder.addHeader("Cookie", cookie);
        }
        final String userAgentHeader = "User-Agent";
        if (request.header(userAgentHeader) == null) {
            builder.addHeader(userAgentHeader, Utils.settingsHelper.getString(hasCookie ? Constants.APP_UA : Constants.BROWSER_UA));
        }
        final String languageHeader = "Accept-Language";
        if (request.header(languageHeader) == null) {
            builder.addHeader(languageHeader, LocaleUtils.getCurrentLocale().getLanguage() + ",en-US;q=0.8");
        }
        Request updatedRequest = builder.build();
        return chain.proceed(updatedRequest);
    }
}
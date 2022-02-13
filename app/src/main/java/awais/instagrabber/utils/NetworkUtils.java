package awais.instagrabber.utils;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Set;

public final class NetworkUtils {
    @NonNull
    public static String readFromConnection(@NonNull HttpURLConnection conn) throws Exception {
        InputStream inputStream = conn.getInputStream();
        return NetworkUtils.readFromInputStream(inputStream);
    }

    @NonNull
    public static String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    public static void setConnectionHeaders(HttpURLConnection connection, Map<String, String> headers) {
        if (connection == null || headers == null || headers.isEmpty()) {
            return;
        }
        for (final Map.Entry<String, String> header : headers.entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
    }

    public static String getQueryString(Map<String, String> queryParamsMap) {
        if (queryParamsMap == null || queryParamsMap.isEmpty()) {
            return "";
        }
        Set<Map.Entry<String, String>> params = queryParamsMap.entrySet();
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> param : params) {
            if (TextUtils.isEmpty(param.getKey())) {
                continue;
            }
            if (builder.length() != 0) {
                builder.append("&");
            }
            builder.append(param.getKey());
            builder.append("=");
            builder.append(param.getValue() != null ? param.getValue() : "");
        }
        return builder.toString();
    }
}

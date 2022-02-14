package awais.instagrabber.repositories.responses.notification;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import awais.instagrabber.utils.TextUtils;

public class NotificationArgs {
    private final String text;
    private final String richText;
    private final long profileId;
    private final String profileImage;
    private final List<NotificationImage> media;
    private final double timestamp;
    private final String profileName;
    private final String fullName; // for AYML, not naturally generated
    private final boolean isVerified; // mostly for AYML, not sure about notif

    public NotificationArgs(String text,
                            String richText, // for AYML, this is the algorithm
                            long profileId,
                            String profileImage,
                            List<NotificationImage> media,
                            double timestamp,
                            String profileName,
                            String fullName,
                            boolean isVerified) {
        this.text = text;
        this.richText = richText;
        this.profileId = profileId;
        this.profileImage = profileImage;
        this.media = media;
        this.timestamp = timestamp;
        this.profileName = profileName;
        this.fullName = fullName;
        this.isVerified = isVerified;
    }

    public String getText() {
        return this.text == null ? this.cleanRichText(this.richText) : this.text;
    }

    public long getUserId() {
        return this.profileId;
    }

    public String getProfilePic() {
        return this.profileImage;
    }

    public String getUsername() {
        return this.profileName;
    }

    public String getFullName() {
        return this.fullName;
    }

    public List<NotificationImage> getMedia() {
        return this.media;
    }

    public double getTimestamp() {
        return this.timestamp;
    }

    public boolean isVerified() {
        return this.isVerified;
    }

    @NonNull
    public String getDateTime() {
        return TextUtils.epochSecondToString(Math.round(this.timestamp));
    }

    private String cleanRichText(String raw) {
        if (raw == null) return null;
        Matcher matcher = Pattern.compile("\\{[\\p{L}\\d._]+\\|000000\\|1\\|user\\?id=\\d+\\}").matcher(raw);
        String result = raw;
        while (matcher.find()) {
            String richObject = raw.substring(matcher.start(), matcher.end());
            String username = richObject.split("\\|")[0].substring(1);
            result = result.replace(richObject, username);
        }
        return result;
    }
}

package awais.instagrabber.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import awais.instagrabber.R;
import awais.instagrabber.activities.MainActivity;
import awais.instagrabber.db.datasources.DMLastNotifiedDataSource;
import awais.instagrabber.db.entities.DMLastNotified;
import awais.instagrabber.db.repositories.DMLastNotifiedRepository;
import awais.instagrabber.fragments.settings.PreferenceKeys;
import awais.instagrabber.managers.DirectMessagesManager;
import awais.instagrabber.managers.InboxManager;
import awais.instagrabber.models.Resource;
import awais.instagrabber.repositories.responses.directmessages.DirectInbox;
import awais.instagrabber.repositories.responses.directmessages.DirectItem;
import awais.instagrabber.repositories.responses.directmessages.DirectThread;
import awais.instagrabber.repositories.responses.directmessages.DirectThreadLastSeenAt;
import awais.instagrabber.utils.AppExecutors;
import awais.instagrabber.utils.Constants;
import awais.instagrabber.utils.CookieUtils;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.utils.DMUtils;
import awais.instagrabber.utils.DateUtils;
import awais.instagrabber.utils.TextUtils;
import kotlinx.coroutines.Dispatchers;

import static awais.instagrabber.utils.Utils.settingsHelper;

public class DMSyncService extends LifecycleService {
    private static final String TAG = DMSyncService.class.getSimpleName();

    private InboxManager inboxManager;
    private DMLastNotifiedRepository dmLastNotifiedRepository;
    private Map<String, DMLastNotified> dmLastNotifiedMap;

    @Override
    public void onCreate() {
        super.onCreate();
        this.startForeground(Constants.DM_CHECK_NOTIFICATION_ID, this.buildForegroundNotification());
        Log.d(DMSyncService.TAG, "onCreate: Service created");
        DirectMessagesManager directMessagesManager = DirectMessagesManager.INSTANCE;
        this.inboxManager = directMessagesManager.getInboxManager();
        Context context = this.getApplicationContext();
        if (context == null) return;
        this.dmLastNotifiedRepository = DMLastNotifiedRepository.getInstance(DMLastNotifiedDataSource.getInstance(context));
    }

    private void parseUnread(@NonNull DirectInbox directInbox) {
        this.dmLastNotifiedRepository.getAllDMDmLastNotified(
                CoroutineUtilsKt.getContinuation((result, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                    if (throwable != null) {
                        Log.e(DMSyncService.TAG, "parseUnread: ", throwable);
                        this.dmLastNotifiedMap = Collections.emptyMap();
                        this.parseUnreadActual(directInbox);
                        return;
                    }
                    this.dmLastNotifiedMap = result != null
                                        ? result.stream().collect(Collectors.toMap(DMLastNotified::getThreadId, Function.identity()))
                                        : Collections.emptyMap();
                    this.parseUnreadActual(directInbox);
                }), Dispatchers.getIO())
        );
        // Log.d(TAG, "inbox observer: " + directInbox);
    }

    private void parseUnreadActual(@NonNull DirectInbox directInbox) {
        List<DirectThread> threads = directInbox.getThreads();
        ImmutableMap.Builder<String, List<DirectItem>> unreadMessagesMapBuilder = ImmutableMap.builder();
        if (threads == null) {
            this.stopSelf();
            return;
        }
        for (DirectThread thread : threads) {
            if (thread.getMuted()) continue;
            boolean read = DMUtils.isRead(thread);
            if (read) continue;
            List<DirectItem> unreadMessages = this.getUnreadMessages(thread);
            if (unreadMessages.isEmpty()) continue;
            unreadMessagesMapBuilder.put(thread.getThreadId(), unreadMessages);
        }
        Map<String, List<DirectItem>> unreadMessagesMap = unreadMessagesMapBuilder.build();
        if (unreadMessagesMap.isEmpty()) {
            this.stopSelf();
            return;
        }
        this.showNotification(directInbox, unreadMessagesMap);
        LocalDateTime now = LocalDateTime.now();
        // Update db
        ImmutableList.Builder<DMLastNotified> lastNotifiedListBuilder = ImmutableList.builder();
        for (Map.Entry<String, List<DirectItem>> unreadMessagesEntry : unreadMessagesMap.entrySet()) {
            List<DirectItem> unreadItems = unreadMessagesEntry.getValue();
            DirectItem latestItem = unreadItems.get(unreadItems.size() - 1);
            lastNotifiedListBuilder.add(new DMLastNotified(0,
                                                           unreadMessagesEntry.getKey(),
                                                           latestItem.getDate(),
                                                           now));
        }
        this.dmLastNotifiedRepository.insertOrUpdateDMLastNotified(
                lastNotifiedListBuilder.build(),
                CoroutineUtilsKt.getContinuation((unit, throwable) -> AppExecutors.INSTANCE.getMainThread().execute(() -> {
                    try {
                        if (throwable != null) {
                            Log.e(DMSyncService.TAG, "parseUnreadActual: ", throwable);
                        }
                    } finally {
                        this.stopSelf();
                    }
                }), Dispatchers.getIO())
        );
    }

    @NonNull
    private List<DirectItem> getUnreadMessages(@NonNull DirectThread thread) {
        List<DirectItem> items = thread.getItems();
        if (items == null) return Collections.emptyList();
        DMLastNotified dmLastNotified = this.dmLastNotifiedMap.get(thread.getThreadId());
        long viewerId = thread.getViewerId();
        Map<Long, DirectThreadLastSeenAt> lastSeenAt = thread.getLastSeenAt();
        ImmutableList.Builder<DirectItem> unreadListBuilder = ImmutableList.builder();
        int count = 0;
        for (DirectItem item : items) {
            if (item == null) continue;
            if (item.getUserId() == viewerId) break; // Reached a message from the viewer, it is assumed the viewer has read the next messages
            boolean read = DMUtils.isRead(item, lastSeenAt, Collections.singletonList(viewerId));
            if (read) break;
            if (dmLastNotified != null && dmLastNotified.getLastNotifiedMsgTs() != null && item.getDate() != null) {
                if (count == 0 && DateUtils.isBeforeOrEqual(item.getDate(), dmLastNotified.getLastNotifiedMsgTs())) {
                    // The first unread item has been notified and hence all subsequent items can be ignored
                    // since the items are in desc timestamp order
                    break;
                }
            }
            unreadListBuilder.add(item);
            count++;
            // Inbox style notification only allows 6 lines
            if (count >= 6) break;
        }
        // Reversing, so that oldest messages are on top
        return unreadListBuilder.build().reverse();
    }

    private void showNotification(DirectInbox directInbox,
                                  Map<String, List<DirectItem>> unreadMessagesMap) {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;
        for (Map.Entry<String, List<DirectItem>> unreadMessagesEntry : unreadMessagesMap.entrySet()) {
            Optional<DirectThread> directThreadOptional = this.getThread(directInbox, unreadMessagesEntry.getKey());
            if (!directThreadOptional.isPresent()) continue;
            DirectThread thread = directThreadOptional.get();
            DirectItem firstDirectItem = thread.getFirstDirectItem();
            if (firstDirectItem == null) continue;
            List<DirectItem> unreadMessages = unreadMessagesEntry.getValue();
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle(thread.getThreadTitle());
            for (DirectItem item : unreadMessages) {
                inboxStyle.addLine(DMUtils.getMessageString(thread, this.getResources(), thread.getViewerId(), item));
            }
            Notification notification = new NotificationCompat.Builder(this, Constants.DM_UNREAD_CHANNEL_ID)
                    .setStyle(inboxStyle)
                    .setSmallIcon(R.drawable.ic_round_mode_comment_24)
                    .setContentTitle(thread.getThreadTitle())
                    .setContentText(DMUtils.getMessageString(thread, this.getResources(), thread.getViewerId(), firstDirectItem))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setGroup(Constants.GROUP_KEY_DM)
                    .setAutoCancel(true)
                    .setContentIntent(this.getThreadPendingIntent(thread.getThreadId(), thread.getThreadTitle()))
                    .build();
            notificationManager.notify(Constants.DM_UNREAD_PARENT_NOTIFICATION_ID, notification);
        }
    }

    private Optional<DirectThread> getThread(@NonNull DirectInbox directInbox, String threadId) {
        return directInbox.getThreads()
                          .stream()
                          .filter(thread -> Objects.equals(thread.getThreadId(), threadId))
                          .findFirst();
    }

    @NonNull
    private PendingIntent getThreadPendingIntent(String threadId, String threadTitle) {
        Intent intent = new Intent(this.getApplicationContext(), MainActivity.class)
                .setAction(Constants.ACTION_SHOW_DM_THREAD)
                .putExtra(Constants.DM_THREAD_ACTION_EXTRA_THREAD_ID, threadId)
                .putExtra(Constants.DM_THREAD_ACTION_EXTRA_THREAD_TITLE, threadTitle)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(this.getApplicationContext(), Constants.SHOW_DM_THREAD, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        String cookie = settingsHelper.getString(Constants.COOKIE);
        boolean isLoggedIn = !TextUtils.isEmpty(cookie) && CookieUtils.getUserIdFromCookie(cookie) != 0;
        if (!isLoggedIn) {
            this.stopSelf();
            return Service.START_NOT_STICKY;
        }
        // Need to setup here if service was started by the boot completed receiver
        CookieUtils.setupCookies(cookie);
        boolean notificationsEnabled = settingsHelper.getBoolean(PreferenceKeys.PREF_ENABLE_DM_NOTIFICATIONS);
        this.inboxManager.getInbox().observe(this, inboxResource -> {
            if (!notificationsEnabled || inboxResource == null || inboxResource.status != Resource.Status.SUCCESS) {
                this.stopSelf();
                return;
            }
            DirectInbox directInbox = inboxResource.data;
            if (directInbox == null) {
                this.stopSelf();
                return;
            }
            this.parseUnread(directInbox);
        });
        Log.d(DMSyncService.TAG, "onStartCommand: refreshing inbox");
        // inboxManager.refresh();
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        super.onBind(intent);
        return null;
    }

    private Notification buildForegroundNotification() {
        Resources resources = this.getResources();
        return new NotificationCompat.Builder(this, Constants.SILENT_NOTIFICATIONS_CHANNEL_ID)
                .setOngoing(true)
                .setSound(null)
                .setContentTitle(resources.getString(R.string.app_name))
                .setContentText(resources.getString(R.string.checking_for_new_messages))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setGroup(Constants.GROUP_KEY_SILENT_NOTIFICATIONS)
                .build();
    }
}
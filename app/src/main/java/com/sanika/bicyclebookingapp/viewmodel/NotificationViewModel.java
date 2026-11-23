package com.sanika.bicyclebookingapp.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.sanika.bicyclebookingapp.model.AppNotification;
import com.sanika.bicyclebookingapp.model.NotificationPreferences;
import com.sanika.bicyclebookingapp.repository.NotificationRepository;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.List;

/**
 * ViewModel for In-App Notification Center, Unread Badges, and Alert Preferences.
 */
public class NotificationViewModel extends ViewModel {

    private final NotificationRepository repository;

    public NotificationViewModel() {
        this.repository = new NotificationRepository();
    }

    public NotificationViewModel(NotificationRepository repository) {
        this.repository = repository;
    }

    public void dispatchNotification(Context context, AppNotification notification) {
        repository.dispatchNotification(context, notification);
    }

    public LiveData<Resource<List<AppNotification>>> getCustomerNotifications(String userId) {
        return repository.getCustomerNotifications(userId);
    }

    public LiveData<Resource<List<AppNotification>>> getAdminNotifications() {
        return repository.getAdminNotifications();
    }

    public void markAsRead(String notificationId) {
        repository.markAsRead(notificationId);
    }

    public void markAllAsRead(String userId) {
        repository.markAllAsRead(userId);
    }

    public LiveData<Integer> getUnreadCount(String userId) {
        return repository.getUnreadCount(userId);
    }

    public LiveData<NotificationPreferences> getPreferences(String userId) {
        return repository.getPreferences(userId);
    }

    public void savePreferences(String userId, NotificationPreferences prefs) {
        repository.savePreferences(userId, prefs);
    }
}

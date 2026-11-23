package com.sanika.bicyclebookingapp.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.sanika.bicyclebookingapp.model.AdminSettings;
import com.sanika.bicyclebookingapp.model.AppNotification;
import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.Coupon;
import com.sanika.bicyclebookingapp.model.Payment;
import com.sanika.bicyclebookingapp.model.Refund;
import com.sanika.bicyclebookingapp.model.SupportTicket;
import com.sanika.bicyclebookingapp.model.User;
import com.sanika.bicyclebookingapp.util.CurrencyUtil;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central Repository for Admin Dashboard, Business Analytics, and Module Management.
 * Queries live Firebase database collections to compute genuine business metrics.
 */
public class AdminRepository {

    private static final String USERS_COLLECTION          = "users";
    private static final String BICYCLES_COLLECTION       = "bicycles";
    private static final String STATIONS_COLLECTION       = "stations";
    private static final String BOOKINGS_COLLECTION       = "bookings";
    private static final String PAYMENTS_COLLECTION       = "payments";
    private static final String REFUNDS_COLLECTION        = "refunds";
    private static final String COUPONS_COLLECTION        = "coupons";
    private static final String SUPPORT_COLLECTION        = "support_tickets";
    private static final String ADMIN_SETTINGS_COLLECTION = "admin_settings";

    private final FirebaseFirestore db;

    private static volatile AdminRepository instance;

    public static AdminRepository getInstance() {
        if (instance == null) {
            synchronized (AdminRepository.class) {
                if (instance == null) {
                    instance = new AdminRepository();
                }
            }
        }
        return instance;
    }

    public AdminRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Aggregates real-time business metrics across all database collections.
     */
    public LiveData<Resource<Map<String, Object>>> getDashboardMetrics() {
        MutableLiveData<Resource<Map<String, Object>>> liveData = new MutableLiveData<>();
        liveData.postValue(Resource.loading(null));

        Map<String, Object> metrics = new HashMap<>();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startOfToday = cal.getTime();

        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date startOfMonth = cal.getTime();

        // 1. Fetch Users
        db.collection(USERS_COLLECTION).get().addOnSuccessListener(userSnaps -> {
            int totalCustomers = 0;
            for (DocumentSnapshot doc : userSnaps.getDocuments()) {
                String role = doc.getString("role");
                if (role == null || User.ROLE_USER.equalsIgnoreCase(role)) {
                    totalCustomers++;
                }
            }
            metrics.put("totalCustomers", totalCustomers);

            // 2. Fetch Bicycles
            db.collection(BICYCLES_COLLECTION).get().addOnSuccessListener(bikeSnaps -> {
                int totalBikes = bikeSnaps.size();
                int availableBikes = 0;
                int reservedBikes = 0;
                int inUseBikes = 0;
                int maintenanceBikes = 0;

                for (DocumentSnapshot doc : bikeSnaps.getDocuments()) {
                    String status = doc.getString("status");
                    if (status != null) {
                        switch (status.toUpperCase()) {
                            case Bicycle.STATUS_AVAILABLE:
                                availableBikes++;
                                break;
                            case Bicycle.STATUS_RESERVED:
                                reservedBikes++;
                                break;
                            case Bicycle.STATUS_IN_USE:
                                inUseBikes++;
                                break;
                            case Bicycle.STATUS_MAINTENANCE:
                            case Bicycle.STATUS_DAMAGED:
                                maintenanceBikes++;
                                break;
                        }
                    }
                }

                metrics.put("totalBicycles", totalBikes);
                metrics.put("availableBicycles", availableBikes);
                metrics.put("reservedBicycles", reservedBikes);
                metrics.put("inUseBicycles", inUseBikes);
                metrics.put("maintenanceBicycles", maintenanceBikes);

                // 3. Fetch Bookings
                db.collection(BOOKINGS_COLLECTION).get().addOnSuccessListener(bookingSnaps -> {
                    int totalBookings = bookingSnaps.size();
                    int todayBookings = 0;
                    int activeRides = 0;
                    int completedRides = 0;
                    int cancelledBookings = 0;

                    Map<String, Integer> stationUsage = new HashMap<>();
                    Map<String, Integer> bikeUsage = new HashMap<>();

                    for (DocumentSnapshot doc : bookingSnaps.getDocuments()) {
                        String status = doc.getString("status");
                        Date createdAt = doc.getDate("createdAt");

                        if (createdAt != null && !createdAt.before(startOfToday)) {
                            todayBookings++;
                        }

                        if (status != null) {
                            switch (status.toUpperCase()) {
                                case Booking.STATUS_ACTIVE:
                                    activeRides++;
                                    break;
                                case Booking.STATUS_COMPLETED:
                                    completedRides++;
                                    break;
                                case Booking.STATUS_CANCELLED:
                                case Booking.STATUS_EXPIRED:
                                    cancelledBookings++;
                                    break;
                            }
                        }

                        // Usage counters for popularity ranking
                        String stationName = doc.getString("stationName");
                        if (stationName != null && !stationName.isEmpty()) {
                            stationUsage.put(stationName, stationUsage.getOrDefault(stationName, 0) + 1);
                        }

                        String bId = doc.getString("bicycleId");
                        if (bId != null && !bId.isEmpty()) {
                            bikeUsage.put(bId, bikeUsage.getOrDefault(bId, 0) + 1);
                        }
                    }

                    metrics.put("totalBookings", totalBookings);
                    metrics.put("todayBookings", todayBookings);
                    metrics.put("activeRides", activeRides);
                    metrics.put("completedRides", completedRides);
                    metrics.put("cancelledBookings", cancelledBookings);

                    // Find most popular station & most rented bicycle
                    String topStation = "None";
                    int maxStationCount = 0;
                    for (Map.Entry<String, Integer> entry : stationUsage.entrySet()) {
                        if (entry.getValue() > maxStationCount) {
                            maxStationCount = entry.getValue();
                            topStation = entry.getKey() + " (" + maxStationCount + " rides)";
                        }
                    }
                    metrics.put("popularStation", topStation);

                    String topBike = "None";
                    int maxBikeCount = 0;
                    for (Map.Entry<String, Integer> entry : bikeUsage.entrySet()) {
                        if (entry.getValue() > maxBikeCount) {
                            maxBikeCount = entry.getValue();
                            topBike = entry.getKey() + " (" + maxBikeCount + " rentals)";
                        }
                    }
                    metrics.put("mostRentedBicycle", topBike);

                    // 4. Fetch Payments for Revenue
                    db.collection(PAYMENTS_COLLECTION).get().addOnSuccessListener(paySnaps -> {
                        long todayRevenuePaise = 0L;
                        long monthlyRevenuePaise = 0L;
                        int pendingPayments = 0;

                        for (DocumentSnapshot doc : paySnaps.getDocuments()) {
                            String pStatus = doc.getString("status");
                            Date paidAt = doc.getDate("paidAt");
                            if (paidAt == null) paidAt = doc.getDate("createdAt");

                            long amtPaise = 0L;
                            Long amtP = doc.getLong("amountPaise");
                            if (amtP != null) {
                                amtPaise = amtP;
                            } else {
                                Double amt = doc.getDouble("amount");
                                if (amt != null) amtPaise = CurrencyUtil.toPaise(amt);
                            }

                            if (Payment.STATUS_SUCCESS.equalsIgnoreCase(pStatus)) {
                                if (paidAt != null) {
                                    if (!paidAt.before(startOfToday)) {
                                        todayRevenuePaise += amtPaise;
                                    }
                                    if (!paidAt.before(startOfMonth)) {
                                        monthlyRevenuePaise += amtPaise;
                                    }
                                }
                            } else if (Payment.STATUS_PENDING.equalsIgnoreCase(pStatus)) {
                                pendingPayments++;
                            }
                        }

                        metrics.put("todayRevenuePaise", todayRevenuePaise);
                        metrics.put("todayRevenueFormatted", CurrencyUtil.formatInrFromPaise(todayRevenuePaise));
                        metrics.put("monthlyRevenuePaise", monthlyRevenuePaise);
                        metrics.put("monthlyRevenueFormatted", CurrencyUtil.formatInrFromPaise(monthlyRevenuePaise));
                        metrics.put("pendingPayments", pendingPayments);

                        // 5. Fetch Refunds
                        db.collection(REFUNDS_COLLECTION).get().addOnSuccessListener(refSnaps -> {
                            long totalRefundsPaise = 0L;
                            int totalRefundsCount = refSnaps.size();
                            for (DocumentSnapshot doc : refSnaps.getDocuments()) {
                                Long rAmt = doc.getLong("amountPaise");
                                if (rAmt != null) totalRefundsPaise += rAmt;
                            }
                            metrics.put("refundsCount", totalRefundsCount);
                            metrics.put("totalRefundsFormatted", CurrencyUtil.formatInrFromPaise(totalRefundsPaise));

                            // 6. Fetch Support Complaints
                            db.collection(SUPPORT_COLLECTION).get().addOnSuccessListener(suppSnaps -> {
                                int openTickets = 0;
                                for (DocumentSnapshot doc : suppSnaps.getDocuments()) {
                                    String sStatus = doc.getString("status");
                                    if (sStatus == null || SupportTicket.STATUS_OPEN.equalsIgnoreCase(sStatus) || SupportTicket.STATUS_IN_PROGRESS.equalsIgnoreCase(sStatus)) {
                                        openTickets++;
                                    }
                                }
                                metrics.put("openTicketsCount", openTickets);

                                liveData.postValue(Resource.success(metrics));
                            }).addOnFailureListener(e -> liveData.postValue(Resource.success(metrics)));
                        }).addOnFailureListener(e -> liveData.postValue(Resource.success(metrics)));
                    }).addOnFailureListener(err -> liveData.postValue(Resource.error("Payment metrics error: " + err.getMessage(), metrics)));
                }).addOnFailureListener(err -> liveData.postValue(Resource.error("Booking metrics error: " + err.getMessage(), metrics)));
            }).addOnFailureListener(err -> liveData.postValue(Resource.error("Bicycle metrics error: " + err.getMessage(), metrics)));
        }).addOnFailureListener(err -> liveData.postValue(Resource.error("Customer metrics error: " + err.getMessage(), null)));

        return liveData;
    }

    /**
     * Customer Management: List all registered customers.
     */
    public LiveData<Resource<List<User>>> getAllCustomers() {
        MutableLiveData<Resource<List<User>>> liveData = new MutableLiveData<>();
        liveData.postValue(Resource.loading(null));

        db.collection(USERS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener((snaps, err) -> {
                    if (err != null) {
                        liveData.postValue(Resource.error("Failed to load customers: " + err.getMessage(), null));
                        return;
                    }
                    if (snaps != null) {
                        List<User> list = new ArrayList<>();
                        for (DocumentSnapshot doc : snaps.getDocuments()) {
                            User u = doc.toObject(User.class);
                            if (u != null) {
                                u.setUid(doc.getId());
                                list.add(u);
                            }
                        }
                        liveData.postValue(Resource.success(list));
                    }
                });

        return liveData;
    }

    /**
     * Updates user role (USER, ADMIN, HANDLER).
     */
    public void updateUserRole(String userId, String newRole, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        boolean isAdmin = User.ROLE_ADMIN.equalsIgnoreCase(newRole);
        db.collection(USERS_COLLECTION).document(userId)
                .update("role", newRole, "isAdmin", isAdmin)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onError.accept(e.getMessage()));
    }

    /**
     * Booking Management: List all bookings with sorting and filtering.
     */
    public LiveData<Resource<List<Booking>>> getAllBookings() {
        MutableLiveData<Resource<List<Booking>>> liveData = new MutableLiveData<>();
        liveData.postValue(Resource.loading(null));

        db.collection(BOOKINGS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener((snaps, err) -> {
                    if (err != null) {
                        liveData.postValue(Resource.error("Failed to load bookings: " + err.getMessage(), null));
                        return;
                    }
                    if (snaps != null) {
                        List<Booking> list = new ArrayList<>();
                        for (DocumentSnapshot doc : snaps.getDocuments()) {
                            Booking b = doc.toObject(Booking.class);
                            if (b != null) {
                                b.setBookingId(doc.getId());
                                list.add(b);
                            }
                        }
                        liveData.postValue(Resource.success(list));
                    }
                });

        return liveData;
    }

    /**
     * Admin Force Cancellation: Cancels a booking and releases bicycle.
     */
    public void forceCancelBooking(String bookingId, String bicycleId, String stationId, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        db.runTransaction(transaction -> {
            transaction.update(db.collection(BOOKINGS_COLLECTION).document(bookingId),
                    "status", Booking.STATUS_CANCELLED,
                    "updatedAt", FieldValue.serverTimestamp());

            if (bicycleId != null && !bicycleId.isEmpty()) {
                transaction.update(db.collection(BICYCLES_COLLECTION).document(bicycleId),
                        "status", Bicycle.STATUS_AVAILABLE,
                        "available", true);
            }

            if (stationId != null && !stationId.isEmpty()) {
                transaction.update(db.collection(STATIONS_COLLECTION).document(stationId),
                        "availableBicycles", FieldValue.increment(1));
            }

            return null;
        }).addOnSuccessListener(v -> onSuccess.run())
          .addOnFailureListener(e -> onError.accept(e.getMessage()));
    }

    /**
     * Coupon Management: Load all coupons.
     */
    public LiveData<Resource<List<Coupon>>> getAllCoupons() {
        MutableLiveData<Resource<List<Coupon>>> liveData = new MutableLiveData<>();
        liveData.postValue(Resource.loading(null));

        db.collection(COUPONS_COLLECTION).get()
                .addOnSuccessListener(snaps -> {
                    List<Coupon> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        Coupon c = doc.toObject(Coupon.class);
                        if (c != null) {
                            c.setCouponId(doc.getId());
                            list.add(c);
                        }
                    }
                    liveData.postValue(Resource.success(list));
                })
                .addOnFailureListener(err -> liveData.postValue(Resource.error("Failed to load coupons: " + err.getMessage(), null)));

        return liveData;
    }

    /**
     * Saves or updates a coupon.
     */
    public void saveCoupon(Coupon coupon, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        String docId = coupon.getCouponId() != null && !coupon.getCouponId().isEmpty()
                ? coupon.getCouponId() : (coupon.getCode() != null ? coupon.getCode().toUpperCase() : "COUPON_" + System.currentTimeMillis());

        coupon.setCouponId(docId);
        db.collection(COUPONS_COLLECTION).document(docId).set(coupon)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onError.accept(e.getMessage()));
    }

    /**
     * Support Ticket Management: Load all tickets.
     */
    public LiveData<Resource<List<SupportTicket>>> getSupportTickets() {
        MutableLiveData<Resource<List<SupportTicket>>> liveData = new MutableLiveData<>();
        liveData.postValue(Resource.loading(null));

        db.collection(SUPPORT_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener((snaps, err) -> {
                    if (err != null) {
                        liveData.postValue(Resource.error("Failed to load tickets: " + err.getMessage(), null));
                        return;
                    }
                    if (snaps != null) {
                        List<SupportTicket> list = new ArrayList<>();
                        for (DocumentSnapshot doc : snaps.getDocuments()) {
                            SupportTicket t = doc.toObject(SupportTicket.class);
                            if (t != null) {
                                t.setTicketId(doc.getId());
                                list.add(t);
                            }
                        }
                        liveData.postValue(Resource.success(list));
                    }
                });

        return liveData;
    }

    /**
     * Creates a new support ticket.
     */
    public void createSupportTicket(SupportTicket ticket, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        String docId = "TKT_" + System.currentTimeMillis();
        ticket.setTicketId(docId);
        db.collection(SUPPORT_COLLECTION).document(docId).set(ticket)
                .addOnSuccessListener(v -> {
                    NotificationRepository notifRepo = new NotificationRepository(db);
                    notifRepo.dispatchNotification(null, new AppNotification(
                            "ADMIN",
                            AppNotification.TYPE_ADMIN_CUSTOMER_COMPLAINT,
                            AppNotification.CHANNEL_ADMIN,
                            "New Customer Complaint",
                            "Grievance submitted: " + ticket.getCategory() + " - " + ticket.getSubject(),
                            AppNotification.TARGET_SUPPORT,
                            docId
                    ));
                    onSuccess.run();
                })
                .addOnFailureListener(e -> onError.accept(e.getMessage()));
    }

    /**
     * Resolves a support ticket.
     */
    public void resolveSupportTicket(String ticketId, String notes, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        String adminUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "ADMIN";

        db.collection(SUPPORT_COLLECTION).document(ticketId)
                .update("status", SupportTicket.STATUS_RESOLVED,
                        "adminNotes", notes,
                        "resolvedBy", adminUid,
                        "resolvedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> {
                    NotificationRepository notifRepo = new NotificationRepository(db);
                    notifRepo.dispatchNotification(null, new AppNotification(
                            "ADMIN",
                            AppNotification.TYPE_SUPPORT_TICKET_UPDATE,
                            AppNotification.CHANNEL_ALERTS,
                            "Grievance Ticket Resolved",
                            "Support ticket #" + ticketId + " has been resolved with notes: " + notes,
                            AppNotification.TARGET_SUPPORT,
                            ticketId
                    ));
                    onSuccess.run();
                })
                .addOnFailureListener(e -> onError.accept(e.getMessage()));
    }

    /**
     * Admin Settings: Load settings.
     */
    public LiveData<Resource<AdminSettings>> getAdminSettings() {
        MutableLiveData<Resource<AdminSettings>> liveData = new MutableLiveData<>();
        liveData.postValue(Resource.loading(null));

        db.collection(ADMIN_SETTINGS_COLLECTION).document(AdminSettings.SETTINGS_ID_DEFAULT)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        liveData.postValue(Resource.error("Failed to load settings: " + err.getMessage(), null));
                        return;
                    }
                    if (snap != null && snap.exists()) {
                        AdminSettings settings = snap.toObject(AdminSettings.class);
                        liveData.postValue(Resource.success(settings != null ? settings : AdminSettings.createDefaultSettings()));
                    } else {
                        AdminSettings def = AdminSettings.createDefaultSettings();
                        db.collection(ADMIN_SETTINGS_COLLECTION).document(AdminSettings.SETTINGS_ID_DEFAULT).set(def);
                        liveData.postValue(Resource.success(def));
                    }
                });

        return liveData;
    }

    /**
     * Admin Settings: Save settings.
     */
    public void saveAdminSettings(AdminSettings settings, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        settings.setUpdatedAt(new Date());
        settings.setUpdatedBy(FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "ADMIN");

        db.collection(ADMIN_SETTINGS_COLLECTION).document(AdminSettings.SETTINGS_ID_DEFAULT)
                .set(settings)
                .addOnSuccessListener(v -> onSuccess.run())
                .addOnFailureListener(e -> onError.accept(e.getMessage()));
    }
}

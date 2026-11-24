package com.sanika.bicyclebookingapp.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sanika.bicyclebookingapp.model.Bicycle;
import com.sanika.bicyclebookingapp.model.Booking;
import com.sanika.bicyclebookingapp.model.BusinessAnalyticsData;
import com.sanika.bicyclebookingapp.model.Invoice;
import com.sanika.bicyclebookingapp.model.MaintenanceRecord;
import com.sanika.bicyclebookingapp.model.Payment;
import com.sanika.bicyclebookingapp.model.Refund;
import com.sanika.bicyclebookingapp.model.RideReview;
import com.sanika.bicyclebookingapp.model.Station;
import com.sanika.bicyclebookingapp.model.SupportTicket;
import com.sanika.bicyclebookingapp.model.User;
import com.sanika.bicyclebookingapp.service.AnalyticsService;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository responsible for loading real Firestore records across all business domains
 * and producing comprehensive business analytics and metrics.
 */
public class AnalyticsRepository {

    private static volatile AnalyticsRepository instance;
    private final FirebaseFirestore db;
    private final AnalyticsService analyticsService;

    // Cache the most recent datasets for instant export generation
    private List<Booking> cachedBookings = new ArrayList<>();
    private List<Payment> cachedPayments = new ArrayList<>();
    private List<Refund> cachedRefunds = new ArrayList<>();
    private List<Invoice> cachedInvoices = new ArrayList<>();
    private List<Bicycle> cachedBicycles = new ArrayList<>();
    private List<Station> cachedStations = new ArrayList<>();
    private List<MaintenanceRecord> cachedMaintenance = new ArrayList<>();
    private List<User> cachedUsers = new ArrayList<>();
    private List<RideReview> cachedReviews = new ArrayList<>();
    private List<SupportTicket> cachedTickets = new ArrayList<>();

    private AnalyticsRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.analyticsService = AnalyticsService.getInstance();
    }

    public static AnalyticsRepository getInstance() {
        if (instance == null) {
            synchronized (AnalyticsRepository.class) {
                if (instance == null) {
                    instance = new AnalyticsRepository();
                }
            }
        }
        return instance;
    }

    /**
     * Loads live Firestore data across all collections and computes business analytics for the date range.
     */
    public LiveData<Resource<BusinessAnalyticsData>> loadAnalytics(Date startDate, Date endDate) {
        MutableLiveData<Resource<BusinessAnalyticsData>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading(null));

        Task<QuerySnapshot> tBookings = db.collection("bookings").get();
        Task<QuerySnapshot> tPayments = db.collection("payments").get();
        Task<QuerySnapshot> tRefunds = db.collection("refunds").get();
        Task<QuerySnapshot> tInvoices = db.collection("invoices").get();
        Task<QuerySnapshot> tBicycles = db.collection("bicycles").get();
        Task<QuerySnapshot> tStations = db.collection("stations").get();
        Task<QuerySnapshot> tMaintenance = db.collection("maintenance_records").get();
        Task<QuerySnapshot> tUsers = db.collection("users").get();
        Task<QuerySnapshot> tReviews = db.collection("reviews").get();
        Task<QuerySnapshot> tTickets = db.collection("support_tickets").get();

        Tasks.whenAllSuccess(tBookings, tPayments, tRefunds, tInvoices, tBicycles,
                tStations, tMaintenance, tUsers, tReviews, tTickets)
                .addOnSuccessListener(results -> {
                    try {
                        List<Booking> bookings = ((QuerySnapshot) results.get(0)).toObjects(Booking.class);
                        List<Payment> payments = ((QuerySnapshot) results.get(1)).toObjects(Payment.class);
                        List<Refund> refunds = ((QuerySnapshot) results.get(2)).toObjects(Refund.class);
                        List<Invoice> invoices = ((QuerySnapshot) results.get(3)).toObjects(Invoice.class);
                        List<Bicycle> bicycles = ((QuerySnapshot) results.get(4)).toObjects(Bicycle.class);
                        List<Station> stations = ((QuerySnapshot) results.get(5)).toObjects(Station.class);
                        List<MaintenanceRecord> maintenance = ((QuerySnapshot) results.get(6)).toObjects(MaintenanceRecord.class);
                        List<User> users = ((QuerySnapshot) results.get(7)).toObjects(User.class);
                        List<RideReview> reviews = ((QuerySnapshot) results.get(8)).toObjects(RideReview.class);
                        List<SupportTicket> tickets = ((QuerySnapshot) results.get(9)).toObjects(SupportTicket.class);

                        // Cache datasets
                        cachedBookings = bookings;
                        cachedPayments = payments;
                        cachedRefunds = refunds;
                        cachedInvoices = invoices;
                        cachedBicycles = bicycles;
                        cachedStations = stations;
                        cachedMaintenance = maintenance;
                        cachedUsers = users;
                        cachedReviews = reviews;
                        cachedTickets = tickets;

                        BusinessAnalyticsData analytics = analyticsService.computeAnalytics(
                                bookings, payments, refunds, invoices, bicycles, stations,
                                maintenance, users, reviews, tickets, startDate, endDate
                        );

                        liveData.setValue(Resource.success(analytics));
                    } catch (Exception e) {
                        liveData.setValue(Resource.error("Failed to parse analytics: " + e.getMessage(), null));
                    }
                })
                .addOnFailureListener(e -> {
                    liveData.setValue(Resource.error("Failed to load business data: " + e.getMessage(), null));
                });

        return liveData;
    }

    // Accessors for cached data used in reports
    public List<Booking> getCachedBookings() { return cachedBookings; }
    public List<Payment> getCachedPayments() { return cachedPayments; }
    public List<Refund> getCachedRefunds() { return cachedRefunds; }
    public List<Invoice> getCachedInvoices() { return cachedInvoices; }
    public List<Bicycle> getCachedBicycles() { return cachedBicycles; }
    public List<Station> getCachedStations() { return cachedStations; }
    public List<MaintenanceRecord> getCachedMaintenance() { return cachedMaintenance; }
    public List<User> getCachedUsers() { return cachedUsers; }
    public List<RideReview> getCachedReviews() { return cachedReviews; }
    public List<SupportTicket> getCachedTickets() { return cachedTickets; }

    public Map<String, Integer> getBicycleRideCounts() {
        Map<String, Integer> counts = new HashMap<>();
        if (cachedBookings != null) {
            for (Booking b : cachedBookings) {
                if (b.getBicycleId() != null) {
                    counts.put(b.getBicycleId(), counts.getOrDefault(b.getBicycleId(), 0) + 1);
                }
            }
        }
        return counts;
    }

    public Map<String, Long> getBicycleMaintenanceCosts() {
        Map<String, Long> costs = new HashMap<>();
        if (cachedMaintenance != null) {
            for (MaintenanceRecord m : cachedMaintenance) {
                if (m.getBicycleId() != null) {
                    long cost = m.getTotalCostPaise() > 0 ? m.getTotalCostPaise() : Math.round(m.getCost() * 100);
                    costs.put(m.getBicycleId(), costs.getOrDefault(m.getBicycleId(), 0L) + cost);
                }
            }
        }
        return costs;
    }

    public Map<String, Integer> getStationPickupCounts() {
        Map<String, Integer> counts = new HashMap<>();
        if (cachedBookings != null) {
            for (Booking b : cachedBookings) {
                if (b.getStationId() != null) {
                    counts.put(b.getStationId(), counts.getOrDefault(b.getStationId(), 0) + 1);
                }
            }
        }
        return counts;
    }

    public Map<String, Integer> getStationDropoffCounts() {
        Map<String, Integer> counts = new HashMap<>();
        if (cachedBookings != null) {
            for (Booking b : cachedBookings) {
                if (b.getReturnStationId() != null) {
                    counts.put(b.getReturnStationId(), counts.getOrDefault(b.getReturnStationId(), 0) + 1);
                }
            }
        }
        return counts;
    }

    public Map<String, Integer> getUserBookingCounts() {
        Map<String, Integer> counts = new HashMap<>();
        if (cachedBookings != null) {
            for (Booking b : cachedBookings) {
                if (b.getUserId() != null) {
                    counts.put(b.getUserId(), counts.getOrDefault(b.getUserId(), 0) + 1);
                }
            }
        }
        return counts;
    }

    public Map<String, Long> getUserSpentPaise() {
        Map<String, Long> spent = new HashMap<>();
        if (cachedPayments != null) {
            for (Payment p : cachedPayments) {
                if (Payment.STATUS_SUCCESS.equalsIgnoreCase(p.getStatus()) && p.getUserId() != null) {
                    long amt = p.getAmountPaise() > 0 ? p.getAmountPaise() : Math.round(p.getAmount() * 100);
                    spent.put(p.getUserId(), spent.getOrDefault(p.getUserId(), 0L) + amt);
                }
            }
        }
        return spent;
    }
}

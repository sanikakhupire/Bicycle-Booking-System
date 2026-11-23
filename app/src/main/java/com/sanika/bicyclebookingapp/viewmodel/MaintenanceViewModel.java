package com.sanika.bicyclebookingapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.sanika.bicyclebookingapp.model.MaintenanceRecord;
import com.sanika.bicyclebookingapp.repository.MaintenanceRepository;
import com.sanika.bicyclebookingapp.util.Resource;

import java.util.List;

/**
 * ViewModel exposing bicycle damage reporting, maintenance workflow, and restoration methods.
 */
public class MaintenanceViewModel extends ViewModel {

    private final MaintenanceRepository repository;

    public MaintenanceViewModel() {
        this.repository = new MaintenanceRepository();
    }

    public MaintenanceViewModel(MaintenanceRepository repository) {
        this.repository = repository;
    }

    public LiveData<Resource<String>> reportDamage(MaintenanceRecord record) {
        return repository.reportDamage(record);
    }

    public LiveData<Resource<List<MaintenanceRecord>>> getMaintenanceRecords(String statusFilter) {
        return repository.getMaintenanceRecords(statusFilter);
    }

    public LiveData<Resource<List<MaintenanceRecord>>> getMaintenanceHistoryForBicycle(String bicycleId) {
        return repository.getMaintenanceHistoryForBicycle(bicycleId);
    }

    public LiveData<Resource<Void>> assignStaffAndPriority(String recordId, String staffName, String priority) {
        return repository.assignStaffAndPriority(recordId, staffName, priority);
    }

    public LiveData<Resource<Void>> startDiagnosisAndRepair(String recordId, String diagnosisNotes) {
        return repository.startDiagnosisAndRepair(recordId, diagnosisNotes);
    }

    public LiveData<Resource<Void>> completeRepair(String recordId, String repairNotes, String partsReplaced,
                                                  long costPaise, String beforeImageUrl, String afterImageUrl) {
        return repository.completeRepair(recordId, repairNotes, partsReplaced, costPaise, beforeImageUrl, afterImageUrl);
    }

    public LiveData<Resource<Void>> verifyAndRestoreBicycle(String recordId, String bicycleId,
                                                           String adminId, String adminName,
                                                           String verificationNotes) {
        return repository.verifyAndRestoreBicycle(recordId, bicycleId, adminId, adminName, verificationNotes);
    }

    public LiveData<Resource<Void>> rejectReport(String recordId, String bicycleId, String reason) {
        return repository.rejectReport(recordId, bicycleId, reason);
    }
}

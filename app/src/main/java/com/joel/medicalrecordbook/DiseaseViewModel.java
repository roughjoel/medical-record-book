package com.joel.medicalrecordbook;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class DiseaseViewModel extends AndroidViewModel {

    private final DiseaseRepository repository;

    public DiseaseViewModel(@NonNull Application application) {
        super(application);
        repository = new DiseaseRepository(application);
    }

    public LiveData<List<DiseaseNodeEntity>> getNodesByDiseaseName(String diseaseName) {
        return repository.getNodesByDiseaseName(diseaseName);
    }

    public LiveData<List<DiseaseNodeEntity>> getNodesByDateRange(String startDate, String endDate) {
        return repository.getNodesByDateRange(startDate, endDate);
    }

    public void insert(DiseaseNodeEntity node) {
        repository.insert(node);
    }

    public void update(DiseaseNodeEntity node) {
        repository.update(node);
    }

    public void delete(DiseaseNodeEntity node) {
        repository.delete(node);
    }

    public void deleteById(int id) {
        repository.deleteById(id);
    }
}

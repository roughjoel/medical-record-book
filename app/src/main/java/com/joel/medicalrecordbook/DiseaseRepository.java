package com.joel.medicalrecordbook;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiseaseRepository {

    private final DiseaseNodeDao diseaseNodeDao;
    private final ExecutorService databaseExecutor;

    public DiseaseRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        diseaseNodeDao = database.diseaseNodeDao();
        databaseExecutor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<DiseaseNodeEntity>> getNodesByDiseaseName(String diseaseName) {
        return diseaseNodeDao.getNodesByDiseaseName(diseaseName);
    }

    public LiveData<List<DiseaseNodeEntity>> getNodesByDateRange(String startDate, String endDate) {
        return diseaseNodeDao.getNodesByDateRange(startDate, endDate);
    }

    public void insert(DiseaseNodeEntity node) {
        databaseExecutor.execute(() -> diseaseNodeDao.insert(node));
    }

    public void update(DiseaseNodeEntity node) {
        databaseExecutor.execute(() -> diseaseNodeDao.update(node));
    }

    public void delete(DiseaseNodeEntity node) {
        databaseExecutor.execute(() -> diseaseNodeDao.delete(node));
    }

    public void deleteById(int id) {
        databaseExecutor.execute(() -> diseaseNodeDao.deleteById(id));
    }
}

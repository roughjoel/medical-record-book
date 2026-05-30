package com.joel.medicalrecordbook;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DiseaseNodeDao {

    @Insert
    void insert(DiseaseNodeEntity node);

    @Update
    void update(DiseaseNodeEntity node);

    @Delete
    void delete(DiseaseNodeEntity node);

    @Query("DELETE FROM disease_nodes WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM disease_nodes WHERE diseaseName = :diseaseName ORDER BY date ASC, id ASC")
    LiveData<List<DiseaseNodeEntity>> getNodesByDiseaseName(String diseaseName);

    @Query("SELECT * FROM disease_nodes " +
            "WHERE diseaseName IN (" +
            "SELECT activeNode.diseaseName FROM disease_nodes AS activeNode " +
            "WHERE activeNode.date >= :startDate AND activeNode.date <= :endDate" +
            ") " +
            "ORDER BY (SELECT MIN(firstNode.date) FROM disease_nodes AS firstNode WHERE firstNode.diseaseName = disease_nodes.diseaseName) ASC, " +
            "diseaseName ASC, date ASC, id ASC")
    LiveData<List<DiseaseNodeEntity>> getNodesByDateRange(String startDate, String endDate);
}

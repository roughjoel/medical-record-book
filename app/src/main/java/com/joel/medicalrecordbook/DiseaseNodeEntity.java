package com.joel.medicalrecordbook;

import androidx.room.Entity;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

@Entity(tableName = "disease_nodes")
public class DiseaseNodeEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String diseaseName;
    private String date;
    @ColumnInfo(name = "isKeyNode")
    private boolean keyNode;
    private String symptoms;
    private String imagePath;
    private String diagnosis;
    private String medications;
    private String notes;

    public DiseaseNodeEntity(
            String diseaseName,
            String date,
            boolean keyNode,
            String symptoms,
            String imagePath,
            String diagnosis,
            String medications,
            String notes
    ) {
        this.diseaseName = diseaseName;
        this.date = date;
        this.keyNode = keyNode;
        this.symptoms = symptoms;
        this.imagePath = imagePath;
        this.diagnosis = diagnosis;
        this.medications = medications;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDiseaseName() {
        return diseaseName;
    }

    public void setDiseaseName(String diseaseName) {
        this.diseaseName = diseaseName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isKeyNode() {
        return keyNode;
    }

    public void setKeyNode(boolean keyNode) {
        this.keyNode = keyNode;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getMedications() {
        return medications;
    }

    public void setMedications(String medications) {
        this.medications = medications;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

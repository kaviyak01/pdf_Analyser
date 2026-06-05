package com.finzly.pdf_Analyser.Model;



public class AnalysisModel {

    private String documentType;
    private String title;
    private String authors;
    private String summary;
    private String keyTakeaway;

    // Used to carry error messages to the UI
    // If this is not null, something went wrong
    private String error;

    public AnalysisModel(String error, String keyTakeaway, String summary, String authors, String title, String documentType) {
        this.error = error;
        this.keyTakeaway = keyTakeaway;
        this.summary = summary;
        this.authors = authors;
        this.title = title;
        this.documentType = documentType;
    }

    public AnalysisModel() {
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getKeyTakeaway() {
        return keyTakeaway;
    }

    public void setKeyTakeaway(String keyTakeaway) {
        this.keyTakeaway = keyTakeaway;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

package com.ad97.imagetotext;

public class FileDetails {

    private String fileName, size, dateAndTime;

    FileDetails(String fileName, String size, String dateAndTime) {
        this.fileName = fileName;
        this.size = size;
        this.dateAndTime = dateAndTime;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSize() {
        return size;
    }

    public String getDateAndTime() {
        return dateAndTime;
    }
}

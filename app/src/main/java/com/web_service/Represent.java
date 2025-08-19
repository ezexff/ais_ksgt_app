package com.web_service;

public class Represent { // Подразделение

    public String id; // Идентификатор подразделения
    public String official_id; // Идентификатор уполномоченного лица
    public String label; // Наименование подразделения

    // Конструктор подразделения
    Represent(String id, String official_id) {
        this.id = id;
        this.official_id = official_id;
    }

    String getId() {
        return this.id;
    }
}

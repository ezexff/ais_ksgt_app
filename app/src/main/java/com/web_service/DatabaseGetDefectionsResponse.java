package com.web_service;

import java.util.List;

public class DatabaseGetDefectionsResponse { // Результат загрузки видов нарушений

    public boolean success;
    public String errorMessage;
    public List<Defection> defections;
}

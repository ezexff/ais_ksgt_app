package com.web_service;

import java.util.List;

public class DatabaseGetPlansResponse { // Результат загрузки планов обхода

    public boolean success;
    public String errorMessage;
    public List<Plan> plans;
}

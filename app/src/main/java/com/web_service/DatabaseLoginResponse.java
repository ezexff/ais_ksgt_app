package com.web_service;

import java.util.List;

public class DatabaseLoginResponse { // Результат авторизации

    public boolean success;
    public String errorMessage;
    public String user_id; // Идентификатор пользователя
    public String first_name;
    public String second_name;
    public String last_name;
    public String webServiceDate; // Текущая дата веб-сервиса
    public List<Represent> represents; // Список подразделений
    public int selected_represent_position; // Позиция выбранного подразделения
}


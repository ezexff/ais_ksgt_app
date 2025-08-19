package com.web_service;

import android.util.Base64;

public class Delict { // Нарушение

    public int id; // Идентификатор нарушения (для локальной БД)
    public String defection_id; // Идентификатор вида нарушения
    public String defection_label; // Краткое описание нарушения
    public String defection_name; // Полное наименование нарушения
    public String time; // Время нарушения
    public byte[] photo; // Фото нарушения
    public String photo_base64; // Фото нарушения (закодировано в Base64)

    // Конструктор нарушения
    public Delict(int id, String defection_id, String defection_label, String defection_name, String time, byte[] photo){
        this.id = id;
        this.defection_id = defection_id;
        this.defection_label = defection_label;
        this.defection_name = defection_name;
        this.time = time;
        this.photo = photo;
    }

    // Конструктор для отправки нарушения на сервер
    public Delict(String defection_id, String defection_name, String time, byte[] photo){
        this.defection_id = defection_id;
        this.defection_name = defection_name;
        this.time = time;

        if(photo != null) {
            this.photo_base64 = Base64.encodeToString(photo, Base64.DEFAULT);
        } else {
            photo_base64 = null;
        }
    }
}
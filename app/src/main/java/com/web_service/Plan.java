package com.web_service;
import java.util.List;

public class Plan { // План обхода

    public String id; // Идентификатор плана
    public String content; // Краткое описание
    public List<PObject> pobjects; // Подконтрольные объекты
    public boolean checkBoxIsChecked; // План выбран для отправки

    // Конструктор для загрузки планов на устройство
    public Plan(String id, String content) {
        this.id = id;
        this.content = content;
    }

    public String getId() {
        return(this.id);
    }

    public String getContent(){
        return this.content;
    }
}

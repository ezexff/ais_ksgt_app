package com.web_service;

public class Defection { // Вид нарушения

    public String id; // Уникальный идентификатор
    public String label; // Краткое наименование нарушения
    public String name; // Полное наименование нарушения

    // Конструктор вида нарушения
    public Defection(String id, String label, String name){
        this.id = id;
        this.label = label;
        this.name = name;
    }

    public String getLabel(){
        return this.label;
    }
}
package com.web_service;

import android.util.Base64;
import java.util.List;

public class PObject { // Подконтрольный объект

    public String sub_control_id; // Идентификатор записи
    public String address_id; // Адрес дома
    public String object_type; // Тип объекта
    public String property_label; // Сокращённое название
    public String cad_number; // Кадастровый номер
    public String address_place; // Местонахождение
    public String element_adr_id; // Ссылка на элемент адреса
    public String element_name; // Наименование улицы
    public String helement_number; // Номер дома
    public String helement_label; // Метка дома
    public String helement_name; // Наименование дома
    public String full_name; // Полное наименование объекта
    public boolean is_checked; // Выполнена проверка
    public List<Delict> delicts; // Нарушения
    public byte[] photo_panorama; // Фото панорамы
    public byte[] photo_house_number; //
    public String photo_panorama_base64; // Фото панорамы (закодировано в Base64)
    public String photo_house_number_base64; // Фото номера дома (закодировано в Base64)

    // Конструкор подконтрольного объекта
    public PObject(String sub_control_id,
                   boolean is_checked,
                   String address_id,
                   String object_type,
                   String property_label,
                   String cad_number,
                   String address_place,
                   String element_adr_id,
                   String element_name,
                   String helement_label,
                   String helement_name,
                   String helement_number,
                   byte[] photo_panorama,
                   byte[] photo_house_number) {

        this.sub_control_id = sub_control_id;
        this.is_checked = is_checked;
        this.address_id = address_id;
        this.object_type = object_type;
        this.property_label = property_label;
        this.cad_number = cad_number;
        this.address_place = address_place;
        this.element_adr_id = element_adr_id;
        this.element_name = element_name;
        this.helement_label = helement_label;
        this.helement_name = helement_name;
        this.helement_number = helement_number;
        this.photo_panorama = photo_panorama;
        this.photo_house_number = photo_house_number;
    }

    // Конструктор для отправки объекта с нарушениями на сервер
    public PObject(String sub_control_id, String address_id, String address_place, byte[] photo_panorama, byte[] photo_house_number, List<Delict> delicts) {
        this.sub_control_id = sub_control_id;
        this.address_id = address_id;
        this.address_place = address_place;

        if (photo_panorama != null) {
            this.photo_panorama_base64 = Base64.encodeToString(photo_panorama, Base64.DEFAULT);
        } else {
            photo_panorama_base64 = null;
        }

        if (photo_house_number != null) {
            this.photo_house_number_base64 = Base64.encodeToString(photo_house_number, Base64.DEFAULT);
        } else {
            photo_house_number_base64 = null;
        }

        this.delicts = delicts;
    }

    // Конструктор для отправки объекта без нарушений на сервер
    public PObject(String sub_control_id, String address_id, String address_place, byte[] photo_panorama, byte[] photo_house_number) {
        this.sub_control_id = sub_control_id;
        this.address_id = address_id;
        this.address_place = address_place;

        if (photo_panorama != null) {
            this.photo_panorama_base64 = Base64.encodeToString(photo_panorama, Base64.DEFAULT);
        } else {
            photo_panorama_base64 = null;
        }

        if (photo_house_number != null) {
            this.photo_house_number_base64 = Base64.encodeToString(photo_house_number, Base64.DEFAULT);
        } else {
            photo_house_number_base64 = null;
        }
    }

    public Integer getHelement_number(){
        return Integer.parseInt(this.helement_number.trim());
    }
}

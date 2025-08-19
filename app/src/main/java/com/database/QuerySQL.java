package com.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.web_service.DatabaseGetDefectionsResponse;
import com.web_service.DatabaseGetPlansResponse;
import com.web_service.DatabaseLoginResponse;
import com.web_service.Defection;
import com.web_service.Delict;
import com.web_service.PObject;
import com.web_service.Plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.utils.StaticMethods.intToBoolean;

public class QuerySQL {

    private final static String TAG = "T_QuerySQL";

    /**
     * Создаём локальную БД с таблицами планов обхода для текущей даты
     */
    public static void createPlansDatabaseForCurrentDate(Context context, DatabaseLoginResponse userInfo){

        String official_id = userInfo.represents.get(userInfo.selected_represent_position).official_id;
        String represent_id = userInfo.represents.get(userInfo.selected_represent_position).id;
        String date_for_db_name = userInfo.webServiceDate.replaceAll("-", "");
        DBHelper dbHelperPlans = new DBHelper(context, official_id + represent_id + "Plans" + date_for_db_name); // Объект для создания и управления версиями БД
        SQLiteDatabase dbp = dbHelperPlans.getWritableDatabase(); // Создаём БД или подключаемся, если создана
        dbp.execSQL("CREATE TABLE IF NOT EXISTS PLANS (" // Создаём новую таблицу с планами, если её не было
                + "id text,"
                + "content text,"
                + "date text" + ");");

        dbp.execSQL("CREATE TABLE IF NOT EXISTS OBJECTS (" // Создаём новую таблицу с объектами, если её не было
                + "plan_id text,"
                + "is_checked integer,"
                + "sub_control_id text,"
                + "address_id text,"
                + "object_type text,"
                + "property_label text,"
                + "cad_number text,"
                + "address_place text,"
                + "element_adr_id text,"
                + "element_name text,"
                + "helement_number text,"
                + "helement_label text,"
                + "helement_name text,"
                + "photo_panorama blob,"
                + "photo_house_number blob);");

        dbp.execSQL("CREATE TABLE IF NOT EXISTS DELICTS (" // Создаём новую таблицу с нарушениями, если её не было
                + "id integer,"
                + "sub_control_id text,"
                + "defection_id text,"
                + "defection_label text,"
                + "defection_name text,"
                + "time text,"
                + "photo blob);");
    }

    /**
     * Проверяем наличие видов нарушений в локальной БД
     */
    public static boolean isDefectionsDownloaded(Context context) {

        boolean Result;
        DBHelper dbHelperDefections = new DBHelper(context, "Defections"); // Объект для создания и управления версиями БД
        SQLiteDatabase dbd = dbHelperDefections.getWritableDatabase(); // Создаём БД или подключаемся, если создана
        dbd.execSQL("CREATE TABLE IF NOT EXISTS DEFECTIONS (" // Создаём новую таблицу с видами нарушений, если её не было
                + "id text,"
                + "label text,"
                + "name text" + ");");
        Cursor c = dbd.query("DEFECTIONS", null, null, null, null, null, null);
        Result = c.moveToFirst();
        c.close();
        dbHelperDefections.close();

        return (Result);
    }

    /**
     * Сохраняем загруженные виды нарушений в локальной БД
     */
    public static void saveDefectionInLocalDatabase(Context context, DatabaseGetDefectionsResponse dbDefections){

        DBHelper dbHelperDefections = new DBHelper(context, "Defections"); // Объект для создания и управления версиями БД
        SQLiteDatabase dbd = dbHelperDefections.getWritableDatabase(); // Создаём БД или подключаемся, если создана
        dbd.delete("DEFECTIONS", null, null); // Удаляем старые записи из локальной БД

        for (int i = 0; i < dbDefections.defections.size(); i++) { // Добавление новых записей в локальную БД
            ContentValues cv = new ContentValues();
            cv.put("id", dbDefections.defections.get(i).id);
            cv.put("label", dbDefections.defections.get(i).label);
            cv.put("name", dbDefections.defections.get(i).name);
            dbd.insert("DEFECTIONS", null, cv);
        }
        dbHelperDefections.close(); // Закрываем подключение к БД
    }

    /**
     * Сохраняем загруженные планы в локальной БД
     */
    public static void savePlansInLocalDatabase(Context context, DatabaseLoginResponse userInfo, DatabaseGetPlansResponse dbPlans){

        String official_id = userInfo.represents.get(userInfo.selected_represent_position).official_id;
        String represent_id = userInfo.represents.get(userInfo.selected_represent_position).id;
        String date_for_db_name = userInfo.webServiceDate.replaceAll("-", "");
        DBHelper dbHelperPlans = new DBHelper(context, official_id + represent_id + "Plans" + date_for_db_name); // Объект для создания и управления версиями БД
        SQLiteDatabase dbp = dbHelperPlans.getWritableDatabase(); // Создаём БД или подключаемся, если создана

        // Удаляем старые записи из локальной БД
        dbp.delete("PLANS", null, null);
        dbp.delete("OBJECTS", null, null);
        dbp.delete("DELICTS", null, null);

        for (int i = 0; i < dbPlans.plans.size(); i++) { // Добавление планов в БД

            if (dbPlans.plans.get(i).pobjects.size() > 0) { // Сохранять в локальную БД только планы с объектами

                ContentValues cv = new ContentValues();
                cv.put("id", dbPlans.plans.get(i).id);
                cv.put("content", dbPlans.plans.get(i).content);
                cv.put("date", userInfo.webServiceDate);
                dbp.insert("PLANS", null, cv);

                for (int j = 0; j < dbPlans.plans.get(i).pobjects.size(); j++) { // Добавление объектов в БД
                    ContentValues cv2 = new ContentValues();
                    cv2.put("plan_id", dbPlans.plans.get(i).id);
                    cv2.put("is_checked", 0);
                    cv2.put("sub_control_id", dbPlans.plans.get(i).pobjects.get(j).sub_control_id);
                    cv2.put("address_id", dbPlans.plans.get(i).pobjects.get(j).address_id);
                    cv2.put("object_type", dbPlans.plans.get(i).pobjects.get(j).object_type);
                    cv2.put("property_label", dbPlans.plans.get(i).pobjects.get(j).property_label);
                    cv2.put("cad_number", dbPlans.plans.get(i).pobjects.get(j).cad_number);
                    cv2.put("address_place", dbPlans.plans.get(i).pobjects.get(j).address_place);
                    cv2.put("element_adr_id", dbPlans.plans.get(i).pobjects.get(j).element_adr_id);
                    cv2.put("element_name", dbPlans.plans.get(i).pobjects.get(j).element_name);
                    cv2.put("helement_number", dbPlans.plans.get(i).pobjects.get(j).helement_number);
                    cv2.put("helement_label", dbPlans.plans.get(i).pobjects.get(j).helement_label);
                    cv2.put("helement_name", dbPlans.plans.get(i).pobjects.get(j).helement_name);
                    dbp.insert("OBJECTS", null, cv2);
                }
            }
        }
        dbHelperPlans.close(); // Закрываем подключение к БД
    }

    /**
     * Загружаем виды нарушений из локальной БД в список видов нарушений
     */
    public static List<Defection> getDefectionsFromDatabase(Context context){

        List<Defection> Result = new ArrayList<>();

        DBHelper dbHelperDefections = new DBHelper(context, "Defections"); // Объект для создания и управления версиями БД
        SQLiteDatabase dbd = dbHelperDefections.getWritableDatabase(); // Создаём БД или подключаемся, если создана
        Cursor defections_c = dbd.query("DEFECTIONS", null, null, null, null, null, null);

        if (defections_c.moveToFirst()) {

            int idColIndex = defections_c.getColumnIndex("id");
            int labelColIndex = defections_c.getColumnIndex("label");
            int nameColIndex = defections_c.getColumnIndex("name");

            do {
                Result.add(new Defection(
                        defections_c.getString(idColIndex),
                        defections_c.getString(labelColIndex).toLowerCase(), // С переводом в нижний регистр
                        defections_c.getString(nameColIndex)));
            } while (defections_c.moveToNext());
        } else {
            return null;
        }

        defections_c.close();
        dbHelperDefections.close();

        // Сортировка видов нарушений
        Collections.sort(Result, new Comparator<Defection>() {
            @Override
            public int compare(Defection a, Defection b) {
                return a.getLabel().compareTo(b.getLabel());
            }
        });

        return Result;
    }

    /**
     * Загружаем планы из локальной БД в список объектов
     */
    public static List<Plan> getPlansFromDatabase(Context context, DatabaseLoginResponse userInfo) {

        List<Plan> Result = new ArrayList<>();

        String plans_selection = "date = ?";
        String[] plans_selectionArgs = new String[]{userInfo.webServiceDate};

        String official_id = userInfo.represents.get(userInfo.selected_represent_position).official_id;
        String represent_id = userInfo.represents.get(userInfo.selected_represent_position).id;
        String date_for_db_name = userInfo.webServiceDate.replaceAll("-", "");
        DBHelper dbHelperPlans = new DBHelper(context, official_id + represent_id + "Plans" + date_for_db_name); // Объект для создания и управления версиями БД
        SQLiteDatabase dbp = dbHelperPlans.getWritableDatabase(); // Создаём БД или подключаемся, если создана
        Cursor plans_c = dbp.query("PLANS", null, plans_selection, plans_selectionArgs, null, null, null);

        if (plans_c.moveToFirst()) {

            int idColIndex = plans_c.getColumnIndex("id");
            int contentColIndex = plans_c.getColumnIndex("content");

            do {
                Result.add(new Plan(
                        plans_c.getString(idColIndex),
                        plans_c.getString(contentColIndex)));
            } while (plans_c.moveToNext());
        } else {
            return null;
        }

        // Сортировка планов
        Collections.sort(Result, new Comparator<Plan>() {
            @Override
            public int compare(Plan a, Plan b) {
                return a.getContent().compareTo(b.getContent());
            }
        });


        // Загружаем объекты из локальной БД
        for (int i = 0; i < Result.size(); i++) {

            List<PObject> objects_tmp = new ArrayList<>();

            List<PObject> objects_tmp_for_sort1 = new ArrayList<>();
            List<PObject> objects_tmp_for_sort2 = new ArrayList<>();
            List<PObject> objects_tmp_for_sort3 = new ArrayList<>();

            String objects_selection = "plan_id = ?";
            String[] objects_selectionArgs = new String[]{Result.get(i).id};

            Cursor objects_c = dbp.query("OBJECTS", null, objects_selection, objects_selectionArgs, null, null, null);

            if (objects_c.moveToFirst()) {

                int sub_control_idColIndex = objects_c.getColumnIndex("sub_control_id");
                int is_checkedColIndex = objects_c.getColumnIndex("is_checked");
                int address_idColIndex = objects_c.getColumnIndex("address_id");
                int object_typeColIndex = objects_c.getColumnIndex("object_type");
                int property_labelColIndex = objects_c.getColumnIndex("property_label");
                int cad_numberColIndex = objects_c.getColumnIndex("cad_number");
                int address_placeColIndex = objects_c.getColumnIndex("address_place");
                int element_adr_idColIndex = objects_c.getColumnIndex("element_adr_id");
                int element_nameColIndex = objects_c.getColumnIndex("element_name");
                int helement_numberColIndex = objects_c.getColumnIndex("helement_number");
                int helement_labelColIndex = objects_c.getColumnIndex("helement_label");
                int helement_nameColIndex = objects_c.getColumnIndex("helement_name");
                int photo_panoramaColIndex = objects_c.getColumnIndex("photo_panorama");
                int photo_house_numberColIndex = objects_c.getColumnIndex("photo_house_number");

                do {
                    objects_tmp.add(new PObject(
                            objects_c.getString(sub_control_idColIndex),
                            intToBoolean(objects_c.getInt(is_checkedColIndex)),
                            objects_c.getString(address_idColIndex),
                            objects_c.getString(object_typeColIndex),
                            objects_c.getString(property_labelColIndex),
                            objects_c.getString(cad_numberColIndex),
                            objects_c.getString(address_placeColIndex),
                            objects_c.getString(element_adr_idColIndex),
                            objects_c.getString(element_nameColIndex),
                            objects_c.getString(helement_labelColIndex),
                            objects_c.getString(helement_nameColIndex),
                            objects_c.getString(helement_numberColIndex),
                            objects_c.getBlob(photo_panoramaColIndex),
                            objects_c.getBlob(photo_house_numberColIndex)));

                    // Формируем полное наименование объекта для вывода на экран
                    int objects_tmp_counter = objects_tmp.size() - 1;
                    if (objects_tmp.get(objects_tmp_counter).address_id != null) { // У объекта указан адрес

                        objects_tmp.get(objects_tmp_counter).full_name =
                                objects_tmp.get(objects_tmp_counter).element_name + " " +
                                objects_tmp.get(objects_tmp_counter).helement_number;

                        if (objects_tmp.get(objects_tmp_counter).helement_label != null) {
                            objects_tmp.get(objects_tmp_counter).full_name += objects_tmp.get(objects_tmp_counter).helement_label;
                        } else {
                            objects_tmp.get(objects_tmp_counter).helement_label = "";
                        }

                        if (objects_tmp.get(objects_tmp_counter).helement_name != null) {
                            objects_tmp.get(objects_tmp_counter).full_name += objects_tmp.get(objects_tmp_counter).helement_name;
                        } else {
                            objects_tmp.get(objects_tmp_counter).helement_name = "";
                        }

                        objects_tmp.get(objects_tmp_counter).full_name += " (" + objects_tmp.get(objects_tmp_counter).property_label + ")";

                        objects_tmp_for_sort1.add(objects_tmp.get(objects_tmp_counter));

                    } else if ((objects_tmp.get(objects_tmp_counter).cad_number != null) && // У объекта имеется кадастровый номер
                            (objects_tmp.get(objects_tmp_counter).address_place != null)) { // и местонахождение

                        objects_tmp.get(objects_tmp_counter).full_name = objects_tmp.get(objects_tmp_counter).cad_number + " - " +
                                objects_tmp.get(objects_tmp_counter).address_place;

                        objects_tmp_for_sort2.add(objects_tmp.get(objects_tmp_counter));

                    } else {

                        objects_tmp.get(objects_tmp_counter).full_name = objects_tmp.get(objects_tmp_counter).address_place;

                        objects_tmp_for_sort3.add(objects_tmp.get(objects_tmp_counter));

                    }

                } while (objects_c.moveToNext());

            } else {
                Log.d(TAG, "Для плана не найдено объектов: " + Result.get(i).content);
            }

            // Сортировка объектов с адресом
             Collections.sort(objects_tmp_for_sort1, new Comparator<PObject>() {
                @Override
                public int compare(PObject a, PObject b) {

                    int c;
                    c = a.element_name.compareTo(b.element_name);
                    if (c == 0) {
                        c = a.getHelement_number().compareTo(b.getHelement_number());
                        if (c == 0)
                            c = a.helement_label.compareTo(b.helement_label);{
                            if (c == 0) {
                                c = a.helement_name.compareTo(b.helement_name);
                                if (c == 0)
                                    c = a.property_label.compareTo(b.property_label);{
                                }
                            }
                        }
                    }
                    return c;
                }
            });

            // Сортировка объектов с кадастровым номером и местонахождением
            Collections.sort(objects_tmp_for_sort2, new Comparator<PObject>() {
                @Override
                public int compare(PObject a, PObject b) {
                    return a.full_name.compareTo(b.full_name);
                }
            });

            // Сортировка объектов с местонахождением
            Collections.sort(objects_tmp_for_sort3, new Comparator<PObject>() {
                @Override
                public int compare(PObject a, PObject b) {
                    return a.full_name.compareTo(b.full_name);
                }
            });

            // Объединение нескольких отсортированных списков в один
            objects_tmp.clear();
            objects_tmp.addAll(objects_tmp_for_sort1);
            objects_tmp.addAll(objects_tmp_for_sort2);
            objects_tmp.addAll(objects_tmp_for_sort3);

            for (int j = 0; j < objects_tmp.size(); j++) { // Загружаем нарушения из локальной БД

                List<Delict> delicts_tmp = new ArrayList();

                // Проверенные объекты с нарушениями
                if (objects_tmp.get(j).is_checked) {

                    String delicts_selection = "sub_control_id = ?";
                    String[] delicts_selectionArgs = new String[]{objects_tmp.get(j).sub_control_id};
                    Cursor delicts_c = dbp.query("DELICTS", null, delicts_selection, delicts_selectionArgs, null, null, null);

                    if (delicts_c.moveToFirst()) {

                        //Log.d(TAG, "Для проверенного объекта есть нарушение: " + objects_tmp.get(j).full_name);

                        int idColIndex = delicts_c.getColumnIndex("id");
                        int defection_idColIndex = delicts_c.getColumnIndex("defection_id");
                        int defection_labelColIndex = delicts_c.getColumnIndex("defection_label");
                        int defection_nameColIndex = delicts_c.getColumnIndex("defection_name");
                        int timeColIndex = delicts_c.getColumnIndex("time");
                        int photoColIndex = delicts_c.getColumnIndex("photo");

                        do {
                            delicts_tmp.add(new Delict(
                                    delicts_c.getInt(idColIndex),
                                    delicts_c.getString(defection_idColIndex),
                                    delicts_c.getString(defection_labelColIndex),
                                    delicts_c.getString(defection_nameColIndex),
                                    delicts_c.getString(timeColIndex),
                                    delicts_c.getBlob(photoColIndex)));
                        } while (delicts_c.moveToNext());

                        objects_tmp.get(j).delicts = delicts_tmp;

                    } else {
                        //Log.d(TAG, "Для проверенного объекта нарушений нет: " + objects_tmp.get(j).full_name);
                    }
                }
            }
            Result.get(i).pobjects = objects_tmp; // Добавляем к планам объекты
        }

        dbHelperPlans.close();

        return Result;
    }

    /**
     * Обновление фото объекта в БД
     */
    public static void updateObjectPhotoInDatabase(Context context, String photoFieldName, byte[] photo, DatabaseLoginResponse userInfo, List<Plan> plans_tmp, int currentPlanPosition, int currentObjectPosition) {

        ContentValues cv_tmp = new ContentValues();
        cv_tmp.put(photoFieldName, photo);
        String current_sub_control_id = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).sub_control_id;
        String official_id = userInfo.represents.get(userInfo.selected_represent_position).official_id;
        String represent_id = userInfo.represents.get(userInfo.selected_represent_position).id;
        String date_for_db_name = userInfo.webServiceDate.replaceAll("-", "");
        DBHelper dbHelperPlans = new DBHelper(context, official_id + represent_id + "Plans" + date_for_db_name); // Объект для создания и управления версиями БД
        SQLiteDatabase dbp = dbHelperPlans.getWritableDatabase(); // Создаём БД или подключаемся, если создана
        dbp.update("OBJECTS", cv_tmp, "sub_control_id = ?", new String[]{current_sub_control_id});
        String debug_full_name = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).full_name;
        Log.d(TAG, "updateObjectPhotoInDatabase | current_sub_control_id=" + current_sub_control_id + " | photoFieldName=" + photoFieldName + " | " + debug_full_name);
        dbHelperPlans.close();
    }

    /**
     * Обновление нарушения в локальной БД
     */
    public static void updateDelictInDatabase(Context context, ContentValues cv, DatabaseLoginResponse userInfo, String current_sub_control_id, int currentDelictPosition){

        String official_id = userInfo.represents.get(userInfo.selected_represent_position).official_id;
        String represent_id = userInfo.represents.get(userInfo.selected_represent_position).id;
        String date_for_db_name = userInfo.webServiceDate.replaceAll("-", "");
        DBHelper dbHelperPlans = new DBHelper(context, official_id + represent_id + "Plans" + date_for_db_name); // Объект для создания и управления версиями БД
        SQLiteDatabase dbp = dbHelperPlans.getWritableDatabase(); // Создаём БД или подключаемся, если создана
        dbp.update("DELICTS", cv, "id = " + currentDelictPosition + " and sub_control_id = " + current_sub_control_id, null);
        Log.d(TAG, "updateDelictIdInDatabase | current_sub_control_id=" + current_sub_control_id + " | delict id=" + currentDelictPosition);
        dbHelperPlans.close();
    }

    /**
     * Обновление признака обхода в локальной БД
     */
    public static void updateObjectCheckStatusInDatabase(Context context, ContentValues cv, DatabaseLoginResponse userInfo, String current_sub_control_id){

        String official_id = userInfo.represents.get(userInfo.selected_represent_position).official_id;
        String represent_id = userInfo.represents.get(userInfo.selected_represent_position).id;
        String date_for_db_name = userInfo.webServiceDate.replaceAll("-", "");
        DBHelper dbHelperPlans = new DBHelper(context, official_id + represent_id + "Plans" + date_for_db_name); // Объект для создания и управления версиями БД
        SQLiteDatabase dbp = dbHelperPlans.getWritableDatabase(); // Создаём БД или подключаемся, если создана
        dbp.update("OBJECTS", cv, "sub_control_id = ?", new String[]{current_sub_control_id});
        Log.d(TAG, "updateObjectCheckStatusInDatabase | current_sub_control_id=" + current_sub_control_id + " | is_checked=" + cv.getAsString("is_checked"));
        dbHelperPlans.close();
    }

    /**
     * Добавление нарушения в локальную БД
     */
    public static void insertDelictInDatabase(Context context, ContentValues cv, DatabaseLoginResponse userInfo){

        String official_id = userInfo.represents.get(userInfo.selected_represent_position).official_id;
        String represent_id = userInfo.represents.get(userInfo.selected_represent_position).id;
        String date_for_db_name = userInfo.webServiceDate.replaceAll("-", "");
        DBHelper dbHelperPlans = new DBHelper(context, official_id + represent_id + "Plans" + date_for_db_name); // Объект для создания и управления версиями БД
        SQLiteDatabase dbp = dbHelperPlans.getWritableDatabase(); // Создаём БД или подключаемся, если создана
        dbp.insert("DELICTS", null, cv);
        Log.d(TAG, "insertDelictInDatabase | current_sub_control_id=" + cv.getAsString("sub_control_id") + " | currentDelictPosition=" + cv.getAsString("id"));
        dbHelperPlans.close();
    }

    /**
     * Добавление нарушения из локальной БД
     */
    public static void deleteDelictFromDatabase(Context context, DatabaseLoginResponse userInfo, String current_sub_control_id, int currentDelictPosition){

        String official_id = userInfo.represents.get(userInfo.selected_represent_position).official_id;
        String represent_id = userInfo.represents.get(userInfo.selected_represent_position).id;
        String date_for_db_name = userInfo.webServiceDate.replaceAll("-", "");
        DBHelper dbHelperPlans = new DBHelper(context, official_id + represent_id + "Plans" + date_for_db_name); // Объект для создания и управления версиями БД
        SQLiteDatabase dbp = dbHelperPlans.getWritableDatabase(); // Создаём БД или подключаемся, если создана
        dbp.delete("DELICTS", "id = " + currentDelictPosition + " and sub_control_id = " + current_sub_control_id, null);
        Log.d(TAG, "deleteDelictFromDatabase | current_sub_control_id=" + current_sub_control_id + " | currentDelictPosition=" + currentDelictPosition);
        dbHelperPlans.close();
    }
}

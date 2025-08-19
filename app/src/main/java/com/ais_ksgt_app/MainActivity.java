package com.ais_ksgt_app;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.ais_ksgt_app.R;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.recyclerview.DelictsRecyclerViewAdapterNew;
import com.recyclerview.PlanRecyclerViewAdapter;
import com.service.SessionAndDateCheckService;
import com.spinner.AdapterDefectionSpinner;
import com.spinner.AdapterPlanSpinner;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.web_service.DatabaseGetDefectionsResponse;
import com.web_service.DatabaseGetPlansResponse;
import com.web_service.DatabaseLoginResponse;
import com.web_service.DatabaseSendObjectsResponse;
import com.web_service.Defection;
import com.web_service.Delict;
import com.utils.GetPhotoResult;
import com.web_service.MyHttpTransportSE;
import com.web_service.PObject;
import com.web_service.Plan;
import com.web_service.SendObjects;
import com.utils.SendResultAdapter;
import com.web_service.WebServiceResponse;

import static com.database.QuerySQL.createPlansDatabaseForCurrentDate;
import static com.database.QuerySQL.deleteDelictFromDatabase;
import static com.database.QuerySQL.getDefectionsFromDatabase;
import static com.database.QuerySQL.getPlansFromDatabase;
import static com.database.QuerySQL.insertDelictInDatabase;
import static com.database.QuerySQL.isDefectionsDownloaded;
import static com.database.QuerySQL.saveDefectionInLocalDatabase;
import static com.database.QuerySQL.savePlansInLocalDatabase;
import static com.database.QuerySQL.updateDelictInDatabase;
import static com.database.QuerySQL.updateObjectCheckStatusInDatabase;
import static com.database.QuerySQL.updateObjectPhotoInDatabase;
import static com.ais_ksgt_app.LoginActivity.BROADCAST_ACTION;
import static com.ais_ksgt_app.LoginActivity.PARAM_RESULT;
import static com.ais_ksgt_app.LoginActivity.PARAM_STATUS;
import static com.ais_ksgt_app.LoginActivity.PARAM_TASK;
import static com.ais_ksgt_app.LoginActivity.STATUS_FINISH;
import static com.ais_ksgt_app.LoginActivity.TASK1_CODE;
import static com.ais_ksgt_app.LoginActivity.TASK2_CODE;
import static com.utils.StaticMethods.*;

public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<WebServiceResponse>, // Загрузчик
        NavigationView.OnNavigationItemSelectedListener, // Обработчик выпадающего меню
        PlanRecyclerViewAdapter.ObjectClickListener // Обработчик нажатия на подконтрольный объект
{

    // Тег класса (для логирования)
    private final static String TAG = "T_MainActivity";

    // Основные переменные/контейнеры и т.д.
    private DatabaseLoginResponse userInfo; // Данные пользователя
    private List<Plan> plans_tmp; // Загруженные планы обхода
    private List<Defection> defections_tmp; // Загруженные виды нарушений
    private int currentPlanPosition; // Позиция выбранного плана
    private int currentObjectPosition; // Позиция выбранного объекта
    private int currentDelictPosition; // Текущее нарушение
    private int currentDefectionPosition; // Позиция выбранного вида нарушения
    private String objects_for_send_JSON_str; // JSON строка с объектами для отправки на сервер

    // Элементы интерфейса
    private AlertDialog dialogPermissions; // Диалог с просьбой предоставить разрешения
    private AlertDialog dialogDefections; // Диалог предложения загрузить виды нарушений
    private AlertDialog mDialog; // Диалог выполнения проверки
    private RecyclerView mORecyclerView; // Список объектов
    private RecyclerView mDRecyclerView; // Список нарушений по объекту
    private ProgressDialog progressDialog; // Прогресс бар во время загрузки данных
    private Spinner spinnerPlan; // Список планов
    private Spinner spinnerDefection; // Список видов нарушений
    private EditText detDescription; // Описание нарушения
    private EditText detTime; // Время нарушения
    private ImageView ivPhotoPanorama; // Фото панорама
    private ImageView ivPhotoHouseNumber; // Фото номера дома
    private ImageView ivPhotoDelict; // Фото нарушения
    private ImageView ivPhotoPreview; // Превью фото
    private DrawerLayout mDrawerLayout; // Меню
    private ActionBarDrawerToggle mToggle; // Меню
    private TextView sbTextView; // Меню
    private LinearLayout sbLinearLayout; // Меню
    private NavigationView navigationView; // Меню

    // Обработчки
    private DelictsRecyclerViewAdapterNew.MyAdapterListener edit_or_delete_delict_listener; // Листенер добавления и удаления нарушений
    private BroadcastReceiver br; // Приёмник сообщений от сервиса
    private IntentFilter intFilt; // Фильтр намерений для приёмника сообщений от сервиса

    // Адаптеры
    private boolean doNotSkipInitTextDescriptionSetter; // Не заполнять описание по виду нарушения при отображении формы редактирования
    private PlanRecyclerViewAdapter ORViewAdapter; // Адаптер объектов
    private DelictsRecyclerViewAdapterNew DRVAdapter; // Адаптер списка нарушений

    // Уникальные id загрузчика Loader
    private static final int LDR_BASIC_ID_GET_DEFECTIONS = 2; // загрузка видов нарушений
    private static final int LDR_BASIC_ID_GET_PLANS = 3; // загрузка планов
    private static final int LDR_BASIC_ID_SEND_OBJECTS = 4; // загрузка планов

    // Уникальные id запросов к камере и галерее
    private final int PANORAMA_REQUEST = 1;
    private final int HOUSE_NUMBER_REQUEST = 2;

    // Уникальные id запросов к камере
    private final int PANORAMA_FROM_CAMERA_REQUEST = 1;
    private final int HOUSE_NUMBER_FROM_CAMERA_REQUEST = 2;
    private final int DELICT_FROM_CAMERA_REQUEST = 3;

    // Уникальные id запросов к галерее
    private final int PANORAMA_FROM_GALLERY_REQUEST = 4;
    private final int HOUSE_NUMBER_FROM_GALLERY_REQUEST = 5;
    private final int DELICT_FROM_GALLERY_REQUEST = 6;

    // TODO: Сохранять фото панорамы и номера дома в локальной БД сразу, после выбора, а не при нажатии на кнопку завершить добавление нарушений?

    // Отключение поворота экрана
    private void stopRotateScreen()
    {
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    }

    // Включение поворота экрана
    private void startRotateScreen()
    {
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    /**
     * Нажание на кнопку Back
     */
    public void onBackPressed() {
        if(mDrawerLayout.isDrawerOpen(GravityCompat.START)) { // Закрыть меню, если открыто
            mDrawerLayout.closeDrawers();
        } else { // Закрыть Activity
            Log.d(TAG, "onBackPressed unregisterReceiver!");
            unregisterReceiver(br);
            finish();
        }
    }

    @Override
    public void onDestroy(){
        try {
            dialogDefections.dismiss();
            dialogPermissions.dismiss();
        } catch (Exception e) {
            Log.d(TAG, "dialog's dissmiss failed!");
        }
        try {
            MainActivity.this.unregisterReceiver(br);
            Log.d(TAG, "onDestroy unregisterReceiver!");
        } catch (Exception e) {
            Log.d(TAG, "unregisterReceiver failed!");
        }
        super.onDestroy();
    }

    /**
     * Отображение Activity
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "ON START!");

        try {
            MainActivity.this.unregisterReceiver(br);
            Log.d(TAG, "onBackPressed unregisterReceiver!");
        } catch (Exception e) {
            Log.d(TAG, "unregisterReceiver failed!");
        }

        br = new BroadcastReceiver() { // Перехватчик сообщений от локального сервиса проверки сессии и даты

            // Действия при получении сообщений
            public void onReceive(Context context, Intent intent) {
                int task = intent.getIntExtra(PARAM_TASK, 0);
                int status = intent.getIntExtra(PARAM_STATUS, 0);

                // Ловим сообщения об окончании задач
                if (status == STATUS_FINISH) {
                    int result = intent.getIntExtra(PARAM_RESULT, 0);

                    stopService(new Intent(getApplicationContext(), SessionAndDateCheckService.class));

                    switch (task) {
                        case TASK1_CODE: {

                            Log.d(TAG, "Message from Service received! " + result);

                            final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Предупреждение")
                                    .setMessage("Время сессии истекло, будет произведён переход на страницу авторизации")
                                    .setPositiveButton(android.R.string.ok, null)
                                    .setCancelable(false)
                                    .create();

                            dialog.setOnShowListener(new DialogInterface.OnShowListener() {

                                @Override
                                public void onShow(DialogInterface dialogInterface) {

                                    Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                                    button.setOnClickListener(new View.OnClickListener() {

                                        @Override
                                        public void onClick(View view) {

                                            dialog.dismiss();
                                            unregisterReceiver(br);
                                            Log.d(TAG, "unregisterReceiver from close dialog button!");
                                            //ProcessPhoenix.triggerRebirth(BaseActivity.this);
                                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            startActivity(intent);
                                        }
                                    });
                                }
                            });
                            dialog.show();
                        }
                        break;

                        case TASK2_CODE: {

                            Log.d(TAG, "Message from Service received! " + result);

                            final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Предупреждение")
                                    .setMessage("Дата устройства не соответствует дате сервера, будет произведён переход на страницу авторизации")
                                    .setPositiveButton(android.R.string.ok, null)
                                    .setCancelable(false)
                                    .create();

                            dialog.setOnShowListener(new DialogInterface.OnShowListener() {

                                @Override
                                public void onShow(DialogInterface dialogInterface) {

                                    Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                                    button.setOnClickListener(new View.OnClickListener() {

                                        @Override
                                        public void onClick(View view) {

                                            dialog.dismiss();
                                            unregisterReceiver(br);
                                            Log.d(TAG, "unregisterReceiver from close dialog button!");
                                            //ProcessPhoenix.triggerRebirth(BaseActivity.this);
                                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            startActivity(intent);
                                        }
                                    });
                                }
                            });
                            dialog.show();

                        }
                        break;
                    }
                }
            }
        };

        // Cоздаем фильтр для BroadcastReceiver
        intFilt = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(br, intFilt);
    }

    /**
     * Создание Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Предложить пользователю загрузить виды нарушений, если их нет на устройстве
        if (!isDefectionsDownloaded(MainActivity.this)) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE: {
                            callWebService("getDefections", LDR_BASIC_ID_GET_DEFECTIONS);
                        }
                        break;
                        case DialogInterface.BUTTON_NEGATIVE: { }
                        break;
                    }
                }
            };
            dialogDefections = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Уведомление")
                    .setMessage("Видов нарушений не найдено. Загрузить?")
                    .setPositiveButton("Да", dialogClickListener)
                    .setNegativeButton("Отмена", dialogClickListener)
                    .setCancelable(false)
                    .create();
            dialogDefections.show();
        }

        // Отобразить диалоговое окно, если не предоставлены разрешения на доступ к камере и хранилищу
        if (!verifyPermissions(MainActivity.this)) {
            stopRotateScreen();
            dialogPermissions = new AlertDialog.Builder(MainActivity.this)
                    //.setView(v)
                    .setTitle("Уведомление")
                    .setMessage("Для корректной работы приложения необходимо предоставить разрешения на доступ к камере и хранилищу")
                    .setPositiveButton("Принять", null)
                    .setCancelable(false)
                    .create();

            dialogPermissions.setOnShowListener(new DialogInterface.OnShowListener() {

                @Override
                public void onShow(DialogInterface dialogInterface) {

                    Button button = ((AlertDialog) dialogPermissions).getButton(AlertDialog.BUTTON_POSITIVE);
                    button.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            // Закрыть диалоговое окно, если предоставлены разрешения на доступ к камере и хранилищу
                            if (verifyPermissions(MainActivity.this)) {
                                dialogPermissions.dismiss();
                                startRotateScreen();
                            }
                        }
                    });
                }
            });
            dialogPermissions.show();
        }

        // Navigation View (выпадающее меню)
        mDrawerLayout = (DrawerLayout) findViewById(R.id.coordinatorLayout);
        mToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.Open, R.string.Close);
        mDrawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        navigationView = (NavigationView) findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);

        // Получение информации о пользователе с RepresentActivity или LoginActivity
        Intent intent = getIntent();
        String jsonUserInfo = intent.getStringExtra("jsonUserInfo");
        Gson gson = new GsonBuilder().create();
        userInfo = gson.fromJson(jsonUserInfo, DatabaseLoginResponse.class);

        // TODO: DEBUG
        /*showToast(MainActivity.this, "of_id=" + userInfo.represents.get(userInfo.selected_represent_position).official_id
                + "\nrep_id=" + userInfo.represents.get(userInfo.selected_represent_position).id
        + "\ndate=" + userInfo.webServiceDate);*/
        //userInfo.represents.get(userInfo.selected_represent_position).official_id = "67";
        //userInfo.represents.get(userInfo.selected_represent_position).id = "71";
        //userInfo.webServiceDate = "20-10-2019";

        // Создаём БД с планами для текущей даты
        createPlansDatabaseForCurrentDate(MainActivity.this, userInfo);
        // TODO: Нужно ли удалять старые БД, дата которых не соответствует текущей,
        //  или же может потребоваться возможность экспорта?

        // Отобразить текущую дату в выпадающем меню
        String date_tmp = userInfo.webServiceDate;
        TextView dhTextViewDate = (TextView) headerView.findViewById(R.id.dhTextViewDate);
        dhTextViewDate.setText(date_tmp);

        // Отобразить ФИО авторизованного пользователя в выпадающем меню
        String fio_tmp = userInfo.last_name + " " + userInfo.first_name + " " + userInfo.second_name;
        TextView dhTextViewUser = (TextView) headerView.findViewById(R.id.dhTextViewUser);
        dhTextViewUser.setText(fio_tmp);

        // Отобразить подразделение авторизованного пользователя в выпадающем меню
        String represent_tmp = userInfo.represents.get(userInfo.selected_represent_position).label;
        TextView dhTextViewSubv = (TextView) headerView.findViewById(R.id.dhTextViewSubv);
        dhTextViewSubv.setText(represent_tmp);

        // Приветственное сообщение с краткой инструкцией по приложению
        sbTextView = (TextView) findViewById(R.id.sbTextView);

        // Список планов и подконтрольных объектов
        sbLinearLayout = (LinearLayout) findViewById(R.id.sbLinearLayout);
    }

    /**
     * Действие при нажатии на подконтрольный объект
     */
    @Override
    public void onObjectClick(View view, final int position) {

        currentObjectPosition = position; // Запоминаем позицию объекта

        // Формируем диалоговое окно проверки
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view111 = inflater.inflate(R.layout.dialog_delicts_list, null);
        mBuilder.setView(view111);
        mBuilder.setCancelable(false);
        mDialog = mBuilder.create();

        // Выбранный объект был проверен и с нарушениями
        if (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).is_checked && plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts != null) {
            mDialog.setView(initDelictsList()); // Отображаем окно со списком нарушений
        }
        else { // Объект без проверки или без нарушений
            mDialog.setView(initNewCheck()); // Отображаем окно с новой проверкой
        }
        mDialog.show();
        stopRotateScreen();
        mDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }

    /**
     * Navigation View - кнопки выпадающего меню
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {

            case R.id.getPlansAndObjects: { // Загрузка планов обхода с сервера на устройство
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE: {

                                callWebService("getPlans", LDR_BASIC_ID_GET_PLANS); // Загрузка планов обхода с сервера в локальную БД

                                sbLinearLayout.setVisibility(View.GONE);
                                sbTextView.setVisibility(View.VISIBLE);
                                break;
                            }
                            case DialogInterface.BUTTON_NEGATIVE: {
                                break;
                            }
                        }
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Загрузка планов")
                        .setMessage("Старые планы и текущий прогресс будут удалены! Вы действительно хотите продолжить загрузку?")
                        .setPositiveButton("Да", dialogClickListener)
                        .setNegativeButton("Отмена", dialogClickListener)
                        .show();
            }
            break;

            case R.id.showPlans: { // Открыть загруженные планы обхода из локальной БД

                defections_tmp = getDefectionsFromDatabase(MainActivity.this); // Загружаем виды нарушений из локальной БД в список видов нарушений

                if (defections_tmp == null) {
                    showToast(MainActivity.this, "Виды нарушений на устройстве не найдены");
                    break;
                }

                plans_tmp = getPlansFromDatabase(MainActivity.this, userInfo); // Загружаем планы из локальной БД в список объектов

                if (plans_tmp == null) {
                    showToast(MainActivity.this, "Планы на устройстве не найдены");
                    break;
                }

                // Отображение планов и объектов
                sbTextView.setVisibility(View.GONE);
                sbLinearLayout.setVisibility(View.VISIBLE);

                // Адаптер списка планов
                spinnerPlan = (Spinner) findViewById(R.id.spinnerPlan);
                AdapterPlanSpinner mPlanAdapter = new AdapterPlanSpinner(this, R.layout.spinner_plan, plans_tmp);
                spinnerPlan.setAdapter(mPlanAdapter);
                spinnerPlan.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                        currentPlanPosition = position; // Устанавливаем позицию текущего плана при выборе в адаптере

                        mORecyclerView = (RecyclerView) findViewById(R.id.mRecyclerView);
                        mORecyclerView.setHasFixedSize(true);
                        mORecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));//Linear Items

                        ORViewAdapter = new PlanRecyclerViewAdapter(MainActivity.this, plans_tmp.get(position).pobjects);
                        ORViewAdapter.setClickListener(MainActivity.this);
                        mORecyclerView.setAdapter(ORViewAdapter);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

                mDrawerLayout.closeDrawers(); // Закрыть меню
            }
            break;

            case R.id.sendObjects: { // Отправить результат обхода на сервер

                plans_tmp = getPlansFromDatabase(MainActivity.this, userInfo);

                if (plans_tmp == null) {
                    showToast(MainActivity.this, "Планы на устройстве не найдены");
                    break;
                }

                AlertDialog.Builder mSendResultBuilder = new AlertDialog.Builder(MainActivity.this);
                LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    final View view333 = inflater.inflate(R.layout.dialog_sendresult, null);
                mSendResultBuilder.setView(view333);
                mSendResultBuilder.setCancelable(false);
                final AlertDialog mSendResultDialog = mSendResultBuilder.create();

                Button btnSelectAll = (Button) view333.findViewById(R.id.btnSelectAll);
                Button dbtnClose = (Button) view333.findViewById(R.id.dbtnClose);
                Button dbtnSave = (Button) view333.findViewById(R.id.dbtnSave);


                SendResultAdapter sendResultAdapter = new SendResultAdapter(MainActivity.this, plans_tmp);
                ListView lvMain = (ListView) view333.findViewById(R.id.listview_sendresult);
                lvMain.setAdapter(sendResultAdapter);

                dbtnClose.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mSendResultDialog.dismiss();
                    }
                });

                btnSelectAll.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        for (Plan p : plans_tmp) {
                            p.checkBoxIsChecked = true;
                        }

                        SendResultAdapter sendResultAdapter = new SendResultAdapter(MainActivity.this, plans_tmp);
                        ListView lvMain = (ListView) view333.findViewById(R.id.listview_sendresult);
                        lvMain.setAdapter(sendResultAdapter);
                    }
                });


                dbtnSave.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        boolean anyCheckBoxIsChecked = false; // Выбран хотя бы один план для выгрузки

                        for (Plan p : plans_tmp) { // Планы

                            if (p.checkBoxIsChecked) {
                                anyCheckBoxIsChecked = true; // Выбран хотя бы один план для отправки
                                boolean checkInPlanFounded = false;

                                for (PObject o : p.pobjects) { // Объекты

                                    if (o.is_checked) { // Имеется хотя бы одна проверка
                                        checkInPlanFounded = true;
                                        break;
                                    }
                                }

                                if (!checkInPlanFounded) {
                                    showToast(MainActivity.this, "Не удалось отправить резульат. В выбранном(ых) плане(ах) должна быть хотя бы одна проверка");
                                    return;
                                }
                            }
                        }

                        if (anyCheckBoxIsChecked) { // Выбран хотя бы один план для выгрузки

                            List<PObject> objects_for_sending = new ArrayList<>();

                            for (Plan p : plans_tmp) { // Обход всех планов
                                if (p.checkBoxIsChecked) { // План выбран для выгрузки
                                    for (PObject o : p.pobjects) { // Обход всех объектов плана

                                        if (o.is_checked && o.delicts != null) { // Объект с нарушениями

                                            List<Delict> delicts_for_sending = new ArrayList<>();
                                            for (Delict d : o.delicts) { // Обход всех нарушений объекта

                                                delicts_for_sending.add(new Delict(d.defection_id, d.defection_name, d.time, d.photo));
                                            }

                                            objects_for_sending.add(new PObject(o.sub_control_id, o.address_id, o.address_place, o.photo_panorama, o.photo_house_number, delicts_for_sending));

                                        } else if (o.is_checked) { // Объект без нарушений

                                            objects_for_sending.add(new PObject(o.sub_control_id, o.address_id, o.address_place, o.photo_panorama, o.photo_house_number));
                                        }
                                    }
                                }
                            }

                            // Добавляем список выгружаемых планов
                            List<String> plans_id_tmp = new ArrayList();
                            for(int i = 0; i < plans_tmp.size(); i++){
                                if(plans_tmp.get(i).checkBoxIsChecked) {
                                    plans_id_tmp.add(plans_tmp.get(i).id);
                                }
                            }

                            // Формируем JSON строку с результатом обхода для отправки на сервер
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            SendObjects so = new SendObjects();
                            so.objects = objects_for_sending;
                            so.plans_id = plans_id_tmp;
                            objects_for_send_JSON_str = gson.toJson(so);

                            callWebService("sendObjects", LDR_BASIC_ID_SEND_OBJECTS); // Отправляем JSON строку на сервер

                        } else {
                            showToast(MainActivity.this, "Выберите планы для отправки");
                            return;
                        }
                    }
                });

                mSendResultDialog.show();

            }
            break;

            case R.id.getDictionary: { // Загрузка видов нарушений
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE: {

                                sbLinearLayout.setVisibility(View.GONE);
                                sbTextView.setVisibility(View.VISIBLE);

                                callWebService("getDefections", LDR_BASIC_ID_GET_DEFECTIONS); // Загрузка видов нарушений с сервера

                                break;
                            }
                            case DialogInterface.BUTTON_NEGATIVE: {
                                break;
                            }
                        }
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Загрузка видов нарушений")
                        .setMessage("Вы действительно хотите загрузить виды нарушений?")
                        .setPositiveButton("Да", dialogClickListener)
                        .setNegativeButton("Отмена", dialogClickListener)
                        .show();
            }
            break;
        }
        return true;
    }

    /**
     * Формирование параметров для загрузчика и его вызов (Loader) для загрузки/вызгрузки данных с сервера/на сервер
     */
    private void callWebService(String callWebServiceFunction, int loader_id) {

        // Формируем необходимые данные для запроса на сервер
        String DEVICE_ID = "Unknown"; // Уникальный идентификатор устройства IMEI на GSM, MEID для CDMA)
        int TIMEOUT = 0;
        try {
            TIMEOUT = getResources().getInteger((R.integer.TIMEOUT));
        } catch (Exception e) {
            TIMEOUT = 5000;
        }
        // Получаем уникальный идентификатор устройства
        try {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            DEVICE_ID = telephonyManager.getDeviceId().toString();
        } catch (SecurityException e) {
            Log.d(TAG, "SecurityException: " + e);
        }

        // Свёрток данных для Loader'a
        Bundle infoBundle = new Bundle();

        // Данные для подключения к веб-сервису
        infoBundle.putString("webServiceFunction", callWebServiceFunction);
        infoBundle.putString("namespace", getString(R.string.WebServiceNAMESPACE));
        infoBundle.putString("url", getString(R.string.WebServiceURL));
        infoBundle.putInt("timeout", TIMEOUT);
        infoBundle.putInt("loader_id", loader_id);

        // Пользовательские данные для разных загрузчиков
        if (loader_id == LDR_BASIC_ID_GET_DEFECTIONS) {

            infoBundle.putString("device_id", DEVICE_ID);

        } else if (loader_id == LDR_BASIC_ID_GET_PLANS) {

            infoBundle.putString("official_id", userInfo.represents.get(userInfo.selected_represent_position).official_id);
            infoBundle.putString("represent_id", userInfo.represents.get(userInfo.selected_represent_position).id);
            infoBundle.putString("device_id", userInfo.webServiceDate);

        } else if (loader_id == LDR_BASIC_ID_SEND_OBJECTS) {

            infoBundle.putString("objects", objects_for_send_JSON_str);
            infoBundle.putString("user_id", userInfo.user_id);
            infoBundle.putString("device_id", DEVICE_ID);
        }

        // Запуск (перезапуск) загрузчика
        Log.d(TAG, "Перезапуск загрузчика: " + callWebServiceFunction);
        getSupportLoaderManager().restartLoader(loader_id, infoBundle, MainActivity.this);
        //getSupportLoaderManager().restartLoader(LDR_BASIC_ID_GET_DEFECTIONS, infoBundle, LoginActivity.this);
        getSupportLoaderManager().initLoader(loader_id, infoBundle, MainActivity.this);
    }

    // Кнопка Принять при редактировании нарушения
    public View.OnClickListener initAcceptEditDelictButtonListener() {
        View.OnClickListener Result = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Получаем id, описание нарушения и время
                String selected_defection_id = defections_tmp.get(currentDefectionPosition).id;
                String selected_defection_label = defections_tmp.get(currentDefectionPosition).label; // Краткое вида описание нарушения
                String selected_defection_name = detDescription.getText().toString().trim();
                String selected_time = detTime.getText().toString().trim();

                if (selected_defection_name.length() == 0) {
                    showToast(MainActivity.this, "Описание правонарушения отсутствует. Данное поле не может быть пустым");
                    return;
                }

                if (selected_time.length() == 0) {
                    showToast(MainActivity.this, "Время правонарушения отсутствует. Данное поле не может быть пустым");
                    return;
                }

                if (selected_time.length() != 5) {
                    showToast(MainActivity.this, "Время правонарушения введено неверно. Пример ввода: 13:54");
                    return;
                }

                // Проверка ввода формата времени
                try {

                    String[] tmp_words = selected_time.split(":", 2);

                    int hours = Integer.parseInt(tmp_words[0]);
                    int minutes = Integer.parseInt(tmp_words[1]);

                    if ((hours < 0 || hours > 24) || (minutes < 0 || minutes > 60)) {
                        showToast(MainActivity.this, "Время правонарушения введено неверно. Пример ввода: 13:54");
                        return;
                    }
                } catch (Exception e) {
                    showToast(MainActivity.this, "Время правонарушения введено неверно. Пример ввода: 13:54");
                    return;
                }

                ContentValues cv = new ContentValues();
                cv.put("defection_id", selected_defection_id);
                cv.put("defection_label", selected_defection_label);
                cv.put("defection_name", selected_defection_name);
                cv.put("time", selected_time);
                cv.put("photo", plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo);
                String current_sub_control_id = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).sub_control_id;
                updateDelictInDatabase(MainActivity.this, cv,  userInfo, current_sub_control_id, currentDelictPosition); // Обновление нарушения в локальной БД

                // Заносим изменённые данные о нарушении в список планов
                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.set(currentDelictPosition, new Delict(
                        currentDelictPosition,
                        selected_defection_id,
                        selected_defection_label,
                        selected_defection_name,
                        selected_time,
                        plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo));

                ORViewAdapter.notifyItemChanged(currentObjectPosition);
                mDialog.setContentView(initDelictsList());
            }
        };

        return Result;
    }

    // Кнопка Принять при добавлении первого нарушения
    public View.OnClickListener initAcceptFirstDelictButtonListener() {
        View.OnClickListener Result = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Получаем id, описание нарушения и время
                String selected_defection_id = defections_tmp.get(currentDefectionPosition).id;
                String selected_defection_label = defections_tmp.get(currentDefectionPosition).label; // Краткое вида описание нарушения
                String selected_defection_name = detDescription.getText().toString().trim();
                String selected_time = detTime.getText().toString().trim();

                if (selected_defection_name.length() == 0) {
                    showToast(MainActivity.this, "Описание правонарушения отсутствует. Данное поле не может быть пустым");
                    return;
                }

                if (selected_time.length() == 0) {
                    showToast(MainActivity.this, "Время правонарушения отсутствует. Данное поле не может быть пустым");
                    return;
                }

                if (selected_time.length() != 5) {
                    showToast(MainActivity.this, "Время правонарушения введено неверно. Пример ввода: 13:54");
                    return;
                }

                try { // Проверка ввода формата времени
                    String[] tmp_words = selected_time.split(":", 2);

                    int hours = Integer.parseInt(tmp_words[0]);
                    int minutes = Integer.parseInt(tmp_words[1]);

                    if ((hours < 0 || hours > 24) || (minutes < 0 || minutes > 60)) {
                        showToast(MainActivity.this, "Время правонарушения введено неверно. Пример ввода: 13:54");
                        return;
                    }
                } catch (Exception e) {
                    showToast(MainActivity.this, "Время правонарушения введено неверно. Пример ввода: 13:54");
                    return;
                }

                // Устанавливаем признак проверки
                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).is_checked = true;

                // Добавляем признак обхода в локальную БД
                ContentValues cv_tmp = new ContentValues();
                cv_tmp.put("is_checked", 1);
                String current_sub_control_id = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).sub_control_id;
                updateObjectCheckStatusInDatabase(MainActivity.this, cv_tmp, userInfo, current_sub_control_id);

                // Добавление нарушения в локальную БД
                ContentValues cv = new ContentValues();
                cv.put("id", 0);
                cv.put("sub_control_id", plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).sub_control_id);
                cv.put("defection_id", selected_defection_id);
                cv.put("defection_label", selected_defection_label);
                cv.put("defection_name", selected_defection_name);
                cv.put("time", selected_time);
                cv.put("photo", plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo);
                insertDelictInDatabase(MainActivity.this, cv, userInfo);

                // Запоминаем текущие данные о нарушении
                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.set(currentDelictPosition, new Delict(
                        0,
                        selected_defection_id,
                        selected_defection_label,
                        selected_defection_name,
                        selected_time,
                        plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo));

                ORViewAdapter.notifyItemChanged(currentObjectPosition);
                mDialog.setContentView(initDelictsList());
            }
        };

        return Result;
    }

    // Кнопка Принять при добавлении нарушения (к имеющимся нарушениям)
    public View.OnClickListener initAddDelictAcceptButtonListener() {
        View.OnClickListener Result = new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Получаем id, описание нарушения и время
                String selected_defection_id = defections_tmp.get(currentDefectionPosition).id;
                String selected_defection_label = defections_tmp.get(currentDefectionPosition).label; // Краткое вида описание нарушения
                String selected_defection_name = detDescription.getText().toString().trim();
                String selected_time = detTime.getText().toString().trim();

                // Проверка на использование вида нарушения этим объектом
                if (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts != null) {
                    // Перебор нарушений на поиск уже выбранного вида нарушения
                    int delicts_size = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.size();
                    for (int i = 0; i < delicts_size - 1; i++) { // обходимо все нарушения, кроме нового (только что созданного)

                        String defection_label_tmp = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(i).defection_label;

                        if (selected_defection_label.equals(defection_label_tmp)) {
                            showToast(MainActivity.this, "Данный вид нарушения уже используется");
                            return;
                        }
                    }
                }

                if (selected_defection_name.length() == 0) {
                    showToast(MainActivity.this, "Описание правонарушения отсутствует. Данное поле не может быть пустым");
                    return;
                }

                if (selected_time.length() == 0) {
                    showToast(MainActivity.this, "Время правонарушения отсутствует. Данное поле не может быть пустым");
                    return;
                }

                if (selected_time.length() != 5) {
                    showToast(MainActivity.this, "Время правонарушения введено неверно. Пример ввода: 13:54");
                    return;
                }

                try { // Проверка ввода формата времени

                    String[] tmp_words = selected_time.split(":", 2);

                    int hours = Integer.parseInt(tmp_words[0]);
                    int minutes = Integer.parseInt(tmp_words[1]);

                    if ((hours < 0 || hours > 24) || (minutes < 0 || minutes > 60)) {
                        showToast(MainActivity.this, "Время правонарушения введено неверно. Пример ввода: 13:54");
                        return;
                    }
                } catch (Exception e) {
                    showToast(MainActivity.this, "Время правонарушения введено неверно. Пример ввода: 13:54");
                    return;
                }

                byte[] photo_tmp = null;
                if (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo != null) {
                    photo_tmp = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo;
                }

                // Изменяем нарушение в списке нарушений
                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.set(currentDelictPosition, new Delict(
                        currentDelictPosition,
                        selected_defection_id,
                        selected_defection_label,
                        selected_defection_name,
                        selected_time,
                        photo_tmp));

                // Добавление нарушения в локальную БД
                ContentValues cv = new ContentValues();
                cv.put("id", currentDelictPosition);
                cv.put("sub_control_id", plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).sub_control_id);
                cv.put("defection_id", selected_defection_id);
                cv.put("defection_label", selected_defection_label);
                cv.put("defection_name", selected_defection_name);
                cv.put("time", selected_time);
                cv.put("photo", plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo);
                insertDelictInDatabase(MainActivity.this, cv, userInfo);

                ORViewAdapter.notifyItemChanged(currentObjectPosition); // Обновляем элемент на экране
                mDialog.setContentView(initDelictsList()); // Отображаем список нарушений
            }
        };

        return Result;
    }

    // Диалоговое окно с превью и добавлениием фото к объекту
    public View initFirstDelictPhotoAction(byte[] photo) {
        LayoutInflater photoInflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View photoView = photoInflater.inflate(R.layout.dialog_photo, null);

        // Заполняем заголовок
        TextView dtvObjectName = (TextView) photoView.findViewById(R.id.dtvObjectName);
        dtvObjectName.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).full_name);

        // Заполняем подзаголовок
        TextView dtvDialogType = (TextView) photoView.findViewById(R.id.dtvDialogType);
        dtvDialogType.setText("Фото нарушения по объекту");

        // Превью фото
        ivPhotoPreview = (ImageView) photoView.findViewById(R.id.ivPhotoPreview);
        if (photo != null) {
            ivPhotoPreview.setVisibility(View.VISIBLE);
            Bitmap bitmap_tmp = BitmapFactory.decodeByteArray(photo, 0, photo.length);
            ivPhotoPreview.setImageBitmap(bitmap_tmp);
        }

        // Сделать фото
        Button dbtnMakePhoto = (Button) photoView.findViewById(R.id.dbtnMakePhoto);
        dbtnMakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent cameraIntent = new Intent(MainActivity.this, CameraActivity.class);
                int orientation = MainActivity.this.getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    cameraIntent.putExtra("orientation", "portrait");
                } else {
                    cameraIntent.putExtra("orientation", "landscape");
                }
                startActivityForResult(cameraIntent, DELICT_FROM_CAMERA_REQUEST);
            }
        });

        // Выбрать из галереи
        Button dbtnChoosePhoto = (Button) photoView.findViewById(R.id.dbtnChoosePhoto);
        dbtnChoosePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent galleryIntent = new Intent(MainActivity.this, GalleryActivity.class);
                int orientation = MainActivity.this.getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    galleryIntent.putExtra("orientation", "portrait");
                } else {
                    galleryIntent.putExtra("orientation", "landscape");
                }
                startActivityForResult(galleryIntent, DELICT_FROM_GALLERY_REQUEST);
            }
        });

        // Нажата кнопка Вернуться к списку нарушений
        Button dbtnBackToDelicts = (Button) photoView.findViewById(R.id.dbtnBackToDelicts);
        dbtnBackToDelicts.setText("Вернуться к нарушению");

        dbtnBackToDelicts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ivPhotoPreview.setVisibility(View.GONE);
                currentDelictPosition = 0;
                mDialog.setContentView(initEditFirstDelict());
            }
        });

        return photoView;
    }

    // Диалоговое окно с превью и добавлениием фото к объекту
    public View initAddDelictPhotoAction(byte[] photo) {
        LayoutInflater photoInflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View photoView = photoInflater.inflate(R.layout.dialog_photo, null);

        // Заполняем заголовок
        TextView dtvObjectName = (TextView) photoView.findViewById(R.id.dtvObjectName);
        dtvObjectName.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).full_name);

        // Заполняем подзаголовок
        TextView dtvDialogType = (TextView) photoView.findViewById(R.id.dtvDialogType);
        dtvDialogType.setText("Фото нарушения по объекту");

        // Превью фото
        ivPhotoPreview = (ImageView) photoView.findViewById(R.id.ivPhotoPreview);
        if (photo != null) {
            ivPhotoPreview.setVisibility(View.VISIBLE);
            Bitmap bitmap_tmp = BitmapFactory.decodeByteArray(photo, 0, photo.length);
            ivPhotoPreview.setImageBitmap(bitmap_tmp);
        }

        // Сделать фото
        Button dbtnMakePhoto = (Button) photoView.findViewById(R.id.dbtnMakePhoto);
        dbtnMakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent cameraIntent = new Intent(MainActivity.this, CameraActivity.class);
                int orientation = MainActivity.this.getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    cameraIntent.putExtra("orientation", "portrait");
                } else {
                    cameraIntent.putExtra("orientation", "landscape");
                }
                startActivityForResult(cameraIntent, DELICT_FROM_CAMERA_REQUEST);
            }
        });

        // Выбрать из галереи
        Button dbtnChoosePhoto = (Button) photoView.findViewById(R.id.dbtnChoosePhoto);
        dbtnChoosePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent galleryIntent = new Intent(MainActivity.this, GalleryActivity.class);
                int orientation = MainActivity.this.getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    galleryIntent.putExtra("orientation", "portrait");
                } else {
                    galleryIntent.putExtra("orientation", "landscape");
                }
                startActivityForResult(galleryIntent, DELICT_FROM_GALLERY_REQUEST);
            }
        });

        // Нажата кнопка Вернуться к списку нарушений
        Button dbtnBackToDelicts = (Button) photoView.findViewById(R.id.dbtnBackToDelicts);
        dbtnBackToDelicts.setText("Вернуться к нарушению");

        dbtnBackToDelicts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ivPhotoPreview.setVisibility(View.GONE);
                mDialog.setContentView(initEditAddedNewDelict());
            }
        });

        return photoView;
    }


    // Диалоговое окно с превью и добавлениием фото к объекту
    public View initEditDelictPhotoAction(byte[] photo) {
        LayoutInflater photoInflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View photoView = photoInflater.inflate(R.layout.dialog_photo, null);

        // Заполняем заголовок
        TextView dtvObjectName = (TextView) photoView.findViewById(R.id.dtvObjectName);
        dtvObjectName.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).full_name);

        // Заполняем подзаголовок
        TextView dtvDialogType = (TextView) photoView.findViewById(R.id.dtvDialogType);
        dtvDialogType.setText("Фото нарушения по объекту");

        // Превью фото
        ivPhotoPreview = (ImageView) photoView.findViewById(R.id.ivPhotoPreview);
        if (photo != null) {
            ivPhotoPreview.setVisibility(View.VISIBLE);
            Bitmap bitmap_tmp = BitmapFactory.decodeByteArray(photo, 0, photo.length);
            ivPhotoPreview.setImageBitmap(bitmap_tmp);
        }

        // Сделать фото
        Button dbtnMakePhoto = (Button) photoView.findViewById(R.id.dbtnMakePhoto);
        dbtnMakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent cameraIntent = new Intent(MainActivity.this, CameraActivity.class);
                int orientation = MainActivity.this.getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    cameraIntent.putExtra("orientation", "portrait");
                } else {
                    cameraIntent.putExtra("orientation", "landscape");
                }
                startActivityForResult(cameraIntent, DELICT_FROM_CAMERA_REQUEST);
            }
        });

        // Выбрать из галереи
        Button dbtnChoosePhoto = (Button) photoView.findViewById(R.id.dbtnChoosePhoto);
        dbtnChoosePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent galleryIntent = new Intent(MainActivity.this, GalleryActivity.class);
                int orientation = MainActivity.this.getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    galleryIntent.putExtra("orientation", "portrait");
                } else {
                    galleryIntent.putExtra("orientation", "landscape");
                }
                startActivityForResult(galleryIntent, DELICT_FROM_GALLERY_REQUEST);
            }
        });

        // Нажата кнопка Вернуться к списку нарушений
        Button dbtnBackToDelicts = (Button) photoView.findViewById(R.id.dbtnBackToDelicts);
        dbtnBackToDelicts.setText("Вернуться к нарушению");

        dbtnBackToDelicts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ivPhotoPreview.setVisibility(View.GONE);
                mDialog.setContentView(initEditDelict());
            }
        });

        return photoView;
    }

    // Диалоговое окно с превью и добавлениием фото к объекту
    public View initPhotoObjectAction(final int requestCodeType, byte[] photo) {
        LayoutInflater photoInflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View photoView = photoInflater.inflate(R.layout.dialog_photo, null);

        // Заполняем заголовок
        TextView dtvObjectName = (TextView) photoView.findViewById(R.id.dtvObjectName);
        dtvObjectName.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).full_name);

        // Заполняем подзаголовок
        TextView dtvDialogType = (TextView) photoView.findViewById(R.id.dtvDialogType);
        switch (requestCodeType) {
            case PANORAMA_REQUEST:
                dtvDialogType.setText("Фото панорамы объекта");
                break;

            case HOUSE_NUMBER_REQUEST:
                dtvDialogType.setText("Фото номера дома объекта");
                break;
        }

        // Превью фото
        ivPhotoPreview = (ImageView) photoView.findViewById(R.id.ivPhotoPreview);
        if (photo != null) {
            ivPhotoPreview.setVisibility(View.VISIBLE);
            Bitmap bitmap_tmp = BitmapFactory.decodeByteArray(photo, 0, photo.length);
            ivPhotoPreview.setImageBitmap(bitmap_tmp);
        }

        // Сделать фото
        Button dbtnMakePhoto = (Button) photoView.findViewById(R.id.dbtnMakePhoto);
        dbtnMakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent cameraIntent = new Intent(MainActivity.this, CameraActivity.class);
                int orientation = MainActivity.this.getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    cameraIntent.putExtra("orientation", "portrait");
                } else {
                    cameraIntent.putExtra("orientation", "landscape");
                }
                switch (requestCodeType) {
                    case PANORAMA_REQUEST:
                        startActivityForResult(cameraIntent, PANORAMA_FROM_CAMERA_REQUEST);
                        break;

                    case HOUSE_NUMBER_REQUEST:
                        startActivityForResult(cameraIntent, HOUSE_NUMBER_FROM_CAMERA_REQUEST);
                        break;
                }
            }
        });

        // Выбрать из галереи
        Button dbtnChoosePhoto = (Button) photoView.findViewById(R.id.dbtnChoosePhoto);
        dbtnChoosePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent galleryIntent = new Intent(MainActivity.this, GalleryActivity.class);
                int orientation = MainActivity.this.getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    galleryIntent.putExtra("orientation", "portrait");
                } else {
                    galleryIntent.putExtra("orientation", "landscape");
                }
                switch (requestCodeType) {
                    case PANORAMA_REQUEST:
                        startActivityForResult(galleryIntent, PANORAMA_FROM_GALLERY_REQUEST);
                        break;

                    case HOUSE_NUMBER_REQUEST:
                        startActivityForResult(galleryIntent, HOUSE_NUMBER_FROM_GALLERY_REQUEST);
                        break;
                }
            }
        });

        // Нажата кнопка Вернуться к списку нарушений
        Button dbtnBackToDelicts = (Button) photoView.findViewById(R.id.dbtnBackToDelicts);

        dbtnBackToDelicts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ivPhotoPreview.setVisibility(View.GONE);
                mDialog.setContentView(initDelictsList());
            }
        });

        return photoView;
    }

    // Окно при добавлении первой проверки
    public View initNewCheck() {

        LayoutInflater CheckInflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View mCheckView = CheckInflater.inflate(R.layout.dialog_check, null);

        // Заполняем заголовок
        TextView dtvObjectName = (TextView) mCheckView.findViewById(R.id.dtvObjectName);
        dtvObjectName.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).full_name);

        // Нажата кнопка Без замечаний
        Button dbtnNoDelict = (Button) mCheckView.findViewById(R.id.dbtnNoDelict);
        dbtnNoDelict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).is_checked = true;
                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_panorama = null;
                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_house_number = null;

                // Создаем объект для данных
                ContentValues cv_tmp = new ContentValues();
                cv_tmp.put("is_checked", 1);
                byte[] b_tmp =  null;
                cv_tmp.put("photo_panorama", b_tmp);
                cv_tmp.put("photo_house_number", b_tmp);

                String current_sub_control_id = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).sub_control_id;
                updateObjectCheckStatusInDatabase(MainActivity.this, cv_tmp, userInfo, current_sub_control_id);

                ORViewAdapter.notifyItemChanged(currentObjectPosition);
                mDialog.dismiss();
                startRotateScreen();
            }
        });

        // Нажата кнопка Есть нарушения (добавляется первое нарушение)
        Button dbtnAddFirstDelict = (Button) mCheckView.findViewById(R.id.dbtnAddDelict);
        dbtnAddFirstDelict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                currentDelictPosition = 0;

                // Добавление первого нарушения
                LayoutInflater addDelictInflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View addDelictView = addDelictInflater.inflate(R.layout.dialog_add_and_edit_delict, null);
                mDialog.setContentView(addDelictView);

                // Заполняем заголовок
                TextView dtvObjectName = (TextView) addDelictView.findViewById(R.id.dtvObjectName);
                dtvObjectName.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).full_name);

                // Заполняем описание
                TextView dtvDescription = (TextView) addDelictView.findViewById(R.id.dtvDescription);
                dtvDescription.setText("Добавление нарушения");

                // Инициализация описания и времени нарушения
                detDescription = (EditText) addDelictView.findViewById(R.id.detDescription);

                detTime = (EditText) addDelictView.findViewById(R.id.detTime);
                DateFormat df = new SimpleDateFormat("HH:mm");
                String currentTime = df.format(Calendar.getInstance().getTime());
                detTime.setText(currentTime);

                // Адаптер видов нарушений
                spinnerDefection = (Spinner) addDelictView.findViewById(R.id.spinnerDefection);
                AdapterDefectionSpinner mDefectionAdapter = new AdapterDefectionSpinner(MainActivity.this, R.layout.spinner_defection, defections_tmp);
                spinnerDefection.setAdapter(mDefectionAdapter);
                spinnerDefection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                        currentDefectionPosition = position;
                        detDescription.setText(defections_tmp.get(position).name);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                // Фото нарушения
                ivPhotoDelict = (ImageView) addDelictView.findViewById(R.id.ivPhotoDelict);
                ivPhotoDelict.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Если не даны разрешения
                        if (!verifyPermissions(MainActivity.this)) {
                            return;
                        }

                        // Получаем id, описание нарушения и время
                        String selected_defection_id = defections_tmp.get(currentDefectionPosition).id;
                        String selected_defection_label = defections_tmp.get(currentDefectionPosition).label; // Краткое вида описание нарушения
                        String selected_defection_name = detDescription.getText().toString().trim();
                        String selected_time = detTime.getText().toString().trim();
                        byte[] photo_ = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo;

                        // Запоминаем текущие данные о нарушении
                        plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.set(0, new Delict(
                                0,
                                selected_defection_id,
                                selected_defection_label,
                                selected_defection_name,
                                selected_time,
                                photo_));

                        // Открываем представление для добавления или просмотра фото
                        mDialog.setContentView(initFirstDelictPhotoAction(photo_));
                    }
                });

                // Создаём контейнер с нарушениями
                List<Delict> delicts_tmp = new ArrayList();

                // Создаём и добавляем пустое нарушение
                delicts_tmp.add(new Delict(
                        0,
                        null,
                        null,
                        null,
                        null,
                        null));

                // Добавляем контейнер с пустым нарушением к объекту
                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts = delicts_tmp;

                Button dbtnAcceptFirstDelict = (Button) addDelictView.findViewById(R.id.dbtnAccept);
                dbtnAcceptFirstDelict.setOnClickListener(initAcceptFirstDelictButtonListener());
            }
        });

        // Нажата кнопка Без проверки
        Button dbtnNotCheked = (Button) mCheckView.findViewById(R.id.dbtnNotCheked);
        dbtnNotCheked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).is_checked = false;
                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_panorama = null;
                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_house_number = null;

                ContentValues cv_tmp = new ContentValues();
                cv_tmp.put("is_checked", 0);
                byte[] b_tmp =  null;
                cv_tmp.put("photo_panorama", b_tmp);
                cv_tmp.put("photo_house_number", b_tmp);

                String current_sub_control_id = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).sub_control_id;
                updateObjectCheckStatusInDatabase(MainActivity.this, cv_tmp, userInfo, current_sub_control_id);

                ORViewAdapter.notifyItemChanged(currentObjectPosition);

                mDialog.dismiss();
                startRotateScreen();
            }
        });

        return mCheckView;
    }

    // Окно списка нарушений
    public View initDelictsList() {

        LayoutInflater addDelictInflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View mDelictsView = addDelictInflater.inflate(R.layout.dialog_delicts_list, null);

        // Заголовок
        TextView dtvObjectName = (TextView) mDelictsView.findViewById(R.id.dtvObjectName);
        dtvObjectName.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).full_name);

        // Завершить добавление нарушений
        Button dbtnClose = (Button) mDelictsView.findViewById(R.id.dbtnClose);
        dbtnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_panorama != null) {
                    byte[] ph = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_panorama;
                    updateObjectPhotoInDatabase(MainActivity.this, "photo_panorama", ph, userInfo, plans_tmp, currentPlanPosition, currentObjectPosition); // Сохранение фото панорамы в локальной БД
                }
                if (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_house_number != null) {
                    byte[] ph = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_house_number;
                    updateObjectPhotoInDatabase(MainActivity.this, "photo_house_number", ph, userInfo, plans_tmp, currentPlanPosition, currentObjectPosition); // Сохранение фото номера дома в локальной БД
                }
                mDialog.dismiss();
                startRotateScreen();
            }
        });

        // Кнопка добавление нарушения
        Button dbtnAdd = (Button) mDelictsView.findViewById(R.id.dbtnAdd);
        dbtnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.size() == 12) {
                    showToast(MainActivity.this, "Для одного объекта максимум 12 нарушений");
                    return;
                }
                currentDelictPosition = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.size();
                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.add(new Delict(
                        currentDelictPosition,
                        null,
                        null,
                        null,
                        null,
                        null));
                mDialog.setContentView(initAddNewDelict()); // Представление добавления нарушения
            }
        });

        // Фото панорамы
        ivPhotoPanorama = (ImageView) mDelictsView.findViewById(R.id.photoPanorama);
        if (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_panorama != null) { // Если в списке планов имеется фото панорамы, то отобразить превью
            byte[] photo_panorama_tmp = (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_panorama);
            Bitmap bitmap_tmp = BitmapFactory.decodeByteArray(photo_panorama_tmp, 0, photo_panorama_tmp.length);
            ivPhotoPanorama.setImageBitmap(bitmap_tmp);
        }
        ivPhotoPanorama.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!verifyPermissions(MainActivity.this)) {  // Если не даны разрешения на доступ к камере и галерее
                    return;
                }
                mDialog.setContentView(initPhotoObjectAction(PANORAMA_REQUEST, plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_panorama));
            }
        });

        // Фото номера дома
        ivPhotoHouseNumber = (ImageView) mDelictsView.findViewById(R.id.photoHouseNumber);
        if (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_house_number != null) {  // Если в списке планов имеется фото номера дома, то отобразить превью
            byte[] photo_house_number_tmp = (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_house_number);
            Bitmap bitmap_tmp = BitmapFactory.decodeByteArray(photo_house_number_tmp, 0, photo_house_number_tmp.length);
            ivPhotoHouseNumber.setImageBitmap(bitmap_tmp);
        }
        ivPhotoHouseNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!verifyPermissions(MainActivity.this)) { // Если не даны разрешения на доступ к камере и галерее
                    return;
                }
                mDialog.setContentView(initPhotoObjectAction(HOUSE_NUMBER_REQUEST, plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_house_number));
            }
        });

        // Список нарушений по объекту
        mDRecyclerView = (RecyclerView) mDelictsView.findViewById(R.id.mRecyclerViewDelicts);
        mDRecyclerView.setHasFixedSize(true);
        mDRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));//Linear Items
        // Адаптер списка нарушений
        edit_or_delete_delict_listener = new DelictsRecyclerViewAdapterNew.MyAdapterListener() {

            // Редактировать нарушение
            @Override
            public void itemViewOnClick(View v, int position) {

                currentDelictPosition = position;
                mDialog.setContentView(initEditDelict());
            }

            // Удалить нарушение
            @Override
            public void dbtnDeleteViewOnClick(View v, int position) {

                String current_sub_control_id = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).sub_control_id;
                deleteDelictFromDatabase(MainActivity.this, userInfo, current_sub_control_id, currentDelictPosition);

                // Удаление нарушения из контейнера планов
                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.remove(position);

                // перезапуск адаптера с обновлённым списком нарушений
                DRVAdapter = new DelictsRecyclerViewAdapterNew(MainActivity.this,
                        plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts, edit_or_delete_delict_listener);

                mDRecyclerView.setAdapter(DRVAdapter);
                DRVAdapter.setAdapterDelicts(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts);

                // Если удалены все нарушения, то показать окно с выполнением проверки (Без замечаний, Есть нарушения, Без проверки)
                if (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.size() == 0) {
                    mDialog.setContentView(initNewCheck()); // Представление с новой проверкой
                    plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts = null;
                }
            }
        };

        DRVAdapter = new DelictsRecyclerViewAdapterNew(MainActivity.this,
                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts, edit_or_delete_delict_listener);
        mDRecyclerView.setAdapter(DRVAdapter);

        return mDelictsView;
    }

    // Редактировать нарушение (нажатие на список нарушений)
    public View initEditDelict() {
        LayoutInflater editDelictInflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View editDelictView = editDelictInflater.inflate(R.layout.dialog_add_and_edit_delict, null);

        // Заполняем заголовок
        TextView dtvObjectName = (TextView) editDelictView.findViewById(R.id.dtvObjectName);
        dtvObjectName.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).full_name);

        // Заполняем описание под заголовком
        TextView dtvDescription = (TextView) editDelictView.findViewById(R.id.dtvDescription);
        dtvDescription.setText("Редактирование нарушения");

        // Инициализация и заполнение описания и времени нарушения
        detDescription = (EditText) editDelictView.findViewById(R.id.detDescription);
        detDescription.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).defection_name);

        // Инициализация и заполнение времени нарушения
        detTime = (EditText) editDelictView.findViewById(R.id.detTime);
        detTime.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).time);

        // Фото нарушения
        ivPhotoDelict = (ImageView) editDelictView.findViewById(R.id.ivPhotoDelict);
        if (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo != null) {

            byte[] photo_delict_tmp = (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo);
            Bitmap bitmap_tmp = BitmapFactory.decodeByteArray(photo_delict_tmp, 0, photo_delict_tmp.length);
            ivPhotoDelict.setImageBitmap(bitmap_tmp);
        }

        ivPhotoDelict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Если не даны разрешения
                if (!verifyPermissions(MainActivity.this)) {
                    //showToast(MainActivity.this, "Необходимо предоставить доступ к камере");
                    return;
                }

                // Получаем id, описание нарушения и время
                String selected_defection_id = defections_tmp.get(currentDefectionPosition).id;
                String selected_defection_label = defections_tmp.get(currentDefectionPosition).label; // Краткое вида описание нарушения
                String selected_defection_name = detDescription.getText().toString().trim();
                String selected_time = detTime.getText().toString().trim();
                byte[] photo_ = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo;

                // Запоминаем текущие данные о нарушении
                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.set(currentDelictPosition, new Delict(
                        currentDelictPosition,
                        selected_defection_id,
                        selected_defection_label,
                        selected_defection_name,
                        selected_time,
                        photo_));

                // Открываем представление для добавления или просмотра фото
                mDialog.setContentView(initEditDelictPhotoAction(photo_));
            }
        });

        // Адаптер видов нарушений
        spinnerDefection = (Spinner) editDelictView.findViewById(R.id.spinnerDefection);
        AdapterDefectionSpinner mDefectionAdapter = new AdapterDefectionSpinner(MainActivity.this, R.layout.spinner_defection, defections_tmp);
        spinnerDefection.setAdapter(mDefectionAdapter);

        // Не заполнять описание по виду нарушения при отображении формы редактирования
        doNotSkipInitTextDescriptionSetter = false;

        spinnerDefection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                currentDefectionPosition = position;

                if (doNotSkipInitTextDescriptionSetter) {
                    detDescription.setText(defections_tmp.get(position).name);
                }

                doNotSkipInitTextDescriptionSetter = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        String tmp_defection_id = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).defection_id;

        // Поиск позиции выбранного нарушения
        int index_defection = 0;
        for (int i = 0; i < defections_tmp.size(); i++) {
            if (defections_tmp.get(i).id.equals(tmp_defection_id)) {
                index_defection = i;
                break;
            }
        }
        spinnerDefection.setSelection(index_defection);

        Button dbtnAccept = (Button) editDelictView.findViewById(R.id.dbtnAccept);

        dbtnAccept.setOnClickListener(initAcceptEditDelictButtonListener());

        return editDelictView;
    }

    public View initEditFirstDelict() {
        LayoutInflater editDelictInflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View editDelictView = editDelictInflater.inflate(R.layout.dialog_add_and_edit_delict, null);

        // Заполняем заголовок
        TextView dtvObjectName = (TextView) editDelictView.findViewById(R.id.dtvObjectName);
        dtvObjectName.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).full_name);

        // Заполняем описание
        TextView dtvDescription = (TextView) editDelictView.findViewById(R.id.dtvDescription);
        dtvDescription.setText("Добавление нарушения");

        // Инициализация и заполнение описания и времени нарушения
        detDescription = (EditText) editDelictView.findViewById(R.id.detDescription);
        detDescription.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).defection_name);

        // Инициализация и заполнение времени нарушения
        detTime = (EditText) editDelictView.findViewById(R.id.detTime);
        detTime.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).time);

        // Фото нарушения
        ivPhotoDelict = (ImageView) editDelictView.findViewById(R.id.ivPhotoDelict);
        if (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo != null) {

            byte[] photo_delict_tmp = (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo);
            Bitmap bitmap_tmp = BitmapFactory.decodeByteArray(photo_delict_tmp, 0, photo_delict_tmp.length);
            ivPhotoDelict.setImageBitmap(bitmap_tmp);
        }

        ivPhotoDelict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Если не даны разрешения
                if (!verifyPermissions(MainActivity.this)) {
                    //showToast(MainActivity.this, "Необходимо предоставить доступ к камере");
                    return;
                }

                // Получаем id, описание нарушения и время
                String selected_defection_id = defections_tmp.get(currentDefectionPosition).id;
                String selected_defection_label = defections_tmp.get(currentDefectionPosition).label; // Краткое вида описание нарушения
                String selected_defection_name = detDescription.getText().toString().trim();
                String selected_time = detTime.getText().toString().trim();
                byte[] photo_ = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo;

                // Запоминаем текущие данные о нарушении
                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.set(currentDelictPosition, new Delict(
                        0,
                        selected_defection_id,
                        selected_defection_label,
                        selected_defection_name,
                        selected_time,
                        photo_));

                // Открываем представление для добавления или просмотра фото
                mDialog.setContentView(initFirstDelictPhotoAction(photo_));
            }
        });

        // Адаптер видов нарушений
        spinnerDefection = (Spinner) editDelictView.findViewById(R.id.spinnerDefection);
        AdapterDefectionSpinner mDefectionAdapter = new AdapterDefectionSpinner(MainActivity.this, R.layout.spinner_defection, defections_tmp);
        spinnerDefection.setAdapter(mDefectionAdapter);

        // Не заполнять описание по виду нарушения при отображении формы редактирования
        doNotSkipInitTextDescriptionSetter = false;

        spinnerDefection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                currentDefectionPosition = position;

                if (doNotSkipInitTextDescriptionSetter) {
                    detDescription.setText(defections_tmp.get(position).name);
                }

                doNotSkipInitTextDescriptionSetter = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        String tmp_defection_id = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).defection_id;

        // Поиск позиции выбранного нарушения
        int index_defection = 0;
        for (int i = 0; i < defections_tmp.size(); i++) {
            if (defections_tmp.get(i).id.equals(tmp_defection_id)) {
                index_defection = i;
                break;
            }
        }
        spinnerDefection.setSelection(index_defection);
        Button dbtnAccept = (Button) editDelictView.findViewById(R.id.dbtnAccept);
        dbtnAccept.setOnClickListener(initAcceptFirstDelictButtonListener());

        return editDelictView;
    }

    // Кнопка Добавить новое нарушение (к списку нарушений)
    public View initEditAddedNewDelict() {
        LayoutInflater addDelictInflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View addDelictView = addDelictInflater.inflate(R.layout.dialog_add_and_edit_delict, null);

        // Заполняем заголовок
        TextView dtvObjectName = (TextView) addDelictView.findViewById(R.id.dtvObjectName);
        dtvObjectName.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).full_name);

        // Заполняем описание
        TextView dtvDescription = (TextView) addDelictView.findViewById(R.id.dtvDescription);
        dtvDescription.setText("Добавление нарушения");

        // Инициализация и заполнение описания и времени нарушения
        detDescription = (EditText) addDelictView.findViewById(R.id.detDescription);
        detDescription.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).defection_name);

        // Инициализация и заполнение времени нарушения
        detTime = (EditText) addDelictView.findViewById(R.id.detTime);
        detTime.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).time);

        // Фото нарушения
        ivPhotoDelict = (ImageView) addDelictView.findViewById(R.id.ivPhotoDelict);
        if (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo != null) {

            byte[] photo_delict_tmp = (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo);
            Bitmap bitmap_tmp = BitmapFactory.decodeByteArray(photo_delict_tmp, 0, photo_delict_tmp.length);
            ivPhotoDelict.setImageBitmap(bitmap_tmp);
        }

        ivPhotoDelict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Если не даны разрешения
                if (!verifyPermissions(MainActivity.this)) {
                    //showToast(MainActivity.this, "Необходимо предоставить доступ к камере");
                    return;
                }

                // Получаем id, описание нарушения и время
                String selected_defection_id = defections_tmp.get(currentDefectionPosition).id;
                String selected_defection_label = defections_tmp.get(currentDefectionPosition).label; // Краткое вида описание нарушения
                String selected_defection_name = detDescription.getText().toString().trim();
                String selected_time = detTime.getText().toString().trim();
                byte[] photo_ = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo;

                // Запоминаем текущие данные о нарушении
                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.set(currentDelictPosition, new Delict(
                        currentDelictPosition,
                        selected_defection_id,
                        selected_defection_label,
                        selected_defection_name,
                        selected_time,
                        photo_));

                // Открываем представление для добавления или просмотра фото
                mDialog.setContentView(initAddDelictPhotoAction(photo_));
            }
        });

        doNotSkipInitTextDescriptionSetter = false;

        // Адаптер видов нарушений
        spinnerDefection = (Spinner) addDelictView.findViewById(R.id.spinnerDefection);
        AdapterDefectionSpinner mDefectionAdapter = new AdapterDefectionSpinner(MainActivity.this, R.layout.spinner_defection, defections_tmp);
        spinnerDefection.setAdapter(mDefectionAdapter);
        spinnerDefection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                currentDefectionPosition = position;

                if (doNotSkipInitTextDescriptionSetter) {
                    detDescription.setText(defections_tmp.get(position).name);
                }

                doNotSkipInitTextDescriptionSetter = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Поиск позиции выбранного нарушения
        String tmp_defection_id = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).defection_id;
        int index_defection = 0;
        for (int i = 0; i < defections_tmp.size(); i++) {
            if (defections_tmp.get(i).id.equals(tmp_defection_id)) {
                index_defection = i;
                break;
            }
        }
        spinnerDefection.setSelection(index_defection);

        Button dbtnAccept = (Button) addDelictView.findViewById(R.id.dbtnAccept);
        dbtnAccept.setOnClickListener(initAddDelictAcceptButtonListener());

        return addDelictView;
    }

    // Кнопка Добавить новое нарушение (к списку нарушений)
    public View initAddNewDelict() {
        LayoutInflater addDelictInflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View addDelictView = addDelictInflater.inflate(R.layout.dialog_add_and_edit_delict, null);

        // Заполняем заголовок
        TextView dtvObjectName = (TextView) addDelictView.findViewById(R.id.dtvObjectName);
        dtvObjectName.setText(plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).full_name);

        // Заполняем описание
        TextView dtvDescription = (TextView) addDelictView.findViewById(R.id.dtvDescription);
        dtvDescription.setText("Добавление нарушения");

        // Инициализация описания и времени нарушения
        detDescription = (EditText) addDelictView.findViewById(R.id.detDescription);

        // Инициализация и заполнение времени нарушения
        detTime = (EditText) addDelictView.findViewById(R.id.detTime);
        DateFormat df = new SimpleDateFormat("HH:mm");
        String currentTime = df.format(Calendar.getInstance().getTime());
        detTime.setText(currentTime);

        // Адаптер видов нарушений
        spinnerDefection = (Spinner) addDelictView.findViewById(R.id.spinnerDefection);
        AdapterDefectionSpinner mDefectionAdapter = new AdapterDefectionSpinner(MainActivity.this, R.layout.spinner_defection, defections_tmp);
        spinnerDefection.setAdapter(mDefectionAdapter);
        spinnerDefection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                currentDefectionPosition = position;
                detDescription.setText(defections_tmp.get(position).name);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Фото нарушения
        ivPhotoDelict = (ImageView) addDelictView.findViewById(R.id.ivPhotoDelict);
        if (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo != null) {

            byte[] photo_delict_tmp = (plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo);
            Bitmap bitmap_tmp = BitmapFactory.decodeByteArray(photo_delict_tmp, 0, photo_delict_tmp.length);
            ivPhotoDelict.setImageBitmap(bitmap_tmp);
        }

        ivPhotoDelict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Если не даны разрешения
                if (!verifyPermissions(MainActivity.this)) {
                    //showToast(MainActivity.this, "Необходимо предоставить доступ к камере");
                    return;
                }

                // Получаем id, описание нарушения и время
                String selected_defection_id = defections_tmp.get(currentDefectionPosition).id;
                String selected_defection_label = defections_tmp.get(currentDefectionPosition).label; // Краткое вида описание нарушения
                String selected_defection_name = detDescription.getText().toString().trim();
                String selected_time = detTime.getText().toString().trim();
                byte[] photo_ = plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo;

                // Запоминаем текущие данные о нарушении
                plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.set(currentDelictPosition, new Delict(
                        currentDelictPosition,
                        selected_defection_id,
                        selected_defection_label,
                        selected_defection_name,
                        selected_time,
                        photo_));

                // Открываем представление для добавления или просмотра фото
                mDialog.setContentView(initAddDelictPhotoAction(photo_));
            }
        });

        Button dbtnAccept = (Button) addDelictView.findViewById(R.id.dbtnAccept);
        dbtnAccept.setOnClickListener(initAddDelictAcceptButtonListener());

        return addDelictView;
    }

    // Получение фото с камеры
    public GetPhotoResult getPhotoFromCamera() {

        Log.d(TAG, "getPhotoFromCamera: " + Environment.getExternalStorageDirectory().toString());

        File f = new File(Environment.getExternalStorageDirectory().toString());
        for (File temp : f.listFiles()) {
            if (temp.getName().equals("ais_ksgt_temp.jpg")) {
                f = temp;

                break;
            }
        }
        try {
            Bitmap bitmap;
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(), bitmapOptions);

            int BITMAP_MAXWIDTH = 0;
            try {
                BITMAP_MAXWIDTH = getResources().getInteger((R.integer.BITMAP_MAXWIDTH));
            } catch (Exception e) {
                BITMAP_MAXWIDTH = 40;
            }

            if (bitmap.getWidth() > BITMAP_MAXWIDTH) {
                bitmap = getResizedBitmap(bitmap, BITMAP_MAXWIDTH);
            }

            System.out.println("width fin: " + bitmap.getWidth() + " | height fin: " + bitmap.getHeight());

            // Дата и время нарушения на фото с камеры
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateTime = sdf.format(Calendar.getInstance().getTime()); // reading local time in the system
            Bitmap dest = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas cs = new Canvas(dest);
            Paint tPaint = new Paint();
            tPaint.setTextSize(35);
            tPaint.setColor(Color.RED);
            tPaint.setStyle(Paint.Style.FILL);
            cs.drawBitmap(bitmap, 0f, 0f, null);
            float height = tPaint.measureText("yY");
            String photo_description = "АИС КСГТ";
            cs.drawText(photo_description + " " + dateTime, 20f, height+15f, tPaint);


            GetPhotoResult Result = new GetPhotoResult();
            Result.bitmap = dest;
            //Result.strPhoto = bitmapCompressAndToString(bitmap);

            int BITMAP_QUALITY = 0;
            try {
                BITMAP_QUALITY = getResources().getInteger((R.integer.BITMAP_QUALITY));
            } catch (Exception e) {
                BITMAP_QUALITY = 40;
            }

            Result.bytePhoto = bitmapCompressAndToByte(dest, BITMAP_QUALITY);

            if (f.delete()) {
                System.out.println(Environment.getExternalStorageDirectory().toString() + "ais_ksgt_temp.jpg файл удален");
            } else
                System.out.println("Файла " + Environment.getExternalStorageDirectory().toString() + "ais_ksgt_temp.jpg не обнаружено");

            return Result;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Получение фото с галереи
    public GetPhotoResult getPhotoFromGallery(Intent data) {

        GetPhotoResult Result = new GetPhotoResult();
        String picturePath = data.getStringExtra("data");
        Bitmap thumbnail = BitmapFactory.decodeFile(picturePath);

        int BITMAP_MAXWIDTH = 0;
        try {
            BITMAP_MAXWIDTH = getResources().getInteger((R.integer.BITMAP_MAXWIDTH));
        } catch (Exception e) {
            BITMAP_MAXWIDTH = 40;
        }

        if (thumbnail.getWidth() > BITMAP_MAXWIDTH) {
            thumbnail = getResizedBitmap(thumbnail, BITMAP_MAXWIDTH);
        }

        Result.bitmap = thumbnail;

        int BITMAP_QUALITY = 0;
        try {
            BITMAP_QUALITY = getResources().getInteger((R.integer.BITMAP_QUALITY));
        } catch (Exception e) {
            BITMAP_QUALITY = 40;
        }

        Result.bytePhoto = bitmapCompressAndToByte(thumbnail, BITMAP_QUALITY);

        return Result;
    }

    /**
     * Получение результата с Activity камеры и галереи (используется для загрузки фото)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            if (requestCode == PANORAMA_FROM_CAMERA_REQUEST) { // Панорама

                GetPhotoResult tmp = getPhotoFromCamera();
                if (tmp != null) {
                    // Отображение фото
                    ivPhotoPanorama.setImageBitmap(tmp.bitmap); // Установить фото для отображения
                    ivPhotoPreview.setImageBitmap(tmp.bitmap); // Изменить текущее превью фото
                    ivPhotoPreview.setVisibility(View.VISIBLE); // Отобразить фото

                    // Сохранение результата
                    plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_panorama = tmp.bytePhoto; // Записать фото в объект
                } else {
                    showToast(MainActivity.this, "Не удалось загрузить фото");
                }

            } else if (requestCode == HOUSE_NUMBER_FROM_CAMERA_REQUEST) { // Номер дома

                GetPhotoResult tmp = getPhotoFromCamera();
                if (tmp != null) {
                    // Отображение фото
                    ivPhotoHouseNumber.setImageBitmap(tmp.bitmap); // Установить фото для отображения
                    ivPhotoPreview.setImageBitmap(tmp.bitmap); // Изменить текущее превью фото
                    ivPhotoPreview.setVisibility(View.VISIBLE); // Отобразить фото

                    // Сохранение результата
                    plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_house_number = tmp.bytePhoto; // Записать фото в объект
                } else {
                    showToast(MainActivity.this, "Не удалось загрузить фото");
                }

            } else if (requestCode == DELICT_FROM_CAMERA_REQUEST) { // Нарушение

                GetPhotoResult tmp = getPhotoFromCamera();
                if (tmp != null) {
                    // Отображение фото
                    ivPhotoDelict.setImageBitmap(tmp.bitmap); // Установить фото для отображения
                    ivPhotoPreview.setImageBitmap(tmp.bitmap); // Изменить текущее превью фото
                    ivPhotoPreview.setVisibility(View.VISIBLE); // Отобразить фото

                    // Сохранение результата
                    plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo = tmp.bytePhoto; // Записать фото в объект
                } else {
                    showToast(MainActivity.this, "Не удалось загрузить фото");
                }

            } else if (requestCode == PANORAMA_FROM_GALLERY_REQUEST) {

                GetPhotoResult tmp = getPhotoFromGallery(data);
                if (tmp != null) {
                    // Отображение фото
                    ivPhotoPanorama.setImageBitmap(tmp.bitmap); // Установить фото для отображения
                    ivPhotoPreview.setImageBitmap(tmp.bitmap); // Изменить текущее превью фото
                    ivPhotoPreview.setVisibility(View.VISIBLE); // Отобразить фото

                    // Сохранение результата
                    plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_panorama = tmp.bytePhoto; // Записать фото в объект

                } else {
                    showToast(MainActivity.this, "Не удалось загрузить фото");
                }

            } else if (requestCode == HOUSE_NUMBER_FROM_GALLERY_REQUEST) {

                GetPhotoResult tmp = getPhotoFromGallery(data);
                if (tmp != null) {
                    // Отображение фото
                    ivPhotoHouseNumber.setImageBitmap(tmp.bitmap);// Установить фото для отображения
                    ivPhotoPreview.setImageBitmap(tmp.bitmap); // Изменить текущее превью фото
                    ivPhotoPreview.setVisibility(View.VISIBLE); // Отобразить фото

                    // Сохранение результата
                    plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).photo_house_number = tmp.bytePhoto; // Записать фото в объект
                } else {
                    showToast(MainActivity.this, "Не удалось загрузить фото");
                }

            } else if (requestCode == DELICT_FROM_GALLERY_REQUEST) {

                GetPhotoResult tmp = getPhotoFromGallery(data);
                if (tmp != null) {
                    // Отображение фото
                    ivPhotoDelict.setImageBitmap(tmp.bitmap); // Установить фото для отображения
                    ivPhotoPreview.setImageBitmap(tmp.bitmap); // Изменить текущее превью фото
                    ivPhotoPreview.setVisibility(View.VISIBLE); // Отобразить фото

                    // Сохранение результата
                    plans_tmp.get(currentPlanPosition).pobjects.get(currentObjectPosition).delicts.get(currentDelictPosition).photo = tmp.bytePhoto; // Записать фото в объект
                } else {
                    showToast(MainActivity.this, "Не удалось загрузить фото");
                }

            }
        }
    }

    /**
     * Вызов меню
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Загрузчик данных с сервера/на сервер
     */
    @Override
    public Loader<WebServiceResponse> onCreateLoader(int id, Bundle args) {

        stopRotateScreen(); // Отключить поворот экрана

        Log.d(TAG, "onCreateLoader");
        String Message = "";
        progressDialog = new ProgressDialog(MainActivity.this);
        if (id == LDR_BASIC_ID_GET_DEFECTIONS) {
            Message = "Пожалуйста подождите... Идёт загрузка видов нарушений";
        } else if (id == LDR_BASIC_ID_GET_PLANS) {
            Message = "Пожалуйста подождите... Идёт загрузка планов";
        } else if (id == LDR_BASIC_ID_SEND_OBJECTS) {
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setTitle("Отправка обхода");
            Message = "Пожалуйста подождите... Идёт отправка результата обхода";
            progressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Закрыть", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    progressDialog.dismiss();
                    startRotateScreen();
                }
            });
        }
        progressDialog.setMessage(Message);
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.show();
            progressDialog.getButton(ProgressDialog.BUTTON_NEGATIVE).setEnabled(false);
        return new BasicLoader(this, args, progressDialog);
    }

    @Override
    public void onLoadFinished(Loader<WebServiceResponse> loader, WebServiceResponse webServiceResponse) { // Обработка результатов загрузки/выгрузки

        Log.d(TAG, "onLoadFinished");

        if (webServiceResponse.success) { // Получен ответ от сервера
            switch (loader.getId()) { // Обработатка ответа загрузчика с уникальным идентификатором
                case LDR_BASIC_ID_GET_DEFECTIONS: { // Были загружены виды нарушений

                    Gson gson = new GsonBuilder().create(); // Разбор JSON ответа сформированного на сервере
                    DatabaseGetDefectionsResponse dbDefections = gson.fromJson(webServiceResponse.json, DatabaseGetDefectionsResponse.class);
                    if (dbDefections.success) { // Успешная загрузка видов нарушений
                        saveDefectionInLocalDatabase(MainActivity.this, dbDefections);
                        showToast(MainActivity.this, "Виды нарушений успешно загружены");
                        progressDialog.setMessage("Виды нарушений успешно загружены");
                        progressDialog.getButton(Dialog.BUTTON_NEGATIVE).setEnabled(true);
                    } else {
                        showToast(this, dbDefections.errorMessage);
                    }
                }
                break;

                case LDR_BASIC_ID_GET_PLANS: { // Были загружены планы обхода

                    Gson gson = new GsonBuilder().create(); // Разбор JSON ответа сформированного на сервере
                    DatabaseGetPlansResponse dbPlans = gson.fromJson(webServiceResponse.json, DatabaseGetPlansResponse.class);
                    if (dbPlans.success) { // Успешная загрузка планов
                        savePlansInLocalDatabase(MainActivity.this, userInfo, dbPlans);
                        showToast(MainActivity.this, "Планы успешно загружены");

                    } else {
                        showToast(this, dbPlans.errorMessage);
                    }
                }
                break;

                case LDR_BASIC_ID_SEND_OBJECTS: {

                    Gson gson = new GsonBuilder().create(); // Разбор JSON ответа сформированного на сервере
                    DatabaseSendObjectsResponse dbSendObjectsResult = gson.fromJson(webServiceResponse.json, DatabaseSendObjectsResponse.class);
                    if (dbSendObjectsResult.success) { // Успешная выгрузка обхода
                        progressDialog.setMessage(dbSendObjectsResult.resultMessage);
                        progressDialog.getButton(Dialog.BUTTON_NEGATIVE).setEnabled(true);
                    } else {
                        progressDialog.setMessage(dbSendObjectsResult.errorMessage);
                        progressDialog.getButton(Dialog.BUTTON_NEGATIVE).setEnabled(true);
                    }
                }
                break;
            }
        } else { // Не удалось получить ответ от сервера
            if(loader.getId() == LDR_BASIC_ID_SEND_OBJECTS) {
                progressDialog.setMessage(webServiceResponse.errorMessage);
                progressDialog.getButton(Dialog.BUTTON_NEGATIVE).setEnabled(true);
            } else {
                showToast(MainActivity.this, webServiceResponse.errorMessage);
            }
        }
        if (loader.getId() == LDR_BASIC_ID_GET_DEFECTIONS || loader.getId() == LDR_BASIC_ID_GET_PLANS) { // Закрыть progressDialog
            startRotateScreen(); // Разрешить поворот экрана
            if (progressDialog != null) {
                progressDialog.hide();
                progressDialog.dismiss();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<WebServiceResponse> loader) {
        Log.d(TAG, "onLoaderReset");
    }

    final static class BasicLoader extends AsyncTaskLoader<WebServiceResponse> {

        private WebServiceResponse webServiceResponse = new WebServiceResponse();
        private int loader_id; // Идентификатор загрузчка
        private Bundle infoBundle; // Параметры для обращения к веб-серверу
        private ProgressDialog dialog; // Диалог, для обновления индикатора

        public BasicLoader(Context context, Bundle args, ProgressDialog dialog) {
            super(context);
            this.infoBundle = args;
            this.dialog = dialog;
        }

        @Override
        public WebServiceResponse loadInBackground() {

            Log.d(TAG, "loadInBackground");

            SoapObject soapObject = new SoapObject(infoBundle.getString("namespace"), infoBundle.getString("webServiceFunction"));
            String SOAP_ACTION = infoBundle.getString("namespace") + infoBundle.getString("webServiceFunction") + "\"";
            loader_id = infoBundle.getInt("loader_id");
            switch (loader_id) {

                case LDR_BASIC_ID_GET_DEFECTIONS: {
                    PropertyInfo propertyInfo1 = new PropertyInfo();
                    propertyInfo1.setName("device_id");
                    propertyInfo1.setValue(infoBundle.getString("device_id"));
                    propertyInfo1.setType(String.class);
                    soapObject.addProperty(propertyInfo1);
                }
                break;

                case LDR_BASIC_ID_GET_PLANS: {
                    PropertyInfo propertyInfo1 = new PropertyInfo();
                    propertyInfo1.setName("official_id");
                    propertyInfo1.setValue(infoBundle.getString("official_id"));
                    propertyInfo1.setType(String.class);
                    soapObject.addProperty(propertyInfo1);

                    PropertyInfo propertyInfo2 = new PropertyInfo();
                    propertyInfo2.setName("represent_id");
                    propertyInfo2.setValue(infoBundle.getString("represent_id"));
                    propertyInfo2.setType(String.class);
                    soapObject.addProperty(propertyInfo2);

                    PropertyInfo propertyInfo3 = new PropertyInfo();
                    propertyInfo3.setName("device_id");
                    propertyInfo3.setValue(infoBundle.getString("device_id"));
                    propertyInfo3.setType(String.class);
                    soapObject.addProperty(propertyInfo3);
                }
                break;

                case LDR_BASIC_ID_SEND_OBJECTS: {
                    PropertyInfo propertyInfo1 = new PropertyInfo();
                    propertyInfo1.setName("objects");
                    propertyInfo1.setValue(infoBundle.getString("objects"));
                    propertyInfo1.setType(String.class);
                    soapObject.addProperty(propertyInfo1);

                    PropertyInfo propertyInfo2 = new PropertyInfo();
                    propertyInfo2.setName("user_id");
                    propertyInfo2.setValue(infoBundle.getString("user_id"));
                    propertyInfo2.setType(String.class);
                    soapObject.addProperty(propertyInfo2);

                    PropertyInfo propertyInfo3 = new PropertyInfo();
                    propertyInfo3.setName("device_id");
                    propertyInfo3.setValue(infoBundle.getString("device_id"));
                    propertyInfo3.setType(String.class);
                    soapObject.addProperty(propertyInfo3);
                }
            }

            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(soapObject);

            try {

                if ((loader_id == LDR_BASIC_ID_GET_DEFECTIONS) || (loader_id == LDR_BASIC_ID_GET_PLANS)) {
                    MyHttpTransportSE httpTransportSE = new MyHttpTransportSE(infoBundle.getString("url"), infoBundle.getInt("timeout"));
                    httpTransportSE.debug = true;
                    httpTransportSE.call(dialog, true, SOAP_ACTION, envelope);

                } else if (loader_id == LDR_BASIC_ID_SEND_OBJECTS) {
                    MyHttpTransportSE httpTransportSE = new MyHttpTransportSE(infoBundle.getString("url"), infoBundle.getInt("timeout"));
                    httpTransportSE.call(dialog, false, SOAP_ACTION, envelope);
                }

                SoapObject resultObject = (SoapObject) envelope.bodyIn;
                if (resultObject != null) {
                    webServiceResponse.success = true;
                    webServiceResponse.json = resultObject.getProperty(0).toString();
                }

            } catch (SoapFault soapFault) {
                webServiceResponse.success = false;
                webServiceResponse.errorMessage = "Soap Fault Exception " + soapFault;

            } catch (IOException ioException) {
                webServiceResponse.success = false;
                webServiceResponse.errorMessage = "Не удалось получить ответ от сервера!";

            } catch (XmlPullParserException parserException) {
                webServiceResponse.success = false;
                webServiceResponse.errorMessage = "XmlPullParserException " + parserException;

            } catch (Exception exception) {
                webServiceResponse.success = false;
                webServiceResponse.errorMessage = "Exception " + exception;
            }

            return webServiceResponse;
        }

        @Override
        public void deliverResult(WebServiceResponse data) {
            Log.d(TAG, "deliverResult");
            if (isStarted()) { // Показать результат, если загрузчик уже запущен
                super.deliverResult(data);
            }
        }

        @Override
        protected void onStartLoading() {
            Log.d(TAG, "onStartLoading");
            super.onStartLoading();

            if (webServiceResponse.json != null) { // Не выполнять запрос по загрузке данных, если они загружены. Это позволяет избежать повторные запросы при разворачивании приложения, нажатии кнопку на back и т.д.
                //Log.d("AsyncTaskLoaderGoogle", "mData.valueRange != null");
                deliverResult(webServiceResponse);
            } else { // Выполнить «принудительную» загрузку данных
                //Log.d("AsyncTaskLoaderGoogle", "mData.valueRange == null");
                forceLoad();
            }
        }

        @Override
        protected void onStopLoading() {
            Log.d(TAG, "onStopLoading");
            cancelLoad();
        }

        @Override
        protected void onReset() {
            Log.d(TAG, "onReset");
            super.onReset();
            // Убедиться, что процесс загрузки остановлен
            onStopLoading();
        }
    }
}



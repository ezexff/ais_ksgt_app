package com.ais_ksgt_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;

import com.ais_ksgt_app.R;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.service.SessionAndDateCheckService;
import com.spinner.AdapterRepresentSpinner;
import com.web_service.DatabaseLoginResponse;

import static com.ais_ksgt_app.LoginActivity.BROADCAST_ACTION;
import static com.ais_ksgt_app.LoginActivity.PARAM_RESULT;
import static com.ais_ksgt_app.LoginActivity.PARAM_STATUS;
import static com.ais_ksgt_app.LoginActivity.PARAM_TASK;
import static com.ais_ksgt_app.LoginActivity.STATUS_FINISH;
import static com.ais_ksgt_app.LoginActivity.TASK1_CODE;
import static com.ais_ksgt_app.LoginActivity.TASK2_CODE;

public class RepresentActivity extends AppCompatActivity {

    // Тег класса (для логирования)
    private final static String TAG = "T_RepresentActivity";

    // Кнопка подтверждения выбора подразделения
    private Button btnChooseRepresent;

    // Адаптер списка подразделений
    private AdapterRepresentSpinner mRepresentAdapter;

    // Список подразделений
    private Spinner spinnerRepresent;

    // Приёмник сообщений от сервиса
    BroadcastReceiver br;
    IntentFilter intFilt;

    public void onBackPressed() {
        Log.d(TAG, "onBackPressed and Receiver unregistered!!!");
        unregisterReceiver(br);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "ON START!");

        try {
            RepresentActivity.this.unregisterReceiver(br);
            Log.d(TAG, "onBackPressed unregisterReceiver!");
        } catch (Exception e){
            Log.d(TAG, "unregisterReceiver failed!");
        }

        br = new BroadcastReceiver() {
            // действия при получении сообщений
            public void onReceive(Context context, Intent intent) {
                int task = intent.getIntExtra(PARAM_TASK, 0);
                int status = intent.getIntExtra(PARAM_STATUS, 0);

                // Ловим сообщения об окончании задач
                if (status == STATUS_FINISH) {
                    int result = intent.getIntExtra(PARAM_RESULT, 0);

                    stopService(new Intent(getApplicationContext(), SessionAndDateCheckService.class));

                    switch (task){
                        case TASK1_CODE:{

                            Log.d(TAG, "Message from Service received! " + result);

                            final AlertDialog dialog = new AlertDialog.Builder(RepresentActivity.this)
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
                                            Intent intent  = new Intent(RepresentActivity.this, LoginActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            startActivity(intent);
                                        }
                                    });
                                }
                            });
                            dialog.show();

                        } break;

                        case TASK2_CODE: {

                            Log.d(TAG, "Message from Service received! " + result);

                            final AlertDialog dialog = new AlertDialog.Builder(RepresentActivity.this)
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
                                            Intent intent  = new Intent(RepresentActivity.this, LoginActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            startActivity(intent);
                                        }
                                    });
                                }
                            });
                            dialog.show();

                        } break;
                    }
                }
            }
        };

        // создаем фильтр для BroadcastReceiver
        intFilt = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(br, intFilt);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_represent);

        spinnerRepresent = (Spinner) findViewById(R.id.spinner1);
        btnChooseRepresent = (Button)findViewById(R.id.btnChooseRepresent);

        // Получение данных о пользователе и подразделениях с Login Activity
        Intent intent = getIntent();
        String jsonUserData = intent.getStringExtra("jsonUserInfo");

        // Создание класса из JSON строки
        Gson gson = new GsonBuilder().create();
        final DatabaseLoginResponse userInfo = gson.fromJson(jsonUserData, DatabaseLoginResponse.class);

        // Инициализация адаптера для подразделений
        mRepresentAdapter = new AdapterRepresentSpinner(this, R.layout.spinner_represent, userInfo.represents);
        spinnerRepresent.setAdapter(mRepresentAdapter);

        btnChooseRepresent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(TAG, "Receiver unregistered!!!");
                unregisterReceiver(br);

                btnChooseRepresent.setEnabled(false);

                // Запоминаем позицию выбранного подразделения
                userInfo.selected_represent_position = spinnerRepresent.getSelectedItemPosition();

                // Передаём jsonUserInfo с результатом выбора подразделения
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String jsonUserInfo = gson.toJson(userInfo);
                Intent intent = new Intent(RepresentActivity.this, MainActivity.class);
                intent.putExtra("jsonUserInfo", jsonUserInfo);
                RepresentActivity.this.startActivity(intent);

                btnChooseRepresent.setEnabled(true);
            }
        });
    }
}

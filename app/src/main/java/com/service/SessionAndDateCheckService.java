package com.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.ais_ksgt_app.LoginActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.ais_ksgt_app.LoginActivity.PARAM_DATE;
import static com.ais_ksgt_app.LoginActivity.TASK1_CODE;
import static com.ais_ksgt_app.LoginActivity.TASK2_CODE;


public class SessionAndDateCheckService extends Service {

    private final static String TAG = "T_SADCheckService";
    ExecutorService es;

    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        es = Executors.newFixedThreadPool(2);
    }

    public void onDestroy() {
        super.onDestroy();
        es.shutdownNow();
        Log.d(TAG, "onDestroy");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        long time = intent.getLongExtra(LoginActivity.PARAM_TIME, 1);
        int task = intent.getIntExtra(LoginActivity.PARAM_TASK, 0);
        String date = intent.getStringExtra(PARAM_DATE);

        SRun sr = new SRun(startId, time, task, date);
        es.execute(sr);

        return super.onStartCommand(intent, flags, startId);
    }

    public IBinder onBind(Intent arg0) {
        return null;
    }

    class SRun implements Runnable { // Поток, выполнящий задачу

        long time;
        int startId;
        int task;
        String date;

        public SRun(int startId, long time, int task, String date) { // Конструктор задачи
            this.time = time;
            this.startId = startId;
            this.task = task;
            this.date = date;
            Log.d(TAG, "SRun: " + startId + " create");
        }

        public void run() { // Реализация потока
            Intent intent = new Intent(LoginActivity.BROADCAST_ACTION);
            Log.d(TAG, "SRun: " + startId + " start, time = " + time);
            int time_tmp = (int)time;

            switch (startId){
                case TASK1_CODE: {
                    try {
                        // Сообщаем о старте задачи
                        intent.putExtra(LoginActivity.PARAM_TASK, task);
                        intent.putExtra(LoginActivity.PARAM_STATUS, LoginActivity.STATUS_START);
                        sendBroadcast(intent);

                        // Начинаем выполнение задачи
                        TimeUnit.MILLISECONDS.sleep(time_tmp);

                        // Сообщаем об окончании задачи
                        intent.putExtra(LoginActivity.PARAM_STATUS, LoginActivity.STATUS_FINISH);
                        intent.putExtra(LoginActivity.PARAM_RESULT, time_tmp * 100);
                        sendBroadcast(intent);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } break;

                case TASK2_CODE: {
                    try {
                        // Сообщаем о старте задачи
                        intent.putExtra(LoginActivity.PARAM_TASK, task);
                        intent.putExtra(LoginActivity.PARAM_STATUS, LoginActivity.STATUS_START);
                        sendBroadcast(intent);

                        // Начинаем выполнение задачи
                        while (true) {
                            DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                            String currentUserDate = df.format(Calendar.getInstance().getTime());

                            // Log.d(TAG, "currentUserDate=" + currentUserDate);
                            // Log.d(TAG, "date=" + date);

                            if(!currentUserDate.equals(date)){
                                // Сообщаем об окончании задачи
                                intent.putExtra(LoginActivity.PARAM_STATUS, LoginActivity.STATUS_FINISH);
                                intent.putExtra(LoginActivity.PARAM_RESULT, time_tmp * 1000);
                                sendBroadcast(intent);
                            }

                            TimeUnit.SECONDS.sleep(time_tmp);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } break;
            }
            stop();
        }

        void stop() {
            Log.d(TAG, "SRun: " + startId + " end, stopSelfResult("
                    + startId + ") = " + stopSelfResult(startId));
        }
    }
}
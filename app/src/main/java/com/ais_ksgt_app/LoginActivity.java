package com.ais_ksgt_app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.ais_ksgt_app.R;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.service.SessionAndDateCheckService;
import com.web_service.DatabaseLoginResponse;
import com.web_service.WebServiceResponse;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.utils.StaticMethods.showToast;

public class LoginActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<WebServiceResponse>  {

    // Тег класса (для логирования)
    private final static String TAG = "T_LoginActivity";

    // Уникальный id загрузчика
    private static final int LDR_BASIC_ID = 1;

    // Прогресс бар во время загрузки данных
    private ProgressDialog progressDialog;

    // Поле ввода логина
    private EditText userLogin;

    // Поле ввода пароля
    private EditText userPassword;

    // Кнопка авторизации
    private Button btnLogin;

    // Чекбокс
    private CheckBox checkBox;

    // Сохранённые настройки приложения (логин)
    SharedPreferences sPref;
    private final static String PREF_LOGIN_PARAM = "userLogin";

    // Константы сервиса (сессия и проверка даты)
    public final static  int TASK1_CODE = 1;
    public final static  int TASK2_CODE = 2;
    public final static int STATUS_START = 100;
    public final static int STATUS_FINISH = 200;
    public final static String PARAM_TIME = "time";
    public final static String PARAM_TASK = "task";
    public final static String PARAM_DATE = "date";
    public final static String PARAM_RESULT = "result";
    public final static String PARAM_STATUS = "status";
    public final static String BROADCAST_ACTION = "LOGIN_SESSION";

    @Override
    public void onBackPressed() {
        finish();
        return;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "ON START!");

        try {
            stopService(new Intent(this, SessionAndDateCheckService.class));

            Log.d(TAG, "Service stopped!");
        } catch (Exception e){
            Log.d(TAG, "stopService failed!");
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Спрятать клавиатуру
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Log.d(TAG, "onCreate");

        userLogin = (EditText) findViewById(R.id.userLogin);
        userPassword = (EditText) findViewById(R.id.userPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        checkBox = (CheckBox) findViewById(R.id.checkBox);

        // Получить сохранённый логин
        sPref = getPreferences(MODE_PRIVATE);
        String savedText = sPref.getString(PREF_LOGIN_PARAM, "");
        if(!savedText.equals("")) {
            userLogin.setText(savedText);
        }

        // TODO: DEBUG
        //userLogin.setText("test_user222");
        //userPassword.setText("1");

        // Кнопка авторизации
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // кнопка Войти

                btnLogin.setEnabled(false);

                // Формируем необходимые данные для запроса на сервер
                String userLoginText = userLogin.getText().toString(); // Логин из поля ввода
                String userPasswordText = userPassword.getText().toString(); // Пароль из поля ввода
                String DEVICE_ID = "Unknown"; // Уникальный идентификатор устройства IMEI на GSM, MEID для CDMA)
                int TIMEOUT = 0; // Время ожидания запроса от веб-сервиса
                try {
                    TIMEOUT = getResources().getInteger((R.integer.TIMEOUT));
                } catch (Exception e){
                    TIMEOUT = 5000;
                }
                String callWebServiceFunction = "tryLogin"; // Метод на веб-сервисе

                // Получаем уникальный идентификатор устройства
                try {
                    TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    DEVICE_ID = telephonyManager.getDeviceId().toString();
                } catch (SecurityException e) {
                    Log.d(TAG, "SecurityException: " + e);
                }

                // Проверка на заполнение полей логина и пароля
                if (userLoginText.trim().equals("") || userPasswordText.trim().equals("")) {
                    showToast(LoginActivity.this, "Введите логин и пароль");
                    btnLogin.setEnabled(true);
                    return;
                }

                // Свёрток данных для Loader'a
                Bundle infoBundle = new Bundle();

                // Данные для подключения к веб-сервису
                infoBundle.putString("webServiceFunction", callWebServiceFunction);
                infoBundle.putString("namespace", getString(R.string.WebServiceNAMESPACE));
                infoBundle.putString("url", getString(R.string.WebServiceURL));
                infoBundle.putInt("timeout", TIMEOUT);

                // Пользовательские данные
                infoBundle.putString("login", userLoginText);
                infoBundle.putString("password", userPasswordText);
                infoBundle.putString("device_id", DEVICE_ID);

                // Запуск (перезапуск) загрузчика
                Log.d(TAG, "Перезапуск загрузчика");
                getSupportLoaderManager().restartLoader(LDR_BASIC_ID, infoBundle, LoginActivity.this);
                //getSupportLoaderManager().restartLoader(LDR_BASIC_ID, infoBundle, LoginActivity.this);
                getSupportLoaderManager().initLoader(LDR_BASIC_ID, infoBundle, LoginActivity.this);
            }
        });
    }

    @Override
    public Loader<WebServiceResponse> onCreateLoader(int id, Bundle args) {

        Log.d(TAG, "onCreateLoader");

        // Создание progressDialog на время запроса данных
        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setMessage("Пожалуйста подождите... Идёт попыта авторизации");
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.show();

        return new BasicLoader(this, args);
    }

    @Override
    public void onLoadFinished(Loader<WebServiceResponse> loader, WebServiceResponse getUserInfoWebServiceResponse) {

        //Log.d(TAG, "getUserInfoWebServiceResponse.json: " + getUserInfoWebServiceResponse.json);

        Log.d(TAG, "onLoadFinished");
        Log.d(TAG, "getUserInfoWebServiceResponse.success: " + getUserInfoWebServiceResponse.success);
        Log.d(TAG, "getUserInfoWebServiceResponse.errorMessage: " + getUserInfoWebServiceResponse.errorMessage);
        //if (getUserInfoWebServiceResponse.json != null)
        //    Log.d(TAG, getUserInfoWebServiceResponse.json);

        if (getUserInfoWebServiceResponse.success) { // Получен ответ от сервера

            // Разбор JSON ответа сформированного на сервере
            Gson gson = new GsonBuilder().create();
            DatabaseLoginResponse userInfo = gson.fromJson(getUserInfoWebServiceResponse.json, DatabaseLoginResponse.class);

            Log.d(TAG, "userDataAndSheets.success: " + userInfo.success);

            if (userInfo.success) { // Успешная авторизация

                long webSerivceDateIncremented;
                long currentDate;
                long differenceBetweenDates;

                try {
                    DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                    Calendar c = Calendar.getInstance();
                    c.setTime(df.parse(userInfo.webServiceDate));
                    c.add(Calendar.DATE, 1);
                    webSerivceDateIncremented = c.getTime().getTime();

                    currentDate = Calendar.getInstance().getTime().getTime();
                    differenceBetweenDates = webSerivceDateIncremented - currentDate;
                } catch (Exception e){
                    showToast(LoginActivity.this, "Не удалось преобразовать дату, полученную от веб-сервиса");
                    if (progressDialog != null) {
                        progressDialog.hide();
                        progressDialog.dismiss();
                    }
                    btnLogin.setEnabled(true);
                    return;
                }

                if(differenceBetweenDates <= 0){
                    showToast(LoginActivity.this, "Дата устройства отличается от даты, полученной с сервера");
                    if (progressDialog != null) {
                        progressDialog.hide();
                        progressDialog.dismiss();
                    }
                    btnLogin.setEnabled(true);
                    return;
                }

                Log.d(TAG, "Разница в датах: " + differenceBetweenDates);
                Intent intent_tmp = new Intent(this, SessionAndDateCheckService.class).putExtra(PARAM_TIME, differenceBetweenDates)
                        .putExtra(PARAM_TASK, TASK1_CODE);
                // стартуем сервис
                Log.d(TAG, "service=" + TASK1_CODE + " start!");
                startService(intent_tmp);

                intent_tmp = new Intent(this, SessionAndDateCheckService.class).putExtra(PARAM_TIME, 1.0)
                        .putExtra(PARAM_TASK, TASK2_CODE).putExtra(PARAM_DATE, userInfo.webServiceDate);

                Log.d(TAG, "service=" + TASK2_CODE + " start!");
                startService(intent_tmp);

                if(checkBox.isChecked()){ // Сохранять текущий логин
                    sPref = getPreferences(MODE_PRIVATE);
                    SharedPreferences.Editor ed = sPref.edit();
                    ed.putString(PREF_LOGIN_PARAM, userLogin.getText().toString());
                    ed.apply();
                } else { // Сохранить пустую строку вместо логина
                    sPref = getPreferences(MODE_PRIVATE);
                    SharedPreferences.Editor ed = sPref.edit();
                    ed.putString(PREF_LOGIN_PARAM, "");
                    ed.apply();
                }

                if(userInfo.represents.size() == 1){ // Пользователь закреплён за одним подразделением
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("jsonUserInfo", getUserInfoWebServiceResponse.json);
                    LoginActivity.this.startActivity(intent);

                } else if (userInfo.represents.size() > 1){ //Пользователь состоит в нескольких подразделениях
                    Intent intent = new Intent(LoginActivity.this, RepresentActivity.class);
                    intent.putExtra("jsonUserInfo", getUserInfoWebServiceResponse.json);
                    LoginActivity.this.startActivity(intent);

                } else { // Пользователь не состоит в подразделениях
                    showToast(LoginActivity.this, "Пользователь не закреплён ни за одним подразделением!");
                }
            } else { // Не удалось авторизоваться (введены неверные данные авторизации)
                Log.d(TAG, "!userDataAndSheets.success: " + userInfo.errorMessage);
                showToast(this, userInfo.errorMessage);
            }
        } else { // Не удалось получить ответ от сервера
            Log.d(TAG, "!getUserInfoAndSheetsWebServiceResponse.success: " + getUserInfoWebServiceResponse.errorMessage);
            showToast(this, getUserInfoWebServiceResponse.errorMessage);
        }

        // Закрыть progressDialog
        if (progressDialog != null) {
            progressDialog.hide();
            progressDialog.dismiss();
        }

        // Разрешить авторизацию с помощью кнопки
        btnLogin.setEnabled(true);
    }

    @Override
    public void onLoaderReset(Loader<WebServiceResponse> loader) {
        Log.d(TAG, "onLoaderReset");
    }

    final static class BasicLoader extends AsyncTaskLoader<WebServiceResponse> {

        private WebServiceResponse getUserInfoWebServiceResponse = new WebServiceResponse();
        private Bundle infoBundle;

        public BasicLoader(Context context, Bundle args) {
            super(context);
            this.infoBundle = args;
        }

        @Override
        public WebServiceResponse loadInBackground() {

            Log.d(TAG, "loadInBackground");

            SoapObject soapObject = new SoapObject(infoBundle.getString("namespace"), infoBundle.getString("webServiceFunction"));
            String SOAP_ACTION = infoBundle.getString("namespace") + infoBundle.getString("webServiceFunction") + "\"";

            PropertyInfo propertyInfo1 = new PropertyInfo();
            propertyInfo1.setName("login");
            propertyInfo1.setValue(infoBundle.getString("login"));
            propertyInfo1.setType(String.class);
            soapObject.addProperty(propertyInfo1);

            PropertyInfo propertyInfo2 = new PropertyInfo();
            propertyInfo2.setName("password");
            propertyInfo2.setValue(infoBundle.getString("password"));
            propertyInfo2.setType(String.class);
            soapObject.addProperty(propertyInfo2);

            PropertyInfo propertyInfo3 = new PropertyInfo();
            propertyInfo3.setName("device_id");
            propertyInfo3.setValue(infoBundle.getString("device_id"));
            propertyInfo3.setType(String.class);
            soapObject.addProperty(propertyInfo3);

            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(soapObject);

            try {
                HttpTransportSE httpTransportSE = new HttpTransportSE(infoBundle.getString("url"), infoBundle.getInt("timeout"));
                httpTransportSE.call(SOAP_ACTION, envelope);

                SoapObject resultObject = (SoapObject) envelope.bodyIn;
                if (resultObject != null) {
                    getUserInfoWebServiceResponse.success = true;
                    getUserInfoWebServiceResponse.json = resultObject.getProperty(0).toString();
                }

            } catch (SoapFault soapFault) {
                getUserInfoWebServiceResponse.success = false;
                getUserInfoWebServiceResponse.errorMessage = "Soap Fault Exception " + soapFault;

            } catch (IOException ioException) {
                getUserInfoWebServiceResponse.success = false;
                getUserInfoWebServiceResponse.errorMessage = "Не удалось получить ответ от сервера!";

            } catch (XmlPullParserException parserException) {
                getUserInfoWebServiceResponse.success = false;
                getUserInfoWebServiceResponse.errorMessage = "XmlPullParserException " + parserException;

            } catch (Exception exception) {
                getUserInfoWebServiceResponse.success = false;
                getUserInfoWebServiceResponse.errorMessage = "Exception " + exception;
            }

            return getUserInfoWebServiceResponse;
        }

        @Override
        public void deliverResult(WebServiceResponse data) {
            Log.d(TAG, "deliverResult");
            if (isStarted()) {
                // Показать результат, если загрузчик уже запущен
                super.deliverResult(data);
            }
        }

        @Override
        protected void onStartLoading() {
            Log.d(TAG, "onStartLoading");
            super.onStartLoading();

            // Не выполнять запрос по загрузке данных, если они загружены. Это позволяет избежать повторные запросы при разворачивании приложения, нажатии кнопку на back и т.д.
            if (getUserInfoWebServiceResponse.json != null) {
                //Log.d("AsyncTaskLoaderGoogle", "mData.valueRange != null");
                deliverResult(getUserInfoWebServiceResponse);
            }

            // Выполнить «принудительную» загрузку данных
            else {
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

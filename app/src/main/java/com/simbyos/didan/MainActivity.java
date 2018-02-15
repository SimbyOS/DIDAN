package com.simbyos.didan;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.AppUpdaterUtils;
import com.github.javiersantos.appupdater.enums.AppUpdaterError;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.github.javiersantos.appupdater.objects.Update;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PERMISSION_REQUEST_CODE = 2228;
    public SharedPreferences sPref;
    public AppUpdater appUpdater;
    String mlogin = "";
    String mpassword = "";
    AsyncTask<Void, Void, Boolean> mCheckLoginTask;
    UpdateUI updateUI;
    NavigationView navigationView;
    WebView webInfo;
    Context context;
    ProgressBar progressBar;
    String action = "maininfo";

    public static boolean hasConnection(final Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiInfo != null && wifiInfo.isConnected()) {
            return true;
        }
        wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifiInfo != null && wifiInfo.isConnected()) {
            return true;
        }
        wifiInfo = cm.getActiveNetworkInfo();
        return wifiInfo != null && wifiInfo.isConnected();
    }

    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.


                } else {

                    AlertDialog.Builder d = new AlertDialog.Builder(this);
                    d.setMessage("Ошибка получения доступа! Предоставте разрешения в настройках!");
                    d.setTitle("Ошибка");
                    d.setPositiveButton("Выход", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    });

                    d.show();
                    finish();

                }
            }
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.app_name);
        context = getBaseContext();
        this.progressBar = findViewById(R.id.progressBar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.


        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.INTERNET,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    PERMISSION_REQUEST_CODE);
            do {
            }
            while ((ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED));
        }

        AppUpdaterUtils appUpdaterUtils = new AppUpdaterUtils(this)
                .setUpdateFrom(UpdateFrom.GITHUB)
                .setGitHubUserAndRepo("SimbyOS", "DIDAN")

                .withListener(new AppUpdaterUtils.UpdateListener() {
                    @Override
                    public void onSuccess(Update update, Boolean isUpdateAvailable) {
                        Log.d("UpdateEngine", Boolean.toString(isUpdateAvailable) + update.getLatestVersion());
                        if (isUpdateAvailable) {
                            final String urlDownloadApk = "http://github.com/SimbyOS/DIDAN/releases/download/" + update.getLatestVersion() + "/DIDAN.apk";
                            AlertDialog.Builder d = new AlertDialog.Builder(MainActivity.this, R.style.Theme_AppCompat_Light_Dialog);
                            d.setTitle("Доступно обновление!");

                            d.setMessage("Доступно обновление DIDAN Client " + update.getLatestVersion() + "" +
                                    " Настоятельно рекомендуем обновиться. Вас ждут улучшение стабильности и новые функции!" +
                                    "");
                            d.setPositiveButton("Обновить", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //get destination to update file and set Uri
                                    //TODO: First I wanted to store my update .apk file on internal storage for my app but apparently android does not allow you to open and install
                                    //aplication with existing package from there. So for me, alternative solution is Download directory in external storage. If there is better
                                    //solution, please inform us in comment
                                    String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
                                    String fileName = "DIDAN.apk";
                                    destination += fileName;
                                    final Uri uri = Uri.parse("file://" + destination);
                                    File file = new File(destination);
                                    if (file.exists())
                                        file.delete();

                                    //get url of app on server
                                    String url = urlDownloadApk;

                                    //set downloadmanager
                                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                                    request.setDescription("Обновление DIDAN Client");
                                    request.setTitle("Обновление");

                                    //set destination
                                    request.setDestinationUri(uri);

                                    // get download service and enqueue file
                                    final DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                                    final long downloadId = manager.enqueue(request);

                                    //set BroadcastReceiver to install app when .apk is downloaded
                                    BroadcastReceiver onComplete = new BroadcastReceiver() {
                                        public void onReceive(Context ctxt, Intent intent) {
                                            Intent install = new Intent(Intent.ACTION_VIEW);
                                            install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            install.setDataAndType(uri,
                                                    manager.getMimeTypeForDownloadedFile(downloadId));
                                            startActivity(install);

                                            unregisterReceiver(this);
                                            finish();
                                        }
                                    };
                                    registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                                }
                            });

                            d.setNegativeButton("Позже", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            }).show();

                        }

                    }

                    @Override
                    public void onFailed(AppUpdaterError error) {
                        Log.d("UpdateEngine", "Something went wrong");
                    }
                });

        appUpdaterUtils.start();


        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        this.webInfo = findViewById(R.id.webViewInfo);

        ///// Получаем логин и пасс из настроек
        sPref = getSharedPreferences("", Context.MODE_PRIVATE);
        mlogin = sPref.getString("login", "");
        mpassword = sPref.getString("password", "");
        if (mlogin == "" || mpassword == "") {
            Intent intentLogin = new Intent(getBaseContext(), LoginActivity.class);
            startActivityForResult(intentLogin, 1);
        } else {
            if (hasConnection(getBaseContext())) {
                this.mCheckLoginTask = new UserLoginTask(mlogin, mpassword);
                this.mCheckLoginTask.execute();
            } else {
                Toast.makeText(getBaseContext(), "Ошибка соединения, проверьте подключение!", Toast.LENGTH_LONG);
            }

        }









    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);


        return true;
    }

    //Клик по дроверу (Меню)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.update_ui_menu: {
                UpdateUIAction();
                break;
            }
            case R.id.menu_settings: {
                Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
                startActivity(intent);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void UpdateUIAction() {
        if (!hasConnection(getBaseContext())) {
            Toast.makeText(getBaseContext(), "Ошибка соединения, проверьте подключение!", Toast.LENGTH_LONG);
        } else {
            if (action == "maininfo") {
                updateUI = new UpdateUI(mlogin, mpassword, getBaseContext());
                updateUI.execute();
            }
            if (action == "contacts") {
                ContactsUpdateUI updateUI = new ContactsUpdateUI(mlogin, mpassword);
                updateUI.execute();
            }
            if (action == "payment_history") {
                PaymentHistoryUpdateTask paymentHistoryUpdateTask = new PaymentHistoryUpdateTask(mlogin, mpassword);
                paymentHistoryUpdateTask.execute();
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_update_ui) {
            UpdateUIAction();

        }
        if (id == R.id.nav_payment_history) {
            action = "payment_history";
            PaymentHistoryUpdateTask paymentHistoryUpdateTask = new PaymentHistoryUpdateTask(mlogin, mpassword);
            paymentHistoryUpdateTask.execute();
        }
        if (id == R.id.nav_main_info) {
            action = "maininfo";
            updateUI = new UpdateUI(mlogin, mpassword, getBaseContext());
            updateUI.execute();
        }
        if (id == R.id.nav_contacts) {
            action = "contacts";
            ContactsUpdateUI updateUI = new ContactsUpdateUI(mlogin, mpassword);
            updateUI.execute();
        }
        if (id == R.id.nav_exit) {
            finish();
        }
        if (id == R.id.nav_settings) {
            Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
            startActivity(intent);
        }
        if (id == R.id.nav_account_exit) {
            SharedPreferences sPref = getBaseContext().getSharedPreferences("", MODE_PRIVATE);
            SharedPreferences.Editor editor = sPref.edit();
            editor.putString("login", "");
            editor.putString("password", "");
            editor.commit();
            Intent mStartActivity = new Intent(getBaseContext(), MainActivity.class);
            int mPendingIntentId = 123456;
            PendingIntent mPendingIntent = PendingIntent.getActivity(getBaseContext(), mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            System.exit(0);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    /**
     * Чек авторизации на сервере, в случаи ошибки вызываем LoginActivity в случаи успеха UpdateUI , мда MVP тут и не пахнет ...
     */
    class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                OkHttpClient client = new OkHttpClient();
                RequestBody formBody = new okhttp3.FormBody.Builder()
                        .add("xl", mEmail)
                        .add("xp", mPassword)
                        .add("x", "45")
                        .add("y", "10")
                        .build();
                Request request = new Request.Builder()
                        .url("http://didan.org/index.php?act=billinfo").post(formBody)
                        .build();

                try {
                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response.toString());
                    return response.body().string().contains("Баланс");
                } catch (Exception e) {
                }

                Log.d("GetBal", "OK");

                return false;
            } catch (Exception d) {
                return false;
            }

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mCheckLoginTask = null;

            if (success) {
                //Выделяем память под updateUI
                updateUI = new UpdateUI(mlogin, mpassword, getBaseContext());
                updateUI.execute();


            } else {
                Intent intentLogin = new Intent(getBaseContext(), LoginActivity.class);
                startActivity(intentLogin);
            }
        }

        @Override
        protected void onCancelled() {
            mCheckLoginTask = null;
        }
    }

    // Обновление UI (Основная информация по аккаунту)
    class UpdateUI extends AsyncTask<Void, Void, Void> {

        final private String login;
        final private String password;
        public String htmldocument = "";
        public String days_count = "";
        String pocket_name = "";
        String tableInfo = "";
        private String balance = "";
        private Context context;

        UpdateUI(String login, String password, Context ctx) {
            this.login = login;
            this.password = password;
            this.context = ctx;
        }

        public String GetBalanceString() {
            try {
                Document doc = Jsoup.parse(htmldocument);
                Elements all = doc.getAllElements();
                Element balanceEl = all.get(64); //index баласна (Да, да, парсинг html это плохо)
                Log.d("Data", balanceEl.text());
                return balanceEl.text();
            } catch (Exception d) {
                Log.e("FatalErrorLogin", d.getMessage());
                return "";
            }

        }

        public Float GetBalanceFloat() {
            try {
                String temp = GetBalanceString();
                temp = temp.replace(" ", "");
                temp = temp.replace(",", ".");
                temp = temp.replace("руб.", "");
                return Float.parseFloat(temp);
            } catch (Exception d) {
                Log.e("ParseException", d.getMessage());
                return 0.0f;
            }

        }

        // Ручками забитые консты
        public Float GetPacketPerDayFloat() {
            try {
                String temp = GetPacketString();

                if (temp.contains("Пакет 33М"))
                    return 3.94f;

                if (temp.contains("Пакет 55М"))
                    return 4.60f;

                if (temp.contains("Пакет 99М"))
                    return 6.60f;
            } catch (Exception d) {
                Log.e("FatalErrorLogin", d.getMessage());
                return -1.0f;
            }
            return 0.0f;
        }

        public String GetInfoTable() {
            try {
                String temphtml = "";
                temphtml += "<h2>Основная информация</h2>";
                Document doc = Jsoup.parse(htmldocument);
                Element all = doc.getElementById("dle-content");
                all.select("div.kbntnv").first().remove();
                temphtml += all.html();

                return temphtml;

            } catch (Exception d) {
                Log.e("FatalErrorLogin", d.getMessage());
                return "";
            }

        }

        public String GetPacketString() {
            try {
                Document doc = Jsoup.parse(htmldocument);
                Elements all = doc.getElementsByTag("td");
                if (all.get(7).text().contains("Пакет")) {
                    Log.d("Data", all.get(7).text());
                    return all.get(7).text();
                } else {
                    if (all.get(8).text().contains("Пакет")) {
                        Log.d("Data", all.get(8).text());
                        return all.get(8).text();
                    }
                }


            } catch (Exception d) {
                Log.e("FatalErrorLogin", d.getMessage());
                return "";
            }
            return ""; //"" В случаи ошибок
        }

        @NonNull
        private String getDocumentHTML(String login, String password) {
            try {
                OkHttpClient client = new OkHttpClient();
                RequestBody formBody = new okhttp3.FormBody.Builder()
                        .add("xl", login)
                        .add("xp", password)
                        .add("x", "45")
                        .add("y", "10")
                        .build();
                Request request = new Request.Builder()
                        .url("http://didan.org/index.php?act=billinfo").post(formBody)
                        .build();

                try {
                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response.toString());
                    htmldocument = response.body().string();
                } catch (Exception e) {
                }

                Log.d("GetBal", "OK");
                if (htmldocument.contains("Баланс")) {
                    Log.d("GetBal", "Login Success");
                }
                return htmldocument;
            } catch (Exception d) {
                return "";
            }

        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            webInfo.setVisibility(View.INVISIBLE);
            Log.d("Task Update UI", "Begin");


        }

        @Override
        protected Void doInBackground(Void... params) {
            htmldocument = getDocumentHTML(login, password);
            String bal = GetBalanceString();
            tableInfo = GetInfoTable();
            pocket_name = GetPacketString();
            if (bal == "") {
                balance = "Ошибка обновления";
            } else {
                balance = "Баланс: " + bal;
                try {
                    Float balanceFloat = GetBalanceFloat();
                    Float packetPerDayFloat = GetPacketPerDayFloat();
                    Float daysFloat = balanceFloat / packetPerDayFloat;
                    int result = (int) Math.floor(daysFloat);
                    this.days_count = "Осталось дней: " + String.valueOf(result);
                } catch (Exception d) {
                    Log.e("Task", d.getMessage());
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Log.d("Task", "UpdateWidgetText");
            if (balance == "Ошибка обновления") {
                Toast.makeText(context, "Ошибка обновления информации, проверьте подключение!", Toast.LENGTH_LONG);
            } else {
                Menu menu = navigationView.getMenu();
                // Баланс в меню дровера
                MenuItem balanceItem = menu.findItem(R.id.nav_balance_info);
                balanceItem.setTitle(balance);
                //Костыль рестарта при ошибке
                if (balance == " логин") {
                    RestartApp();
                }
                // Кол-во дней в зависимости от пакета
                MenuItem daysc = menu.findItem(R.id.nav_dayscount_info);
                daysc.setTitle(days_count);
                // Пакет
                MenuItem pock = menu.findItem(R.id.nav_pocket_info);
                pock.setTitle(pocket_name);
                String css = "<style>" +
                        "table {\n" +
                        " border-collapse: collapse;\n" +
                        "    text-align: start;" +
                        "color: black;" +

                        "}\n" +
                        "\n" +
                        "table, th, td {\n" +
                        "    border: 1px solid black;\n " +
                        "     color: black;" +
                        "    text-align: start;" +
                        "" +

                        "}" +
                        "</style>";
                webInfo.loadData(css + this.tableInfo, "text/html; charset=utf-8", "utf-8");
                webInfo.setWebViewClient(new WebViewClient() {

                    public void onPageFinished(WebView view, String url) {
                        progressBar.setVisibility(View.INVISIBLE);
                        webInfo.setVisibility(View.VISIBLE);
                    }
                });
                Log.d("Update UI Task", pocket_name + balance + days_count);

                TextView tv = findViewById(R.id.header);
                tv.setText("DIDAN | Логин: " + login);
            }

        }

        public void RestartApp() {
            Intent mStartActivity = new Intent(context, MainActivity.class);
            int mPendingIntentId = 123456;
            PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            System.exit(0);
        }
    }


    //Класс обновления информации контактов (Обратной связи)

    public class ContactsUpdateUI extends AsyncTask<Void, Void, Void> {

        private final String mEmail;
        private final String mPassword;
        String htmldocument = "";
        String htmluouput = "";

        ContactsUpdateUI(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            webInfo.setVisibility(View.INVISIBLE);
            Log.d("Task Update UI", "Begin");


        }

        @NonNull
        private String getDocumentHTML(String login, String password) {
            try {
                String htmldocument = "";
                OkHttpClient client = new OkHttpClient();
                RequestBody formBody = new okhttp3.FormBody.Builder()
                        .add("xl", login)
                        .add("xp", password)
                        .add("x", "45")
                        .add("y", "10")
                        .build();
                Request request = new Request.Builder()
                        .url("http://didan.org/?act=contacts").post(formBody)
                        .build();

                try {
                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response.toString());
                    htmldocument = response.body().string();
                } catch (Exception e) {
                }

                Log.d("GetBal", "OK");
                if (htmldocument.contains("Баланс")) {
                    Log.d("GetBal", "Login Success");
                }
                return htmldocument;
            } catch (Exception d) {
                return "";
            }

        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                String css = "<h2> Телефоны</h2>" +
                        "<style>" +
                        "table {\n" +
                        " border-collapse: collapse;\n" +
                        "    text-align: start;" +
                        "color: black;" +
                        " display: table;" +
                        "    text-align: center;" +
                        "    font-style: normal;" +
                        "    font-size: high;" +
                        "}\n" +
                        "\n" +
                        "table, th, td {\n" +
                        "" +
                        "    border: 1px solid black;\n " +
                        "     color: black;" +
                        "    text-align: center;" +
                        "    font-style: normal;" +
                        "    font-size: high;" +
                        "" +

                        "}" +
                        "</style> <table>";
                htmldocument = getDocumentHTML(mEmail, mPassword);
                Document doc = Jsoup.parse(htmldocument);

                Elements all = doc.select("table.tbl5");

                all.get(3).select("div.spoiler").remove();
                htmluouput = css + all.get(0).html() + "</table> <h2>Терминалы</h2> <table>" + all.get(3).html();
            } catch (Exception d) {
                Log.e("FatalErrorLogin", d.getMessage());

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            //
            webInfo.loadData(htmluouput, "text/html; charset=utf-8", "utf-8");

            //
            progressBar.setVisibility(View.INVISIBLE);
            webInfo.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onCancelled() {

        }
    }

    //Класс обновления информации контактов (Обратной связи)

    public class PaymentHistoryUpdateTask extends AsyncTask<Void, Void, Void> {

        private final String mEmail;
        private final String mPassword;
        String htmldocument = "";
        String htmluouput = "";

        PaymentHistoryUpdateTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            webInfo.setVisibility(View.INVISIBLE);
            Log.d("Task Update UI", "Begin");


        }

        @NonNull
        private String getDocumentHTML(String login, String password) {
            try {
                String curDate = (String) android.text.format.DateFormat.format("yyyyMM", new Date());
                String htmldocument = "";
                OkHttpClient client = new OkHttpClient();
                RequestBody formBody = new okhttp3.FormBody.Builder()
                        .add("xl", login)
                        .add("xp", password)
                        .add("x", "45")
                        .add("y", "10")
                        .build();
                Request request = new Request.Builder()

                        .url("http://didan.org/index.php?act=billhistory&date=" + curDate + "&type=all").post(formBody)
                        .build();

                try {
                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response.toString());
                    htmldocument = response.body().string();
                } catch (Exception e) {
                }

                Log.d("GetBal", "OK");
                if (htmldocument.contains("Баланс")) {
                    Log.d("GetBal", "Login Success");
                }
                return htmldocument;
            } catch (Exception d) {
                return "";
            }

        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                String css = "<h2> История платежей</h2>" +
                        "<style>" +
                        "table {\n" +
                        " border-collapse: collapse;\n" +
                        "    text-align: start;" +
                        "color: black;" +
                        " display: table;" +
                        "    text-align: center;" +
                        "    font-style: normal;" +
                        "    font-size: high;" +
                        "}\n" +
                        "\n" +
                        "table, th, td {\n" +
                        "" +
                        "    border: 1px solid black;\n " +
                        "     color: black;" +
                        "    text-align: center;" +
                        "    font-style: normal;" +
                        "    font-size: high;" +
                        "" +

                        "}" +
                        "</style> <table>";
                htmldocument = getDocumentHTML(mEmail, mPassword);
                Document doc = Jsoup.parse(htmldocument);

                Elements all = doc.select("table.tbl4");
                //       all.get(3).select("div.spoiler").remove();
                htmluouput = css + all.get(0).html();
            } catch (Exception d) {
                Log.e("FatalErrorLogin", d.getMessage());

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            //
            webInfo.loadData(htmluouput, "text/html; charset=utf-8", "utf-8");
            webInfo.setWebViewClient(new WebViewClient() {

                public void onPageFinished(WebView view, String url) {
                    progressBar.setVisibility(View.INVISIBLE);
                    webInfo.setVisibility(View.VISIBLE);
                }
            });
            //
            progressBar.setVisibility(View.INVISIBLE);
            webInfo.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onCancelled() {

        }
    }
}

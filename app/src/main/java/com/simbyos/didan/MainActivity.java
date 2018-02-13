package com.simbyos.didan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    public SharedPreferences sPref;
    String mlogin = "";
    String mpassword = "";
    AsyncTask<Void, Void, Boolean> mCheckLoginTask;
    UpdateUI updateUI;
    NavigationView navigationView;
    WebView webInfo;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.app_name);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        this.webInfo = findViewById(R.id.webViewInfo);

        ///// Получаем логин и пасс из настроек
        sPref = getSharedPreferences("", Context.MODE_PRIVATE);
        mlogin = sPref.getString("login", "");
        mpassword = sPref.getString("password", "");
        if (mlogin == "" || mpassword == "") {
            Intent intentLogin = new Intent(getBaseContext(), LoginActivity.class);
            startActivity(intentLogin);
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
            case R.id.nav_exit: {
                finish();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_update_ui) {
            if (!hasConnection(getBaseContext())) {
                Toast.makeText(getBaseContext(), "Ошибка соединения, проверьте подключение!", Toast.LENGTH_LONG);
            } else {
                updateUI = new UpdateUI(mlogin, mpassword, getBaseContext());
                updateUI.execute();
            }

        }
        if (id == R.id.nav_exit) {
            finish();
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
                Document doc = Jsoup.parse(htmldocument);
                Element all = doc.getElementById("dle-content");

                return all.child(1).html();

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
                // Кол-во дней в зависимости от пакета
                MenuItem daysc = menu.findItem(R.id.nav_dayscount_info);
                daysc.setTitle(days_count);
                // Пакет
                MenuItem pock = menu.findItem(R.id.nav_pocket_info);
                pock.setTitle(pocket_name);
                webInfo.loadData(this.tableInfo, "text/html; charset=utf-8", "utf-8");
                Log.d("Update UI Task", pocket_name + balance + days_count);

            }

        }
    }
}

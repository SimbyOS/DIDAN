package com.simbyos.didan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public SharedPreferences sPref;
    String mlogin = "";
    String mpassword = "";
    AsyncTask<Void, Void, Boolean> mCheckLoginTask;
    UpdateUI updateUI;
    NavigationView navigationView;
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


        ///// Получаем логин и пасс из настроек
        sPref = getSharedPreferences("", Context.MODE_PRIVATE);
        mlogin = sPref.getString("login", "");
        mpassword = sPref.getString("password", "");
        if (mlogin == "" || mpassword == "") {
            Intent intentLogin = new Intent(getBaseContext(), LoginActivity.class);
            startActivity(intentLogin);
        } else {
            this.mCheckLoginTask = new UserLoginTask(mlogin, mpassword);
            this.mCheckLoginTask.execute();
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
            this.updateUI.execute();
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
                Map<String, String> data = new HashMap<String, String>();
                data.put("xl", mEmail);
                data.put("xp", mPassword);
                data.put("x", "43");
                data.put("y", "10");

                Log.d("GetBal", "OK");
                String htmldocument = "";
                htmldocument = HttpRequest.post("http://didan.org").form(data).body();
                if (htmldocument.contains("Баланс")) {
                    Log.d("GetBal", "Login Success");
                    return true;
                } else {
                    Log.d("GetBal", htmldocument);
                    return false;
                }


            } catch (Exception d) {
                Log.e("FatalErrorLogin", d.getMessage());
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
                return;
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
                Map<String, String> data = new HashMap<String, String>();
                data.put("xl", login);
                data.put("xp", password);
                data.put("x", "10");
                data.put("y", "45");

                Log.d("GetBal", "OK");
                htmldocument = com.simbyos.didan.HttpRequest.post("http://didan.org/index.php?act=billinfo").form(data).body();
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

                Log.d("Update UI Task", pocket_name + balance + days_count);

            }

        }
    }
}

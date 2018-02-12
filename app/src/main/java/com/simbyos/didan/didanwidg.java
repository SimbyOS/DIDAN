package com.simbyos.didan;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of App Widget functionality.
 */
public class didanwidg extends AppWidgetProvider {

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                         int appWidgetId) {


        SharedPreferences sPref = context.getSharedPreferences("",context.MODE_PRIVATE);
        String login = sPref.getString("login","");
        String password  = sPref.getString("password","");
        Log.d("Preferences",login + ":" + password);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.didanwidg);

        UpdateBalTask task = new UpdateBalTask(login,password,views,appWidgetManager,appWidgetId,context);
        task.execute();
        // Instruct the widget manager to update the widget

        Intent updateIntent = new Intent(context, didanwidg.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
                new int[] { appWidgetId });
        PendingIntent pIntent = PendingIntent.getBroadcast(context, appWidgetId, updateIntent, 0);
        views.setOnClickPendingIntent(R.id.llo, pIntent);

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
    final String UPDATE_ALL_WIDGETS = "update_all_widgets";
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Intent intent = new Intent(context, didanwidg.class);
        intent.setAction(UPDATE_ALL_WIDGETS);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(),
                900000, pIntent);

    }
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equalsIgnoreCase(UPDATE_ALL_WIDGETS)) {
            ComponentName thisAppWidget = new ComponentName(
                    context.getPackageName(), getClass().getName());
            AppWidgetManager appWidgetManager = AppWidgetManager
                    .getInstance(context);
            int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
            for (int appWidgetID : ids) {
                updateAppWidget(context, appWidgetManager, appWidgetID);
            }
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Intent intent = new Intent(context, didanwidg.class);
        intent.setAction(UPDATE_ALL_WIDGETS);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pIntent);
    }

    class UpdateBalTask extends AsyncTask<Void, Void, Void> {

        final private String login;
        final private String password;
        private String balance ="";
        private  RemoteViews rv;
        int WidgetId;
        private AppWidgetManager wgmanager;
        private Context context;
        UpdateBalTask(String login,String password, RemoteViews d,AppWidgetManager wg,int widid,Context ctx){
            this.login = login;
            this.password = password;
            this.rv = d;
            this.wgmanager = wg;
            this.WidgetId = widid;
            this.context = ctx;
        }
        public String htmldocument = "";
        public String GetBalanceString(){
            try{
                Document doc = Jsoup.parse(htmldocument);
                Elements all = doc.getAllElements();
                Element balanceEl = all.get(64); //index баласна (Да, да, парсинг html это плохо)
                Log.d("Data", balanceEl.text());
                return balanceEl.text();
            }
            catch(Exception d){
                Log.e("FatalErrorLogin",d.getMessage());
                return "";
            }

        }

        @NonNull
        private String getDocumentHTML(String login, String password) {
            try{
                Map<String, String> data = new HashMap<String, String>();
                data.put("xl", login);
                data.put("xp", password);
                data.put("x", "10");
                data.put("y", "45");

                Log.d("GetBal","OK");
                htmldocument = com.simbyos.didan.HttpRequest.post("http://didan.org").form(data).body();
                if(htmldocument.contains("Баланс")) {
                    Log.d("GetBal", "Login Success");
                }
                return htmldocument;
            }
            catch (Exception d){
                return "";
            }

        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.rv.setTextColor(R.id.balance, Color.MAGENTA);
            this.wgmanager.updateAppWidget(this.WidgetId, this.rv);
            Log.d("Task","Begin")      ;

        }

        @Override
        protected Void doInBackground(Void... params) {
            htmldocument = getDocumentHTML(login, password);
            String bal =  GetBalanceString();
            if(bal == ""){
                balance ="Ошибка обновления";
            }
            else{
                balance ="Баланс: "+ bal;
            }

            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Log.d("Task","UpdateWidgetText");
            if(balance == "Ошибка обновления"){
        //        this.rv.setTextColor(R.id.name, Color.RED);
                this.rv.setTextColor(R.id.balance, Color.WHITE);
            }
            else{
      //          this.rv.setTextColor(R.id.name, Color.BLUE);

                this.rv.setTextViewText(R.id.balance, balance);
                Log.d("Task","End");
                this.rv.setTextColor(R.id.balance, Color.WHITE);
            }


            this.wgmanager.updateAppWidget(this.WidgetId, this.rv);
        }
    }
}


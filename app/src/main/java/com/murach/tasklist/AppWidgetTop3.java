package com.murach.tasklist;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

/**Providers class for an app Widget*/
// must extend WidgetProbider (here we use support version)
public class AppWidgetTop3 extends AppWidgetProvider {

    public static final String TAG=AppWidgetTop3.class.getSimpleName();

    // Android calls this method when the user adds the app widget to the Home screen.
    @Override
    public void onUpdate(Context context, 
            AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // set up the app widget and display on the UI

        // loop through all app widgets for this provider
        // (because user can add multiple instances of a widget to the Home screen)
        for (int i = 0; i < appWidgetIds.length; i++) {
            
            // create a pending intent for the Task List activity
            Intent intent = new Intent(context, TaskListActivity.class);
            PendingIntent pendingIntent = 
                    PendingIntent.getActivity(context, 0, intent, 0);

            // get the layout and set the listener for the app widget
            RemoteViews views = new RemoteViews(
                    context.getPackageName()// name of the package that contaitns layout resource
                    , R.layout.app_widget_top3);
            views.setOnClickPendingIntent(
                    R.id.appwidget_top3, pendingIntent);
            /* RemoteViews allows android to display the layout for the app widget in another process
            (in the process for the home screen) */

            // get the names to display on the app widget
            TaskListDB db = new TaskListDB(context);
            String[] names = db.getTopTaskNames(3);
            
            // update the user interface
            views.setTextViewText(R.id.task1TextView, 
                    names[0] == null ? "" : names[0]);
            views.setTextViewText(R.id.task2TextView, 
                    names[1] == null ? "" : names[1]);
            views.setTextViewText(R.id.task3TextView, 
                    names[2] == null ? "" : names[2]);
            // atm changes have only been made to RemoteViews, but not the widget yet

            // update the current app widget
            int appWidgetId = appWidgetIds[i];
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    // executed when the app widget receives a broadcast
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // check whether is TASK_MODIFIED action that's broadcast by the database
        if (intent.getAction().equals(TaskListDB.TASK_MODIFIED)) {
            // update the app widget
            AppWidgetManager manager = 
                    AppWidgetManager.getInstance(context);
            // app widget provider Component identifier
            ComponentName provider = 
                    new ComponentName(context, AppWidgetTop3.class);
            // Get the list of appWidgetIds that have been bound to the given AppWidget provider.
            int[] appWidgetIds = manager.getAppWidgetIds(provider);
            // trigger onUpdate method to update widget display
            onUpdate(context, manager, appWidgetIds);
        }
    }


    // ============== NOT REQUIRED =================================================================

    // when app widget is added to the home screen
    @Override
    public void onEnabled(Context context) {
        Log.d(TAG,"onEnabled");
        super.onEnabled(context);
    }

    // when widget instance is removed from the home screen
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(TAG,"onDeleted");
        super.onDeleted(context, appWidgetIds);
    }

    // when last instance of app widget is remove from the home screen
    @Override
    public void onDisabled(Context context) {
        Log.d(TAG,"onDisabled");
        super.onDisabled(context);
    }
}
package com.murach.tasklist;

import java.util.ArrayList;

import com.google.tabmanager.TabManager;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import static com.murach.tasklist.TaskListDB.TASK_COMPLETED_COL;
import static com.murach.tasklist.TaskListDB.TASK_HIDDEN_COL;
import static com.murach.tasklist.TaskListDB.TASK_ID;
import static com.murach.tasklist.TaskListDB.TASK_ID_COL;
import static com.murach.tasklist.TaskListDB.TASK_LIST_ID_COL;
import static com.murach.tasklist.TaskListDB.TASK_NAME_COL;
import static com.murach.tasklist.TaskListDB.TASK_NOTES_COL;

public class TaskListActivity extends FragmentActivity {
    TabHost tabHost;
    TabManager tabManager;
    TaskListDB db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        // get tab manager
        tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();        
        tabManager = new TabManager(this, tabHost, R.id.realtabcontent);
        
        // get database
        db = new TaskListDB(getApplicationContext());

        // add a tab for each list in the database
        ArrayList<List> lists = db.getLists();
        if (lists != null && lists.size() > 0) {
            for (List list : lists) {
                // TabSpec represents a tab specialization
                // create a new TabSpec with associated tag
                TabSpec tabSpec = tabHost.newTabSpec(list.getName());
                // specify a labed as the tab indicator
                tabSpec.setIndicator(list.getName());
                // add new tab with tabSpec and attached fragment
                tabManager.addTab(tabSpec, TaskListFragment.class, null);
                /* That's how fragment knows which list he is attached to:
                TabHost tabHost = (TabHost) container.getParent().getParent();
                currentTabTag = tabHost.getCurrentTabTag();
                refreshTaskList();  */
            }
        }

        // sets current tab to the last tab opened
        if (savedInstanceState != null) {
            tabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
        
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tab", tabHost.getCurrentTabTag());
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_task_list, menu);
        return true;
    }    

    /** fetch all tasks from the database by using content provider query */
    public ArrayList<Task> requestTasks(String listName){
        ArrayList<Task> tasks=new ArrayList<>();
        Cursor cursor = getContentResolver().query(TaskListProvider.BASE_URI,null,null,null,null);
        while (cursor.moveToNext()) { // while cursor successfully moved to a next record
            tasks.add(new Task(
                    cursor.getInt(TASK_ID_COL),
                    cursor.getInt(TASK_LIST_ID_COL),
                    cursor.getString(TASK_NAME_COL),
                    cursor.getString(TASK_NOTES_COL),
                    cursor.getString(TASK_COMPLETED_COL),
                    cursor.getString(TASK_HIDDEN_COL)));
        }
        /* Second Way:
        String[] columns = {TASK_ID, TASK_NAME, TASK_NOTES}
        Cursor cursor = getContentResolver().query(TASKS_URI, columns, null, null, null);
        // Third way:

        */
        return tasks;
    }

    /** delete task from the database by using content provider */
    public int requestDelete(int taskId){
        String where = TASK_ID + " = ?";
        String[] whereArgs = { Integer.toString(taskId) };
        int deleteCount = getContentResolver()
                .delete(TaskListProvider.BASE_URI, where, whereArgs);
        return deleteCount;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menuAddTask:
                Intent intent = new Intent(this, AddEditActivity.class);
                intent.putExtra("tab", tabHost.getCurrentTabTag());
                startActivity(intent);
                break;
            case R.id.menuDelete:
                // Hide all tasks marked as complete
//                ArrayList<Task> tasks = db.getTasks(tabHost.getCurrentTabTag());
                // Using content provider
                ArrayList<Task> tasks = requestTasks(tabHost.getCurrentTabTag());

                for (Task task : tasks){
                    // when task's checkbox is clicked, it's completedDate is set to current time
                    // else, when unchecked - to 0
                    if (task.getCompletedDateMillis() > 0){
                        task.setHidden(Task.TRUE);
                        db.updateTask(task);
                    }
                }
                
                // Refresh list
                TaskListFragment currentFragment = (TaskListFragment) 
                        getSupportFragmentManager().
                        findFragmentByTag(tabHost.getCurrentTabTag());
                currentFragment.refreshTaskList();
                
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
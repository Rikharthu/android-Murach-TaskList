package com.murach.tasklist;

import java.sql.PreparedStatement;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/** Class that encapsulates everything related to databases in our app (DAO) */
public class TaskListDB {
    // TEXT(String), INTEGER(int), REAL(double)

    // database constants
    public static final String DB_NAME = "tasklist.db";
    public static final int    DB_VERSION = 1; // increment if change structure to call onUpgrade() or onDowngrade()

    // list table constants
    public static final String LIST_TABLE = "list";
    
    public static final String LIST_ID = "_id";
    public static final int    LIST_ID_COL = 0;

    public static final String LIST_NAME = "list_name";
    public static final int    LIST_NAME_COL = 1;

    // task table constants
    public static final String TASK_TABLE = "task";

    public static final String TASK_ID = "_id";
    public static final int    TASK_ID_COL = 0;

    public static final String TASK_LIST_ID = "list_id";
    public static final int    TASK_LIST_ID_COL = 1;
    
    public static final String TASK_NAME = "task_name";
    public static final int    TASK_NAME_COL = 2;
    
    public static final String TASK_NOTES = "notes";
    public static final int    TASK_NOTES_COL = 3;
    
    public static final String TASK_COMPLETED = "date_completed";
    public static final int    TASK_COMPLETED_COL = 4;

    public static final String TASK_HIDDEN = "hidden";
    public static final int    TASK_HIDDEN_COL = 5;
    
    // CREATE and DROP TABLE statements
    public static final String CREATE_LIST_TABLE = 
            "CREATE TABLE " + LIST_TABLE + " (" +
            LIST_ID   + " INTEGER PRIMARY KEY AUTOINCREMENT, " + 
            LIST_NAME + " TEXT    UNIQUE)";
    // for instance: Personal, Business
    
    public static final String CREATE_TASK_TABLE = 
            "CREATE TABLE " + TASK_TABLE + " (" + 
            TASK_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " + 
            TASK_LIST_ID    + " INTEGER, " + 
            TASK_NAME       + " TEXT, " + 
            TASK_NOTES      + " TEXT, " + 
            TASK_COMPLETED  + " TEXT, " + 
            TASK_HIDDEN     + " TEXT)";

    // doesn't cause error if table doesn't exist thanks to IF EXISTS keyword
    public static final String DROP_LIST_TABLE = 
            "DROP TABLE IF EXISTS " + LIST_TABLE;

    public static final String DROP_TASK_TABLE = 
            "DROP TABLE IF EXISTS " + TASK_TABLE;
    
    public static final String TASK_MODIFIED =  
            "com.murach.tasklist.TASK_MODIFIED";


    // Database Helper
    private static class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context, String name, 
                CursorFactory factory, int version) {
            super(context, name, factory, version);
        }


        // called if database doesn't exist on the device yer
        @Override
        public void onCreate(SQLiteDatabase db) {
            // execute simple SQL queries
            // execSQL is not injection-safe

            // create tables
            db.execSQL(CREATE_LIST_TABLE);
            db.execSQL(CREATE_TASK_TABLE);
            
            // insert lists
            db.execSQL("INSERT INTO list VALUES (1, 'Personal')");
            db.execSQL("INSERT INTO list VALUES (2, 'Business')");
            
            // insert sample tasks
            db.execSQL("INSERT INTO task VALUES (1, 1, 'Pay bills', " +
                    "'Rent\nPhone\nInternet', '0', '0')");
            db.execSQL("INSERT INTO task VALUES (2, 1, 'Get hair cut', " +
                    "'', '0', '0')");
        }

        // called if android finds a database on a device version that is lower than the one passed into constructor
        // DBHelper(..., ..., ..., DB_VERSION)
        @Override
        public void onUpgrade(SQLiteDatabase db, 
                int oldVersion, int newVersion) {
            // update database structure depending on oldVersion and newVersion
            if(oldVersion<2){
                // . . .
            }
            if(oldVersion<3){
                // . . .
            }
            // etc.

            // use ALTER to add a new columnd without removing the data
            Log.d("Task list", "Upgrading db from version " 
                    + oldVersion + " to " + newVersion);
            
            Log.d("Task list", "Deleting all data!");
            db.execSQL(TaskListDB.DROP_LIST_TABLE);
            db.execSQL(TaskListDB.DROP_TASK_TABLE);
            onCreate(db);
        }
    }
    
    // database object and database helper object
    private SQLiteDatabase db;
    private DBHelper dbHelper;
    private Context context;
    
    // constructor
    public TaskListDB(Context context) {
        this.context = context;
        // pass curent DB version
        dbHelper = new DBHelper(context, DB_NAME, null, DB_VERSION);
    }
    
    // private methods
    private void openReadableDB() {
        db = dbHelper.getReadableDatabase();
    }
    
    private void openWriteableDB() {
        db = dbHelper.getWritableDatabase();
    }
    
    private void closeDB() {
        if (db != null)
            db.close();
    }
    
    private void broadcastTaskModified() {
        Intent intent = new Intent(TASK_MODIFIED);
        context.sendBroadcast(intent);
    }

    // public (client) methods
    public ArrayList<List> getLists() {
        ArrayList<List> lists = new ArrayList<List>();
        openReadableDB();
        Cursor cursor = db.query(LIST_TABLE, 
                null, null, null, null, null, null);
        while (cursor.moveToNext()) {
             List list = new List();
             list.setId(cursor.getInt(LIST_ID_COL));
             list.setName(cursor.getString(LIST_NAME_COL));
             
             lists.add(list);
        }
        cursor.close();
        closeDB();
        return lists;
    }

    /** returns a List object that corresponds with the specified list name */
    public List getList(String name) {
        String where = LIST_NAME + "= ?";
        String[] whereArgs = { name };

        openReadableDB();
        Cursor cursor = db.query(LIST_TABLE, null, 
                where, whereArgs, null, null, null);
        List list = null;
        cursor.moveToFirst();
        list = new List(cursor.getInt(LIST_ID_COL),
                        cursor.getString(LIST_NAME_COL));
        cursor.close();
        this.closeDB();
        
        return list;
    }

    /** Retrieve all tasks from the specified List (model, not collection)*/
    public ArrayList<Task> getTasks(String listName) {
        // specify which rows to retrieve
        // list id and not hidden
        // '?' marks the parameter that will be supplied later
        /* as a result, the WHERE clause retrieves all rows where the list_id column
         is equal to the supplied listID value and where hidden columnd is not equal to '1' */
        String where = 
                TASK_LIST_ID + "= ? AND " + 
                TASK_HIDDEN + "!='1'";
        long listID = getList(listName).getId();
        // arguments for the where clause
        String[] whereArgs = { Long.toString(listID) };

        this.openReadableDB();

        Cursor cursor = db.query(TASK_TABLE, null, 
                where, whereArgs, 
                null, null, null);
        /* As a result, the query method retrieves all columns and
        doesn’t include GROUP BY, HAVING, or ORDER BY clauses.
        However, if you want to specify the columns to retrieve, you can code an array */
        ArrayList<Task> tasks = new ArrayList<Task>();
        // must move to first record too
        // though moveToNext() can replace moveToFirst()
        while (cursor.moveToNext()) { // while cursor successfully moved to a next record
             tasks.add(getTaskFromCursor(cursor));
        }
        if (cursor != null)
            cursor.close();
        this.closeDB();
        return tasks;
    }

    /** Retrieve task from the DB with specified id */
    public Task getTask(long id) {
        String where = TASK_ID + "= ?";
        String[] whereArgs = { Long.toString(id) };

        this.openReadableDB();        
        Cursor cursor = db.query(TASK_TABLE, 
                null, where, whereArgs, null, null, null);
        cursor.moveToFirst();
        Task task = getTaskFromCursor(cursor);
        if (cursor != null)
            cursor.close();
        this.closeDB();
        
        return task;
    }    

    /** Returns a single Task object from the cursor */
    private static Task getTaskFromCursor(Cursor cursor) {
        if (cursor == null || cursor.getCount() == 0){
            // null or 0 rows
            return null;
        }
        else {
            try {
                // get values from the cursor at specified column names
                Task task = new Task(
                    cursor.getInt(TASK_ID_COL), 
                    cursor.getInt(TASK_LIST_ID_COL),
                    cursor.getString(TASK_NAME_COL), 
                    cursor.getString(TASK_NOTES_COL), 
                    cursor.getString(TASK_COMPLETED_COL),
                    cursor.getString(TASK_HIDDEN_COL));
                return task;
            }
            catch(Exception e) {
                return null;
            }
        }
    }

    /** Insert Task into the database */
    public long insertTask(Task task) {
        // object used to store column names and their corresponding values
        ContentValues cv = new ContentValues();
        // ID columnd is AUTO_INCREMENT => it will be generated
        cv.put(TASK_LIST_ID, task.getListId());
        cv.put(TASK_NAME, task.getName());
        cv.put(TASK_NOTES, task.getNotes());
        cv.put(TASK_COMPLETED, task.getCompletedDate());
        cv.put(TASK_HIDDEN, task.getHidden());
        
        this.openWriteableDB();
        long rowID = db.insert(TASK_TABLE, null, cv);
        this.closeDB();
        
        broadcastTaskModified();
        
        return rowID;
    }

    public int updateTaskPStmt(Task task){
        // A SQL statement is precompiled and stored in a PreparedStatement object
        // injection-free
        // TODO finish me
        SQLiteStatement stmt = db.compileStatement("UPDATE "+TASK_TABLE
        + " SET "+TASK_LIST_ID+" = ?, "+TASK_NAME+" = ?,"
        +TASK_NOTES+" = ?,"+TASK_COMPLETED+" = ?, "+TASK_HIDDEN+" =? WHERE "+TASK_ID+" = ?");
        stmt.bindLong(0,task.getListId());
        stmt.bindString(1,task.getName()+"");
        stmt.bindString(2,task.getNotes()+"");
        stmt.bindString(3,task.getCompletedDate()+"");
        stmt.bindString(4,task.getHidden()+"");
        stmt.execute();
        return -1;
    }

    public int updateTask(Task task) {
        ContentValues cv = new ContentValues();
        cv.put(TASK_LIST_ID, task.getListId());
        cv.put(TASK_NAME, task.getName());
        cv.put(TASK_NOTES, task.getNotes());
        cv.put(TASK_COMPLETED, task.getCompletedDate());
        cv.put(TASK_HIDDEN, task.getHidden());
        
        String where = TASK_ID + "= ?";
        String[] whereArgs = { String.valueOf(task.getId()) };

        this.openWriteableDB();
        // update the record that corresponds to our WHERE clause
        // returns the count of affected rows
        int rowCount = db.update(TASK_TABLE, cv, where, whereArgs);
        this.closeDB();
        
        broadcastTaskModified();
        
        return rowCount;
    }    

    /** Delete the Task with the specified id from the database */
    public int deleteTask(long id) {
        String where = TASK_ID + "= ?";
        String[] whereArgs = { String.valueOf(id) };

        // you can also use transaction
        db.beginTransaction();

        this.openWriteableDB();
        // delete the Entry that corresponds to the passed WHERE clause
        // returns the count of affected rows
        int rowCount = db.delete(TASK_TABLE, where, whereArgs);
        db.setTransactionSuccessful();
        // all changes will be reverted unles marked "clean" by setTranscationSuccessful()
        db.endTransaction();
        this.closeDB();
        
        broadcastTaskModified();
        
        return rowCount;
    }
    
    public String[] getTopTaskNames(int taskCount) {
        String where = TASK_COMPLETED + "= '0'";
        String orderBy = TASK_COMPLETED + " DESC";
        this.openReadableDB();
        Cursor cursor = db.query(TASK_TABLE, null, 
                where, null, null, null, orderBy);

        String[] taskNames = new String[taskCount];
        for (int i = 0; i < taskCount; i++) {
            if (cursor.moveToNext()) {
                Task task = getTaskFromCursor(cursor);
                taskNames[i] = task.getName();
            }
        }
        
        if (cursor != null)
            cursor.close();
        db.close();
                
        return taskNames;
    }
    
    /*
     * Methods for content provider
     * NOTE: You don't close the DB connection after executing
     * a query, insert, update, or delete operation
     */
    public Cursor genericQuery(String[] projection, String where,
            String[] whereArgs, String orderBy) {
        this.openReadableDB();
        return db.query(TASK_TABLE, projection, where, whereArgs, null, null, orderBy);
    }

    public long genericInsert(ContentValues values) {
        this.openWriteableDB();
        return db.insert(TASK_TABLE, null, values);
    }

    public int genericUpdate(ContentValues values, String where,
            String[] whereArgs) {
        this.openWriteableDB();
        return db.update(TASK_TABLE, values, where, whereArgs);
    }

    public int genericDelete(String where, String[] whereArgs) {
        this.openWriteableDB();
        return db.delete(TASK_TABLE, where, whereArgs);
    }
}
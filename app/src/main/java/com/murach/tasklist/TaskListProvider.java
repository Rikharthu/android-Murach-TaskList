package com.murach.tasklist;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/*
Querying Content Provider:
How to define constants for columns
public static final String TASK_ID = "_id";
public static final int TASK_ID_COL = 0;
public static final String TASK_NAME = "task_name";
public static final int TASK_NAME_COL = 2;
How to create the base Uri object
public static final String AUTHORITY = ncom.murach.tasklist.provider"
public static final Uri TASKS_URI =
Uri -parse("content:// 11 + AUTHORITY + "/tasks");
How to query a content provider
Cursor cursor = getContentResolver( )
.query(TASKS_URI, null, null, null, null);
A second way to query a content provider
string where = TASK_HIDDEN + " = '1' ";
string orderBy = TASK_COMPLETED + " DESC";
Cursor cursor = getContentResolver( )
.query(TASKS_URI, null, where, null, orderBy);
A third way to query a content provider
String[] columns = {TASK_ID, TASK_NAME, TASK_NOTES}
Cursor cursor = getContentResolver( )
.query(TASKS_URI, columns, null, null, null);
How to delete data from the content provider
String where = TASK_ID + " = ?";
String[] whereArgs = { Integer.toString(taskld) };
int deleteCount = getContentResolver()
.delete(TASKS_URI, where, whereArgs);
Another way to delete data from the content provider
Uri taskUri = ContentUris.withAppendedld(TASKS_URI, taskld);
int deleteCount = getContentResolver()
.delete(taskUri, null, null);
 */

/** Content provider allows multiple apps to share the same data. Provides data in table form
 * An application accesses the data from a content provider with a ContentResolver client object. */
public class TaskListProvider extends ContentProvider{
    /* Built-in android content providers:
        - Contacts
        - Calendar
        - Settings
        - Bookmarks
        - Media (images, music, video) */
    // A content URI is a URI that identifies data in a provider.
    // Content URIs include the symbolic name of the entire provider (its authority)
    // and a name that points to a table (a path).
    // URI syntax: content://authority/path[/id]
    // authority - name of the provider (same as in manifest)
    // path - table or file
    // id (optional) - id of the table row
    // A URI for all rows of the Task table:    content://com.murach.tasklist.provider/tasks
    // Single row of Task table with ID=2: content://com.murach.tasklist.provider/tasks/2
    // MIME types for content providers
    // multiple rows: vnd.android.cursor.dir/vnd.company_name.contenfc_type
    //      vnd.android.cursor.dir/vnd.murach.tasklist.tasks
    // single row: vnd.android.cursor.item/vnd.company_name.content_type
    //      vnd.android.cursor.item/vnd.murach.tasklist.tasks

    public static final String AUTHORITY = "com.murach.tasklist.provider"; // name of the provider
    public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY + "/table");
    public static final int MATCH_INT = 1;

    // maps content URI "patterns" to integer values
    private UriMatcher uriMatcher;

    @Override
    public boolean onCreate() {
        // initialize URI matcher
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "table", MATCH_INT);
        // maps "com.murach.tasklist.provider/table" to MATCH_INT (1)
        return true;
    }

    // adds a new row to the appropriate table, using the values in the ContentValues argument
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // check the URI argument
        int match = uriMatcher.match(uri);
        switch(match){
            case MATCH_INT:{
                long id = new TaskListDB(getContext()).genericInsert(values);
                // notify registered observers that row was changed
                // By default, CursorAdapter objects get this notification
                getContext().getContentResolver().notifyChange(uri, null);
                // return uri object for the inserted row
                // for instance, if id=14, then:
                // content://com.murach.tasklist.provider/tasks/14
                return uri.buildUpon().appendPath(String.valueOf(id)).build();
            }
            default:
                throw new UnsupportedOperationException(
                        "URI: " + uri + " not supported.");
        }
    }

    // Retrieve data from your provider
    // URI received from client, columns, WHERE clause, WHERE args, sorting order
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        Log.d("TaskListProvider","query received");
        // get matching int to received URI
        int match = uriMatcher.match(uri);
        // and take appropriate action depending on the URI argument
        switch(match){
            case MATCH_INT:
                // com.murach.tasklist.provider/table
                // return a cursor with database data
                return new TaskListDB(getContext())
                        .genericQuery(projection, selection, selectionArgs, sortOrder);
            // mapping to database query:
            // db.query("<TABLE_NAME>", projection, selection, selectionArgs, null, null, sortOrder);
            default:
                // send error message thath this URI could not be handled by our Content Provider
                throw new UnsupportedOperationException (
                        "URI " + uri + " is not supported.");
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        int match = uriMatcher.match(uri);
        switch(match){
        case MATCH_INT:
            int n = new TaskListDB(getContext()).genericUpdate(
                            values, selection, selectionArgs);
            getContext().getContentResolver().notifyChange(uri, null);
            return n;
        default:
            throw new UnsupportedOperationException (
                    "URI " + uri + " is not supported.");
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int match = uriMatcher.match(uri);
        switch(match){
        case MATCH_INT:
            int n = new TaskListDB(getContext()).genericDelete(selection, selectionArgs);
            getContext().getContentResolver().notifyChange(uri, null);
            return n;
        default:
            throw new UnsupportedOperationException ("URI " + uri + " is not supported.");
        }
    }

    // this method must be overrided for querying to work
    @Override
    public String getType(Uri uri) {
        final int match = uriMatcher.match(uri);
        switch(match) {
        case MATCH_INT:
            // returns the MIME type for passed URI
            // "dir" - URI that is mapped to MATCH_INT return a multiple rows
            return "vnd.android.cursor.dir/vnd.com.murach.tasklist.provider";
        default:
            throw new UnsupportedOperationException ("URI " + uri + " is not supported.");
        }
    }
}
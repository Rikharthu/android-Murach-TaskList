package com.murach.tasklist;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TabHost;

public class TaskListFragment extends Fragment {

    private ListView taskListView;
    private String currentTabTag;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
            Bundle savedInstanceState) {
        
        // inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_task_list, 
            container, false);
        
        // get references to widgets
        taskListView = (ListView) view.findViewById (R.id.taskListView);

        // get the current tab
        TabHost tabHost = (TabHost) container.getParent().getParent();
        //  This Fragment is hosted inside a FrameLayout in activity_task_list.xml
        //  <TabHost> <- second getPArent() call
        //      <LinearLayout> <- first getParent() call
        //          ...
        //          <FrameLayout>
        currentTabTag = tabHost.getCurrentTabTag();
        
        // refresh the task list view
        refreshTaskList();

        // return the view
        return view;
    }
    
    public void refreshTaskList() {
        // get task list for current tab from database
        Context context = getActivity().getApplicationContext();
        TaskListDB db = new TaskListDB(context);
        ArrayList<Task> tasks = db.getTasks(currentTabTag); // get tasks for current tag (list name)

        // create adapter and set it in the ListView widget
        TaskListAdapter adapter = new TaskListAdapter(context, tasks);
        taskListView.setAdapter(adapter);
        // TODO could remake to notifyDataSetChanged()
    }
    
    @Override
    public void onResume() {
        super.onResume();
        refreshTaskList();
    }
}
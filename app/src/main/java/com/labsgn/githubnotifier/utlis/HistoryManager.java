package com.labsgn.githubnotifier.utlis;

import android.content.Context;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by rhony on 11/03/16.
 */
public class HistoryManager {
    private Context context;
    private Set<String> searchList;
    private String
            className = "HistoryManager" ,
            fileName = Constant.HISTORY_FILENAME;
    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructs a SearchHistoryManager, then call loadData() method. It requires an Activity Context.
     * @param context a Context from an Activity
     */
    public HistoryManager(Context context){
        this.context = context;
        loadData();
    }

    private void loadData() {
        try{
            if (!fileExists(fileName)){
                resetHistory();
            }
            searchList = mapper.readValue(context.openFileInput(fileName),
                     Set.class);
        }catch (IOException e){
            Logger.log_e(className, e.toString());
        }
        Logger.log_i(className, "load data "+searchList);
    }

    public void resetHistory() {
        List<String> list = new ArrayList<>();
        list.clear();
        try {
            mapper.writeValue(context.openFileOutput(fileName,
                    Context.MODE_PRIVATE), list);
            if (searchList != null)
                searchList.clear();
        }
        catch (IOException e){
            Logger.log_e(className, e.toString());
        }
    }

    /**
     * Return whether a file exits in the context path
     * @param fname path to the file
     * @return true if the file exits, else return false.
     */
    private boolean fileExists(String fname) {
        File file = context.getFileStreamPath(fname);
        return file.exists();
    }

    /**
     * Add an entry of search visited by user. Appends it at the end of the JSON file.
     * @param s Repository ID as a String
     */
    public void addSearch(String s){
        try {
            searchList.add(s);
            mapper.writeValue(context.openFileOutput(fileName,
                    Context.MODE_PRIVATE), searchList);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Return all the history searches as a List of Strings
     * @return
     */
    public Set getSearches() {
        return searchList;
    }

}

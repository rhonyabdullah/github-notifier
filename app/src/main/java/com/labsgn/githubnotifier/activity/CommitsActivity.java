package com.labsgn.githubnotifier.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.ListViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.labsgn.githubnotifier.R;
import com.labsgn.githubnotifier.utlis.Constant;
import com.labsgn.githubnotifier.utlis.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import tylerjroach.com.eventsource_android.EventSource;
import tylerjroach.com.eventsource_android.EventSourceHandler;
import tylerjroach.com.eventsource_android.MessageEvent;

public class CommitsActivity extends Activity {

    private String className = "CommitsActivity";
    private final ObjectMapper mapper = new ObjectMapper();

    // Global List of Commits
    private ArrayList<Commit> commits = new ArrayList<>();

    // Map of EventSource foreach repo: (repositoriesID, EventSource)
    private ConcurrentHashMap<String, EventSource> reposEventSources = new ConcurrentHashMap<>();

    // Map of colors foreach repo: (repositoriesID, color)
    private HashMap<String, Integer> repoColors = new HashMap<>();

    private MyListAdapter adapter;

    // List of repositories identifiers
    private ArrayList<String> reposIdArray = new ArrayList<>();

    private String myApi;
    private String gitHubApiToken;
    private String streamdataioAppToken;
    private String streamdataioProxyPrefix;

    private ListViewCompat commitsListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.log_i(className, "onCreate(Bundle savedInstanceState) CALLED");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_commits);

        Intent intent = getIntent();

        // Get GitHub API token from SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        //If this preference does not exist return null value
        gitHubApiToken = sharedPref.getString("gitHubApiToken", "");

        // If the user is not gitHub-authenticated --> start LoginActivity
        if (gitHubApiToken.isEmpty()) {
            startLoginActivity();
        }

        // Get the array of Repositories-ID as Strings
        reposIdArray = intent.getStringArrayListExtra("reposId");

        Logger.log_i(className, "gitHubApiToken = "+gitHubApiToken);

        // Generate a random color & eventsource foreach repository
        for (int i = 0; i < reposIdArray.size(); i++) {
            repoColors.put(reposIdArray.get(i), randomColor());
        }

        streamdataioAppToken = Constant.STREAMDATAIO_API_TOKEN;
        streamdataioProxyPrefix = Constant.STREAMDATAIO_PROXY_PREFIX;

        commitsListView = (ListViewCompat) findViewById(R.id.commitsListView);
        adapter = new MyListAdapter(this, commits);
        commitsListView.setAdapter(adapter);

        //Create new instance arrayList of commits
        commits = new ArrayList<>();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Connect to every eventsource
        connectAll();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Disconnect all
        disconnectAll();
    }

    private class SSEHandler implements EventSourceHandler{

        private JsonNode ownData;
        private String repoName;

        public SSEHandler(){
            Logger.log_i(className, "SSEHandler() CALLED");
        }

        public SSEHandler(String repoName){
            this();
            this.repoName = repoName;
            Logger.log_i(className, "SSEHandler(String repoName) CALLED");
        }

        /**
         * SSE Handler for connection starting
         */
        @Override
        public void onConnect(){
            Logger.log_i(className,"onConnect() CALLED");
        }

        /**
         * Incoming message handler
         *
         * @param event type of message
         * @param message message JSON content
         * @throws IOException if JSON syntax isn't valid
         */
        @Override
        public void onMessage(String event, MessageEvent message) throws IOException {
            Logger.log_i(className,"onMessage(String event, MessageEvent message) CALLED");

            if ("data".equals(event)){
                //SSE message is a snapshot, no need to create just renew
                ownData = mapper.readTree(message.data);

                updateCommits();
            }
            else if ("patch".equals(event)) {
                // SSE message is a patch
                try {
                    JsonNode patchNode = mapper.readTree(message.data);
                    JsonPatch patch = JsonPatch.fromJson(patchNode);
                    ownData = patch.apply(ownData);

                    // Get the concerned repository name
                    String commitMessage = ownData.get(0).path("commit").path("message").textValue();

                    // Spawning an Android notification
                    createNotification("New Commit: " + repoName.substring(repoName.indexOf("/") + 1), commitMessage);

                    // Update commits array
                    updateCommits();
                } catch (JsonPatchException e) {
                    e.printStackTrace();
                }
            }
            else {
                Logger.log_e(className, "Disconnecting : " + message.toString());

                if (message.data.contains("HTTP/1.1 401 Unauthorized")) {
                    // GitHub api token may be out-of-date --> restart oauth procedure
                    SharedPreferences settings = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
                    settings.edit().clear().apply();
                    startLoginActivity();
                }
                throw new RuntimeException("Wrong SSE message!");
            }


        }

        @Override
        public void onError(Throwable t) {
            Logger.log_i(className, "onError(Throwable t) CALLED");

            // Network error message...
            if (t.toString().contains("java.nio.channels.UnresolvedAddressException")) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Logger.toast(CommitsActivity.this, "You need to connect to the Internet");
                    }
                });
                CommitsActivity.this.disconnectAll();
            }
        }

        @Override
        public void onClosed(boolean willReconnect) {
            Logger.log_i(className, "onClosed(boolean willReconnect) CALLED");
        }

        /**
         * @return commits of ONE repository, depending on its EventSource, as a JsonNode
         */
        public JsonNode getOwnData() {
            return ownData;
        }

        /**
         * Create an Android Notification with the given title et content
         * @param title notification title
         * @param text notification content text
         */
        private final void createNotification(String title, String text) {

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(CommitsActivity.this)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(title)
                            .setContentText(text);

            // Sets an ID for the notification
            Random rand = new Random();
            int mNotificationId = rand.nextInt(51);

            // Gets an instance of the NotificationManager service
            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // Builds the notification and issues it.
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }
    }

    /**
     * Update the cached commits array. This method should be called on snapshot or patch event.
     */
    public void updateCommits() {
        Logger.log_i(className,"updateCommits() CALLED");

        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        //Clear commits
        commits.clear();

        Commit commit = null;
        // foreach EventSource Handler ...
        for (Map.Entry<String, EventSource> entry : reposEventSources.entrySet()) {
            SSEHandler sseHandler = (SSEHandler) entry.getValue().getEventSourceHandler();
            JsonNode data = sseHandler.getOwnData();

            // Reconstructs commits array from JSON data
            for (Iterator<JsonNode> iterator = data.iterator(); iterator.hasNext(); ) {
                JsonNode commitJson = iterator.next();
                try {
                    commit = new Commit(
                            sdf.parse(commitJson.path("commit").path("author").path("date").textValue()),
                            commitJson.path("commit").path("author").path("name").textValue(),
                            commitJson.path("commit").path("message").textValue(),
                            commitJson.path("sha").textValue().substring(0, 9),
                            entry.getKey()
                    );

                } catch (ParseException e) {
                    e.printStackTrace();
                }
                //commit.print();
                commits.add(commit);
            }
        }
        Collections.sort(commits, new DateComparator());

        //Refresh UI
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.updateData(commits);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private class Commit {
        public Date date;
        public String user;
        public String comment;
        public String uid;
        public String repositoryID;

        public Commit(Date date, String user, String comment, String uid, String repositoryID) {
            this.date = date;
            this.user = user;
            this.comment = comment;
            this.uid = uid;
            this.repositoryID = repositoryID;

            Logger.log_i(className,"Commit(Date date, String user...) CALLED");
        }
    }

    public class DateComparator implements Comparator<Commit> {
        @Override
        public int compare(Commit o1, Commit o2) {

            Logger.log_i(className,"compare(Commit o1, Commit o2) CALLED");

            return o2.date.compareTo(o1.date);
        }
    }

    private class MyListAdapter extends BaseAdapter {

        private final LayoutInflater inflater;
        private ArrayList<Commit> commits;

        /**
         * Constructor for this adapter, we'll accept all the things we need here
         *
         * @param commits
         */
        public MyListAdapter(final Context context, final ArrayList<Commit> commits) {
            this.commits = commits;
            inflater = LayoutInflater.from(context);

            Logger.log_i(className, "MyListAdapter(final Context context ...) CALLED");
        }

        public void updateData(ArrayList<Commit> commits) {
            this.commits = new ArrayList<Commit>(commits);
        }

        public ArrayList<Commit> getData() {
            return commits;
        }

        @Override
        public int getCount() {
            return commits != null ? commits.size() : 0;
        }

        @Override
        public Object getItem(int i) {
            return commits != null ? commits.get(i) : null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            Logger.log_i(className, "getView(int position ...) CALLED");

            ViewWrapper viewWrapper;
            DateFormat mediumDateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, new Locale("EN", "en"));

            if (convertView == null) {
                // Store view elements in the Tag to improve list performance
                viewWrapper = new ViewWrapper();
                convertView = inflater.inflate(R.layout.single_commit, null);
                viewWrapper.uid = (TextView) convertView.findViewById(R.id.commit_id);
                viewWrapper.title = (TextView) convertView.findViewById(R.id.commit_message);
                viewWrapper.user = (TextView) convertView.findViewById(R.id.commit_author);
                viewWrapper.date = (TextView) convertView.findViewById(R.id.commit_date);
                viewWrapper.repoID = (TextView) convertView.findViewById(R.id.commit_repoid);
                convertView.setTag(viewWrapper);

            } else {
                // we've just avoided calling findViewById() on resource every time just use the viewHolder instead
                viewWrapper = (ViewWrapper) convertView.getTag();
            }

            // assign values if the object is not null
            if (commits != null) {
                // get the TextView from the ViewHolder and then set the text
                viewWrapper.uid.setText(commits.get(position).uid);
                viewWrapper.date.setText(mediumDateFormat.format(commits.get(position).date));
                viewWrapper.title.setText(commits.get(position).comment);
                viewWrapper.user.setText("by " + commits.get(position).user);
                viewWrapper.repoID.setText(commits.get(position).repositoryID);
                viewWrapper.repoID.setBackgroundColor(repoColors.get(commits.get(position).repositoryID));
            }

            if (position % 2 == 1) {
                convertView.setBackgroundColor(Color.rgb(230, 235, 255));
            } else {
                convertView.setBackgroundColor(Color.rgb(255, 255, 255));
            }
            return convertView;
        }
    }

    private static class ViewWrapper {
        TextView uid;
        TextView title;
        TextView user;
        TextView date;
        TextView repoID;
    }

    private void startLoginActivity() {
        Intent inte = new Intent(this, LoginActivity.class);
        startActivity(inte);
    }

    /**
     * Closes every open EventSources
     */
    private void disconnectAll() {
        Logger.log_i(className, "disconnectAll() CALLED");
        for (Map.Entry<String, EventSource> entry : reposEventSources.entrySet()) {
            disconnect(entry.getKey());
            Logger.log_i(className,"Disconnecting : " + entry.getKey());
        }
    }

    /**
     * Closes the event source connection and dereference the EventSource object
     */
    private void disconnect(String api) {
        // Disconnect the eventSource Handler
        if (reposEventSources.containsKey(api)) {
            EventSource e = reposEventSources.get(api);
            e.close();
            reposEventSources.remove(api);
        }
    }

    /**
     * Start listening to every selected repositories commits
     */
    private void connectAll() {
        Logger.log_i(className, "connectAll() CALEED" );
        for (String repo : reposIdArray) {
            Log.i("info", "Connecting : " + repo);
            Logger.log_i(className, "Connecting : " + repo);
            connect(repo);
        }
    }

    /**
     * Create the EventSource object & start listening SSE incoming messages
     */
    private void connect(String api) {

        //remove the private-repo-lock character if exists
        String clearApi = api.startsWith("\uD83D\uDD12") ? api.substring(2) :api;

        // Add the GitHub API token with an URL parameter (only way to authenticate)
        //myApi = "https://api.github.com/repos/" + api + "/commits?X-Sd-Token="+streamdataioAppToken+"&access_token=" + gitHubApiToken;
        myApi = "https://api.github.com/repos/" + clearApi + "/commits?access_token=" + gitHubApiToken;

        // Add the Streamdata.io authentication token
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-Sd-Token", streamdataioAppToken);

        // Create the EventSource with API URL & Streamdata.io authentication token
        try {
            SSEHandler sseHandler = new SSEHandler(api);
            EventSource eventSource = new EventSource
                    (
                            new URI(streamdataioProxyPrefix),
                            new URI(myApi),
                            sseHandler,
                            headers
                    );

            reposEventSources.put(api, eventSource);

        } catch (URISyntaxException e) {
            Logger.log_e(className, e.toString());
        }
    }

    /**
     * Draw a random color in [#000000 .. #E6E6E6]. (about 12 millions outcomes).
     * Used as text background, the lightest configuration render a white text still readable
     * @return
     */
    public int randomColor() {
        Random rnd = new Random();
        return Color.argb(255, rnd.nextInt(230), rnd.nextInt(230), rnd.nextInt(230));
    }
}

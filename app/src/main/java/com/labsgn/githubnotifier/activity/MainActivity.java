package com.labsgn.githubnotifier.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.labsgn.githubnotifier.R;
import com.labsgn.githubnotifier.utlis.HistoryManager;
import com.labsgn.githubnotifier.utlis.Logger;

import org.eclipse.egit.github.core.SearchRepository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static com.labsgn.githubnotifier.utlis.Constant.CONCURRENT_REPOSITORIES;
import static com.labsgn.githubnotifier.utlis.Constant.GITHUB_PUBLIC_TOKEN;

public class MainActivity extends AppCompatActivity {

    private ImageButton mainSearchButton;
    private carbon.widget.Button mainShowCommitButton;
    private ListView mainListView;
    private EditText mainSearchField;

    private Intent intent;
    private HistoryManager history;
    private RepositoryService service;

    private String
            className = "MainActivity",
            gitHubApiToken;

    private boolean hasResult = false;

    private ArrayList<String> list, selectedItems;

    private Toolbar mainToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        initComponent();

        // Get GitHub API token
        SharedPreferences sharedPref = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        gitHubApiToken = sharedPref.getString("gitHubApiToken", "");

        // If the user is not gitHub-authenticated --> start LoginActivity
        if (gitHubApiToken.isEmpty()) {
            startLoginActivity();
        }

        //Loading search history
        history = new HistoryManager(this);

        hasResult = history.getSearches().size() > 0;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayAdapter<String> listAdapter =
                        new ArrayAdapter<>(MainActivity.this,
                                R.layout.single_repo,
                                new ArrayList(history.getSearches()));

                mainListView.setAdapter(listAdapter);
            }
        });

        // Authenticating by GitHub Java SDK
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(gitHubApiToken);
        service = new RepositoryService(client);

        // Instanciate the list of repositories ID as strings
        list = new ArrayList<>();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Display the 'Sign-in' menu entry if we are use the public token, 'Sign-out' if the used token is different
     * @param menu
     * @return
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        // If the user has chosen to use public version
        if (gitHubApiToken.equals(GITHUB_PUBLIC_TOKEN)) {
            MenuItem signInButton = menu.findItem(R.id.action_signin);
            signInButton.setVisible(true);

            MenuItem signOutButton = menu.findItem(R.id.action_signout);
            signOutButton.setVisible(false);
        }

        // If the user has chosen to use authenticated api
        else {
            MenuItem signInButton = menu.findItem(R.id.action_signin);
            signInButton.setVisible(false);

            MenuItem signOutButton = menu.findItem(R.id.action_signout);
            signOutButton.setVisible(true);
        }
        return true;
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private void initComponent() {
        mainToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(mainToolbar);

        mainListView = (ListView) findViewById(R.id.mainListView);
        mainListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onItemClickMainListView(parent);
            }
        });

        mainSearchButton = (ImageButton) findViewById(R.id.mainSearchButton);
        mainSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickMainSearchButton();
            }
        });

        mainSearchField = (EditText) findViewById(R.id.mainSearchField);
        mainSearchField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) { onClickMainSearchField(actionId); return false;}
        });

        mainShowCommitButton = (carbon.widget.Button) findViewById(R.id.mainShowCommit);
        initMainShowCommitButton();
        mainShowCommitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onClickMainShowCommit();
            }
        });
    }

    private boolean onClickMainSearchField(int actionId) {
        mainShowCommitButton.setVisibility(View.GONE);
        hideKeyboard();

        if (actionId == EditorInfo.IME_ACTION_DONE){
            String searchSentence = ((EditText)findViewById(R.id.mainSearchField)).getText().toString();

            if (searchSentence.isEmpty()){return false;}

            try{
                List<SearchRepository> listRepo = service.searchRepositories(searchSentence);
                this.list.clear();
                for (SearchRepository repository : listRepo) {
                    // Add a Unicode lock if the repo is private, at the beginning of the repo name
                    String repo = repository.isPrivate() ? "\uD83D\uDD12" + repository.getId() : repository.getId();

                    // Push the repo name to the selected repos list
                    this.list.add(repo);
                }
                hasResult = !this.list.isEmpty();

                // Configure the list view
                if (!hasResult) this.list.add("No result for '" + searchSentence + "'");

                // Refresh UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.single_repo, MainActivity.this.list);
                        mainListView.setAdapter(listAdapter);
                    }
                });

                // Display the search results
                int numResults = listRepo.size();
                String message = numResults > 99 ? "> 100" : numResults == 0 ? "No" : numResults + "";
                message += " repositories found";
                Logger.toast(this, message);
            }
            catch (UnknownHostException e) {
                Logger.toast(this,"You need to connect to the Internet");
            }
            catch (RequestException e) {
                // GitHub api token may be out-of-date --> restart oauth procedure
                Logger.log_e(className, e.toString());
                signIn(null);
            }
            catch (IOException e) {
                Logger.log_e(className, e.toString());
            }
            return true;
        }
        return false;
    }

    public void signIn(MenuItem v) {
        SharedPreferences settings = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        settings.edit().clear().apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("SIGN_IN", true);
        startActivity(intent);
    }

    public void signOut(MenuItem v) {
        // Display historical searches
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                clearHistory(null);

                //Get back to login activity
                Intent inte = new Intent(MainActivity.this, LoginActivity.class);
                inte.putExtra("SIGN_OUT", true);
                startActivity(inte);
            }
        });
    }

    public void clearHistory(MenuItem v) {
        history.resetHistory();
        // Display historical searches
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayAdapter<String> listAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.single_repo, new ArrayList(history.getSearches()));
                mainListView.setAdapter(listAdapter);
            }
        });
    }

    // ListView Item click handler
    private void onItemClickMainListView(AdapterView<?> parent) {
        hideKeyboard();
        intent = new Intent(MainActivity.this, CommitsActivity.class);
        if (hasResult){
            selectedItems = new ArrayList<>();
            // Get multiple selected items
            SparseBooleanArray checked = ((ListView) parent).getCheckedItemPositions();
            for (int i = 0; i < ((ListView) parent).getAdapter().getCount(); i++) {
                if (checked.get(i)) {
                    // Add item as a String in selectedItems Array
                    selectedItems.add(parent.getItemAtPosition(i).toString());
                }
            }

            // Put repositories-IDs available for the commits view
            intent.putStringArrayListExtra("reposId", selectedItems);

            // Check number of selected repositories in [1..5]
            int numberSelectedRepos = selectedItems.size();

            if (numberSelectedRepos > 0 && numberSelectedRepos <= CONCURRENT_REPOSITORIES){
                mainShowCommitButton.setClickable(true);
                mainShowCommitButton.setText(R.string.show_commits);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mainShowCommitButton.setBackgroundColor(getResources().getColor(R.color.colorError, null));
                }
                else{
                    mainShowCommitButton.setBackgroundColor(getResources().getColor(R.color.colorError));
                }
                mainShowCommitButton.setVisibility(View.VISIBLE);
            }
            else if (numberSelectedRepos > CONCURRENT_REPOSITORIES){
                mainShowCommitButton.setClickable(false);
                mainShowCommitButton.setText(R.string.too_many_repos);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mainShowCommitButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
                }
                else{
                    mainShowCommitButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                }
                mainShowCommitButton.setVisibility(View.VISIBLE);
            }
            else{
                mainShowCommitButton.setClickable(false);
                mainShowCommitButton.setVisibility(View.GONE);
            }
        }
    }

    private void initMainShowCommitButton(){
        boolean hasMenuKey = ViewConfiguration.get(this).hasPermanentMenuKey();
        int
                resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android"),
                navigationHeight = 48;

        carbon.widget.RelativeLayout.LayoutParams buttonParams = new carbon.widget.RelativeLayout.LayoutParams
                (
                        carbon.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                        carbon.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
                );
        buttonParams.addRule(carbon.widget.RelativeLayout.ALIGN_PARENT_RIGHT);
        buttonParams.addRule(carbon.widget.RelativeLayout.ALIGN_PARENT_BOTTOM);
        if (resourceId > 0 && !hasMenuKey){
            buttonParams.setMargins(0,0, dpToPx(15), dpToPx(navigationHeight+15));
        }
        else{
            buttonParams.setMargins(0,0, dpToPx(15), dpToPx(15));
        }
        mainShowCommitButton.setLayoutParams(buttonParams);
    }

    private int dpToPx(int mDp){
        return (int) TypedValue.applyDimension
                (
                        TypedValue.COMPLEX_UNIT_DIP,
                        mDp,
                        this.getResources().getDisplayMetrics()
                );
    }

    private void onClickMainShowCommit() {
        for (String item : selectedItems) {
            history.addSearch(item);
        }
        startActivity(intent);
    }

    private void onClickMainSearchButton() {
        onClickMainSearchField(EditorInfo.IME_ACTION_DONE);
    }

    public void hideKeyboard() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Hide the virtual keyboard
                if (MainActivity.this.getCurrentFocus() != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(MainActivity.this.getCurrentFocus().getWindowToken(), 0);
                }
            }
        });
    }
}

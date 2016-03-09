package com.labsgn.githubnotifier.activity;

import android.os.Bundle;

import com.labsgn.githubnotifier.R;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * Create the EventSource object & start listening SSE incoming messages

    private void connect(String api) {
        EventSource eventSource = null;
        // Create headers: Add the streamdata.io app token
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-Sd-Token", Constant.STREAMDATAIO_API_TOKEN);

        // Create the EventSource with API URL & Streamdata.io authentication token
        try {
            eventSource = new EventSource(
                    new URI(Constant.STREAMDATAIO_PROXY_PREFIX),
                    new URI(Constant.API_URL),
                    new SSEHandler(api),
                    headers
            );
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // Start data receiving
        eventSource.connect();
    }
    */
}

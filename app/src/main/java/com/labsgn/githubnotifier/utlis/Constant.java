package com.labsgn.githubnotifier.utlis;

/**
 * Created by rhony on 08/03/16.
 */
public class Constant {
    public static final String STREAMDATAIO_API_TOKEN = "MY_STREAMDATAIO_API_TOKEN";
    public static final String STREAMDATAIO_PROXY_PREFIX = "https://streamdata.motwin.net/";

    //GitHub OAuth client public key
    public static final String GITHUB_CLIENT_PUBLIC_KEY = "MY_GITHUB_CLIENT_PUBLIC_KEY";

    //GitHub OAuth client secret key
    public static final String GITHUB_CLIENT_SECRET_KEY = "MY_GITHUB_CLIENT_SECRET_KEY";

    //GitHub access token for public utilization this token allows only to read public repos
    public static final String GITHUB_PUBLIC_TOKEN = "MY_GITHUB_PUBLIC_TOKEN";

    //Host-URI of LoginActivity Web View
    public static final String GITHUB_REDIRECT_URI = "your://redirecturi";

    //Maximum number of concurrent-listenable-repositories
    public static final int CONCURRENT_REPOSITORIES = 5;

    //File that stores history searches (json-file)
    public static final String HISTORY_FILENAME = "searches_history.json";
}

package com.labsgn.githubnotifier.utlis;

/**
 * Created by rhony on 08/03/16.
 */
public class Constant {
    public static final String STREAMDATAIO_API_TOKEN = "MWZmZjUxNDktNzkxYi00ODY4LWFmZjktM2JlZmFmOWYyNDNh";
    public static final String STREAMDATAIO_PROXY_PREFIX = "https://streamdata.motwin.net/";

    //GitHub OAuth client public key
    public static final String GITHUB_CLIENT_PUBLIC_KEY = "127abc2fddf4d8eb2359";

    //GitHub OAuth client secret key
    public static final String GITHUB_CLIENT_SECRET_KEY = "f50dca8734219ac169cf616636b0ff2fd841bc70";

    //GitHub access token for public utilization this token allows only to read public repos
    public static final String GITHUB_PUBLIC_TOKEN = "5f6a0400fb1670d9ea32f276846275f25894fb2b";

    //Host-URI of LoginActivity Web View
    public static final String GITHUB_REDIRECT_URI = "your://redirecturi";

    //Maximum number of concurrent-listenable-repositories
    public static final int CONCURRENT_REPOSITORIES = 5;

    //File that stores history searches (json-file)
    public static final String HISTORY_FILENAME = "searches_history.json";
}

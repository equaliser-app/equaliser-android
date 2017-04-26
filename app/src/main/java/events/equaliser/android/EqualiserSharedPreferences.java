package events.equaliser.android;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Wrapper for SharedPreferences.
 */
class EqualiserSharedPreferences {
    public static String EQUALISER_SHARED_PREFERENCES = "EQUALISER_SHARED_PREFERENCES";
    public static String USERNAME = "username";
    public static String FORENAME = "forename";
    public static String SURNAME = "surname";
    public static String EMAIL = "email";
    public static String PHONE = "phone";
    public static String TOKEN = "token";
    public static String SESSION_TOKEN = "session_token";
    public static String PROFILE_IMAGE = "profile_image";
    public static String URL = "url";

    public static SharedPreferences getEqualiserSharedPreferences(Context context) {
        return context.getSharedPreferences(EQUALISER_SHARED_PREFERENCES, Context.MODE_PRIVATE);
    }


}

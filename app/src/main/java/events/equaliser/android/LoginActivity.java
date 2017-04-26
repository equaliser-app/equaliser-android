package events.equaliser.android;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import events.equaliser.android.barcode.BarcodeCaptureActivity;

/**
 * Allows the user to log in to their account.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final int RC_BARCODE_CAPTURE = 9001;
    private static final String LOGIN_INFORMATION_EXTRA = "LOGIN_INFORMATION_EXTRA";
    private static final String VOLLEY_TAG = "VOLLEY_STRING_REQUEST";
    private static final String USER_ENDPOINT = Utils.API_ENDPOINT + "/auth/ephemeral";

    private RequestQueue requestQueue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = EqualiserSharedPreferences.getEqualiserSharedPreferences(getApplicationContext());

        // if the user is already logged in, take them to the main app
        if (sharedPreferences.contains(EqualiserSharedPreferences.SESSION_TOKEN)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }

        setContentView(R.layout.activity_login);

        // ring up the camera for the user to scan a log in QR code when they press the button
        Button scanQrButton = (Button) findViewById(R.id.scan_qr_button);
        scanQrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openQRScanActivity();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    final Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);

                    final ProgressDialog dialog = new ProgressDialog(this);
                    dialog.setMessage("Logging in...");
                    dialog.show();

                    requestQueue = Volley.newRequestQueue(getApplicationContext());

                    // use StringRequest as JSONObjectRequest doesn't work properly with POST requests
                    StringRequest stringRequest = new StringRequest
                            (Request.Method.POST, USER_ENDPOINT, new Response.Listener<String>() {

                                @Override
                                public void onResponse(String response) {
                                    try {
                                        JSONObject jsonObject = new JSONObject(response);
                                        if (jsonObject.getBoolean("success")) {
                                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            intent.putExtra(LOGIN_INFORMATION_EXTRA, response);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            showFailToast();
                                        }

                                        dismissDialog(dialog);

                                    } catch (JSONException e) {
                                        Log.e(TAG, e.getMessage());
                                        showFailToast();
                                        dismissDialog(dialog);
                                    }
                                }
                            }, new Response.ErrorListener() {

                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.e(TAG, error.getMessage());
                                    showFailToast();
                                    dismissDialog(dialog);
                                }
                            }) {
                        @Override
                        protected Map<String, String> getParams() throws AuthFailureError {
                            Map<String, String> params = new HashMap<>();
                            params.put("token", barcode.displayValue);
                            return params;
                        }

                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            Map<String, String> params = new HashMap<String, String>();
                            params.put("Content-Type", "application/x-www-form-urlencoded");
                            return params;
                        }
                    };

                    stringRequest.setTag(VOLLEY_TAG);
                    stringRequest.setShouldCache(false);
                    requestQueue.add(stringRequest);

                    Log.d(TAG, "Barcode read: " + barcode.displayValue);
                } else {
                    Log.d(TAG, "No barcode captured, intent data is null");
                }
            } else {
                Log.e(TAG, String.format("Error {0}", CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onStop() {
        if (requestQueue != null) {
            requestQueue.cancelAll(VOLLEY_TAG);
        }

        super.onStop();
    }

    /**
     * Dismisses a dialog on the screen.
     * @param dialog The dialog.
     */
    private void dismissDialog(Dialog dialog) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /**
     * Shows a toast to the user if login failed.
     */
    private void showFailToast() {
        Toast.makeText(getApplicationContext(), "Login failed", Toast.LENGTH_LONG).show();
    }

    /**
     * Launches the QR scan activity.
     */
    public void openQRScanActivity() {
        Intent intent = new Intent(this, BarcodeCaptureActivity.class);
        startActivityForResult(intent, RC_BARCODE_CAPTURE);
    }
}

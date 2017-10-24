/**
 * Copyright 2017 Boundless, http://boundlessgeo.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License
 */
package com.boundlessgeo.spatialconnect.services.authService;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.github.rtoshiro.secure.SecureSharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import okhttp3.Response;

public class SCExchangeAuthMethod implements ISCAuth {

    private static String LOG_TAG = SCExchangeAuthMethod.class.getSimpleName();
    private static final String USERNAME = "username";
    private static final String PWD = "pwd";
    private static final String ACCESS_TOKEN = "accessToken";
    private SecureSharedPreferences settings;
    private String serverUrl;
    private Context context;
    private String clientId;

    public SCExchangeAuthMethod(Context context, String serverUrl, String clientId) {
        this.context = context;
        this.settings = new SecureSharedPreferences(context);
        this.serverUrl = serverUrl;
        this.clientId = clientId;
    }

    @Override
    public boolean authFromCache() {
        String u = username();
        String p = getPassword();
        if (u != null && p != null) {
            return authenticate(u,p);
        } else {
            return false;
        }
    }

    @Override
    public boolean authenticate(String username, String pwd) {
        return auth(username, pwd);
    }

    @Override
    public boolean refreshToken(String token) {
        //TODO: implement
        return false;
    }

    @Override
    public void logout() {
        removeCredentials();
    }

    @Override
    public String xAccessToken() {
        return null;
    }

    @Override
    public String username() {
        return null;
    }

    private boolean auth(final String username, final String pwd) {
        boolean authed = false;
        try {
            final String theUrl = String.format(Locale.US, "%s/o/token/", serverUrl);
            final String oauthCreds = String.format(Locale.US, "%s:", clientId);
            final String base64Encoded = Base64.encodeToString(oauthCreds.getBytes("UTF-8"), Base64.NO_WRAP);
            final String authHeader = String.format(Locale.US, "Basic %s", base64Encoded);
            Response response = HttpHandler.getInstance()
                    .postBlocking(theUrl,
                            String.format("grant_type=password&username=%s&password=%s",
                                    username, pwd), authHeader, HttpHandler.XML);

            if (response.isSuccessful()) {
                JSONObject responseJson = new JSONObject(response.body().string());
                saveAccessToken(responseJson.getString("access_token"));
                saveCredentials(username, pwd);
                authed = true;
            } else {
                logout();
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG,"JSON error trying to auth with exchange: " + e.getMessage());
        } catch (Exception e) {
            Log.e(LOG_TAG,"Error trying to auth with exchange: " + e.getMessage());
        }
        return authed;
    }

    private void saveCredentials(String username, String password) {
        SecureSharedPreferences.Editor editor = settings.edit();
        editor.putString(USERNAME, username);
        editor.putString(PWD, password);
        editor.commit();
    }

    private String getPassword() {
        SecureSharedPreferences settings = new SecureSharedPreferences(context);
        return settings.getString(PWD, null);
    }

    private void saveAccessToken(String accessToken) {
        SecureSharedPreferences.Editor editor = settings.edit();
        editor.putString(ACCESS_TOKEN, accessToken);
        editor.commit();
    }

    private void removeCredentials() {
        SecureSharedPreferences.Editor editor = settings.edit();
        editor.remove(USERNAME);
        editor.remove(PWD);
        editor.commit();
    }
}

/**
 * Copyright 2016 Boundless, http://boundlessgeo.com
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
package com.boundlessgeo.spatialconnect.services;


import android.content.Context;
import android.util.Log;

import com.boundlessgeo.spatialconnect.SpatialConnect;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import rx.subjects.BehaviorSubject;


public class SCAuthService extends SCService {

    private static final String LOG_TAG = SCAuthService.class.getSimpleName();
    private static Context context;
    public static BehaviorSubject<Integer> loginStatus = BehaviorSubject.create(0);
    public static final String AUTH_HEADER_NAME = "x-access-token";
    public static String accessToken;
    public static String email;
    public static String password;

    public SCAuthService(Context context) {
        this.context = context;
    }

    public void authenticate(String email, String password) {
        // TODO: save email and password in encrypted storage
        // probably want to use the KeyStore: https://developer.android.com/training/articles/keystore.html
        this.email = email;
        this.password = password;
        try {
            refreshToken();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logout() {
        // TODO: post to the API to logout and invalidate the auth token when it's implemented in the api
        SCAuthService.loginStatus.onNext(0);
    }

    /**
     * Interceptor to attach the auth token to every request if it isn't present.
     */
    static class AuthHeaderInterceptor implements Interceptor {
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Request originalRequest = chain.request();

            // skip if the request already has the auth header or if this request is to authenticate
            if (originalRequest.header(AUTH_HEADER_NAME) != null ||
                    originalRequest.toString().contains("authenticate")) {
                return chain.proceed(originalRequest);
            }

            if (accessToken == null) {
                getAccessToken();
            }
            else {
                Request updatedRequest = originalRequest.newBuilder()
                        .header(AUTH_HEADER_NAME, accessToken)
                        .build();
                return chain.proceed(updatedRequest);
            }
            return chain.proceed(originalRequest);
        }
    }


    /**
     * Authenticator to add new header to rejected request and retry it after refreshing the token.
     */
    static class SCAuthenticator implements Authenticator {
        @Override
        public Request authenticate(Route route, Response response) throws IOException {
            return response.request().newBuilder()
                    .addHeader(AUTH_HEADER_NAME, refreshToken())
                    .build();
        }
    }

    /**
     * Method that returns a new auth token from the api after authenticating.
     *
     * @return
     */
    private static String refreshToken() throws IOException {
        Log.d(LOG_TAG, "Refreshing auth token.");
        final String theUrl = SCConfigService.API_URL + "authenticate";
        SCNetworkService networkService = SpatialConnect.getInstance().getNetworkService();
        // TODO: get the email and password from encrypted storage
        // probably want to use the KeyStore: https://developer.android.com/training/articles/keystore.html
        if (email != null && password != null) {
            String response = networkService.post(
                    theUrl,
                    String.format("{\"email\": \"%s\", \"password\":\"%s\"}", email, password)
            );
            try {
                accessToken = new JSONObject(response).getString("token");
                if (accessToken != null) {
                    loginStatus.onNext(1);
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.w(LOG_TAG, "Cannot refresh token b/c login credentials are not available.");
        }
        return accessToken;
    }

    public static String getAccessToken() throws IOException {
        return accessToken != null ? accessToken : refreshToken();
    }

}

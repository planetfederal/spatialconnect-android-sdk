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
import com.boundlessgeo.spatialconnect.scutilities.HttpHandler;
import com.github.rtoshiro.secure.SecureSharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

public class SCAuthService extends SCService implements SCServiceLifecycle {

    private static final String LOG_TAG = SCAuthService.class.getSimpleName();
    private static final String SERVICE_NAME = "SC_AUTH_SERVICE";
    private static Context context;
    public static BehaviorSubject<Boolean> loginStatus = BehaviorSubject.create(false);
    public static final String AUTH_HEADER_NAME = "x-access-token";
    private static String accessToken;
    private static String password;
    private static SecureSharedPreferences settings;
    private static String username;

    public SCAuthService(Context context) {
        this.context = context;
        this.settings = new SecureSharedPreferences(context);
        this.username = getUsername();
        this.password = getPassword();
        this.accessToken = getAccessToken();
        if (this.accessToken != null) {
            loginStatus.onNext(true);
        }
    }

    public void authenticate(String username, String password) {
        auth(username, password);
    }

    public static void logout() {
        // TODO: post to the API to logout and invalidate the auth token when it's implemented in the api
        loginStatus.onNext(false);
    }

    /**
     * Interceptor to attach the auth token to every request if it isn't present.
     */
    public static class AuthHeaderInterceptor implements Interceptor {
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

    private  void auth(final String username, final String pwd) {
        final String theUrl = SCBackendService.backendUri + "/api/authenticate";
        HttpHandler.getInstance()
                .post(theUrl, String.format("{\"email\": \"%s\", \"password\":\"%s\"}", username, pwd))
                .subscribe(
                        new Action1<Response>() {
                            @Override
                            public void call(Response response) {
                                if (response.isSuccessful()) {
                                    try {
                                        accessToken = new JSONObject(response.body().string())
                                                .getJSONObject("result").getString("token");
                                        if (accessToken != null) {
                                            saveAccessToken(accessToken);
                                            saveCredentials(username, pwd);
                                            loginStatus.onNext(true);
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    loginStatus.onNext(false);
                                }
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                loginStatus.onNext(false);
                                Log.e(LOG_TAG, "something went wrong refreshing token: " + throwable.getMessage());
                            }
                        });
    }

    public static String getAccessToken() {
        return settings.getString(AUTH_HEADER_NAME, null);
    }

    public static void saveAccessToken(String accessToken) {
        SecureSharedPreferences.Editor editor = settings.edit();
        editor.putString(AUTH_HEADER_NAME, accessToken);
        editor.commit();
    }

    public void saveCredentials(String username, String password) {
        SecureSharedPreferences.Editor editor = settings.edit();
        editor.putString("username", username);
        editor.putString("password", password);
        editor.commit();
    }

    public static String getPassword() {
        SecureSharedPreferences settings = new SecureSharedPreferences(context);
        return settings.getString("password", null);
    }

    public static String getUsername() {
        SecureSharedPreferences settings = new SecureSharedPreferences(context);
        return settings.getString("username", null);
    }

    public Observable<Void> start() {
        super.start();
        SpatialConnect sc = SpatialConnect.getInstance();
        sc.serviceRunning(SCBackendService.serviceId()).subscribe(new Action1<SCServiceStatusEvent>() {
            @Override
            public void call(SCServiceStatusEvent scServiceStatusEvent) {
                String username = getUsername();
                String pwd = getPassword();
                if (username != null && pwd != null) {
                    auth(username, pwd);
                } else {
                    loginStatus.onNext(false);
                }

            }
        });
        return Observable.empty();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void resume() {
        super.resume();
    }

    @Override
    public void pause() {
        super.pause();
    }

    @Override
    public void startError() {
        super.startError();
    }

    public static String serviceId() {
        return SERVICE_NAME;
    }
}

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

import okhttp3.Response;
import rx.Observable;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

public class SCAuthService extends SCService implements SCServiceLifecycle {

    public enum SCAuthStatus {
        NOT_AUTHENTICATED(0),
        AUTHENTICATED(1),
        AUTHENTICATION_FAILED(2);

        private final int value;

        SCAuthStatus(final int v) {
            value = v;
        }

        static SCAuthStatus fromValue(int v) {
            for (SCAuthStatus scAuthStatus: SCAuthStatus.values()) {
                if (scAuthStatus.value == v) {
                    return  scAuthStatus;
                }
            }
            return null;
        }

        public int value() {
            return value;
        }
    }

    private static final String LOG_TAG = SCAuthService.class.getSimpleName();
    private static final String SERVICE_NAME = "SC_AUTH_SERVICE";
    private static final String JSON_WEB_TOKEN = "x-access-token";
    private static final String USERNAME = "username";
    private static final String PWD = "pwd";
    private Context context;
    private String jsonWebToken;
    private SecureSharedPreferences settings;
    private  BehaviorSubject<Integer> loginStatus;

    public SCAuthService(Context context) {
        this.context = context;
        this.settings = new SecureSharedPreferences(context);
        loginStatus = BehaviorSubject.create(SCAuthStatus.NOT_AUTHENTICATED.value());
    }

    public void authenticate(String username, String password) {
        auth(username, password);
    }

    public BehaviorSubject<Integer> getLoginStatus() {
        return loginStatus;
    }

    public String getAccessToken() {
        return settings.getString(JSON_WEB_TOKEN, null);
    }

    public void logout() {
        removeCredentials();
        loginStatus.onNext(SCAuthStatus.NOT_AUTHENTICATED.value());
    }

    public String getUsername() {
        SecureSharedPreferences settings = new SecureSharedPreferences(context);
        return settings.getString(USERNAME, null);
    }

    @Override
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
                    loginStatus.onNext(SCAuthStatus.NOT_AUTHENTICATED.value());
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

    @Override
    String getId() {
        return SERVICE_NAME;
    }

    private  void auth(final String username, final String pwd) {
        SpatialConnect sc = SpatialConnect.getInstance();
        SCBackendService bs = sc.getBackendService();
        final String theUrl = bs.backendUri + "/api/authenticate";
        HttpHandler.getInstance()
                .post(theUrl, String.format("{\"email\": \"%s\", \"password\":\"%s\"}", username, pwd))
                .subscribe(
                        new Action1<Response>() {
                            @Override
                            public void call(Response response) {
                                if (response.isSuccessful()) {
                                    try {
                                        jsonWebToken = new JSONObject(response.body().string())
                                                .getJSONObject("result").getString("token");
                                        if (jsonWebToken != null) {
                                            saveAccessToken(jsonWebToken);
                                            saveCredentials(username, pwd);
                                            loginStatus.onNext(SCAuthStatus.AUTHENTICATED.value());
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    logout();
                                    loginStatus.onNext(SCAuthStatus.AUTHENTICATION_FAILED.value());
                                }
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                loginStatus.onNext(SCAuthStatus.NOT_AUTHENTICATED.value());
                                Log.e(LOG_TAG, "something went wrong refreshing token: " + throwable.getMessage());
                            }
                        });
    }

    private void saveAccessToken(String accessToken) {
        SecureSharedPreferences.Editor editor = settings.edit();
        editor.putString(JSON_WEB_TOKEN, accessToken);
        editor.commit();
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

    private void removeCredentials() {
        SecureSharedPreferences.Editor editor = settings.edit();
        editor.remove(USERNAME);
        editor.remove(PWD);
        editor.commit();
    }

    public static String serviceId() {
        return SERVICE_NAME;
    }
}

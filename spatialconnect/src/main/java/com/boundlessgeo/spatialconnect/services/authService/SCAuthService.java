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
package com.boundlessgeo.spatialconnect.services.authService;

import android.content.Context;

import com.boundlessgeo.spatialconnect.services.SCService;
import com.boundlessgeo.spatialconnect.services.SCServiceLifecycle;
import com.boundlessgeo.spatialconnect.services.SCServiceStatus;

import rx.Observable;
import rx.Subscriber;
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

        public static SCAuthStatus fromValue(int v) {
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
    private  BehaviorSubject<Integer> loginStatus;
    private ISCAuth authMethod;

    public SCAuthService(Context context, ISCAuth authMethod) {
        loginStatus = BehaviorSubject.create(SCAuthStatus.NOT_AUTHENTICATED.value());
        this.authMethod = authMethod;
    }

    /**
     * sets the token and auth status in the library for the user and pass
     * @param username user's email address
     * @param password clear text password
     */
    public void authenticate(String username, String password) {
        boolean authed = authMethod.authenticate(username, password);
        if (authed) {
            loginStatus.onNext(SCAuthStatus.AUTHENTICATED.value());
        } else {
            authMethod.logout();
            loginStatus.onNext(SCAuthStatus.AUTHENTICATION_FAILED.value());
        }
    }

    /**
     * Observable that will send current status and will send updates as subscribed
     * @return BehaviorSubject<Integer>
     */
    public BehaviorSubject<Integer> getLoginStatus() {
        return loginStatus;
    }

    /**
     * JSONWebToken from auth server
     * @return String
     */
    public String getAccessToken() {
        return authMethod.xAccessToken();
    }

    /**
     * this will void the x-access-token
     */
    public void logout() {
        authMethod.logout();
        loginStatus.onNext(SCAuthStatus.NOT_AUTHENTICATED.value());
    }

    /**
     * The user's email address
     *
     * @return String
     */
    public String getUsername() {
        return authMethod.username();
    }

    @Override
    public Observable<SCServiceStatus> start() {

        return Observable.create(new Observable.OnSubscribe<SCServiceStatus>() {
            @Override
            public void call(Subscriber<? super SCServiceStatus> subscriber) {
                setStatus(SCServiceStatus.SC_SERVICE_STARTED);
                subscriber.onNext(getStatus());
                boolean authed = authMethod.authFromCache();
                if (authed) {
                    loginStatus.onNext(SCAuthStatus.AUTHENTICATED.value());
                } else {
                    loginStatus.onNext(SCAuthStatus.NOT_AUTHENTICATED.value());
                }
                setStatus(SCServiceStatus.SC_SERVICE_RUNNING);
                subscriber.onNext(getStatus());
                subscriber.onCompleted();
            }
        });
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
    public String getId() {
        return SERVICE_NAME;
    }

    public static String serviceId() {
        return SERVICE_NAME;
    }
}

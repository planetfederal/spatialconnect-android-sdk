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

import java.util.List;
import java.util.Map;

import rx.subjects.BehaviorSubject;

/**
 * Service that uses an implementation of ISCAuth to authenticate the user.
 */
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
     * this will void the JWT
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
    public boolean start(Map<String, SCService> deps) {
        boolean authed = authMethod.authFromCache();
        if (authed) {
            loginStatus.onNext(SCAuthStatus.AUTHENTICATED.value());
        } else {
            loginStatus.onNext(SCAuthStatus.NOT_AUTHENTICATED.value());
        }
        return super.start(deps);
    }

    @Override
    public String getId() {
        return SERVICE_NAME;
    }

    @Override
    public List<String> getRequires() {
        return null;
    }

    public static String serviceId() {
        return SERVICE_NAME;
    }

    /**
     * Returns the implementation of ISCAuth used by the SCAuthService.
     *
     * @return an implementation of ISCAuth
     */
    public ISCAuth getAuthMethod() {
        return authMethod;
    }
}

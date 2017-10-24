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

/**
 * Authentication implementation that requires no authentication
 */
public class NoAuth implements ISCAuth {

  @Override public boolean authFromCache() {
    // Always return true b/c we're not authenticating
    return true;
  }

  @Override public boolean authenticate(String username, String pwd) {
    // Always return true b/c we're not authenticating
    return true;
  }

  @Override
  public boolean refreshToken() {
    return false;
  }

  @Override public void logout() {
  }

  @Override public String xAccessToken() {
    return null;
  }

  @Override public String username() {
    return null;
  }
}

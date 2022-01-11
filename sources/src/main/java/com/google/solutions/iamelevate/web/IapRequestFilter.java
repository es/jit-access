//
// Copyright 2021 Google LLC
//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package com.google.solutions.iamelevate.web;

import com.google.auth.oauth2.TokenVerifier;
import com.google.common.base.Preconditions;
import com.google.solutions.iamelevate.core.adapters.DeviceInfo;
import com.google.solutions.iamelevate.core.adapters.LogAdapter;
import com.google.solutions.iamelevate.core.adapters.UserId;
import com.google.solutions.iamelevate.core.adapters.UserPrincipal;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.security.Principal;

/**
 * Verifies that requests have a valid IAP assertion, and makes the assertion available as
 * SecurityContext.
 */
@Dependent
@Provider
@Priority(Priorities.AUTHENTICATION)
public class IapRequestFilter implements ContainerRequestFilter {
  private static final String EVENT_AUTHENTICATE = "iap.authenticate";

  private static final String IAP_ISSUER_URL = "https://cloud.google.com/iap";
  private static final String IAP_ASSERTION_HEADER = "x-goog-iap-jwt-assertion";

  @Inject LogAdapter log;

  @Inject RuntimeEnvironment runtimeEnvironment;

  private UserPrincipal authenticateRequest(ContainerRequestContext requestContext) {
    //
    // Read IAP assertion header and validate it.
    //
    // NB. For AppEngine, we can derive the expected audience
    // from the project number and name.
    //
    String expectedAudience =
        String.format(
            "/projects/%s/apps/%s",
            this.runtimeEnvironment.getProjectNumber(), this.runtimeEnvironment.getProjectId());

    String assertion = requestContext.getHeaderString(IAP_ASSERTION_HEADER);
    if (assertion == null) {
      throw new ForbiddenException("IAP assertion missing, application must be accessed via IAP");
    }

    try {
      final var verifiedAssertion = new IapAssertion(
          TokenVerifier.newBuilder()
              .setAudience(expectedAudience)
              .setIssuer(IAP_ISSUER_URL)
              .build()
              .verify(assertion));

      //
      // Associate the token with the request so that controllers
      // can access it.
      //
      return new UserPrincipal() {
        @Override
        public String getName() {
          return getId().toString();
        }

        @Override
        public UserId getId() {
          return verifiedAssertion.getUserId();
        }

        @Override
        public DeviceInfo getDevice() {
          return verifiedAssertion.getDeviceInfo();
        }
      };
    } catch (TokenVerifier.VerificationException | IllegalArgumentException e) {
      throw new ForbiddenException("Invalid IAP assertion", e);
    }
  }

  public void filter(ContainerRequestContext requestContext) {
    Preconditions.checkNotNull(this.log, "log");
    Preconditions.checkNotNull(this.runtimeEnvironment, "runtimeEnvironment");

    var principal =
        this.runtimeEnvironment.getStaticPrincipal() == null
            ? authenticateRequest(requestContext)
            : this.runtimeEnvironment.getStaticPrincipal();

    this.log.setPrincipal(principal);

    requestContext.setSecurityContext(
        new SecurityContext() {
          @Override
          public Principal getUserPrincipal() {
            return principal;
          }

          @Override
          public boolean isUserInRole(String s) {
            return false;
          }

          @Override
          public boolean isSecure() {
            return true;
          }

          @Override
          public String getAuthenticationScheme() {
            return "IAP";
          }
        });

    this.log.newInfoEntry(EVENT_AUTHENTICATE, "Authenticated IAP principal").write();
  }
}

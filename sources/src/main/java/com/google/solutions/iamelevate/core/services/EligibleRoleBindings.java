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

package com.google.solutions.iamelevate.core.services;

import java.util.List;

/** Set of role bindings that a user has been found eligible for. */
public class EligibleRoleBindings {
  /** List of bindings. Might be incomplete if Warnings is non-empty. */
  private final List<RoleBinding> roleBindings;

  /** Non-fatal issues encountered. */
  private final List<String> warnings;

  public EligibleRoleBindings(List<RoleBinding> roleBindings, List<String> warnings) {
    this.roleBindings = roleBindings;
    this.warnings = warnings;
  }

  public List<RoleBinding> getRoleBindings() {
    return roleBindings;
  }

  public List<String> getWarnings() {
    return warnings;
  }
}

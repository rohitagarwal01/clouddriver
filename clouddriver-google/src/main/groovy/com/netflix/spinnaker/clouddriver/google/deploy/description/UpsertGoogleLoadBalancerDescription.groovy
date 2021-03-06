/*
 * Copyright 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.google.deploy.description

import com.netflix.spinnaker.clouddriver.google.model.loadbalancing.GoogleBackendService
import com.netflix.spinnaker.clouddriver.google.model.loadbalancing.GoogleHostRule
import com.netflix.spinnaker.clouddriver.google.model.loadbalancing.GoogleLoadBalancerType

class UpsertGoogleLoadBalancerDescription extends AbstractGoogleCredentialsDescription {
  // Common attributes.
  String loadBalancerName
  HealthCheck healthCheck
  List<String> instances // The local names of the instances.
  String ipAddress
  String ipProtocol
  String portRange
  String region
  String accountName

  // Http(s) attributes.
  String urlMapName
  GoogleBackendService defaultService
  List<GoogleHostRule> hostRules
  String certificate
  List<String> listenersToDelete
  /**
   * If we update an L7 and change the backend services, this field is the difference between the old and new.
   * We need this to update the instance metadata for the server groups that are no longer associated with
   * this L7.
   */
  List<GoogleBackendService> backendServiceDiff

  // ILB attributes.
  String network
  String subnet
  GoogleBackendService backendService
  List<String> ports

  GoogleLoadBalancerType loadBalancerType

  static class HealthCheck {
    Integer checkIntervalSec
    Integer healthyThreshold
    Integer unhealthyThreshold
    Integer port
    Integer timeoutSec
    String requestPath
  }
}

/*
 * Copyright 2014 Google, Inc.
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

package com.netflix.spinnaker.kato.gce.deploy.ops

import com.google.api.services.compute.Compute
import com.google.api.services.compute.model.Operation
import com.netflix.spinnaker.kato.data.task.Task
import com.netflix.spinnaker.kato.data.task.TaskRepository
import com.netflix.spinnaker.kato.gce.deploy.GCEResourceNotFoundException
import com.netflix.spinnaker.kato.gce.deploy.description.CreateGoogleNetworkLoadBalancerDescription
import com.netflix.spinnaker.kato.gce.deploy.description.DeleteGoogleNetworkLoadBalancerDescription
import com.netflix.spinnaker.kato.gce.security.GoogleCredentials
import spock.lang.Specification
import spock.lang.Subject

class DeleteGoogleNetworkLoadBalancerAtomicOperationUnitSpec extends Specification {
  private static final ACCOUNT_NAME = "auto"
  private static final PROJECT_NAME = "my_project"
  private static final NETWORK_LOAD_BALANCER_NAME = "default"
  private static final ZONE = "us-central1-b"
  private static final REGION = "us-central1"
  private static final FORWARDING_RULE_DELETE_OP_NAME = "delete-forwarding-rule"
  private static final TARGET_POOL_URL = "project/target-pool"
  private static final TARGET_POOL_NAME = "target-pool"
  private static final TARGET_POOL_DELETE_OP_NAME = "delete-target-pool"
  private static final HEALTH_CHECK_URL = "project/health-check"
  private static final HEALTH_CHECK_NAME = "health-check"
  private static final HEALTH_CHECK_DELETE_OP_NAME = "delete-health-check"

  def setupSpec() {
    TaskRepository.threadLocalTask.set(Mock(Task))
  }

  void "should delete a Network Load Balancer with health checks"() {
    setup:
      def computeMock = Mock(Compute)
      def regionOperations = Mock(Compute.RegionOperations)
      def forwardingRuleOperationGet = Mock(Compute.RegionOperations.Get)
      def targetPoolOperationGet = Mock(Compute.RegionOperations.Get)
      def globalOperations = Mock(Compute.GlobalOperations)
      def healthCheckOperationGet = Mock(Compute.GlobalOperations.Get)
      def zones = Mock(Compute.Zones)
      def getZoneRequest = Mock(Compute.Zones.Get)
      def forwardingRules = Mock(Compute.ForwardingRules)
      def forwardingRulesGet = Mock(Compute.ForwardingRules.Get)
      def forwardingRulesDelete = Mock(Compute.ForwardingRules.Delete)
      def forwardingRulesDeleteOp = new Operation(
          name: FORWARDING_RULE_DELETE_OP_NAME,
          status: "DONE")
      def forwardingRule = new com.google.api.services.compute.model.ForwardingRule(target: TARGET_POOL_URL)
      def targetPools = Mock(Compute.TargetPools)
      def targetPoolsGet = Mock(Compute.TargetPools.Get)
      def targetPoolsDelete = Mock(Compute.TargetPools.Delete)
      def targetPoolsDeleteOp = new Operation(
          name: TARGET_POOL_DELETE_OP_NAME,
          status: "DONE")
      def targetPool = new com.google.api.services.compute.model.TargetPool(healthChecks: [HEALTH_CHECK_URL])
      def healthChecks = Mock(Compute.HttpHealthChecks)
      def healthChecksDelete = Mock(Compute.HttpHealthChecks.Delete)
      def healthChecksDeleteOp = new Operation(
          name: HEALTH_CHECK_DELETE_OP_NAME,
          status: "DONE")
      def credentials = new GoogleCredentials(PROJECT_NAME, computeMock)
      def description = new DeleteGoogleNetworkLoadBalancerDescription(
          networkLoadBalancerName: NETWORK_LOAD_BALANCER_NAME,
          zone: ZONE,
          accountName: ACCOUNT_NAME,
          credentials: credentials)
      @Subject def operation = new DeleteGoogleNetworkLoadBalancerAtomicOperation(description)

    when:
      operation.operate([])

    then:
      1 * computeMock.zones() >> zones
      1 * zones.get(PROJECT_NAME, ZONE) >> getZoneRequest
      1 * getZoneRequest.execute() >> new com.google.api.services.compute.model.Zone().setRegion(REGION)
      2 * computeMock.forwardingRules() >> forwardingRules
      1 * forwardingRules.get(PROJECT_NAME, REGION, NETWORK_LOAD_BALANCER_NAME) >> forwardingRulesGet
      1 * forwardingRulesGet.execute() >> forwardingRule
      2 * computeMock.targetPools() >> targetPools
      1 * targetPools.get(PROJECT_NAME, REGION, TARGET_POOL_NAME) >> targetPoolsGet
      1 * targetPoolsGet.execute() >> targetPool
      1 * computeMock.httpHealthChecks() >> healthChecks
      1 * forwardingRules.delete(PROJECT_NAME, REGION, NETWORK_LOAD_BALANCER_NAME) >> forwardingRulesDelete
      1 * forwardingRulesDelete.execute() >> forwardingRulesDeleteOp
      1 * targetPools.delete(PROJECT_NAME, REGION, TARGET_POOL_NAME) >> targetPoolsDelete
      1 * targetPoolsDelete.execute() >> targetPoolsDeleteOp
      1 * healthChecks.delete(PROJECT_NAME, HEALTH_CHECK_NAME) >> healthChecksDelete
      1 * healthChecksDelete.execute() >> healthChecksDeleteOp
      2 * computeMock.regionOperations() >> regionOperations
      1 * regionOperations.get(PROJECT_NAME, REGION, FORWARDING_RULE_DELETE_OP_NAME) >> forwardingRuleOperationGet
      1 * forwardingRuleOperationGet.execute() >> forwardingRulesDeleteOp
      1 * regionOperations.get(PROJECT_NAME, REGION, TARGET_POOL_DELETE_OP_NAME) >> targetPoolOperationGet
      1 * targetPoolOperationGet.execute() >> targetPoolsDeleteOp
      1 * computeMock.globalOperations() >> globalOperations
      1 * globalOperations.get(PROJECT_NAME, HEALTH_CHECK_DELETE_OP_NAME) >> healthCheckOperationGet
      1 * healthCheckOperationGet.execute() >> healthChecksDeleteOp
  }

  void "should delete a Network Load Balancer even if it lacks any health checks"() {
    setup:
      def computeMock = Mock(Compute)
      def regionOperations = Mock(Compute.RegionOperations)
      def forwardingRuleOperationGet = Mock(Compute.RegionOperations.Get)
      def targetPoolOperationGet = Mock(Compute.RegionOperations.Get)
      def zones = Mock(Compute.Zones)
      def getZoneRequest = Mock(Compute.Zones.Get)
      def forwardingRules = Mock(Compute.ForwardingRules)
      def forwardingRulesGet = Mock(Compute.ForwardingRules.Get)
      def forwardingRulesDelete = Mock(Compute.ForwardingRules.Delete)
      def forwardingRulesDeleteOp = new Operation(
          name: FORWARDING_RULE_DELETE_OP_NAME,
          status: "DONE")
      def forwardingRule = new com.google.api.services.compute.model.ForwardingRule(target: TARGET_POOL_URL)
      def targetPools = Mock(Compute.TargetPools)
      def targetPoolsGet = Mock(Compute.TargetPools.Get)
      def targetPoolsDelete = Mock(Compute.TargetPools.Delete)
      def targetPoolsDeleteOp = new Operation(
          name: TARGET_POOL_DELETE_OP_NAME,
          status: "DONE")
      def targetPool = new com.google.api.services.compute.model.TargetPool()
      def credentials = new GoogleCredentials(PROJECT_NAME, computeMock)
      def description = new DeleteGoogleNetworkLoadBalancerDescription(
          networkLoadBalancerName: NETWORK_LOAD_BALANCER_NAME,
          zone: ZONE,
          accountName: ACCOUNT_NAME,
          credentials: credentials)
      @Subject def operation = new DeleteGoogleNetworkLoadBalancerAtomicOperation(description)

    when:
      operation.operate([])

    then:
      1 * computeMock.zones() >> zones
      1 * zones.get(PROJECT_NAME, ZONE) >> getZoneRequest
      1 * getZoneRequest.execute() >> new com.google.api.services.compute.model.Zone().setRegion(REGION)
      2 * computeMock.forwardingRules() >> forwardingRules
      1 * forwardingRules.get(PROJECT_NAME, REGION, NETWORK_LOAD_BALANCER_NAME) >> forwardingRulesGet
      1 * forwardingRulesGet.execute() >> forwardingRule
      2 * computeMock.targetPools() >> targetPools
      1 * targetPools.get(PROJECT_NAME, REGION, TARGET_POOL_NAME) >> targetPoolsGet
      1 * targetPoolsGet.execute() >> targetPool
      0 * computeMock.httpHealthChecks()
      1 * forwardingRules.delete(PROJECT_NAME, REGION, NETWORK_LOAD_BALANCER_NAME) >> forwardingRulesDelete
      1 * forwardingRulesDelete.execute() >> forwardingRulesDeleteOp
      1 * targetPools.delete(PROJECT_NAME, REGION, TARGET_POOL_NAME) >> targetPoolsDelete
      1 * targetPoolsDelete.execute() >> targetPoolsDeleteOp
      2 * computeMock.regionOperations() >> regionOperations
      1 * regionOperations.get(PROJECT_NAME, REGION, FORWARDING_RULE_DELETE_OP_NAME) >> forwardingRuleOperationGet
      1 * forwardingRuleOperationGet.execute() >> forwardingRulesDeleteOp
      1 * regionOperations.get(PROJECT_NAME, REGION, TARGET_POOL_DELETE_OP_NAME) >> targetPoolOperationGet
      1 * targetPoolOperationGet.execute() >> targetPoolsDeleteOp
  }

  void "should fail to delete a Network Load Balancer that does not exist"() {
    setup:
      def computeMock = Mock(Compute)
      def zones = Mock(Compute.Zones)
      def getZoneRequest = Mock(Compute.Zones.Get)
      def forwardingRules = Mock(Compute.ForwardingRules)
      def forwardingRulesGet = Mock(Compute.ForwardingRules.Get)
      def credentials = new GoogleCredentials(PROJECT_NAME, computeMock)
      def description = new DeleteGoogleNetworkLoadBalancerDescription(
          networkLoadBalancerName: NETWORK_LOAD_BALANCER_NAME,
          zone: ZONE,
          accountName: ACCOUNT_NAME,
          credentials: credentials)
      @Subject def operation = new DeleteGoogleNetworkLoadBalancerAtomicOperation(description)

    when:
      operation.operate([])

    then:
      1 * computeMock.zones() >> zones
      1 * zones.get(PROJECT_NAME, ZONE) >> getZoneRequest
      1 * getZoneRequest.execute() >> new com.google.api.services.compute.model.Zone().setRegion(REGION)
      1 * computeMock.forwardingRules() >> forwardingRules
      1 * forwardingRules.get(PROJECT_NAME, REGION, NETWORK_LOAD_BALANCER_NAME) >> forwardingRulesGet
      1 * forwardingRulesGet.execute() >> null
      thrown GCEResourceNotFoundException
  }

  void "should fail if failed to delete a resource"() {
    setup:
      def computeMock = Mock(Compute)
      def regionOperations = Mock(Compute.RegionOperations)
      def forwardingRuleOperationGet = Mock(Compute.RegionOperations.Get)
      def zones = Mock(Compute.Zones)
      def getZoneRequest = Mock(Compute.Zones.Get)
      def forwardingRules = Mock(Compute.ForwardingRules)
      def forwardingRulesGet = Mock(Compute.ForwardingRules.Get)
      def forwardingRulesDelete = Mock(Compute.ForwardingRules.Delete)
      def forwardingRulesPendingDeleteOp = new Operation(
          name: FORWARDING_RULE_DELETE_OP_NAME,
          status: "PENDING")
      def forwardingRulesFailingDeleteOp = new Operation(
          name: FORWARDING_RULE_DELETE_OP_NAME,
          status: "DONE",
          error: new Operation.Error(errors: [new Operation.Error.Errors(message: "error")]))
      def forwardingRule = new com.google.api.services.compute.model.ForwardingRule(target: TARGET_POOL_URL)
      def targetPools = Mock(Compute.TargetPools)
      def targetPoolsGet = Mock(Compute.TargetPools.Get)
      def targetPoolsDelete = Mock(Compute.TargetPools.Delete)
      def targetPoolsDeleteOp = new Operation(name: TARGET_POOL_DELETE_OP_NAME)
      def targetPool = new com.google.api.services.compute.model.TargetPool()
      def credentials = new GoogleCredentials(PROJECT_NAME, computeMock)
      def description = new DeleteGoogleNetworkLoadBalancerDescription(
          networkLoadBalancerName: NETWORK_LOAD_BALANCER_NAME,
          zone: ZONE,
          accountName: ACCOUNT_NAME,
          credentials: credentials)
      @Subject def operation = new DeleteGoogleNetworkLoadBalancerAtomicOperation(description)

    when:
      operation.operate([])

    then:
      1 * computeMock.zones() >> zones
      1 * zones.get(PROJECT_NAME, ZONE) >> getZoneRequest
      1 * getZoneRequest.execute() >> new com.google.api.services.compute.model.Zone().setRegion(REGION)
      2 * computeMock.forwardingRules() >> forwardingRules
      1 * forwardingRules.get(PROJECT_NAME, REGION, NETWORK_LOAD_BALANCER_NAME) >> forwardingRulesGet
      1 * forwardingRulesGet.execute() >> forwardingRule
      2 * computeMock.targetPools() >> targetPools
      1 * targetPools.get(PROJECT_NAME, REGION, TARGET_POOL_NAME) >> targetPoolsGet
      1 * targetPoolsGet.execute() >> targetPool
      1 * forwardingRules.delete(PROJECT_NAME, REGION, NETWORK_LOAD_BALANCER_NAME) >> forwardingRulesDelete
      1 * forwardingRulesDelete.execute() >> forwardingRulesPendingDeleteOp
      1 * targetPools.delete(PROJECT_NAME, REGION, TARGET_POOL_NAME) >> targetPoolsDelete
      1 * targetPoolsDelete.execute() >> targetPoolsDeleteOp
      1 * computeMock.regionOperations() >> regionOperations
      1 * regionOperations.get(PROJECT_NAME, REGION, FORWARDING_RULE_DELETE_OP_NAME) >> forwardingRuleOperationGet
      1 * forwardingRuleOperationGet.execute() >> forwardingRulesFailingDeleteOp
      thrown GCEResourceNotFoundException
  }

  void "should fail if timed out while deleting a resource"() {
    setup:
      def computeMock = Mock(Compute)
      def regionOperations = Mock(Compute.RegionOperations)
      def forwardingRuleOperationGet = Mock(Compute.RegionOperations.Get)
      def zones = Mock(Compute.Zones)
      def getZoneRequest = Mock(Compute.Zones.Get)
      def forwardingRules = Mock(Compute.ForwardingRules)
      def forwardingRulesGet = Mock(Compute.ForwardingRules.Get)
      def forwardingRulesDelete = Mock(Compute.ForwardingRules.Delete)
      def forwardingRulesPendingDeleteOp = new Operation(
          name: FORWARDING_RULE_DELETE_OP_NAME,
          status: "PENDING")
      def forwardingRule = new com.google.api.services.compute.model.ForwardingRule(target: TARGET_POOL_URL)
      def targetPools = Mock(Compute.TargetPools)
      def targetPoolsGet = Mock(Compute.TargetPools.Get)
      def targetPoolsDelete = Mock(Compute.TargetPools.Delete)
      def targetPoolsDeleteOp = new Operation(name: TARGET_POOL_DELETE_OP_NAME, status: "PENDING")
      def targetPool = new com.google.api.services.compute.model.TargetPool()
      def credentials = new GoogleCredentials(PROJECT_NAME, computeMock)
      def description = new DeleteGoogleNetworkLoadBalancerDescription(
          deleteOperationTimeoutSeconds: 0,
          networkLoadBalancerName: NETWORK_LOAD_BALANCER_NAME,
          zone: ZONE,
          accountName: ACCOUNT_NAME,
          credentials: credentials)
      @Subject def operation = new DeleteGoogleNetworkLoadBalancerAtomicOperation(description)

    when:
      operation.operate([])

    then:
      1 * computeMock.zones() >> zones
      1 * zones.get(PROJECT_NAME, ZONE) >> getZoneRequest
      1 * getZoneRequest.execute() >> new com.google.api.services.compute.model.Zone().setRegion(REGION)
      2 * computeMock.forwardingRules() >> forwardingRules
      1 * forwardingRules.get(PROJECT_NAME, REGION, NETWORK_LOAD_BALANCER_NAME) >> forwardingRulesGet
      1 * forwardingRulesGet.execute() >> forwardingRule
      2 * computeMock.targetPools() >> targetPools
      1 * targetPools.get(PROJECT_NAME, REGION, TARGET_POOL_NAME) >> targetPoolsGet
      1 * targetPoolsGet.execute() >> targetPool
      1 * forwardingRules.delete(PROJECT_NAME, REGION, NETWORK_LOAD_BALANCER_NAME) >> forwardingRulesDelete
      1 * forwardingRulesDelete.execute() >> forwardingRulesPendingDeleteOp
      1 * targetPools.delete(PROJECT_NAME, REGION, TARGET_POOL_NAME) >> targetPoolsDelete
      1 * targetPoolsDelete.execute() >> targetPoolsDeleteOp
      1 * computeMock.regionOperations() >> regionOperations
      1 * regionOperations.get(PROJECT_NAME, REGION, FORWARDING_RULE_DELETE_OP_NAME) >> forwardingRuleOperationGet
      1 * forwardingRuleOperationGet.execute() >> forwardingRulesPendingDeleteOp
      thrown GCEResourceNotFoundException
  }
}
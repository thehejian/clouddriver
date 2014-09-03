/*
 * Copyright 2014 Netflix, Inc.
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

package com.netflix.spinnaker.oort.data.aws

import com.netflix.spinnaker.oort.data.aws.cachers.AbstractInfrastructureCachingAgent
import com.netflix.spinnaker.oort.data.aws.cachers.AtlasHealthCachingAgent
import com.netflix.spinnaker.oort.config.atlas.AtlasHealthApi
import com.netflix.spinnaker.oort.model.atlas.AtlasInstanceHealth
import com.netflix.spinnaker.oort.security.aws.OortNetflixAmazonCredentials
import spock.lang.Shared

class AtlasHealthCachingAgentSpec extends AbstractCachingAgentSpec {

  @Shared
  AtlasHealthApi atlasHealthApi

  @Override
  AbstractInfrastructureCachingAgent getCachingAgent() {
    def account = Mock(OortNetflixAmazonCredentials)
    account.getName() >> ACCOUNT
    account.getAtlasHealth() >> 'http://atlas'
    atlasHealthApi = Mock(AtlasHealthApi)
    new AtlasHealthCachingAgent(account, REGION, atlasHealthApi)
  }

  void "load new health when new ones are available, remove missing ones, and do nothing when theres nothing new to process"() {
    setup:
    def health = new AtlasInstanceHealth(instanceId: "i-12345")
    def key = Keys.getInstanceHealthKey(health.instanceId, ACCOUNT, REGION, AtlasHealthCachingAgent.PROVIDER_NAME)

    when:
    agent.load()

    then:
    1 * atlasHealthApi.loadInstanceHealth() >> [health]
    1 * cacheService.put(key, health)

    when:
    agent.load()

    then:
    1 * atlasHealthApi.loadInstanceHealth() >> []
    1 * cacheService.free(key)

    when:
    agent.load()

    then:
    1 * atlasHealthApi.loadInstanceHealth() >> []
    0 * cacheService.free(_)
  }
}

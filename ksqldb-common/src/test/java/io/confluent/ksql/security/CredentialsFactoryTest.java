/*
 * Copyright 2024 Confluent Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.confluent.ksql.security;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.confluent.ksql.security.oauth.OAuthBearerCredentials;
import io.confluent.ksql.security.oauth.StaticTokenCredentials;
import org.junit.Test;

public class CredentialsFactoryTest {
  @Test
  public void testCreateBasicCredentials() {
    Credentials credentials = CredentialsFactory.createCredentials(AuthType.BASIC);
    assertInstanceOf(BasicCredentials.class, credentials,
        "Should return an instance of BasicCredentials");
  }

  @Test
  public void testCreateOAuthBearerCredentials() {
    Credentials credentials = CredentialsFactory.createCredentials(AuthType.OAUTHBEARER);
    assertInstanceOf(OAuthBearerCredentials.class, credentials,
        "Should return an instance of OAuthBearerCredentials");
  }

  @Test
  public void testCreateStaticTokenCredentials() {
    Credentials credentials = CredentialsFactory.createCredentials(AuthType.STATIC_TOKEN);
    assertInstanceOf(StaticTokenCredentials.class, credentials,
        "Should return an instance of StaticTokenCredentials");
  }

  @Test
  public void testCreateCredentialsWithUnsupportedAuthType() {
    Credentials credentials = CredentialsFactory.createCredentials(AuthType.NONE);
    assertNull(credentials, "Should return null for unsupported AuthType");
  }
}
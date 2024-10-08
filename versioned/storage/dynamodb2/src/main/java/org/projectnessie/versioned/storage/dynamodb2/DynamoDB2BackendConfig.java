/*
 * Copyright (C) 2024 Dremio
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
package org.projectnessie.versioned.storage.dynamodb2;

import java.util.Optional;
import org.immutables.value.Value;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Value.Immutable
public interface DynamoDB2BackendConfig {
  DynamoDbClient client();

  Optional<String> tablePrefix();

  static ImmutableDynamoDB2BackendConfig.Builder builder() {
    return ImmutableDynamoDB2BackendConfig.builder();
  }
}

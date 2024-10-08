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
package org.projectnessie.catalog.files.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import org.immutables.value.Value;
import org.projectnessie.nessie.docgen.annotations.ConfigDocs.ConfigItem;
import org.projectnessie.nessie.docgen.annotations.ConfigDocs.ConfigPropertyName;
import org.projectnessie.nessie.immutables.NessieImmutable;

@NessieImmutable
@JsonSerialize(as = ImmutableAdlsOptions.class)
@JsonDeserialize(as = ImmutableAdlsOptions.class)
public interface AdlsOptions {

  /** Override the default read block size used when writing to ADLS. */
  OptionalInt readBlockSize();

  /** Override the default write block size used when writing to ADLS. */
  OptionalLong writeBlockSize();

  /**
   * Default file-system configuration, default/fallback values for all file-systems are taken from
   * this one.
   */
  @ConfigItem(section = "default-options")
  Optional<AdlsFileSystemOptions> defaultOptions();

  /** ADLS file-system specific options, per file system name. */
  @ConfigItem(section = "buckets")
  @ConfigPropertyName("filesystem-name")
  Map<String, AdlsNamedFileSystemOptions> fileSystems();

  default AdlsOptions validate() {
    boolean hasDefaultEndpoint = defaultOptions().map(o -> o.endpoint().isPresent()).orElse(false);
    if (!hasDefaultEndpoint && !fileSystems().isEmpty()) {
      List<String> missing =
          fileSystems().entrySet().stream()
              .filter(e -> e.getValue().endpoint().isEmpty())
              .map(Map.Entry::getKey)
              .sorted()
              .collect(Collectors.toList());
      if (!missing.isEmpty()) {
        String msg =
            missing.stream()
                .collect(
                    Collectors.joining(
                        "', '",
                        "Mandatory ADLS endpoint is not configured for file system '",
                        "'."));
        throw new IllegalStateException(msg);
      }
    }
    return this;
  }

  default AdlsFileSystemOptions effectiveOptionsForFileSystem(Optional<String> filesystemName) {
    AdlsFileSystemOptions defaultOptions =
        defaultOptions()
            .map(AdlsFileSystemOptions.class::cast)
            .orElse(AdlsNamedFileSystemOptions.FALLBACK);
    if (filesystemName.isEmpty()) {
      return defaultOptions;
    }

    AdlsFileSystemOptions specific = fileSystems().get(filesystemName.get());

    if (specific == null) {
      return defaultOptions;
    }

    return ImmutableAdlsNamedFileSystemOptions.builder()
        .from(defaultOptions)
        .from(specific)
        .build();
  }

  @Value.Check
  default AdlsOptions normalizeBuckets() {
    Map<String, AdlsNamedFileSystemOptions> fileSystems = new HashMap<>();
    for (String fileSystemName : fileSystems().keySet()) {
      AdlsNamedFileSystemOptions options = fileSystems().get(fileSystemName);
      if (options.name().isPresent()) {
        fileSystemName = options.name().get();
      } else {
        options =
            ImmutableAdlsNamedFileSystemOptions.builder()
                .from(options)
                .name(fileSystemName)
                .build();
      }
      if (fileSystems.put(fileSystemName, options) != null) {
        throw new IllegalArgumentException(
            "Duplicate ADLS filesystem name '"
                + fileSystemName
                + "', check your ADLS file system configurations");
      }
    }
    if (fileSystems.equals(fileSystems())) {
      return this;
    }
    return ImmutableAdlsOptions.builder()
        .from(this)
        .defaultOptions(defaultOptions())
        .fileSystems(fileSystems)
        .build();
  }

  @Value.NonAttribute
  @JsonIgnore
  default AdlsOptions deepClone() {
    ImmutableAdlsOptions.Builder b =
        ImmutableAdlsOptions.builder().from(this).fileSystems(Map.of());
    defaultOptions().ifPresent(v -> b.defaultOptions(v.deepClone()));
    fileSystems().forEach((n, v) -> b.putFileSystem(n, v.deepClone()));
    return b.build();
  }
}

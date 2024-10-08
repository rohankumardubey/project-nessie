/*
 * Copyright (C) 2022 Dremio
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
package org.projectnessie.versioned.storage.cache;

import static org.projectnessie.versioned.storage.common.persist.ObjId.zeroLengthObjId;
import static org.projectnessie.versioned.storage.common.persist.Reference.reference;

import jakarta.annotation.Nonnull;
import org.projectnessie.versioned.storage.common.persist.Backend;
import org.projectnessie.versioned.storage.common.persist.Obj;
import org.projectnessie.versioned.storage.common.persist.ObjId;
import org.projectnessie.versioned.storage.common.persist.ObjType;
import org.projectnessie.versioned.storage.common.persist.Persist;
import org.projectnessie.versioned.storage.common.persist.Reference;

/**
 * Provides the cache primitives for a caching {@link Persist} facade, suitable for multiple
 * repositories. It is adviseable to have one {@link CacheBackend} per {@link Backend}.
 */
public interface CacheBackend {
  /**
   * Special sentinel reference instance to indicate that a referenc object has been marked as "not
   * found". This object is only for cache-internal purposes.
   */
  Reference NON_EXISTENT_REFERENCE_SENTINEL =
      reference("NON_EXISTENT", zeroLengthObjId(), false, -1L, null);

  /**
   * Special sentinel object instance to indicate that an object has been marked as "not found".
   * This object is only for cache-internal purposes.
   */
  Obj NOT_FOUND_OBJ_SENTINEL =
      new Obj() {
        @Override
        public ObjType type() {
          throw new UnsupportedOperationException();
        }

        @Override
        public ObjId id() {
          throw new UnsupportedOperationException();
        }

        @Override
        public Obj withReferenced(long referenced) {
          throw new UnsupportedOperationException();
        }
      };

  /**
   * Returns the {@link Obj} for the given {@link ObjId id}.
   *
   * @return One of these alternatives: the cached object if present, the {@link
   *     CacheBackend#NOT_FOUND_OBJ_SENTINEL} indicating that the object does <em>not</em> exist as
   *     previously marked via {@link #putNegative(String, ObjId, ObjType)}, or {@code null}.
   */
  Obj get(@Nonnull String repositoryId, @Nonnull ObjId id);

  /**
   * Adds the given object to the local cache and sends a cache-invalidation message to Nessie
   * peers.
   */
  void put(@Nonnull String repositoryId, @Nonnull Obj obj);

  /** Adds the given object only to the local cache, does not send a cache-invalidation message. */
  void putLocal(@Nonnull String repositoryId, @Nonnull Obj obj);

  /**
   * Record the "not found" sentinel for the given {@link ObjId id} and {@link ObjType type}.
   * Behaves like {@link #remove(String, ObjId)}, if {@code type} is {@code null}.
   */
  void putNegative(@Nonnull String repositoryId, @Nonnull ObjId id, @Nonnull ObjType type);

  void remove(@Nonnull String repositoryId, @Nonnull ObjId id);

  void clear(@Nonnull String repositoryId);

  Persist wrap(@Nonnull Persist persist);

  Reference getReference(@Nonnull String repositoryId, @Nonnull String name);

  void removeReference(@Nonnull String repositoryId, @Nonnull String name);

  /**
   * Adds the given reference to the local cache and sends a cache-invalidation message to Nessie
   * peers.
   */
  void putReference(@Nonnull String repositoryId, @Nonnull Reference r);

  /**
   * Adds the given reference only to the local cache, does not send a cache-invalidation message.
   */
  void putReferenceLocal(@Nonnull String repositoryId, @Nonnull Reference r);

  void putReferenceNegative(@Nonnull String repositoryId, @Nonnull String name);

  static CacheBackend noopCacheBackend() {
    return NoopCacheBackend.INSTANCE;
  }
}

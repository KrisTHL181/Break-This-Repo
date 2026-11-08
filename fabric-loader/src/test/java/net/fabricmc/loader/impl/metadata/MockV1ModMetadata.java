/*
 * Copyright 2016 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.loader.impl.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModEnvironment;

public class MockV1ModMetadata {
	public static Builder builder(String id, Version version) {
		return new Builder(id, version).setName(id.toUpperCase());
	}

	public static Builder builder(String id, String version) {
		try {
			return new Builder(id, Version.parse(version)).setName(id.toUpperCase());
		} catch (VersionParsingException e) {
			throw new RuntimeException(e);
		}
	}

	public static class Builder {
		private final String id;
		private final Version version;
		private Collection<String> provides = new ArrayList<>();
		private ModEnvironment environment = ModEnvironment.UNIVERSAL;
		private Collection<ModDependency> dependencies = new ArrayList<>();
		private Collection<Builder> nested = new ArrayList<>();
		private String name = null;

		private Builder(String id, Version version) {
			this.id = id;
			this.version = version;
			this.name = id.toUpperCase();
		}

		public Builder addProvides(String provides) {
			this.provides.add(provides);
			return this;
		}

		public Builder setProvides(Collection<String> provides) {
			this.provides = provides;
			return this;
		}

		public Builder setEnvironment(ModEnvironment environment) {
			this.environment = environment;
			return this;
		}

		public Builder addDependency(ModDependency dependency) {
			this.dependencies.add(dependency);
			return this;
		}

		public Builder setDependencies(Collection<ModDependency> dependencies) {
			this.dependencies = dependencies;
			return this;
		}

		public Builder addNestedMod(Builder nested) {
			this.nested.add(nested);
			return this;
		}

		public Collection<Builder> getNestedMods() {
			return nested;
		}

		public Builder setNestedMods(Collection<Builder> nested) {
			this.nested = nested;
			return this;
		}

		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		public LoaderModMetadata build() {
			return new V1ModMetadata(id, version, provides, environment, Collections.emptyMap(), Collections.emptyList(),
					Collections.emptyList(), null, dependencies, false, name, null,
					Collections.emptyList(), Collections.emptyList(), null, Collections.emptyList(), null,
					Collections.emptyMap(), Collections.emptyMap());
		}
	}
}

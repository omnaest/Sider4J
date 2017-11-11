/*

	Copyright 2017 Danny Kunz

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.


*/
package org.omnaest.metabolics.sider;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.omnaest.metabolics.sider.domain.Compound;
import org.omnaest.metabolics.sider.domain.SideEffect;
import org.omnaest.metabolics.sider.domain.SiderDataSet;
import org.omnaest.metabolics.sider.domain.raw.RawCompound;
import org.omnaest.utils.MapUtils;
import org.omnaest.utils.csv.CSVUtils;

public class SiderUtils
{
	public static SiderDataSet getInstance(File siderSideEffectsFile, File stitchChemicalsFile)
	{
		Map<String, Set<String>> compoundIdToSideEffect = loadSideEffects(siderSideEffectsFile);
		Map<String, List<String>> sideEffectToCompoundId = MapUtils.invertMultiValue(compoundIdToSideEffect);
		Map<String, RawCompound> compoundIdToCompound = loadCompounds(compoundIdToSideEffect.keySet(), stitchChemicalsFile);

		return new SiderDataSet()
		{

			@Override
			public Stream<SideEffect> getSideEffects()
			{
				return sideEffectToCompoundId	.entrySet()
												.stream()
												.map(entry ->
												{
													List<String> compoundIds = entry.getValue();
													String sideEffect = entry.getKey();
													return this.createSideEffect(sideEffect, compoundIds);
												});
			}

			private SideEffect createSideEffect(String sideEffect, List<String> compoundIds)
			{
				return new SideEffect()
				{
					@Override
					public Stream<Compound> getRelatedCompounds()
					{

						return compoundIds	.stream()
											.map(compoundId -> createCompound(compoundId));
					}

					@Override
					public String getName()
					{
						return sideEffect;
					}

					@Override
					public boolean matches(String regex)
					{
						return Pattern	.compile(regex, Pattern.CASE_INSENSITIVE)
										.matcher(this.getName())
										.matches();
					}
				};
			}

			private Compound createCompound(String compoundId)
			{
				return new Compound()
				{
					@Override
					public String getId()
					{
						return compoundId;
					}

					@Override
					public String getName()
					{
						RawCompound rawCompound = compoundIdToCompound.get(this.getId());
						return rawCompound != null ? rawCompound.getName() : this.getId();
					}

					@Override
					public Stream<SideEffect> getSideEffects()
					{
						return compoundIdToSideEffect	.get(this.getId())
														.stream()
														.map(sideEffect -> createSideEffect(sideEffect, sideEffectToCompoundId.get(sideEffect)));
					}

					@Override
					public boolean matches(String regex)
					{
						Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
						return pattern	.matcher(this.getName())
										.matches()
								|| pattern	.matcher(this.getId())
											.matches();
					}
				};
			}

			@Override
			public Stream<Compound> getCompounds()
			{
				return compoundIdToCompound	.keySet()
											.stream()
											.map(compoundId -> this.createCompound(compoundId));
			}
		};
	}

	private static ConcurrentHashMap<String, RawCompound> loadCompounds(Set<String> compoundIds, File stitchChemicalsFile)
	{
		ConcurrentHashMap<String, RawCompound> retmap = new ConcurrentHashMap<>();

		try
		{
			retmap.putAll(CSVUtils	.parse()
									.from(stitchChemicalsFile)
									.withEncoding(StandardCharsets.UTF_8)
									.withFormat(CSVFormat.TDF.withFirstRecordAsHeader())
									.enableStreaming()
									.get()
									.map(row ->
									{
										String id = row.get("chemical");
										String name = row.get("name");
										return new RawCompound(id, name);
									})
									.filter(compound -> compoundIds.contains(compound.getId()))
									.collect(Collectors.toMap(compound -> compound.getId(), compound -> compound)));

		} catch (IOException e)
		{
			throw new IllegalStateException(e);
		}

		return retmap;
	}

	private static ConcurrentHashMap<String, Set<String>> loadSideEffects(File siderSideEffectsFile)
	{
		ConcurrentHashMap<String, Set<String>> retmap = new ConcurrentHashMap<>();
		try
		{
			CSVUtils.parse(siderSideEffectsFile, CSVFormat.TDF, StandardCharsets.UTF_8)
					.forEach(row ->
					{
						retmap	.computeIfAbsent(row.get("0"), id -> new LinkedHashSet<>())
								.add(row.get("5"));
					});

		} catch (IOException e)
		{
			throw new IllegalStateException(e);
		}

		return retmap;
	}
}

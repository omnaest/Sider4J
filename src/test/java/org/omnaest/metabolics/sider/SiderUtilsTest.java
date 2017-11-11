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

import org.junit.Test;
import org.omnaest.metabolics.sider.domain.SiderDataSet;

public class SiderUtilsTest
{

	@Test
	public void testGetInstance() throws Exception
	{
		File siderSideEffectsFile = new File("C:/Z/databases/sider/meddra_all_se.tsv");
		File stitchChemicalsFile = new File("C:/Z/databases/stitch/4.0/partition/CID100.tsv");
		SiderDataSet siderDataSet = SiderUtils.getInstance(siderSideEffectsFile, stitchChemicalsFile);
		siderDataSet.getCompounds()
					.filter(compound -> compound.matches(".*lactate.*"))
					.peek(compound -> System.out.println(compound.getName()))
					.flatMap(compound -> compound.getSideEffects())
					.map(sideEffect -> "  " + sideEffect.getName())
					.forEach(System.out::println);

		siderDataSet.getCompounds()
					.filter(compound -> compound.matches(".*acetate.*"))
					.peek(compound -> System.out.println(compound.getName()))
					.flatMap(compound -> compound.getSideEffects())
					.map(sideEffect -> "  " + sideEffect.getName())
					.forEach(System.out::println);

		siderDataSet.getSideEffects()
					.filter(sideEffect -> sideEffect.matches(".*sweat.*"))
					.forEach(sideEffect ->
					{
						System.out.println(sideEffect.getName());
						sideEffect	.getRelatedCompounds()
									.map(compound -> compound.getName())
									.forEach(name ->
									{
										System.out.println("  " + name);
									});

					});
		siderDataSet.getSideEffects()
					.filter(sideEffect -> sideEffect.matches(".*hyperhidrosis.*"))
					.forEach(sideEffect ->
					{
						System.out.println(sideEffect.getName());
						sideEffect	.getRelatedCompounds()
									.map(compound -> compound.getName())
									.forEach(name ->
									{
										System.out.println("  " + name);
									});

					});
	}

}

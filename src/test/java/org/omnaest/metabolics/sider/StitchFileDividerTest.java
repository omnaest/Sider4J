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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.csv.CSVFormat;
import org.junit.Test;
import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.csv.CSVUtils;
import org.omnaest.utils.csv.CSVUtils.SerializationOutput;
import org.omnaest.utils.element.ListenableElement;

public class StitchFileDividerTest
{
	public static interface FileHandler extends Consumer<Map<String, String>>
	{
		public void close();

		public String getPrefix();
	}

	@Test
	public void test() throws FileNotFoundException, IOException
	{
		File baseDir = new File("C:/Z/databases/stitch/4.0");
		File stitchChemicalsFile = new File(baseDir, "chemicals.v4.0.tsv");

		Function<String, String> prefixExtractor = id -> id.substring(0, 6);

		AtomicReference<FileHandler> fileHandler = new AtomicReference<>();
		ListenableElement<String> currentPrefix = new ListenableElement<>();
		currentPrefix.registerOnChange(change ->
		{
			this.closeFileHandler(fileHandler);
			fileHandler.set(this.createConsumer(change.getNext(), baseDir));
		});

		CSVUtils.parse()
				.from(stitchChemicalsFile)
				.withEncoding(StandardCharsets.UTF_8)
				.withFormat(CSVFormat.TDF.withFirstRecordAsHeader())
				.enableStreaming()
				.get()
				.peek(row -> System.out.println(JSONHelper.prettyPrint(row)))
				//.limit(10)
				.forEach(row ->
				{
					String id = row.get("chemical");

					String prefix = prefixExtractor.apply(id);
					currentPrefix.set(prefix);

					fileHandler	.get()
								.accept(row);
				});

		this.closeFileHandler(fileHandler);
	}

	private void closeFileHandler(AtomicReference<FileHandler> fileHandler)
	{
		Optional.ofNullable(fileHandler.get())
				.ifPresent(handler -> handler.close());
	}

	private FileHandler createConsumer(String prefix, File baseDir)
	{
		File file = new File(baseDir, "partition/" + prefix + ".tsv");
		List<Map<String, String>> data = new ArrayList<>();
		return new FileHandler()
		{

			@Override
			public void accept(Map<String, String> row)
			{
				data.add(row);
			}

			@Override
			public void close()
			{
				try
				{
					SerializationOutput serializationOutput = CSVUtils.serialize(data.stream(), CSVFormat.TDF);
					serializationOutput.writeTo(file);
					data.clear();
				} catch (IOException e)
				{
					throw new RuntimeException(e);
				}
			}

			@Override
			public String getPrefix()
			{
				return prefix;
			}
		};
	}
}

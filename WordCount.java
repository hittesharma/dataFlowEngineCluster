/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.examples.wordcount;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.examples.wordcount.util.WordCountData;
import org.apache.flink.util.Collector;
import org.apache.flink.util.Preconditions;

/**
 * Implements the "WordCount" program that computes a simple word occurrence
 * histogram over text files in a streaming fashion.
 *
 * <p>The input is a plain text file with lines separated by newline characters.
 *
 * <p>Usage: <code>WordCount --input &lt;path&gt; --output &lt;path&gt;</code><br>
 * If no parameters are provided, the program is run with default data from
 * {@link WordCountData}.
 *
 * <p>This example shows how to:
 * <ul>
 * <li>write a simple Flink Streaming program,
 * <li>use tuple data types,
 * <li>write and use user-defined functions.
 * </ul>
 */
public class WordCount {

	// *************************************************************************
	// PROGRAM
	// *************************************************************************

	public static void main(String[] args) throws Exception {

		// Checking input parameters
		final ParameterTool params = ParameterTool.fromArgs(args);

		// set up the execution environment
		final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

		// make parameters available in the web interface
		env.getConfig().setGlobalJobParameters(params);

		// get input data
		DataSet<String> text = null;
		if (params.has("input")) {
			// union all the inputs from text files
			text = env.readTextFile(params.get("input"));
			Preconditions.checkNotNull(text, "Input DataStream should not be null.");
		} else {
			System.out.println("Executing WordCount example with default input data set.");
			System.out.println("Use --input to specify file input.");
			// get default test text data
			text = env.fromElements(WordCountData.WORDS);
		}

		DataSet<Tuple2<String, Integer>> counts =
				// split up the lines in pairs (2-tuples) containing: (word,1)
				text.flatMap(new Tokenizer())
						.groupBy(0)
						.sum(1)
						.sortPartition(1, Order.DESCENDING).setParallelism(1);

		// emit result
		if (params.has("output")) {
			counts.writeAsCsv(params.get("output"), FileSystem.WriteMode.OVERWRITE);
		} else {
			System.out.println("Printing result to stdout. Use --output to specify output path.");
			counts.print();
		}
		// execute program
		env.execute("Streaming WordCount");
	}

	// *************************************************************************
	// USER FUNCTIONS
	// *************************************************************************

	/**
	 * Implements the string tokenizer that splits sentences into words as a
	 * user-defined FlatMapFunction. The function takes a line (String) and
	 * splits it into multiple pairs in the form of "(word,1)" ({@code Tuple2<String,
	 * Integer>}).
	 */
	public static final class Tokenizer implements FlatMapFunction<String, Tuple2<String, Integer>> {

		@Override
		public void flatMap(String value, Collector<Tuple2<String, Integer>> out) {
			// normalize and split the line
			String[] tokens = value.toLowerCase().split("\\W+");

			// emit the pairs
			for (String token : tokens) {
				// Replace all punctiation in a token
				token.replaceAll("\\p{Punct}+", "");
				// Only match  AlphaNum chars
				if (token.matches("\\p{Alpha}+")) {
					out.collect(new Tuple2<>(token, 1));
				}
			}
		}
	}
}

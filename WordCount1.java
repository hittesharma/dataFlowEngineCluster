package org.apache.flink.streaming.examples.wordcount;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.streaming.examples.wordcount.util.WordCountData;
import org.apache.flink.util.Collector;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Preconditions;
import org.apache.flink.api.java.DataSet;

// ****************
 *'WordCount' program that computes a simple word occurrence histogram over text files in a stream.
 * The input is a plain text file with lines separated by newline characters.
 * If no parameters are provided, the program is run with default data from
// ****************
public class WordCount {
	public static void main(String[] args) throws Exception {
		//Checking input
		final ParameterTool params = ParameterTool.fromArgs(args);
		//setting the execution environment
		final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
		env.getConfig().setGlobalJobParameters(params);

		// getting input
		DataSet<String> text = null;
		if (params.has("input")) {
			// collate all inputs from text files
			text = env.readTextFile(params.get("input"));
			Preconditions.checkNotNull(text, "Input DataStream should not be empty.");
		} else {
			System.out.println("Executing WordCount example with default input data set.");
			System.out.println("Use --input to specify file input.");
			text = env.fromElements(WordCountData.WORDS);
		}

		DataSet<Tuple2<String, Integer>> counts =
				//split up the lines in pairs
				text.flatMap(new Tokenizer())
						.groupBy(0)
						.sum(1)
						.sortPartition(1, Order.DESCENDING).setParallelism(1);

		//result
		if (params.has("output")) {
			counts.writeAsCsv(params.get("output"), FileSystem.WriteMode.OVERWRITE);
		} else {
			System.out.println("Printing result to stdout. Use --output to specify output path.");
			counts.print();
		}
		env.execute("Streaming WordCount");
	}

	// ****************
	// USER FUNCTION
	// ****************

/**
 Implements the string tokenizer
*/
	public static final class Tokenizer implements FlatMapFunction<String, Tuple2<String, Integer>> {
		@Override
		public void flatMap(String value, Collector<Tuple2<String, Integer>> out) {
			//spliting the line
			String[] tokens = value.toLowerCase().split("\\W+");
			// get pairs
			for (String token : tokens) {
				token.replaceAll("\\p{Punct}+", "");
				//AlphaNum chars match only
				if (token.matches("\\p{Alpha}+")) {
					out.collect(new Tuple2<>(token, 1));
				}
			}
		}
	}
}

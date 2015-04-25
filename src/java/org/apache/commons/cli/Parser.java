/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.cli;

import java.util.*;

/**
 * <p><code>Parser</code> creates {@link CommandLine}s.</p>
 *
 * @author John Keyes (john at integralsource.com)
 * @see Parser
 * @version $Revision: 551815 $
 */
public abstract class Parser implements CommandLineParser {

	/** commandline instance */
	private CommandLine cmd;

	private boolean eatTheRest, processArg, processPropertiesBreakLoop;
	private Option opt;
	private ParseException exceptionToThrow;

	/**
	 * <p>Subclasses must implement this method to reduce
	 * the <code>arguments</code> that have been passed to the parse
	 * method.</p>
	 *
	 * @param opts The Options to parse the arguments by.
	 * @param arguments The arguments that have to be flattened.
	 * @param stopAtNonOption specifies whether to stop
	 * flattening when a non option has been encountered
	 * @return a String array of the flattened arguments
	 */
	protected abstract String[] flatten(Options opts, String[] arguments,
			boolean stopAtNonOption);

	/**
	 * <p>Parses the specified <code>arguments</code>
	 * based on the specifed {@link Options}.</p>
	 *
	 * @param options the <code>Options</code>
	 * @param arguments the <code>arguments</code>
	 * @return the <code>CommandLine</code>
	 * @throws ParseException if an error occurs when parsing the
	 * arguments.
	 */
	public CommandLine parse(Options options, String[] arguments)
			throws ParseException
	{
		return parse(options, arguments, null, false);
	}

	/**
	 * Parse the arguments according to the specified options and
	 * properties.
	 *
	 * @param options the specified Options
	 * @param arguments the command line arguments
	 * @param properties command line option name-value pairs
	 * @return the list of atomic option and value tokens
	 *
	 * @throws ParseException if there are any problems encountered
	 * while parsing the command line tokens.
	 */
	public CommandLine parse(Options options, String[] arguments,
			Properties properties)
					throws ParseException
	{
		return parse(options, arguments, properties, false);
	}

	/**
	 * <p>Parses the specified <code>arguments</code>
	 * based on the specifed {@link Options}.</p>
	 *
	 * @param options the <code>Options</code>
	 * @param arguments the <code>arguments</code>
	 * @param stopAtNonOption specifies whether to stop
	 * interpreting the arguments when a non option has
	 * been encountered and to add them to the CommandLines
	 * args list.
	 *
	 * @return the <code>CommandLine</code>
	 * @throws ParseException if an error occurs when parsing the
	 * arguments.
	 */
	public CommandLine parse(Options options, String[] arguments,
			boolean stopAtNonOption)
					throws ParseException
	{
		return parse(options, arguments, null, stopAtNonOption);
	}

	private boolean processArgCheckException() {
		boolean res = opt.getValues() == null && !opt.hasOptionalArg();
		if (res) exceptionToThrow = new MissingArgumentException(
				"Missing argument for option:" + opt.getKey());
		return res;
	}

	/**
	 * Parse the arguments according to the specified options and
	 * properties.
	 *
	 * @param options the specified Options
	 * @param arguments the command line arguments
	 * @param properties command line option name-value pairs
	 * @param stopAtNonOption stop parsing the arguments when the first
	 * non option is encountered.
	 *
	 * @return the list of atomic option and value tokens
	 *
	 * @throws ParseException if there are any problems encountered
	 * while parsing the command line tokens.
	 */
	public CommandLine parse(Options options, String[] arguments,
			Properties properties, boolean stopAtNonOption)
					throws ParseException
	{
		// clear out the data in options in case it's been used before (CLI-71)
		options.helpOptions().forEach(o -> ((Option)o).clearValues());

		List requiredOptions = options.getRequiredOptions();
		cmd = new CommandLine();

		eatTheRest = processArg = false;
		exceptionToThrow = null;

		List<String> tokenList = Arrays
				.asList(flatten(options, arguments == null ? new String[0]
						: arguments, stopAtNonOption));

		// process each flattened token
		tokenList.stream().forEachOrdered(t ->
		{
			// Abort in case of exception.
			if (exceptionToThrow != null)
				return;

			if (processArg)
				// found an Option, not an argument
				if (options.hasOption(t) && t.startsWith("-")) {
					processArg = false;
					if (processArgCheckException())
						return;

				} else
					// found a value
					try {
						opt.addValueForProcessing(Util
								.stripLeadingAndTrailingQuotes(t));
					} catch (RuntimeException exp) {
						processArg = false;
						if (processArgCheckException())
							return;

					}
			if (processArg)
				return;

			// eat the remaining tokens
			if (eatTheRest) {
				// ensure only one double-dash is added
				if (!"--".equals(t))
					cmd.addArg(t);
				return;
			}

			// the value is the double-dash OR single dash and stopAtNonOption=true.
			if ((t+stopAtNonOption).matches("-true|--(true|false)")) {
				eatTheRest = true;
				return;
			}

			// the value is a single dash
			if ("-".equals(t)) {
				cmd.addArg(t);
				return;
			}

			// the value is an option
			if (t.startsWith("-")) {
				if (!options.hasOption(t)) {
					if (eatTheRest = stopAtNonOption)
						cmd.addArg(t);
					else
						// <processOptions>
						// if there is no option throw an
						exceptionToThrow = new UnrecognizedOptionException(
								"Unrecognized option: " + t);

					return;
				}

				// get the option represented by arg
				opt = options.getOption(t);

				// if the option is a required option remove the option from
				// the requiredOptions list
				if (opt.isRequired())
					requiredOptions.remove(opt.getKey());

				// if the option is in an OptionGroup make that option the
				// selected
				// option of the group
				OptionGroup group = options.getOptionGroup(opt);
				if (group != null) {
					if (group.isRequired())
						requiredOptions.remove(group);

					try {
						group.setSelected(opt);
					} catch (AlreadySelectedException e) {
						exceptionToThrow = e;
						return;
					}
				}
				// set the option on the command line
				cmd.addOption(opt);

				// if the option takes an argument value
				processArg = opt.hasArg();
				// </processOptions>
				return;
			}

			// the value is an argument
			cmd.addArg(t);

			eatTheRest = stopAtNonOption;
		});

		// In case the loop ends while still in "processArg" mode.
		if (processArg)
			processArgCheckException();

		// Throw any exception that should have been thrown from the forEach.
		if (exceptionToThrow != null)
			throw exceptionToThrow;

		// <processProperties>
		if (properties != null) {
			processPropertiesBreakLoop = false;

			properties.stringPropertyNames().stream().forEachOrdered(optionName ->
			{
				if (processPropertiesBreakLoop)
					return;

				if (!cmd.hasOption(optionName)) {
					Option opt = options.getOption(optionName);

					// get the value from the properties instance
					String value = properties.getProperty(optionName);

					if (opt.hasArg()) {
						if (opt.getValues() == null || opt.getValues().length == 0)
							try {
								opt.addValueForProcessing(value);
							} catch (RuntimeException exp) {
								// if we cannot add the value don't worry about it
							}
					} else if (processPropertiesBreakLoop = !value.toLowerCase().matches("yes|true|1"))
						// if the value is not yes, true or 1 then don't add the
						// option to the CommandLine
						return;

					cmd.addOption(opt);
				}
			});
		}
		// </processProperties>

		if (requiredOptions.isEmpty())
			return cmd;

		String buff = requiredOptions.size() == 1 ? "Missing required option: "
				: "Missing required options: ";

		// loop through the required options
		throw new MissingOptionException((String)requiredOptions.stream().reduce(buff, (a,b) -> "" + a + b));

	}
}

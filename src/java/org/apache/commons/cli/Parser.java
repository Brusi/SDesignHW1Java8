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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Properties;

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

    /** current Options */
    private Options opts;

    /** list of required options strings */
    private List requiredOptions;

    List<String> tokenList;
    ParseException parseExp = null; // helper for catching and throwing exception from within forEach() of a stream
    
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

    /**
     * Parse the arguments according to the specified options and
     * properties.
     *
     * @param options the specified Options
     * @param args the command line arguments
     * @param properties command line option name-value pairs
     * @param stopAtNonOption stop parsing the arguments when the first
     * non option is encountered.
     *
     * @return the list of atomic option and value tokens
     *
     * @throws ParseException if there are any problems encountered
     * while parsing the command line tokens.
     */
    private boolean stop;
    public CommandLine parse(Options options, String[] args, 
                             Properties properties, boolean stopAtNonOption)
        throws ParseException {
        // initialise members
        opts = options;
        stop = stopAtNonOption;

        // clear out the data in options in case it's been used before (CLI-71)
        options.helpOptions().forEach(o -> ((Option)o).clearValues());

        requiredOptions = options.getRequiredOptions();
        cmd = new CommandLine();

//        boolean eatTheRest = false;

//		if (args == null) {
//			args = new String[0];
//		}

		tokenList = Arrays.asList(flatten(opts, args != null ? args : new String[0], stopAtNonOption));

		List<String> eatenRest = new ArrayList<String>();
		
		Optional<String> firstEater = tokenList.stream().filter(t -> { return filterStrings(t); }).findFirst();
		int index = firstEater.isPresent() ? tokenList.indexOf(firstEater.get()) : tokenList.size(); 

		tokenList.stream().skip(index).forEach(t -> { if (!"--".equals(t)) eatenRest.add(t); }); // eaten rest
		
		parseExp = null;
		tokenList.stream().limit(index).forEach(t -> handleOption(t)); // handle first part (before eaten rest)
		if (null != parseExp) {
			throw (parseExp);
		}
		
		// only eat rest here, because handleOption() might add args to cmd, in order
		eatenRest.stream().forEach(t -> cmd.addArg(t));
		
		
// ORIGINAL		
//		ListIterator iterator = tokenList.listIterator();
//
//		// process each flattened token
//		while (iterator.hasNext()) {
//			String t = (String) iterator.next();
//			System.out.println("t: " + t);
//
//			// the value is the double-dash
//			if ("--".equals(t)) {
//				eatTheRest = true;
//			}
//
//			// the value is a single dash
//			else if ("-".equals(t)) {
//				if (stopAtNonOption) {
//					eatTheRest = true;
//				} else {
//					cmd.addArg(t);
//				}
//			}
//
//			// the value is an option
//			else if (t.startsWith("-")) {
//				if (stopAtNonOption && !options.hasOption(t)) {
//					eatTheRest = true;
//					cmd.addArg(t);
//				} else {
//					processOption(t, iterator);
//				}
//			}
//
//			// the value is an argument
//			else {
//				cmd.addArg(t);
//
//				if (stopAtNonOption) {
//					eatTheRest = true;
//				}
//			}
//
//			// eat the remaining tokens
//			if (eatTheRest) {
//				while (iterator.hasNext()) {
//					String str = (String) iterator.next();
//
//					// ensure only one double-dash is added
//					if (!"--".equals(str)) {
//						cmd.addArg(str);
//					}
//				}
//			}
//		}

		processProperties(properties);
		checkRequiredOptions();

		return cmd;
	}

    private void handleOption(String t) {
    	if (null != parseExp) return; // avoid handling args if previous one caused exception
    	if ("--".equals(t)) return;
    	
    	if ("-".equals(t) && !stop) {
    		cmd.addArg(t);
    		return;
    	}
    	
    	if (t.startsWith("-")) { // value is option
    		if (stop && !opts.hasOption(t)) {
    			cmd.addArg(t);
    		}
    		else {
    			try {
					processOption(t);
					
				} catch (ParseException e) {
					parseExp = e;
				} // TODO maybe handle case where same arg appears twice in list?
    		}
    		return;
    	}
    	
    	// value is argument
    	cmd.addArg(t);
    }

	private boolean filterStrings(String t) {
    	if ("--".equals(t)) return true;
		if ("-".equals(t) && stop) return true;
		if (t.startsWith("-") && stop && !opts.hasOption(t)) return true;
		if (stop) return true;
    	
    	return false;
	}

//    private static final int OTHER = 0;
//    private static final int OPTION = 1;
//    private static final int ARGUMENT = 2;
//    private static final int DOUBLE_DASH = 3;
//    private static final int SINGLE_DASH = 4;
    
//	private int classify(String t) {
//		if ("--".equals(t)) return DOUBLE_DASH;
//		if ("-".equals(t) && stop) return SINGLE_DASH;
//		if (t.startsWith("-") && stop && !opts.hasOption(t)) return OPTION;
//		if (stop) return ARGUMENT;
//
//		return OTHER;
//	}

	/**
     * <p>Sets the values of Options using the values in 
     * <code>properties</code>.</p>
     *
     * @param properties The value properties to be processed.
     */
    private void processProperties(Properties properties)
    {
        if (properties == null)
        {
            return;
        }

        for (Enumeration e = properties.propertyNames(); e.hasMoreElements();)
        {
            String option = e.nextElement().toString();

            if (!cmd.hasOption(option))
            {
                Option opt = opts.getOption(option);

                // get the value from the properties instance
                String value = properties.getProperty(option);

                if (opt.hasArg())
                {
                    if ((opt.getValues() == null)
                        || (opt.getValues().length == 0))
                    {
                        try
                        {
                            opt.addValueForProcessing(value);
                        }
                        catch (RuntimeException exp)
                        {
                            // if we cannot add the value don't worry about it
                        }
                    }
                }
                else if (!("yes".equalsIgnoreCase(value) 
                           || "true".equalsIgnoreCase(value)
                           || "1".equalsIgnoreCase(value)))
                {
                    // if the value is not yes, true or 1 then don't add the
                    // option to the CommandLine
                    break;
                }

                cmd.addOption(opt);
            }
        }
    }

    /**
     * <p>Throws a {@link MissingOptionException} if all of the
     * required options are no present.</p>
     *
     * @throws MissingOptionException if any of the required Options
     * are not present.
     */
    private void checkRequiredOptions()
        throws MissingOptionException
    {
        // if there are required options that have not been
        // processsed
        if (requiredOptions.size() > 0)
        {
            Iterator iter = requiredOptions.iterator();
            StringBuffer buff = new StringBuffer("Missing required option");
            buff.append(requiredOptions.size() == 1 ? "" : "s");
            buff.append(": ");


            // loop through the required options
            while (iter.hasNext())
            {
                buff.append(iter.next());
            }

            throw new MissingOptionException(buff.toString());
        }
    }

    /**
     * <p>Process the argument values for the specified Option
     * <code>opt</code> using the values retrieved from the 
     * specified iterator <code>iter</code>.
     *
     * @param opt The current Option
     * @param iter The iterator over the flattened command line
     * Options.
     *
     * @throws ParseException if an argument value is required
     * and it is has not been found.
     */
    // ORIGINAL
    public void processArgs(Option opt, int index/*, ListIterator iter*/) throws ParseException {
		// loop until an option is found
    	ListIterator iter = tokenList.listIterator(index);
		while (iter.hasNext()) {
			String str = (String) iter.next();

			// found an Option, not an argument
			if (opts.hasOption(str) && str.startsWith("-")) {
				iter.previous();
				break;
			}

			// found a value
			try {
				opt.addValueForProcessing(Util
						.stripLeadingAndTrailingQuotes(str));
			} catch (RuntimeException exp) {
				iter.previous();
				break;
			}
		}

		if ((opt.getValues() == null) && !opt.hasOptionalArg()) {
			throw new MissingArgumentException("Missing argument for option:"
					+ opt.getKey());
		}
	}

    // JAVA 8 impl. (ODED)
    public void processArgs2(Option opt, int startIndex) throws ParseException {
    	
    	Optional<String> firstOption = tokenList.stream().skip(startIndex).filter(t -> t.startsWith("-") && opts.hasOption(t)).findFirst();
    	if (firstOption.isPresent()) {
    		int index = tokenList.indexOf(firstOption);
    		try {
	    		tokenList.stream()
	    			.skip(startIndex)
	    			.limit(index)
	    			.forEach(t -> opt.addValueForProcessing(Util.stripLeadingAndTrailingQuotes(t)));
    		} catch (RuntimeException e) {	}
    	}
		
		if ((opt.getValues() == null) && !opt.hasOptionalArg()) {
			throw new MissingArgumentException("Missing argument for option:"
					+ opt.getKey());
		}
	}
    
    
    /**
     * <p>Process the Option specified by <code>arg</code>
     * using the values retrieved from the specfied iterator
     * <code>iter</code>.
     *
     * @param arg The String value representing an Option
     * @param iter The iterator over the flattened command 
     * line arguments.
     *
     * @throws ParseException if <code>arg</code> does not
     * represent an Option
     */
    // ORIGINAL
//	private void processOption(String arg, ListIterator iter) throws ParseException {
//		boolean hasOption = opts.hasOption(arg);
//
//		// if there is no option throw an UnrecognisedOptionException
//		if (!hasOption) {
//			throw new UnrecognizedOptionException("Unrecognized option: " + arg);
//		}
//
//		// get the option represented by arg
//		final Option opt = opts.getOption(arg);
//
//		// if the option is a required option remove the option from
//		// the requiredOptions list
//		if (opt.isRequired()) {
//			requiredOptions.remove(opt.getKey());
//		}
//
//		// if the option is in an OptionGroup make that option the selected
//		// option of the group
//		if (opts.getOptionGroup(opt) != null) {
//			OptionGroup group = opts.getOptionGroup(opt);
//
//			if (group.isRequired()) {
//				requiredOptions.remove(group);
//			}
//
//			group.setSelected(opt);
//		}
//
//		// if the option takes an argument value
//		if (opt.hasArg()) {
//			processArgs(opt, 1/*, iter*/);
//		}
//
//		// set the option on the command line
//		cmd.addOption(opt);
//	}
	
	// JAVA 8 impl.
	private void processOption(String arg) throws ParseException {
		// if there is no option throw an UnrecognisedOptionException
		if (!opts.hasOption(arg)) {
			throw new UnrecognizedOptionException("Unrecognized option: " + arg);
		}

		// get the option represented by arg
		final Option opt = opts.getOption(arg);

		// if the option is a required option remove the option from
		// the requiredOptions list
		if (opt.isRequired()) {
			requiredOptions.remove(opt.getKey());
		}

		// if the option is in an OptionGroup make that option the selected
		// option of the group
		if (opts.getOptionGroup(opt) != null) {
			OptionGroup group = opts.getOptionGroup(opt);

			if (group.isRequired()) requiredOptions.remove(group);

			group.setSelected(opt);
		}

		// if the option takes an argument value
		if (opt.hasArg()) {
			processArgs(opt, tokenList.indexOf(arg) + 1);
		}

		// set the option on the command line
		cmd.addOption(opt); // adds unwanted tokens (includes arguments of options)
	}

}

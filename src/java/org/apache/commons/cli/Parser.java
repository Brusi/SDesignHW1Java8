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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
    private Options options;

    /** list of required options strings */
    private List requiredOptions;

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
    
    private boolean eatTheRest;
    private boolean processArg;
    private Option opt;
    private ParseException exceptionToThrow;
    
    public CommandLine parse(Options options, String[] arguments, 
                             Properties properties, boolean stopAtNonOption)
        throws ParseException
    {
        // initialise members
        this.options = options;

        // clear out the data in options in case it's been used before (CLI-71)
        options.helpOptions().forEach(o -> ((Option)o).clearValues());

        requiredOptions = options.getRequiredOptions();
        cmd = new CommandLine();

        eatTheRest = false;
        processArg = false;
        exceptionToThrow = null;

        if (arguments == null)
        {
            arguments = new String[0];
        }

        List<String> tokenList = Arrays.asList(flatten(this.options, 
                                               arguments, 
                                               stopAtNonOption));

        // process each flattened token
        tokenList.stream().forEachOrdered(t -> 
        {
        	// Abort in case of exception.
        	if (exceptionToThrow != null) {
        		return;
        	}
        	
        	if (processArg) {
        		// found an Option, not an argument
        		if (options.hasOption(t) && t.startsWith("-"))
        		{
        			processArg = false;
        			
					if ((opt.getValues() == null) && !opt.hasOptionalArg()) {
						exceptionToThrow = new MissingArgumentException(
								"Missing argument for option:" + opt.getKey());
						return;
					}
				} else {
					// found a value
					try {
						opt.addValueForProcessing(Util
								.stripLeadingAndTrailingQuotes(t));
					} catch (RuntimeException exp) {
						processArg = false;

						if ((opt.getValues() == null) && !opt.hasOptionalArg()) {
							exceptionToThrow = new MissingArgumentException(
									"Missing argument for option:"
											+ opt.getKey());
							return;
						}
					}
				}
        	}
        	if (processArg) {
        		return;
        	}
        	
            // eat the remaining tokens
			if (eatTheRest) {
				// ensure only one double-dash is added
				if (!"--".equals(t)) {
					cmd.addArg(t);
				}
				return;
			}

            // the value is the double-dash
			if ("--".equals(t)) {
				eatTheRest = true;
			}

            // the value is a single dash
			else if ("-".equals(t)) {
				if (stopAtNonOption) {
					eatTheRest = true;
				} else {
					cmd.addArg(t);
				}
			}

            // the value is an option
            else if (t.startsWith("-"))
            {
                if (stopAtNonOption && !options.hasOption(t))
                {
                    eatTheRest = true;
                    cmd.addArg(t);
                }
                else
                {
                	// <processOptions>
                	boolean hasOption = options.hasOption(t);

                    // if there is no option throw an UnrecognisedOptionException
                    if (!hasOption)
                    {
                        exceptionToThrow =  new UnrecognizedOptionException("Unrecognized option: " 
                                                              + t);
                        return;
                    }
                    
                    // get the option represented by arg
                    opt = options.getOption(t);

                    // if the option is a required option remove the option from
                    // the requiredOptions list
                    if (opt.isRequired())
                    {
                        requiredOptions.remove(opt.getKey());
                    }

                    // if the option is in an OptionGroup make that option the selected
                    // option of the group
                    if (options.getOptionGroup(opt) != null)
                    {
                        OptionGroup group = options.getOptionGroup(opt);

                        if (group.isRequired())
                        {
                            requiredOptions.remove(group);
                        }

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
                    if (opt.hasArg())
                    {
//                    	processArgs(opt, iterator);
                    	processArg = true;
                    	return;
                    }
                    // </processOptions>
                }
            }

            // the value is an argument
            else
            {
                cmd.addArg(t);

                if (stopAtNonOption)
                {
                    eatTheRest = true;
                }
            }
        });
        
		if (processArg && (opt.getValues() == null) && !opt.hasOptionalArg()) {
			exceptionToThrow =  new MissingArgumentException("Missing argument for option:"
					+ opt.getKey());
		}
		
		// Throw any exception that should have been thrown from the forEach.
		if (exceptionToThrow != null) {
			throw exceptionToThrow;
		}
        

        processProperties(properties);
        checkRequiredOptions();

        return cmd;
    }

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
                Option opt = options.getOption(option);

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
			String buff = requiredOptions.size() == 1 ? "Missing required option: "
					: "Missing required options: ";

			// loop through the required options
			buff = (String)requiredOptions.stream().reduce(buff, (a,b) -> ""+a+b);
			
			throw new MissingOptionException(buff.toString());
        }
    }
}

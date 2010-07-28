/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 22, 2010
 */
package org.jsoar.soarunit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.DebuggerProvider;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.DebuggerProvider.CloseAction;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.FileTools;
import org.jsoar.util.StringTools;
import org.jsoar.util.commands.DefaultInterpreterParser;

/**
 * @author ray
 */
public class TestCase
{
    private final File file;
    private final String name;
    private String setup = "";
    private final List<Test> tests = new ArrayList<Test>();
    
    private static String getNameFromFile(File file)
    {
        final String name = file.getName();
        final int dot = name.lastIndexOf('.');
        
        return dot > 0 ? name.substring(0, dot) : name;
    }
    
    public static TestCase fromFile(File file) throws SoarException, IOException
    {
        final PushbackReader reader = new PushbackReader(new BufferedReader(new FileReader(file)));
        try
        {
            final TestCase testCase = new TestCase(file, getNameFromFile(file));
            final DefaultInterpreterParser parser = new DefaultInterpreterParser();
            List<String> parsedCommand = parser.parseCommand(reader);
            while(!parsedCommand.isEmpty())
            {
                final String name = parsedCommand.get(0);
                if("setup".equals(name))
                {
                    testCase.setup += "\n";
                    testCase.setup += parsedCommand.get(1);
                }
                else if("test".equals(name))
                {
                    final Test test = new Test(testCase, parsedCommand.get(1), parsedCommand.get(2));
                    testCase.addTest(test);
                }
                else
                {
                    throw new SoarException("Unsupported SoarUnit command '" + name + "'");
                }
                
                parsedCommand = parser.parseCommand(reader);
            }
            return testCase;
        }
        finally
        {
            reader.close();
        }
    }
    
    public static int getTotalTests(List<TestCase> allCases)
    {
        int result = 0;
        for(TestCase testCase : allCases)
        {
            result += testCase.getTests().size();
        }
        return result;
    }
    
    public TestCase(File file, String name)
    {
        this.file = file;
        this.name = name;
    }

    /**
     * @return the setup
     */
    public String getSetup()
    {
        return setup;
    }

    /**
     * @param setup the setup to set
     */
    public void setSetup(String setup)
    {
        this.setup = setup;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * @return the file
     */
    public File getFile()
    {
        return file;
    }

    public void addTest(Test test)
    {
        tests.add(test);
    }
 
    /**
     * @return the tests
     */
    public List<Test> getTests()
    {
        return tests;
    }

    public Test getTest(String name)
    {
        for(Test test : tests)
        {
            if(name.equals(test.getName()))
            {
                return test;
            }
        }
        return null;
    }
    public TestCaseResult run(int index, int total, boolean haltOnFailure) throws SoarException
    {
        System.out.printf("%d/%d: Running test case '%s' from '%s'%n", index + 1, total, name, file);
        final TestCaseResult result = new TestCaseResult(this);
        for(Test test : tests)
        {
            final Agent agent = new Agent(test.getName());
            agent.initialize();
            try
            {
                final TestResult testResult = runTest(test, agent);
                result.addTestResult(testResult);
                if(haltOnFailure && !testResult.isPassed())
                {
                    break;
                }
            }
            finally
            {
                agent.dispose();
            }
        }
        return result;
    }
    
    public void debugTest(Test test) throws SoarException, InterruptedException
    {
        final ThreadedAgent agent = ThreadedAgent.create(test.getName());
        
        TestRhsFunction.addTestFunction(agent.getAgent(), "pass");
        TestRhsFunction.addTestFunction(agent.getAgent(), "fail");
        
        final Map<String, Object> debugProps = new HashMap<String, Object>();
        debugProps.put(DebuggerProvider.CLOSE_ACTION, CloseAction.DISPOSE);
        agent.getDebuggerProvider().setProperties(debugProps);
        agent.openDebuggerAndWait();
        
        agent.getPrinter().print("SoarUnit: Debugging %s%n", test);
        agent.getInterpreter().eval(String.format("pushd \"%s\"", FileTools.getParent(file).replace('\\', '/')));
        agent.getInterpreter().eval(setup);
        
        agent.getInterpreter().eval(test.getContent());
        agent.getPrinter().flush();
    }
    
    private void printMatchesOnFailure(Agent agent) throws SoarException
    {
        final Printer printer = agent.getPrinter();
        printer.startNewLine().print("# Matches for pass rules #\n");
        for(Production p : agent.getProductions().getProductions(null))
        {
            if(p.getName().startsWith("pass"))
            {
                printer.startNewLine().print("Partial matches for rule '%s'\n", p.getName());
                p.printPartialMatches(printer, WmeTraceType.NONE);
            }
        }
        printer.flush();
    }
    
    private TestResult runTest(Test test, final Agent agent) throws SoarException
    {
        final StringWriter output = new StringWriter();
        agent.getTrace().setWatchLevel(1);
        agent.getPrinter().addPersistentWriter(output);
        
        final TestRhsFunction succeededFunction = TestRhsFunction.addTestFunction(agent, "pass");
        final TestRhsFunction failedFunction = TestRhsFunction.addTestFunction(agent, "fail");
        
        agent.getInterpreter().eval(String.format("pushd \"%s\"", FileTools.getParent(file).replace('\\', '/')));
        agent.getInterpreter().eval(setup);
        
        System.out.printf("Running test: %s%n", test.getName());
        agent.getInterpreter().eval(test.getContent());
        final int cycles = 50000; 
        agent.runFor(cycles, RunType.DECISIONS);
        agent.getPrinter().flush();
        
        final FiringCounts firingCounts = getFiringCountsForTest(agent);
        
        if(failedFunction.isCalled())
        {
            printMatchesOnFailure(agent);
            return new TestResult(test, false, 
                              StringTools.join(failedFunction.getArguments(), ", "),
                              output.toString(),
                              firingCounts);
        }
        else if(!succeededFunction.isCalled())
        {
            printMatchesOnFailure(agent);
            final Long actualCycles = agent.getProperties().get(SoarProperties.D_CYCLE_COUNT);
            return new TestResult(test, false, 
                    String.format("never called (pass) function. Ran %d decisions.", actualCycles),
                              output.toString(),
                              firingCounts);
        }
        else
        {
            return new TestResult(test, true,
                    StringTools.join(succeededFunction.getArguments(), ", "), 
                     "",
                     firingCounts);
        }
    }
    
    
    private FiringCounts getFiringCountsForTest(Agent agent)
    {
        final FiringCounts result = new FiringCounts();
        for(Production p : agent.getProductions().getProductions(ProductionType.USER))
        {
            result.adjust(p.getName(), p.getFiringCount());
        }
        return result;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return name;
    }
    
}
/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import org.jsoar.tcl.SoarTclException;

/**
 * @author ray
 */
public class CommandLineRunnable implements Runnable
{
    private final JSoarDebugger ifc;
    private final String command;
    
    /**
     * @param ifc The debugger object
     * @param command The command string to evaluate
     */
    public CommandLineRunnable(JSoarDebugger ifc, String command)
    {
        this.ifc = ifc;
        this.command = command;
    }



    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        try
        {
            String result = ifc.getTcl().eval(command);
            if(result != null && result.length() != 0)
            {
                ifc.getTcl().getAgent().getPrinter().startNewLine().print(result).flush();
            }
        }
        catch (SoarTclException e)
        {
            ifc.getTcl().getAgent().getPrinter().error(e.getMessage() + "\n");
        }
        ifc.updateActionsAndStatus();
    }

}

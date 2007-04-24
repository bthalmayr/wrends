/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2007 Sun Microsystems, Inc.
 */

package org.opends.quicksetup;

import org.opends.quicksetup.util.Utils;
import org.opends.quicksetup.i18n.ResourceProvider;

import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

/**
 * Responsible for providing initial evaluation of command line arguments
 * and determining whether to launch a CLI, GUI, or print a usage statement.
 */
public abstract class Launcher {

  /** Arguments with which this launcher was invoked. */
  protected String[] args;

  /**
   * Creates a Launcher.
   * @param args String[] of argument passes from the command line
   */
  public Launcher(String[] args) {
    if (args == null) {
      throw new IllegalArgumentException("args cannot be null");
    }
    this.args = args;
  }

  /**
   * Indicates whether or not the launcher should print a usage
   * statement based on the content of the arguments passed into
   * the constructor.
   * @return boolean where true indicates usage should be printed
   */
  protected boolean shouldPrintUsage() {
    boolean printUsage = false;
    if (!isCli() && args.length > 0) {
      printUsage = true;
    } else {
      if ((args != null) && (args.length > 0)) {
        for (String arg : args) {
          if (arg.equalsIgnoreCase("-H") ||
                  arg.equalsIgnoreCase("--help")) {
            printUsage = true;
          }
        }
      }
    }
    return printUsage;
  }

  /**
   * Indicates whether the launcher will launch a command line versus
   * a graphical application based on the contents of the arguments
   * passed into the constructor.
   * @return boolean where true indicates that a CLI application should
   * be launched
   */
  protected boolean isCli() {
    boolean isCli = false;
    for (String arg : args) {
      if (arg.equalsIgnoreCase("--cli")) {
        isCli = true;
        break;
      }
    }
    return isCli;
  }

  /**
   * Creates an internationaized message based on the input key and
   * properly formatted for the terminal.
   * @param key for the message in the bundle
   * @return String message properly formatted for the terminal
   */
  protected String getMsg(String key)
  {
    return org.opends.server.util.StaticUtils.wrapText(getI18n().getMsg(key),
        Utils.getCommandLineMaxLineWidth());
  }

  /**
   * Prints a usage message to the terminal and exits
   * with exit code QuickSetupCli.USER_DATA_ERROR.
   * @param i18nMsg localized user message that will
   * be printed to std error
   */
  protected void printUsage(String i18nMsg) {
    System.err.println(i18nMsg);
  }

  /**
   * Launches the graphical uninstall. The graphical uninstall is launched in a
   * different thread that the main thread because if we have a problem with the
   * graphical system (for instance the DISPLAY environment variable is not
   * correctly set) the native libraries will call exit. However if we launch
   * this from another thread, the thread will just be killed.
   *
   * This code also assumes that if the call to SplashWindow.main worked (and
   * the splash screen was displayed) we will never get out of it (we will call
   * a System.exit() when we close the graphical uninstall dialog).
   *
   * @param args String[] the arguments used to call the SplashWindow main
   *         method
   * @return 0 if everything worked fine, or 1 if we could not display properly
   *         the SplashWindow.
   */
  protected int launchGui(final String[] args)
  {
    final int[] returnValue =
      { -1 };
    Thread t = new Thread(new Runnable()
    {
      public void run()
      {
        try
        {
          // Setup MacOSX native menu bar before AWT is loaded.
          Utils.setMacOSXMenuBar(getFrameTitle());
          SplashScreen.main(args);
          returnValue[0] = 0;
        }
        catch (Throwable t)
        {
          t.printStackTrace();
        }
      }
    });
    /*
     * This is done to avoid displaying the stack that might occur if there are
     * problems with the display environment.
     */
    PrintStream printStream = System.err;
    //System.setErr(new EmptyPrintStream());
    t.start();
    try
    {
      t.join();
    }
    catch (InterruptedException ie)
    {
      /* An error occurred, so the return value will be -1.  We got nothing to
      do with this exception. */
    }
    System.setErr(printStream);
    return returnValue[0];
  }

  /**
   * Gets the frame title of the GUI application that will be used
   * in some operating systems.
   * @return internationalized String representing the frame title
   */
  abstract protected String getFrameTitle();

  /**
   * Launches the command line based uninstall.
   *
   * @param args the arguments passed
   * @param cliApp the CLI application to launch
   * @return 0 if everything worked fine, and an error code if something wrong
   *         occurred.
   */
  protected int launchCli(String[] args, CliApplication cliApp) {
    System.setProperty("org.opends.quicksetup.cli", "true");

    QuickSetupCli cli = new QuickSetupCli(cliApp, args);
    int returnValue = cli.run();
    if (returnValue == QuickSetupCli.USER_DATA_ERROR) {
      printUsage();
    }

    // Add an extra space systematically
    System.out.println();

    return returnValue;
  }

  /**
   * Prints a usage statement to terminal and exits
   * with an error code.
   */
  protected abstract void printUsage();

  /**
   * Creates a CLI application that will be run if the
   * launcher needs to launch a CLI application.
   * @return CliApplication that will be run
   */
  protected abstract CliApplication createCliApplication();

  /**
   * Called before the launcher launches the GUI.  Here
   * subclasses can do any application specific things
   * like set system properties of print status messages
   * that need to be done before the GUI launches.
   */
  protected abstract void willLaunchGui();

  /**
   * Called if launching of the GUI failed.  Here
   * subclasses can so application specific things
   * like print a message.
   */
  protected abstract void guiLaunchFailed();

  /**
   * The main method which is called by the uninstall command lines.
   * @return int exit code that should be returned upon exit of this program
   */
  public int launch() {
    int exitCode = 0;
    if (shouldPrintUsage()) {
      printUsage();
      exitCode = QuickSetupCli.USER_DATA_ERROR;
    } else if (isCli()) {
      exitCode = launchCli(args, createCliApplication());
    } else {
      willLaunchGui();
      exitCode = launchGui(args);
      if (exitCode != 0) {
        guiLaunchFailed();
        exitCode = launchCli(args, createCliApplication());
      }
    }
    return exitCode;
  }

  /**
   * This class is used to avoid displaying the error message related to display
   * problems that we might have when trying to display the SplashWindow.
   *
   */
  private class EmptyPrintStream extends PrintStream {
    /**
     * Default constructor.
     *
     */
    public EmptyPrintStream()
    {
      super(new ByteArrayOutputStream(), true);
    }

    /**
     * {@inheritDoc}
     */
    public void println(String msg)
    {
    }
  }

  private ResourceProvider getI18n()
  {
    return ResourceProvider.getInstance();
  }

}

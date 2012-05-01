package loop;

import loop.ast.script.Unit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;

/**
 * Converts parsed, type-solved, emitted code to Java classes.
 */
public class Loop {

  public static void main(String[] args) {
    if (args.length == 0) {
      LoopShell.shell();
    }

    if (args.length > 1)
      run(args[0], false, true);
    else
      run(args[0]);
  }

  public static Object run(String file) {
    Executable unit = loopCompile(file);
    unit.runMain(true);

    return safeEval(unit);
  }

  public static Object run(String file,
                           boolean print,
                           boolean runMain) {
    Executable unit = loopCompile(file);
    if (print)
      System.out.println(unit.getCompiled());

    unit.runMain(runMain);
    return safeEval(unit);
  }

  public static Object evalClassOrFunction(String function,
                                           Unit shellScope) {
    Executable executable = new Executable(new StringReader(function));
    try {
      executable.compileClassOrFunction(shellScope);
    } catch (Exception e) {
      e.printStackTrace();
      return new LoopError("malformed function");
    }

    return "ok";
  }

  static Object safeEval(Executable executable) {
    try {
      if (executable.runMain())
        return executable.getCompiled().getDeclaredMethod("main").invoke(null);
      else
        return executable.getCompiled();
    } catch (InvocationTargetException e) {
      // Unwrap Java stack trace using our special wrapper exception.
      Throwable cause = e.getCause();
      StackTraceSanitizer.clean(cause);

      if (cause instanceof VerifyError)
        throw (Error) cause;
      throw (RuntimeException) cause;
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Compiles the specified file into a binary Java executable.
   */
  public static Class<?> compile(String file) {
    return loopCompile(file).getCompiled();
  }

  /**
   * Returns an executable that represents the compiled form of the Loop program.
   * <p/>
   * See {@link Executable} for more details on the compilation process.
   */
  private static Executable loopCompile(String file) {
    Executable executable;
    try {
      File script = new File(file);
      executable = new Executable(new FileReader(script), script.getName());
      executable.compile();
      if (executable.hasErrors()) {
        String errors = executable.printStaticErrorsIfNecessary();

        throw new LoopCompileException("Syntax errors exist:\n" + errors, executable);
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    return executable;
  }

  public static void error(String error) {
    throw new LoopExecutionException(error);
  }
}

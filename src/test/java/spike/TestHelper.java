package spike;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import junit.framework.Assert;

public class TestHelper {

    public static void compareFiles(File expected, File result) throws Exception {
        Assert.assertTrue("Missing reference file: " + expected, expected.exists());
        Assert.assertTrue("Missing result file: " + result, result.exists());
        BufferedReader expectedReader = new BufferedReader(
                new InputStreamReader(new FileInputStream(expected), "UTF-8"));
        BufferedReader resultReader = new BufferedReader(new InputStreamReader(new FileInputStream(result), "UTF-8"));

        int n = 0;
        while (true) {
            n++;
            String expectedLine = expectedReader.readLine();
            String resultLine = resultReader.readLine();
            if (expectedLine == null && resultLine == null) {
                return;
            }
            Assert.assertFalse("Too many rows in result: " + n, resultLine != null && expectedLine == null);
            Assert.assertFalse("Too few rows in result: " + n, expectedLine != null && resultLine == null);
            Assert.assertEquals("Diff in line " + n + " : ", expectedLine, resultLine);
        }

    }

    public static Process startJVM(Class<? extends Object> clazz, String arg) throws Exception {
        System.out.println("Starting JVM Process for " + clazz.getSimpleName());
        String separator = System.getProperty("file.separator");
        String classpath = System.getProperty("java.class.path");
        String path = System.getProperty("java.home") + separator + "bin" + separator + "java";
        ProcessBuilder processBuilder;
        if (arg == null) {
            processBuilder = new ProcessBuilder(path, "-Xms256m", "-Xmx512m", "-cp", classpath,
                    clazz.getCanonicalName());
        } else {
            processBuilder = new ProcessBuilder(path, "-Xms256m", "-Xmx512m", "-cp", classpath,
                    clazz.getCanonicalName(), arg);
        }
        // processBuilder.redirectErrorStream(true);
        System.out.println(processBuilder.command().toString().replaceAll(",", ""));
        Process process = processBuilder.start();
        return process;
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }
}

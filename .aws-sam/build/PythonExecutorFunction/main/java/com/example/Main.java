// src/main/java/com/example/Main.java
package com.example;

public class Main {
    public static void main(String[] args) {
        SecureLambdaPythonExecutor executor = new SecureLambdaPythonExecutor();

        // A simple, safe Python script to test with
        String testScript =
            "import json\n" +
            "print(json.dumps({'message': 'Hello from the secure Lambda sandbox!'}))";

        System.out.println("Invoking Lambda to execute Python script...");

        try {
            String result = executor.executeScript(testScript);
            System.out.println("\n--- Execution Result ---");
            System.out.println(result);
            System.out.println("------------------------");
        } catch (Exception e) {
            System.err.println("An error occurred during execution:");
            e.printStackTrace();
        }
    }
}
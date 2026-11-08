package com.hodastar.photosreview.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class TerminalCommandListener implements ApplicationRunner {

    private TerminalCommandTask terminalCommandTask;

    public TerminalCommandListener(TerminalCommandTask terminalCommandTask) {
        this.terminalCommandTask = terminalCommandTask;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Thread t = new Thread(this::listen);
        t.setName("terminal-command-listener");
        t.setDaemon(false);
        t.start();
    }

    private void runCommand(String[] parts) {
        if (parts.length == 0) {
            return;
        }
        if (parts.length == 1) {
            String command = parts[0];
            switch (command) {
                case "resetAdminPassword":
                    terminalCommandTask.resetAdminPassword();
                    break;
                default:
                    System.err.println("Unknown command: " + command);
            }
        }
    }

    private void listen() {
        try(BufferedReader reader =
                    new BufferedReader(new InputStreamReader(System.in))) {
            String command;
            while((command = reader.readLine()) != null) {
                String[] parts = command.trim().split("\\s+");
                runCommand(parts);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}

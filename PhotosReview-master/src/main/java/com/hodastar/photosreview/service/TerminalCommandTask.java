package com.hodastar.photosreview.service;

import com.hodastar.photosreview.mappers.UserMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class TerminalCommandTask {

    private final UserMapper userMapper;

    public TerminalCommandTask(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public void resetAdminPassword() {
        if (userMapper.resetAdminPassword()) {
            System.out.println("Reset admin password successful");
        } else  {
            System.out.println("Reset admin password failed");
        }
    }
}

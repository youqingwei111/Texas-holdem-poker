package com.poker.config;

import com.poker.entity.User;
import com.poker.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.logging.Logger;

/**
 * 测试数据初始化器
 * 系统启动时自动导入基本测试数据，便于功能演示和测试检查
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = Logger.getLogger(DataInitializer.class.getName());
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private static final List<TestUser> TEST_USERS = List.of(
            new TestUser("player1", "123456", "player1@example.com", "Player One"),
            new TestUser("player2", "123456", "player2@example.com", "Player Two"),
            new TestUser("player3", "123456", "player3@example.com", "Player Three"),
            new TestUser("test", "123456", "test@example.com", "Test User")
    );

    @Override
    @Transactional
    public void run(String... args) {
        long existingUsers = userRepository.count();
        if (existingUsers > 0) {
            log.info("数据库已有数据，跳过测试数据初始化");
            return;
        }

        log.info("开始初始化测试数据...");
        for (TestUser tu : TEST_USERS) {
            if (!userRepository.existsByUsername(tu.username)) {
                User user = new User();
                user.setUsername(tu.username);
                user.setPassword(passwordEncoder.encode(tu.password));
                user.setEmail(tu.email);
                user.setNickname(tu.nickname);
                user.setChips(1000L);
                userRepository.save(user);
                log.info("创建测试用户: " + tu.username + ", 密码: " + tu.password + ", 筹码: 1000");
            }
        }
        log.info("测试数据初始化完成，共 " + userRepository.count() + " 个用户");
    }

    private record TestUser(String username, String password, String email, String nickname) {}
}
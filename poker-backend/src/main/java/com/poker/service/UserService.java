package com.poker.service;

import com.poker.common.ErrorCode;
import com.poker.dto.LoginDTO;
import com.poker.dto.RegisterDTO;
import com.poker.entity.User;
import com.poker.exception.BusinessException;
import com.poker.repository.UserRepository;
import com.poker.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User register(RegisterDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        // chips 使用 User.java 中的默认值 1000L

        return userRepository.save(user);
    }

    public String login(LoginDTO dto) {
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public void updateChips(Long userId, Long chips) {
        User user = getById(userId);
        user.setChips(chips);
        userRepository.save(user);
    }

    /**
     * 增加玩家筹码（充值）
     */
    @Transactional
    public void addChips(Long userId, Long amount) {
        if (amount <= 0) return;
        userRepository.addChips(userId, amount);
        log.info("[UserService] 玩家 {} 增加 {} 筹码", userId, amount);
    }

    /**
     * 扣除玩家筹码（买入）
     * @return 成功返回剩余筹码，失败抛出异常
     */
    @Transactional
    public Long deductChips(Long userId, Long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "扣款金额必须大于0");
        }
        int rows = userRepository.deductChips(userId, amount);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_CHIPS, "账户筹码不足，买入失败");
        }
        log.info("[UserService] 玩家 {} 扣除 {} 筹码", userId, amount);
        // 返回扣款后的最新筹码
        User user = getById(userId);
        return user.getChips();
    }

    /**
     * 获取玩家当前筹码
     */
    public Long getChips(Long userId) {
        User user = getById(userId);
        return user.getChips();
    }
}
// src/main/java/com/example/bankingproject/common/security/repository/DeviceFingerprintRepository.java

package com.example.bankingproject.common.security.repository;

import com.example.bankingproject.common.security.entity.DeviceFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeviceFingerprintRepository extends JpaRepository<DeviceFingerprint, Long> {

    Optional<DeviceFingerprint> findByDeviceId(String deviceId);

    Optional<DeviceFingerprint> findByUserIdAndDeviceId(Long userId, String deviceId);

    @Query("""
        SELECT df FROM DeviceFingerprint df 
        WHERE df.userId = :userId 
        ORDER BY df.lastSeenAt DESC
        LIMIT 1
    """)
    Optional<DeviceFingerprint> findMostRecentDeviceForUser(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(DISTINCT df.deviceId) FROM DeviceFingerprint df 
        WHERE df.userId = :userId
    """)
    Integer countDistinctDevicesForUser(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(DISTINCT df.userId) FROM DeviceFingerprint df 
        WHERE df.deviceId = :deviceId
    """)
    Integer countDistinctUsersOnDevice(@Param("deviceId") String deviceId);
}
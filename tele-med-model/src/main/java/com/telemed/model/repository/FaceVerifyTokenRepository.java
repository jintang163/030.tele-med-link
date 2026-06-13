package com.telemed.model.repository;

import com.telemed.model.entity.FaceVerifyToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface FaceVerifyTokenRepository extends JpaRepository<FaceVerifyToken, Long> {

    Optional<FaceVerifyToken> findByToken(String token);

    @Query("SELECT t FROM FaceVerifyToken t WHERE t.token = :token AND t.used = 0 AND t.expireTime > :now")
    Optional<FaceVerifyToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE FaceVerifyToken t SET t.used = 1, t.usedTime = :usedTime, t.usedResource = :resource WHERE t.id = :id")
    int markUsed(@Param("id") Long id, @Param("usedTime") LocalDateTime usedTime, @Param("resource") String resource);
}

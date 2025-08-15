package com.adminservice.repository;

import com.adminservice.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    Optional<SystemConfig> findByConfigKey(String configKey);

    @Modifying
    @Transactional
    @Query("UPDATE StaticContent s SET s.content = :content WHERE s.page = :page")
    void updateContent(@Param("page") String page, @Param("content") String content);

}
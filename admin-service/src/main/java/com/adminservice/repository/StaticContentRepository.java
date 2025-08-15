package com.adminservice.repository;

import com.adminservice.entity.StaticContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface StaticContentRepository extends JpaRepository<StaticContent, Long> {
    Optional<StaticContent> findByPage(String page);
    
    @Modifying
    @Transactional
    @Query("UPDATE StaticContent s SET s.content = :content WHERE s.page = :page")
    void updateContent(@Param("page") String page, @Param("content") String content);
}
package com.adminservice.entity;

import com.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "static_contents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaticContent extends BaseEntity {
    
    @Column(unique = true, nullable = false)
    private String page;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private String contentType;
}
package com.commonlibrary.dto;


import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CustomDoctorInfoDto {
    private Long id;
    private Long userId;
    private String fullName;
}

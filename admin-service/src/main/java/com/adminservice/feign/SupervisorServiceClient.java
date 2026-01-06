package com.adminservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "supervisor-service")
public interface SupervisorServiceClient {

//    @GetMapping("/api/admin/supervisors")
//    List<SupervisorDto> getAllSupervisors();
//
//    @GetMapping("/api/admin/supervisors/pending")
//    List<SupervisorDto> getPendingVerification();
//
//    @PutMapping("/api/admin/supervisors/{id}/verify")
//    void verify(@PathVariable Long id, @RequestBody VerifyRequest req);
//
//    @GetMapping("/api/admin/supervisors/statistics")
//    SupervisorStatsDto getStatistics();

}

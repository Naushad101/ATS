package com.ats.poc.match;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    @Autowired private MatchingService matchingService;

    @GetMapping("/{jdId}")
    public ResponseEntity<List<Map<String, Object>>> match(@PathVariable UUID jdId) {
        return ResponseEntity.ok(matchingService.rankCandidates(jdId));
    }
}

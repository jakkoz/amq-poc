package com.example.one.web;

import com.example.one.service.RequestReplyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RequestController {

    private final RequestReplyService requestReplyService;

    public RequestController(RequestReplyService requestReplyService) {
        this.requestReplyService = requestReplyService;
    }

    @GetMapping("/api/send")
    public ResponseEntity<String> send(@RequestParam(defaultValue = "hello") String message) {
        String response = requestReplyService.sendAndWaitForResponse(message);
        return ResponseEntity.ok(response);
    }
}

package com.sittingspot.reviewprocesslayer.controllers;

import com.sittingspot.reviewprocesslayer.models.Review;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/review-pl/api/v1")
public class ReviewProcessLayerController {
    
    @GetMapping
    public List<Review> getReviewById(@RequestParam String id){
        return List.of(); //TODO ASK REVIEW DL
    }
}

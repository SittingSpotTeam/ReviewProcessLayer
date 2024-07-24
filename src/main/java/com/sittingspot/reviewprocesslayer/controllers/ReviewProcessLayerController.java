package com.sittingspot.reviewprocesslayer.controllers;

import com.sittingspot.reviewprocesslayer.models.Review;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/review-pl/api/v1")
public class ReviewProcessLayerController {
    
    @GetMapping
    public List<Review> getReviewById(@RequestParam UUID id){
        return List.of(); //TODO ASK REVIEW DL
    }

    @PostMapping
    public void postReview(@RequestParam UUID id, @RequestBody Review review){
        //TODO
    }
}

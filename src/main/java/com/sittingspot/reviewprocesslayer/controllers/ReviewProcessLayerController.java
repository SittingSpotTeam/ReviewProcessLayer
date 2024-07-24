package com.sittingspot.reviewprocesslayer.controllers;

import com.sittingspot.reviewprocesslayer.models.Review;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/review-pl/api/v1")
public class ReviewProcessLayerController {

    //TODO uncomment
    // @Value("${sittingspot.reviewdl.host}")
    // private String reviewdl_host;

    // @Value("${sittingspot.reviewdl.port}")
    // private int reviewdl_port;

    // @Value("${sittingspot.searchadapter.host}")
    // private String searchadapter_host;

    // @Value("${sittingspot.searchadapter.port}")
    // private int searchadapter_port;
    
    // @Value("${sittingspot.tagextractor.host}")
    // private String tagextractor;

    // @Value("${sittingspot.tagextractor.port}")
    // private int tagextractor_port;

    // @Value("${sittingspot.moderation.host}")
    // private String moderation_host;

    // @Value("${sittingspot.moderation.port}")
    // private int moderation_port;
    //TODO COMMENT
    private String reviewdl_host = "localhost";
    private int reviewdl_port = 8080;
    private String searchadapter_host = "localhost";
    private int searchadapter_port = 8080;
    private String moderation_host = "localhost";
    private int moderation_port = 8080;
    private String tagextractor_host = "localhost";
    private int tagextractor_port = 8080;
    private RestTemplate restTemplate = new RestTemplate();


    //TODO WHEN TAG EXTRACTION?

    @GetMapping
    public List<Review> getReviewById(@RequestParam UUID id){
        List<Review> review_list = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        //SET CUSTOM HEADERS
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        String url = "http://"+ reviewdl_host+":"+reviewdl_port+"/review-dl/api/v1";
        ResponseEntity<List<Review>> result = restTemplate.exchange(
                                                url, HttpMethod.GET, request,
                                                new ParameterizedTypeReference<List<Review>>() {},
                                                Collections.emptyMap() ) ;
        if (result.getStatusCode() == HttpStatus.OK) {
            review_list = result.getBody();
        }
        return review_list;
    }


    @PostMapping
    public HttpStatusCode postReview(@RequestParam UUID id, @RequestBody Review review){
        HttpHeaders headers = new HttpHeaders();
        //TODO SET CUSTOM PARAMETERS

        //CENSORING REVIEW
        HttpEntity<Review> moderation_request = new HttpEntity<>(review, headers);
        String moderation_url = "http://"+ moderation_host+":"+moderation_port+"/moderation/api/v1";
        ResponseEntity<Review> moderation_result = restTemplate.exchange(moderation_url, HttpMethod.POST, moderation_request,Review.class ) ;
        Review censored_review = new Review(moderation_url);
        if(moderation_result.getStatusCode() == HttpStatus.OK){
            censored_review = moderation_result.getBody();
        }
        else{
            return moderation_result.getStatusCode();
        }

        //POSTING CENSORED REVIEW
        HttpEntity<Review> publishing_request = new HttpEntity<>(censored_review, headers);
        String publishing_url = "http://"+ reviewdl_host+":"+reviewdl_port+"/review-dl/api/v1";
        ResponseEntity<HttpStatusCode> publishing_result = restTemplate.exchange(publishing_url, HttpMethod.POST,
                                                                                publishing_request, HttpStatusCode.class ) ;
        if(publishing_result.getStatusCode() == HttpStatus.OK){
            return publishing_result.getBody();
        }
        else{
            return publishing_result.getStatusCode();
        }

        //TODO
    }
}

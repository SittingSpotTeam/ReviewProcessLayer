package com.sittingspot.reviewprocesslayer.controllers;

import com.sittingspot.reviewprocesslayer.models.Review;
import com.sittingspot.reviewprocesslayer.models.Tag;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@RestController
@RequestMapping("/api/v1")
public class ReviewProcessLayerController {

    private RestTemplate restTemplate = new RestTemplate();

    //TODO uncomment
    @Value("${sittingspot.reviewdl.url}")
    private String reviewdl_url;
    @Value("${sittingspot.searchadapter.url}")
    private String searchadapter_url;
    @Value("${sittingspot.tagextractor.url}")
    private String tagextractor_url;
    @Value("${sittingspot.moderation.url}")
    private String moderation_url;

    private String current_version = "v1";
    //TODO WHEN TAG EXTRACTION?

    @GetMapping
    public List<Review> getReviewById(@RequestParam String id) throws IOException, InterruptedException {

        String url = "http://"+ reviewdl_url+"/review-dl/api/"+current_version;
        HttpHeaders headers = new HttpHeaders();
        //TODO SET CUSTOM HEADERS
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        //generating url
        System.out.println(url);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("sittingSpotId", id);
        // System.out.println(url)
        //fetching resources
        ResponseEntity<List<Review>> result = restTemplate.exchange(
                                                builder.toUriString(), HttpMethod.GET, request,
                                                new ParameterizedTypeReference<List<Review>>() {},
                                                Collections.emptyMap() ) ;
        return result.getBody();
    }


    @PostMapping
    public void postReview(@RequestBody Review review){
        HttpHeaders headers = new HttpHeaders();
        //TODO SET CUSTOM HEADERS

        //TODO CHECKING THAT SITTING SPOT EXISTS

        //censoring corpus
        HttpEntity<String> moderation_request = new HttpEntity<>(review.corpus(), headers);
        String moderation_request_url = "http://"+ moderation_url+"/";
        System.out.println(moderation_request_url);
        ResponseEntity<String> moderation_result = restTemplate.exchange(moderation_request_url, HttpMethod.POST, moderation_request,String.class ) ;
        if(moderation_result.getStatusCode() != HttpStatus.OK){
            System.out.println("Error while moderating review: "+moderation_result.getStatusCode());
        }
        String censored_corpus = moderation_result.getBody();
        
        System.out.println("ReviewCensored:"+censored_corpus);
        Review censored_review = new Review(review.sittingSpotId(), censored_corpus);

        
        //Extracting tags
        HttpEntity<Review> tag_request = new HttpEntity<>(censored_review, headers);
        String tag_url = "http://"+tagextractor_url+"/"+review.getSittingSpotId();
        ResponseEntity<List<Tag>> tag_result = restTemplate.exchange(
                                                tag_url, HttpMethod.POST, tag_request,
                                                new ParameterizedTypeReference<List<Tag>>() {},
                                                Collections.emptyMap() ) ;
        if (tag_result.getStatusCode() == HttpStatus.OK){
            //TODO WHO TO SEND TAGS TO? USE tag_result.getBody()
            System.out.println(tag_result.getBody());
        }
        else{
            System.err.println("Error while retrieving tags: "+tag_result.getStatusCode());
        }

        System.out.println("Review Tagged");


        //Posting censored review
        HttpEntity<Review> publishing_request = new HttpEntity<>(censored_review, headers);
        String publishing_url = "http://"+ reviewdl_url+"/";
        ResponseEntity<Void> publishing_result = restTemplate.exchange(publishing_url, HttpMethod.POST, publishing_request, Void.class) ;
        if (publishing_result.getStatusCode() == HttpStatus.OK){
            return;
        }
        else{
            System.err.println("Error while publishing review: "+ publishing_result.getStatusCode());
        }

        return;
    }
}

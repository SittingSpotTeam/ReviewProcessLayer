package com.sittingspot.reviewprocesslayer.controllers;

import com.sittingspot.reviewprocesslayer.models.Review;
import com.sittingspot.reviewprocesslayer.models.Tag;

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
@RequestMapping("/review-pl/api/v1")
public class ReviewProcessLayerController {

    private RestTemplate restTemplate = new RestTemplate();
    //TODO uncomment
    @Value("${sittingspot.reviewdl.host}")
    private String reviewdl_host;

    @Value("${sittingspot.reviewdl.port}")
    private int reviewdl_port;

    @Value("${sittingspot.searchadapter.host}")
    private String searchadapter_host;

    @Value("${sittingspot.searchadapter.port}")
    private int searchadapter_port;
    
    @Value("${sittingspot.tagextractor.host}")
    private String tagextractor_host;

    @Value("${sittingspot.tagextractor.port}")
    private int tagextractor_port;

    @Value("${sittingspot.moderation.host}")
    private String moderation_host;

    @Value("${sittingspot.moderation.port}")
    private int moderation_port;
    //TODO COMMENT
    // private String reviewdl_host = "localhost";
    // private int reviewdl_port = 8080;
    // private String searchadapter_host = "localhost";
    // private int searchadapter_port = 8080;
    // private String moderation_host = "localhost";
    // private int moderation_port = 8080;
    // private String tagextractor_host = "localhost";
    // private int tagextractor_port = 8080;
    // private RestTemplate restTemplate = new RestTemplate();

    private String current_version = "v1";
    //TODO WHEN TAG EXTRACTION?

    @GetMapping
    public List<Review> getReviewById(@RequestParam String id) throws IOException, InterruptedException {

        String url = "http://"+ reviewdl_host+":"+reviewdl_port+"/review-dl/api/"+current_version;
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
        String moderation_url = "http://"+ moderation_host+":"+moderation_port+"/moderation/api/"+current_version;
        System.out.println(moderation_url);
        ResponseEntity<String> moderation_result = restTemplate.exchange(moderation_url, HttpMethod.POST, moderation_request,String.class ) ;
        if(moderation_result.getStatusCode() != HttpStatus.OK){
            System.out.println("Error while moderating review: "+moderation_result.getStatusCode());
        }
        String censored_corpus = moderation_result.getBody();
        
        System.out.println("ReviewCensored:"+censored_corpus);
        Review censored_review = new Review(review.sittingSpotId(), censored_corpus);

        
        //Extracting tags
        HttpEntity<Review> tag_request = new HttpEntity<>(censored_review, headers);
        String tag_url = "http://"+tagextractor_host+":"+tagextractor_port+"/tag-extractor/api/"+current_version;
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
        String publishing_url = "http://"+ reviewdl_host+":"+reviewdl_port+"/review-dl/api/"+current_version;
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

package com.sittingspot.reviewprocesslayer.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sittingspot.reviewprocesslayer.models.Review;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Log4j2
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

        String searchUrl = "http://"+reviewdl_url+"?id="+id;
        log.info("Sending request "+searchUrl);

        var searchRequest = HttpRequest.newBuilder()
                .uri(URI.create(searchUrl))
                .header("Content-Type", "application/json")
                .build();
        var result = HttpClient.newHttpClient().send(searchRequest, HttpResponse.BodyHandlers.ofString());

        log.info("Got response code: "+result.statusCode());

        if(result.statusCode() != 200) {
            throw new ResponseStatusException(HttpStatus.valueOf(result.statusCode()));
        }

        List<Review> ret = new ObjectMapper().readerForListOf(Review.class).readValue(result.body());

        return ret;
    }


    @PostMapping("/{id}")
    public void postReview(@PathVariable String id,@RequestBody String text) throws IOException, InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        //TODO SET CUSTOM HEADERS

        //TODO CHECKING THAT SITTING SPOT EXISTS

        var review = new Review(id,text);
        //censoring corpus
        HttpEntity<String> moderation_request = new HttpEntity<>(review.getCorpus(), headers);
        String moderation_request_url = "http://"+ moderation_url;
        log.info("Sending request "+moderation_request_url + " with "+review.getCorpus());
        ResponseEntity<String> moderation_result = restTemplate.exchange(moderation_request_url, HttpMethod.POST, moderation_request,String.class ) ;
        if(moderation_result.getStatusCode() != HttpStatus.OK){
            System.out.println("Error while moderating review: "+moderation_result.getStatusCode());
        }
        String censored_corpus = moderation_result.getBody();
        
        log.info("ReviewCensored:"+censored_corpus);
        Review censored_review = new Review(review.getSittingSpotId(), censored_corpus);

        
        //Extracting tags
        String tag_url = "http://"+tagextractor_url+"/"+review.getSittingSpotId();
        log.info("Sending request "+tag_url + " with "+censored_review);

        var tagExtractionRequest = HttpRequest.newBuilder()
                .uri(URI.create(tag_url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(censored_review.getCorpus()))
                .build();
        var tagExtractionResutl = HttpClient.newHttpClient().send(tagExtractionRequest, HttpResponse.BodyHandlers.ofString());

        log.info("Got response code: "+tagExtractionResutl.statusCode());

        //Posting censored review
        HttpEntity<Review> publishing_request = new HttpEntity<>(censored_review, headers);
        String publishing_url = "http://"+ reviewdl_url;
        log.info("Sending request "+publishing_url + " with "+censored_review);
        ResponseEntity<Void> publishing_result = restTemplate.exchange(publishing_url, HttpMethod.POST, publishing_request, Void.class) ;
        if(publishing_result.getStatusCode() != HttpStatus.OK){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Error while publishing review");
        }

        return;
    }
}

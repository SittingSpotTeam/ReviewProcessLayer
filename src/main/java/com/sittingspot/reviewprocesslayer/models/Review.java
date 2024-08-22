package com.sittingspot.reviewprocesslayer.models;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Review{


    private String sittingSpotId;

    private String corpus;

    public Review(String sittingSpotId, String corpus){      
        this.sittingSpotId=sittingSpotId;
        this.corpus=corpus;
    }

    public void print(){
        System.out.println("sittingSpotId:"+this.sittingSpotId);
        System.out.println("corpus:"+this.corpus);
    }
}


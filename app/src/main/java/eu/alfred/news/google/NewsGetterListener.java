package eu.alfred.news.google;

import android.support.annotation.NonNull;

import eu.alfred.news.nytimes.NewsNYTimesResponse;

public interface NewsGetterListener {
    void onSuccess(@NonNull NewsNYTimesResponse newsResponse);

    void onError(@NonNull Exception e);
}

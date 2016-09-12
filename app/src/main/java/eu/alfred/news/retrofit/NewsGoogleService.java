package eu.alfred.news.retrofit;

import eu.alfred.news.google.NewsResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NewsGoogleService {

    @GET("ajax/services/search/news?v=1.0")
    Call<NewsResponse> getNews(@Query("q") String searchItem, @Query("hl") String lang, @Query("ned") String newsCountry);

    @GET("ajax/services/search/news?v=1.0")
    Call<NewsResponse> getNews(@Query("q") String searchItem, @Query("hl") String lang, @Query("ned") String newsCountry, @Query("userip") String userIP);

}
package eu.alfred.news.retrofit;

import eu.alfred.news.google.NewsResponse;
import eu.alfred.news.nytimes.NewsNYTimesResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NewsNYTimesService {

    @GET("svc/search/v2/articlesearch.json")
    Call<ResponseBody> getNews(@Query("api-key") String apiKey, @Query("q") String searchItem);

}
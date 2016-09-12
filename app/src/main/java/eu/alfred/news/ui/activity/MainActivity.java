package eu.alfred.news.ui.activity;

import com.google.gson.Gson;

import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import eu.alfred.news.R;
import eu.alfred.news.google.NewsGetterListener;
import eu.alfred.news.google.NewsResponse;
import eu.alfred.news.google.Result;
import eu.alfred.news.nytimes.Doc;
import eu.alfred.news.nytimes.NewsNYTimesResponse;
import eu.alfred.news.retrofit.NewsGoogleService;
import eu.alfred.news.retrofit.NewsNYTimesService;
import eu.alfred.news.util.Constants;
import eu.alfred.news.util.Prefs;
import eu.alfred.news.util.StringUtils;
import eu.alfred.ui.CircleButton;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import eu.alfred.ui.BackToPAButton;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    @InjectView(R.id.imageButtonLocalSettings)
    ImageButton imageButtonLocalSettings;
    @InjectView(R.id.editTextSetAddress)
    EditText editTextSetAddress;
    @InjectView(R.id.buttonSetAddress)
    ImageButton buttonSetAddress;
    @InjectView(R.id.contentNews)
    TextView contentNews;
    @InjectView(R.id.progressBarLoading)
    ProgressBar progressBarLoading;
    @InjectView(R.id.buttonSpeakNews)
    ImageButton buttonSpeakNews;

    private NewsNYTimesResponse newsResponse;
    private String newsTypeSaved = "";
    private TextToSpeech textToSpeech;
    private boolean textToSpeechInit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        init();
    }

    private void init() {
        initViews();
        setListeners();

        textToSpeechInit = false;
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeechInit = true;
                } else {
                    textToSpeechInit = false;
                }
            }
        });
    }

    private void initViews() {
        initActionBar();
        circleButton = (CircleButton) findViewById(R.id.voiceControlBtn);
        backToPAButton = (BackToPAButton) findViewById(R.id.backControlBtn);
        editTextSetAddress.setText(Prefs.getString(Constants.KEY_CADE_URL, Constants.LOCAL_CADE_URL));
        progressBarLoading.setAlpha(0f);
    }

    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    private void setListeners() {
        circleButton.setOnTouchListener(new MicrophoneTouchListener());
        backToPAButton.setOnTouchListener(new BackTouchListener());
        imageButtonLocalSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextSetAddress.getVisibility() == View.GONE) {
                    editTextSetAddress.setVisibility(View.VISIBLE);
                    buttonSetAddress.setVisibility(View.VISIBLE);
                } else {
                    editTextSetAddress.setVisibility(View.GONE);
                    buttonSetAddress.setVisibility(View.GONE);
                }
            }
        });
        editTextSetAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Prefs.setString(Constants.KEY_CADE_URL, editTextSetAddress.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        buttonSetAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cade.SetCadeBackendUrl(editTextSetAddress.getText().toString());
            }
        });
        buttonSpeakNews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String titles = newsTypeSaved + " "+getString(R.string.news)+".";
                int newsCount = 1;
                for (Doc doc : newsResponse.getResponse().getDocs()) {
                    String page = doc.getHeadline().getMain();
                    page = Html.fromHtml(page).toString();
                    if (!page.endsWith(".")) {
                        page += ".";
                    }
                    page = "Page " + newsCount + ". " + page;
                    titles += page;
                    newsCount++;
                }
                speak(titles);
            }
        });
    }

    private void speak(@NonNull String textToRead) {
        Log.d(TAG, "textToRead " + textToRead);
        try {
            if (textToSpeech != null && !TextUtils.isEmpty(textToRead) && textToSpeechInit) {
                if (textToSpeech.isSpeaking()) {
                    textToSpeech.stop();
                }
                textToSpeech.setLanguage(Locale.getDefault());
                if (textToRead.length() > TextToSpeech.getMaxSpeechInputLength()) {
                    textToRead = textToRead.substring(0, TextToSpeech.getMaxSpeechInputLength() - 1);
                }
                textToSpeech.setSpeechRate(0.8f);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    textToSpeech.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "" + System.currentTimeMillis());
                } else {
                    textToSpeech.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showNews(@NonNull final String newsType, @NonNull final TextView textView) {
        progressBarLoading.animate().alpha(1).setDuration(100).start();
        textView.animate().alpha(0).setDuration(150).start();
        getNews(newsType, new NewsGetterListener() {
            @Override
            public void onSuccess(NewsNYTimesResponse newsResponse) {
                textView.setText("");
                String pages = "<b><i>" + newsType + "</i></b><br><br>";
                MainActivity.this.newsResponse = newsResponse;
                for (Doc doc : newsResponse.getResponse().getDocs()) {
                    String page = "<b>" + doc.getHeadline().getMain() + "</b><br><br>" + doc.getLeadParagraph() + "<br><br>Link:<br>" + doc.getWebUrl() + "<br><br><br><br>";
                    page = page.replace("<b>", "");
                    page = page.replace("</b>", "");
                    pages += page;
                }
                pages += "<br><br><br><br><br><br><br><br><br><br><br><br>";
                Spanned spanned = Html.fromHtml(pages);
                textView.setText(spanned);
                textView.animate().alpha(1).setDuration(500).start();
                progressBarLoading.animate().alpha(0).setDuration(300).start();
                buttonSpeakNews.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(Exception e) {
                textView.setText(getString(R.string.error_retrieving)+" " + newsTypeSaved + " "+getString(R.string.news)+".");
                textView.animate().alpha(1).setDuration(500).start();
                progressBarLoading.animate().alpha(0).setDuration(400).start();
                buttonSpeakNews.setVisibility(View.GONE);
            }
        });
    }

    private void openLink(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        } catch (Exception ignored) {
        }
    }


    @Override
    public void performAction(String command, Map<String, String> map) {
        Log.d(TAG, "performAction command: #" + command + "#, map: #" + StringUtils.getReadableString(map) + "#");
        if (TextUtils.equals(command, Constants.CADE_ACTION_GET_NEWS)) {
            String nType = map.get(Constants.CADE_WHQUERY_SELECTED_NEWS_TYPE);
            if (!TextUtils.isEmpty(nType)) {
                newsTypeSaved = nType;
            } else {
                newsTypeSaved = Constants.DEFAULT_SEARCH_TERM;
            }
            showNews(newsTypeSaved, contentNews);
        }
        cade.sendActionResult(true);
    }

    @Override
    public void performWhQuery(String command, Map<String, String> map) {
    }

    @Override
    public void performValidity(String command, Map<String, String> map) {
    }

    @Override
    public void performEntityRecognizer(String command, Map<String, String> map) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        editTextSetAddress.setText(Prefs.getString(Constants.KEY_CADE_URL, Constants.LOCAL_CADE_URL));
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            textToSpeech.stop();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseTextToSpeech();
    }

    private void releaseTextToSpeech() {
        try {
            textToSpeechInit = false;
            if (textToSpeech.isSpeaking()) {
                textToSpeech.stop();
            }
            textToSpeech.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getNews(@NonNull final String newsType, @NonNull final NewsGetterListener newsGetterListener) {
        //getGoogleNews(newsType, newsGetterListener);
        getNYTimesNews(newsType, newsGetterListener);
    }

    /*private void getGoogleNews(@NonNull final String newsType, @NonNull final NewsGetterListener newsGetterListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String lang = Locale.getDefault().getLanguage();
                String newsCountry = Locale.getDefault().getCountry();

                String userIP = getUserIP();

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("https://ajax.googleapis.com/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                NewsGoogleService service = retrofit.create(NewsGoogleService.class);
                Call<NewsResponse> newsGetterCall;
                if (!TextUtils.isEmpty(userIP)) {
                    newsGetterCall = service.getNews(!TextUtils.isEmpty(newsType) ? newsType : Constants.DEFAULT_SEARCH_TERM, lang, newsCountry, userIP);
                } else {
                    newsGetterCall = service.getNews(!TextUtils.isEmpty(newsType) ? newsType : Constants.DEFAULT_SEARCH_TERM, lang, newsCountry);
                }
                try {
                    Response<NewsResponse> response = newsGetterCall.execute();
                    if (response.isSuccessful()) {
                        final NewsResponse newsResponse = response.body();
                        if (newsGetterListener != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (newsResponse != null && newsResponse.getResponseData() != null && newsResponse.getResponseData().getResults() != null && !newsResponse.getResponseData().getResults().isEmpty()) {
                                        newsGetterListener.onSuccess(newsResponse);
                                    } else {
                                        newsGetterListener.onError(new NullPointerException(getString(R.string.error_retrieving_data)));
                                    }
                                }
                            });
                        }
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (newsGetterListener != null) {
                                newsGetterListener.onError(e);
                            }
                        }
                    });
                }
            }
        }).start();
    }*/
    private void getNYTimesNews(@NonNull final String newsType, @NonNull final NewsGetterListener newsGetterListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String lang = Locale.getDefault().getLanguage();
                String newsCountry = Locale.getDefault().getCountry();

                String userIP = getUserIP();

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("https://api.nytimes.com/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                NewsNYTimesService service = retrofit.create(NewsNYTimesService.class);
                String apiKey = "ce102dd00bd74449a8cdeaf259d5ce9f";

                Call<ResponseBody> newsGetterCall = service.getNews(apiKey, !TextUtils.isEmpty(newsType) ? newsType : Constants.DEFAULT_SEARCH_TERM);

                try {
                    Response<ResponseBody> response = newsGetterCall.execute();
                    if (response.isSuccessful()) {
                        String content = response.body().string();
                        final NewsNYTimesResponse newsResponse = new Gson().fromJson(content, NewsNYTimesResponse.class);
                        if (newsGetterListener != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (
                                            newsResponse != null
                                            && newsResponse.getResponse() != null
                                            && newsResponse.getResponse().getDocs() != null
                                            && !newsResponse.getResponse().getDocs().isEmpty()
                                            ) {
                                        newsGetterListener.onSuccess(newsResponse);
                                    } else {
                                        newsGetterListener.onError(new NullPointerException(getString(R.string.error_retrieving_data)));
                                    }
                                }
                            });
                        }
                    }else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (newsGetterListener != null) {
                                    newsGetterListener.onError(new RuntimeException("Response not successful"));
                                }
                            }
                        });
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (newsGetterListener != null) {
                                newsGetterListener.onError(e);
                            }
                        }
                    });
                }
            }
        }).start();
    }

    public String getUserIP() {
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet("http://ifcfg.me/ip");
            // HttpGet httpget = new HttpGet("http://ipecho.net/plain");
            HttpResponse response;
            response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                long len = entity.getContentLength();
                if (len != -1 && len < 1024) {
                    String str = EntityUtils.toString(entity);
                    return str;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

}

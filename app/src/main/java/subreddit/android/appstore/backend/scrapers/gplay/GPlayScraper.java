package subreddit.android.appstore.backend.scrapers.gplay;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import subreddit.android.appstore.backend.ScrapeResult;
import subreddit.android.appstore.backend.TargetScraper;
import subreddit.android.appstore.backend.data.AppInfo;
import subreddit.android.appstore.backend.data.Download;

public class GPlayScraper implements TargetScraper {
    final OkHttpClient client = new OkHttpClient();
    @Override
    public Observable<ScrapeResult> scrape(final AppInfo appToScrape) {
        return Observable
                .create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(ObservableEmitter<String> e) throws Exception {
                        for (Download d : appToScrape.getDownloads()) {
                            if (d.getType() == Download.Type.GPLAY) {
                                e.onNext(d.getTarget());
                            }
                        }
                        e.onComplete();
                    }
                })
                .map(new Function<String, Response>() {
                    @Override
                    public Response apply(String s) throws Exception {
                        return client.newCall(new Request.Builder().url(s).build()).execute();
                    }
                })
                .map(new Function<Response, ScrapeResult>() {
                    @Override
                    public ScrapeResult apply(Response response) throws Exception {
                        Collection<String> urls = new ArrayList<String>();
                        String body = response.body().string();
                        int iconStart = body.indexOf("<img class=\"cover-image\"");
                        int iconEnd = body.indexOf("\" alt=\"Cover art\"",iconStart);
                        String icon = body.substring(body.indexOf("lh",iconStart),iconEnd);
                        while (body.contains("<img class=\"screenshot\"")) {
                            int start = body.indexOf("<img class=\"screenshot\"");
                            int end = body.indexOf("itemprop=\"screenshot\"",start);
                            String workingString = body.substring(start,end);
                            int subStart = workingString.indexOf("lh");
                            int subEnd = workingString.indexOf("\"",subStart);
                            workingString = workingString.substring(subStart,subEnd);
                            body = body.substring(end,body.length());
                            urls.add(workingString);
                        }
                        return new GPlayResult(urls,icon);
                    }
                });

    }
}

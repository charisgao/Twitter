package com.codepath.apps.restclienttemplate;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.parceler.Parcels;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Headers;

public class TimelineActivity extends AppCompatActivity implements TweetsAdapter.TweetListener {

    public static final String TAG = "TimelineActivity";
    public static final int REQUEST_CODE = 20;

    private SwipeRefreshLayout swipeContainer;
    private EndlessRecyclerViewScrollListener scrollListener;

    TwitterClient client;
    RecyclerView rvTweets;
    Button btnLogout;
    List<Tweet> tweets;
    TweetsAdapter adapter;

    MenuItem miActionProgressItem;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        client = TwitterApp.getRestClient(this);

        // Find the RecyclerView, log out button, and swipe container
        rvTweets = findViewById(R.id.rvTweets);
        btnLogout = findViewById(R.id.btnLogout);
        swipeContainer = findViewById(R.id.swipeContainer);

        // Initialize the list of tweets and adapter
        tweets = new ArrayList<>();
        adapter = new TweetsAdapter(this, tweets, this);

        // RecyclerView setup: layout manager and the adapter
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvTweets.setLayoutManager(linearLayoutManager);
        rvTweets.setAdapter(adapter);


        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, linearLayoutManager.getOrientation());
        rvTweets.addItemDecoration(dividerItemDecoration);

        populateHomeTimeline();

        // Button click listener
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onLogoutButton();
                Toast.makeText(getApplicationContext(), "Logout successful", Toast.LENGTH_SHORT).show();
            }
        });


        // Refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                populateHomeTimeline();
            }
        });

        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        // Endless scroll listener
        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                String lastTweetId = tweets.get(tweets.size() - 1).id;
                client.getHomeTimelineEndless(lastTweetId, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Headers headers, JSON json) {
                        // Show progress bar as an operation is in progress
                        miActionProgressItem.setVisible(true);

                        JSONArray jsonArray = json.jsonArray;
                        try {
                            tweets.addAll(Tweet.fromJsonArray(jsonArray));
                            adapter.notifyItemRangeInserted(tweets.size() - 1, 25);

                            // Hide progress bar as operation is finished
                            miActionProgressItem.setVisible(false);
                        } catch (JSONException e) {
                            Log.e(TAG, "Json exception", e);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                        Log.e("EndlessScroll", "onFailure" + response);
                    }
                });
            }
        };
        rvTweets.addOnScrollListener(scrollListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {   // item is the MenuItem that is tapped
        // Handle presses on the action bar items
        if (item.getItemId() == R.id.compose) { // Compose icon has been selected
            // Navigate to the compose activity
            Intent intent = new Intent(this, ComposeActivity.class);
            startActivityForResult(intent, REQUEST_CODE);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_CODE == requestCode && resultCode == RESULT_OK) {
            // Get data from the intent (tweet)
            Tweet tweet = Parcels.unwrap(data.getParcelableExtra("tweet"));

            // Update the RecyclerView with the tweet
            tweets.add(0, tweet);
            adapter.notifyItemInserted(0);
            rvTweets.smoothScrollToPosition(0);
        }
    }

    private void populateHomeTimeline() {
        // Send the network request to fetch the updated data
        client.getHomeTimeline(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                Log.i(TAG, "onSuccess" + json.toString());

                // Show progress bar as an operation is in progress
                miActionProgressItem.setVisible(true);

                JSONArray jsonArray = json.jsonArray;
                try {
                    // Clear out old items and add new items to adapter
                    adapter.clear();
                    tweets.addAll(Tweet.fromJsonArray(jsonArray));
                    adapter.notifyDataSetChanged();
                    scrollListener.resetState();

                    // setRefreshing false to signal refresh has finished
                    swipeContainer.setRefreshing(false);

                    // Hide progress bar as operation is finished
                    miActionProgressItem.setVisible(false);
                } catch (JSONException e) {
                    Log.e(TAG, "Json exception", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure" + response, throwable);
            }
        });
    }

    public void onLogoutButton() {
        // Forget who's logged in
        client.clearAccessToken();

        // Navigate backwards to Login screen
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Makes sure the Back button won't work
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Same as above
        startActivity(i);
    }

    // Prepare options menu for progress bar
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        miActionProgressItem = menu.findItem(R.id.miActionProgress);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void retweet(ImageButton ibRetweet, int position) {
        Tweet tweet = tweets.get(position);

        // if tweet is not retweeted yet
        if (!tweet.retweeted) {
            ibRetweet.setImageResource(R.drawable.ic_vector_retweet);
            client.retweet(tweet.id, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Headers headers, JSON json) {
                    Log.i("Retweet", "onSuccess" + json.toString());
                    Tweet newTweet = new Tweet();
                    try {
                        newTweet = Tweet.fromJson(json.jsonObject);
                        newTweet.favorited = false;
                        newTweet.retweeted = false;
                    } catch (JSONException e) {
                        Log.e("RetweetTimelineActivity", "Json exception", e);
                    }
                    tweets.add(0, newTweet);
                    adapter.notifyItemInserted(0);
                    rvTweets.smoothScrollToPosition(0);
                }

                @Override
                public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                    Log.e("Retweet", "onFailure" + response, throwable);
                    Toast.makeText(TimelineActivity.this, "Retweet unsuccessful", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            ibRetweet.setImageResource(R.drawable.ic_vector_retweet_stroke);
            client.unRetweet(tweet.id, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Headers headers, JSON json) {
                    Log.i("UnRetweet", "onSuccess" + json.toString());
                    rvTweets.smoothScrollToPosition(0);
                }

                @Override
                public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                    Log.e("UnRetweet", "onFailure" + response, throwable);
                    Toast.makeText(TimelineActivity.this, "Undo retweet unsuccessful", Toast.LENGTH_SHORT).show();
                }
            });
        }
        tweet.retweeted = !tweet.retweeted;
    }

    @Override
    public void favorite(ImageButton ibFavorite, int position) {
        Tweet tweet = tweets.get(position);

        // if tweet is not favorited yet
        if (!tweet.favorited) {
            ibFavorite.setImageResource(R.drawable.ic_vector_heart);
            client.favorite(tweet.id, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Headers headers, JSON json) {
                    Log.i("Favorite", "onSuccess" + json.toString());
                }

                @Override
                public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                    Log.e("Favorite", "onFailure" + response, throwable);
                    Toast.makeText(TimelineActivity.this, "Favorite unsuccessful", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            ibFavorite.setImageResource(R.drawable.ic_vector_heart_stroke);
            client.unFavorite(tweet.id, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Headers headers, JSON json) {
                    Log.i("UnFavorite", "onSuccess" + json.toString());
                }

                @Override
                public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                    Log.e("UnFavorite", "onFailure" + response, throwable);
                    Toast.makeText(TimelineActivity.this, "Unfavorite unsuccessful", Toast.LENGTH_SHORT).show();
                }
            });
        }
        tweet.favorited = !tweet.favorited;
    }
}
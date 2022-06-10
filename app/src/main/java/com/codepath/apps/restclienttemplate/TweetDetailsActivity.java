package com.codepath.apps.restclienttemplate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;

import org.json.JSONException;
import org.parceler.Parcels;

import okhttp3.Headers;

public class TweetDetailsActivity extends AppCompatActivity {

    ImageView ivDetailProfileImage;
    TextView tvDetailName;
    TextView tvDetailScreenName;
    TextView tvDetailBody;
    ImageView ivDetailImage;
    ImageButton ibTweetReply;
    ImageButton ibRetweet;
    ImageButton ibHeart;
    TextView tvDetailTimestamp;

    MenuItem miHome;

    Tweet tweet;
    TwitterClient client;
    int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tweet_details);

        client = TwitterApp.getRestClient(this);

        ivDetailProfileImage = findViewById(R.id.ivDetailProfileImage);
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailScreenName = findViewById(R.id.tvDetailScreenName);
        tvDetailBody = findViewById(R.id.tvDetailBody);
        ivDetailImage = findViewById(R.id.ivDetailImage);
        ibTweetReply = findViewById(R.id.ibTweetReply);
        ibRetweet = findViewById(R.id.ibRetweet);
        ibHeart = findViewById(R.id.ibHeart);
        tvDetailTimestamp = findViewById(R.id.tvDetailTimestamp);

        Toolbar toolbarDetail = (Toolbar) findViewById(R.id.toolbarDetail);
        setSupportActionBar(toolbarDetail);

        tweet = (Tweet) Parcels.unwrap(getIntent().getParcelableExtra(Tweet.class.getSimpleName()));
        position = getIntent().getIntExtra("position", 0);

        tvDetailName.setText(tweet.user.name);
        tvDetailScreenName.setText("@" + tweet.user.screenName);
        tvDetailBody.setText(tweet.body);
        tvDetailTimestamp.setText(formatTimestamp(tweet.createdAt));

        Glide.with(this).load(tweet.user.profileImageUrl).into(ivDetailProfileImage);
        ivDetailProfileImage.setClipToOutline(true);

        if (tweet.mediaUrl != null) {
            ivDetailImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(tweet.mediaUrl).centerCrop().transform(new RoundedCorners(20)).into(ivDetailImage);
        } else {
            ivDetailImage.setVisibility(View.GONE);
        }

        ibTweetReply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TweetDetailsActivity.this, ComposeActivity.class);
                intent.putExtra("isComment", true);
                intent.putExtra("screenName", tweet.user.screenName);
                intent.putExtra("replyId", tweet.id);
                startActivity(intent);
            }
        });

        if (tweet.retweeted) {
            ibRetweet.setImageResource(R.drawable.ic_vector_retweet);
        } else {
            ibRetweet.setImageResource(R.drawable.ic_vector_retweet_stroke);
        }

        if (tweet.favorited){
            ibHeart.setImageResource(R.drawable.ic_vector_heart);
        } else {
            ibHeart.setImageResource(R.drawable.ic_vector_heart_stroke);
        }

        ibRetweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retweet(tweet, ibRetweet, position);
            }
        });

        ibHeart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favorite(tweet, ibHeart, position);
            }
        });
    }

    public String formatTimestamp(String timeStamp) {
        int hour = Integer.parseInt(timeStamp.substring(11, 13));
        String append = "";
        if (hour < 12) {
            append = "AM";
        } else {
            hour -= 12;
            append = "PM";
        }
        String minute = timeStamp.substring(13, 16);
        String time = hour + minute + " " + append;
        String date = timeStamp.substring(4, 10) + "," + timeStamp.substring(25);
        return time + " · " + date;
    }

    public void retweet(Tweet tweet, ImageButton ibRetweet, int position) {
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
                    } catch (JSONException e) {
                        Log.e("RetweetTimelineActivity", "Json exception", e);
                    }
                }

                @Override
                public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                    Log.e("Retweet", "onFailure" + response, throwable);
                    Toast.makeText(TweetDetailsActivity.this, "Retweet unsuccessful", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            ibRetweet.setImageResource(R.drawable.ic_vector_retweet_stroke);
            client.unRetweet(tweet.id, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Headers headers, JSON json) {
                    Log.i("UnRetweet", "onSuccess" + json.toString());
                }

                @Override
                public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                    Log.e("UnRetweet", "onFailure" + response, throwable);
                    Toast.makeText(TweetDetailsActivity.this, "Undo retweet unsuccessful", Toast.LENGTH_SHORT).show();
                }
            });
        }
        tweet.retweeted = !tweet.retweeted;
    }

    public void favorite(Tweet tweet, ImageButton ibFavorite, int position) {
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
                    Toast.makeText(TweetDetailsActivity.this, "Favorite unsuccessful", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(TweetDetailsActivity.this, "Unfavorite unsuccessful", Toast.LENGTH_SHORT).show();
                }
            });
        }
        tweet.favorited = !tweet.favorited;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present
        getMenuInflater().inflate(R.menu.menu_details, menu);
        return true;
    }

    // Prepare options menu for progress bar
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        miHome = menu.findItem(R.id.miHome);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.miHome) {
            Intent intent = new Intent(TweetDetailsActivity.this, TimelineActivity.class);
            startActivity(intent);
        }
        return true;
    }
}
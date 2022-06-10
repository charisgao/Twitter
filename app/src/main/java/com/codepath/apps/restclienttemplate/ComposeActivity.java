package com.codepath.apps.restclienttemplate;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.asynchttpclient.RequestParams;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;
import com.codepath.oauth.OAuthBaseClient;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.parceler.Parcels;

import okhttp3.Headers;

public class ComposeActivity extends AppCompatActivity {

    public static final int MAX_TWEET_LENGTH = 280;

    EditText etCompose;
    Button btnTweet;
    TextInputLayout tilCompose;

    String tweetContent;
    String replyName = null;
    String replyId = "";

    TwitterClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        client = TwitterApp.getRestClient(this);

        etCompose = findViewById(R.id.etCompose);

        tilCompose = findViewById(R.id.tilCompose);
        tilCompose.setCounterMaxLength(MAX_TWEET_LENGTH);

        btnTweet = findViewById(R.id.btnTweet);

        boolean isComment = getIntent().getBooleanExtra("isComment", false);
        if (isComment) {
            replyName = "@" + getIntent().getStringExtra("screenName");
            replyId = getIntent().getStringExtra("replyId");
            Log.i("reply", replyId);

            String at = replyName + " ";
            etCompose.setText(at);

            etCompose.requestFocus();
        }

        // Set click listener on button
        btnTweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tweetContent = etCompose.getText().toString();
                if (replyName != null) {
                    tweetContent = tweetContent.substring(replyName.length());
                }

                if (tweetContent.isEmpty()) {
                    Toast.makeText(ComposeActivity.this, "Sorry, your tweet cannot be empty", Toast.LENGTH_LONG).show();
                    return;
                }
                else if (tweetContent.length() > MAX_TWEET_LENGTH) {
                    Toast.makeText(ComposeActivity.this, "Sorry, your tweet is too long", Toast.LENGTH_LONG).show();
                    return;
                }
                else {
                    // Make an API call to Twitter to publish the tweet
                    client.publishTweet(tweetContent, replyId, new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Headers headers, JSON json) {
                            Log.i("ComposeActivity", "onSuccess" + json.toString());
                            Tweet tweet = new Tweet();
                            try {
                                tweet = Tweet.fromJson(json.jsonObject);
                            } catch (JSONException e) {
                                Log.e("ComposeActivity", "Json exception", e);
                            }
                            Intent intent = new Intent();
                            intent.putExtra("tweet", Parcels.wrap(tweet));
                            setResult(RESULT_OK, intent);
                            finish();
                        }

                        @Override
                        public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                            Log.e("ComposeActivity", "onFailure" + response, throwable);
                        }
                    });
                }
            }
        });
    }

}
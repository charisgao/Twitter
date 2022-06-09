package com.codepath.apps.restclienttemplate;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.codepath.apps.restclienttemplate.models.Tweet;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TweetsAdapter extends RecyclerView.Adapter<TweetsAdapter.ViewHolder>{

    public interface TweetListener {
        void retweet(ImageButton ibRetweet, int position);
        void favorite(ImageButton ibFavorite, int position);
    }

    private static final int SECOND_MILLIS = 1000;  // 1 second = 1000 milliseconds
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;    // 1 minute = 60 seconds
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;


    Context context;
    List<Tweet> tweets;
    TweetListener tweetListener;

    // Pass in the context and list of tweets
    public TweetsAdapter(Context context, List<Tweet> tweets, TweetListener listener) {
        this.context = context;
        this.tweets = tweets;
        this.tweetListener = listener;
    }

    // For each row, inflate the layout
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tweet, parent, false);
        return new ViewHolder(view);
    }

    // Bind values based on the position of the element
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the data at position
        Tweet tweet = tweets.get(position);

        // Bind the tweet with ViewHolder
        holder.bind(tweet);
    }

    @Override
    public int getItemCount() {
        return tweets.size();
    }

    // Clean all elements of the recycler
    public void clear() {
        tweets.clear();
        notifyDataSetChanged();
    }

    public String getRelativeTimeAgo(String rawJsonDate) {
        String twitterFormat = "EEE MMM dd HH:mm:ss ZZZZZ yyyy";
        SimpleDateFormat sf = new SimpleDateFormat(twitterFormat, Locale.ENGLISH);
        sf.setLenient(true);

        try {
            long time = sf.parse(rawJsonDate).getTime();
            long now = System.currentTimeMillis();

            final long diff = now - time;
            if (diff < MINUTE_MILLIS) {
                return "· " + diff / SECOND_MILLIS + "s";
            } else if (diff < 60 * MINUTE_MILLIS) {
                return "· " + diff / MINUTE_MILLIS + "m";
            } else if (diff < 24 * HOUR_MILLIS) {
                return "· " + diff / HOUR_MILLIS + "h";
            } else {
                return "· " + diff / DAY_MILLIS + "d";
            }
        } catch (ParseException e) {
            Log.i("TweetsAdapter", "getRelativeTimeAgo failed");
            e.printStackTrace();
        }

        return "";
    }

    // Define a viewholder, itemView = one row/tweet in RecyclerView
    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivProfileImage;
        TextView tvName;
        TextView tvScreenName;
        TextView tvBody;
        TextView tvTimestamp;
        ImageView ivImage;
        ImageButton ibTweetReply;
        ImageButton ibRetweet;
        ImageButton ibHeart;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
            tvName = itemView.findViewById(R.id.tvName);
            tvScreenName = itemView.findViewById(R.id.tvScreenName);
            tvBody = itemView.findViewById(R.id.tvBody);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            ivImage = itemView.findViewById(R.id.ivImage);
            ibTweetReply = itemView.findViewById(R.id.ibTweetReply);
            ibRetweet = itemView.findViewById(R.id.ibRetweet);
            ibHeart = itemView.findViewById(R.id.ibHeart);
        }

        public void bind(Tweet tweet) {
            tvName.setText(tweet.user.name);
            tvScreenName.setText("@" + tweet.user.screenName);
            tvBody.setText(tweet.body);
            tvTimestamp.setText(getRelativeTimeAgo(tweet.createdAt));

            Glide.with(context).load(tweet.user.profileImageUrl).into(ivProfileImage);

            if (tweet.mediaUrl != null) {
                ivImage.setVisibility(View.VISIBLE);
                Glide.with(context).load(tweet.mediaUrl).into(ivImage);
            } else {
                ivImage.setVisibility(View.GONE);
            }

            ibTweetReply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, ComposeActivity.class);
                    intent.putExtra("isComment", true);
                    intent.putExtra("screenName", tweet.user.screenName);
                    intent.putExtra("replyId", tweet.id);
                    context.startActivity(intent);
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
                    tweetListener.retweet(ibRetweet, getAdapterPosition());
                }
            });

            ibHeart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tweetListener.favorite(ibHeart, getAdapterPosition());
                }
            });
        }
    }

}

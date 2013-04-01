package com.aokp.gerrit;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import com.aokp.gerrit.objects.CommitCard;
import com.aokp.gerrit.objects.JSONCommit;
import com.fima.cardsui.objects.Card;
import com.fima.cardsui.objects.CardStack;
import com.fima.cardsui.views.CardUI;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/1/13
 * Time: 2:07 AM
 */
public abstract class CardsActivity extends Activity {
    protected String TAG = getClass().getSimpleName();
    public static String GERRIT_WEBADDRESS = "http://gerrit.sudoservers.com";
    private static String GERRIT_PARAMS = "'Accept-Type: application/json'";
    public static String GERRIT_REVIEWABLE_COMMITS = "/changes/?q=status:open";
    public static String GERRIT_MERGED_COMMITS = "/changes/?q=status:merged";
    public static String GERRIT_ABANDONED_COMMITS = "/changes/?q=status:abandoned";

    protected void drawCardsFromList(List<Card> cards, CardUI cardUI) {
        CardStack cardStack = new CardStack();
        for (Card card : cards) {
            cardStack.add(card);
        }
        cardUI.addStack(cardStack);
        // once we finish adding all the cards begin building the screen
        cardUI.refresh();
    }

    protected List<Card> generateCards(String result) {
        List<Card> commitCardList = new LinkedList<Card>();
        try {
            JSONArray jsonArray = new JSONArray(result);
            int arraySize = jsonArray.length();
            for (int i = 0; arraySize > i; i++) {
                commitCardList.add(
                        CommitCard.generateCommitCard(
                                new JSONCommit(jsonArray.getJSONObject(i))));
            }
        } catch (JSONException e) {
            Log.d(TAG, "Failed to parse response from " + GERRIT_WEBADDRESS);
        }
        return commitCardList;
    }

    CardUI mCards;

    /**
     * This class handles the boilerplate code for those that inherit
     * @param savedInstanceState bundle containing state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.commit_list);
        mCards = (CardUI) findViewById(R.id.commit_cards);
        drawCardsFromList(generateCards(getJSONCommits()), mCards);
    }

    abstract String getQuery();

    // !!!TODO!!! MOVE TO ASYNC
    String getJSONCommits() {
        BufferedReader in = null;
        StringBuffer sb = new StringBuffer();
        BufferedReader inPost = null;
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpPost httpost = new HttpPost(GERRIT_WEBADDRESS);
            httpost.setHeader("Accept-Type", "application/json");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("status", getQuery()));
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = httpclient.execute(httpost);
            HttpEntity entity = response.getEntity();
            inPost = new BufferedReader(new InputStreamReader(entity.getContent()));
            String linePost = "";
            String NLPOST = System.getProperty("line.separator");
            while ((linePost = inPost.readLine()) != null) {
                sb.append(linePost + NLPOST);
            }
            inPost.close();
            if (entity != null) {
                entity.consumeContent();
            }
            httpclient.getConnectionManager().shutdown();
        } catch (ClientProtocolException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            return sb.toString();
        }
    }
}

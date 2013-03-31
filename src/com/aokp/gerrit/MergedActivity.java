package com.aokp.gerrit;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import com.aokp.gerrit.shell.AbstractAsyncSuCMDProcessor;
import com.fima.cardsui.objects.Card;
import com.fima.cardsui.views.CardUI;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 3/31/13
 * Time: 12:52 PM
 */
public class MergedActivity extends Activity {
    private static final String TAG = MergedActivity.class.getSimpleName();
    CardUI mCards;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.commit_list);
        mCards = (CardUI) findViewById(R.id.commit_cards);

        AbstractAsyncSuCMDProcessor cardFinder = new AbstractAsyncSuCMDProcessor() {
            @Override
            protected void onPostExecute(String result) {
                generateCards(result);
            }
        };
        cardFinder.execute(ShellHandler.GERRIT_REVIEWABLE_COMMITS);
    }

    private void generateCards(String result) {
        List<Card> commitCardList = new LinkedList<Card>();
        try {
            JSONArray jsonArray = new JSONArray(result);
            int arraySize = jsonArray.length();
            for (int i = 0; arraySize > i; i++) {
                commitCardList.add(new Card() {
                    @Override
                    public View getCardContent(Context context) {
                        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                        View commitCardView = inflater.inflate(R.layout.commit_card);
                    }
                });
            }
        } catch (JSONException e) {
            Log.d(TAG, "Failed to parse response from gerrit");
        }
    }
}

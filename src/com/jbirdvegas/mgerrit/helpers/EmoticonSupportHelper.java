package com.jbirdvegas.mgerrit.helpers;

import android.content.Context;
import android.text.Spannable;
import android.text.style.ImageSpan;
import com.jbirdvegas.mgerrit.R;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Injects emoticons into textviews
 * <p/>
 * Class based on code from StackOverflow
 * answer: http://stackoverflow.com/a/4302199/873237
 */
public class EmoticonSupportHelper {
    private static final Spannable.Factory spannableFactory
            = Spannable.Factory.getInstance();

    private static final Map<Pattern, Integer> emoticons
            = new HashMap<>(0);

    static {
        // standard emoticons
        addPattern(emoticons, ":)", R.drawable.emo_im_happy);
        addPattern(emoticons, ":-)", R.drawable.emo_im_happy);
        addPattern(emoticons, ":'(", R.drawable.emo_im_crying);
        addPattern(emoticons, ":-/", R.drawable.emo_im_undecided);
        addPattern(emoticons, "0:-)", R.drawable.emo_im_angel);
        addPattern(emoticons, "o:-)", R.drawable.emo_im_angel);
        addPattern(emoticons, "O:-)", R.drawable.emo_im_angel);
        addPattern(emoticons, ":-[", R.drawable.emo_im_embarrassed);
        addPattern(emoticons, ":-!", R.drawable.emo_im_foot_in_mouth);
        addPattern(emoticons, ":-$", R.drawable.emo_im_money_mouth);
        addPattern(emoticons, "B-)", R.drawable.emo_im_cool);
        addPattern(emoticons, ":O", R.drawable.emo_im_yelling);
        addPattern(emoticons, ":-*", R.drawable.emo_im_kissing);
        addPattern(emoticons, "=-O", R.drawable.emo_im_surprised);
        addPattern(emoticons, ":-P", R.drawable.emo_im_tongue_sticking_out);
        addPattern(emoticons, ";-)", R.drawable.emo_im_winking);
        addPattern(emoticons, ":-(", R.drawable.emo_im_sad);
        // custom emoticons
        addPattern(emoticons, ">-<", R.drawable.emo_im_chuck);
    }

    private static void addPattern(Map<Pattern, Integer> map, String smile,
                                   int resource) {
        map.put(Pattern.compile(Pattern.quote(smile)), resource);
    }

    public static boolean addSmiles(Context context, Spannable spannable) {
        boolean hasChanges = false;
        for (Map.Entry<Pattern, Integer> entry : emoticons.entrySet()) {
            Matcher matcher = entry.getKey().matcher(spannable);
            while (matcher.find()) {
                boolean set = true;
                for (ImageSpan span : spannable.getSpans(matcher.start(),
                        matcher.end(), ImageSpan.class)) {
                    if (spannable.getSpanStart(span) >= matcher.start()
                            && spannable.getSpanEnd(span) <= matcher.end()) {
                        spannable.removeSpan(span);
                    } else {
                        set = false;
                        break;
                    }
                }
                if (set) {
                    hasChanges = true;
                    spannable.setSpan(new ImageSpan(context, entry.getValue()),
                            matcher.start(), matcher.end(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        return hasChanges;
    }

    public static Spannable getSmiledText(Context context, CharSequence text) {
        Spannable spannable = spannableFactory.newSpannable(text);
        addSmiles(context, spannable);
        return spannable;
    }
}
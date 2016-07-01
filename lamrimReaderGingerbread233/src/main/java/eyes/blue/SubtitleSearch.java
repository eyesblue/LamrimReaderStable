package eyes.blue;

/**
 * Created by father on 16/6/21.
 */

import java.util.ArrayList;
import java.io.Serializable;
import android.util.Log;

public class SubtitleSearch implements Serializable{
    public static final int SUBTITLE_INDEX = 0;
    public static final int TEXT_INDEX = 1;

    public static final int LINEAR_INDEX =0;
    public static final int START_TIME_MS=1;
    public static final int END_TIME_MS=2;

    public int[][] subtitleTextInfo;
    public String allText;

    SubtitleSearch(SubtitleElement[] subtitle) {
        subtitleTextInfo = new int[subtitle.length][3];
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < subtitle.length; i++) {
            subtitleTextInfo[i][LINEAR_INDEX] = sb.length();
            subtitleTextInfo[i][START_TIME_MS] =subtitle[i].startTimeMs;
            subtitleTextInfo[i][END_TIME_MS]=subtitle[i].endTimeMs;
            sb.append(subtitle[i].text);
        }
        allText = sb.toString();
    }

    // 從串起來的文字(allText)中尋找該字串，再使用subtitleTextIndex找回該字串在字幕的哪一個位置，回傳為[字幕index][字幕文字中的第幾個字開始]。
    public int[][] search(String trg) {
        int i = 0;
        ArrayList<Integer> result = new ArrayList<Integer>();
        while ((i = allText.indexOf(trg, i)) > -1) {
            if(result.size()==0)result.add(i++);
            else if(result.get(result.size()-1) != i)// 過濾相同的目標字串位於同一個字幕內，而產生兩筆相同字幕的結果。
                result.add(i++);
            if (i >= allText.length())
                break;
        }

        if (result.size() == 0) return null;

        int[][] a = new int[result.size()][2];
        for (int j = 0; j < result.size(); j++) {
            a[j][SUBTITLE_INDEX] = getSubtitleIndex(result.get(j));
            a[j][TEXT_INDEX] = result.get(j) - subtitleTextInfo[a[j][SUBTITLE_INDEX]][LINEAR_INDEX];// 算出文字開頭的位置
        }
        return a;
    }

    // 從線性座標strIndex中轉換回字幕的index。
    private int getSubtitleIndex(int strIndex) {
//        Log.d(getClass().getName(), "Transfer linear index ["+strIndex+"] to subtitle index:");
        for (int i = 0; i < subtitleTextInfo.length; i++) {
//            Log.d(getClass().getName(), "Compare region index "+i+": linear index="+strIndex+", compare with: "+subtitleTextInfo[i][LINEAR_INDEX]+" and "+(subtitleTextInfo[i][LINEAR_INDEX] + getTextLength(i)));
            if (strIndex >= subtitleTextInfo[i][LINEAR_INDEX] && strIndex < subtitleTextInfo[i][LINEAR_INDEX] + getTextLength(i)) {
//                Log.d(getClass().getName(), "This is what we want");
                return i;
            }
        }
//        Log.d(getClass().getName(), "Not found!!!");
        return -1;
    }

    public SubtitleElement getSubtitle(int i) {
        SubtitleElement se=new SubtitleElement();
        se.startTimeMs=subtitleTextInfo[i][START_TIME_MS];
        se.endTimeMs=subtitleTextInfo[i][END_TIME_MS];
        se.text=getString(i);
        return se;
    }

    // Get the String of subtitle of index i.
    public String getString(int i){
        if(i==subtitleTextInfo.length-1)return allText.substring(subtitleTextInfo[i][LINEAR_INDEX]);

        return allText.substring(subtitleTextInfo[i][LINEAR_INDEX], subtitleTextInfo[i][LINEAR_INDEX]+getTextLength(i));
    }

    private int getTextLength(int i){
//        Log.d(getClass().getName(),"get text length: i="+i+", is last: "+(i==subtitleTextInfo.length-1));
        if(i==subtitleTextInfo.length-1)return allText.length()-subtitleTextInfo[i][LINEAR_INDEX];

        int len=subtitleTextInfo[i+1][LINEAR_INDEX] - subtitleTextInfo[i][LINEAR_INDEX];
//        if(len<0)Log.d(getClass().getName(),"Length="+subtitleTextInfo[i+1][LINEAR_INDEX]+" - "+subtitleTextInfo[i][LINEAR_INDEX]);
        return len;
    }
}

package eyes.blue;

/**
 * Created by father on 16/6/21.
 */

import java.util.ArrayList;
import java.io.Serializable;

public class SubtitleSearch implements Serializable{
    public static final int SUBTITLE_INDEX = 0;
    public static final int TEXT_INDEX = 1;

    public SubtitleElement[] subtitles;
    public int[] subtitleTextIndex;
    public String allText;

    SubtitleSearch(SubtitleElement[] subtitle) {
        this.subtitles = subtitle;
        subtitleTextIndex = new int[subtitles.length];
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < subtitles.length; i++) {
            subtitleTextIndex[i] = sb.length();
            sb.append(subtitles[i].text);
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
            a[j][TEXT_INDEX] = result.get(j) - subtitleTextIndex[a[j][SUBTITLE_INDEX]];// 算出文字開頭的位置
//			System.out.println(result.get(j)+"-"+subtitleTextIndex[a[j][SUBTITLE_INDEX]]+"="+a[j][TEXT_INDEX]);
        }
        return a;
    }

    // 從線性座標strIndex中轉換回字幕的index。
    private int getSubtitleIndex(int strIndex) {
        for (int i = 0; i < subtitles.length; i++)
            if (strIndex >= subtitleTextIndex[i] && strIndex < subtitleTextIndex[i] + subtitles[i].text.length())
                return i;

        return -1;
    }

    public SubtitleElement[] getSubtitles() {
        return subtitles;
    }
}

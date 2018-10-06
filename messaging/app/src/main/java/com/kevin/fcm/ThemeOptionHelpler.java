package com.kevin.fcm;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import com.blankj.utilcode.util.SPUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Iterator;
import java.util.Set;

public class ThemeOptionHelpler implements OnCompleteListener<Void> {

    public static int sequence = 1;

    private static final String TAG = ThemeOptionHelpler.class.getSimpleName();

    private static ThemeOptionHelpler mInstance;

    private ThemeOptionHelpler() {
    }

    public static ThemeOptionHelpler getInstance() {
        if (mInstance == null) {
            synchronized (ThemeOptionHelpler.class) {
                if (mInstance == null) {
                    mInstance = new ThemeOptionHelpler();
                }
            }
        }
        return mInstance;
    }

    private Context context;

    public static final String TAGSP = "addTagSp";

    public static final int NUll = 0x110;


    public static final int ADD_SUC = 0x111;
    public static final int ADD_FAI = 0x112;
    public static final int DEL_SUC = 0x113;
    public static final int DEL_FAI = 0x114;

    private SparseArray<Object> setActionCache = new SparseArray<Object>();

    public Object get(int sequence) {
        return setActionCache.get(sequence);
    }

    public Object remove(int sequence) {
        return setActionCache.get(sequence);
    }

    public void put(int sequence, Object tagAliasBean) {
        setActionCache.put(sequence, tagAliasBean);
    }

    /**
     * 增加
     */
    public static final int ACTION_ADD = 11;

    /**
     * 删除
     */
    public static final int ACTION_DELETE = 12;

    private static String curOptTheme = "";

    private void init(Context context) {
        this.context = context.getApplicationContext();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            {

                String theme = (String) msg.obj;
                if (theme == null
                        || theme.isEmpty()) {
                    return;
                }

                Log.e(TAG, "handleMessage: " + theme);
            }

            switch (msg.what) {

                case ACTION_ADD: {

                    final String theme = (String) msg.obj;
                    if (theme == null
                            || theme.isEmpty()) {
                        return;
                    }

                    curOptTheme = theme;

                    FirebaseMessaging
                            .getInstance()
                            .subscribeToTopic(theme)
                            .addOnCompleteListener(ThemeOptionHelpler.this);

                }
                break;
                case ACTION_DELETE: {

                    String theme = (String) msg.obj;
                    if (theme == null
                            || theme.isEmpty()) {
                        return;
                    }

                    curOptTheme = theme;

                    FirebaseMessaging
                            .getInstance()
                            .unsubscribeFromTopic(theme)
                            .addOnCompleteListener(ThemeOptionHelpler.this);
                }

                break;
                default:
                    break;
            }
        }
    };


    public void themeAction(Context context, int sequence, TagAliasBean tagAliasBean) {
        if (tagAliasBean == null
                || tagAliasBean.tags == null
                || tagAliasBean.tags.size() == 0) {
            return;
        }
        init(context);
        put(sequence, tagAliasBean);
        Iterator iterator = tagAliasBean.tags.iterator();
        int delay = 0;
        while (iterator.hasNext()) {
            Message message = handler.obtainMessage();
            message.what = tagAliasBean.action;
            message.obj = iterator.next();
            handler.sendMessageDelayed(message, delay += 3000);
        }
    }

    @Override
    public void onComplete(@NonNull Task<Void> task) {
        saveOptionResultOk(task.isSuccessful());
    }

    public void saveOptionResultOk(Boolean result) {
        TagAliasBean tagAliasBean = (TagAliasBean) get(sequence);
        if (tagAliasBean == null
                || tagAliasBean.tags == null
                || tagAliasBean.tags.size() == 0) {
            return;
        }

        switch (tagAliasBean.action) {
            case ACTION_ADD: {
                //绑定TAG成功就记下来
                Iterator iterator = tagAliasBean.tags.iterator();
                while (iterator.hasNext()) {
                    String tag = (String) iterator.next();
                    if (curOptTheme == null
                            || curOptTheme.isEmpty()
                            || tag == null
                            || tag.isEmpty()
                            || !curOptTheme.equals(tag)) {
                        continue;
                    }
                    Log.e(TAG, "saveOptionResultOk: " + curOptTheme + " : " + result);
                    SPUtils.getInstance(TAGSP).put(tag, result ? ADD_SUC : ADD_FAI);
                }
            }

            break;
            case ACTION_DELETE:
                Iterator iterator = tagAliasBean.tags.iterator();
                while (iterator.hasNext()) {
                    String tag = (String) iterator.next();
                    if (curOptTheme == null
                            || curOptTheme.isEmpty()
                            || tag == null
                            || tag.isEmpty()
                            || !curOptTheme.equals(tag)) {
                        continue;
                    }
                    Log.e(TAG, "saveOptionResultOk: " + curOptTheme + " : " + result);
                    SPUtils.getInstance(TAGSP).put(tag, result ? DEL_SUC : DEL_FAI);
                }
                break;
            default:
                break;
        }

    }

    public static class TagAliasBean {
        int action;
        Set<String> tags;

        @Override
        public String toString() {
            return "TagAliasBean{" +
                    "action=" + action +
                    ", tags=" + tags +
                    '}';
        }
    }
}

package com.amaze.filemanager.activities;

import java.util.ArrayList;

/**
 * Created by MoMxMo on 2016/4/24.
 */
public interface OnSendFileListChangeListener {
    void onSendFileListChange(ArrayList<String> selectFiles, int num);
}

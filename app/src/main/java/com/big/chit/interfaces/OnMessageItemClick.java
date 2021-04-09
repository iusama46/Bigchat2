package com.big.chit.interfaces;

import com.big.chit.models.Message;

/**
 * Created by a_man on 5/14/2017.
 */

public interface OnMessageItemClick {
    void OnMessageClick(Message message, int position);

    void OnMessageLongClick(Message message, int position);
}

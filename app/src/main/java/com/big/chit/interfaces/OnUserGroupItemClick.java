package com.big.chit.interfaces;

import android.view.View;

import com.big.chit.models.Group;
import com.big.chit.models.User;


public interface OnUserGroupItemClick {
    void OnUserClick(User user, int position, View userImage);
    void OnGroupClick(Group group, int position, View userImage);
}

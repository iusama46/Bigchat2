package com.big.chit.interfaces;

import com.big.chit.models.Contact;
import com.big.chit.models.User;

import java.util.ArrayList;

/**
 * Created by a_man on 01-01-2018.
 */

public interface HomeIneractor {
    User getUserMe();

    ArrayList<Contact> getLocalContacts();

}
